package jdbcnav;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.*;
import javax.swing.table.*;
import jdbcnav.util.MenuLayout;


public class MyTable extends JTable {
    
    private static final int MAX_COLUMN_WIDTH = 512;

    private int highlightIndex = 0;
    private ArrayList userInteractionListeners = new ArrayList();
    private boolean[] notNull;
    private ArrayList columnTypeMap;

    public MyTable(TableModel dm) {
	super();
	setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	setModel(dm);

	setDefaultRenderer(String.class, new FastTableCellRenderer());
	setDefaultRenderer(java.util.Date.class, new UtilDateRenderer());
	setDefaultRenderer(java.sql.Time.class, new FastTableCellRenderer());
	setDefaultRenderer(java.sql.Date.class, new FastTableCellRenderer());
	setDefaultRenderer(java.sql.Timestamp.class, new FastTableCellRenderer());
	try {
	    Class tsClass = Class.forName("oracle.sql.TIMESTAMP");
	    setDefaultRenderer(tsClass, new OracleTimestampRenderer());
	} catch (ClassNotFoundException e) {}
	try {
	    Class tsClass = Class.forName("oracle.sql.TIMESTAMPTZ");
	    setDefaultRenderer(tsClass, new OracleTimestampRenderer());
	} catch (ClassNotFoundException e) {}
	try {
	    Class tsClass = Class.forName("oracle.sql.TIMESTAMPLTZ");
	    setDefaultRenderer(tsClass, new OracleTimestampRenderer());
	} catch (ClassNotFoundException e) {}

	setDefaultRenderer(Number.class, new FastTableCellRenderer(false));
	setDefaultRenderer(Float.class, new FastTableCellRenderer(false));
	setDefaultRenderer(Double.class, new FastTableCellRenderer(false));
	setDefaultRenderer(Boolean.class, new FastTableCellRenderer());
	setDefaultRenderer(Object.class, new FastTableCellRenderer());
	setDefaultRenderer((new byte[1]).getClass(), new ByteArrayRenderer());
	setDefaultRenderer(java.sql.Blob.class, new BlobRenderer());
	setDefaultRenderer(java.sql.Clob.class, new FastTableCellRenderer());

	try {
	    Class bfileClass = Class.forName("oracle.sql.BFILE");
	    setDefaultRenderer(bfileClass, new BfileRenderer());
	} catch (ClassNotFoundException e) {}

	setDefaultEditor(java.util.Date.class,
			 new DateEditor(java.util.Date.class));
	setDefaultEditor(java.sql.Time.class,
			 new DateEditor(java.sql.Time.class));
	setDefaultEditor(java.sql.Date.class,
			 new DateEditor(java.sql.Date.class));
	setDefaultEditor(java.sql.Timestamp.class,
			 new DateEditor(java.sql.Timestamp.class));

	try {
	    Class klass = Class.forName("oracle.sql.TIMESTAMP");
	    setDefaultEditor(klass, new OracleTimestampEditor(klass));
	} catch (ClassNotFoundException e) {}
	try {
	    Class klass = Class.forName("oracle.sql.TIMESTAMPTZ");
	    setDefaultEditor(klass, new OracleTimestampEditor(klass));
	} catch (ClassNotFoundException e) {}
	try {
	    Class klass = Class.forName("oracle.sql.TIMESTAMPLTZ");
	    setDefaultEditor(klass, new OracleTimestampEditor(klass));
	} catch (ClassNotFoundException e) {}
	try {
	    Class klass = Class.forName("oracle.sql.INTERVALDS");
	    setDefaultEditor(klass, new GenericEditor(klass));
	} catch (ClassNotFoundException e) {}
	try {
	    Class klass = Class.forName("oracle.sql.INTERVALDS");
	    setDefaultEditor(klass, new GenericEditor(klass));
	} catch (ClassNotFoundException e) {}


	if (dm instanceof SortedTableModel) {
	    getTableHeader().addMouseListener(
		    new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
			    mouseInHeader(e);
			}
		    });
	    highlightIndex = ((SortedTableModel) dm).getSortedColumn();
	    ((MyTableColumn) getColumnModel().getColumn(highlightIndex))
						    .setHighlighted(true);
	}
	
	addMouseListener(new MouseAdapter() {
			    public void mouseReleased(MouseEvent e) {
				eventInTableHappened();
			    }
			});
	addKeyListener(new KeyAdapter() {
			    public void keyTyped(KeyEvent e) {
				eventInTableHappened();
			    }
			});
    }

    public void setModel(TableModel dm) {
	notNull = new boolean[dm.getColumnCount()];
	columnTypeMap = new ArrayList();
	super.setModel(dm);
    }

    public void setNiceSize() {
	setNiceSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public void setNiceSize(Dimension d) {
	setNiceSize(d.width, d.height);
    }

    public void setNiceSize(int maxWidth, int maxHeight) {
	// First, figure out the optimum width for all columns.
	// This is a bit of a chore...

	TableModel tm = getModel();
	TableColumnModel tcm = getColumnModel();
	int numColumns = tcm.getColumnCount();
	int numRows = tm.getRowCount();
	int totalwidth = 0;
	int margin = 2 * tcm.getColumnMargin();

	for (int column = 0; column < numColumns; column++) {
	    TableColumn tc = tcm.getColumn(column);
	    TableCellRenderer tcr = tc.getHeaderRenderer();
	    if (tcr == null)
		tcr = getTableHeader().getDefaultRenderer();

	    Dimension d = tcr.getTableCellRendererComponent(this,
				    tm.getColumnName(column),
				    false, true, -1, column)
			    .getPreferredSize();
	    int width = d.width;

	    for (int row = 0; row < numRows; row++) {
		int w = getCellRenderer(row, column)
			    .getTableCellRendererComponent(this,
				    tm.getValueAt(row, column),
				    true, true, row, column)
			    .getPreferredSize().width;
		if (w > width)
		    width = w;
		if (width > MAX_COLUMN_WIDTH) {
		    width = MAX_COLUMN_WIDTH;
		    break;
		}
	    }

	    width += margin;
	    tc.setPreferredWidth(width);
	    tc.setWidth(width);
	    totalwidth += tc.getWidth();
	}
	
	// Now, the height.

	int totalheight = numRows * getRowHeight();

	// Finally, update the preferred viewport size.

	if (totalwidth > maxWidth)
	    totalwidth = maxWidth;
	if (totalheight > maxHeight)
	    totalheight = maxHeight;
	Dimension d = new Dimension(totalwidth, totalheight);
	setPreferredScrollableViewportSize(d);

	JTableHeader th = getTableHeader();
	if (th != null)
	    th.repaint();
    }

    public void createDefaultColumnsFromModel() {
	TableModel m = getModel();
	if (m != null) {
	    // Remove any current columns
	    TableColumnModel cm = getColumnModel();
	    while (cm.getColumnCount() > 0) {
		cm.removeColumn(cm.getColumn(0));
	    }

	    // Create new columns from the data model info
	    for (int i = 0; i < m.getColumnCount(); i++) {
		MyTableColumn newColumn = new MyTableColumn(i, this);
		addColumn(newColumn);
	    }
	}
    }


    public void setColumnNotNull(int column, boolean notNull) {
	this.notNull[column] = notNull;
    }

    public void setColumnType(int column, int type) {
	Integer t = new Integer(type);
	try {
	    columnTypeMap.set(column, t);
	} catch (IndexOutOfBoundsException e) {
	    while (column > columnTypeMap.size())
		columnTypeMap.add(new Integer(0));
	    columnTypeMap.add(t);
	}
    }

    public int getColumnType(int column) {
	try {
	    return ((Integer) columnTypeMap.get(column)).intValue();
	} catch (IndexOutOfBoundsException e) {
	    return 0;
	}
    }

    public static void setTypeColor(int type, Color color) {
	FastTableCellRenderer.setTypeColor(type, color);
    }


    /**
     * The JTable version of this method looks in defaultRenderersByColumnClass
     * for the given class; if it is not found, it looks for the superclass,
     * then the superclass' superclass, etc.
     * My version also looks for all of the interfaces implemented by a class,
     * before moving on to the superclass. This allows me to set renderers for
     * things like java.sql.Blob, which is an interface, and so could not be
     * used with the original code.
     */
    public TableCellRenderer getDefaultRenderer(Class columnClass) {
	if (columnClass == null) {
	    return null;
	} else {
	    Object renderer = defaultRenderersByColumnClass.get(columnClass);
	    if (renderer != null) {
		return (TableCellRenderer) renderer;
	    } else {
		Class[] interfaces = columnClass.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
		    TableCellRenderer tcr = getDefaultRenderer(interfaces[i]);
		    if (tcr != null)
			return tcr;
		}
		return getDefaultRenderer(columnClass.getSuperclass());
	    }
	}
    }


    public interface UserInteractionListener {
	void eventInScrollBar();
	void eventInTable();
    }

    public void addUserInteractionListener(UserInteractionListener listener) {
	userInteractionListeners.add(listener);
    }

    public void removeUserInteractionListener(UserInteractionListener listener){
	userInteractionListeners.remove(listener);
    }

    private void eventInScrollBarHappened() {
	ArrayList listeners = (ArrayList) userInteractionListeners.clone();
	for (Iterator iter = listeners.iterator(); iter.hasNext();) {
	    UserInteractionListener listener =
				    (UserInteractionListener) iter.next();
	    listener.eventInScrollBar();
	}
    }

    private void eventInTableHappened() {
	ArrayList listeners = (ArrayList) userInteractionListeners.clone();
	for (Iterator iter = listeners.iterator(); iter.hasNext();) {
	    UserInteractionListener listener =
				    (UserInteractionListener) iter.next();
	    listener.eventInTable();
	}
    }

    public void addNotify() {
	super.addNotify();
	JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(
				JScrollPane.class, this);
	if (scroll == null)
	    return;
	JScrollBar hscroll = scroll.getHorizontalScrollBar();
	JScrollBar vscroll = scroll.getVerticalScrollBar();
	if (hscroll != null || vscroll != null) {
	    MouseListener ml = new MouseAdapter() {
				    public void mouseReleased(MouseEvent e) {
					eventInScrollBarHappened();
				    }
				};
	    KeyListener kl = new KeyAdapter() {
				    public void keyTyped(KeyEvent e) {
					eventInScrollBarHappened();
				    }
				};
	    if (hscroll != null) {
		hscroll.addMouseListener(ml);
		hscroll.addKeyListener(kl);
	    }
	    if (vscroll != null) {
		vscroll.addMouseListener(ml);
		vscroll.addKeyListener(kl);
	    }
	}
    }


    public void stopEditing() {
	if (!isEditing())
	    return;
	int row = getEditingRow();
	int column = getEditingColumn();
	TableCellEditor editor = getCellEditor(row, column);
	if (!editor.stopCellEditing())
	    editor.cancelCellEditing();
    }

    public void cancelEditing() {
	if (!isEditing())
	    return;
	int row = getEditingRow();
	int column = getEditingColumn();
	TableCellEditor editor = getCellEditor(row, column);
	editor.cancelCellEditing();
    }

    private void mouseInHeader(MouseEvent e) {
	int physicalColumn = columnAtPoint(e.getPoint());
	if (physicalColumn == -1)
	    return;
	int logicalColumn = convertColumnIndexToModel(physicalColumn);

	if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
	    // Right-click: present a menu of column names allowing
	    // the user to jump to a column
	    JPopupMenu menu = new JPopupMenu();
	    int columns = getModel().getColumnCount();
	    for (int i = 0; i < columns; i++) {
		JMenuItem mi = new JMenuItem(getColumnName(i));
		mi.addActionListener(new ColumnJumper(i));
		int col = convertColumnIndexToModel(i);

		mi.setFont(mi.getFont().deriveFont(
				    notNull[col] ? Font.BOLD : Font.PLAIN));

		try {
		    int type = ((Integer) columnTypeMap.get(col)).intValue();
		    if (type == 3)
			type = 1;
		    Color bg = FastTableCellRenderer.getTypeColor(type, true);
		    Color fg = FastTableCellRenderer.getTypeColor(type, false);
		    if (bg != null && fg != null) {
			mi.setBackground(bg);
			mi.setForeground(fg);
		    }
		} catch (IndexOutOfBoundsException ex) {
		    // columnTypeMap does not have an entry for 'col';
		    // no worries, we just leave the menu item's color alone.
		}
		menu.add(mi);
	    }
	    menu.setLayout(new MenuLayout());
	    menu.show(e.getComponent(), e.getX(), e.getY());
	} else {
	    // Left (or middle) click: change the sorting order
	    SortedTableModel model = (SortedTableModel) getModel();
	    int[] selection = getSelectedRows();
	    model.selectionFromViewToModel(selection);
	    ((SortedTableModel) getModel()).sortColumn(logicalColumn);
	    model.selectionFromModelToView(selection);
	    for (int i = 0; i < selection.length; i++)
		addRowSelectionInterval(selection[i], selection[i]);
	    if (logicalColumn != highlightIndex) {
		TableColumnModel tcm = getColumnModel();
		((MyTableColumn) tcm.getColumn(convertColumnIndexToView(
					highlightIndex))).setHighlighted(false);
		((MyTableColumn) tcm.getColumn(physicalColumn))
							.setHighlighted(true);
		highlightIndex = logicalColumn;
	    }
	}
    }

    private class ColumnJumper implements ActionListener {
	private int col;
	public ColumnJumper(int col) {
	    this.col = col;
	}
	public void actionPerformed(ActionEvent e) {
	    Rectangle r = getCellRect(-1, col, true);
	    JViewport view = (JViewport) SwingUtilities.getAncestorOfClass(
						JViewport.class, MyTable.this);
	    if (view != null) {
		// Clip 'r' vertically against view rectangle.
		// This is necessary to prevent vertical scrolling.
		Rectangle vr = view.getViewRect();
		r.y = vr.y;
		r.height = vr.height;
	    }
	    scrollRectToVisible(r);
	}
    }

    private static class UtilDateRenderer extends FastTableCellRenderer {
	protected String valueToString(Object value) {
	    return value == null ? null : new java.sql.Timestamp(((java.util.Date) value).getTime()).toString();
	}
    }

    private static class ByteArrayRenderer extends FastTableCellRenderer {
	protected String valueToString(Object value) {
	    if (value == null)
		return null;
	    byte[] barray = (byte[]) value;
	    StringBuffer buf = new StringBuffer();
	    for (int i = 0; i < barray.length; i++) {
		byte b = barray[i];
		buf.append("0123456789ABCDEF".charAt((b >> 4) & 15));
		buf.append("0123456789ABCDEF".charAt(b & 15));
	    }
	    return buf.toString();
	}
    }

    private static class BlobRenderer extends ByteArrayRenderer {
	protected String valueToString(Object value) {
	    if (value instanceof java.sql.Blob) {
		java.sql.Blob blob = (java.sql.Blob) value;
		try {
		    return "Blob (length = " + blob.length() + ")";
		} catch (java.sql.SQLException e) {
		    return "Blob (length = ?)";
		}
	    } else {
		// Assuming byte[]; this can happen with Oracle drivers,
		// where Blob.setBytes() is not implemented, so Binary-
		// EditorFrame stores the byte array back into the model
		return super.valueToString(value);
	    }
	}
    }

    private static class BfileRenderer extends FastTableCellRenderer {
	protected String valueToString(Object value) {
	    // Can't just cast to oracle.sql.BFILE and call getName(),
	    // because that would introduce a run-time dependency on
	    // the Oracle driver (loading the MyTable class could fail).
	    // I could also fix this by moving BfileRenderer to a
	    // separate class, of course, but I don't like scattering
	    // things about. :-)

	    if (value == null)
		return null;

//	    oracle.sql.BFILE bfile = (oracle.sql.BFILE) value;
//	    try {
//		return "Bfile (name = \"" + bfile.getName() + "\")";
//	    } catch (java.sql.SQLException e) {
//		return "Bfile (name = ?)";
//	    }

	    try {
		Class bfileClass = value.getClass();
		Method getNameMethod = bfileClass.getMethod("getName", null);
		String name = (String) getNameMethod.invoke(value, null);
		return "Bfile (name = \"" + name + "\")";
	    } catch (NoSuchMethodException e) {
		// From Class.getMethod()
		// Should not happen
		return "Not a BFILE";
	    } catch (IllegalAccessException e) {
		// From Method.invoke()
		// Should not happen
		return "Not a BFILE";
	    } catch (InvocationTargetException e) {
		// From Method.invoke()
		// Should be a SQLException from BFILE.getName()
		return "Bfile (name = ?)";
	    }
	}
    }
    
    private static String oracleTimestampToString(Object value) {
	if (value == null)
	    return null;
	Class klass = value.getClass();
	if (klass.getName().equals("oracle.sql.TIMESTAMP")) {
	    try {
		Method m = klass.getMethod("timestampValue", null);
		java.sql.Timestamp ts = (java.sql.Timestamp)
		    m.invoke(value, null);
		return ts.toString();
	    } catch (Exception e) {
		return value.toString();
	    }
	}
	try {
	    Method m = klass.getMethod("getBytes", null);
	    byte[] b = (byte[]) m.invoke(value, null);
	    int year = (b[0] < 0 ? b[0] + 256 : b[0]) * 100
			+ (b[1] < 0 ? b[1] + 256 : b[1])
			- 10100;
	    GregorianCalendar cal = new GregorianCalendar(
		    year, b[2] - 1, b[3], b[4], b[5], b[6]);
	    java.sql.Timestamp ts = new java.sql.Timestamp(
		    cal.getTimeInMillis());
	    int nanos;
	    if (b.length >= 11)
		nanos = (b[7] << 24)
		    + ((b[8] < 0 ? b[8] + 256 : b[8]) << 16)
		    + ((b[9] < 0 ? b[9] + 256 : b[9]) << 8)
		    + (b[10] < 0 ? b[10] + 256 : b[10]);
	    else
		nanos = 0;
	    ts.setNanos(nanos);
	    String s = ts.toString();
	    if (klass.getName().equals("oracle.sql.TIMESTAMPTZ")) {
		int id = ((b[11] < 0 ? b[11] + 256 : b[11]) << 8)
		    + (b[12] < 0 ? b[12] + 256 : b[12]);
		s += " TZ(" + id + ")";
	    }
	    return s;
	} catch (Exception e) {
	    return value.toString();
	}
    }

    private static class OracleTimestampRenderer extends FastTableCellRenderer {
	protected String valueToString(Object value) {
	    return oracleTimestampToString(value);
	}
    }

    private static class DateEditor extends DefaultCellEditor {

	private Class klass;
	private Object value;

	public DateEditor(Class klass) {
	    super(new JTextField());
	    this.klass = klass;
	}

	public boolean stopCellEditing() {
	    String s = (String) super.getCellEditorValue();
	    try {
		if ("".equals(s))
		    value = null;
		else if (klass == java.util.Date.class)
		    value = new java.util.Date(java.sql.Timestamp.valueOf(s).getTime());
		else if (klass == java.sql.Time.class)
		    value = java.sql.Time.valueOf(s);
		else if (klass == java.sql.Date.class)
		    value = java.sql.Date.valueOf(s);
		else // klass == java.sql.Timestamp.class
		    value = java.sql.Timestamp.valueOf(s);
	    }
	    catch (IllegalArgumentException e) {
		((JComponent) getComponent()).setBorder(
					    new LineBorder(Color.red));
		return false;
	    }
	    return super.stopCellEditing();
	}
    }

    private static class OracleTimestampEditor extends DefaultCellEditor {

	private Object value;
	private Class klass;

	public OracleTimestampEditor(Class klass) {
	    super(new JTextField());
	    this.klass = klass;
	}

	public boolean stopCellEditing() {
	    String s = (String) super.getCellEditorValue();
	    if ("".equals(s))
		value = null;
	    else {
		try {
		    if (klass.getName().equals("oracle.sql.TIMESTAMP")) {
			java.sql.Timestamp ts = java.sql.Timestamp.valueOf(s);
			Constructor cnstr = klass.getConstructor(
				new Class[] { java.sql.Timestamp.class });
			value = cnstr.newInstance(new Object[] { ts });
		    } else {
			boolean have_tz = klass.getName().equals(
						    "oracle.sql.TIMESTAMPTZ");
			byte[] b = new byte[have_tz ? 13 : 11];
			if (have_tz) {
			    int tzpos = s.indexOf("TZ(");
			    if (tzpos == -1)
				b[11] = b[12] = 0; // Just guessing...
			    else {
				String tz = s.substring(tzpos + 3);
				s = s.substring(0, tzpos);
				tzpos = tz.indexOf(")");
				if (tzpos != -1)
				    tz = tz.substring(0, tzpos);
				try {
				    int tznum = Integer.parseInt(tz);
				    b[11] = (byte) (tznum >> 8);
				    b[12] = (byte) tznum;
				} catch (NumberFormatException e) {
				    b[11] = b[12] = 0;
				}
			    }
			}
			GregorianCalendar cal = new GregorianCalendar();
			java.sql.Timestamp ts = java.sql.Timestamp.valueOf(s);
			cal.setTimeInMillis(ts.getTime());
			b[0] = (byte) (cal.get(Calendar.YEAR) / 100 + 100);
			b[1] = (byte) (cal.get(Calendar.YEAR) % 100 + 100);
			b[2] = (byte) (cal.get(Calendar.MONTH) + 1);
			b[3] = (byte) cal.get(Calendar.DAY_OF_MONTH);
			b[4] = (byte) cal.get(Calendar.HOUR_OF_DAY);
			b[5] = (byte) cal.get(Calendar.MINUTE);
			b[6] = (byte) cal.get(Calendar.SECOND);
			int nanos = ts.getNanos();
			b[7] = (byte) (nanos >> 24);
			b[8] = (byte) (nanos >> 16);
			b[9] = (byte) (nanos >> 8);
			b[10] = (byte) nanos;
			Constructor cnstr = klass.getConstructor(
					    new Class[] { b.getClass() });
			value = cnstr.newInstance(new Object[] { b });
		    }
		} catch (Exception e) {
		    ((JComponent) getComponent()).setBorder(
						new LineBorder(Color.red));
		    return false;
		}
	    }
	    return super.stopCellEditing();
	}

	public Component getTableCellEditorComponent(JTable table, Object value,
						     boolean isSelected,
						     int row, int column) {
	    this.value = oracleTimestampToString(value);
	    ((JComponent) getComponent()).setBorder(
					    new LineBorder(Color.black));
	    return super.getTableCellEditorComponent(table, this.value,
						     isSelected, row, column);
	}

	public Object getCellEditorValue() {
	    return value;
	}
    }

    private static class GenericEditor extends DefaultCellEditor {

	private Object value;
	private Class klass;

	public GenericEditor(Class klass) {
	    super(new JTextField());
	    this.klass = klass;
	}

	public boolean stopCellEditing() {
	    String s = (String) super.getCellEditorValue();
	    try {
		Constructor cnstr = klass.getConstructor(
				    new Class[] { String.class });
		value = cnstr.newInstance(new Object[] { s });
	    } catch (Exception e) {
		((JComponent) getComponent()).setBorder(
					    new LineBorder(Color.red));
		return false;
	    }
	    return super.stopCellEditing();
	}

	public Component getTableCellEditorComponent(JTable table, Object value,
						     boolean isSelected,
						     int row, int column) {
	    this.value = null;
	    ((JComponent) getComponent()).setBorder(
					    new LineBorder(Color.black));
	    return super.getTableCellEditorComponent(table, value, isSelected,
						     row, column);
	}

	public Object getCellEditorValue() {
	    return value;
	}
    }


    private static class MyTableColumn extends TableColumn {
	private boolean highlighted = false;
	private MyTable table;

	public MyTableColumn(int index, MyTable table) {
	    super(index);
	    this.table = table;
	    headerRenderer = new MyHeaderRenderer(
					createDefaultHeaderRenderer());
	}

	public void setHighlighted(boolean highlighted) {
	    if (this.highlighted == highlighted)
		return;
	    this.highlighted = highlighted;
	    table.getTableHeader().repaint();
	}
	
	private class MyHeaderRenderer implements TableCellRenderer {
	    TableCellRenderer tcr;
	    public MyHeaderRenderer(TableCellRenderer tcr) {
		this.tcr = tcr;
	    }
	    public Component getTableCellRendererComponent(JTable table,
							Object value,
							boolean isSelected,
							boolean hasFocus,
							int row,
							int column) {
		Component comp = tcr.getTableCellRendererComponent(
			    table, value, isSelected, hasFocus, row, column);
		if (highlighted) {
		    Color c = comp.getBackground();
		    comp.setBackground(comp.getForeground());
		    comp.setForeground(c);
		}
		return comp;
	    }
	}
    }


    private static class FastTableCellRenderer extends JComponent
					       implements TableCellRenderer {
	private static Font nonNullFont;
	private static FontMetrics nonNullFontMetrics;
	private static Font nullFont;
	private static FontMetrics nullFontMetrics;
	private static ArrayList unselectedBackground = new ArrayList();
	private static ArrayList unselectedForeground = new ArrayList();
	private static ArrayList unselectedNullColor = new ArrayList();
	private static Color selectedBackground;
	private static Color selectedForeground;
	private static Color selectedNullColor;

	private String value;
	private boolean isSelected;
	private boolean hasFocus;
	private boolean leftJustified;
	private int type;

	private static ArrayList typeColorMap = new ArrayList();

	public FastTableCellRenderer() {
	    initialize();
	    leftJustified = true;
	}

	public FastTableCellRenderer(boolean leftJustified) {
	    initialize();
	    this.leftJustified = leftJustified;
	}

	private static void initialize() {
	    if (nullFont != null)
		return;
	    JTable table = new JTable(1, 1);
	    TableCellRenderer renderer = table.getDefaultRenderer(String.class);
	    Component comp = renderer.getTableCellRendererComponent(
					    table, "foo", false, false, 0, 0);
	    nonNullFont = comp.getFont();
	    nonNullFontMetrics = comp.getFontMetrics(nonNullFont);
	    nullFont = nonNullFont.deriveFont(Font.ITALIC);
	    nullFontMetrics = comp.getFontMetrics(nullFont);

	    Color unsel_bg = comp.getBackground();
	    Color unsel_fg = comp.getForeground();
	    Color unsel_null = mixColors(unsel_bg, unsel_fg);
	    if (unselectedBackground.size() == 0) {
		unselectedBackground.add(unsel_bg);
		unselectedForeground.add(unsel_fg);
		unselectedNullColor.add(unsel_null);
	    } else {
		unselectedBackground.set(0, unsel_bg);
		unselectedForeground.set(0, unsel_fg);
		unselectedNullColor.set(0, unsel_null);
	    }

	    comp = renderer.getTableCellRendererComponent(
					    table, "foo", true, false, 0, 0);
	    selectedBackground = comp.getBackground();
	    selectedForeground = comp.getForeground();
	    selectedNullColor = mixColors(selectedBackground,
					  selectedForeground);
	}

	public static void setTypeColor(int type, Color color) {
	    double value = 0.299 * color.getRed()
			 + 0.587 * color.getGreen()
			 + 0.114 * color.getBlue();
	    Color fg;
	    if (value < 128)
		fg = Color.WHITE;
	    else
		fg = Color.BLACK;
	    Color nl = mixColors(color, fg);
	    try {
		unselectedBackground.set(type, color);
		unselectedForeground.set(type, fg);
		unselectedNullColor.set(type, nl);
	    } catch (IndexOutOfBoundsException e) {
		while (type > unselectedBackground.size()) {
		    unselectedBackground.add(null);
		    unselectedForeground.add(null);
		    unselectedNullColor.add(null);
		}
		unselectedBackground.add(color);
		unselectedForeground.add(fg);
		unselectedNullColor.add(nl);
	    }
	}

	// TODO: this supports coloring in the ColumnJumper menu.
	// It doesn't really belong here, and neither does setTypeColor().
	public static Color getTypeColor(int type, boolean isBackground) {
	    try {
		if (isBackground)
		    return (Color) unselectedBackground.get(type);
		else
		    return (Color) unselectedForeground.get(type);
	    } catch (IndexOutOfBoundsException e) {
		return null;
	    }
	}

	public Component getTableCellRendererComponent(JTable table,
						    Object value,
						    boolean isSelected,
						    boolean hasFocus,
						    int row,
						    int column) {
	    this.value = valueToString(value);
	    this.isSelected = isSelected;
	    this.hasFocus = hasFocus;
	    column = table.convertColumnIndexToModel(column);
	    if (column < ((MyTable) table).columnTypeMap.size())
		type = ((Integer) ((MyTable) table).columnTypeMap.get(column)).intValue();
	    else
		type = 0;
	    // Special type for columns that are both PK and FK; we don't want
	    // to use a special color for this, but instead we use the PK and
	    // FK colors on alternating rows.
	    if (type == 3)
		type = (row & 1) == 0 ? 1 : 2;
	    return this;
	}

	protected String valueToString(Object value) {
	    return value == null ? null : value.toString();
	}

	public Dimension getPreferredSize() {
	    // This is the method this class is really all about. The
	    // combination of setting the value and getting the preferred size,
	    // using DefaultTableCellRenderer, is majorly slowed down by all
	    // kinds of overheads (property change notifications and whatnot).
	    // This renderer has virtually zero overhead -- and so now the
	    // various toString() methods are actually becoming hot spots!

	    FontMetrics fm = value == null ? nullFontMetrics
					   : nonNullFontMetrics;
	    String v = value == null ? "null" : value;
	    int h = fm.getAscent() + fm.getDescent() + 2;
	    int w = fm.stringWidth(v) + 2;
	    return new Dimension(w, h);
	}

	public void paint(Graphics g) {
	    Dimension d = getSize();
	    if (hasFocus) {
		g.setColor(selectedBackground);
		g.drawRect(0, 0, d.width - 1, d.height - 1);
		g.setColor((Color) unselectedBackground.get(type));
		g.fillRect(1, 1, d.width - 2, d.height - 2);
		if (value == null)
		    g.setColor((Color) unselectedNullColor.get(type));
		else
		    g.setColor((Color) unselectedForeground.get(type));
	    } else if (isSelected) {
		g.setColor(selectedBackground);
		g.fillRect(0, 0, d.width, d.height);
		if (value == null)
		    g.setColor(selectedNullColor);
		else
		    g.setColor(selectedForeground);
	    } else {
		g.setColor((Color) unselectedBackground.get(type));
		g.fillRect(0, 0, d.width, d.height);
		if (value == null)
		    g.setColor((Color) unselectedNullColor.get(type));
		else
		    g.setColor((Color) unselectedForeground.get(type));
	    }
	    Font f;
	    FontMetrics fm;
	    String v;
	    if (value == null) {
		f = nullFont;
		fm = nullFontMetrics;
		v = "null";
	    } else {
		f = nonNullFont;
		fm = nonNullFontMetrics;
		v = value;
	    }
	    int w = fm.stringWidth(v);
	    if (w > d.width - 2) {
		int xarrow = d.width - 6;
		int yarrow = d.height / 2;
		g.drawLine(xarrow, yarrow - 3, xarrow, yarrow + 3);
		g.drawLine(xarrow + 1, yarrow - 2, xarrow + 1, yarrow + 2);
		g.drawLine(xarrow + 2, yarrow - 1, xarrow + 2, yarrow + 1);
		g.drawLine(xarrow + 3, yarrow, xarrow + 3, yarrow);
		g.clipRect(1, 1, d.width - 8, d.height - 2);
	    } else
		g.clipRect(1, 1, d.width - 2, d.height - 2);
	    g.setFont(f);
	    int x;
	    if (leftJustified)
		x = 1;
	    else {
		x = getWidth() - fm.stringWidth(v) - 1;
		if (x < 1)
		    x = 1;
	    }
	    g.drawString(v, x, fm.getAscent());
	}

	private static Color mixColors(Color a, Color b) {
	    return new Color((a.getRed() + b.getRed()) / 2,
			    (a.getGreen() + b.getGreen()) / 2,
			    (a.getBlue() + b.getBlue()) / 2);
	}
    }
}
