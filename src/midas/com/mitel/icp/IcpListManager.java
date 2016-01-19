/*
 * IcpListManager.java
 *
 * Created on April 29, 2004, 9:50 AM
 */

package com.mitel.icp;

import com.mitel.guiutil.MiDoubleClickDetector;
import com.mitel.guiutil.MiGuiUtil;
import com.mitel.guiutil.MiLookAndFeel;
import com.mitel.icp.IcpConfigDialog.IcpConfigException;
import com.mitel.icp.IcpConfigDialog.IcpConfigListener;
import com.mitel.miutil.MiExceptionUtil;
import com.mitel.miutil.MiLogMsg.Category;
import com.mitel.miutil.MiSystem;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

/**
 * This dialog allows the modification of a predefined list of IcpDescriptor's.
 * Items in the list may be edited, added, and deleted.
 * @author haiv
 */
public class IcpListManager extends JDialog
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * Creates a new IcpListManager dialog and displays it (making visible).
	 * MUST be invoked from EDT.
	 * @param parent The parent frame of this dialog.
	 * @param selectedHost Name of a selected ICP, if any.
	 * @param listener Listener interface for receiving asynchronous events.
	 */
	public IcpListManager(Frame parent, String selectedHost, ListManagerListener listener)
	{
		super(parent, true);
		
		if(!SwingUtilities.isEventDispatchThread())
			throw new RuntimeException("Not invoked from EDT");
		
		initComponents();
		m_parentWindow = parent;
		m_listener = listener;
		initGui( IcpRepository.getInstance().getIcpList(), selectedHost);
		icpDoubleClickDetector = new MiDoubleClickDetector(m_icpTable, new Runnable()
		{
			@Override
			public void run()
			{
				editIcp();
			}
		}, 200);
	}
	
	/**
	 * Creates a new IcpListManager dialog and displays it (making visible).
	 * MUST be invoked from EDT.
	 * @param parent The parent dialog of this dialog.
	 * @param selectedHost Name of a selected ICP, if any.
	 * @param listener Listener interface for receiving asynchronous events.
	 */
	public IcpListManager(Dialog parent, String selectedHost, ListManagerListener listener)
	{
		super(parent, true);
		
		if(!SwingUtilities.isEventDispatchThread())
			throw new RuntimeException("Not invoked from EDT");
		
		initComponents();
		m_parentWindow = parent;
		m_listener = listener;
		initGui( IcpRepository.getInstance().getIcpList(), selectedHost);
		icpDoubleClickDetector = new MiDoubleClickDetector(m_icpTable, new Runnable()
		{
			@Override
			public void run()
			{
				editIcp();
			}
		}, 200);
	}
	
	/**
	 * Rebuilds the ICP list
	 * @param icpList The new ICP list with which to rebuild the local ICP list.
	 */
	private void updateIcpList(Collection<IcpDescriptor> icpList)
	{
		m_icpList.clear();
		for(IcpDescriptor icp : icpList)
		{
			// Make a copy so that we don't modify the original value
			m_icpList.put(icp.getName(), new IcpDescriptor(icp));
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_icpTableModel.fireTableDataChanged();
			}
		});
	}
	
	/**
	 * Initializes the ICP list manager with an initial ICP list.
	 * @param icpList The ICP list with which to populate the ICP list manager.
	 * @param selectedIcp An optional name of a selected ICP within the given list.
	 */
	private void initGui(Collection<IcpDescriptor> icpList, String selectedIcp)
	{
		m_icpTable.getSelectionModel().addListSelectionListener(this.m_IcpTableSelectionListener);
		m_icpTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		m_ExportImportFile = System.getProperty("user.home") + "/icp.csv";
		m_selectedIcpName = selectedIcp;
		
		updateIcpList(icpList);
		setSelectedIcp(m_selectedIcpName);

		MiGuiUtil.registerKeyAction(getRootPane(), KeyEvent.VK_ESCAPE, new AbstractAction() 
		{
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if(null != m_listener)
				{
					m_listener.onCancel(IcpListManager.this);
				}
			}
		});
		MiGuiUtil.registerKeyAction(getRootPane(), KeyEvent.VK_DELETE, new AbstractAction()
		{
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e)
			{
				int selectedRowView = m_icpTable.getSelectedRow();
				if(selectedRowView > -1)
				{
					deleteIcp(m_icpTable.convertRowIndexToModel(selectedRowView));
				}
			}
		});
		MiGuiUtil.registerKeyAction(getRootPane(), KeyEvent.VK_INSERT, new AbstractAction()
		{
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e)
			{
				addIcp();
			}
		});
		MiGuiUtil.registerKeyAction(getRootPane(), KeyEvent.VK_SPACE, new AbstractAction()
		{
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e)
			{
				editIcp();
			}
		});

		m_icpTableModel.fireUpdateEvent();
		pack();
		MiGuiUtil.centerAndMakeVisible(this, m_parentWindow);
	}
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        m_icpTablePopup = new javax.swing.JPopupMenu();
        m_icpTablePopupAdd = new javax.swing.JMenuItem();
        m_icpTablePopupDelete = new javax.swing.JMenuItem();
        m_icpTablePopupEdit = new javax.swing.JMenuItem();
        javax.swing.JPanel jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        javax.swing.JScrollPane m_icpTableScrollPane = new javax.swing.JScrollPane();
        m_icpTable = new javax.swing.JTable();
        javax.swing.JPanel jPanel2 = new javax.swing.JPanel();
        hostManagementOk = new javax.swing.JButton();
        hostManagementCancel = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        m_menuFile = new javax.swing.JMenu();
        m_menuFileExport = new javax.swing.JMenuItem();
        m_menuFileImport = new javax.swing.JMenuItem();
        m_menuEdit = new javax.swing.JMenu();
        m_menuEditAdd = new javax.swing.JMenuItem();
        m_menuEditDelete = new javax.swing.JMenuItem();
        m_menuEditEdit = new javax.swing.JMenuItem();

        m_icpTablePopupAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/table_add.png"))); // NOI18N
        m_icpTablePopupAdd.setText(MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("addIcpMenu"),new Object[] {"(INS)"})); // NOI18N
        m_icpTablePopupAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_icpTablePopupAddActionPerformed(evt);
            }
        });
        m_icpTablePopup.add(m_icpTablePopupAdd);

        m_icpTablePopupDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/table_delete.png"))); // NOI18N
        m_icpTablePopupDelete.setText(MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("removeIcpMenu"),new Object[] {"(DEL)"})); // NOI18N
        m_icpTablePopupDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_icpTablePopupDeleteActionPerformed(evt);
            }
        });
        m_icpTablePopup.add(m_icpTablePopupDelete);

        m_icpTablePopupEdit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/table_edit.png"))); // NOI18N
        m_icpTablePopupEdit.setText(MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("editIcpMenu"),new Object[] {"(SPACE)"})); // NOI18N
        m_icpTablePopupEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_icpTablePopupEditActionPerformed(evt);
            }
        });
        m_icpTablePopup.add(m_icpTablePopupEdit);

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement"); // NOI18N
        setTitle(bundle.getString("icpListManager")); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(new java.awt.BorderLayout(4, 4));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/lightbulb.png"))); // NOI18N
        jLabel1.setText("Right-click below to add/modify/delete ICP's");
        jPanel1.add(jLabel1);

        getContentPane().add(jPanel1, java.awt.BorderLayout.NORTH);

        m_icpTableScrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        m_icpTableScrollPane.setViewportBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        m_icpTableScrollPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                m_icpTableScrollPaneMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                m_icpTableScrollPaneMouseReleased(evt);
            }
        });

        m_icpTable.setModel(m_icpTableModel);
        m_icpTable.setToolTipText("Right-click for edit options. Press DELETE to delete an ICP.");
        m_icpTable.setRowSorter(new TableRowSorter<IcpTableModel>(m_icpTableModel));
        m_icpTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                m_icpTableMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                m_icpTableMouseReleased(evt);
            }
        });
        m_icpTableScrollPane.setViewportView(m_icpTable);

        getContentPane().add(m_icpTableScrollPane, java.awt.BorderLayout.CENTER);

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));

        java.util.ResourceBundle bundle1 = java.util.ResourceBundle.getBundle("com/mitel/guiutil/GeneralGui"); // NOI18N
        hostManagementOk.setText(bundle1.getString("okLabel")); // NOI18N
        hostManagementOk.setToolTipText("Accept the selected ICP");
        hostManagementOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hostManagementOkActionPerformed(evt);
            }
        });
        jPanel2.add(hostManagementOk);

        hostManagementCancel.setText(bundle1.getString("cancelLabel")); // NOI18N
        hostManagementCancel.setToolTipText("Dismiss this dialog without selecting anything");
        hostManagementCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hostManagementCancelActionPerformed(evt);
            }
        });
        jPanel2.add(hostManagementCancel);

        getContentPane().add(jPanel2, java.awt.BorderLayout.SOUTH);

        jMenuBar1.setRequestFocusEnabled(false);
        jMenuBar1.setVerifyInputWhenFocusTarget(false);

        m_menuFile.setText("File");
        m_menuFile.setBorderPainted(false);

        m_menuFileExport.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/database_save.png"))); // NOI18N
        m_menuFileExport.setText("Export ICP's (CSV format)");
        m_menuFileExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_menuFileExportActionPerformed(evt);
            }
        });
        m_menuFile.add(m_menuFileExport);

        m_menuFileImport.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/database_edit.png"))); // NOI18N
        m_menuFileImport.setText("Import ICP's (CSV format)");
        m_menuFileImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_menuFileImportActionPerformed(evt);
            }
        });
        m_menuFile.add(m_menuFileImport);

        jMenuBar1.add(m_menuFile);

        m_menuEdit.setText(bundle1.getString("editLabel")); // NOI18N
        m_menuEdit.setBorderPainted(false);

        m_menuEditAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/table_add.png"))); // NOI18N
        m_menuEditAdd.setText(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("addIcpMenu"),new Object[] {"(INS)"})); // NOI18N
        m_menuEditAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_menuEditAddActionPerformed(evt);
            }
        });
        m_menuEdit.add(m_menuEditAdd);

        m_menuEditDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/table_delete.png"))); // NOI18N
        m_menuEditDelete.setText(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("removeIcpMenu"),new Object[] {"(DEL)"})); // NOI18N
        m_menuEditDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_menuEditDeleteActionPerformed(evt);
            }
        });
        m_menuEdit.add(m_menuEditDelete);

        m_menuEditEdit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/table_edit.png"))); // NOI18N
        m_menuEditEdit.setText(java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("editIcpMenu"), new Object[] {"(SPACE)"})); // NOI18N
        m_menuEditEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_menuEditEditActionPerformed(evt);
            }
        });
        m_menuEdit.add(m_menuEditEdit);

        jMenuBar1.add(m_menuEdit);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void m_icpTableScrollPaneMouseReleased(java.awt.event.MouseEvent evt)//GEN-FIRST:event_m_icpTableScrollPaneMouseReleased
	{//GEN-HEADEREND:event_m_icpTableScrollPaneMouseReleased
		if(evt.isPopupTrigger())
		{
			int rowAt = m_icpTable.rowAtPoint(evt.getPoint());
			if(rowAt > -1)
			{
				m_icpTable.setRowSelectionInterval(rowAt, rowAt);
			}
			m_icpTablePopup.show(evt.getComponent(), evt.getX(), evt.getY());
		}
	}//GEN-LAST:event_m_icpTableScrollPaneMouseReleased

	private void m_icpTableScrollPaneMousePressed(java.awt.event.MouseEvent evt)//GEN-FIRST:event_m_icpTableScrollPaneMousePressed
	{//GEN-HEADEREND:event_m_icpTableScrollPaneMousePressed
		if(evt.isPopupTrigger())
		{
			int rowAt = m_icpTable.rowAtPoint(evt.getPoint());
			if(rowAt > -1)
			{
				m_icpTable.setRowSelectionInterval(rowAt, rowAt);
			}
			m_icpTablePopup.show(evt.getComponent(), evt.getX(), evt.getY());
		}
	}//GEN-LAST:event_m_icpTableScrollPaneMousePressed

	private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
	{//GEN-HEADEREND:event_formWindowClosing
		if( m_listener != null )
		{
			m_listener.onCancel(this);
		}
	}//GEN-LAST:event_formWindowClosing

	private void m_menuFileImportActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_m_menuFileImportActionPerformed
	{//GEN-HEADEREND:event_m_menuFileImportActionPerformed
		importIcpList();
	}//GEN-LAST:event_m_menuFileImportActionPerformed

	private void m_menuFileExportActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_m_menuFileExportActionPerformed
	{//GEN-HEADEREND:event_m_menuFileExportActionPerformed
		exportIcpList();
	}//GEN-LAST:event_m_menuFileExportActionPerformed

	private void m_menuEditEditActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_m_menuEditEditActionPerformed
	{//GEN-HEADEREND:event_m_menuEditEditActionPerformed
		editIcp();
	}//GEN-LAST:event_m_menuEditEditActionPerformed

	private void m_menuEditDeleteActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_m_menuEditDeleteActionPerformed
	{//GEN-HEADEREND:event_m_menuEditDeleteActionPerformed
		int selectedRow = m_icpTable.getSelectedRow();
		if(selectedRow > -1)
		{
			deleteIcp(m_icpTable.convertRowIndexToModel(selectedRow));
		}
	}//GEN-LAST:event_m_menuEditDeleteActionPerformed

	private void m_menuEditAddActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_m_menuEditAddActionPerformed
	{//GEN-HEADEREND:event_m_menuEditAddActionPerformed
		addIcp();
	}//GEN-LAST:event_m_menuEditAddActionPerformed

	private void m_icpTablePopupDeleteActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_m_icpTablePopupDeleteActionPerformed
	{//GEN-HEADEREND:event_m_icpTablePopupDeleteActionPerformed
		int selectedRow = m_icpTable.getSelectedRow();
		if(selectedRow > -1)
		{
			deleteIcp(m_icpTable.convertRowIndexToModel(selectedRow));
		}
	}//GEN-LAST:event_m_icpTablePopupDeleteActionPerformed

	private void m_icpTablePopupAddActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_m_icpTablePopupAddActionPerformed
	{//GEN-HEADEREND:event_m_icpTablePopupAddActionPerformed
		addIcp();
	}//GEN-LAST:event_m_icpTablePopupAddActionPerformed

	private void m_icpTableMouseReleased(java.awt.event.MouseEvent evt)//GEN-FIRST:event_m_icpTableMouseReleased
	{//GEN-HEADEREND:event_m_icpTableMouseReleased
		if(evt.isPopupTrigger())
		{
			int rowAt = m_icpTable.rowAtPoint(evt.getPoint());
			if(rowAt > -1)
			{
				m_icpTable.setRowSelectionInterval(rowAt, rowAt);
			}
			m_icpTablePopup.show(evt.getComponent(), evt.getX(), evt.getY());
		}
	}//GEN-LAST:event_m_icpTableMouseReleased

	private void m_icpTableMousePressed(java.awt.event.MouseEvent evt)//GEN-FIRST:event_m_icpTableMousePressed
	{//GEN-HEADEREND:event_m_icpTableMousePressed
		if(evt.isPopupTrigger())
		{
			int rowAt = m_icpTable.rowAtPoint(evt.getPoint());
			if(rowAt > -1)
			{
				m_icpTable.setRowSelectionInterval(rowAt, rowAt);
			}
			m_icpTablePopup.show(evt.getComponent(), evt.getX(), evt.getY());
		}
	}//GEN-LAST:event_m_icpTableMousePressed

	private void m_icpTablePopupEditActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_m_icpTablePopupEditActionPerformed
	{//GEN-HEADEREND:event_m_icpTablePopupEditActionPerformed
		editIcp();
	}//GEN-LAST:event_m_icpTablePopupEditActionPerformed

	private void hostManagementCancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_hostManagementCancelActionPerformed
	{//GEN-HEADEREND:event_hostManagementCancelActionPerformed
		if( m_listener != null )
		{
			m_listener.onCancel(this);
		}
	}//GEN-LAST:event_hostManagementCancelActionPerformed

	private void hostManagementOkActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_hostManagementOkActionPerformed
	{//GEN-HEADEREND:event_hostManagementOkActionPerformed
		if( m_listener != null )
		{
			IcpDescriptor selectedIcp = locateSelectedIcp();
			if(selectedIcp != null)
			{
				m_listener.onAccept(selectedIcp.getName(), this);
				return;
			}
				m_listener.onAccept("", this);
		}
	}//GEN-LAST:event_hostManagementOkActionPerformed
	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
	{
		MiLookAndFeel.setCurrentLookAndFeel(MiLookAndFeel.LAF_SUBSTANCE_CREME_COFFEE);
		SwingUtilities.invokeLater(new Runnable ()
		{
			private Frame m_Frame;
			
			public void run()
			{
				IcpService.getInstance();

				m_Frame = new JFrame();
				new IcpListManager(m_Frame, "Host8" /*Selected Host*/, new ListManagerListener()
				{
					public void onAccept(String selectedIcpName, JDialog thisDialog)
					{
						System.out.println("Accepted");
						thisDialog.dispose();
						m_Frame.dispose();
					}

					public void onCancel(JDialog thisDialog)
					{
						System.out.println("Cancelled");
						thisDialog.dispose();
						m_Frame.dispose();
					}
				}).setVisible(true);
			}
		});
	}
	
	/**
	 * Highlights the specified ICP name in the ICP table.
	 * @param selectedHost The name of the selected ICP.
	 */
	private void setSelectedIcp(String selectedHost)
	{
		m_selectedIcpName = selectedHost;

		// Set the selected row to reflect the selected ICP name
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				int row = 0;
				Object[] icpArray = m_icpList.values().toArray();
				if(m_selectedIcpName != null)
				{
					// Locate the selected ICP
					for(int i =0; (i < icpArray.length); ++i)
					{
						IcpDescriptor icp = (IcpDescriptor)icpArray[i];
						if(null != m_selectedIcpName)
						{
							if(icp.getName().equals(m_selectedIcpName))
							{
								// Found the selected ICP in the list
								row = i;
								break;
							}
						}
						else
						{
							// No selected ICP givem, grab the first one available
							m_selectedIcpName = icp.getName();
							break;
						}
					}
				}

				m_icpTable.clearSelection();
				if(icpArray.length > 0)
				{
					m_icpTable.setRowSelectionInterval(row,row);
					m_icpTable.scrollRectToVisible(m_icpTable.getCellRect(row, 0, true));
				}				
			}
		});
	}
	
	private IcpDescriptor locateSelectedIcp()
	{
		int selectedRowView = m_icpTable.getSelectedRow();
		if(selectedRowView > -1)
		{
			return locateSelectedIcp(m_icpTable.convertRowIndexToModel(selectedRowView));
		} 
		return null;
	}
	
	private IcpDescriptor locateSelectedIcp(int row)
	{
		if(-1 < row)
		{
			Object[] icps = m_icpList.values().toArray();
			if(icps.length > row)
			{
				return (IcpDescriptor)icps[row];
			}
		}
		return null;
	}
	
	/**
	 * Deletes the selected ICP. Prompts the user first though.
	 * @param row The row that identifies the ICP to be deleted.
	 */
	private void deleteIcp(int row)
	{
		try
		{
			IcpDescriptor selectedIcp = locateSelectedIcp(row);
			if(selectedIcp != null)
			{
				String message = ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("icpDeletionWarning");
				Object[] args = new Object[1];
				args[0] = selectedIcp.getName();
				if(JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(IcpListManager.this, MessageFormat.format(message,args), ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("removeIcpLabel"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE))
				{
					// Remove the selected ICP from the repository
					IcpRepository.getInstance().removeIcp(selectedIcp.getName());

					// Rebuild the local ICP list
					updateIcpList(IcpRepository.getInstance().getIcpList());

					// Readjust the highted ICP in the table.
					int icpListSize = m_icpList.size();
					if((icpListSize > 0)&&(row == icpListSize))
					{
						// The deleted row was the last row, move the select index to the next higher row
						IcpDescriptor icp = locateSelectedIcp(row - 1);
						if(icp != null) {
							setSelectedIcp(icp.getName());
						}
					}
					else if(icpListSize > 0)
					{
						IcpDescriptor icp = locateSelectedIcp(row);
						if(icp != null) {
							setSelectedIcp(icp.getName());
						}
					}
				}		
			}
		}
		catch(IcpException e)
		{
			JOptionPane.showMessageDialog(this, "Deletion Failed due to " + e.getMessage(), "ICP Deletion", JOptionPane.WARNING_MESSAGE);
		}
	}

	/**
	 * Retrieves the list of artifacts required to display the ICP list manager help content in a browser.
	 * @return The list of help content artifacts, non-null;
	 */
	@SuppressWarnings("ReturnOfCollectionOrArrayField")
	public static MiSystem.ArtifactDesc[] getHelpContent()
	{
		return gIcpListMgrHelpContent;
	}

	/**
	 * Launches an edit ICP dialog with a brand new ICP record.
	 */
	private void addIcp()
	{
		try
		{
			String newIcpName = ("ICP"+m_newIcpNameCounter++);
			MiGuiUtil.centerAndMakeVisible(new IcpConfigDialog(this,
				new IcpDescriptor(newIcpName, IcpProperty.getDefaultMiNetVersion(),
				"###", "RtcAddress", "E2tAddress", "ICP3300"), m_icpAddListener), this);
		}
		catch(IcpConfigException ex)
		{
			JOptionPane.showMessageDialog(this, "Addition failed due to " + ex.getMessage(), "ICP Addition", JOptionPane.WARNING_MESSAGE);
		}
		catch(RuntimeException e)
		{
			MiSystem.logError(Category.DESIGN,
				MiExceptionUtil.simpleTrace(e));
		}
	}
	
	/**
	 * Launches the edit ICP dialog targeting the selected ICP in the list.
	 */
	private void editIcp()
	{
		if(null != locateSelectedIcp())
		{
			try
			{
				MiGuiUtil.centerAndMakeVisible(new IcpConfigDialog(this,
					locateSelectedIcp(), m_icpAddListener), this);
			}
			catch( IcpConfigException e )
			{
				JOptionPane.showMessageDialog(this, "Edit failed due to " + e.getMessage(), "ICP Edit", JOptionPane.WARNING_MESSAGE);
			}
		}
	}
	
	/**
	 * Interface for receiving asynchronous events from the ICP list manager dialog.
	 */
	public interface ListManagerListener
	{
		
		/**
		 * The OK button was pressed on the ICP list manager dialog.
		 * Invoked by the EDT.
		 * @param selectedIcpName The name of the selected ICP in the list if any.
		 * @param thisDialog The ICP list manager dialog from which this event originated.
		 */
		public abstract void onAccept(String selectedIcpName, JDialog thisDialog);
		
		/**
		 * The Cancel button was pressed on the ICP list manager dialog.
		 * Invoked by the EDT.
		 * @param thisDialog The ICP list manager dialog from which this event originated.
		 */
		public abstract void onCancel(JDialog thisDialog);
		
	}
	
	private class icpAddConfigListener implements IcpConfigListener
	{
		@Override
		public void onAccept(IcpDescriptor desc, JDialog thisDialog)
		{
			thisDialog.dispose();
			IcpRepository.getInstance().setIcp(desc);
			updateIcpList(IcpRepository.getInstance().getIcpList());
			setSelectedIcp(desc.getName());

			// Make the latest MiNET version setting the default.
			IcpProperty.setDefaultMiNetVersion(desc.getMiNetVersion());
		}

		@Override
		public void onCancel(JDialog thisDialog)
		{
			thisDialog.dispose();
		}
	}
	
	private class IcpTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;

		private IcpTableModel()
		{
			super();
		}
		
		@Override
		public int getColumnCount()
		{
			return 6;
		}
		
		@Override
		public String getColumnName(int col)
		{
			switch(col)
			{
				case 0:
					return ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("nameLabel");
					
				case 1:
					return ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("hostAddressLabel");
					
				case 2:
					return ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("e2tAddressLabel");
					
				case 3:
					return ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("icpTypeLabel");
					
				case 4:
					return ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("registrationCodeLabel");
					
				case 5:
					return ResourceBundle.getBundle("com/mitel/icp/IcpManagement").getString("minetVersionLabel");
					
				default:
					return "UNDEFINED";
			}
		}
		
		@Override
		public int getRowCount()
		{
			return m_icpList.size();
		}
		
		@Override
		public Object getValueAt(int row, int col)
		{
			Object[] icpArray = m_icpList.values().toArray();
			if((null != icpArray)&&(row < icpArray.length))
			{
				IcpDescriptor icp = (IcpDescriptor)icpArray[row];
				switch(col)
				{
					case 0:
						return icp.getName();

					case 1:
					{
						String addr = icp.getAddress(IcpProcessor.ProcessorName.RTC);
						if(null != addr)
							return addr;
						
						return "";
					}

					case 2:
					{
						String addr = icp.getAddress(IcpProcessor.ProcessorName.E2T);
						if(null != addr)
							return addr;
						
						return "";
					}

					case 3:
						if(null != icp.getType())
							return icp.getType();
						
						return "null";

					case 4:
						return icp.getRegistrationCode();

					case 5:
						if(null != icp.getMiNetVersion())
							return icp.getMiNetVersion();
						
						return "null";
						
					default:
						break;
				}
			}
			return "";
		}
		
		@Override
		public Class getColumnClass(int col)
		{
			return getValueAt(0, col).getClass();
		}
		
		@Override
		public boolean isCellEditable(int row, int col)
		{
			if((col == 4)||(col == 5)) // Reg code and MiNET are editable	
				return true;
			
			return false;
		}
		
		public void clear()
		{
			if(getRowCount() > 0)
			{
				fireTableRowsDeleted(0, getRowCount() - 1);
			}
		}
		
		public void fireUpdateEvent()
		{
			this.fireTableDataChanged();
		}
		
		public int locateRowUsingIcpName(java.lang.String icpName)
		{
			Object[] icpArray = m_icpList.values().toArray();
			for(int i = 0; i < icpArray.length; ++i)
			{
				IcpDescriptor icp = (IcpDescriptor)icpArray[i];
				if(icp.getName().equals(icpName))
					return i;
			}
			return -1;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
			super.setValueAt(aValue, rowIndex, columnIndex);
			if(columnIndex == 4)
			{
				Object[] icpArray = m_icpList.values().toArray();
				if(rowIndex < icpArray.length)
				{
					IcpDescriptor icp = (IcpDescriptor)icpArray[rowIndex];
					icp.setRegistrationCode((String)aValue);
					IcpRepository.getInstance().setIcp(icp);
				}
			}
			else if(columnIndex == 5)
			{
				Object[] icpArray = m_icpList.values().toArray();
				if(rowIndex < icpArray.length)
				{
					IcpDescriptor icp = (IcpDescriptor)icpArray[rowIndex];
					icp.setMiNetVersion((String)aValue);
					IcpRepository.getInstance().setIcp(icp);
				}
			}
		}
		
	}
	
	public class IcpTableSelectionListener implements ListSelectionListener
	{
		@Override
		public void valueChanged(ListSelectionEvent evt)
		{
			if(!evt.getValueIsAdjusting())
			{
				IcpDescriptor icp = locateSelectedIcp();
				if(null != icp)
				{
					m_selectedIcpName = icp.getName();
				}
			}
		}
		
	}
	
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton hostManagementCancel;
    private javax.swing.JButton hostManagementOk;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JTable m_icpTable;
    private javax.swing.JPopupMenu m_icpTablePopup;
    private javax.swing.JMenuItem m_icpTablePopupAdd;
    private javax.swing.JMenuItem m_icpTablePopupDelete;
    private javax.swing.JMenuItem m_icpTablePopupEdit;
    private javax.swing.JMenu m_menuEdit;
    private javax.swing.JMenuItem m_menuEditAdd;
    private javax.swing.JMenuItem m_menuEditDelete;
    private javax.swing.JMenuItem m_menuEditEdit;
    private javax.swing.JMenu m_menuFile;
    private javax.swing.JMenuItem m_menuFileExport;
    private javax.swing.JMenuItem m_menuFileImport;
    // End of variables declaration//GEN-END:variables

	private Map<String, IcpDescriptor> m_icpList = new TreeMap<String, IcpDescriptor>();	
	
	private final ListManagerListener m_listener;
	
	private final Window m_parentWindow;
	
	private icpAddConfigListener m_icpAddListener = new icpAddConfigListener();
	
	private final IcpTableModel m_icpTableModel = new IcpTableModel();

	private final MiDoubleClickDetector icpDoubleClickDetector;
	
	private String m_selectedIcpName;

	private static final MiSystem.ArtifactDesc[] gIcpListMgrHelpContent = new MiSystem.ArtifactDesc[]
	{
		new MiSystem.ArtifactDesc(IcpListManager.class.getClassLoader(), "ICPListManagerHelp.html", MiSystem.ArtifactDesc.Type.TEXT, false),
		new MiSystem.ArtifactDesc(IcpListManager.class.getClassLoader(), "images/icpconfig.gif", MiSystem.ArtifactDesc.Type.BINARY, false),
		new MiSystem.ArtifactDesc(IcpListManager.class.getClassLoader(), "images/icplistmgrexport.gif", MiSystem.ArtifactDesc.Type.BINARY, false)
	};

	private final IcpTableSelectionListener m_IcpTableSelectionListener = new IcpTableSelectionListener();;
	
	private static int m_newIcpNameCounter = 0;

    private void exportIcpList() 
	{
		JFileChooser fc;
		if(null != m_ExportImportFile)
		{
			fc = new JFileChooser();
			fc.setSelectedFile(new File(m_ExportImportFile));
		}
		else
		{
			fc = new JFileChooser();
		}
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc. setDialogTitle("Specify ICP Export File");
        if( JFileChooser.APPROVE_OPTION == fc.showOpenDialog(this))
        {
			File selectedFile = fc.getSelectedFile();
			m_ExportImportFile = selectedFile.getPath();
			FileWriter writer = null;
			try
			{
				writer = new FileWriter(selectedFile);
				Iterator iter = m_icpList.values().iterator();
				
				// Write the heading
				writer.write("ICP Name,ICP Type,RegCode,RTC,E2T,Lite\n");
				while(iter.hasNext())
				{
					writer.write(((IcpDescriptor)iter.next()).serialized());
					writer.write("\n");
				}
			}
			catch(java.io.IOException e)
			{
				MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
			}
			finally
			{
				if( null != writer)
				{
					try
					{
						writer.close();
					}catch(Exception e2){}
				}
			}
        }
    }

	private IcpDescriptor parseLine(String line)
	{
		if(line != null)
		{
			try
			{
				return IcpDescriptor.deserialized(line);
			}
			catch(IcpException e)
			{
				MiSystem.logInfo(Category.DESIGN, e.getMessage());
			}
		}
		return null;
	}
	
	/**
	 * Initiates the whole process of importing the ICP list from some external file
	 * (user is prompted to select the desired file).
	 * Invoked from the EDT.
	 */
	@SuppressWarnings("NestedAssignment")
    private void importIcpList() 
	{
		JFileChooser fc;
		if(null != m_ExportImportFile)
		{
			fc = new JFileChooser();
			fc.setSelectedFile(new File(m_ExportImportFile));
		}
		else
		{
			fc = new JFileChooser();
		}
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc. setDialogTitle("Specify ICP Import File");
        if( JFileChooser.APPROVE_OPTION == fc.showOpenDialog(this))
        {
			// Clear existing list
			m_icpList.clear();
			
			File selectedFile = fc.getSelectedFile();
			m_ExportImportFile = selectedFile.getPath();
			BufferedReader reader = null;
			try
			{
				reader = new BufferedReader(new FileReader(selectedFile));
				String line;
				String selectedIcp = null;
				while(null != (line = reader.readLine()))
				{
					IcpDescriptor newIcp = parseLine(line);
					if(null != newIcp) {
						IcpRepository.getInstance().setIcp(newIcp);
						if(selectedIcp == null) {
							selectedIcp = newIcp.getName();
						}
					}
				}
				updateIcpList(IcpRepository.getInstance().getIcpList());
				setSelectedIcp(selectedIcp);
			}
			catch(java.io.IOException e)
			{
				MiGuiUtil.showMessageDialog(this, "Failed to import ICP list due to " + e 
					+ ", check log for more details.", "ICP List Import", JOptionPane.WARNING_MESSAGE);
				
				MiSystem.logWarning(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
			}
			catch(RuntimeException e)
			{
				MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
			}
			finally
			{
				
				if(null != reader)
				{
					try
					{
						reader.close();
					}catch(Exception e2){}
				}
			}
        }
    }

	private String m_ExportImportFile;
}
