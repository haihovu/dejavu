/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * MiLogPanel.java
 *
 * Created on Jan 28, 2010, 12:47:42 PM
 */

package com.mitel.guiutil;

import com.mitel.miutil.MiBackgroundTask;
import com.mitel.miutil.MiExceptionUtil;
import com.mitel.miutil.MiLogHandler;
import com.mitel.miutil.MiLogMsg;
import com.mitel.miutil.MiSystem;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.LinkedList;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * A log level panel with filtering user interface. This wraps around the MiLogPane
 * widget and adds a filter panel.
 * @author haiv
 */
public class MiLogPanel extends javax.swing.JPanel
{
	private static final long serialVersionUID = 1L;

	/**
	 * Background task for updating the number of messages being displayed.
	 */
	private class BgTaskNumMsgsUpdater extends MiBackgroundTask
	{
		private final int lWaitMs;
		/**
		 * Creates a new background task.
		 * @param waitMs Time to wait before updating the affected field, for efficiency purpose,
		 * in case a large number of log messages are coming at once. Zero or less means no wait.
		 */
		private BgTaskNumMsgsUpdater(int waitMs)
		{
			super();
			lWaitMs = waitMs;
		}

		@Override
		public void run()
		{
			synchronized(MiLogPanel.this)
			{
				try
				{
					if(lWaitMs > 0)
					{
						MiLogPanel.this.wait(lWaitMs);
					}
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							logFilterNumMsgs.setText(m_LogPane.getNumMsgsDisplayed() + "/" + m_LogPane.getNumMsgsTotal());
						}
					});
				}
				catch(InterruptedException ex)
				{
				}
				finally
				{
					if(m_Updater == this)
					{
						m_Updater = null;
					}
				}
			}
		}
	}

	/**
	 * Custom log handler for this log panel. This simply wraps around a <i>real</i>
	 * log handler and provide some display function.
	 */
	private class LocalLogHandler implements MiLogHandler{
		/**
		 * Creates a new log handler instance.
		 */
		private LocalLogHandler(){
			super();
		}

		@Override
		public MiLogMsg logMsg(MiLogMsg.Category category, int severity, Class origClass, String origMethod, String message){
			MiLogMsg msg = new MiLogMsg(category, severity, origClass, origMethod, message);
			m_LogPane.addMessage(msg);
			synchronized(MiLogPanel.this)
			{
				if(m_Updater == null)
				{
					m_Updater = new BgTaskNumMsgsUpdater(m_UpdateThresholdMs + 1000).start();
				}
			}
			return msg;
		}

		@Override
		public MiLogMsg logMsg(MiLogMsg msg) {
			m_LogPane.addMessage(msg);
			synchronized(MiLogPanel.this)
			{
				if(m_Updater == null)
				{
					m_Updater = new BgTaskNumMsgsUpdater(m_UpdateThresholdMs + 1000).start();
				}
			}
			return msg;
		}
		
		/**
		 * Support adding a collection of messages
		 * @param msgs The messages to be added
		 */
		private void logMsgs(Collection<MiLogMsg> msgs) {
			m_LogPane.addMessages(msgs);
			synchronized(MiLogPanel.this){
				if(m_Updater == null){
					m_Updater = new BgTaskNumMsgsUpdater(m_UpdateThresholdMs + 1000).start();
				}
			}
		}
		
		@Override
		public void setLogFilterLevel(int logLevel)
		{
			m_LogPane.setSeverityLevel(logLevel);
		}

		@Override
		public int getLogFilterLevel()
		{
			return m_LogPane.getSeverityLevel();
		}

		@Override
		public void start() {
		}

		@Override
		public void stop() {
		}
	}
	private final MiLogPane m_LogPane;
	private final String m_Title;
	private final LocalLogHandler m_LogHandler;
	private final int m_UpdateThresholdMs;
	private MiBackgroundTask m_Updater;
	
	/** Creates new form MiLogPanel
	 * @param title The title of the log list.
	 * @param updateThresholdMs The log display is only updated at this threshold,
	 * this is to prevent large amount of log messages in short period of time from generating too many display update requests.
	 * @param maxLogMsgs Maximum number of messages that is kept in the display, this is to prevent memory exhaustion.
	 * @param designLog Initial design log filter flag
	 * @param maintLog Initial maintenance log filter flag
	 * @param severity Initial severity filter value.
	 */
    public MiLogPanel(String title, int updateThresholdMs, int maxLogMsgs, boolean designLog, boolean maintLog, int severity)
	{
		m_Title = title;
		m_UpdateThresholdMs = updateThresholdMs;
		m_LogPane = new MiLogPane(title, updateThresholdMs, maxLogMsgs).setDesignLog(designLog).setMaintenanceLog(maintLog).setSeverityLevel(severity);
		m_LogHandler = new LocalLogHandler();
        initComponents();
		initialize(designLog, maintLog, severity);
    }
	
	/**
	 * Retrieves the first log in the log tree, the top of the tree, the oldest message.
	 * May be invoked from any thread.
	 * @return The first log in the tree, or null if the tree is empty. 
	 */
	public MiLogMsg getFirstMsg(){
		return m_LogPane.getFirstMsg();
	}
	
	/**
	 * Retrieves the last log in the log tree, the bottom of the tree, the newest message.
	 * May be invoked from any thread.
	 * @return The last log in the tree, or null if the tree is empty. 
	 */
	public MiLogMsg getLastMsg(){
		return m_LogPane.getLastMsg();
	}
	
	/**
	 * Specifies a listener for significant log scrolling events.
	 * @param listener The listener to be registered
	 * @return This object
	 */
	public MiLogPanel registerListener(MiLogPane.LogPaneListener listener){
		m_LogPane.setListener(listener);
		return this;
	}
	
	/**
	 * Common initialize logic
	 * @param designLog Initial design log filter flag
	 * @param maintLog Initial maintenance log filter flag
	 * @param severity Initial severity filter value.
	 */
	private void initialize(boolean designLog, boolean maintLog, int severity) {
		logFilterDesign.setSelected(designLog);
		logFilterMaintenance.setSelected(maintLog);
		if((severity > -1)&&(severity < 3))
		{
			logFilterSeverity.setSelectedIndex(severity);
		}
		add(m_LogPane, BorderLayout.CENTER);
	}
	
	/**
	 * Retrieves the log handler. This is typically used to feed into MiSystem.registerLogHandler().
	 * @return The log handler.
	 */
	public MiLogHandler getLogHandler()
	{
		return m_LogHandler;
	}

	/**
	 * Adds a new message to the log tree (depending on the index of the message,
	 * it may be added to the top or bottom of the tree).
	 * Asynchronous, may be invoked by any thread.
	 * @param msg The new message to add.
	 * @return This object.
	 */
	public MiLogPanel addMessage(MiLogMsg msg) {
		this.m_LogHandler.logMsg(msg);
		return this;
	}
	
	/**
	 * Adds a collection of new messages to the log tree (depending on the index of the messages,
	 * they may be added to the top or bottom of the tree).
	 * Asynchronous, may be invoked by any thread.
	 * @param msgs The new messages to add.
	 * @return This object.
	 */
	public MiLogPanel addMessages(Collection<MiLogMsg> msgs) {
		m_LogHandler.logMsgs(msgs);
		return this;
	}
	
	/**
	 * Clears all log messages from the log tree.
	 * @return This object
	 */
	public MiLogPanel clearMessages() {
		m_LogPane.clearAllLogs();
		return this;
	}
	
	/**
	 * Refreshes the log tree display.
	 * @return This object.
	 */
	public MiLogPanel refreshLogPane() {
		m_LogPane.refreshLogPane();
		return this;
	}
	
	/**
	 * Test program
	 * @param args 
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final LinkedList<MiLogMsg> logs = new LinkedList<>();
				final JFrame frame = new JFrame("Test");
				final MiLogPanel logpanel = new MiLogPanel("Logs", 1000, 1024, true, true, 2);
				logpanel.registerListener(new MiLogPane.LogPaneListener() {
					@Override
					public void scrolledToTop(final long topIndex) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								MiSystem.logInfo(MiLogMsg.Category.DESIGN, "Scrolled to the top mate: " + topIndex);
								if(topIndex > 0) {
									try {
										int backIndex = (int)topIndex - 25;
										if(backIndex < 0) {
											backIndex = 0;
										}
										Collection<MiLogMsg> olderLogs = new LinkedList<>();
										synchronized(logs) {
											for(int i = backIndex; i < topIndex; ++i) {
												olderLogs.add(logs.get(i));
											}
										}
										logpanel.addMessages(olderLogs);
									} catch(RuntimeException e) {
										MiSystem.logError(MiLogMsg.Category.DESIGN, MiExceptionUtil.simpleTrace(e));
									}
								}
							}
						}).start();
					}

					@Override
					public void scrolledToBottom(final long bottomIndex) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								MiSystem.logInfo(MiLogMsg.Category.DESIGN, "Scrolled to the bottom mate: " + bottomIndex);
								try {
									synchronized(logs) {
										int logSize = logs.size();
										if(bottomIndex < (logSize - 1)) {
											Collection<MiLogMsg> newerLogs = new LinkedList<>();
											int count = 0;
											for(int i = (int)bottomIndex + 1; (i < logSize)&&(++count < 25); ++i) {
												newerLogs.add(logs.get(i));
											}
											logpanel.addMessages(newerLogs);
										}
									}
								} catch(RuntimeException e){
									MiSystem.logError(MiLogMsg.Category.DESIGN, MiExceptionUtil.simpleTrace(e));
								}
							}
						}).start();
					}

					@Override
					public void scrolledOffTop() {
						MiSystem.logInfo(MiLogMsg.Category.DESIGN, "Scrolled off the top mate");
					}

					@Override
					public void scrolledOffBottom() {
						MiSystem.logInfo(MiLogMsg.Category.DESIGN, "Scrolled off the bottom mate");
					}
				});
				logpanel.setPreferredSize(new Dimension(600, 400));
				JPanel logGenPanel = new JPanel(new FlowLayout());
				logGenPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
				JButton logGen = new JButton(new AbstractAction("Log gen") {
					private static final long serialVersionUID = 1L;
					@Override
					public void actionPerformed(ActionEvent ae) {
						synchronized(logs){
							for(int i = 0; i < 100; ++i){
								MiLogMsg log = new MiLogMsg(MiLogMsg.Category.DESIGN, 2, MiLogPanel.class, "main", "New log " + System.currentTimeMillis());
								logs.add(log);
								logpanel.addMessage(log);
							}
						}
					}
				});
				logGenPanel.add(logGen);
				frame.setLayout(new BorderLayout());
				frame.add(logGenPanel, BorderLayout.NORTH);
				frame.add(logpanel, BorderLayout.CENTER);
				frame.pack();
				MiSystem.setLogLevel(2);
				synchronized(logs){
					int logSize = 2048;
					for(int i = 0; i < logSize; ++i) {
						MiLogMsg.Category cat = (i % 2 == 0) ? MiLogMsg.Category.DESIGN : MiLogMsg.Category.MAINTENANCE;
						MiLogMsg log;
						switch(i % 3) {
							case 0:
								log = new MiLogMsg(cat, 0, MiLogPanel.class, "main", "Test log # " + i);
								break;

							case 1:
								log = new MiLogMsg(cat, 1, MiLogPanel.class, "main", "Test log # " + i);
								break;

							default:
								log = new MiLogMsg(cat, 2, MiLogPanel.class, "main", "Test log # " + i);
								break;
						}
						logs.add(log);
						logpanel.addMessage(log);
					}
				}
				frame.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent we) {
						super.windowClosing(we);
						frame.dispose();
					}
					
				});
				frame.setVisible(true);
			}
		});
	}
	
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        filterPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        logFilterDesign = new javax.swing.JCheckBox();
        logFilterMaintenance = new javax.swing.JCheckBox();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        logFilterSeverity = new javax.swing.JComboBox();
        autoScroll = new javax.swing.JToggleButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        logFilterNumMsgs = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createTitledBorder(m_Title), javax.swing.BorderFactory.createEmptyBorder(0, 10, 10, 10)));
        setLayout(new java.awt.BorderLayout());

        filterPanel.setLayout(new java.awt.BorderLayout());

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/mitel/guiutil/GeneralGui"); // NOI18N
        jLabel1.setText(bundle.getString("log.Filter")); // NOI18N
        jPanel1.add(jLabel1);

        logFilterDesign.setText(bundle.getString("log.design")); // NOI18N
        logFilterDesign.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logFilterDesignActionPerformed(evt);
            }
        });
        jPanel1.add(logFilterDesign);

        logFilterMaintenance.setText(bundle.getString("log.maintenance")); // NOI18N
        logFilterMaintenance.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logFilterMaintenanceActionPerformed(evt);
            }
        });
        jPanel1.add(logFilterMaintenance);

        jLabel2.setText(bundle.getString("log.severity")); // NOI18N
        jPanel1.add(jLabel2);

        logFilterSeverity.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Error", "Error|Warning", "Error|Warning|Info" }));
        logFilterSeverity.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logFilterSeverityActionPerformed(evt);
            }
        });
        jPanel1.add(logFilterSeverity);

        autoScroll.setText("Auto-scroll");
        autoScroll.setToolTipText("Auto scroll to bottom of the log tree");
        autoScroll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoScrollActionPerformed(evt);
            }
        });
        jPanel1.add(autoScroll);

        filterPanel.add(jPanel1, java.awt.BorderLayout.CENTER);

        jLabel3.setText(bundle.getString("log.numberMsgs")); // NOI18N
        jPanel2.add(jLabel3);

        logFilterNumMsgs.setText("0");
        jPanel2.add(logFilterNumMsgs);

        filterPanel.add(jPanel2, java.awt.BorderLayout.SOUTH);

        add(filterPanel, java.awt.BorderLayout.NORTH);
    }// </editor-fold>//GEN-END:initComponents

	private void logFilterDesignActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_logFilterDesignActionPerformed
	{//GEN-HEADEREND:event_logFilterDesignActionPerformed
		m_LogPane.setDesignLog(logFilterDesign.isSelected());
		synchronized(this)
		{
			if(m_Updater == null)
			{
				m_Updater = new BgTaskNumMsgsUpdater(0).start();
			}
		}
	}//GEN-LAST:event_logFilterDesignActionPerformed

	private void logFilterMaintenanceActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_logFilterMaintenanceActionPerformed
	{//GEN-HEADEREND:event_logFilterMaintenanceActionPerformed
		m_LogPane.setMaintenanceLog(logFilterMaintenance.isSelected());
		synchronized(this)
		{
			if(m_Updater == null)
			{
				m_Updater = new BgTaskNumMsgsUpdater(0).start();
			}
		}
	}//GEN-LAST:event_logFilterMaintenanceActionPerformed

	private void logFilterSeverityActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_logFilterSeverityActionPerformed
	{//GEN-HEADEREND:event_logFilterSeverityActionPerformed
		m_LogPane.setSeverityLevel(logFilterSeverity.getSelectedIndex());
		synchronized(this)
		{
			if(m_Updater == null)
			{
				m_Updater = new BgTaskNumMsgsUpdater(0).start();
			}
		}
	}//GEN-LAST:event_logFilterSeverityActionPerformed

    private void autoScrollActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoScrollActionPerformed
        m_LogPane.setAutoScroll(autoScroll.isSelected());
    }//GEN-LAST:event_autoScrollActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton autoScroll;
    private javax.swing.JPanel filterPanel;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JCheckBox logFilterDesign;
    private javax.swing.JCheckBox logFilterMaintenance;
    private javax.swing.JLabel logFilterNumMsgs;
    private javax.swing.JComboBox logFilterSeverity;
    // End of variables declaration//GEN-END:variables

}
