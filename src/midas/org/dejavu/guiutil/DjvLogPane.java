/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dejavu.guiutil;

import org.dejavu.util.DjvLogMsg;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 * <p>
 A custom log pane that works with the MiSystem log framework.
 This is an extension of the JScrollPane that contains a log tree.
 DjvLogPane takes care of updating the log tree with data from the MiSystem log framework.
 </p>
 * <p>
 A more convenient widget, the MiLogPanel provides in addition to the DjvLogPane a filtering panel.
 </p>
 * @author haiv
 */
public class DjvLogPane extends JScrollPane implements ClipboardOwner
{
	private static final long serialVersionUID = 1L;
	/**
	 * Interface for handling significant log pane events
	 */
	public static interface LogPaneListener {
		/**
		 * The log pane had been scrolled to the top
		 * @param topIndex Index of the top log message
		 */
		public void scrolledToTop(long topIndex);
		/**
		 * The log pane had been scrolled to the bottom
		 * @param bottomIndex Index of the bottom message
		 */
		public void scrolledToBottom(long bottomIndex);
		/**
		 * The log pane had been scrolled off the top
		 */
		public void scrolledOffTop();
		/**
		 * The log pane had been scrolled off the bottom
		 */
		public void scrolledOffBottom();
	}
	
	/**
	 * Creates a new instance of the log pane.
	 * @param title The title of the log pane.
	 * @param updateThresholdMs The threshold at which the background task that updates the log tree will
	 * wait for more messages to be buffered before actually issuing the commands to update the tree.
	 * @param maxSize Maximum size of the log tree, in number of messages (help prevent the log tree from growing indefinitely eating up all memory).
	 */
	public DjvLogPane(String title, int updateThresholdMs, int maxSize)
	{
		super();
		m_UpdateThresholdMs = updateThresholdMs;
		m_LogTree = new DjvLogTree(title, maxSize);
		m_Popup = new JPopupMenu();
		JMenuItem copy = new JMenuItem(new ImageIcon(getClass().getResource("/images/page_copy.png")));
		copy.setText(java.util.ResourceBundle.getBundle("com/mitel/guiutil/GeneralGui").getString("guiutil.CopySelectedMsgs"));
		copy.addActionListener(new AbstractAction()
		{
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e)
			{
				copySelectedNodes();
			}
		});
		m_Popup.add(copy);
		JMenuItem copyAll = new JMenuItem(new ImageIcon(getClass().getResource("/images/page_edit.png")));
		copyAll.setText(java.util.ResourceBundle.getBundle("com/mitel/guiutil/GeneralGui").getString("guiutil.SelectAllMsgs"));
		copyAll.addActionListener(new AbstractAction()
		{
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e)
			{
				m_LogTree.selectAllNodes();
			}
		});
		m_Popup.add(copyAll);
		m_LogTree.addMouseListener(new PopupListener());
		m_MaxsLogTreeSize = maxSize;
		setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		super.setViewportView(m_LogTree);
		addMouseListener(new PopupListener());
		m_LogTree.setToolTipText(java.util.ResourceBundle.getBundle("com/mitel/guiutil/GeneralGui").getString("guiutil.LogClickHint"));
		getViewport().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent ce) {
				Rectangle viewRect = m_LogTree.getVisibleRect();
				int rowCount = m_LogTree.getRowCount();
				LogPaneListener callback = getListener();
				// Note that the top row is the root and not a message
				if(rowCount > 1) {
					boolean bottomRowVisible = (viewRect.intersects(m_LogTree.getRowBounds(rowCount - 1)));
					boolean topRowVisible = (viewRect.intersects(m_LogTree.getRowBounds(1)));
					if(topRowVisible) {
						if(callback  != null) {
							if(!atTop) {
								atTop = true;
								Object top = m_LogTree.getPathForRow(1).getLastPathComponent();
								long index = -1;
								if(top instanceof DefaultMutableTreeNode) {
									Object userObj = ((DefaultMutableTreeNode)top).getUserObject();
									if(userObj instanceof DjvLogMsg) {
										index = ((DjvLogMsg)userObj).m_Index;
									}
								}
								callback.scrolledToTop(index);
							}
						}
					} else if(bottomRowVisible) {
						if(callback  != null) {
							if(!atBottom) {
								atBottom = true;
								long index = -1;
								Object bottom = m_LogTree.getPathForRow(rowCount - 1).getLastPathComponent();
								if(bottom instanceof DefaultMutableTreeNode) {
									Object userObj = ((DefaultMutableTreeNode)bottom).getUserObject();
									if(userObj instanceof DjvLogMsg) {
										index = ((DjvLogMsg)userObj).m_Index;
									}
								}
								callback.scrolledToBottom(index);
							}
						}
					} else {
						if(callback  != null) {
							if(atTop) {
								if((rowCount > 0)&&(!viewRect.intersects(m_LogTree.getRowBounds(1)))) {
									atTop = false;
									callback.scrolledOffTop();
								}
							}
							if(atBottom) {
								// For scroll hysteresis, since new logs are added to the bottom,
								// and that autoscrolling might readjust the bottom.
								boolean secondToLastRowVisible = (rowCount > 1)&&(viewRect.intersects(m_LogTree.getRowBounds(rowCount - 2)));
								if(!secondToLastRowVisible) {
									atBottom = false;
									callback.scrolledOffBottom();
								}
							}
						}
					}
				}
			}
		});
	}

	/**
	 * Specifies a listener for important scroll events
	 * @param listener The listener to be registered
	 * @return This object
	 */
	public DjvLogPane setListener(LogPaneListener listener) {
		synchronized(this){
			this.listener = listener;
		}
		return this;
	}
	
	/**
	 * Retrieves the previously registered listener for scroll events
	 * @return The previously registered listener, or null if none was registered.
	 */
	private LogPaneListener getListener() {
		synchronized(this){
			return listener;
		}
	}
	
	/**
	 * Sets the auto scroll feature. When enabled, this causes the pane to automatically
	 * scroll to the bottom of the log tree as new messages are inserted.
	 * @param value The new auto scroll value
	 * @return This object
	 */
	public DjvLogPane setAutoScroll(boolean value) {
		synchronized(this) {
			autoScroll = value;
		}
		return this;
	}
	
	/**
	 * Adds a new message to the log pane, at either the bottom or the top of the tree.
	 * Asynchronous, may be invoked by any thread.
	 * @param msg The new message.
	 * @return The same log message if added successfully, or null if the message was filtered out, and dropped.
	 */
	public DjvLogMsg addMessage(DjvLogMsg msg){
		synchronized(this){
			if(msg.m_Severity > getSeverityLevel()) {
				return null;
			}
			
			boolean addedAtBottom = true;
			if(m_LogBufferCache.isEmpty()) {
				m_LogBufferCache.add(msg);
			} else {
				if(m_LogBufferCache.getFirst().m_Index > msg.m_Index) {
					m_LogBufferCache.addFirst(msg);
					addedAtBottom = false;
				} else if(m_LogBufferCache.getLast().m_Index < msg.m_Index) {
					m_LogBufferCache.add(msg);
				} else {
					System.err.println("Discarding out of sequence message " + msg);
					return null;
				}
			}
			// 100 is the hysteresis value
			if(m_LogBufferCache.size() > m_MaxsLogTreeSize)
			{
				if(addedAtBottom) {
					m_LogBufferCache.removeLast();
				} else {
					m_LogBufferCache.removeLast();
				}
			}

			if((msg.getCategory() == DjvLogMsg.Category.MAINTENANCE) && (!m_MaintenanceLogFilter)) {
				return msg;
			}

			if((msg.getCategory() == DjvLogMsg.Category.DESIGN) && (!m_DesignLogFilter)) {
				return msg;
			}
		}
		m_LogTree.addMessage(msg, m_UpdateThresholdMs, autoScroll);
		return msg;
	}

	/**
	 * Adds a collection of messages to the log pane. Either all messages are added to the top,
	 * or are added to the bottom, no out-of-sequence allowed.
	 * Asynchronous, may be invoked by any thread.
	 * @param msgs The log messages to be added
	 */
	public void addMessages(Collection<DjvLogMsg> msgs){
		// Validate the msgs
		long idx = -1;
		for(DjvLogMsg msg : msgs) {
			if(idx < 0) {
				idx = msg.m_Index;
			} else {
				if(msg.m_Index > idx) {
					idx = msg.m_Index;
				} else {
					System.err.println("Detected out-of-sequence messages, reject request");
					return;
				}
			}
		}
		
		List<DjvLogMsg> filtered = new LinkedList<>();
		synchronized(this){
			int addMode = -1;
			int filterLevel = getSeverityLevel();
			long topIdx = m_LogBufferCache.isEmpty() ? -1 : m_LogBufferCache.getFirst().m_Index;
			long bottomIdx = m_LogBufferCache.isEmpty() ? -1 : m_LogBufferCache.getLast().m_Index;
			int count = 0; // Used for top insert
			for(DjvLogMsg msg : msgs) {
				if(msg.m_Severity > filterLevel) {
					continue;
				}
				if(m_LogBufferCache.isEmpty()) {
					// Empty cache, equivalent to add at bottom
					addMode = 0;
					m_LogBufferCache.add(msg);
					bottomIdx = msg.m_Index;
				} else if(msg.m_Index < topIdx) {
					// Insert at top
					if(addMode < 0) {
						addMode = 1;
					} else if(addMode != 1) {
						System.err.println("Encountered out-of-order messages, received top msg when in bottom add mode");
						return;
					}
					m_LogBufferCache.add(count++, msg);
				} else if(msg.m_Index > bottomIdx) {
					// Add to bottom
					if(addMode < 0) {
						addMode = 0;
					} else if(addMode != 0) {
						System.err.println("Encountered out-of-order messages, received bottom msg while in top add mode");
						return;
					}
					if(msg.m_Index > m_LogBufferCache.getLast().m_Index) {
						m_LogBufferCache.add(msg);
					} else {
						System.err.println("Encountered out-of-order messages while adding to bottom, discard " + msg);
						return;
					}
				} else {
					System.err.println("Encountered out-of-order messages, discard " + msg.m_Index + " top = " + m_LogBufferCache.getFirst().m_Index + " bottom = " + m_LogBufferCache.getLast().m_Index);
					return;
				}
				
				if(m_LogBufferCache.size() > m_MaxsLogTreeSize){
					if(addMode == 1) {
						// Top add, trim bottom
						m_LogBufferCache.removeLast();
					} else {
						// Bottom add, trim top
						m_LogBufferCache.removeFirst();
					}
				}

				if((msg.getCategory() == DjvLogMsg.Category.MAINTENANCE) && (!m_MaintenanceLogFilter)) {
					// Keep msg , but don't add to tree
					continue;
				}

				if((msg.getCategory() == DjvLogMsg.Category.DESIGN) && (!m_DesignLogFilter)) {
					// Keep msg , but don't add to tree
					continue;
				}
				
				filtered.add(msg);
			}
		}
		m_LogTree.addMessages(filtered, autoScroll);
	}

	/**
	 * Retrieves the first log in the log tree, the top of the tree, the oldest message.
	 * May be invoked from any thread.
	 * @return The first log in the tree, or null if the tree is empty.
	 */
	public DjvLogMsg getFirstMsg() {
		return m_LogTree.getTopMsg();
	}
	
	/**
	 * Retrieves the last log in the log tree, the bottom of the tree, the newest message.
	 * May be invoked from any thread.
	 * @return The last log in the tree, or null if the tree is empty.
	 */
	public DjvLogMsg getLastMsg() {
		return m_LogTree.getBottomMsg();
	}
	
	@Override
	public void setViewportView(Component view)
	{
		throw new RuntimeException("Method setViewportView() not supported for " + getClass());
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents)
	{
		// dummy
	}

	/**
	 * Refreshes the log tree display.
	 * @return This object.
	 */
	public DjvLogPane refreshLogPane() {
		synchronized(this) {
			refreshLogTree();
		}
		return this;
	}
	
	/**
	 * Enables/disables design log filter
	 * @param value True to display design log messages, false to filter them out of the view.
	 * @return This object.
	 */
	public DjvLogPane setDesignLog(boolean value)
	{
		synchronized(this)
		{
			m_DesignLogFilter = value;
			refreshLogTree();
		}
		return this;
	}

	/**
	 * Enables/disables maintenance log filter
	 * @param value True to display maintenance log messages, false to filter them out of the view.
	 * @return This object.
	 */
	public DjvLogPane setMaintenanceLog(boolean value)
	{
		synchronized(this)
		{
			m_MaintenanceLogFilter = value;
			refreshLogTree();
		}
		return this;
	}
	
	/**
	 * Sets the log severity filter, this dictates the minimum severity of messages to be displayed as described below.
	 * This only affects the display.
	 * <ul>
	 * <li>0 - Only error logs are displayed</li>
	 * <li>1 - Only error and warning logs are displayed</li>
	 * <li>2 - error, warning, and info logs are displayed</li>
	 * </ul>
	 * @param level The minimum severity to be filtered out of the display.
	 * @return This object.
	 */
	public DjvLogPane setSeverityLevel(int level){
		synchronized(this){
			severityFilter = level;
			refreshLogTree();
		}
		return this;
	}

	/**
	 * Retrieves the current log severity filter, this dictates the minimum severity of messages to be displayed as described below.
	 * This only affects the display.
	 * <ul>
	 * <li>0 - Only error logs are displayed</li>
	 * <li>1 - Only error and warning logs are displayed</li>
	 * <li>2 - error, warning, and info logs are displayed</li>
	 * </ul>
	 * @return The current log severity level.
	 */
	public int getSeverityLevel() {
		synchronized(this){
			return severityFilter;
		}
	}
	/**
	 * Retrieves the number of messages that are currently displayed in the log tree.
	 * This may be a subset of the total log messages stored in local buffer due to filtering effects.
	 * @return The number of log messages being displayed in the log tree.
	 */
	public int getNumMsgsDisplayed()
	{
		return m_LogTree.getNumMessages();
	}

	/**
	 * Retrieves the total messages currently cached in local buffer.
	 * @return The total number of log messages cached locally.
	 */
	public int getNumMsgsTotal()
	{
		synchronized(this)
		{
			return m_LogBufferCache.size();
		}
	}

	/**
	 * Refresh the log tree with the content of the log buffer.
	 * Not thread safe, caller must synchronize this object.
	 * May be invoked by any thread.
	 */
	private void refreshLogTree()
	{
		// First clear the tree of all messages ...
		m_LogTree.clearMessages();
		
		// ... then repopulate it with the content of the log buffer ...
		List<DjvLogMsg> logs = new LinkedList<>();
		synchronized(DjvLogPane.this) {
			int severity = getSeverityLevel();
			for(DjvLogMsg msg : m_LogBufferCache){
				// ... don't forget to apply any filter
				if((!m_MaintenanceLogFilter)&&(msg.getCategory() == DjvLogMsg.Category.MAINTENANCE)){
					continue;
				}
				if((!m_DesignLogFilter)&&(msg.getCategory() == DjvLogMsg.Category.DESIGN)) {
					continue;
				}
				if(msg.m_Severity > severity){
					continue;
				}

				logs.add(msg);
			}
		}
		m_LogTree.addMessages(logs, autoScroll);
	}
	
	/**
	 * Places the content of the selected nodes in the clipboard.
	 * @NotThreadSafe Must be invoked from EDT.
	 */
	private void copySelectedNodes()
	{
		StringBuilder content = new StringBuilder(4096);
		Clipboard clipBoard = Toolkit.getDefaultToolkit().getSystemClipboard();
		TreePath[] selectedRows = m_LogTree.getSelectionPaths();
		int pathCount = 0;
		
		if(selectedRows == null)
			return;
		
		for(TreePath idx : selectedRows)
		{
			Object[] elements = idx.getPath();
			int elementCount = elements.length;
			if(elementCount > 0)
			{
				if(pathCount++ > 0)
				{
					content.append("\n");
				}
				content.append(elements[elementCount - 1]);
			}
		}
		clipBoard.setContents(new StringSelection(content.toString()), this);
	}
	
	/**
	 * Scrolls the log pane view port to the bottom of the log tree.
	 * May be invoked by any thread.
	 */
	public void scrollToEnd()
	{
		m_LogTree.scrollToEnd();
	}

	/**
	 * Clears all log messages from the pane, i.e. makes the log tree empty.
	 * May be invoked by any thread.
	 */
	public void clearAllLogs()
	{
		synchronized(this)
		{
			m_LogBufferCache.clear();
			refreshLogTree();
		}
	}
	
	private final DjvLogTree m_LogTree;
	private final int m_MaxsLogTreeSize;
	private final JPopupMenu m_Popup;
	private final int m_UpdateThresholdMs;
	private LogPaneListener listener;
	private boolean m_DesignLogFilter = true;
	private boolean m_MaintenanceLogFilter = true;
	private boolean atTop;
	private boolean atBottom;
	private boolean autoScroll;
	
	/**
	 * Severity filter, default to most verbose level, 2.
	 */
	private int severityFilter = 2;

	
	/**
	 * Cache of all messages received by the log handler, up to some maximum number of messages.
	 * Oldest messages over the limit will be disposed to prevent run away memory consumption.
	 */
	private final LinkedList<DjvLogMsg> m_LogBufferCache = new LinkedList<>();
	
	/**
	 * Custom mouse listener
	 */
	private class PopupListener implements MouseListener
	{
		@Override
		public void mouseClicked(MouseEvent e)
		{
		}

		@Override
		public void mousePressed(MouseEvent e)
		{
			handlePopupMouseEvent(e);
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			handlePopupMouseEvent(e);
		}

		private void handlePopupMouseEvent(MouseEvent e)
		{
			if(e.isPopupTrigger())
			{
				int selectedRow = m_LogTree.getRowForLocation(e.getX(), e.getY());
				if((selectedRow > -1)&&(!m_LogTree.isRowSelected(selectedRow)))
				{
					m_LogTree.setSelectionRow(selectedRow);
				}
				m_Popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}

		@Override
		public void mouseEntered(MouseEvent e)
		{
		}

		@Override
		public void mouseExited(MouseEvent e)
		{
		}
	}
}
