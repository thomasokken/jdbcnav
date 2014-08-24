///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2010  Thomas Okken
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

package jdbcnav.model;

import java.util.*;


public class BasicData implements Data {
    private String[] columnNames;
    private TypeSpec[] typeSpecs;
    private ArrayList<Object[]> data;

    public BasicData() {
        //
    }

    public BasicData(Data src) {
        int rows = src.getRowCount();
        int cols = src.getColumnCount();
        columnNames = new String[cols];
        typeSpecs = new TypeSpec[cols];
        for (int i = 0; i < cols; i++) {
            columnNames[i] = src.getColumnName(i);
            typeSpecs[i] = src.getTypeSpec(i);
        }
        data = new ArrayList<Object[]>();
        for (int i = 0; i < rows; i++) {
            Object[] row = new Object[cols];
            for (int j = 0; j < cols; j++) {
                Object o = src.getValueAt(i, j);
                if (o instanceof BlobWrapper)
                    o = ((BlobWrapper) o).load();
                else if (o instanceof ClobWrapper)
                    o = ((ClobWrapper) o).load();
                row[j] = o;
            }
            data.add(row);
        }
    }

    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames;
    }

    public void setTypeSpecs(TypeSpec[] typeSpecs) {
        this.typeSpecs = typeSpecs;
    }

    public void setData(ArrayList<Object[]> data) {
        this.data = data;
    }

    public void addRow(Object[] row) {
        data.add(row);
    }

    @SuppressWarnings("unchecked")
    public Object clone() {
        BasicData that = new BasicData();
        that.columnNames = columnNames.clone();
        that.typeSpecs = typeSpecs.clone();
        that.data = (ArrayList<Object[]>) data.clone();
        return that;
    }

    ////////////////
    ///// Data /////
    ////////////////

    public int getRowCount() {
        return data.size();
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public TypeSpec getTypeSpec(int col) {
        return typeSpecs[col];
    }

    public Object getValueAt(int row, int col) {
        return data.get(row)[col];
    }


    // Trivial implementation of asynchronicity support

    public void setState(int state) {
        // Nothing to do
    }

    public int getState() {
        return FINISHED;
    }

    public void addStateListener(StateListener listener) {
        listener.stateChanged(FINISHED, data.size());
    }

    public void removeStateListener(StateListener listener) {
        // Nothing to do
    }
}
