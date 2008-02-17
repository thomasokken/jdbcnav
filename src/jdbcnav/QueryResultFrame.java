///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2008  Thomas Okken
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
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import jdbcnav.model.*;
import jdbcnav.util.*;


public class QueryResultFrame extends MyFrame
			      implements ResultSetTableModel.UndoListener {
    protected BrowserFrame browser;
    protected ResultSetTableModel model;
    private String query;
    protected Table dbTable;
    protected MyTable table;
    private Data.StateListener datastatelistener;
    private boolean askedToShow, initializationFinished;
    private boolean sortAfterLoading;
    private Thread tenSecondDelay;
    private boolean editable;
    private JMenuBar menubar;
    private JMenu progressMenu;
    private JMenuItem commitMI;
    private JMenuItem rollbackMI;
    private JMenu editMenu;
    private JMenuItem cutMI;
    private JMenuItem copyMI;
    private JMenuItem setCellNullMI;
    private JMenuItem editCellMI;
    private JMenuItem undoMI;
    private JMenuItem redoMI;
    private JMenuItem deleteRowMI;
    private SequenceDialog sequenceDialog;
    private RowOrCellDialog rowOrCellDialog;
    private ColumnMatchDialog columnMatchDialog;

    private QueryResultFrame(BrowserFrame browser, Table dbTable, String query,
			     Data queryOutput) throws NavigatorException {
	super(browser.getTitle() + "/" + (dbTable == null ? "query-output"
	    : (dbTable.getName() +
		(dbTable.isUpdatableQueryResult() ? " (query)" : ""))),
					    true, true, true, true);
	this.browser = browser;
	this.query = query;
	this.dbTable = dbTable;
	editable = dbTable != null && dbTable.isEditable();
	
	if (dbTable == null)
	    model = new ResultSetTableModel(queryOutput, null);
	else
	    model = dbTable.createModel();

	setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
	addInternalFrameListener(new InternalFrameAdapter() {
			public void internalFrameClosing(InternalFrameEvent e) {
			    nuke();
			}
		    });
	
	menubar = new JMenuBar();
	JMenuItem mi;

	JMenu m = new JMenu("Table");
	if (editable) {
	    commitMI = new JMenuItem("Commit");
	    commitMI.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					commit();
				    }
				});
	    commitMI.setEnabled(model.isDirty());
	    m.add(commitMI);
	    rollbackMI = new JMenuItem("Rollback");
	    rollbackMI.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					rollback();
				    }
				});
	    rollbackMI.setEnabled(model.isDirty());
	    m.add(rollbackMI);
	    m.addSeparator();
	}
	mi = new JMenuItem("Reload");
	mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    reload();
				}
			    });
	m.add(mi);
	mi = new JMenuItem("Re-Sort");
	mi.setAccelerator(KeyStroke.getKeyStroke('S', Event.ALT_MASK));
	mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    model.sort();
				}
			    });
	m.add(mi);
	if (editable) {
	    mi = new JMenuItem("Import CSV...");
	    mi.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					doImport();
				    }
				});
	    m.add(mi);
	}
	mi = new JMenuItem("Export CSV...");
	mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    export();
				}
			    });
	m.add(mi);
	m.addSeparator();
	mi = new JMenuItem("Close");
	mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    nuke();
				}
			    });
	mi.setAccelerator(KeyStroke.getKeyStroke('W', Event.CTRL_MASK));
	m.add(mi);
	menubar.add(m);

	editMenu = new JMenu("Edit");
	if (editable) {
	    undoMI = new JMenuItem("Undo");
	    undoMI.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    undo();
				}
			    });
	    String undo = model.getUndoTitle();
	    if (undo != null)
		undoMI.setText(undo);
	    else
		undoMI.setEnabled(false);
	    undoMI.setAccelerator(KeyStroke.getKeyStroke('Z', Event.CTRL_MASK));
	    editMenu.add(undoMI);
	    redoMI = new JMenuItem("Redo");
	    redoMI.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    redo();
				}
			    });
	    String redo = model.getRedoTitle();
	    if (redo != null)
		redoMI.setText(redo);
	    else
		redoMI.setEnabled(false);
	    redoMI.setAccelerator(KeyStroke.getKeyStroke('Y', Event.CTRL_MASK));
	    editMenu.add(redoMI);
	    editMenu.addSeparator();
	    cutMI = new JMenuItem("Cut");
	    cutMI.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					cut();
				    }
				});
	    cutMI.setEnabled(false);
	    cutMI.setAccelerator(KeyStroke.getKeyStroke('X', Event.CTRL_MASK));
	    editMenu.add(cutMI);
	}
	copyMI = new JMenuItem("Copy");
	copyMI.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    copy();
				}
			    });
	copyMI.setEnabled(false);
	copyMI.setAccelerator(KeyStroke.getKeyStroke('C', Event.CTRL_MASK));
	editMenu.add(copyMI);
	if (editable) {
	    mi = new JMenuItem("Paste");
	    mi.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					paste();
				    }
				});
	    mi.setAccelerator(KeyStroke.getKeyStroke('V', Event.CTRL_MASK));
	    editMenu.add(mi);
	    editMenu.addSeparator();
	    mi = new JMenuItem("Insert Row");
	    mi.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					insertRow();
				    }
				});
	    editMenu.add(mi);
	    deleteRowMI = new JMenuItem("Delete Row");
	    deleteRowMI.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					deleteRow();
				    }
				});
	    deleteRowMI.setEnabled(false);
	    editMenu.add(deleteRowMI);
	    setCellNullMI = new JMenuItem("Set Cell to Null");
	    setCellNullMI.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					setCellToNull();
				    }
				});
	    setCellNullMI.setEnabled(false);
	    setCellNullMI.setAccelerator(
				KeyStroke.getKeyStroke('N', Event.CTRL_MASK));
	    editMenu.add(setCellNullMI);
	    editCellMI = new JMenuItem("Edit Cell");
	    editCellMI.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					editCell();
				    }
				});
	    editCellMI.setEnabled(false);
	    editCellMI.setAccelerator(
				KeyStroke.getKeyStroke('E', Event.CTRL_MASK));
	    editMenu.add(editCellMI);
	} else {
	    editCellMI = new JMenuItem("View Cell");
	    editCellMI.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					editCell();
				    }
				});
	    editCellMI.setEnabled(false);
	    editCellMI.setAccelerator(
				KeyStroke.getKeyStroke('E', Event.CTRL_MASK));
	    editMenu.add(editCellMI);
	}
	editMenu.setEnabled(editable);
	menubar.add(editMenu);

	progressMenu = new JMenu("0 rows");
	mi = new JMenuItem("Pause");
	mi.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					pause();
				    }
				});
	progressMenu.add(mi);
	mi = new JMenuItem("Resume");
	mi.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					resume();
				    }
				});
	progressMenu.add(mi);
	mi = new JMenuItem("Finish");
	mi.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					finish();
				    }
				});
	progressMenu.add(mi);
	menubar.add(progressMenu);
	
	setJMenuBar(menubar);

	Container c = getContentPane();
	c.setLayout(new GridLayout(1, 1));
	table = new MyTable(model);
	table.setEditHandler(new MyTable.EditHandler() {
		    public void cut() {
			QueryResultFrame.this.cut();
		    }
		    public void copy() {
			QueryResultFrame.this.copy();
		    }
		    public void paste() {
			QueryResultFrame.this.paste();
		    }
		});
	model.setTable(table);
	sortAfterLoading = true;
	
	// We put off the remainder of our initialization until we have
	// loaded 50 rows, or waited 10 seconds, or loading has finished,
	// whichever comes first.
	datastatelistener = new DataStateListener();
	model.addStateListener(datastatelistener);
	table.addUserInteractionListener(new MyUserInteractionListener());
	tenSecondDelay = new Thread(new WaitTenSecondsThenFinish());
	tenSecondDelay.setPriority(Thread.MIN_PRIORITY);
	tenSecondDelay.setDaemon(true);
	tenSecondDelay.start();
    }

    public QueryResultFrame(BrowserFrame browser, Table table)
						throws NavigatorException {
	this(browser, table, null, null);
    }

    public QueryResultFrame(BrowserFrame browser, String query,
			    Data queryOutput) throws NavigatorException {
	this(browser, null, query, queryOutput);
    }

    public void dispose() {
	tenSecondDelay = null;
	model.setState(Data.FINISHED);
	model.setTable(null);
	super.dispose();
    }


    private class DataStateListener implements Data.StateListener {
	private class DelayedStateChanged implements Runnable {
	    private int state, row;
	    public DelayedStateChanged(int state, int row) {
		this.state = state;
		this.row = row;
	    }
	    public void run() {
		stateChanged(state, row);
	    }
	}
	public void stateChanged(int state, int row) {
	    if (SwingUtilities.isEventDispatchThread())
		stateChanged2(state, row);
	    else
		SwingUtilities.invokeLater(new DelayedStateChanged(state, row));
	}
	private void stateChanged2(int state, int row) {
	    if (state == Data.FINISHED) {
		menubar.remove(progressMenu);
		menubar.validate();
		menubar.repaint();
		if (sortAfterLoading)
		    model.sort();
		datastatelistener = null;
	    } else
		progressMenu.setText(Integer.toString(row) + " rows");
	    if (state == Data.FINISHED || row >= 50)
		finishInitialization();
	}
    }

    private class WaitTenSecondsThenFinish implements Runnable {
	private long startTime;
	public void run() {
	    startTime = new Date().getTime();
	    long timeToWait = 10000;
	    do {
		try {
		    Thread.sleep(timeToWait);
		} catch (InterruptedException e) {}
		timeToWait = startTime + 10000 - new Date().getTime();
	    } while (timeToWait > 0);
	    // Check if we're still needed!
	    if (tenSecondDelay != null)
		if (SwingUtilities.isEventDispatchThread())
		    finishInitialization();
		else
		    SwingUtilities.invokeLater(new Runnable() {
			    public void run() {
				finishInitialization();
			    }
			});
	}
    }

    private class MyUserInteractionListener
				implements MyTable.UserInteractionListener {
	public void eventInScrollBar() {
	    sortAfterLoading = false;
	    table.removeUserInteractionListener(this);
	}
	public void eventInTable() {
	    sortAfterLoading = false;
	    table.removeUserInteractionListener(this);
	}
    }

    private void finishInitialization() {
	if (initializationFinished)
	    // Only once, please
	    return;

	model.sort();
	if (editable) {
	    model.addUndoListener(this);
	    model.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
			    modelChanged();
			}
		    });
	}
	table.getSelectionModel().addListSelectionListener(
			    new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
				    selectionChanged();
				}
			    });

	table.setNiceSize();
	JScrollPane sp = new JScrollPane(table,
				    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				    JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	getContentPane().add(sp);
	pack();

	// aaargh...
	Dimension d = getSize();
	Dimension ds = Main.getDesktop().getSize();
	d.height += table.getTableHeader().getPreferredSize().height;
	if (d.height > ds.height)
	    d.height = ds.height;
	if (d.width > ds.width)
	    d.width = ds.width;
	setSize(d);

	initializationFinished = true;
	if (askedToShow)
	    SwingUtilities.invokeLater(new Runnable() {
					    public void run() {
						showStaggered();
					    }
					});
    }

    private void pause() {
	model.setState(Data.PAUSED);
    }

    private void resume() {
	model.setState(Data.LOADING);
    }

    private void finish() {
	model.setState(Data.FINISHED);
    }

    public void showStaggered() {
	if (initializationFinished)
	    super.showStaggered();
	else
	    askedToShow = true;
    }

    private void commit() {
	// Orphans can't be committed; if the user wants to keep the data,
	// they'll have to export it to a CSV file, or copy it to another
	// table.

	if (dbTable.getQualifiedName().indexOf("...") != -1) {
	    Toolkit.getDefaultToolkit().beep();
	    JOptionPane.showInternalMessageDialog(Main.getDesktop(),
			"You cannot commit tables once they are in the\n"
			+ "orphanage. If you want to keep this data, you\n"
			+ "will have to copy it to another table, or you\n"
			+ "could export it to a CSV file.");
	    return;
	}

	// Updatable query results (PartialTable instances) are handled
	// differently; they are not tracked in the Database's list of
	// dirty tables, and committed by themselves.

	if (dbTable.isUpdatableQueryResult()) {
	    Toolkit.getDefaultToolkit().beep();
	    if (JOptionPane.showInternalConfirmDialog(Main.getDesktop(),
			"Are you sure you want to commit all\n"
			+ "the changes you made in this window?\n"
			+ "This is not undoable!",
			"Confirm", JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION)
		return;
	    ArrayList list = new ArrayList();
	    list.add(dbTable);
	    try {
		dbTable.getDatabase().commitTables(list);
	    } catch (NavigatorException e) {
		MessageBox.show(e);
	    }
	    return;
	}

	Collection dirty = dbTable.getDatabase().getDirtyTables();
	if (dirty.size() == 1 && dirty.iterator().next() == dbTable) {
	    Toolkit.getDefaultToolkit().beep();
	    if (JOptionPane.showInternalConfirmDialog(Main.getDesktop(),
			"Are you sure you want to commit all\n"
			+ "the changes you made to this table?\n"
			+ "This is not undoable!",
			"Confirm", JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION)
		return;
	    ArrayList list = new ArrayList();
	    list.add(dbTable);
	    try {
		dbTable.getDatabase().commitTables(list);
	    } catch (NavigatorException e) {
		MessageBox.show(e);
	    }
	} else {
	    MultiCommitDialog mcd = new MultiCommitDialog(dbTable, dirty, true);
	    mcd.setParent(this);
	    mcd.showCentered();
	}
    }

    private void rollback() {
	Toolkit.getDefaultToolkit().beep();
	if (JOptionPane.showInternalConfirmDialog(Main.getDesktop(),
		    "Are you sure you want to discard all\n"
		    + "the changes you made to this table?\n"
		    + "This is not undoable!",
		    "Confirm", JOptionPane.YES_NO_OPTION,
		    JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION)
	    return;
	table.stopEditing();
	table.clearSelection();
	model.rollback();
    }

    private void reload() {
	if (model.isDirty()) {
	    Toolkit.getDefaultToolkit().beep();
	    if (JOptionPane.showInternalConfirmDialog(
			Main.getDesktop(),
			"Are you sure you want to discard all\n"
			+ "the changes you made to this table?\n"
			+ "This is not undoable!",
			"Confirm", JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION)
		return;
	}
	table.stopEditing();
	table.clearSelection();
	try {
	    if (dbTable != null)
		dbTable.reload();
	    else {
		Database db = browser.getDatabase();
		Data data = (Data) db.runQuery(query, true, false);
		model.load(data);
	    }
	    progressMenu.setText("0 rows");
	    menubar.add(progressMenu);
	    menubar.validate();
	    menubar.repaint();
	} catch (NavigatorException e) {
	    MessageBox.show("Reload failed!", e);
	    dispose();
	}
	sortAfterLoading = true;
	datastatelistener = new DataStateListener();
	model.addStateListener(datastatelistener);
	table.addUserInteractionListener(new MyUserInteractionListener());
    }

    private void doImport() {
	JFileChooser jfc = new JFileChooser();
	jfc.setDialogTitle("Import");
	JPanel p1 = new JPanel(new MyGridBagLayout());
	MyGridBagConstraints gbc = new MyGridBagConstraints();
	JCheckBox cb = new JCheckBox("First row contains column names");
	cb.setSelected(true);
	gbc.gridx = 0;
	gbc.gridy = 0;
	p1.add(cb, gbc);
	JRadioButton rb1 = new JRadioButton("Import only non-conflicting rows");
	JRadioButton rb2 = new JRadioButton("Overwrite conflicting rows");
	JRadioButton rb3 = new JRadioButton("Replace conflicting rows");
	JRadioButton rb4 = new JRadioButton("Replace all existing rows");
	JPanel p2 = new JPanel(new GridLayout(4, 1));
	p2.add(rb1);
	p2.add(rb2);
	p2.add(rb3);
	p2.add(rb4);
	ButtonGroup bg = new ButtonGroup();
	bg.add(rb1);
	bg.add(rb2);
	bg.add(rb3);
	bg.add(rb4);
	rb2.setSelected(true);
	gbc.gridy = 1;
	p1.add(p2, gbc);
	jfc.setAccessory(p1);
	if (jfc.showOpenDialog(Main.getDesktop())
					== JFileChooser.APPROVE_OPTION) {
	    File file = jfc.getSelectedFile();
	    int importMode;
	    if (rb1.isSelected())
		importMode = ResultSetTableModel.GENTLE;
	    else if (rb2.isSelected())
		importMode = ResultSetTableModel.ASSERTIVE;
	    else if (rb3.isSelected())
		importMode = ResultSetTableModel.RUDE;
	    else
		importMode = ResultSetTableModel.VICIOUS;
	    if (cb.isSelected()) {
		table.stopEditing();
		table.clearSelection();
		model.doImport(file, importMode, null);
	    } else {
		if (sequenceDialog != null)
		    sequenceDialog.dispose();
		sequenceDialog = new SequenceDialog(model.getHeaders(),
					    new SeqListener(file, importMode));
		sequenceDialog.setParent(this);
		sequenceDialog.showCentered();
	    }
	}
    }

    private class SeqListener implements SequenceDialog.Listener {
	private File file;
	private int importMode;
	public SeqListener(File file, int importMode) {
	    this.file = file;
	    this.importMode = importMode;
	}
	public void done(String[] headers) {
	    table.stopEditing();
	    table.clearSelection();
	    model.doImport(file, importMode, headers);
	}
    }
    
    private void export() {
	JFileChooser jfc = new JFileChooser();
	jfc.setDialogTitle("Export");
	JCheckBox cb = new JCheckBox("Write column names as first row");
	cb.setSelected(true);
	jfc.setAccessory(cb);
	jfc.setSelectedFile(new File((dbTable == null ? "query-output"
		    : dbTable.getName()) + ".csv"));
	if (jfc.showSaveDialog(Main.getDesktop())
				    == JFileChooser.APPROVE_OPTION) {
	    table.stopEditing();
	    model.export(jfc.getSelectedFile(), cb.isSelected());
	}
    }
    
    private void undo() {
	table.cancelEditing();
	table.clearSelection();
	model.undo();
    }

    private void redo() {
	table.cancelEditing();
	table.clearSelection();
	model.redo();
    }

    private void cut() {
	if (table.isEditing()) {
	    ((JTextField) table.getEditorComponent()).cut();
	    return;
	}
	if (table.getSelectedRows().length == 1) {
	    if (rowOrCellDialog != null)
		rowOrCellDialog.dispose();
	    rowOrCellDialog = new RowOrCellDialog("Cut row or cell?",
			new RowOrCellDialogListener() {
			    public void row() {
				cutRow();
			    }
			    public void cell() {
				cutCell();
			    }
			});
	    rowOrCellDialog.setParent(this);
	    rowOrCellDialog.showCentered();
	} else
	    cutRow();
    }

    private void cutRow() {
	int[] selRows = table.getSelectedRows();
	if (selRows.length == 0)
	    Toolkit.getDefaultToolkit().beep();
	else {
	    copyRow();
	    model.deleteRow(selRows, true);
	    table.clearSelection();
	}
    }

    private void cutCell() {
	int row = table.getSelectionModel().getAnchorSelectionIndex();
	int column = table.getColumnModel().getSelectionModel()
			  .getAnchorSelectionIndex();
	if (row == -1 || column == -1)
	    Toolkit.getDefaultToolkit().beep();
	else {
	    table.stopEditing();
	    Main.getClipboard().put(model.getValueAt(row, column));
	    model.setValueAt(null, row, column, "Cut Cell");
	}
    }

    private void copy() {
	if (table.isEditing()) {
	    ((JTextField) table.getEditorComponent()).copy();
	    return;
	}
	if (table.getSelectedRows().length == 1) {
	    if (rowOrCellDialog != null)
		rowOrCellDialog.dispose();
	    rowOrCellDialog = new RowOrCellDialog("Copy row or cell?",
			new RowOrCellDialogListener() {
			    public void row() {
				copyRow();
			    }
			    public void cell() {
				copyCell();
			    }
			});
	    rowOrCellDialog.setParent(this);
	    rowOrCellDialog.showCentered();
	} else
	    copyRow();
    }

    private void copyRow() {
	int[] selRows = table.getSelectedRows();
	int nrows = selRows.length;
	if (nrows == 0)
	    Toolkit.getDefaultToolkit().beep();
	else {
	    table.stopEditing();
	    int ncols = model.getColumnCount();
	    Object[][] array = new Object[nrows + 3][ncols];
	    for (int col = 0; col < ncols; col++) {
		array[0][col] = model.getColumnName(col);
		array[1][col] = model.getColumnClass(col);
		array[2][col] = model.getTypeSpec(col);
	    }
	    for (int row = 0; row < nrows; row++)
		for (int col = 0; col < ncols; col++)
		    array[row + 3][col] = model.getValueAt(selRows[row], col);
	    Main.getClipboard().put(array);
	}
    }

    private void copyCell() {
	int row = table.getSelectionModel().getAnchorSelectionIndex();
	int column = table.getColumnModel().getSelectionModel()
			  .getAnchorSelectionIndex();
	column = table.convertColumnIndexToModel(column);
	if (row == -1 || column == -1)
	    Toolkit.getDefaultToolkit().beep();
	else {
	    table.stopEditing();
	    Main.getClipboard().put(model.getValueAt(row, column));
	}
    }

    private void paste() {
	if (table.isEditing()) {
	    ((JTextField) table.getEditorComponent()).paste();
	    return;
	}
	Object clipdata = Main.getClipboard().get();
	if (clipdata == null)
	    return;
	if (clipdata instanceof Object[][]) {
	    Object[][] array = (Object[][]) clipdata;
	    if (columnMatchDialog != null)
		columnMatchDialog.dispose();
	    int clen = array[0].length;
	    String[] clipHeaders = new String[clen];
	    System.arraycopy(array[0], 0, clipHeaders, 0, clen);
	    int tlen = model.getColumnCount();
	    String[] tableHeaders = new String[tlen];
	    for (int i = 0; i < tlen; i++)
		tableHeaders[i] = model.getColumnName(i);
	    columnMatchDialog = new ColumnMatchDialog(clipHeaders, tableHeaders,
		new ColumnMatchDialog.Listener() {
			public void done(String[] mapping, boolean setNull) {
			    pasteRow(mapping, setNull);
			}
		    });
	    columnMatchDialog.setParent(this);
	    columnMatchDialog.showCentered();
	} else {
	    int row = table.getSelectionModel().getAnchorSelectionIndex();
	    int column = table.getColumnModel().getSelectionModel()
			    .getAnchorSelectionIndex();
	    if (row == -1 || column == -1)
		Toolkit.getDefaultToolkit().beep();
	    else {
		table.cancelEditing();
		model.setValueAt(clipdata, row, column, "Paste");
	    }
	}
    }

    private void pasteRow(String[] mapping, boolean setNull) {
	Object clipdata = Main.getClipboard().get();
	if (!(clipdata instanceof Object[][])) {
	    Toolkit.getDefaultToolkit().beep();
	    return;
	}
	model.pasteRow((Object[][]) clipdata, mapping, setNull);
    }

    private void insertRow() {
	table.stopEditing();
	int row = table.getSelectedRow();
	model.insertRow(row);
	if (row == -1)
	    row = model.getRowCount() - 1;
	table.clearSelection();
	table.addRowSelectionInterval(row, row);
    }

    private void deleteRow() {
	table.stopEditing();
	model.deleteRow(table.getSelectedRows(), false);
	table.clearSelection();
    }

    private void setCellToNull() {
	int row = table.getSelectionModel().getAnchorSelectionIndex();
	int column = table.getColumnModel().getSelectionModel()
			  .getAnchorSelectionIndex();
	column = table.convertColumnIndexToModel(column);
	if (row == -1 || column == -1)
	    Toolkit.getDefaultToolkit().beep();
	else {
	    table.cancelEditing();
	    model.setValueAt(null, row, column, "Set Cell to Null");
	}
    }

    private void editCell() {
	int row = table.getSelectionModel().getAnchorSelectionIndex();
	int column = table.getColumnModel().getSelectionModel()
			  .getAnchorSelectionIndex();
	column = table.convertColumnIndexToModel(column);
	if (row == -1 || column == -1)
	    Toolkit.getDefaultToolkit().beep();
	else {
	    table.stopEditing();
	    String name = getTitle() + " [" + row + ", " + column + "]";
	    Object o = model.getValueAt(row, column);
	    TypeSpec spec = model.getTypeSpec(column);
	    Class k = spec.jdbcJavaClass;
	    
	    if (o instanceof BlobWrapper)
		o = ((BlobWrapper) o).load();
	    if (k == new byte[1].getClass()
		    || spec.jdbcJavaType.equals("transbase.tbx.types.TBBits")
		    || o instanceof byte[]
		    || o == null && java.sql.Blob.class.isAssignableFrom(k)) {
		byte[] data = (byte[]) o;
		BinaryEditorFrame bef;
		ResultSetTableModel p_model = editable ? model : null;
		bef = new BinaryEditorFrame(name, data, p_model, row, column);
		bef.setParent(this);
		bef.showStaggered();
		return;
	    }

	    if (o instanceof BfileWrapper) {
		byte[] data = ((BfileWrapper) o).load();
		if (data != null) {
		    BinaryEditorFrame bef = new BinaryEditorFrame("BFILE data", data);
		    bef.setParent(this);
		    bef.showStaggered();
		}
		return;
	    }

	    String text;
	    if (o instanceof ClobWrapper)
		text = ((ClobWrapper) o).load();
	    else
		text = spec.objectToString(o);
	    TextEditorFrame tef;
	    ResultSetTableModel p_model = editable ? model : null;
	    tef = new TextEditorFrame(name, text, p_model, row, column);
	    tef.setParent(this);
	    tef.showStaggered();
	}
    }

    private void modelChanged() {
	commitMI.setEnabled(model.isDirty());
	rollbackMI.setEnabled(model.isDirty());
	selectionChanged();
    }

    private void selectionChanged() {
	int[] rows = table.getSelectedRows();
	int row = table.getSelectionModel().getAnchorSelectionIndex();
	int column = table.getColumnModel().getSelectionModel()
			  .getAnchorSelectionIndex();
	boolean haveSelection = row != -1 && column != -1;
	if (editable) {
	    if (rows.length < 2) {
		deleteRowMI.setText("Delete Row");
		deleteRowMI.setEnabled(rows.length == 1);
	    } else {
		deleteRowMI.setText("Delete Rows");
		deleteRowMI.setEnabled(true);
	    }
	    setCellNullMI.setEnabled(haveSelection);
	    cutMI.setEnabled(rows.length > 0);
	}
	copyMI.setEnabled(rows.length > 0);
	editCellMI.setEnabled(haveSelection);
	if (!editable)
	    editMenu.setEnabled(haveSelection || rows.length > 0);
    }

    public void updateTitle() {
	setTitle(getParentTitle() + "/" + (dbTable == null ? "query-output"
	    : (dbTable.getName() +
		(dbTable.isUpdatableQueryResult() ? " (query)" : ""))));
    }

    public void undoRedoTitleChanged() {
	String undo = model.getUndoTitle();
	if (undo == null) {
	    undoMI.setText("Undo");
	    undoMI.setEnabled(false);
	} else {
	    undoMI.setText(undo);
	    undoMI.setEnabled(true);
	}
	String redo = model.getRedoTitle();
	if (redo == null) {
	    redoMI.setText("Redo");
	    redoMI.setEnabled(false);
	} else {
	    redoMI.setText(redo);
	    redoMI.setEnabled(true);
	}
    }

    public boolean isDirty() {
	return (model.isDirty()
		    // Check for orphans -- they are never considered "dirty"
		    && dbTable.getQualifiedName().indexOf("...") == -1)
		|| super.isDirty();
    }

    public void nuke() {
	if (!isDirty()) {
	    dispose();
	    return;
	}

	if (dbTable.isUpdatableQueryResult()) {
	    Toolkit.getDefaultToolkit().beep();
	    int result = JOptionPane.showInternalConfirmDialog(
		    Main.getDesktop(),
		    "You have made changes to this query result which you\n"
		    + "have not yet committed. Would you like to commit them\n"
		    + "now? (If you answer \"No\", your changes will be lost.)",
		    "Confirm", JOptionPane.YES_NO_CANCEL_OPTION,
		    JOptionPane.QUESTION_MESSAGE);
	    if (result == JOptionPane.CANCEL_OPTION)
		return;
	    if (result == JOptionPane.NO_OPTION) {
		dispose();
		return;
	    }
	    ArrayList list = new ArrayList();
	    list.add(dbTable);
	    try {
		dbTable.getDatabase().commitTables(list);
		dispose();
	    } catch (NavigatorException e) {
		MessageBox.show(e);
	    }
	    return;
	}

	Toolkit.getDefaultToolkit().beep();
	int result = JOptionPane.showInternalConfirmDialog(
		    Main.getDesktop(),
		    "You have made changes to this table which you have\n"
		    + "not yet committed. Would you like to commit them now?\n"
		    + "(If you answer \"No\", the window will close, but\n"
		    + "your changes will be kept in memory, and you will be\n"
		    + "asked again when you close the database connection or\n"
		    + "when you quit JDBC Navigator.)",
		    "Confirm", JOptionPane.YES_NO_CANCEL_OPTION,
		    JOptionPane.QUESTION_MESSAGE);
	if (result == JOptionPane.CANCEL_OPTION)
	    return;
	if (result == JOptionPane.NO_OPTION) {
	    dispose();
	    return;
	}
	
	Collection dirty = dbTable.getDatabase().getDirtyTables();
	if (dirty == null || dirty.size() == 0) {
	    // Hmm, apparently the table is no longer dirty. This can happen
	    // if the user causes it to be committed while the JOptionPane is
	    // still up (or if there's a bug somewhere!).
	    dispose();
	    return;
	}

	if (dirty.size() == 1 && dirty.iterator().next() == dbTable) {
	    // dbTable is the only dirty table in this database connection.
	    // Never mind the MultiCommitDialog step; just commit it with
	    // no further ado.
	    ArrayList list = new ArrayList();
	    list.add(dbTable);
	    try {
		dbTable.getDatabase().commitTables(list);
		dispose();
	    } catch (NavigatorException e) {
		MessageBox.show(e);
	    }
	    return;
	}

	// There are other dirty tables besides dbTable.
	// Give the user the option of committing any or all of them.
	MultiCommitDialog mcd = new MultiCommitDialog(dbTable, dirty, true);
	mcd.setParent(this);
	mcd.showCentered();
    }
    
    private class RowOrCellDialog extends MyFrame {
	private RowOrCellDialogListener listener;
	public RowOrCellDialog(String message, RowOrCellDialogListener listener_p) {
	    super("Row or Cell?");
	    listener = listener_p;

	    Container c = getContentPane();
	    c.setLayout(new GridLayout(1, 1));
	    JPanel p = new JPanel();
	    p.setLayout(new MyGridBagLayout());
	    c.add(p);

	    JLabel label = new JLabel(message);
	    MyGridBagConstraints gbc = new MyGridBagConstraints();
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.gridwidth = 3;
	    gbc.weightx = 1;
	    gbc.anchor = MyGridBagConstraints.WEST;
	    p.add(label, gbc);

	    JButton button = new JButton("Row");
	    button.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				dispose();
				listener.row();
			    }
			});
	    gbc.gridy++;
	    gbc.gridwidth = 1;
	    gbc.weightx = 0;
	    p.add(button, gbc);

	    button = new JButton("Cell");
	    button.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				dispose();
				listener.cell();
			    }
			});
	    gbc.gridx++;
	    p.add(button, gbc);

	    button = new JButton("Cancel");
	    button.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				dispose();
			    }
			});
	    gbc.gridx++;
	    p.add(button, gbc);

	    pack();
	}
    }

    private interface RowOrCellDialogListener {
	void row();
	void cell();
    }

    public void childDisposed(MyFrame child) {
	if (child == sequenceDialog)
	    sequenceDialog = null;
	if (child == rowOrCellDialog)
	    rowOrCellDialog = null;
	if (child == columnMatchDialog)
	    columnMatchDialog = null;
    }
}
