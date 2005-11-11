package jdbcnav;

import java.awt.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

import jdbcnav.model.BasicData;
import jdbcnav.model.Data;
import jdbcnav.model.PrimaryKey;
import jdbcnav.model.Table;
import jdbcnav.model.TypeSpec;
import jdbcnav.util.ArrayCollection;
import jdbcnav.util.CSVTokenizer;
import jdbcnav.util.NavigatorException;


// TODO: when deleting or updating primary key values in a table that
// is referenced by others, check if any of the referencing tables are
// currently being displayed and do something (warn the user, or update
// them automagically if possible).

public class ResultSetTableModel extends AbstractTableModel
				 implements SortedTableModel {
    
    public static final int GENTLE = 0;
    public static final int RUDE = 1;
    public static final int VICIOUS = 2;
    private static final Object NULL = new Object();
    private static final Class[] STR_ARGLIST = new Class[] { String.class };

    private Table dbTable;
    private Data data;
    private Data.StateListener datastatelistener;
    private int datatotallength;
    private MyTable table;
    private String[] headers;
    private ArrayList cells = new ArrayList();
    private ArrayList sequence = new ArrayList();
    private int columns;
    private int colIndex[];
    private int[] sortPriority;
    private boolean[] sortAscending;
    private boolean editable;
    private ArrayList original;
    private ArrayList undoListeners;
    private ArrayList undoStack;
    private int undoStackIndex = -1;
    
    public ResultSetTableModel(Data data, Table dbTable) {
	this.dbTable = dbTable;
	editable = dbTable != null && dbTable.isEditable();
	if (editable) {
	    original = new ArrayList();
	    undoListeners = new ArrayList();
	    undoStack = new ArrayList();
	}
	columns = data.getColumnCount();
	colIndex = new int[columns];
	headers = new String[columns];
	for (int i = 0; i < columns; i++) {
	    colIndex[i] = i + 1;
	    headers[i] = data.getColumnName(i);
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
			sequence.add(new Integer(cells.size() - 1));
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
	    fireTableRowsInserted(first, last);
	}
    }

    public synchronized Data getOriginalData() {
	BasicData origdata = new BasicData();
	origdata.setColumnNames(headers);
	TypeSpec[] typeSpecs = new TypeSpec[columns];
	for (int i = 0; i < columns; i++)
	    typeSpecs[i] = data.getTypeSpec(i);
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
	int realRow = ((Integer) sequence.get(row)).intValue();
	return ((Object[]) cells.get(realRow))[column];
    }
    
    public synchronized String getColumnName(int column) {
	return headers[column];
    }
    
    public synchronized Class getColumnClass(int column) {
	TypeSpec spec;
	if (dbTable != null)
	    spec = dbTable.getTypeSpecs()[column];
	else
	    spec = data.getTypeSpec(column);
	if (spec.type == TypeSpec.CLASS)
	    return spec.jdbcJavaClass;
	else
	    return TypeSpec.class;
    }

    public synchronized TypeSpec getTypeSpec(int column) {
	if (dbTable != null)
	    return dbTable.getTypeSpecs()[column];
	else
	    return data.getTypeSpec(column);
    }

    public synchronized boolean isCellEditable(int row, int column) {
	return editable;
    }

    public synchronized void setValueAt(Object value, int row, int column) {
	setValueAt(value, row, column, "Change Cell");
    }

    public synchronized void setValueAt(Object value, int row, int column,
								String why) {
	if (!editable)
	    return;
	int realRow = ((Integer) sequence.get(row)).intValue();
	Object[] currRow = (Object[]) cells.get(realRow);
	Object prev = currRow[column];

	if (value == null ? prev == null : value.equals(prev))
	    return;
	currRow[column] = value;
	editHappened(new SingleCellEdit(realRow, column, prev, value, why));
	fireTableCellUpdated(row, column);
    }

    public synchronized String[] getHeaders() {
	return (String[]) headers.clone();
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

	ArrayList deleted = new ArrayList();
	ArrayList modified = new ArrayList();
	ArrayList modifiedBak = new ArrayList();
	ArrayList inserted = new ArrayList();
	int currRows = cells.size();
	int prevRows = original.size();
	for (int i = 0; i < prevRows; i++) {
	    if (sequence.indexOf(new Integer(i)) == -1)
		deleted.add(original.get(i));
	    else {
		Object[] current = (Object[]) cells.get(i);
		Object[] backup = (Object[]) original.get(i);
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
	    if (sequence.indexOf(new Integer(i)) != -1)
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
	    Object[] row = (Object[]) deleted.get(i);
	    Object[] key = new Object[pk.length];
	    for (int j = 0; j < key.length; j++)
		key[j] = row[pkIndex[j]];
	    tch.deleteRow(dbTable, key);
	}
	for (int i = 0; i < modified.size(); i++) {
	    Object[] row = (Object[]) modified.get(i);
	    Object[] bak = (Object[]) modifiedBak.get(i);
	    tch.updateRow(dbTable, bak, row);
	}
	for (int i = 0; i < inserted.size(); i++) {
	    Object[] row = (Object[]) inserted.get(i);
	    tch.insertRow(dbTable, row);
	}
    }

    public synchronized void postCommit() {
	// This is called "post" commit, because the actual work of committing
	// changes is done by Database (with help from MultiTableCommit).
	if (!isDirty())
	    return;
	clearUndoStack();
	ArrayList newCells = new ArrayList();
	original.clear();
	for (int i = 0; i < sequence.size(); i++) {
	    int row = ((Integer) sequence.get(i)).intValue();
	    Object[] current = (Object[]) cells.get(row);
	    newCells.add(current);
	    sequence.set(i, new Integer(i));
	    original.add(current.clone());
	}
	cells = newCells;
	// Just for good measure, and to get the commit/rollback menu
	// items to be disabled
	fireTableDataChanged();
    }

    public synchronized void rollback() {
	if (!isDirty())
	    return;
	int rows = original.size();
	sequence.clear();
	for (int i = 0; i < rows; i++) {
	    sequence.add(new Integer(i));
	    cells.set(i, ((Object[]) original.get(i)).clone());
	}
	for (int i = cells.size() - 1; i >= rows; i--)
	    cells.remove(i);
	clearUndoStack();
	Collections.sort(sequence, rowComparator);
	fireTableDataChanged();
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
	fireTableDataChanged();
    }

    public synchronized int getSortedColumn() {
	return sortPriority[0];
    }

    public synchronized void sort() {
	Collections.sort(sequence, rowComparator);
	fireTableDataChanged();
    }

    public synchronized void selectionFromViewToModel(int[] selection) {
	int len = selection.length;
	if (len == 0)
	    return;
	for (int i = 0; i < len; i++)
	    selection[i] = ((Integer) sequence.get(selection[i])).intValue();
	Arrays.sort(selection);
    }

    public synchronized void selectionFromModelToView(int[] selection) {
	int len = selection.length;
	if (len == 0)
	    return;
	int seqlen = sequence.size();
	int[] reverseSequence = new int[seqlen];
	for (int i = 0; i < seqlen; i++)
	    reverseSequence[((Integer) sequence.get(i)).intValue()] = i;
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
	Edit e = (Edit) undoStack.get(undoStackIndex);
	return e.getUndoTitle();
    }

    public synchronized String getRedoTitle() {
	if (undoStackIndex >= undoStack.size() - 1)
	    return null;
	Edit e = (Edit) undoStack.get(undoStackIndex + 1);
	return e.getRedoTitle();
    }

    private synchronized void undoRedoTitleChanged() {
	for (Iterator iter = undoListeners.iterator(); iter.hasNext();) {
	    UndoListener listener = (UndoListener) iter.next();
	    listener.undoRedoTitleChanged();
	}
    }

    private synchronized boolean canUndo() {
	return undoStackIndex != -1;
    }

    private synchronized boolean canRedo() {
	return undoStackIndex < undoStack.size() - 1;
    }
    
    public synchronized void undo() {
	if (canUndo()) {
	    Edit e = (Edit) undoStack.get(undoStackIndex--);
	    e.undo();
	    undoRedoTitleChanged();
	}
    }

    public synchronized void redo() {
	if (canRedo()) {
	    Edit e = (Edit) undoStack.get(++undoStackIndex);
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
	    sequence.add(new Integer(modelRow));
	} else {
	    beforeModelRow = ((Integer) sequence.get(viewRow)).intValue();
	    sequence.add(viewRow, new Integer(modelRow));
	}
	editHappened(new InsertRowEdit(beforeModelRow));
	fireTableRowsInserted(viewRow, viewRow);
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
		precedes[i] = ((Integer) sequence.get(following)).intValue();
	    removed[i] = ((Integer) sequence.get(rows[i])).intValue();
	}
	for (int i = length - 1; i >= 0; i--)
	    sequence.remove(rows[i]);
	editHappened(new DeleteRowEdit(removed, precedes, cut));
	int first = rows[0];
	int last = rows[length - 1];
	if (last - first == length - 1)
	    fireTableRowsDeleted(first, last);
	else
	    fireTableDataChanged();
    }

    public synchronized void pasteRow(int[] selection, Object[][] grid,
							String[] mapping) {
	Arrays.sort(selection);
	int length = selection.length;
	int[] precedes = new int[length];
	int[] removed = new int[length];
	Object[][] added = new Object[grid.length - 2][columns];
	for (int i = 0; i < length; i++) {
	    int following = selection[i] + 1;
	    if (following == sequence.size())
		precedes[i] = -1;
	    else
		precedes[i] = ((Integer) sequence.get(following)).intValue();
	    removed[i] = ((Integer) sequence.get(selection[i])).intValue();
	}
	for (int i = length - 1; i >= 0; i--)
	    sequence.remove(selection[i]);
	for (int col = 0; col < columns; col++) {
	    String clipColName = mapping[col];
	    if (clipColName == null)
		continue;
	    for (int i = 0; i < grid[0].length; i++)
		if (clipColName.equalsIgnoreCase((String) grid[0][i]))
		    for (int row = 0; row < grid.length - 2; row++)
			added[row][col] = grid[row + 2][i];
	}
	int where = selection.length == 0 ? -1 : selection[0];
	int w = where;
	for (int row = 0; row < added.length; row++) {
	    cells.add(added[row]);
	    int n = cells.size() - 1;
	    if (where == -1)
		sequence.add(new Integer(n));
	    else
		sequence.add(w++, new Integer(n));
	}
	editHappened(new PasteRowEdit(removed, precedes, added, where));
	fireTableDataChanged();
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

    private class RowComparator implements Comparator {
	public int compare(Object A, Object B) {
	    Object a = cells.get(((Integer) A).intValue());
	    Object b = cells.get(((Integer) B).intValue());
	    if (a == null)
		return b == null ? 0 :
			sortAscending[sortPriority[0]] ? 1 : -1;
	    else if (b == null)
		return sortAscending[sortPriority[0]] ? -1 : 1;
	    Object[] aa = (Object[]) a;
	    Object[] bb = (Object[]) b;
	    for (int i = 0; i < sortPriority.length; i++) {
		try {
		    int col = sortPriority[i];
		    boolean ascending = sortAscending[col];
		    Comparable ac = (Comparable) aa[col];
		    Comparable bc = (Comparable) bb[col];
		    if (ac == null)
			if (bc != null)
			    return ascending ? 1 : -1;
			else
			    continue;
		    else if (bc == null)
			return ascending ? -1 : 1;
		    int res;
		    if ((ac instanceof String) && (bc instanceof String))
			res = ((String) ac).compareToIgnoreCase((String) bc);
		    else
			res = ac.compareTo(bc);
		    if (res != 0)
			return ascending ? res : -res;
		} catch (ClassCastException e) {
		    // Never mind; try next column
		}
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
				    == JOptionPane.CANCEL_OPTION)
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

		boolean[] number = new boolean[columns];
		for (int i = 0; i < columns; i++) {
		    Class c = getColumnClass(i);
		    number[i] = c != null && Number.class.isAssignableFrom(c);
		}

		int rows = getRowCount();
		for (int i = 0; i < rows; i++) {
		    for (int j = 0; j < columns; j++) {
			Object o = getValueAt(i, j);
			if (o != null)
			    if (number[j])
				pw.print(o);
			    else {
				// The important subclasses of java.util.Date,
				// that is, java.sql.Date, java.sql.Time, and
				// java.sql.Timestamp, all have resonable
				// toString() methods, but
				// java.util.Date.toString() produces the kind
				// of human-friendly mess we really don't want
				// here. So, use java.sql.Timestamp instead.
				if (o.getClass() == java.util.Date.class)
				    o = new java.sql.Timestamp(
						((java.util.Date) o).getTime());
				pw.print(quote(o.toString()));
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
	ArrayList imported = new ArrayList();
	int[] pkIndex;
	try {
	    in = new LineNumberReader(new FileReader(file));
	    String line;
	    CSVTokenizer tok;
	    if (colName == null) {
		line = readLines(in);
		tok = new CSVTokenizer(line);
		ArrayList al = new ArrayList();
		while (tok.hasMoreTokens())
		    al.add(tok.nextToken());
		colName = (String[]) al.toArray(new String[0]);
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
	    String[] pk;
	    if (dpk == null)
		pk = colName;
	    else {
		int n = dpk.getColumnCount();
		pk = new String[n];
		for (int i = 0; i < n; i++)
		    pk[i] = dpk.getColumnName(i);
	    }
	    boolean[] pkCompPresent = new boolean[pk.length];
	    pkIndex = new int[pk.length];
	    int found = 0;
	    for (int i = 0; i < pk.length; i++) {
		pkCompPresent[i] = false;
		String cn = pk[i];
		for (int j = 0; j < colName.length; j++)
		    if (cn.equalsIgnoreCase(colName[j])) {
			pkCompPresent[i] = true;
			pkIndex[i] = colIndex[j];
			found++;
			break;
		    }
	    }
	    if (found < pk.length) {
		StringBuffer buf = new StringBuffer();
		buf.append("Can't import file ");
		buf.append(file.getName());
		buf.append(" into table ");
		buf.append(dbTable.getName());
		buf.append(".\nPrimary key column");
		if (pk.length - found > 1)
		    buf.append("s ");
		else
		    buf.append(" ");
		boolean first = true;
		for (int i = 0; i < pk.length; i++)
		    if (!pkCompPresent[i]) {
			if (first)
			    first = false;
			else
			    buf.append(", ");
			buf.append(pk[i]);
		    }
		if (pk.length - found == 1)
		    buf.append(" is missing.");
		else
		    buf.append(" are missing.");
		JOptionPane.showInternalMessageDialog(Main.getDesktop(),
						      buf.toString());
		return;
	    }

	    while ((line = readLines(in)) != null) {
		Object[] row = new Object[columns];
		if (importMode == RUDE) {
		    // In "rude" mode, we overwrite existing rows -- but
		    // only the fields that we actually read from the file.
		    // So, we must have a way to distinguish, while actually
		    // doing/redoing the edit, between nulls that we actually
		    // read from the file, and cells that were not read.
		    for (int i = 0; i < columns; i++)
			row[i] = NULL;
		}
		tok = new CSVTokenizer(line);
		int index = -1;
		while (tok.hasMoreTokens() && ++index < colIndex.length) {
		    String s = tok.nextToken();
		    int i = colIndex[index];
		    if (i == -1)
			// Column in imported file does not exist in table
			continue;
		    row[i] = data.getTypeSpec(i).stringToObject(s);
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
	    edit = new RudeImportEdit(imported, pkIndex);
	else // importMode == GENTLE
	    edit = new GentleImportEdit(imported, pkIndex);
	editHappened(edit);
	edit.redo();
    }

    private static String quote(String s) {
	StringBuffer buf = new StringBuffer("\"");
	StringTokenizer tok = new StringTokenizer(s, "\"", true);
	while (tok.hasMoreTokens()) {
	    String t = tok.nextToken();
	    buf.append(t);
	    if (t.equals("\""))
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
	    ((Object[]) cells.get(row))[column] = before;
	    fireTableCellUpdated(sequence.indexOf(new Integer(row)), column);
	}

	public void redo() {
	    ((Object[]) cells.get(row))[column] = after;
	    fireTableCellUpdated(sequence.indexOf(new Integer(row)), column);
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
	    int row = sequence.indexOf(new Integer(index));
	    sequence.remove(row);
	    cells.remove(index);
	    fireTableRowsDeleted(row, row);
	}

	public void redo() {
	    cells.add(new Object[columns]);
	    int modelRow = cells.size() - 1;
	    int viewRow;
	    if (row == -1) {
		viewRow = sequence.size() - 1;
		sequence.add(new Integer(modelRow));
	    } else {
		viewRow = sequence.indexOf(new Integer(row));
		sequence.add(viewRow, new Integer(modelRow));
	    }
	    fireTableRowsInserted(viewRow, viewRow);
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
		    sequence.add(new Integer(removed[i]));
		else {
		    insertPos = sequence.indexOf(new Integer(insertPos));
		    sequence.add(insertPos, new Integer(removed[i]));
		}
	    }
	    fireTableDataChanged();
	}

	public void redo() {
	    sequence.removeAll(new ArrayCollection(removed));
	    fireTableDataChanged();
	}
    }

    private class PasteRowEdit implements Edit {
	private int[] removed;
	private int[] precedes;
	private Object[][] added;
	private int where;
	public PasteRowEdit(int[] removed, int[] precedes, Object[][] added, int where) {
	    this.removed = removed;
	    this.precedes = precedes;
	    this.added = added;
	    this.where = where;
	}
	public String getUndoTitle() {
	    return added.length == 1 ? "Undo Paste Row" : "Undo Paste Rows";
	}
	public String getRedoTitle() {
	    return added.length == 1 ? "Redo Paste Row" : "Redo Paste Rows";
	}
	public void undo() {
	    for (int i = 0; i < added.length; i++) {
		int index = cells.size() - 1;
		int row = sequence.indexOf(new Integer(index));
		sequence.remove(row);
		cells.remove(index);
	    }
	    for (int i = removed.length - 1; i >= 0; i--) {
		int insertPos = precedes[i];
		if (insertPos == -1)
		    sequence.add(new Integer(removed[i]));
		else {
		    insertPos = sequence.indexOf(new Integer(insertPos));
		    sequence.add(insertPos, new Integer(removed[i]));
		}
	    }
	    fireTableDataChanged();
	}
	public void redo() {
	    sequence.removeAll(new ArrayCollection(removed));
	    int w = where;
	    for (int row = 0; row < added.length; row++) {
		cells.add(added[row]);
		int n = cells.size() - 1;
		if (where == -1)
		    sequence.add(new Integer(n));
		else
		    sequence.add(w++, new Integer(n));
	    }
	    fireTableDataChanged();
	}
    }

    private class ViciousImportEdit implements Edit {
	private ArrayList seq;
	private ArrayList imports;
	public ViciousImportEdit(ArrayList imports) {
	    seq = (ArrayList) sequence.clone();
	    this.imports = imports;
	}
	public String getUndoTitle() {
	    return "Undo Import";
	}
	public String getRedoTitle() {
	    return "Redo Import";
	}
	public void undo() {
	    int last = cells.size() - 1;
	    int first = last - imports.size() + 1;
	    for (int i = last; i >= first; i--)
		cells.remove(i);
	    sequence = (ArrayList) seq.clone();
	    fireTableDataChanged();
	}
	public void redo() {
	    sequence.clear();
	    for (int i = 0; i < imports.size(); i++) {
		sequence.add(new Integer(cells.size()));
		cells.add(imports.get(i));
	    }
	    fireTableDataChanged();
	}
    }
    
    private class RudeImportEdit implements Edit {
	ArrayList updated = new ArrayList();
	ArrayList updatedIndex = new ArrayList();
	ArrayList updatedBak = new ArrayList();
	ArrayList added = new ArrayList();
	public RudeImportEdit(ArrayList imports, int[] pkIndex) {
	    int seqSize = sequence.size();
	    int impSize = imports.size();
	    for (int i = 0; i < impSize; i++) {
		Object[] impRow = (Object[]) imports.get(i);
		int matchRow = -1;
		for (int j = 0; j < seqSize; j++) {
		    int cellIndex = ((Integer) sequence.get(j)).intValue();
		    Object[] seqRow = (Object[]) cells.get(cellIndex);
		    if (rowEqual(impRow, seqRow, pkIndex)) {
			matchRow = cellIndex;
			break;
		    }
		}
		if (matchRow == -1) {
		    for (int j = 0; j < columns; j++)
			if (impRow[j] == NULL)
			    impRow[j] = null;
		    added.add(impRow);
		} else {
		    Object[] prev = (Object[]) cells.get(matchRow);
		    updatedBak.add(prev);
		    for (int j = 0; j < columns; j++)
			if (impRow[j] == NULL)
			    impRow[j] = prev[j];
		    updated.add(impRow);
		    updatedIndex.add(new Integer(matchRow));
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
		int dst = ((Integer) updatedIndex.get(src)).intValue();
		cells.set(dst, updatedBak.get(src));
	    }
	    int last = cells.size() - 1;
	    int first = last - added.size() + 1;
	    for (int i = last; i >= first; i--) {
		cells.remove(i);
		sequence.remove(new Integer(i));
	    }
	    Collections.sort(sequence, rowComparator);
	    fireTableDataChanged();
	}
	public void redo() {
	    for (int src = 0; src < updated.size(); src++) {
		int dst = ((Integer) updatedIndex.get(src)).intValue();
		cells.set(dst, updated.get(src));
	    }
	    int first = cells.size();
	    int last = first + added.size() - 1;
	    cells.addAll(added);
	    for (int i = first; i <= last; i++)
		sequence.add(new Integer(i));
	    Collections.sort(sequence, rowComparator);
	    fireTableDataChanged();
	}
    }
    
    private class GentleImportEdit implements Edit {
	ArrayList added = new ArrayList();
	public GentleImportEdit(ArrayList imports, int[] pkIndex) {
	    int seqSize = sequence.size();
	    int impSize = imports.size();
	    for (int i = 0; i < impSize; i++) {
		Object[] impRow = (Object[]) imports.get(i);
		boolean matched = false;
		for (int j = 0; j < seqSize; j++) {
		    Object[] seqRow = (Object[]) cells.get(((Integer)
				    sequence.get(j)).intValue());
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
		sequence.remove(new Integer(i));
	    }
	    Collections.sort(sequence, rowComparator);
	    fireTableDataChanged();
	}
	public void redo() {
	    int first = cells.size();
	    int last = first + added.size() - 1;
	    cells.addAll(added);
	    for (int i = first; i <= last; i++)
		sequence.add(new Integer(i));
	    Collections.sort(sequence, rowComparator);
	    fireTableDataChanged();
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
}
