///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2023  Thomas Okken
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

package jdbcnav;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import jdbcnav.model.Data;
import jdbcnav.model.Database;
import jdbcnav.model.ForeignKey;
import jdbcnav.model.PrimaryKey;
import jdbcnav.model.Table;
import jdbcnav.model.TypeSpec;
import jdbcnav.util.MiscUtils;
import jdbcnav.util.NavigatorException;


public class TableFrame extends QueryResultFrame {
    private JPopupMenu popupMenu;
    private int popupRow;
    private int popupColumn;
    private RowSelectionHandler rowSelectionHandler;
    private int fkIndex = -1;
    private int fkRow;

    public TableFrame(Table dbTable, BrowserFrame browser)
                                                    throws NavigatorException {
        super(browser, dbTable);
        JMenu m = getJMenuBar().getMenu(0);
        JMenuItem mi = new JMenuItem("Details");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    details();
                                }
                            });
        m.add(mi, m.getItemCount() - 1);

        table.addMouseListener(new MouseAdapter() {
                                    public void mousePressed(MouseEvent e) {
                                        if (e.getButton() == MouseEvent.BUTTON3)
                                            showPopup(e);
                                    }
                                });

        // Highlight "not null" columns
        for (int i = 0; i < dbTable.getColumnCount(); i++) {
            if (!"YES".equals(dbTable.getIsNullable()[i]))
                table.setColumnNotNull(i, true);
        }

        // Apply FK highlight color
        ForeignKey[] fks = dbTable.getForeignKeys();
        TreeSet<String> fkcol = new TreeSet<String>();
        for (int i = 0; i < fks.length; i++) {
            ForeignKey fk = fks[i];
            for (int j = 0; j < fk.getColumnCount(); j++)
                fkcol.add(fk.getThisColumnName(j));
        }
        for (int i = 0; i < dbTable.getColumnCount(); i++)
            if (fkcol.contains(dbTable.getColumnNames()[i]))
                table.setColumnType(i, 2);

        // Apply PK highlight color
        PrimaryKey pk = dbTable.getPrimaryKey();
        if (pk != null) {
            int[] pkcol = dbTable.getPKColumns();
            for (int i = 0; i < pkcol.length; i++) {
                int j = pkcol[i];
                table.setColumnType(j, table.getColumnType(j) | 1);
            }
        }

        PreferencesFrame.addHighlightColorChangeListener(table);
    }

    public void dispose() {
        if (!dbTable.isUpdatableQueryResult())
            dbTable.getDatabase().tableFrameClosed(dbTable.getQualifiedName());
        PreferencesFrame.removeHighlightColorChangeListener(table);
        super.dispose();
    }

    private void showPopup(MouseEvent e) {
        ForeignKey[] fks = dbTable.getForeignKeys();
        ForeignKey[] rks = dbTable.getReferencingKeys();
        boolean haveFks = fks.length != 0;
        boolean haveRks = rks.length != 0;
        if (!haveFks && !haveRks)
            return;
        
        if (popupMenu == null) {
            popupMenu = new JPopupMenu();
            ActionListener listener =
                        new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                MyMenuItem mi = (MyMenuItem) e.getSource();
                                handlePopup(mi.foreign, mi.index, mi.partial);
                            }
                        };

            if (haveFks && dbTable.isEditable()) {
                JMenuItem jmi = new JMenuItem("Select FK Value...");
                jmi.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                selectFkValue();
                            }
                        });
                popupMenu.add(jmi);
            }

            for (int p = 1; p >= 0; p--) {
                JMenuItem tmi = new JMenuItem(p == 1 ? "Partial Table" : "Whole Table");
                tmi.setEnabled(false);
                popupMenu.add(tmi);

                if (haveFks) {
                    JMenu m = new JMenu("References");
                    for (int i = 0; i < fks.length; i++) {
                        ForeignKey fk = fks[i];
                        String name;
                        if (fk.getThatCatalog() != null)
                            if (fk.getThatSchema() != null)
                                name = fk.getThatCatalog()
                                        + "." + fk.getThatSchema()
                                        + "." + fk.getThatName();
                            else
                                name = fk.getThatCatalog()
                                        + "." + fk.getThatName();
                        else if (fk.getThatSchema() != null)
                            name = fk.getThatSchema() + "." + fk.getThatName();
                        else
                            name = fk.getThatName();
                        MyMenuItem mi = new MyMenuItem(name, true, i, p == 1);
                        mi.addActionListener(listener);
                        m.add(mi);
                    }
                    popupMenu.add(m);
                }
                if (haveRks) {
                    JMenu m = new JMenu("Referenced By");
                    for (int i = 0; i < rks.length; i++) {
                        ForeignKey rk = rks[i];
                        String name;
                        if (rk.getThatCatalog() != null)
                            if (rk.getThatSchema() != null)
                                name = rk.getThatCatalog()
                                        + "." + rk.getThatSchema()
                                        + "." + rk.getThatName();
                            else
                                name = rk.getThatCatalog()
                                        + "." + rk.getThatName();
                        else if (rk.getThatSchema() != null)
                            name = rk.getThatSchema() + "." + rk.getThatName();
                        else
                            name = rk.getThatName();
                        MyMenuItem mi = new MyMenuItem(name, false, i, p == 1);
                        mi.addActionListener(listener);
                        m.add(mi);
                    }
                    popupMenu.add(m);
                }
            }
        }

        int x = e.getX();
        int y = e.getY();
        Point p = new Point(x, y);
        popupRow = table.rowAtPoint(p);
        popupColumn = table.convertColumnIndexToModel(table.columnAtPoint(p));
        String columnName = model.getColumnName(popupColumn);

        int id = haveFks && dbTable.isEditable() ? 1 : 0;
        for (int m = 0; m < 6; m++) {
            if (m == 0 || m == 3) {
                // "Partial Table" / "Whole Table" titles; skip
                id++;
                continue;
            }
            if (m == 1 || m == 4) {
                if (haveFks) {
                    Component[] items = ((JMenu) popupMenu.getComponent(id++))
                            .getMenuComponents();
                    fkIndex = -1;
                    for (int i = 0; i < items.length; i++) {
                        ForeignKey fk = fks[i];
                        boolean matches = false;
                        for (int j = 0; j < fk.getColumnCount(); j++)
                            if (fk.getThisColumnName(j).equals(columnName)) {
                                matches = true;
                                break;
                            }
                        Component c = items[i];
                        Font f = c.getFont();
                        f = f.deriveFont(matches ? Font.BOLD : Font.PLAIN);
                        if (matches) {
                            fkIndex = i;
                            fkRow = popupRow;
                        }
                        c.setFont(f);
                    }
                    JMenuItem jmi = (JMenuItem) popupMenu.getComponent(0);
                    jmi.setEnabled(fkIndex != -1);
                }
            } else {
                if (haveRks) {
                    Component[] items = ((JMenu) popupMenu.getComponent(id++))
                            .getMenuComponents();
                    for (int i = 0; i < items.length; i++) {
                        ForeignKey rk = rks[i];
                        boolean matches = false;
                        for (int j = 0; j < rk.getColumnCount(); j++)
                            if (rk.getThisColumnName(j).equals(columnName)) {
                                matches = true;
                                break;
                            }
                        Component c = items[i];
                        Font f = c.getFont();
                        f = f.deriveFont(matches ? Font.BOLD : Font.PLAIN);
                        c.setFont(f);
                    }
                }
            }
        }
                
        popupMenu.show(e.getComponent(), x, y);
    }

    private void handlePopup(boolean foreign, int index, boolean partial) {
        ForeignKey k =
                        (foreign ? dbTable.getForeignKeys()
                                 : dbTable.getReferencingKeys())[index];
        int n = k.getColumnCount();
        String[] thisColumns = new String[n];
        String[] thatColumns = new String[n];
        for (int i = 0; i < n; i++) {
            thisColumns[i] = k.getThisColumnName(i);
            thatColumns[i] = k.getThatColumnName(i);
        }
        Object[] val = new Object[n];

        int left = thisColumns.length;
        fetchvalue: {
            for (int i = 0; i < model.getColumnCount(); i++) {
                String columnName = model.getColumnName(i);
                for (int j = 0; j < thisColumns.length; j++)
                    if (thisColumns[j].equals(columnName)) {
                        val[j] = model.getValueAt(popupRow, i);
                        if (--left == 0)
                            break fetchvalue;
                        else
                            break;
                    }
            }
        }
            
        Database db = dbTable.getDatabase();
        if (partial) {
            StringBuffer qbuf = new StringBuffer();
            qbuf.append("select * from " + k.getThatQualifiedName());
            for (int i = 0; i < n; i++) {
                qbuf.append(i == 0 ? " where " : " and ");
                qbuf.append(db.quote(thatColumns[i]));
                qbuf.append(" = ?");
            }
            Table table;
            try {
                table = (Table) db.runQuery(qbuf.toString(), val);
                TableFrame tf = new TableFrame(table, browser);
                tf.setParent(browser);
                tf.showStaggered();
            } catch (Exception e) {
                MessageBox.show(e);
                return;
            }
        } else {
            TableFrame editFrame = db.showTableFrame(k.getThatQualifiedName());
            if (editFrame != null)
                editFrame.selectRows(thatColumns, val);
        }
    }

    private void details() {
        Database db = dbTable.getDatabase();
        if (dbTable.isUpdatableQueryResult()) {
            TableDetailsFrame tdf = new TableDetailsFrame(dbTable, db.getBrowser());
            tdf.setParent(browser);
            tdf.showStaggered();
        } else
            db.showTableDetailsFrame(dbTable.getQualifiedName());
    }

    private void selectFkValue() {
        if (fkIndex == -1)
            return;
        ForeignKey fk = dbTable.getForeignKeys()[fkIndex];
        String thatName = fk.getThatQualifiedName();
        Database db = dbTable.getDatabase();
        PrimaryKey pk = null;
        Data pkValues = null;
        try {
            Table thatTable = db.getTable(thatName);
            pk = thatTable.getPrimaryKey();
            pkValues = thatTable.getPKValues();
        } catch (NavigatorException e) {
            MessageBox.show("Can't load primary key values.", e);
            return;
        }
        if (pkValues == null) {
            MessageBox.show("Can't load primary key values.", null);
            return;
        }

        // We wrap the Data object in an FkData object, which takes care
        // of displaying the referencing table's column names (instead of the
        // names of the PK columns of the referenced table), and of ordering
        // the columns to make their order match the order in this TableFrame.

        int ncols = pkValues.getColumnCount();
        String[] oldNames = new String[ncols];
        String[] newNames = new String[ncols];
        int mcols = model.getColumnCount();
        int n = 0;
        boolean allowNull = true;
        for (int i = 0; i < mcols; i++) {
            int c = table.convertColumnIndexToModel(i);
            String name = model.getColumnName(c);
            for (int j = 0; j < ncols; j++) {
                if (name.equals(fk.getThisColumnName(j))) {
                    oldNames[n] = fk.getThatColumnName(j);
                    newNames[n] = name;
                    n++;
                    if (!"YES".equals(dbTable.getIsNullable()[c]))
                        allowNull = false;
                    break;
                }
            }
        }

        pkValues = new FkData(pkValues, allowNull, oldNames, newNames);

        // We want the PK data to be sorted in the same order as the referenced
        // table would be, if opened in a new window; this means, sort
        // according to the order of the PK columns in the PrimaryKey object.

        int[] sortOrder = new int[ncols];
        for (int i = 0; i < ncols; i++) {
            String name = pk.getColumnName(i);
            for (int j = 0; j < ncols; j++) {
                if (name.equals(oldNames[j])) {
                    sortOrder[i] = j;
                    break;
                }
            }
        }

        ForeignKeySelector fksel = new ForeignKeySelector(fkRow, pkValues,
                                                                sortOrder);
        fksel.setParent(this);
        fksel.setCallback(new ForeignKeySelector.Callback() {
                public void select(int row, String[] names, Object[] values) {
                    fkValueSelected(row, names, values);
                }
            });
        fksel.showCentered();
    }

    private void fkValueSelected(int row, String[] names, Object[] values) {
        int ncols = model.getColumnCount();
        for (int i = 0; i < ncols; i++) {
            int col = MiscUtils.arrayLinearSearch(names, model.getColumnName(i));
            if (col != -1)
                model.setValueAt(values[col], row, i);
        }
    }

    private void selectRows(String[] key, Object[] value) {
        try {
            rowSelectionHandler.finish();
        } catch (NullPointerException e) {
            // Guess we didn't have an active RowSelectionHandler, then.
            // Note: I don't do the usual thing of simply testing for 'null'
            // before dereferencing 'rowSelectionHandler', because there is
            // a race condition there (RowSelectionHandler.finish(), which
            // sets 'rowSelectionHandler' to null, can get called between the
            // test and the dereference, since it can get called from the
            // background thread that loads table data asynchronously).
            // We could avoid the race condition by using a lock, but this
            // was easier. :-)
        }

        int[] keyIndex = new int[key.length];
        matchkey:
        for (int i = 0; i < key.length; i++) {
            String columnName = key[i];
            for (int j = 0; j < model.getColumnCount(); j++) {
                if (columnName.equalsIgnoreCase(model.getColumnName(j))) {
                    keyIndex[i] = j;
                    continue matchkey;
                }
            }
            // Key component not found! This is fatal.
            return;
        }

        rowSelectionHandler = new RowSelectionHandlerForKey(keyIndex, value);
    }
    
    public void selectRowsForSearch(String searchText) {
        try {
            rowSelectionHandler.finish();
        } catch (NullPointerException e) {
            // Guess we didn't have an active RowSelectionHandler, then.
            // Note: I don't do the usual thing of simply testing for 'null'
            // before dereferencing 'rowSelectionHandler', because there is
            // a race condition there (RowSelectionHandler.finish(), which
            // sets 'rowSelectionHandler' to null, can get called between the
            // test and the dereference, since it can get called from the
            // background thread that loads table data asynchronously).
            // We could avoid the race condition by using a lock, but this
            // was easier. :-)
        }

        rowSelectionHandler = new RowSelectionHandlerForSearch(searchText);
    }

    private class RowSelectionHandlerForKey extends RowSelectionHandler {
        private int[] keyIndex;
        private Object[] keyValue;

        public RowSelectionHandlerForKey(int[] keyIndex, Object[] keyValue) {
            this.keyIndex = keyIndex;
            this.keyValue = keyValue;
            init();
        }
        
        protected boolean matches(int row) {
            for (int j = 0; j < keyIndex.length; j++) {
                Object o1 = keyValue[j];
                Object o2 = model.getValueAt(row, keyIndex[j]);
                if (o1 == null ? o2 != null : !o1.equals(o2))
                    return false;
            }
            return true;
        }
    }
    
    private class RowSelectionHandlerForSearch extends RowSelectionHandler {
        private Object[] obj;
        private boolean caseInsensitive;
        
        public RowSelectionHandlerForSearch(String searchText) {
            caseInsensitive = dbTable.getDatabase().isCaseSensitive();
            int cols = model.getColumnCount();
            obj = new Object[cols];
            for (int c = 0; c < cols; c++) {
                TypeSpec spec = dbTable.getTypeSpecs()[c];
                try {
                    obj[c] = spec.stringToObject(searchText);
                } catch (Exception e) {}
            }

            init();
        }
        
        protected boolean matches(int row) {
            for (int c = 0; c < obj.length; c++) {
                Object so = obj[c];
                if (so == null)
                    continue;
                Object to = model.getValueAt(row, c);
                if (so instanceof String) {
                    if (!(to instanceof String))
                        continue;
                    String s, t;
                    if (caseInsensitive) {
                        s = ((String) so).toLowerCase();
                        t = ((String) to).toLowerCase();
                    } else {
                        s = (String) so;
                        t = (String) to;
                    }
                    if (t.contains(s))
                        return true;
                } else {
                    if (so.equals(to))
                        return true;
                }
            }
            return false;
        }
    }
    
    private abstract class RowSelectionHandler implements Data.StateListener,
                                    MyTable.UserInteractionListener, Runnable {
        private int lastRow = 0;
        private boolean allowAutoScroll = true;
        private ArrayList<Integer> rowsToSelect = new ArrayList<Integer>();
        private Rectangle selectRect;

        protected void init() {
            model.addStateListener(this);
            table.addUserInteractionListener(this);
            table.clearSelection();
        }

        public synchronized void finish() {
            model.removeStateListener(this);
            table.removeUserInteractionListener(this);
            rowSelectionHandler = null;
        }
        
        protected abstract boolean matches(int row);

        // Data.StateListener methods
        public synchronized void stateChanged(int state, int row) {
            boolean workToDo = false;
            if (row > lastRow) {
                for (int i = lastRow; i < row; i++) {
                    if (matches(i)) {
                        rowsToSelect.add(i);
                        workToDo = true;
                    }
                }
                // This is needed in case the previous last row was selected;
                // in that case, all the added rows are automagically selected
                // as well, and we don't want that.
                table.removeRowSelectionInterval(lastRow, row - 1);
                lastRow = row;
            }
            if (workToDo)
                SwingUtilities.invokeLater(this);
            if (state == Data.FINISHED)
                finish();
        }

        // MyTable.UserInteractionListener methods
        public void eventInScrollBar() {
            // If the user interacts with the scroll bars, we stop scrolling
            // on finding selected rows (we don't want to interfere).
            allowAutoScroll = false;
        }
        public void eventInTable() {
            // If the user interacts with the table, we stop selecting
            // rows that match keyValue as they are loaded (we don't want
            // to interfere).
            finish();
        }

        // Runnable methods (for SwingUtilities.invokeLater())
        public synchronized void run() {
            if (rowsToSelect.size() == 0)
                return;
            boolean needToScroll = false;
            table.addColumnSelectionInterval(0, table.getColumnCount() - 1);
            for (int row : rowsToSelect) {
                table.addRowSelectionInterval(row, row);
                if (allowAutoScroll) {
                    Rectangle r2 = table.getCellRect(row, -1, true);
                    selectRect = selectRect == null ? r2 : selectRect.union(r2);
                    needToScroll = true;
                }
            }
            rowsToSelect.clear();
            if (needToScroll) {
                JViewport view = (JViewport) SwingUtilities
                            .getAncestorOfClass(JViewport.class, table);
                if (view != null) {
                    // Clip 'selectRect' horizontally against the view
                    // rectangle.
                    // This is necessary to prevent horizontal scrolling.
                    Rectangle vr = view.getViewRect();
                    selectRect.x = vr.x;
                    selectRect.width = vr.width;
                    table.scrollRectToVisible(selectRect);
                }
            }
        }
    }

    protected void rowsWereLoaded() {
        //
    }

    protected void doneLoadingRows() {
        //
    }

    private class MyMenuItem extends JMenuItem {
        public boolean foreign;
        public int index;
        public boolean partial;
        public MyMenuItem(String title, boolean foreign, int index, boolean partial) {
            super(title);
            this.foreign = foreign;
            this.index = index;
            this.partial = partial;
        }
    }

    private static class FkData implements Data, Data.StateListener {
        private Data data;
        private int[] colIndexes;
        private String[] colNames;
        private boolean allowNull;
        private ArrayList<StateListener> listeners = new ArrayList<StateListener>();
        public FkData(Data data, boolean allowNull,
                            String[] oldNames, String[] newNames) {
            this.data = data;
            this.allowNull = allowNull;
            int ncols = data.getColumnCount();
            colIndexes = new int[ncols];
            colNames = new String[ncols];
            for (int i = 0; i < ncols; i++) {
                String oldName = oldNames[i];
                String newName = newNames[i];
                for (int j = 0; j < ncols; j++) {
                    String name = data.getColumnName(j);
                    if (name.equals(oldName)) {
                        colIndexes[i] = j;
                        colNames[i] = newName;
                        break;
                    }
                }
            }
            data.addStateListener(this);
        }
        public int getRowCount() {
            return data.getRowCount() + (allowNull ? 1 : 0);
        }
        public int getColumnCount() {
            return data.getColumnCount();
        }
        public String getColumnName(int col) {
            return colNames[col];
        }
        public TypeSpec getTypeSpec(int col) {
            return data.getTypeSpec(colIndexes[col]);
        }
        public Object getValueAt(int row, int col) {
            if (allowNull)
                row--;
            if (row == -1)
                return null;
            else
                return data.getValueAt(row, colIndexes[col]);
        }
        public void setState(int state) {
            data.setState(state);
        }
        public int getState() {
            return data.getState();
        }
        public void addStateListener(StateListener listener) {
            if (data.getState() == FINISHED)
                listener.stateChanged(FINISHED, getRowCount());
            else
                listeners.add(listener);
        }
        public void removeStateListener(StateListener listener) {
            listeners.remove(listener);
        }
        @SuppressWarnings("unchecked")
        public void stateChanged(int state, int row) {
            ArrayList<StateListener> al = (ArrayList<StateListener>) listeners.clone();
            for (int i = 0; i < al.size(); i++)
                al.get(i).stateChanged(state, row + (allowNull ? 1 : 0));
            if (state == FINISHED)
                listeners.clear();
        }
    }
}
