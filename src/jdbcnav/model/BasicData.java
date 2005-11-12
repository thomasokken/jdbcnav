package jdbcnav.model;

import java.sql.Blob;
import java.sql.Clob;
import java.util.*;
import jdbcnav.util.MiscUtils;


public class BasicData implements Data {
    private String[] columnNames;
    private TypeSpec[] typeSpecs;
    private ArrayList data;

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
	data = new ArrayList();
	for (int i = 0; i < rows; i++) {
	    Object[] row = new Object[cols];
	    for (int j = 0; j < cols; j++) {
		Object o = src.getValueAt(i, j);
		if (o instanceof Blob)
		    o = MiscUtils.loadBlob((Blob) o);
		else if (o instanceof Clob)
		    o = MiscUtils.loadClob((Clob) o);
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

    public void setData(ArrayList data) {
	this.data = data;
    }

    public void addRow(Object[] row) {
	data.add(row);
    }

    public Object clone() {
	BasicData that = new BasicData();
	that.columnNames = (String[]) columnNames.clone();
	that.typeSpecs = (TypeSpec[]) typeSpecs.clone();
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

    public TypeSpec getTypeSpec(int col) {
	return typeSpecs[col];
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
