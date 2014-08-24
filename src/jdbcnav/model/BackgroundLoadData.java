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


public class BackgroundLoadData implements Data {
    private String[] columnNames;
    private TypeSpec[] typeSpecs;
    private ArrayList<Object[]> data;
    private ArrayList<StateListener> listeners;
    private int state;
    private long lastUpdateTime;

    public BackgroundLoadData(String[] columnNames, TypeSpec[] typeSpecs) {
        this.columnNames = columnNames;
        this.typeSpecs = typeSpecs;
        data = new ArrayList<Object[]>();
        listeners = new ArrayList<StateListener>();
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

    public TypeSpec getTypeSpec(int col) {
        return typeSpecs[col];
    }

    public synchronized Object getValueAt(int row, int col) {
        return data.get(row)[col];
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
        ArrayList<StateListener> l = listeners;
        if (state == FINISHED)
            listeners = null;
        for (StateListener listener : l)
            listener.stateChanged(state, rows);
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
        for (StateListener listener : listeners)
            listener.stateChanged(state, newRows);
    }
}
