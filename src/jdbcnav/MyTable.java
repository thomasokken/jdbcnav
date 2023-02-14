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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultCellEditor;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import jdbcnav.model.TypeSpec;
import jdbcnav.model.TypeSpecTableModel;
import jdbcnav.util.MenuLayout;
import jdbcnav.util.MiscUtils;
import jdbcnav.util.MyTextField;


public class MyTable extends JTable {
    
    private static final int MAX_COLUMN_WIDTH = 512;
    private static final String ILLEGAL = new String("illegal");

    private int highlightIndex = 0;
    private ArrayList<UserInteractionListener> userInteractionListeners = new ArrayList<UserInteractionListener>();
    private boolean[] notNull;
    private ArrayList<Integer> columnTypeMap;
    private EditHandler editHandler = null;

    public MyTable(TableModel dm) {
        super();
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setModel(dm);
        setCellSelectionEnabled(true);

        FastTableCellRenderer ljr = new FastTableCellRenderer();
        FastTableCellRenderer rjr = new FastTableCellRenderer(false);
        DatabaseObjectRenderer dbor = new DatabaseObjectRenderer();
        setDefaultRenderer(String.class, ljr);
        setDefaultRenderer(java.util.Date.class, new UtilDateRenderer());
        setDefaultRenderer(java.sql.Time.class, ljr);
        setDefaultRenderer(java.sql.Date.class, ljr);
        setDefaultRenderer(java.sql.Timestamp.class, ljr);
        setDefaultRenderer(Number.class, rjr);
        setDefaultRenderer(Float.class, rjr);
        setDefaultRenderer(Double.class, rjr);
        setDefaultRenderer(Boolean.class, ljr);
        setDefaultRenderer(Object.class, ljr);
        setDefaultRenderer(jdbcnav.model.DateTime.class, dbor);
        setDefaultRenderer(jdbcnav.model.Interval.class, dbor);
        setDefaultRenderer(TypeSpec.class, dbor);

        DatabaseObjectEditor dboe = new DatabaseObjectEditor();
        setDefaultEditor(java.util.Date.class,
                            new DateEditor(java.util.Date.class));
        setDefaultEditor(java.sql.Time.class,
                            new DateEditor(java.sql.Time.class));
        setDefaultEditor(java.sql.Date.class,
                            new DateEditor(java.sql.Date.class));
        setDefaultEditor(java.sql.Timestamp.class,
                            new DateEditor(java.sql.Timestamp.class));
        GenericEditor ge = new GenericEditor();
        setDefaultEditor(Object.class, ge);
        setDefaultEditor(Boolean.class, ge);
        setDefaultEditor(Number.class, new NumberEditor());
        setDefaultEditor(jdbcnav.model.DateTime.class, dboe);
        setDefaultEditor(jdbcnav.model.Interval.class, dboe);
        setDefaultEditor(TypeSpec.class, dboe);

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
        columnTypeMap = new ArrayList<Integer>();
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

    public interface EditHandler {
        void cutCell();
        void cutRow();
        void copyCell();
        void copyRow();
        void paste();
    }

    public void setEditHandler(EditHandler eh) {
        editHandler = eh;
        KeyStroke ctrl_x = KeyStroke.getKeyStroke('X', MiscUtils.getMenuShortcutKeyMask());
        KeyStroke ctrl_shift_x = KeyStroke.getKeyStroke('X', MiscUtils.SHIFT_MASK | MiscUtils.getMenuShortcutKeyMask());
        KeyStroke ctrl_c = KeyStroke.getKeyStroke('C', MiscUtils.getMenuShortcutKeyMask());
        KeyStroke ctrl_shift_c = KeyStroke.getKeyStroke('C', MiscUtils.SHIFT_MASK | MiscUtils.getMenuShortcutKeyMask());
        KeyStroke ctrl_v = KeyStroke.getKeyStroke('V', MiscUtils.getMenuShortcutKeyMask());
        InputMap im = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        Object cut_cell_id = im.get(ctrl_x);
        Object cut_row_id = im.get(ctrl_shift_x);
        Object copy_cell_id = im.get(ctrl_c);
        Object copy_row_id = im.get(ctrl_shift_c);
        Object paste_id = im.get(ctrl_v);
        ActionMap am = getActionMap();
        if (editHandler == null) {
            am.remove(cut_cell_id);
            am.remove(cut_row_id);
            am.remove(copy_cell_id);
            am.remove(copy_row_id);
            am.remove(paste_id);
        } else {
            am.put(cut_cell_id,
                    new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            editHandler.cutCell();
                        }
                    });
            am.put(cut_row_id,
                    new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            editHandler.cutRow();
                        }
                    });
            am.put(copy_cell_id,
                    new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            editHandler.copyCell();
                        }
                    });
            am.put(copy_row_id,
                    new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            editHandler.copyRow();
                        }
                    });
            am.put(paste_id,
                    new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            editHandler.paste();
                        }
                    });
        }
    }

    public void setColumnNotNull(int column, boolean notNull) {
        this.notNull[column] = notNull;
    }

    public void setColumnType(int column, int type) {
        try {
            columnTypeMap.set(column, type);
        } catch (IndexOutOfBoundsException e) {
            while (column > columnTypeMap.size())
                columnTypeMap.add(0);
            columnTypeMap.add(type);
        }
    }

    public int getColumnType(int column) {
        try {
            return columnTypeMap.get(column);
        } catch (IndexOutOfBoundsException e) {
            return 0;
        }
    }

    public static void setTypeColor(int type, Color color) {
        FastTableCellRenderer.setTypeColor(type, color);
    }


    /**
     * The JTable version of this method looks in
     * <code>defaultRenderersByColumnClass</code> for the given class; if it is
     * not found, it looks for the superclass, then the superclass' superclass,
     * etc.
     * <br>
     * My version also looks for all of the interfaces implemented by a class,
     * before moving on to the superclass. This allows me to set renderers for
     * things like <code>java.sql.Blob</code>, which is an interface, and so
     * could not be used with the original code.
     * <br>
     * Also, in the case where <code>columnClass</code> is an interface that
     * does not match anything in <code>defaultRenderersByColumnClass</code>,
     * this method will return the renderer for <code>Object.class</code>,
     * so as long as that renderer exists, this method will never return
     * <code>null</code>.
     */
    public TableCellRenderer getDefaultRenderer(Class<?> columnClass) {
        TableCellRenderer tcr = null;
        if (columnClass != null)
            tcr = getDefaultRenderer2(columnClass);
        if (tcr != null)
            return tcr;
        else
            return (TableCellRenderer)
                            defaultRenderersByColumnClass.get(Object.class);
    }

    private TableCellRenderer getDefaultRenderer2(Class<?> columnClass) {
        Object renderer = defaultRenderersByColumnClass.get(columnClass);
        if (renderer != null)
            return (TableCellRenderer) renderer;
        Class<?>[] interfaces = columnClass.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            TableCellRenderer tcr = getDefaultRenderer2(interfaces[i]);
            if (tcr != null)
                return tcr;
        }
        Class<?> superclass = columnClass.getSuperclass();
        if (superclass == null)
            return null;
        else
            return getDefaultRenderer2(superclass);
    }

    /**
     * The JTable version of this method looks in
     * <code>defaultEditorsByColumnClass</code> for the given class; if it is
     * not found, it looks for the superclass, then the superclass' superclass,
     * etc.
     * <br>
     * My version also looks for all of the interfaces implemented by a class,
     * before moving on to the superclass. This allows me to set editors for
     * things like <code>java.sql.Blob</code>, which is an interface, and so
     * could not be used with the original code.
     * <br>
     * Also, in the case where <code>columnClass</code> is an interface that
     * does not match anything in <code>defaultEditorsByColumnClass</code>,
     * this method will return the editor for <code>Object.class</code>,
     * so as long as that editor exists, this method will never return
     * <code>null</code>.
     */
    public TableCellEditor getDefaultEditor(Class<?> columnClass) {
        TableCellEditor tce = null;
        if (columnClass != null)
            tce = getDefaultEditor2(columnClass);
        if (tce != null)
            return tce;
        else
            return (TableCellEditor)
                            defaultEditorsByColumnClass.get(Object.class);
    }

    private TableCellEditor getDefaultEditor2(Class<?> columnClass) {
        Object editor = defaultEditorsByColumnClass.get(columnClass);
        if (editor != null)
            return (TableCellEditor) editor;
        Class<?>[] interfaces = columnClass.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            TableCellEditor tce = getDefaultEditor2(interfaces[i]);
            if (tce != null)
                return tce;
        }
        Class<?> superclass = columnClass.getSuperclass();
        if (superclass == null)
            return null;
        else
            return getDefaultEditor2(superclass);
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

    @SuppressWarnings("unchecked")
    private void eventInScrollBarHappened() {
        ArrayList<UserInteractionListener> listeners = (ArrayList<UserInteractionListener>) userInteractionListeners.clone();
        for (UserInteractionListener listener : listeners)
            listener.eventInScrollBar();
    }

    @SuppressWarnings("unchecked")
    private void eventInTableHappened() {
        ArrayList<UserInteractionListener> listeners = (ArrayList<UserInteractionListener>) userInteractionListeners.clone();
        for (UserInteractionListener listener : listeners)
            listener.eventInTable();
    }

    public void addNotify() {
        super.addNotify();

        JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(
                                JScrollPane.class, this);
        if (scroll != null) {
            JScrollBar hscroll = scroll.getHorizontalScrollBar();
            JScrollBar vscroll = scroll.getVerticalScrollBar();
            if (hscroll != null || vscroll != null) {
                MouseListener ml =
                    new MouseAdapter() {
                        public void mouseReleased(MouseEvent e) {
                            eventInScrollBarHappened();
                        }
                    };
                KeyListener kl =
                    new KeyAdapter() {
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

        if (e.getButton() == MouseEvent.BUTTON3) {
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
                    int type = columnTypeMap.get(col);
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
            clearSelection();
            addColumnSelectionInterval(0, getColumnCount() - 1);
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

    private static class DatabaseObjectRenderer extends FastTableCellRenderer {
        private TypeSpec spec;

        protected String valueToString(Object value) {
            try {
                return spec.objectToString(value);
            } catch (RuntimeException e) {
                return ILLEGAL;
            }
        }

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            TypeSpecTableModel m = (TypeSpecTableModel) table.getModel();
            spec = m.getTypeSpec(table.convertColumnIndexToModel(column));
            leftJustified = !Number.class.isAssignableFrom(spec.jdbcJavaClass);
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    // I define my own GenericEditor and NumberEditor, cloned from JTable, so
    // that I can make sure they use a MyTextField instead of a JTextField.
    // This is necessary to support automatic refreshing of the Clipboard
    // window.

    private static class GenericEditor extends DefaultCellEditor {

        private Class<?>[] argTypes = new Class[]{String.class};
        private java.lang.reflect.Constructor<?> constructor;
        protected Object value;

        public GenericEditor() {
            super(new MyTextField());
        }

        public boolean stopCellEditing() {
            String s = (String)super.getCellEditorValue();
            // Here we are dealing with the case where a user
            // has deleted the string value in a cell, possibly
            // after a failed validation. Return null, so that
            // they have the option to replace the value with
            // null or use escape to restore the original.
            // For Strings, return "" for backward compatibility.
            if ("".equals(s)) {
                if (constructor.getDeclaringClass() == String.class) {
                    value = s;
                }
                super.stopCellEditing();
            }

            try {
                value = constructor.newInstance(new Object[]{s});
            }
            catch (Exception e) {
                ((JComponent)getComponent()).setBorder(new LineBorder(Color.red));
                return false;
            }
            return super.stopCellEditing();
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected,
                                                 int row, int column) {
            this.value = null;
            ((JComponent)getComponent()).setBorder(new LineBorder(Color.black));
            try {
                Class<?> type = table.getColumnClass(column);
                // Since our obligation is to produce a value which is
                // assignable for the required type it is OK to use the
                // String constructor for columns which are declared
                // to contain Objects. A String is an Object.
                if (type == Object.class) {
                    type = String.class;
                }
                constructor = type.getConstructor(argTypes);
            }
            catch (Exception e) {
                return null;
            }
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }

        public Object getCellEditorValue() {
            return value;
        }
    }

    private static class NumberEditor extends GenericEditor {
        public NumberEditor() {
            ((JTextField)getComponent()).setHorizontalAlignment(JTextField.RIGHT);
        }
    }

    private static class DateEditor extends GenericEditor {

        private Class<?> klass;

        public DateEditor(Class<?> klass) {
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

    private static class DatabaseObjectEditor extends DefaultCellEditor {

        private TypeSpec spec;
        private Object value;

        public DatabaseObjectEditor() {
            super(new MyTextField());
        }

        public boolean stopCellEditing() {
            String s = (String) super.getCellEditorValue();
            if (!java.sql.Clob.class.isAssignableFrom(spec.jdbcJavaClass))
                try {
                    value = spec.stringToObject(s);
                } catch (IllegalArgumentException e) {
                    ((JComponent) getComponent()).setBorder(
                                                new LineBorder(Color.red));
                    return false;
                }
            return super.stopCellEditing();
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected,
                                                     int row, int column) {
            TypeSpecTableModel m = (TypeSpecTableModel) table.getModel();
            column = table.convertColumnIndexToModel(column);
            spec = m.getTypeSpec(column);
            try {
                this.value = spec.objectToString(value);
            } catch (RuntimeException e) {
                this.value = null;
            }
            JTextField tf = (JTextField) getComponent();
            tf.setBorder(new LineBorder(Color.black));
            tf.setHorizontalAlignment(
                    Number.class.isAssignableFrom(spec.jdbcJavaClass)
                                        ? JTextField.RIGHT : JTextField.LEFT);
            return super.getTableCellEditorComponent(table, this.value,
                                                     isSelected, row, column);
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
        private static ArrayList<Color> unselectedBackground = new ArrayList<Color>();
        private static ArrayList<Color> unselectedForeground = new ArrayList<Color>();
        private static ArrayList<Color> unselectedNullColor = new ArrayList<Color>();
        private static Color selectedBackground;
        private static Color selectedForeground;
        private static Color selectedNullColor;

        private String value;
        private boolean isSelected;
        private boolean hasFocus;
        protected boolean leftJustified;
        private int type;

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
                    return unselectedBackground.get(type);
                else
                    return unselectedForeground.get(type);
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
                type = ((MyTable) table).columnTypeMap.get(column);
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

            FontMetrics fm = value == null || value == ILLEGAL
                                                ? nullFontMetrics
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
                g.setColor(unselectedBackground.get(type));
                g.fillRect(1, 1, d.width - 2, d.height - 2);
                if (value == null || value == ILLEGAL)
                    g.setColor(unselectedNullColor.get(type));
                else
                    g.setColor(unselectedForeground.get(type));
            } else if (isSelected) {
                g.setColor(selectedBackground);
                g.fillRect(0, 0, d.width, d.height);
                if (value == null || value == ILLEGAL)
                    g.setColor(selectedNullColor);
                else
                    g.setColor(selectedForeground);
            } else {
                g.setColor(unselectedBackground.get(type));
                g.fillRect(0, 0, d.width, d.height);
                if (value == null || value == ILLEGAL)
                    g.setColor(unselectedNullColor.get(type));
                else
                    g.setColor(unselectedForeground.get(type));
            }
            Font f;
            FontMetrics fm;
            String v;
            if (value == null) {
                f = nullFont;
                fm = nullFontMetrics;
                v = "null";
            } else if (value == ILLEGAL) {
                f = nullFont;
                fm = nullFontMetrics;
                v = value;
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
            g.drawString(v, x, fm.getAscent() - 1);
        }

        private static Color mixColors(Color a, Color b) {
            return new Color((a.getRed() + b.getRed()) / 2,
                            (a.getGreen() + b.getGreen()) / 2,
                            (a.getBlue() + b.getBlue()) / 2);
        }
    }
}
