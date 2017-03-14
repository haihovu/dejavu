/*
 * TableSearcher.java
 *
 * Created on May 27, 2004, 10:16 AM
 */
package org.dejavu.guiutil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 *
 * @author haiv
 */
public class TableSearcher {

	private final JTable targetTable;
	private String regExp;
	private int lastSelectedRow = 0;
	private int lastSelectedCol = 0;

	/**
	 * Creates a new instance of TableSearcher
	 * @param target The target table
	 */
	public TableSearcher(JTable target) {
		targetTable = target;
	}
	/**
	 * Initiates a search. Must be invoked from EDT.
	 * @param regexp The regex string used for searching.
	 * @param searchFlag Regex seach option.
	 * @return True if search found something, false otherwise.
	 */
	public boolean initiateSearch(String regexp, int searchFlag) {
		regExp = regexp;
		TableModel model = targetTable.getModel();
		if (null != model) {
			Pattern p = Pattern.compile(regexp, searchFlag);

			for (int row = 0; row < model.getRowCount(); ++row) {
				for (int col = 0; col < model.getColumnCount(); ++col) {
					Matcher m = p.matcher(model.getValueAt(row, col).toString());
					if (m.find()) {
						lastSelectedRow = row;
						lastSelectedCol = col;
						targetTable.changeSelection(row, col, false, false);
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean continueSearch(String regexp, int searchFlag) {
		regExp = regexp;
		TableModel model = targetTable.getModel();
		boolean cellSearch = targetTable.getCellSelectionEnabled();
		if (null != model) {
			Pattern p = Pattern.compile(regexp, searchFlag);

			int startRow;
			int startCol;
			if (cellSearch) {
				startRow = lastSelectedRow;
				startCol = lastSelectedCol + 1;

				// Wraps around
				if (startCol >= model.getColumnCount()) {
					startCol = 0;
					++startRow;
				}
			} else {
				startRow = lastSelectedRow + 1;
				startCol = 0;
			}

			if (startRow >= model.getRowCount()) {
				startRow = 0;
			}

			int curRow = startRow;
			int curCol = startCol;
			while (true) {
				Matcher m = p.matcher(model.getValueAt(curRow, curCol).toString());
				if (m.find()) {
					lastSelectedRow = curRow;
					lastSelectedCol = curCol;
					targetTable.changeSelection(curRow, curCol, false, false);
					return true;
				}
				++curCol;
				if (curCol >= model.getColumnCount()) {
					// Line wrap
					curCol = 0;
					++curRow;

					// Table wrap check
					if (curRow >= model.getRowCount()) {
						curRow = 0;
					}
				}

				// Roll over check
				if ((curRow == startRow) && (curCol == startCol)) {
					break;
				}
			}
		}
		return false;
	}
}
