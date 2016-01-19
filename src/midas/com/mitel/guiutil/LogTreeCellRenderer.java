package com.mitel.guiutil;

import com.mitel.miutil.MiLogMsg;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * Custom renderer for the log tree (colored according to severity).
 */
public class LogTreeCellRenderer extends DefaultTreeCellRenderer
{
	private static final long serialVersionUID = 1L;
	private boolean m_DefaultOpacity;
	private boolean m_IsNimbus;

	public LogTreeCellRenderer()
	{
		super();
		m_DefaultOpacity = isOpaque();
		
		// While NIMBUS is an excellent look and feel, its implementation of the tree cell renderer is a bit non-standard in the area of opacity.
		m_IsNimbus = UIManager.getLookAndFeel().getName().equals(MiLookAndFeel.LAF_NIMBUS);
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
	{
		setBackground(null);
		setBackgroundNonSelectionColor(null);
		setForeground(null);
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		Color bg = null;
		Color fg = null;
		boolean opacity = m_DefaultOpacity;
		if(value instanceof DefaultMutableTreeNode)
		{
			DefaultMutableTreeNode logNode = (DefaultMutableTreeNode)value;
			if(logNode.getUserObject() instanceof MiLogMsg)
			{
				MiLogMsg msg = (MiLogMsg)logNode.getUserObject();
				switch(msg.m_Severity)
				{
					case 0:
						if(!sel)
						{
							bg = Color.RED;
							fg = Color.BLACK;
						}
						opacity = !sel;
						break;
					case 1:
						if(!sel)
						{
							bg = Color.ORANGE;
							fg = Color.BLACK;
						}
						opacity = !sel;
						break;
				}
				setText(msg.m_Index + " : " + msg.tsToString() + " " + msg.toString());
			}
		}

		if(m_IsNimbus)
		{
			setOpaque(opacity);
		}
		if(bg != null)
		{
			setBackground(bg);
			setBackgroundNonSelectionColor(bg);
			setForeground(fg);
		}
		return this;
	}
}
