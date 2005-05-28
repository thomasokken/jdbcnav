package jdbcnav;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import jdbcnav.model.BasicData;
import jdbcnav.model.Data;
import jdbcnav.model.Database;
import jdbcnav.model.ForeignKey;
import jdbcnav.model.PrimaryKey;
import jdbcnav.model.Table;
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
					if ((e.getModifiers() &
						InputEvent.BUTTON3_MASK) != 0)
					    showPopup(e);
				    }
				});

	// Apply FK highlight color
	ForeignKey[] fks = dbTable.getForeignKeys();
	TreeSet fkcol = new TreeSet();
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
	    for (int i = 0; i < pkcol.length; i++)
		table.setColumnType(i, table.getColumnType(i) | 1);
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
				handlePopup(mi.foreign, mi.index);
			    }
			};

	    if (haveFks) {
		JMenuItem jmi = new JMenuItem("Select FK Value...");
		jmi.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				selectFkValue();
			    }
			});
		popupMenu.add(jmi);
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
		    MyMenuItem mi = new MyMenuItem(name, true, i);
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
		    MyMenuItem mi = new MyMenuItem(name, false, i);
		    mi.addActionListener(listener);
		    m.add(mi);
		}
		popupMenu.add(m);
	    }
	}

	int x = e.getX();
	int y = e.getY();
	Point p = new Point(x, y);
	popupRow = table.rowAtPoint(p);
	popupColumn = table.convertColumnIndexToModel(table.columnAtPoint(p));
	String columnName = model.getColumnName(popupColumn);

	if (haveFks) {
	    Component[] items = ((JMenu) popupMenu.getComponent(1))
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
	if (haveRks) {
	    Component[] items = ((JMenu) popupMenu.getComponent(
				    haveFks ? 2 : 0)).getMenuComponents();
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
		
	popupMenu.show(e.getComponent(), x, y);
    }

    private void handlePopup(boolean foreign, int index) {
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
	TableFrame editFrame = db.showTableFrame(k.getThatQualifiedName());
	if (editFrame != null)
	    editFrame.selectRows(thatColumns, val);
    }

    private void details() {
	Database db = dbTable.getDatabase();
	if (dbTable.isUpdatableQueryResult())
	    new TableDetailsFrame(dbTable, db.getBrowser()).showStaggered();
	else
	    db.showTableDetailsFrame(dbTable.getQualifiedName());
    }

    private void selectFkValue() {
	if (fkIndex == -1)
	    return;
	ForeignKey fk = dbTable.getForeignKeys()[fkIndex];
	String thatName = fk.getThatQualifiedName();
	Database db = dbTable.getDatabase();
	Data pkValues = null;
	try {
	    Table thatTable = db.getTable(thatName);
	    pkValues = thatTable.getPKValues();
	} catch (NavigatorException e) {
	    MessageBox.show("Can't load primary key values.", e);
	    return;
	}

	// Replace the referenced table's column names with the referencing
	// table's.
	BasicData bd;
	try {
	    bd = (BasicData) pkValues;
	} catch (ClassCastException e) {
	    bd = new BasicData(pkValues);
	}
	// TODO: can this code be simplified by making a safe assumption
	// about the order of the PK components?
	int ncols = bd.getColumnCount();
	String[] newnames = new String[ncols];
	for (int i = 0; i < ncols; i++) {
	    String name = bd.getColumnName(i);
	    for (int j = 0; j < ncols; j++)
		if (name.equals(fk.getThatColumnName(j))) {
		    newnames[i] = fk.getThisColumnName(j);
		    break;
		}
	}
	bd.setColumnNames(newnames);
	// TODO: reorder the columns so that their order matches the order
	// of the corresponding columns in this TableFrame, taking the
	// Model/View mapping into account

	ForeignKeySelector fksel = new ForeignKeySelector(fkRow, pkValues);
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
	    String colName = model.getColumnName(i);
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

	rowSelectionHandler = new RowSelectionHandler(keyIndex, value);
    }

    private class RowSelectionHandler implements Data.StateListener,
				    MyTable.UserInteractionListener, Runnable {
	private int lastRow = 0;
	private boolean allowAutoScroll = true;
	private int[] keyIndex;
	private Object[] keyValue;
	private ArrayList rowsToSelect = new ArrayList();
	private Rectangle selectRect;

	public RowSelectionHandler(int[] keyIndex, Object[] keyValue) {
	    this.keyIndex = keyIndex;
	    this.keyValue = keyValue;
	    model.addStateListener(this);
	    table.addUserInteractionListener(this);
	    table.clearSelection();
	}

	public synchronized void finish() {
	    model.removeStateListener(this);
	    table.removeUserInteractionListener(this);
	    rowSelectionHandler = null;
	}

	// Data.StateListener methods
	public synchronized void stateChanged(int state, int row) {
	    boolean workToDo = false;
	    if (row > lastRow) {
		for (int i = lastRow; i < row; i++) {
		    matchvalue: {
			for (int j = 0; j < keyIndex.length; j++) {
			    Object o1 = keyValue[j];
			    Object o2 = model.getValueAt(i, keyIndex[j]);
			    if (o1 == null ? o2 != null : !o1.equals(o2))
				break matchvalue;
			}
			rowsToSelect.add(new Integer(i));
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
	    for (Iterator iter = rowsToSelect.iterator(); iter.hasNext();) {
		int row = ((Integer) iter.next()).intValue();
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
	public MyMenuItem(String title, boolean foreign, int index) {
	    super(title);
	    this.foreign = foreign;
	    this.index = index;
	}
    }
}
