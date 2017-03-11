/*
 * DjvComboBox.java
 *
 * Created on April 20, 2004, 7:32 PM
 */

package org.dejavu.guiutil;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.plaf.basic.BasicComboBoxEditor;


/**
 * This class helps make the look & feel of the combo box more intuitive,
 * i.e. when the combo box gains focus, the editor should select the content of
 * its text field.
 * Also, if a value is newly edited it should be added to the drop down list.
 * @author Hai Vu
 */
public class DjvComboBox extends JComboBox
{
	private static final long serialVersionUID = 1L;
	private final DjvComboBox.MiComboBoxEditor m_MyEditor = new DjvComboBox.MiComboBoxEditor();
	private final DjvComboBox.MiComboBoxActionListener m_ActionListener;
	
	private boolean m_UpdatingMode = false;
	
	/**
	 * Creates a new instance of MiComboBox.
	 * Like a normal JComboBox but is always editable and the edit field automatically
	 * select all when focus is gained.
	 */
	public DjvComboBox()
	{
		setEditor(m_MyEditor);
		m_MyEditor.getTextField().setBorder(new BevelBorder(BevelBorder.LOWERED));
		m_ActionListener = new MiComboBoxActionListener();
		addActionListener(m_ActionListener);

		// This is to prevent the act of simply scrolling through the list of items with the keyboard causing selection events to be fired.
		putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
	}
	
	/**
	 * Since the JComboBox's focus is made up of sub-component, this helps identify the
	 * subcomponent to receive focus attentions: the text field of the editor.
	 * @return 
	 */	
	@Override
	public boolean requestFocusInWindow()
	{
		// Pass the request on to the editor's text field
		if(isEditable)
			return m_MyEditor.getTextField().requestFocusInWindow();
		
		return super.requestFocusInWindow();
	}
	
	/**
	 * Since the JComboBox's focus is made up of sub-component, this helps identify the
	 * subcomponent to receive focus attentions: the text field of the editor.
	 */	
	@Override
	public void requestFocus()
	{
		super.requestFocus();
		
		// Pass the request on to the editor's text field
		if(isEditable)
			m_MyEditor.getTextField().requestFocus();
	}
	
	@SuppressWarnings("unchecked")
	private void updateList()
	{
		if( !m_UpdatingMode )
		{
			m_UpdatingMode = true;
			Object selectedItem = getSelectedItem();
			removeItem(selectedItem);
			insertItemAt(selectedItem,0);
			setSelectedIndex(0);

			m_UpdatingMode = false;
		}
	}

	@Override
	public void addFocusListener(FocusListener listener)
	{
		super.addFocusListener(listener);
		
		// Since this is invoked from the constructor of the base class, this check is required
		if(null != m_MyEditor) 
			m_MyEditor.getTextField().addFocusListener(listener);
	}

	@Override
	public void removeFocusListener(FocusListener listener)
	{
		super.removeFocusListener(listener);

		// Since this is invoked from the constructor of the base class, this check is required
		if(null != m_MyEditor)
			m_MyEditor.getTextField().removeFocusListener(listener);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void addItem(Object anObject)
	{
		removeItem(anObject);
		super.addItem(anObject);
	}
	
	/**
	 * Needed to expose the text field.
	 */
	private static class MiComboBoxEditor extends BasicComboBoxEditor
	{
		/**
		 * Create a MiComboBoxEditor
		 */		
		public MiComboBoxEditor()
		{
			// Setup the proper focus properties
			editor.setFocusable(true);
			editor.setRequestFocusEnabled(true);
		}
		
		/**
		 * Accessor for the embeded text field.
		 *
		 * @return
		 */
		public JTextField getTextField()
		{
			return editor;
		}
	}
	
	private class MiComboBoxActionListener implements ActionListener
	{
		public MiComboBoxActionListener()
		{
		}
		
		@Override
		public void actionPerformed(ActionEvent actionEvent)
		{
			if(actionEvent.getActionCommand().equals("comboBoxEdited")
				||(actionEvent.getModifiers()==16/*Hmmm, can't remember what mod 16 was*/))
			{
				updateList();
			}
		}
	}

}
