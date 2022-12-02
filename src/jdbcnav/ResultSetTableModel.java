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

package jdbcnav;

import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

import jdbcnav.model.*;
import jdbcnav.util.*;


// TODO: when deleting or updating primary key values in a table that
// is referenced by others, check if any of the referencing tables are
// currently being displayed and do something (warn the user, or update
// them automagically if possible).

public class ResultSetTableModel extends AbstractTableModel
                            implements SortedTableModel, TypeSpecTableModel {
    
    public static final int GENTLE = 0;
    public static final int ASSERTIVE = 1;
    public static final int RUDE = 2;
    public static final int VICIOUS = 3;
    private static final Object UNSET = new Object();

    private Table dbTable;
    private Data data;
    private Data.StateListener datastatelistener;
    private int datatotallength;
    private MyTable table;
    private String[] headers;
    private TypeSpec[] specs;
    private ArrayList<Object[]> cells = new ArrayList<Object[]>();
    private ArrayList<Integer> sequence = new ArrayList<Integer>();
    private int columns;
    private int colIndex[];
    private int[] sortPriority;
    private boolean[] sortAscending;
    private boolean editable;
    private ArrayList<Object[]> original;
    private ArrayList<UndoListener> undoListeners;
    private ArrayList<Edit> undoStack;
    private int undoStackIndex = -1;
    
    public ResultSetTableModel(Data data, Table dbTable) {
        this.dbTable = dbTable;
        editable = dbTable != null && dbTable.isEditable();
        if (editable) {
            original = new ArrayList<Object[]>();
            undoListeners = new ArrayList<UndoListener>();
            undoStack = new ArrayList<Edit>();
        }
        columns = data.getColumnCount();
        colIndex = new int[columns];
        headers = new String[columns];
        specs = new TypeSpec[columns];
        for (int i = 0; i < columns; i++) {
            colIndex[i] = i + 1;
            headers[i] = data.getColumnName(i);
            if (dbTable != null)
                specs[i] = dbTable.getTypeSpecs()[i];
            else
                specs[i] = data.getTypeSpec(i);
        }
        sortPriority = new int[columns];
        sortAscending = new boolean[columns];
        for (int i = 0; i < columns; i++) {
            sortPriority[i] = i;
            sortAscending[i] = true;
        }
        if (dbTable != null && dbTable.getPrimaryKey() != null) {
            PrimaryKey pk = dbTable.getPrimaryKey();
            for (int i = 0; i < pk.getColumnCount(); i++)
                for (int j = 0; j < headers.length; j++)
                    if (pk.getColumnName(i).equalsIgnoreCase(headers[j])) {
                        int sp = j;
                        int k = i;
                        do {
                            int tmp = sortPriority[k];
                            sortPriority[k] = sp;
                            sp = tmp;
                            k++;
                        } while (sp != j);
                        break;
                    }
        }
        
        load(data);
    }


    public synchronized void setState(int state) {
        if (data != null)
            data.setState(state);
    }

    public synchronized int getState() {
        if (data != null)
            return data.getState();
        else
            return Data.FINISHED;
    }

    public synchronized void addStateListener(Data.StateListener listener) {
        if (data != null)
            data.addStateListener(listener);
        else
            listener.stateChanged(Data.FINISHED, datatotallength);
    }

    public synchronized void removeStateListener(Data.StateListener listener) {
        if (data != null)
            data.removeStateListener(listener);
    }

    /**
     * This method will block until the underlying Data object is finished.
     * Do not call this from the AWT Event thread!
     */
    public synchronized void waitUntilReady() {
        while (data != null)
            try {
                wait();
            } catch (InterruptedException e) {}
    }

    public synchronized void setTable(MyTable table) {
        this.table = table;
    }

    public synchronized void stopEditing() {
        if (table != null)
            table.stopEditing();
    }

    public synchronized void load(Data data) {
        if (datastatelistener != null)
            this.data.setState(Data.FINISHED);
        cells.clear();
        sequence.clear();
        if (editable) {
            original.clear();
            clearUndoStack();
        }
        this.data = data;
        datastatelistener = new DataStateListener();
        data.addStateListener(datastatelistener);
    }

    private class DataStateListener implements Data.StateListener {
        private int lastrows = 0;
        public void stateChanged(int state, int rows) {
            synchronized (ResultSetTableModel.this) {
                if (rows > lastrows) {
                    int first = sequence.size();
                    for (int i = lastrows; i < rows; i++) {
                        Object[] row = new Object[columns];
                        for (int j = 0; j < columns; j++)
                            row[j] = data.getValueAt(i, j);
                        cells.add(row);
                        sequence.add(cells.size() - 1);
                        if (editable)
                            original.add(row.clone());
                    }
                    int last = sequence.size() - 1;
                    SwingUtilities.invokeLater(
                                new TableRowsInsertedNotifier(first, last));
                    lastrows =  rows;
                }
                if (state == Data.FINISHED) {
                    datatotallength = data.getRowCount();
                    datastatelistener = null;
                    data = null;
                    // Allow threads blocked in waitUntilReady() to proceed
                    ResultSetTableModel.this.notifyAll();
                }
            }
        }
    }

    private class TableRowsInsertedNotifier implements Runnable {
        private int first, last;
        public TableRowsInsertedNotifier(int first, int last) {
            this.first = first;
            this.last = last;
        }
        public void run() {
            safelyFireTableRowsInserted(first, last);
        }
    }

    public synchronized Data getOriginalData() {
        BasicData origdata = new BasicData();
        origdata.setColumnNames(headers);
        TypeSpec[] typeSpecs = new TypeSpec[columns];
        for (int i = 0; i < columns; i++)
            typeSpecs[i] = specs[i];
        origdata.setTypeSpecs(typeSpecs);
        origdata.setData(original);
        return origdata;
    }
    
    public synchronized int getRowCount() {
        return sequence.size();
    }
    
    public synchronized int getColumnCount() {
        return columns;
    }
    
    public synchronized Object getValueAt(int row, int column) {
        int realRow = sequence.get(row);
        return cells.get(realRow)[column];
    }
    
    public synchronized String getColumnName(int column) {
        return headers[column];
    }
    
    public synchronized Class<?> getColumnClass(int column) {
        TypeSpec spec = specs[column];
        if (spec.type == TypeSpec.CLASS)
            return spec.jdbcJavaClass;
        else
            return TypeSpec.class;
    }

    public synchronized TypeSpec getTypeSpec(int column) {
        return specs[column];
    }

    public synchronized boolean isCellEditable(int row, int column) {
        if (!editable)
            return false;
        int type = specs[column].type;
        return type != TypeSpec.LONGVARCHAR
            && type != TypeSpec.LONGVARNCHAR
            && type != TypeSpec.RAW
            && type != TypeSpec.VARRAW
            && type != TypeSpec.LONGVARRAW
            && type != TypeSpec.UNKNOWN;
    }

    public synchronized void setValueAt(Object value, int row, int column) {
        setValueAt(value, row, column, "Change Cell");
    }

    public synchronized void setValueAt(Object value, int row, int column,
                                                                String why) {
        if (!editable)
            return;
        int realRow = sequence.get(row);
        Object[] currRow = cells.get(realRow);
        Object prev = currRow[column];

        if (value instanceof String) {
            TypeSpec spec = specs[column];
            if (!java.sql.Clob.class.isAssignableFrom(spec.jdbcJavaClass))
                try {
                    value = spec.stringToObject((String) value);
                } catch (IllegalArgumentException e) {}
        }

        if (value == null ? prev == null : value.equals(prev))
            return;
        currRow[column] = value;
        editHappened(new SingleCellEdit(realRow, column, prev, value, why));
        safelyFireTableCellUpdated(row, column);
    }

    public synchronized void setValuesAt(String[][] values, int row, int[] columns,
                                                                String why) {
        if (!editable)
            return;
        Object[][] before = new Object[values.length][values[0].length];
        Object[][] after = new Object[values.length][values[0].length];
        int[] rows = new int[values.length];
        for (int r = 0; r < values.length; r++) {
            rows[r] = sequence.get(r + row);
            Object[] currRow = cells.get(rows[r]);
            for (int c = 0; c < values[0].length; c++) {
                int col = columns[c];
                before[r][c] = currRow[col];
                TypeSpec spec = specs[col];
                Object value = null;
                if (!java.sql.Clob.class.isAssignableFrom(spec.jdbcJavaClass))
                    try {
                        value = spec.stringToObject(values[r][c]);
                    } catch (IllegalArgumentException e) {}
                after[r][c] = value;
            }
        }
        Edit edit = new MultiCellEdit(rows, columns, before, after, why);
        editHappened(edit);
        edit.redo();
    }

    public synchronized String[] getHeaders() {
        return headers.clone();
    }

    public synchronized boolean isDirty() {
        return canUndo();
    }

    public void commit(TableChangeHandler tch) throws NavigatorException {

        // This method is called by JDBCDatabase when commitTables() is
        // called with only one table to commit. This code is more efficient
        // than the code in MultiTableDiff, since it can take advantage of
        // inside knowledge of ResultSetTableModel to generate the list of
        // required changes efficiently.
        // (TODO: it would be better to return a list of change actions,
        // without executing them here; MultiTableDiff could thus let
        // ResultSetTableModel generate lists of changes efficiently *even* in
        // a multi-table commit, and then use a graph sorting algorithm to
        // reorder them to resolve dependencies.
        // On the other hand, we can't get rid of the brute force diffing code
        // in MultiTableDiff altogether, since it is needed when diffing tables
        // that are truly distinct (as opposed to diffing old and new versions
        // of the same table, as we do on commit, or diffing actual and empty
        // versions of the same table, as we do when generating a populate
        // script).

        ArrayList<Object[]> deleted = new ArrayList<Object[]>();
        ArrayList<Object[]> modified = new ArrayList<Object[]>();
        ArrayList<Object[]> modifiedBak = new ArrayList<Object[]>();
        ArrayList<Object[]> inserted = new ArrayList<Object[]>();
        int currRows = cells.size();
        int prevRows = original.size();
        for (int i = 0; i < prevRows; i++) {
            if (sequence.indexOf(i) == -1)
                deleted.add(original.get(i));
            else {
                Object[] current = cells.get(i);
                Object[] backup = original.get(i);
                for (int j = 0; j < columns; j++)
                    if (backup[j] == null ? current[j] != null
                                          : !backup[j].equals(current[j])) {
                        modified.add(current);
                        modifiedBak.add(backup);
                        break;
                    }
            }
        }
        for (int i = prevRows; i < currRows; i++)
            if (sequence.indexOf(i) != -1)
                inserted.add(cells.get(i));

        PrimaryKey dpk = dbTable.getPrimaryKey();
        String[] pk;
        int pkLength;
        if (dpk == null) {
            pk = headers;
            pkLength = columns;
        } else {
            pkLength = dpk.getColumnCount();
            pk = new String[pkLength];
            for (int i = 0; i < pkLength; i++)
                pk[i] = dpk.getColumnName(i);
        }
        int[] pkIndex = new int[pkLength];
        for (int i = 0; i < pkLength; i++)
            for (int j = 0; j < columns; j++)
                if (pk[i].equalsIgnoreCase(headers[j])) {
                    pkIndex[i] = j;
                    break;
                }

        for (int i = 0; i < deleted.size(); i++) {
            Object[] row = deleted.get(i);
            Object[] key = new Object[pk.length];
            for (int j = 0; j < key.length; j++)
                key[j] = row[pkIndex[j]];
            tch.deleteRow(dbTable, key);
        }
        for (int i = 0; i < modified.size(); i++) {
            Object[] row = modified.get(i);
            Object[] bak = modifiedBak.get(i);
            tch.updateRow(dbTable, bak, row);
        }
        for (int i = 0; i < inserted.size(); i++) {
            Object[] row = inserted.get(i);
            tch.insertRow(dbTable, row);
        }
    }

    public synchronized void postCommit() {
        // This is called "post" commit, because the actual work of committing
        // changes is done by Database (with help from MultiTableCommit).
        if (!isDirty())
            return;
        clearUndoStack();
        ArrayList<Object[]> newCells = new ArrayList<Object[]>();
        original.clear();
        for (int i = 0; i < sequence.size(); i++) {
            int row = sequence.get(i);
            Object[] current = cells.get(row);
            newCells.add(current);
            sequence.set(i, i);
            original.add(current.clone());
        }
        cells = newCells;
        // Just for good measure, and to get the commit/rollback menu
        // items to be disabled
        safelyFireTableDataChanged();
    }

    public synchronized void rollback() {
        if (!isDirty())
            return;
        int rows = original.size();
        sequence.clear();
        for (int i = 0; i < rows; i++) {
            sequence.add(i);
            cells.set(i, original.get(i).clone());
        }
        for (int i = cells.size() - 1; i >= rows; i--)
            cells.remove(i);
        clearUndoStack();
        Collections.sort(sequence, rowComparator);
        safelyFireTableDataChanged();
    }
    
    public synchronized void sortColumn(int col) {
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
        Collections.sort(sequence, rowComparator);
        safelyFireTableDataChanged();
    }

    public synchronized int getSortedColumn() {
        return sortPriority[0];
    }

    public synchronized void sort() {
        Collections.sort(sequence, rowComparator);
        safelyFireTableDataChanged();
    }

    public synchronized void selectionFromViewToModel(int[] selection) {
        int len = selection.length;
        if (len == 0)
            return;
        for (int i = 0; i < len; i++)
            selection[i] = sequence.get(selection[i]);
        Arrays.sort(selection);
    }

    public synchronized void selectionFromModelToView(int[] selection) {
        int len = selection.length;
        if (len == 0)
            return;
        int seqlen = sequence.size();
        int[] reverseSequence = new int[seqlen];
        for (int i = 0; i < seqlen; i++)
            reverseSequence[sequence.get(i)] = i;
        for (int i = 0; i < len; i++)
            selection[i] = reverseSequence[selection[i]];
        Arrays.sort(selection);
    }

    public interface UndoListener {
        public void undoRedoTitleChanged();
    }

    public synchronized void addUndoListener(UndoListener c) {
        undoListeners.add(c);
    }

    public synchronized void removeUndoListener(UndoListener c) {
        undoListeners.remove(c);
    }

    public synchronized String getUndoTitle() {
        if (undoStackIndex == -1)
            return null;
        Edit e = undoStack.get(undoStackIndex);
        return e.getUndoTitle();
    }

    public synchronized String getRedoTitle() {
        if (undoStackIndex >= undoStack.size() - 1)
            return null;
        Edit e = undoStack.get(undoStackIndex + 1);
        return e.getRedoTitle();
    }

    private synchronized void undoRedoTitleChanged() {
        for (UndoListener listener : undoListeners)
            listener.undoRedoTitleChanged();
    }

    private synchronized boolean canUndo() {
        return undoStackIndex != -1;
    }

    private synchronized boolean canRedo() {
        return undoStackIndex < undoStack.size() - 1;
    }
    
    public synchronized void undo() {
        if (canUndo()) {
            Edit e = undoStack.get(undoStackIndex--);
            e.undo();
            undoRedoTitleChanged();
        }
    }

    public synchronized void redo() {
        if (canRedo()) {
            Edit e = undoStack.get(++undoStackIndex);
            e.redo();
            undoRedoTitleChanged();
        }
    }

    public synchronized void insertRow(int viewRow) {
        cells.add(new Object[columns]);
        int modelRow = cells.size() - 1;
        int beforeModelRow;
        if (viewRow == -1) {
            viewRow = sequence.size() - 1;
            beforeModelRow = -1;
            sequence.add(modelRow);
        } else {
            beforeModelRow = sequence.get(viewRow);
            sequence.add(viewRow, modelRow);
        }
        editHappened(new InsertRowEdit(beforeModelRow));
        safelyFireTableRowsInserted(viewRow, viewRow);
    }

    public synchronized void deleteRow(int[] rows, boolean cut) {
        Arrays.sort(rows);
        int length = rows.length;
        if (length == 0)
            return;
        int[] precedes = new int[length];
        int[] removed = new int[length];
        for (int i = 0; i < length; i++) {
            int following = rows[i] + 1;
            if (following == sequence.size())
                precedes[i] = -1;
            else
                precedes[i] = sequence.get(following);
            removed[i] = sequence.get(rows[i]);
        }
        for (int i = length - 1; i >= 0; i--)
            sequence.remove(rows[i]);
        editHappened(new DeleteRowEdit(removed, precedes, cut));
        int first = rows[0];
        int last = rows[length - 1];
        if (last - first == length - 1)
            safelyFireTableRowsDeleted(first, last);
        else
            safelyFireTableDataChanged();
    }

    @SuppressWarnings("unchecked")
    public synchronized void pasteRow(Object[][] grid,
                                        String[] mapping, boolean setNull) {
        Object[] colIndex = new Object[grid[0].length];
        PrimaryKey dpk = dbTable.getPrimaryKey();
        int[] pkIndex = dpk == null ? null : new int[dpk.getColumnCount()];
        int pkFound = 0;

        for (int col = 0; col < columns; col++) {
            String clipColName = mapping[col];
            if (clipColName == null)
                continue;
            for (int i = 0; i < grid[0].length; i++)
                if (clipColName.equalsIgnoreCase((String) grid[0][i])) {
                    Object idx = colIndex[i];
                    if (idx == null)
                        colIndex[i] = col;
                    else if (idx instanceof Integer) {
                        ArrayList<Integer> al = new ArrayList<Integer>();
                        al.add((Integer) idx);
                        al.add(col);
                        colIndex[i] = al;
                    } else
                        ((ArrayList<Integer>) colIndex[i]).add(col);
                }
            if (dpk != null)
                for (int i = 0; i < dpk.getColumnCount(); i++)
                    if (dpk.getColumnName(i).equalsIgnoreCase(headers[col])) {
                        pkIndex[i] = col;
                        pkFound++;
                        break;
                    }
        }
        if (dpk != null && pkFound < dpk.getColumnCount())
            pkIndex = null;

        ArrayList<Object[]> pasted = new ArrayList<Object[]>();
        for (int i = 3; i < grid.length; i++) {
            Object[] row = new Object[columns];
            if (!setNull)
                for (int j = 0; j < columns; j++)
                    row[j] = UNSET;
            for (int j = 0; j < grid[0].length; j++) {
                Object idx = colIndex[j];
                if (idx == null)
                    continue;
                else if (idx instanceof Integer)
                    row[(Integer) idx] = grid[i][j];
                else {
                    Object o = grid[i][j];
                    for (int n : (ArrayList<Integer>) idx)
                        row[n] = o;
                }
            }
            pasted.add(row);
        }

        Edit edit = new PasteRowEdit(pasted, pkIndex, setNull);
        editHappened(edit);
        edit.redo();
    }

    private synchronized void editHappened(Edit e) {
        for (int i = undoStack.size() - 1; i > undoStackIndex; i--)
            undoStack.remove(i);
        undoStack.add(e);
        undoStackIndex++;
        undoRedoTitleChanged();
    }

    private synchronized void clearUndoStack() {
        undoStack.clear();
        undoStackIndex = -1;
        undoRedoTitleChanged();
    }

    private RowComparator rowComparator = new RowComparator();

    private class RowComparator implements Comparator<Integer> {
        public int compare(Integer A, Integer B) {
            Object[] a = cells.get(A);
            Object[] b = cells.get(B);
            if (a == null)
                return b == null ? 0 :
                        sortAscending[sortPriority[0]] ? 1 : -1;
            else if (b == null)
                return sortAscending[sortPriority[0]] ? -1 : 1;
            for (int i = 0; i < sortPriority.length; i++) {
                int col = sortPriority[i];
                int res = MiscUtils.compareObjects(a[col], b[col], true);
                if (res != 0)
                    return sortAscending[col] ? res : -res;
            }
            return 0;
        }
    }

    public void export(File file, boolean printColumnNames) {
        if (file.exists()) {
            Toolkit.getDefaultToolkit().beep();
            if (JOptionPane.showInternalConfirmDialog(
                            Main.getDesktop(),
                            "Overwrite existing " + file.getName() + "?",
                            "Confirm", JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE)
                                    != JOptionPane.OK_OPTION)
                return;
        }
        try {
            synchronized (this) {
                PrintWriter pw = new PrintWriter(new FileOutputStream(file));
                int columns = getColumnCount();
                if (printColumnNames) {
                    for (int i = 0; i < columns; i++) {
                        pw.print(quote(getColumnName(i)));
                        if (i < columns - 1)
                            pw.print(",");
                        else
                            pw.println();
                    }
                }

                int rows = getRowCount();
                Class<?> byteArrayClass = new byte[1].getClass();
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < columns; j++) {
                        Object o = getValueAt(i, j);
                        if (o != null) {
                            if (o instanceof BlobWrapper)
                                o = ((BlobWrapper) o).load();
                            else if (o instanceof ClobWrapper)
                                o = ((ClobWrapper) o).load();
                            Class<?> k = specs[j].jdbcJavaClass;
                            String s;
                            if (k == String.class
                                    || java.sql.Clob.class.isAssignableFrom(k))
                                s = quote((String) o);
                            else if (k == byteArrayClass
                                    || java.sql.Blob.class.isAssignableFrom(k))
                                s = FileUtils.byteArrayToBase64((byte[]) o);
                            else {
                                s = specs[j].objectToString(o);
                                if (!Number.class.isAssignableFrom(k))
                                    s = quote(s);
                            }
                            pw.print(s);
                        }
                        if (j < columns - 1)
                            pw.print(",");
                        else
                            pw.println();
                    }
                }
                
                pw.close();
            }
        } catch (IOException e) {
            MessageBox.show("Export failed.", e);
        }
    }

    public synchronized void doImport(File file, int importMode,
                                                String[] colName) {
        LineNumberReader in = null;
        ArrayList<Object[]> imported = new ArrayList<Object[]>();
        int[] pkIndex = null;

        try {
            in = new LineNumberReader(new FileReader(file));
            String line;
            CSVTokenizer tok;
            if (colName == null) {
                line = readLines(in);
                tok = new CSVTokenizer(line);
                ArrayList<String> al = new ArrayList<String>();
                while (tok.hasMoreTokens())
                    al.add(tok.nextToken());
                colName = al.toArray(new String[al.size()]);
            }
            int[] colIndex = new int[colName.length];
            for (int i = 0; i < colName.length; i++) {
                colIndex[i] = -1;
                String cn = colName[i];
                for (int j = 0; j < columns; j++)
                    if (cn.equalsIgnoreCase(headers[j])) {
                        colIndex[i] = j;
                        break;
                    }
            }

            PrimaryKey dpk = dbTable.getPrimaryKey();
            if (dpk != null) {
                int n = dpk.getColumnCount();
                String[] pk = new String[n];
                for (int i = 0; i < n; i++)
                    pk[i] = dpk.getColumnName(i);
                pkIndex = new int[pk.length];
                int found = 0;
                for (int i = 0; i < pk.length; i++) {
                    String cn = pk[i];
                    for (int j = 0; j < colName.length; j++)
                        if (cn.equalsIgnoreCase(colName[j])) {
                            pkIndex[i] = colIndex[j];
                            found++;
                            break;
                        }
                }
                if (found < pk.length)
                    pkIndex = null;
            }

            TypeSpec[] specs = new TypeSpec[columns];
            for (int i = 0; i < columns; i++)
                specs[i] = getTypeSpec(i);
            Class<?> byteArrayClass = new byte[1].getClass();

            while ((line = readLines(in)) != null) {
                Object[] row = new Object[columns];
                if (importMode == ASSERTIVE) {
                    // In "assertive" mode, we overwrite existing rows -- but
                    // only the fields that we actually read from the file.
                    // So, we must have a way to distinguish, while actually
                    // doing/redoing the edit, between nulls that we actually
                    // read from the file, and cells that were not read.
                    for (int i = 0; i < columns; i++)
                        row[i] = UNSET;
                }
                tok = new CSVTokenizer(line);
                int index = -1;
                while (tok.hasMoreTokens() && ++index < colIndex.length) {
                    String s = tok.nextToken();
                    int i = colIndex[index];
                    if (i == -1)
                        // Column in imported file does not exist in table
                        continue;
                    Class<?> k = specs[i].jdbcJavaClass;
                    if (k == String.class
                            || java.sql.Clob.class.isAssignableFrom(k))
                        row[i] = s;
                    else if (k == byteArrayClass
                            || java.sql.Blob.class.isAssignableFrom(k))
                        row[i] = FileUtils.base64ToByteArray(s);
                    else
                        try {
                            row[i] = specs[i].stringToObject(s);
                        } catch (IllegalArgumentException e) {
                            if (!specs[i].jdbcJavaType.startsWith("java.")) {
                                // Probably a DB-specific type; we just put
                                // the String version into the model and
                                // hope for the best.
                                row[i] = s;
                            } else {
                                // If instantiating a standard java class
                                // fails, we have a bad input file, and we
                                // really should complain.throw
                                MessageBox.show("Bad value in CSV file (row "
                                        + in.getLineNumber() + ", column "
                                        + (index + 1) + ").", e);
                                return;
                            }
                        }

                }
                imported.add(row);
            }
        } catch (IOException e) {
            MessageBox.show("I/O error while trying to import file "
                            + file.getName() + " into table "
                            + dbTable.getName() + ".", e);
            return;
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {}
        }

        Edit edit;
        if (importMode == VICIOUS)
            edit = new ViciousImportEdit(imported);
        else if (importMode == RUDE)
            edit = new AssertiveImportEdit(imported, pkIndex, true);
        else if (importMode == ASSERTIVE)
            edit = new AssertiveImportEdit(imported, pkIndex, false);
        else // importMode == GENTLE
            edit = new GentleImportEdit(imported, pkIndex);
        editHappened(edit);
        edit.redo();
    }

    private static String quote(String s) {
        StringBuffer buf = new StringBuffer("\"");
        StringTokenizer tok = new StringTokenizer(s, "\"\\\r\n", true);
        while (tok.hasMoreTokens()) {
            String t = tok.nextToken();
            if (t.equals("\""))
                buf.append("\"\"");
            else if (t.equals("\\"))
                buf.append("\\\\");
            else if (t.equals("\r"))
                buf.append("\\r");
            else if (t.equals("\n"))
                buf.append("\\n");
            else
                buf.append(t);
        }
        buf.append("\"");
        return buf.toString();
    }

    private static boolean rowEqual(Object[] a, Object[] b, int[] key) {
        for (int i = 0; i < key.length; i++) {
            int j = key[i];
            Object A = a[j];
            Object B = b[j];
            if (A == null ? B != null : !A.equals(B))
                return false;
        }
        return true;
    }

    private interface Edit {
        public String getUndoTitle();
        public String getRedoTitle();
        public void undo();
        public void redo();
    }

    private class SingleCellEdit implements Edit {
        private int row;
        private int column;
        private Object before;
        private Object after;
        private String description;

        public SingleCellEdit(int row, int column, Object before, Object after,
                              String description) {
            this.row = row;
            this.column = column;
            this.before = before;
            this.after = after;
            this.description = description;
        }

        public String getUndoTitle() {
            return "Undo " + description;
        }

        public String getRedoTitle() {
            return "Redo " + description;
        }

        public void undo() {
            cells.get(row)[column] = before;
            safelyFireTableCellUpdated(sequence.indexOf(row), column);
        }

        public void redo() {
            cells.get(row)[column] = after;
            safelyFireTableCellUpdated(sequence.indexOf(row), column);
        }
    }

    private class MultiCellEdit implements Edit {
        private int[] rows;
        private int[] columns;
        private Object[][] before;
        private Object[][] after;
        private String description;

        public MultiCellEdit(int[] rows, int[] columns, Object[][] before, Object[][] after,
                              String description) {
            this.rows = rows.clone();
            this.columns = columns;
            this.before = before.clone();
            this.after = after.clone();
            this.description = description;
        }

        public String getUndoTitle() {
            return "Undo " + description;
        }

        public String getRedoTitle() {
            return "Redo " + description;
        }

        public void undo() {
            for (int r = 0; r < rows.length; r++) {
                int row = rows[r];
                for (int c = 0; c < before[0].length; c++) {
                    int col = columns[c];
                    cells.get(row)[col] = before[r][c];
                }
            }
            safelyFireTableDataChanged();
        }

        public void redo() {
            for (int r = 0; r < rows.length; r++) {
                int row = rows[r];
                for (int c = 0; c < after[0].length; c++) {
                    int col = columns[c];
                    cells.get(row)[col] = after[r][c];
                }
            }
            safelyFireTableDataChanged();
        }
    }

    private class InsertRowEdit implements Edit {
        private int row;

        public InsertRowEdit(int row) {
            this.row = row;
        }

        public String getUndoTitle() {
            return "Undo Insert Row";
        }

        public String getRedoTitle() {
            return "Redo Insert Row";
        }

        public void undo() {
            int index = cells.size() - 1;
            int row = sequence.indexOf(index);
            sequence.remove(row);
            cells.remove(index);
            safelyFireTableRowsDeleted(row, row);
        }

        public void redo() {
            cells.add(new Object[columns]);
            int modelRow = cells.size() - 1;
            int viewRow;
            if (row == -1) {
                viewRow = sequence.size() - 1;
                sequence.add(modelRow);
            } else {
                viewRow = sequence.indexOf(row);
                sequence.add(viewRow, modelRow);
            }
            safelyFireTableRowsInserted(viewRow, viewRow);
        }
    }

    private class DeleteRowEdit implements Edit {
        private int[] removed;
        private int[] precedes;
        private boolean cut;

        public DeleteRowEdit(int[] removed, int[] precedes, boolean cut) {
            this.removed = removed;
            this.precedes = precedes;
            this.cut = cut;
        }

        public String getUndoTitle() {
            return cut ? removed.length == 1 ? "Undo Cut Row"
                                             : "Undo Cut Rows"
                       : removed.length == 1 ? "Undo Delete Row"
                                             : "Undo Delete Rows";
        }

        public String getRedoTitle() {
            return cut ? removed.length == 1 ? "Redo Cut Row"
                                             : "Redo Cut Rows"
                       : removed.length == 1 ? "Redo Delete Row"
                                             : "Redo Delete Rows";
        }

        public void undo() {
            for (int i = removed.length - 1; i >= 0; i--) {
                int insertPos = precedes[i];
                if (insertPos == -1)
                    sequence.add(removed[i]);
                else {
                    insertPos = sequence.indexOf(insertPos);
                    sequence.add(insertPos, removed[i]);
                }
            }
            safelyFireTableDataChanged();
        }

        public void redo() {
            sequence.removeAll(new ArrayCollection<Integer>(removed));
            safelyFireTableDataChanged();
        }
    }

    private class PasteRowEdit extends AssertiveImportEdit {
        private boolean plural;
        public PasteRowEdit(ArrayList<Object[]> pasted, int[] pkIndex, boolean setNull) {
            super(pasted, pkIndex, setNull);
            plural = pasted.size() != 1;
        }
        public String getUndoTitle() {
            return plural ? "Undo Paste Rows" : "Undo Paste Row";
        }
        public String getRedoTitle() {
            return plural ? "Redo Paste Rows" : "Redo Paste Row";
        }
    }

    private class ViciousImportEdit implements Edit {
        private ArrayList<Integer> seq;
        private ArrayList<Object[]> imports;
        @SuppressWarnings("unchecked")
        public ViciousImportEdit(ArrayList<Object[]> imports) {
            seq = (ArrayList<Integer>) sequence.clone();
            this.imports = imports;
        }
        public String getUndoTitle() {
            return "Undo Import";
        }
        public String getRedoTitle() {
            return "Redo Import";
        }
        @SuppressWarnings("unchecked")
        public void undo() {
            int last = cells.size() - 1;
            int first = last - imports.size() + 1;
            for (int i = last; i >= first; i--)
                cells.remove(i);
            sequence = (ArrayList<Integer>) seq.clone();
            safelyFireTableDataChanged();
        }
        public void redo() {
            sequence.clear();
            for (int i = 0; i < imports.size(); i++) {
                sequence.add(cells.size());
                cells.add(imports.get(i));
            }
            safelyFireTableDataChanged();
        }
    }
    
    private class AssertiveImportEdit implements Edit {
        ArrayList<Object[]> updated = new ArrayList<Object[]>();
        ArrayList<Integer> updatedIndex = new ArrayList<Integer>();
        ArrayList<Object[]> updatedBak = new ArrayList<Object[]>();
        ArrayList<Object[]> added = new ArrayList<Object[]>();
        public AssertiveImportEdit(ArrayList<Object[]> imports, int[] pkIndex,
                                                            boolean rude) {
            int seqSize = sequence.size();
            int impSize = imports.size();
            for (int i = 0; i < impSize; i++) {
                Object[] impRow = imports.get(i);
                int matchRow = -1;
                if (pkIndex != null)
                    for (int j = 0; j < seqSize; j++) {
                        int cellIndex = sequence.get(j);
                        Object[] seqRow = cells.get(cellIndex);
                        if (rowEqual(impRow, seqRow, pkIndex)) {
                            matchRow = cellIndex;
                            break;
                        }
                    }
                if (matchRow == -1) {
                    for (int j = 0; j < columns; j++)
                        if (impRow[j] == UNSET)
                            impRow[j] = null;
                    added.add(impRow);
                } else {
                    Object[] prev = cells.get(matchRow);
                    updatedBak.add(prev);
                    for (int j = 0; j < columns; j++)
                        if (impRow[j] == UNSET)
                            impRow[j] = rude ? null : prev[j];
                    updated.add(impRow);
                    updatedIndex.add(matchRow);
                }
            }
        }
        public String getUndoTitle() {
            return "Undo Import";
        }
        public String getRedoTitle() {
            return "Redo Import";
        }
        public void undo() {
            for (int src = 0; src < updated.size(); src++) {
                int dst = updatedIndex.get(src);
                cells.set(dst, updatedBak.get(src));
            }
            int last = cells.size() - 1;
            int first = last - added.size() + 1;
            for (int i = last; i >= first; i--) {
                cells.remove(i);
                sequence.remove((Integer) i);
            }
            Collections.sort(sequence, rowComparator);
            safelyFireTableDataChanged();
        }
        public void redo() {
            for (int src = 0; src < updated.size(); src++) {
                int dst = updatedIndex.get(src);
                cells.set(dst, updated.get(src));
            }
            int first = cells.size();
            int last = first + added.size() - 1;
            cells.addAll(added);
            for (int i = first; i <= last; i++)
                sequence.add(i);
            Collections.sort(sequence, rowComparator);
            safelyFireTableDataChanged();
        }
    }
    
    private class GentleImportEdit implements Edit {
        ArrayList<Object[]> added = new ArrayList<Object[]>();
        public GentleImportEdit(ArrayList<Object[]> imports, int[] pkIndex) {
            int seqSize = sequence.size();
            int impSize = imports.size();
            for (int i = 0; i < impSize; i++) {
                Object[] impRow = imports.get(i);
                boolean matched = false;
                if (pkIndex != null)
                    for (int j = 0; j < seqSize; j++) {
                        Object[] seqRow = cells.get(sequence.get(j));
                        if (rowEqual(impRow, seqRow, pkIndex)) {
                            matched = true;
                            break;
                        }
                    }
                if (!matched)
                    added.add(impRow);
            }
        }
        public String getUndoTitle() {
            return "Undo Import";
        }
        public String getRedoTitle() {
            return "Redo Import";
        }
        public void undo() {
            int last = cells.size() - 1;
            int first = last - added.size() + 1;
            for (int i = last; i >= first; i--) {
                cells.remove(i);
                sequence.remove((Integer) i);
            }
            Collections.sort(sequence, rowComparator);
            safelyFireTableDataChanged();
        }
        public void redo() {
            int first = cells.size();
            int last = first + added.size() - 1;
            cells.addAll(added);
            for (int i = first; i <= last; i++)
                sequence.add(i);
            Collections.sort(sequence, rowComparator);
            safelyFireTableDataChanged();
        }
    }

    private static String readLines(LineNumberReader in) throws IOException {
        String s = in.readLine();
        if (s == null || !oddNumberOfDoubleQuotes(s))
            return s;
        StringBuffer buf = new StringBuffer();
        buf.append(s);
        while (true) {
            s = in.readLine();
            if (s == null)
                return buf.toString();
            buf.append("\n");
            buf.append(s);
            if (oddNumberOfDoubleQuotes(s))
                return buf.toString();
        }
    }

    private static boolean oddNumberOfDoubleQuotes(String s) {
        boolean odd = false;
        int pos = -1;
        while ((pos = s.indexOf('"', pos + 1)) != -1)
            odd = !odd;
        return odd;
    }

    // Because of the JavaScript integration, and because of background table
    // loading, a lot of ResultSetTableModel activity takes place on background
    // threads. Since some of the callbacks attached to my tables perform Swing
    // calls, we must not fire any events from background threads; so, I have a
    // few background-safe versions of the AbstractTableModel's fire* methods
    // here.

    public void safelyFireTableCellUpdated(int row, int column) {
        if (SwingUtilities.isEventDispatchThread())
            fireTableCellUpdated(row, column);
        else
            SwingUtilities.invokeLater(new DelayedFireTableCellUpdated(row, column));
    }

    public void safelyFireTableRowsInserted(int first, int last) {
        if (SwingUtilities.isEventDispatchThread())
            fireTableRowsInserted(first, last);
        else
            SwingUtilities.invokeLater(new DelayedFireTableRowsInserted(first, last));
    }

    public void safelyFireTableRowsDeleted(int first, int last) {
        if (SwingUtilities.isEventDispatchThread())
            fireTableRowsDeleted(first, last);
        else
            SwingUtilities.invokeLater(new DelayedFireTableRowsDeleted(first, last));
    }

    public void safelyFireTableDataChanged() {
        if (SwingUtilities.isEventDispatchThread())
            fireTableDataChanged();
        else
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        fireTableDataChanged();
                    }
                });
    }

    private class DelayedFireTableCellUpdated implements Runnable {
        private int row, column;
        public DelayedFireTableCellUpdated(int row, int column) {
            this.row = row;
            this.column = column;
        }
        public void run() {
            fireTableCellUpdated(row, column);
        }
    }

    private class DelayedFireTableRowsDeleted implements Runnable {
        private int first, last;
        public DelayedFireTableRowsDeleted(int first, int last) {
            this.first = first;
            this.last = last;
        }
        public void run() {
            fireTableRowsDeleted(first, last);
        }
    }

    private class DelayedFireTableRowsInserted implements Runnable {
        private int first, last;
        public DelayedFireTableRowsInserted(int first, int last) {
            this.first = first;
            this.last = last;
        }
        public void run() {
            fireTableRowsInserted(first, last);
        }
    }
}
