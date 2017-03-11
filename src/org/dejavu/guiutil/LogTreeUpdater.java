package org.dejavu.guiutil;

import org.dejavu.util.DjvLogMsg;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

/**
 * Updater of a the log tree. To be invoked by the AWT's dispatcher thread.
 */
public final class LogTreeUpdater implements Runnable
{
	private final JTree m_Tree;
	private final DefaultTreeModel m_TreeModel;
	private final DefaultMutableTreeNode m_TreeRoot;
	private final List<DjvLogMsg> m_NewLogBuffer;
	private final int m_MaxTreeSize;
	private final boolean autoScroll;
	
	/**
	 * Insertion mode for new messages.
	 */
	private static enum InsertionMode {
		TOP,
		BOTTOM,
		UNKNOWN
	}
	
	/**
	 * Creates a new log tree updater given a tree and a buffer of new log messages with which to update the tree.
	 * The new messages are either all added at the top of the tree or at the bottom of the tree, no out-of-sequence
	 * allowed.
	 * @param tree The log tree.
	 * @param logBuffer The buffer of new log messages.
	 * @param maxTreeSize The maximum size of the log tree (used to keep the tree trim).
	 * @param autoScroll Flag indicating whether the log tree is automatically scrolled to
	 * the bottom if the bottom of the tree was already in view, and that the addition of new
	 * messages causes the bottom of the tree to fall out of view.
	 */
	public LogTreeUpdater(JTree tree, Collection<DjvLogMsg> logBuffer, int maxTreeSize, boolean autoScroll)
	{
		super();
		m_Tree = tree;
		m_TreeModel = (DefaultTreeModel)tree.getModel();
		m_TreeRoot = (DefaultMutableTreeNode)m_TreeModel.getRoot();
		m_NewLogBuffer = new ArrayList<>(logBuffer);
		m_MaxTreeSize = maxTreeSize;
		this.autoScroll = autoScroll;
	}

	@Override
	public void run()
	{
		// Step1: Determine if auto scrolling is required: if bottom of log tree is visible AND no user-selected node is visible.
		boolean scrolling = DjvGuiUtil.autoScrollIsDesirable(m_Tree) && autoScroll;
		/*
		Rectangle viewRect = m_ScrollPane.getViewport().getViewRect();
		TreePath selectedNode = m_Tree.getSelectionPath();
		if((selectedNode == null) || (!viewRect.intersects(m_Tree.getPathBounds(selectedNode))))
		{
			TreePath bottom = m_Tree.getPathForRow(m_TreeModel.getChildCount(m_TreeRoot));
			if(bottom != null)
			{
				scrolling = viewRect.intersects(m_Tree.getPathBounds(bottom));
			}
		}
		*/

		//Step2: Extract all buffered messages.

		if(m_NewLogBuffer.size() < 1)
		{
			return;
		}

		List<Integer> insertionIndices = new LinkedList<>();
		InsertionMode mode = InsertionMode.UNKNOWN;
		int treeIndex = 0;
		long topLogIndex = -1;
		long bottomLogIndex = -1;
		int previousTreeSize = -1;
		for(DjvLogMsg msg : m_NewLogBuffer){
			if(mode == InsertionMode.UNKNOWN) {
				while(true) {
					// Determine the insertion mode, we either insert at the top
					// or at the bottom, never in the middle, too much CPU that way.
					if(m_TreeRoot.getChildCount() > 0) {
						TreeNode top = m_TreeRoot.getChildAt(0);
						TreeNode bottom = m_TreeRoot.getLastChild();
						
						// Check bottom first
						if(bottom instanceof DefaultMutableTreeNode) {
							Object obj = ((DefaultMutableTreeNode)bottom).getUserObject();
							if(obj instanceof DjvLogMsg) {
								DjvLogMsg bottomMsg = (DjvLogMsg)obj;
								bottomLogIndex = bottomMsg.index;
								if(msg.index > bottomMsg.index) {
									// New msg is bigger than bottom, insert at bottom
									mode = InsertionMode.BOTTOM;
									treeIndex = m_TreeRoot.getChildCount();
									previousTreeSize = treeIndex;
									break;
								}
							} else {
								System.err.println("Bottom " + obj + " is not a valid log msg");
							}
						} else {
							System.err.println("Bottom " + bottom + " is not a valid node");
						}
						
						// Then check top
						if(top instanceof DefaultMutableTreeNode) {
							Object obj = ((DefaultMutableTreeNode)top).getUserObject();
							if(obj instanceof DjvLogMsg) {
								if(msg.index < ((DjvLogMsg)obj).index) {
									// New msg is smaller than top, insert at top
									mode = InsertionMode.TOP;
									topLogIndex = ((DjvLogMsg)obj).index;
									break;
								}
							} else {
								System.err.println("Top " + obj + " is not a valid log msg");
							}
						} else {
							System.err.println("Top " + top + " is not a valid node");
						}
						// Not able to determine mode here
						System.err.println("Not able to determine log insertion mode");
					} else {
						//  Empty tree, insert at bottom
						mode = InsertionMode.BOTTOM;
					}
					break; // Not real loop
				}
			}
			switch(mode) {
				case BOTTOM:
					if(msg.index > bottomLogIndex) {
						m_TreeRoot.add(new DefaultMutableTreeNode(msg));
						insertionIndices.add(treeIndex++);
						bottomLogIndex = msg.index;
					} else {
						System.err.println("Discarding " + msg + " not fitting into bottom");
					}
					break;
					
				case TOP:
					if(msg.index < topLogIndex) {
						m_TreeRoot.insert(new DefaultMutableTreeNode(msg), treeIndex);
						insertionIndices.add(treeIndex++);
					} else {
						System.out.println("Discarding " + msg + " not fitting into top");
					}
					break;
					
				default:
					// Discard msg
					System.err.println("Discarding " + msg + " not able to determine insertion mode");
					break;
			}
		}

		// Calculating the insertion indices, a real pain in the a$$. Why oh why.
		int[] inserts = new int[insertionIndices.size()];
		int counter = 0;
		for(Integer idx : insertionIndices) {
			inserts[counter++] = idx;
		}
		m_TreeModel.nodesWereInserted(m_TreeRoot, inserts);
		
		// Expand log tree
		if(m_Tree.getRowCount() > 0) {
			m_Tree.isCollapsed(0); //???
			{
				m_Tree.expandRow(0);
			}
		}
		
		// Inserting at top will push all existing msgs down, we need to scroll
		// them back up
		if(mode == InsertionMode.TOP) {
			m_Tree.scrollRowToVisible(counter);
		}
		
		// Step3: Trim excess messages.
		int excessLogs = m_TreeRoot.getChildCount() - m_MaxTreeSize;
		// Some hysteresis (100)
		if(excessLogs > 100) {
			int[] deletionIndices = new int[excessLogs];
			Object[] deletedNodes = new Object[excessLogs];
			if(mode == InsertionMode.BOTTOM) {
				// Insert at bottom, so we trim the top
				for(int i = 0; i < excessLogs; ++i)
				{
					deletedNodes[i] = m_TreeRoot.getChildAt(0);
					deletionIndices[i] = i;
					m_TreeRoot.remove(0);
				}
			} else {
				// Insert at top, so we trim the bottom. This is tricky.
				int idx = m_TreeRoot.getChildCount() - 1;
				int count = excessLogs - 1;
				for(int i = 0; (i < excessLogs)&&(idx > -1); ++i)
				{
					deletedNodes[count] = m_TreeRoot.getChildAt(idx);
					deletionIndices[count--] = idx;
					m_TreeRoot.remove(idx--);
				}
			}
			m_TreeModel.nodesWereRemoved(m_TreeRoot, deletionIndices, deletedNodes);
			
			// In we trim the top (insert at bottom) then all the logs get scrolled
			// up, we need to scroll them back down.
			if((mode == InsertionMode.BOTTOM)){
				// This is the top visible row before insertion, bring it back into view.
				int previousTopVisibleRow = previousTreeSize - m_Tree.getVisibleRowCount();
				final int newTopVisibleRow = previousTopVisibleRow - excessLogs;
				if(newTopVisibleRow > -1) {
					// Allow all events to be handled before scrolling, by invoking scroll later.
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							m_Tree.scrollRowToVisible(newTopVisibleRow);
						}
					});
				}
			}
		}

		// Step4: execute any scrolling desired.
		if(scrolling){
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					// Note there are always one more row than number of child nodes
					// since the tree root is also a row.
					m_Tree.scrollRowToVisible(m_TreeRoot.getChildCount());
				}
			});
		}
	}
}
