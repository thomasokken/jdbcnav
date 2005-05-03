package jdbcnav.model;

import java.util.*;


public class BackgroundLoadData implements Data {
    private String[] columnNames;
    private Class[] columnClasses;
    private ArrayList data;
    private ArrayList listeners;
    private int state;
    private long lastUpdateTime;

    public BackgroundLoadData(String[] columnNames, Class[] columnClasses) {
	this.columnNames = columnNames;
	this.columnClasses = columnClasses;
	data = new ArrayList();
	listeners = new ArrayList();
	state = LOADING;
	lastUpdateTime = new Date().getTime();
    }

    ////////////////
    ///// Data /////
    ////////////////

    public synchronized int getRowCount() {
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

    public synchronized Object getValueAt(int row, int col) {
	return ((Object[]) data.get(row))[col];
    }

    //////////////////////////////////////
    ///// Background Loading Support /////
    //////////////////////////////////////

    public synchronized void addStateListener(StateListener listener) {
	if (state == FINISHED)
	    listener.stateChanged(FINISHED, data.size());
	else
	    listeners.add(listener);
    }

    public synchronized void removeStateListener(StateListener listener) {
	if (listeners != null)
	    listeners.remove(listener);
    }

    public synchronized void setState(int state) {
	if (this.state == FINISHED)
	    return;
	this.state = state;
	int rows = data.size();
	ArrayList l = listeners;
	if (state == FINISHED)
	    listeners = null;
	for (Iterator iter = l.iterator(); iter.hasNext();) {
	    StateListener listener = (StateListener) iter.next();
	    listener.stateChanged(state, rows);
	}
    }

    public synchronized int getState() {
	return state;
    }

    public synchronized void addRow(Object[] row) {
	if (state == FINISHED)
	    return;
	data.add(row);
	long now = new Date().getTime();
	if (now - lastUpdateTime < 5000)
	    return;
	lastUpdateTime = now;
	int newRows = data.size();
	for (Iterator iter = listeners.iterator(); iter.hasNext();) {
	    StateListener listener = (StateListener) iter.next();
	    listener.stateChanged(state, newRows);
	}
    }
}
