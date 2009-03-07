///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2009	Thomas Okken
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License, version 2,
// as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
///////////////////////////////////////////////////////////////////////////////

package jdbcnav.util;

import java.util.*;
import javax.swing.table.*;
import jdbcnav.SortedTableModel;
import jdbcnav.model.TypeSpec;
import jdbcnav.model.TypeSpecTableModel;


public class ArrayTableModel extends AbstractTableModel
							 implements SortedTableModel, TypeSpecTableModel {
	private String[] names;
	private Class<?>[] classes;
	private TypeSpec[] typeSpecs;
	private Object[][] data;
	private int[] sortPriority;
	private boolean[] sortAscending;

	public ArrayTableModel(Object[][] data) {
		int columns = data[0].length;
		names = new String[columns];
		classes = new Class[columns];
		typeSpecs = new TypeSpec[columns];
		for (int i = 0; i < columns; i++) {
			names[i] = (String) data[0][i];
			classes[i] = (Class<?>) data[1][i];
			typeSpecs[i] = (TypeSpec) data[2][i];
		}
		int rows = data.length - 3;
		this.data = new Object[rows][columns];
		for (int i = 0; i < rows; i++)
			for (int j = 0; j < columns; j++)
				this.data[i][j] = data[i + 3][j];
		init();
	}

	public ArrayTableModel(String[] names, Class<?>[] classes,
									TypeSpec[] typeSpecs, Object[][] data) {
		this.names = names;
		this.classes = classes;
		this.typeSpecs = typeSpecs;
		this.data = data;
		init();
	}

	private void init() {
		int columns = names.length;
		sortPriority = new int[columns];
		sortAscending = new boolean[columns];
		for (int i = 0; i < columns; i++) {
			sortPriority[i] = i;
			sortAscending[i] = true;
		}
	}

	public Class<?> getColumnClass(int col) {
		return classes[col];
	}

	public int getColumnCount() {
		return names.length;
	}

	public String getColumnName(int col) {
		return names[col];
	}

	public TypeSpec getTypeSpec(int col) {
		return typeSpecs[col];
	}

	public int getRowCount() {
		return data.length;
	}

	public Object getValueAt(int row, int col) {
		return data[row][col];
	}

	public boolean isCellEditable(int row, int col) {
		return false;
	}

	////////////////////////////
	///// SortedTableModel /////
	////////////////////////////

	public void sortColumn(int col) {
		if (col < 0 || col >= sortPriority.length)
			return;
		if (col == sortPriority[0])
			sortAscending[col] = !sortAscending[col];
		else {
			int temp = sortPriority[0];
			sortPriority[0] = col;
			int i = 1;
			while (sortPriority[i] != col) {
				int temp2 = sortPriority[i];
				sortPriority[i] = temp;
				temp = temp2;
				i++;
			}
			sortPriority[i] = temp;
		}
		Arrays.sort(data, rowComparator);
		fireTableDataChanged();
	}

	public int getSortedColumn() {
		return sortPriority[0];
	}

	public void selectionFromViewToModel(int[] selection) {
		// We don't really care about preserving selections when the table
		// is re-sorted... Or at least, *I* don't care enough right now to
		// go to the trouble to implement this method.
	}

	public void selectionFromModelToView(int[] selection) {
		// We don't really care about preserving selections when the table
		// is re-sorted... Or at least, *I* don't care enough right now to
		// go to the trouble to implement this method.
	}

	private RowComparator rowComparator = new RowComparator();

	private class RowComparator implements Comparator<Object[]> {
		public int compare(Object[] a, Object[] b) {
			for (int i = 0; i < sortPriority.length; i++) {
				int col = sortPriority[i];
				int res = MiscUtils.compareObjects(a[col], b[col], true);
				if (res != 0)
					return sortAscending[col] ? res : -res;
			}
			return 0;
		}
	}
}
