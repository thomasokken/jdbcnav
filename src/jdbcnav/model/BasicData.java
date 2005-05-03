package jdbcnav.model;

import java.util.*;


public class BasicData implements Data {
    private String[] columnNames;
    private Class[] columnClasses;
    private ArrayList data;

    public BasicData() {
	//
    }

    public BasicData(Data src) {
	int rows = src.getRowCount();
	int cols = src.getColumnCount();
	columnNames = new String[cols];
	columnClasses = new Class[cols];
	for (int i = 0; i < cols; i++) {
	    columnNames[i] = src.getColumnName(i);
	    columnClasses[i] = src.getColumnClass(i);
	}
	data = new ArrayList();
	for (int i = 0; i < rows; i++) {
	    Object[] row = new Object[cols];
	    // TODO: handle objects that need to be cloned (mutable objects;
	    // e.g. java.sql.Blob).
	    for (int j = 0; j < cols; j++)
		row[j] = src.getValueAt(i, j);
	    data.add(row);
	}
    }

    public void setColumnNames(String[] columnNames) {
	this.columnNames = columnNames;
    }

    public void setColumnClasses(Class[] columnClasses) {
	this.columnClasses = columnClasses;
    }

    public void setData(ArrayList data) {
	this.data = data;
    }

    public void addRow(Object[] row) {
	data.add(row);
    }

    public Object clone() {
	BasicData that = new BasicData();
	that.columnNames = (String[]) columnNames.clone();
	that.columnClasses = (Class[]) columnClasses.clone();
	that.data = (ArrayList) data.clone();
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

    public Class getColumnClass(int col) {
	return columnClasses[col];
    }

    public Object getValueAt(int row, int col) {
	return ((Object[]) data.get(row))[col];
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
