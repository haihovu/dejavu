/*
 * DjvLogTree.java
 *
 * Created on July 19, 2008, 2:25 PM
 */

package org.dejavu.guiutil;

import org.dejavu.util.DjvBackgroundTask;
import org.dejavu.util.DjvExceptionUtil;
import org.dejavu.util.DjvLogMsg;
import org.dejavu.util.DjvSystem;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * Implements a tree of log messages.
 * @author  haiv
 */
public class DjvLogTree extends javax.swing.JTree
{
	private static final long serialVersionUID = 1L;

	/**
	 * Drag listener for the log tree.
	 */
	private static class LogTreeDragListener implements MouseMotionListener
	{
		private int initialSelectedRow = -1;
		private final JTree targetTree;
		/**
		 * Creates a new log tree drag listener instance.
		 * @param thetree The JTree against which drag events are listened to.
		 */
		LogTreeDragListener(JTree thetree)
		{
			targetTree = thetree;
		}

		@Override
		public void mouseDragged(MouseEvent e)
		{
			int lowIndex = 0x7ffffff;
			int highIndex = -1;
			int[] selectedRows = targetTree.getSelectionRows();
			if(selectedRows != null)
			{
				int numSelectedRows = selectedRows.length;
				for(int sRow : selectedRows)
				{
					if(numSelectedRows == 1)
					{
						initialSelectedRow = sRow;
						lowIndex = sRow;
						highIndex = sRow;
						break;
					}
					if(sRow < lowIndex)
					{
						lowIndex = sRow;
					}
					if(sRow > highIndex)
					{
						highIndex = sRow;
					}
				}
			}
			int row = targetTree.getRowForLocation(e.getX(), e.getY());
			if(row > -1)
			{
				if(row < lowIndex)
				{
					lowIndex = row;
				}
				else if(row > highIndex)
				{
					highIndex = row;
				}

				if(initialSelectedRow > highIndex)
					initialSelectedRow = highIndex;
				else if(initialSelectedRow < lowIndex)
					initialSelectedRow = lowIndex;

				if(row < initialSelectedRow)
				{
					highIndex = initialSelectedRow;
					if(lowIndex < row)
					{
						lowIndex = row;
					}
				}
				else if(row > initialSelectedRow)
				{
					lowIndex = initialSelectedRow;
					if(highIndex > row)
					{
						highIndex = row;
					}
				}
				targetTree.setSelectionInterval(lowIndex, highIndex);
			}
		}

		@Override
		public void mouseMoved(MouseEvent e)
		{
		}
	}

	/**
	 * Updater of the log tree, will wait for a bout 1 second to buffer up more logs before displaying them.
	 */
	private final class BgTaskLogTreeUpdater extends DjvBackgroundTask
	{
		/**
		 * Threshold which this task will wait before updating the tree,
		 * allowing more messages to be buffered. Zero or less means no wait.
		 */
		private final int waitThresholdMs;
		private final boolean autoScroll;
		/**
		 * Creates a new instance of the log tree updater.
		 * @param updateThresholdMs Threshold which this task will wait before updating the tree,
		 * allowing more messages to be buffered. Zero or less means no wait.
		 * @param autoScroll Flag indicating whether the log tree should be automatically
		 * scrolled to the bottom if the bottom was previously in view.
		 */
		private BgTaskLogTreeUpdater(int updateThresholdMs, boolean autoScroll)
		{
			super();
			waitThresholdMs = updateThresholdMs;
			this.autoScroll = autoScroll;
		}

		@Override
		public void run()
		{
			synchronized(m_LogBuffer)
			{
				try
				{
					if(waitThresholdMs > 0)
					{
						// Give the buffer a bit of time to collect more messages, for efficiency's sake.
						m_LogBuffer.wait(waitThresholdMs);
					}
					
					SwingUtilities.invokeLater(new LogTreeUpdater(DjvLogTree.this, m_LogBuffer, m_MaxSize, autoScroll));
					m_LogBuffer.clear();
				}
				catch(InterruptedException ex)
				{
				}
				finally
				{
					if(this == m_UpdateThread)
					{
						m_UpdateThread = null;
					}
				}
			}
		}
	}
	
	/**
	 * Swing task for retrieving the top message in the log tree
	 */
	private class GetTopMsg implements Runnable{
		private DjvLogMsg retValue;
		private boolean done;
		GetTopMsg() {
			super();
		}
		DjvLogMsg waitForIt(long waitMs){
			synchronized(this){
				if(!done){
					if(waitMs > 0){
						try {
							this.wait(waitMs);
						}
						catch(InterruptedException ex) {
						}
					}
				}
				return retValue;
			}
		}
		@Override
		public void run() {
			synchronized(this){
				try{
					if(m_Root.getChildCount() > 0){
						TreeNode top = m_Root.getFirstChild();
						if(top instanceof DefaultMutableTreeNode){
							Object userObj = ((DefaultMutableTreeNode)top).getUserObject();
							if(userObj instanceof DjvLogMsg){
								retValue = (DjvLogMsg)userObj;
							}
						}
					}
				}finally{
					done = true;
					this.notify();
				}
			}
		}
	}
	/**
	 * Swing task for retrieving the bottom message in the log tree
	 */
	private class GetBottomMsg implements Runnable{
		private DjvLogMsg retValue;
		private boolean done;
		GetBottomMsg() {
			super();
		}
		DjvLogMsg waitForIt(long waitMs){
			synchronized(this){
				if(!done){
					if(waitMs > 0){
						try {
							this.wait(waitMs);
						}
						catch(InterruptedException ex) {
						}
					}
				}
				return retValue;
			}
		}
		@Override
		public void run() {
			synchronized(this){
				try{
					if(m_Root.getChildCount() > 0){
						TreeNode bottom = m_Root.getLastChild();
						if(bottom instanceof DefaultMutableTreeNode){
							Object userObj = ((DefaultMutableTreeNode)bottom).getUserObject();
							if(userObj instanceof DjvLogMsg){
								retValue = (DjvLogMsg)userObj;
							}
						}
					}
				}finally{
					done = true;
					this.notify();
				}
			}
		}
	}
	
	/** Creates new instance of MiLogTree
	 * Must be invoked from EDT.
	 * @param name The name of this log tree.
	 * @param maxSize Maximum number of lines of log in the tree before oldest ones are discarded.
	 */
    public DjvLogTree(String name, int maxSize)
	{
		m_MaxSize = maxSize;
		m_Root = new DefaultMutableTreeNode(name);
		m_Model = new DefaultTreeModel(m_Root);
		setModel(m_Model);
		setCellRenderer(new LogTreeCellRenderer());
		setShowsRootHandles(true);
		addMouseMotionListener(new LogTreeDragListener(this));
    }

	/**
	 * Retrieves the top message in the tree, i.e. the oldest message.
	 * May be invoked from any thread.
	 * @return The top message in the tree, or null if the tree is empty or if an interruption occurred.
	 */
	public DjvLogMsg getTopMsg() {
		GetTopMsg get = new GetTopMsg();
		if(!SwingUtilities.isEventDispatchThread()){
			SwingUtilities.invokeLater(get);
		} else {
			get.run();
		}
		return get.waitForIt(10000);
	}
	
	/**
	 * Retrieves the bottom message in the tree, i.e. the newest message.
	 * May be invoked from any thread.
	 * @return The bottom message in the tree, or null if the tree is empty or if an interruption occurred.
	 */
	public DjvLogMsg getBottomMsg() {
		GetBottomMsg get = new GetBottomMsg();
		if(SwingUtilities.isEventDispatchThread()){
			get.run();
		}else{
			SwingUtilities.invokeLater(get);
		}
		return get.waitForIt(10000);
	}
	
	/**
	 * Adds a new log message to the log tree, using an update threshold of 1 second.
	 * Asynchronous (use background thread), may be invoked by any thread.
	 * @param logMsg The message to be added.
	 * @param autoScroll Flag indicating whether the log tree should be automatically
	 * scrolled to the bottom if the bottom was previously in view.
	 */
	public void addMessage(DjvLogMsg logMsg, boolean autoScroll)
	{
		addMessage(logMsg, 1000, autoScroll);
	}

	/**
	 * Adds a new log message to the log tree.
	 * Asynchronous (use background thread), may be invoked by any thread.
	 * @param logMsg The message to be added.
	 * @param updateThresholdMs Threshold which the background task task will wait before updating the tree,
	 * allowing more messages to be buffered. Zero or less means no wait.
	 * @param autoScroll Flag indicating whether the log tree should be automatically
	 * scrolled to the bottom if the bottom was previously in view.
	 */
	public void addMessage(DjvLogMsg logMsg, int updateThresholdMs, boolean autoScroll)
	{
		synchronized(m_LogBuffer)
		{
			m_LogBuffer.add(logMsg);
			if(m_UpdateThread == null)
			{
				m_UpdateThread = new BgTaskLogTreeUpdater(updateThresholdMs, autoScroll).start();
			}
		}
	}

	/**
	 * Adds a number of new log messages to the log tree (may be at the top or bottom depending on their indices).
	 * Asynchronous (use background thread), may be invoked by any thread.
	 * @param msgs The messages to be added.
	 * @param autoScroll Flag indicating whether the log tree should be automatically
	 * scrolled to the bottom if the bottom was previously in view.
	 */
	public void addMessages(Collection<DjvLogMsg> msgs, boolean autoScroll){
		// Add these as a whole, right away
		// Process any pending log msgs first
		synchronized(m_LogBuffer){
			if(!m_LogBuffer.isEmpty()) {
				SwingUtilities.invokeLater(new LogTreeUpdater(DjvLogTree.this, m_LogBuffer, m_MaxSize, autoScroll));
				m_LogBuffer.clear();
			}
		}
		SwingUtilities.invokeLater(new LogTreeUpdater(DjvLogTree.this, msgs, m_MaxSize, autoScroll));
	}

	/**
	 * Retrieves the number of messages displayed in the log tree.
	 * @return The number of messages.
	 * @NotThreadSafe Must be invoked from EDT.
	 */
	public int getNumMessages()
	{
		return m_Root.getChildCount();
	}

	/**
	 * Selects all nodes on the log tree.
	 * May be invoked by any thread.
	 */
	public void selectAllNodes()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				Enumeration<?> children = m_Root.children();
				TreePath[] selectionPaths = new TreePath[m_Root.getChildCount()];
				int idx = 0;
				while(children.hasMoreElements())
				{
					if(idx < selectionPaths.length)
					{
						selectionPaths[idx++] = new TreePath(new Object[]{m_Root, children.nextElement()});
					}
					else
					{
						break;
					}
				}
				setSelectionPaths(selectionPaths);
			}
		});
	}
	
	/**
	 * Removes all messages from the log tree.
	 * May be invoked by any thread.
	 */
	public void clearMessages()
	{
		synchronized(m_LogBuffer)
		{
			m_LogBuffer.clear();
			if(m_UpdateThread != null)
			{
				m_UpdateThread.stop();
			}
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						if(isCollapsed(0))
						{
							expandRow(0);
						}

						int numMsgs = m_Root.getChildCount();
						if(numMsgs > 0)
						{
							int[] deletionIndices = new int[numMsgs];
							Object[] deletedNodes = new Object[numMsgs];
							for(int i = 0; i < numMsgs; ++i)
							{
								deletedNodes[i] = m_Root.getChildAt(0);
								deletionIndices[i] = i;
								m_Root.remove(0);
							}
							m_Model.nodesWereRemoved(m_Root, deletionIndices, deletedNodes);
						}
					}
					catch(RuntimeException e)
					{
						DjvSystem.logError(DjvLogMsg.Category.DESIGN, DjvExceptionUtil.simpleTrace(e));
					}
				}
			});
		}
	}

	/**
	 * Scrolls the log pane view port to the bottom of the log tree.
	 * May be invoked by any thread.
	 */
	public void scrollToEnd()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				scrollRowToVisible(m_Root.getChildCount());
			}
		});
	}
	
	private final DefaultMutableTreeNode m_Root;
	private final DefaultTreeModel m_Model;
	private final int m_MaxSize;
	/**
	 * List of new messages to be appended to the log tree.
	 */
	private final List<DjvLogMsg> m_LogBuffer = new LinkedList<>();
	private DjvBackgroundTask m_UpdateThread;
}
