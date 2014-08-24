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

public interface Data {
    int getRowCount();
    int getColumnCount();
    String getColumnName(int col);
    TypeSpec getTypeSpec(int col);
    Object getValueAt(int row, int col);

    static final int LOADING = 0;
    static final int PAUSED = 1;
    static final int FINISHED = 2;
    void setState(int state);
    int getState();
    void addStateListener(StateListener listener);
    void removeStateListener(StateListener listener);
    interface StateListener {
        void stateChanged(int state, int rows);
    }
}
