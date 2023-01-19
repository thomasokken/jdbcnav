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

import java.util.*;

import jdbcnav.model.Data;
import jdbcnav.model.Database;
import jdbcnav.model.ForeignKey;
import jdbcnav.model.Table;
import jdbcnav.util.MiscUtils;
import jdbcnav.util.NavigatorException;


public class MultiTableDiff {
    public static void populate(TableChangeHandler tch, Collection<Table> tablesColl,
                                boolean waitUntilReady)
                                                throws NavigatorException {
        new MultiTableDiff().diff2(tch, tablesColl, null, waitUntilReady, true);
    }

    public static void diff(TableChangeHandler tch, Collection<Table> oldColl,
                            Collection<Table> newColl, boolean waitUntilReady)
                                                throws NavigatorException {
        new MultiTableDiff().diff2(tch, oldColl, newColl, waitUntilReady, true);
    }

    public static void diff(TableChangeHandler tch, Collection<Table> oldColl,
                            Collection<Table> newColl, boolean waitUntilReady,
                            boolean returnNewTables)
                                                throws NavigatorException {
        new MultiTableDiff().diff2(tch, oldColl, newColl, waitUntilReady,
                                   returnNewTables);
    }

    private TreeMap<Table, ArrayList<Object[]>> oldcells;
    private TreeMap<Table, ArrayList<Object[]>> newcells;
    private ArrayList<Table> oldtables;
    private ArrayList<Table> newtables;

    private void diff2(TableChangeHandler tch, Collection<Table> oldColl,
                       Collection<Table> newColl, boolean waitUntilReady,
                       boolean returnNewTables) throws NavigatorException {

        // The waitUntilReady parameter specifies whether to wait for tables
        // that have a model to finish loading. If true, this method may block
        // for a long time; therefore, this parameter should only be 'true' if
        // this method is being called from a background thread, or else the
        // user interface may freeze for a long time, depending on the size of
        // the table loading operations that are in progress.

        // It should be OK to use waitUntilReady = false when using
        // MultiTableDiff to commit changes that were made interactively,
        // provided that there are no loads in progress while we're constucting
        // the diff (otherwise, there is a race condition because rows may be
        // loaded between us taking the 'old' and 'new' snapshots of a table).
        // It is our caller's responsibility to pause or cancel loads that are
        // in progress before calling us.

        oldcells = new TreeMap<Table, ArrayList<Object[]>>();
        newcells = new TreeMap<Table, ArrayList<Object[]>>();
        oldtables = new ArrayList<Table>();
        newtables = new ArrayList<Table>();
        Database olddb = null;
        Database newdb = null;

        for (Table t1 : oldColl) {
            if (olddb == null)
                olddb = t1.getDatabase();
            else if (olddb != t1.getDatabase())
                throw new IllegalArgumentException("All tables in a Table Set "
                                                + "must come from the same "
                                                + "Data Source.");

            if (newColl == null) {
                oldcells.put(t1, makeCellArray(t1, waitUntilReady, true));
                newcells.put(t1, makeCellArray(t1, waitUntilReady, false));
                oldtables.add(t1);
                newtables.add(t1);
            } else {
                // TODO: three things --
                // 1) Allow arbitrary mappings, i.e. user can select tables
                //    from two table sets and declare them to be correspondent,
                //    rather than relying on matching catalog/schema/name;
                // 2) Detect and handle non-mapped tables
                // 3) Even if (1) and (2) are a bit too ambitious, this code
                //    still needs to be fixed to handle the case that there
                //    is more than one table in 'oldtables' with the same
                //    unqualified name (since these names are used to match
                //    the 'old' and 'new' tables).
                Table t2 = null;
                for (Table t : newColl) {
                    if (t1.getName().equalsIgnoreCase(t.getName())) {
                        t2 = t;
                        break;
                    }
                }
                if (t2 != null) {
                    if (newdb == null)
                        newdb = t2.getDatabase();
                    else if (newdb != t2.getDatabase())
                        throw new IllegalArgumentException(
                                                  "All tables in a Table Set "
                                                + "must come from the same "
                                                + "Data Source.");
                    oldcells.put(t1, makeCellArray(t1, waitUntilReady, false));
                    newcells.put(t2, makeCellArray(t2, waitUntilReady, false));
                    oldtables.add(t1);
                    newtables.add(t2);
                }
            }
        }


        // NOTE:
        // The algorithm implemented here was designed for simplicity
        // and correctness. In the interest of efficiency, it may need
        // to be completely rewritten. The two main flaws of the
        // current implementation: deletes that would cause cascaded
        // deletes are not done (the cascaded deletes must be done
        // separately), which may cause unnecessary extra passes over
        // the table set; and no attempt is made to update primary
        // keys (rows are matched by primary key, so a change to a
        // primary key ends up generating a delete and an insert, plus
        // possibly lots and lots of updates to emulate cascaded
        // updates to foreign keys). The second flaw in particular
        // is nontrivial to correct: the current implementation assumes
        // that updates have only prerequisites, but never side
        // effects, and that is only true if primary keys are never
        // updated.
        // Having said all that, the current algorithm will always
        // generate correct SQL; it still remains to be proved if it
        // will always *succeed*, but that is an exercise in math that
        // I don't have time for at the moment, and so I assume and
        // hope that whatever brittleness this algorihm may have is
        // only triggered by pathological schemas.

        boolean notDone, didSomething;
        boolean postmortem = false;
        do {
            notDone = false;
            didSomething = false;
            Iterator<Table> iter1 = oldtables.iterator();
            Iterator<Table> iter2 = newtables.iterator();
            while (iter1.hasNext()) {
                Table t1 = iter1.next();
                Table t2 = iter2.next();
                ArrayList<Object[]> cells1 = oldcells.get(t1);
                ArrayList<Object[]> cells2 = newcells.get(t2);
                int rows1 = cells1.size();
                int rows2 = cells2.size();
                ArrayList<Object[]> deletedKey = new ArrayList<Object[]>();
                ArrayList<Object[]> updatedRow = new ArrayList<Object[]>();
                ArrayList<Object[]> insertedRow = new ArrayList<Object[]>();
                ArrayList<Integer> foundIndex = new ArrayList<Integer>();

                for (int j = 0; j < rows1; j++) {
                    Object[] key = indexToPK(t1, cells1, j);
                    int k = pkToIndex(t2, cells2, key);
                    if (k >= 0) {
                        Object[] row1 = indexToRow(cells1, j);
                        Object[] row2 = indexToRow(cells2, k);
                        if (!Arrays.equals(row1, row2))
                            updatedRow.add(row2);
                        foundIndex.add(k);
                    } else
                        deletedKey.add(key);
                }
                Collections.sort(foundIndex);
                for (int j = 0; j < rows2; j++) {
                    if (Collections.binarySearch(foundIndex, j) < 0)
                        insertedRow.add(indexToRow(cells2, j));
                }

                for (int j = 0; j < deletedKey.size(); j++) {
                    Object[] key = deletedKey.get(j);
                    boolean doit = false;
                    if (canDeleteKey(t1, key))
                        doit = true;
                    else {
                        notDone = true;
                        if (postmortem)
                            doit = true;
                    }
                    if (doit) {
                        tch.deleteRow(returnNewTables ? t2 : t1, key);
                        cells1.remove(pkToIndex(t1, cells1, key));
                        didSomething = true;
                    }
                }

                for (int j = 0; j < updatedRow.size(); j++) {
                    Object[] row = updatedRow.get(j);
                    boolean doit = false;
                    if (canUpdateRow(t1, cells1, row))
                        doit = true;
                    else {
                        notDone = true;
                        if (postmortem)
                            doit = true;
                    }
                    if (doit) {
                        int index = rowToIndex(t1, cells1, row);
                        Object[] oldRow = indexToRow(cells1, index);
                        tch.updateRow(returnNewTables ? t2 : t1, oldRow, row);
                        cells1.set(index, row);
                        didSomething = true;
                    }
                }

                for (int j = 0; j < insertedRow.size(); j++) {
                    Object[] row = insertedRow.get(j);
                    boolean doit = false;
                    if (canInsertRow(t1, cells1, row))
                        doit = true;
                    else {
                        notDone = true;
                        if (postmortem)
                            doit = true;
                    }
                    if (doit) {
                        // We know the row is new, so will not be found,
                        // but rowToIndex() will helpfully let us know
                        // where to insert the row so as to keep the list
                        // sorted.
                        int index = rowToIndex(t1, cells1, row);
                        if (index < 0)
                            index = -1 - index;
                        tch.insertRow(returnNewTables ? t2 : t1, row);
                        cells1.add(index, row);
                        didSomething = true;
                    }
                }
            }
            if (notDone && !didSomething && !postmortem) {
                if (!tch.continueAfterError())
                    // TableChangeHandler not interested
                    // in postmortem debugging
                    throw new NavigatorException(
                            "An internal error occurred trying to do the\n"
                            + "multi-table commit. This is a bug! Please\n"
                            + "send a bug report... And meanwhile, try\n"
                            + "committing your changes in smaller chunks.");
                postmortem = true;   // Dump remaining changes without
                                     // regard for constraints; we notify
                                     // the TableChangeHandler what we're
                                     // doing, so they know not to take the
                                     // generated output too seriously
                didSomething = true; // This to make sure we don't fall out
                                     // of the outer loop
            }
        } while (notDone && didSomething);
    }

    private static Table findTable(ArrayList<Table> set, String catalog, String schema,
                                                            String name) {
        for (Table t : set)
            if (MiscUtils.strEq(catalog, t.getCatalog())
                    && MiscUtils.strEq(schema, t.getSchema())
                    && MiscUtils.strEq(name, t.getName()))
                return t;
        return null;
    }

    private static ArrayList<Object[]> makeCellArray(Table table, boolean waitUntilReady,
                                    boolean empty) throws NavigatorException {
        ArrayList<Object[]> cells = new ArrayList<Object[]>();
        if (!empty) {
            ResultSetTableModel model = table.getModel();
            if (model != null) {
                model.stopEditing();
                if (waitUntilReady)
                    model.waitUntilReady();
                int columns = model.getColumnCount();
                for (int i = 0; i < model.getRowCount(); i++) {
                    Object[] row = new Object[columns];
                    for (int j = 0; j < columns; j++)
                        row[j] = model.getValueAt(i, j);
                    cells.add(row);
                }
            } else {
                Data data = table.getData(false);
                int columns = data.getColumnCount();
                for (int i = 0; i < data.getRowCount(); i++) {
                    Object[] row = new Object[columns];
                    for (int j = 0; j < columns; j++)
                        row[j] = data.getValueAt(i, j);
                    cells.add(row);
                }
            }
            Collections.sort(cells, new RowComparator(table));
        }
        return cells;
    }

    private static class RowComparator implements Comparator<Object[]> {
        private int[] key;
        public RowComparator(Table table) throws NavigatorException {
            key = table.getPKColumns();
        }
        public int compare(Object[] a, Object[] b) {
            for (int i = 0; i < key.length; i++) {
                int col = key[i];
                int res = MiscUtils.compareObjects(a[col], b[col], false);
                if (res != 0)
                    return res;
            }
            return 0;
        }
    }

    private static Object[] indexToPK(Table table, ArrayList<Object[]> cells, int index)
                                                throws NavigatorException {
        int[] key = table.getPKColumns();
        Object[] res = new Object[key.length];
        Object[] row = cells.get(index);
        for (int i = 0; i < key.length; i++)
            res[i] = row[key[i]];
        return res;
    }

    private static int pkToIndex(Table table, ArrayList<Object[]> cells, Object[] key)
                                                throws NavigatorException {
        Object[] row = new Object[table.getColumnCount()];
        int[] pkColumns = table.getPKColumns();
        for (int i = 0; i < pkColumns.length; i++)
            row[pkColumns[i]] = key[i];
        return rowToIndex(table, cells, row);
    }

    private static Object[] indexToRow(ArrayList<Object[]> cells, int index) {
        return cells.get(index);
    }

    private static int rowToIndex(Table table, ArrayList<Object[]> cells, Object[] row)
                                                    throws NavigatorException {
        return Collections.binarySearch(cells, row, new RowComparator(table));
    }

    private static int[] rkToIndexes(Table thisTable, Table thatTable,
                            ArrayList<Object[]> thatCells, int rkIndex, Object[] key)
                                                    throws NavigatorException {
        int[] rkColumns = thisTable.getRKColumns(rkIndex, thatTable);
        ArrayList<Integer> al = new ArrayList<Integer>();
        outer:
        for (int i = 0; i < thatCells.size(); i++) {
            Object[] row = thatCells.get(i);
            for (int j = 0; j < rkColumns.length; j++) {
                Object o = row[rkColumns[j]];
                if (o == null ? key[j] != null : !o.equals(key[j]))
                    continue outer;
            }
            al.add(i);
        }
        int sz = al.size();
        int[] res = new int[sz];
        for (int i = 0; i < sz; i++)
            res[i] = al.get(i);
        return res;
    }

    private boolean canDeleteKey(Table table, Object[] key)
                                            throws NavigatorException {
        ForeignKey[] rks = table.getReferencingKeys();
        for (int i = 0; i < rks.length; i++) {
            ForeignKey rk = rks[i];
            Table t = findTable(oldtables, rk.getThatCatalog(),
                                           rk.getThatSchema(),
                                           rk.getThatName());
            if (t == null)
                // Table is not in table set; ignore this constraint
                continue;
            ArrayList<Object[]> cells = oldcells.get(t);
            int[] index2 = rkToIndexes(table, t, cells, i, key);
            if (index2.length == 0)
                // No matching rows
                continue;
            else
                return false;
        }
        return true;
    }

    private boolean canUpdateRow(Table table, ArrayList<Object[]> cells,
                                    Object[] row) throws NavigatorException {
        if (rowToIndex(table, cells, row) < 0)
            return false;
        return checkForeignKeys(table, row);
    }

    private boolean canInsertRow(Table table, ArrayList<Object[]> cells,
                                    Object[] row) throws NavigatorException {
        // Check the primary key for non-nullness and uniqueness
        // *only* if it is a real primary key; we must not perform
        // this check if we're using a surrogate primary key!
        if (table.getPrimaryKey() != null) {
            int[] pkColumns = table.getPKColumns();
            for (int i = 0; i < pkColumns.length; i++) {
                if (row[pkColumns[i]] == null)
                    // Should never happen
                    return false;
            }
            if (rowToIndex(table, cells, row) >= 0)
                return false;
        }

        return checkForeignKeys(table, row);
    }

    private boolean checkForeignKeys(Table table, Object[] row)
                                                    throws NavigatorException {
        ForeignKey[] fks = table.getForeignKeys();
        for (int i = 0; i < fks.length; i++) {
            ForeignKey fk = fks[i];
            Table t = findTable(oldtables, fk.getThatCatalog(),
                                           fk.getThatSchema(),
                                           fk.getThatName());
            if (t == null)
                // Table is not in table set; ignore this constraint
                continue;
            int[] fkColumns = table.getFKColumns(i, t);
            boolean keyIsNull = true;
            Object[] key = new Object[fkColumns.length];
            for (int j = 0; j < fkColumns.length; j++) {
                Object o = row[fkColumns[j]];
                key[j] = o;
                if (o != null)
                    keyIsNull = false;
            }
            if (keyIsNull)
                continue;
            ArrayList<Object[]> c = oldcells.get(t);
            int index2 = pkToIndex(t, c, key);
            if (index2 < 0)
                // No matching rows
                return false;
            else
                continue;
        }
        return true;
    }
}
