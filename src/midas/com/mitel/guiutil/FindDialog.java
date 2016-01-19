/*
 * FindDialog.java
 *
 * Created on May 26, 2004, 10:29 PM
 */

package com.mitel.guiutil;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Find Dialog allowing user to enter regular expression.
 * @author Hai Vu
 */
public class FindDialog extends JDialog
{
	private static final long serialVersionUID = 1L;
	
	/** Creates new form FindDialog
	 * @param parent
	 * @param predefinedPatterns
	 * @param caseInsensitive
	 * @param listener
	 */
	public FindDialog(Frame parent, List<String> predefinedPatterns, boolean caseInsensitive, ActionListener listener)
	{
		super(parent, true);
		m_caseInsensitive = caseInsensitive;
		initComponents();

		ActionListener[] listeners = findDialogRegExp.getActionListeners();
		for(ActionListener l : listeners)
		{
			findDialogRegExp.removeActionListener(l);
		}
		// Initialize regular expression list with that passed in
		if(null != predefinedPatterns)
		{
			Iterator iter = predefinedPatterns.iterator();
			while(iter.hasNext())
			{
				findDialogRegExp.addItem(iter.next());
			}
		}
		for(ActionListener l : listeners)
		{
			findDialogRegExp.addActionListener(l);
		}
		
		m_listener = listener;
		m_savedRegExps = predefinedPatterns;
		
		MiGuiUtil.registerKeyAction(getRootPane(), KeyEvent.VK_ESCAPE, new AbstractAction()
		{
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e)
			{
				fireCancelEvent();
			}
		});
		MiGuiUtil.registerKeyAction(getRootPane(), KeyEvent.VK_ENTER, new AbstractAction()
		{
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e)
			{
				fireAcceptEvent();
			}
		});
	}
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        final javax.swing.JPanel findDialogMain = new javax.swing.JPanel();
        final javax.swing.JLabel jLabel5 = new javax.swing.JLabel();
        findDialogRegExp = new com.mitel.guiutil.MiComboBox();
        findDialogCaseSensitivity = new javax.swing.JCheckBox();
        final javax.swing.JPanel findDialogButtons = new javax.swing.JPanel();
        findDialogOk = new javax.swing.JButton();
        findDialogCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/mitel/guiutil/GeneralGui"); // NOI18N
        setTitle(bundle.getString("findTitleLabel")); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        findDialogMain.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        findDialogMain.setLayout(new java.awt.GridBagLayout());

        jLabel5.setText(bundle.getString("enterRegExpLabel")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.3;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        findDialogMain.add(jLabel5, gridBagConstraints);

        findDialogRegExp.setEditable(true);
        findDialogRegExp.setMaximumRowCount(10);
        findDialogRegExp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findDialogRegExpActionPerformed(evt);
            }
        });
        findDialogRegExp.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                findDialogRegExpFocusGained(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.8;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        findDialogMain.add(findDialogRegExp, gridBagConstraints);

        if(m_caseInsensitive)
        findDialogCaseSensitivity.setSelected(true);
        else
        findDialogCaseSensitivity.setSelected(false);
        findDialogCaseSensitivity.setText(bundle.getString("caseSensitiveSearchLabel")); // NOI18N
        findDialogCaseSensitivity.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                findDialogCaseSensitivityStateChanged(evt);
            }
        });
        findDialogCaseSensitivity.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findDialogCaseSensitivityActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        findDialogMain.add(findDialogCaseSensitivity, gridBagConstraints);

        getContentPane().add(findDialogMain, java.awt.BorderLayout.CENTER);

        findDialogButtons.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));

        findDialogOk.setText(bundle.getString("okLabel")); // NOI18N
        findDialogOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findDialogOkActionPerformed(evt);
            }
        });
        findDialogButtons.add(findDialogOk);

        findDialogCancel.setText(bundle.getString("cancelLabel")); // NOI18N
        findDialogCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findDialogCancelActionPerformed(evt);
            }
        });
        findDialogButtons.add(findDialogCancel);

        getContentPane().add(findDialogButtons, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void findDialogRegExpFocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_findDialogRegExpFocusGained
	{//GEN-HEADEREND:event_findDialogRegExpFocusGained
		findDialogRegExp.getEditor().selectAll();
	}//GEN-LAST:event_findDialogRegExpFocusGained

	private void findDialogCaseSensitivityActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_findDialogCaseSensitivityActionPerformed
	{//GEN-HEADEREND:event_findDialogCaseSensitivityActionPerformed
		// TODO add your handling code here:
	}//GEN-LAST:event_findDialogCaseSensitivityActionPerformed

	private void findDialogCaseSensitivityStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_findDialogCaseSensitivityStateChanged
	{//GEN-HEADEREND:event_findDialogCaseSensitivityStateChanged
		// TODO add your handling code here:
	}//GEN-LAST:event_findDialogCaseSensitivityStateChanged

	private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
	{//GEN-HEADEREND:event_formWindowClosing
		fireCancelEvent();
	}//GEN-LAST:event_formWindowClosing

	private void findDialogCancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_findDialogCancelActionPerformed
	{//GEN-HEADEREND:event_findDialogCancelActionPerformed
		this.fireCancelEvent();
	}//GEN-LAST:event_findDialogCancelActionPerformed

	private void findDialogOkActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_findDialogOkActionPerformed
	{//GEN-HEADEREND:event_findDialogOkActionPerformed
		this.fireAcceptEvent();
	}//GEN-LAST:event_findDialogOkActionPerformed

	private void findDialogRegExpActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_findDialogRegExpActionPerformed
	{//GEN-HEADEREND:event_findDialogRegExpActionPerformed
		fireAcceptEvent();
	}//GEN-LAST:event_findDialogRegExpActionPerformed
	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[])
	{
		MiLookAndFeel.setCurrentLookAndFeel(MiLookAndFeel.LAF_SUBSTANCE_CREME_COFFEE);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				Frame test = new JFrame();
				List<String> existingPatterns = new ArrayList<String>();
				for(int i =0; i < 10; ++i)
				{
					existingPatterns.add("Pattern"+i);
				}
				new FindDialog(test, existingPatterns, true, new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						JDialog originator = (JDialog)e.getSource();
						originator.dispose();
					}
				}).setVisible(true);
				Iterator iter = existingPatterns.iterator();
				while(iter.hasNext())
				{
					System.out.println(iter.next());
				}
				test.dispose();
			}
		});
	}
	
	private void fireAcceptEvent()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				// Rebuilding the original regexp list (if exists)
				if(null != m_savedRegExps)
				{
					m_savedRegExps.clear();
					for(int i = 0; i < findDialogRegExp.getItemCount(); ++i)
					{
						m_savedRegExps.add((String)findDialogRegExp.getItemAt(i));
					}
				}
				if(m_listener != null)
				{
					if(findDialogCaseSensitivity.isSelected())
						m_listener.actionPerformed(new ActionEvent(FindDialog.this, ACCEPTED, (String)findDialogRegExp.getSelectedItem(), CASE_INSENSITIVE));
					else
						m_listener.actionPerformed(new ActionEvent(FindDialog.this, ACCEPTED, (String)findDialogRegExp.getSelectedItem(), CASE_SENSITIVE));
				}

				m_listener = null;
			}
		});
	}
	
	private void fireCancelEvent()
	{
		if(m_listener != null)
			m_listener.actionPerformed(new ActionEvent(FindDialog.this, CANCELLED, ""));

		m_listener = null;
	}
	
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton findDialogCancel;
    private javax.swing.JCheckBox findDialogCaseSensitivity;
    private javax.swing.JButton findDialogOk;
    private javax.swing.JComboBox findDialogRegExp;
    // End of variables declaration//GEN-END:variables

	private ActionListener m_listener;
	
	public static final int ACCEPTED = 1212;
	
	public static final int CANCELLED = 3434;
	
	private final List<String> m_savedRegExps;
	
	/**
	 * ActionEvent's modifier
	 */	
	public static final int CASE_SENSITIVE = 1;
	
	/**
	 * ActionEvent's modifier
	 */	
	public static final int CASE_INSENSITIVE = 2;
	
	private boolean m_caseInsensitive = false;
	
}