/*
 * TableSearcher.java
 *
 * Created on May 27, 2004, 10:16 AM
 */

package com.mitel.guiutil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 *
 * @author  haiv
 */
public class TableSearcher
{
	private JTable m_targetTable;
	private String m_regExp;
	private int m_lastSelectedRow = 0;
	private int m_lastSelectedCol = 0;
	
	/** Creates a new instance of TableSearcher */
	public TableSearcher(JTable target)
	{
		m_targetTable = target;
	}
	
	public boolean initiateSearch(String regexp, int searchFlag)
	{
		m_regExp = regexp;
		TableModel model = m_targetTable.getModel();
		if(null != model)
		{
			Pattern p = Pattern.compile(regexp, searchFlag);
			
			for(int row = 0; row < model.getRowCount(); ++row)
			{
				for(int col = 0; col < model.getColumnCount(); ++col)
				{
					Matcher m = p.matcher(model.getValueAt(row, col).toString());
					if(m.find())
					{
						m_lastSelectedRow = row;
						m_lastSelectedCol = col;
						m_targetTable.changeSelection(row,col,false,false);
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean continueSearch(String regexp, int searchFlag)
	{
		m_regExp = regexp;
		TableModel model = m_targetTable.getModel();
		boolean cellSearch = m_targetTable.getCellSelectionEnabled();
		if(null != model)
		{
			Pattern p = Pattern.compile(regexp, searchFlag);
			
			int startRow;
			int startCol;
			if(cellSearch)
			{
				startRow = m_lastSelectedRow;
				startCol = m_lastSelectedCol + 1;
				
				// Wraps around
				if(startCol >= model.getColumnCount())
				{
					startCol = 0;
					++startRow;
				}
			}
			else
			{
				startRow = m_lastSelectedRow + 1;
				startCol = 0;
			}
			
			if(startRow >= model.getRowCount())
				startRow = 0;
			
			int curRow = startRow;
			int curCol = startCol;
			while(true)
			{
				Matcher m = p.matcher(model.getValueAt(curRow, curCol).toString());
				if(m.find())
				{
					m_lastSelectedRow = curRow;
					m_lastSelectedCol = curCol;
					m_targetTable.changeSelection(curRow,curCol,false,false);
					return true;
				}
				++curCol;
				if(curCol >= model.getColumnCount())
				{
					// Line wrap
					curCol = 0;
					++curRow;
					
					// Table wrap check
					if(curRow >= model.getRowCount())
						curRow = 0;
				}
				
				// Roll over check
				if((curRow == startRow)&&(curCol == startCol))
					break;
			}
		}
		return false;
	}
}
