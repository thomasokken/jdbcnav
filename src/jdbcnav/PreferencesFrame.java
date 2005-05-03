package jdbcnav;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import jdbcnav.util.*;


public class PreferencesFrame extends MyFrame {
    private Preferences prefs = Preferences.getPreferences();
    private UIManager.LookAndFeelInfo[] laf =
				UIManager.getInstalledLookAndFeels();
    private JComboBox lafNameCB;
    private JTextArea systemPropsTA;

    JTable classPathTable;
    ClassPathTableModel classPathModel;
    JButton cpUp;
    JButton cpDown;
    JButton cpInsert;
    JButton cpRemove;

    public PreferencesFrame() {
	super("Preferences", true, true, true, true);
	Container c = getContentPane();
	c.setLayout(new MyGridBagLayout());
	MyGridBagConstraints gbc = new MyGridBagConstraints();

	JPanel p = new JPanel();
	p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
	p.setBorder(BorderFactory.createTitledBorder("Look and Feel"));

	String[] lafName = new String[laf.length];
	for (int i = 0; i < laf.length; i++)
	    lafName[i] = laf[i].getName();
	Arrays.sort(lafName);
	p.add(new JLabel("Name: "));
	lafNameCB = new JComboBox(lafName);
	lafNameCB.setSelectedItem(prefs.getLookAndFeelName());
	p.add(lafNameCB);

	gbc.gridx = 0;
	gbc.gridy = 0;
	gbc.anchor = MyGridBagConstraints.WEST;
	c.add(p, gbc);

	int totalwidth = 1;

	if (prefs.usingSneakyClassLoader()) {
	    p = new JPanel();
	    p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
	    p.setBorder(BorderFactory.createTitledBorder("Class Path"));
	    classPathModel = new ClassPathTableModel(prefs.getClassPath());
	    classPathTable = new JTable(classPathModel);
	    classPathTable.setTableHeader(null);
	    classPathTable.setSelectionMode(
				ListSelectionModel.SINGLE_INTERVAL_SELECTION);
	    classPathTable.getSelectionModel().addListSelectionListener(
		new ListSelectionListener() {
		    public void valueChanged(ListSelectionEvent e) {
			int[] sel = classPathTable.getSelectedRows();
			if (sel.length == 0) {
			    cpUp.setEnabled(false);
			    cpDown.setEnabled(false);
			    cpRemove.setEnabled(false);
			} else {
			    cpUp.setEnabled(sel[0] != 0);
			    cpDown.setEnabled(sel[sel.length - 1] !=
					    classPathModel.getRowCount() - 1);
			    cpRemove.setEnabled(true);
			}
		    }
		});
	    JScrollPane scroll = new JScrollPane(classPathTable);
	    scroll.setPreferredSize(new Dimension(300, 100));
	    p.add(scroll);
	    JPanel p2 = new JPanel();
	    p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS));
	    JPanel p3 = new JPanel(new GridLayout(1, 4));
	    Insets insets = new Insets(2, 4, 2, 4);
	    cpUp = new JButton("Up");
	    cpUp.setEnabled(false);
	    cpUp.setMargin(insets);
	    cpUp.addActionListener(
		new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			int[] sel = classPathTable.getSelectedRows();
			sel = classPathModel.up(sel);
			selectRows(sel);
		    }
		});
	    p3.add(cpUp);
	    cpDown = new JButton("Down");
	    cpDown.setEnabled(false);
	    cpDown.setMargin(insets);
	    cpDown.addActionListener(
		new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			int[] sel = classPathTable.getSelectedRows();
			sel = classPathModel.down(sel);
			selectRows(sel);
		    }
		});
	    p3.add(cpDown);
	    cpInsert = new JButton("Insert");
	    cpInsert.setMargin(insets);
	    cpInsert.addActionListener(
		new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			int[] sel = classPathTable.getSelectedRows();
			sel = classPathModel.insert(sel);
			selectRows(sel);
		    }
		});
	    p3.add(cpInsert);
	    cpRemove = new JButton("Remove");
	    cpRemove.setEnabled(false);
	    cpRemove.setMargin(insets);
	    cpRemove.addActionListener(
		new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			int[] sel = classPathTable.getSelectedRows();
			sel = classPathModel.remove(sel);
			selectRows(sel);
		    }
		});
	    p3.add(cpRemove);
	    p2.add(p3);
	    JButton question = new JButton("?");
	    question.setMargin(insets);
	    question.addActionListener(
		new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			JOptionPane.showInternalMessageDialog(
			    Main.getDesktop(),
			    "Use the \"Class Path\" area to specify a list of\n"
			    + "directories and jar (or zip) files where JDBC\n"
			    + "Navigator should load JDBC drivers from.\n"
			    + "If you're using JDK 1.5 or later, you may also\n"
			    + "use the word \"CLASSPATH\" to indicate that\n"
			    + "the value of the CLASSPATH environment\n"
			    + "variable should be inserted at that point.\n"
			    + "NOTE: adding items to the class path takes\n"
			    + "effect immediately after you click \"OK\",\n"
			    + "but removing or reordering items will not\n"
			    + "take effect until you restart JDBC Navigator.");
		    }
		});
	    p2.add(question);
	    p.add(p2);

	    gbc.weightx = 1;
	    gbc.weighty = 1;
	    gbc.fill = MyGridBagConstraints.BOTH;
	    gbc.gridx = 1;
	    gbc.gridheight = 2;
	    c.add(p, gbc);

	    gbc.gridx = 0;
	    gbc.gridheight = 1;
	    totalwidth = 2;
	}


	p = new JPanel();
	p.setLayout(new GridLayout(1, 1));
	p.setBorder(BorderFactory.createTitledBorder("System Properties"));

	systemPropsTA = new NonTabJTextArea(prefs.getSystemPropertiesAsText(), 4, 30);
	JScrollPane scroll = new JScrollPane(systemPropsTA);
	p.add(scroll);

	gbc.weightx = 1;
	gbc.weighty = 1;
	gbc.fill = MyGridBagConstraints.BOTH;
	gbc.gridy++;
	c.add(p, gbc);

	p = new JPanel();
	p.setLayout(new GridLayout(1, 2));
	JButton button = new JButton("OK");
	button.addActionListener(
		new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			ok();
		    }
		});
	p.add(button);
	button = new JButton("Cancel");
	button.addActionListener(
		new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			dispose();
		    }
		});
	p.add(button);

	gbc.weightx = 0;
	gbc.weighty = 0;
	gbc.fill = MyGridBagConstraints.NONE;
	gbc.gridy++;
	gbc.anchor = MyGridBagConstraints.CENTER;
	gbc.gridwidth = totalwidth;
	c.add(p, gbc);

	pack();
    }

    private void ok() {
	String lafName = (String) lafNameCB.getSelectedItem();
	boolean found = false;
	for (int i = 0; i < laf.length; i++)
	    if (laf[i].getName().equals(lafName)) {
		prefs.setLookAndFeelName(lafName);
		prefs.write();
		found = true;
		break;
	    }
	if (!found) {
	    Toolkit.getDefaultToolkit().beep();
	    JOptionPane.showInternalMessageDialog(Main.getDesktop(),
		    "\"" + lafName + "\" is not a valid Look-and-Feel name.");
	    return;
	}

	prefs.setSystemPropertiesAsText(systemPropsTA.getText());
	prefs.setClassPath(classPathModel.getItems());
	prefs.write();
	dispose();
    }

    private void selectRows(int[] rows) {
	int tablesize = classPathModel.getRowCount();
	if (tablesize != 0) {
	    if (rows.length > 0)
		classPathTable.setRowSelectionInterval(rows[0],
						       rows[rows.length - 1]);
	    else
		classPathTable.removeRowSelectionInterval(0, tablesize - 1);
	}
    }

    private class ClassPathTableModel extends AbstractTableModel {
	private ArrayList items;
	public ClassPathTableModel(ArrayList items) {
	    this.items = (ArrayList) items.clone();
	}
	public ArrayList getItems() {
	    return items;
	}
	public int getRowCount() {
	    return items.size();
	}
	public int getColumnCount() {
	    return 1;
	}
	public Class getColumnClass(int column) {
	    return String.class;
	}
	public boolean isCellEditable(int row, int column) {
	    return true;
	}
	public Object getValueAt(int row, int column) {
	    return items.get(row);
	}
	public void setValueAt(Object obj, int row, int column) {
	    String newVal = String.valueOf(obj);
	    String oldVal = (String) items.get(row);
	    if (!newVal.equals(oldVal)) {
		items.set(row, newVal);
		fireTableCellUpdated(row, column);
	    }
	}
	public int[] up(int[] sel) {
	    int remove = sel[0] - 1;
	    int insert = sel[sel.length - 1];
	    String s = (String) items.remove(remove);
	    fireTableRowsDeleted(remove, remove);
	    items.add(insert, s);
	    fireTableRowsInserted(insert, insert);
	    for (int i = 0; i < sel.length; i++)
		sel[i]--;
	    return sel;
	}
	public int[] down(int[] sel) {
	    int remove = sel[sel.length - 1] + 1;
	    int insert = sel[0];
	    String s = (String) items.remove(remove);
	    fireTableRowsDeleted(remove, remove);
	    items.add(insert, s);
	    fireTableRowsInserted(insert, insert);
	    for (int i = 0; i < sel.length; i++)
		sel[i]++;
	    return sel;
	}
	public int[] remove(int[] sel) {
	    for (int i = 0; i < sel.length; i++)
		items.remove(sel[0]);
	    fireTableRowsDeleted(sel[0], sel[sel.length - 1]);
	    return new int[0];
	}
	public int[] insert(int[] sel) {
	    int insert;
	    if (sel.length == 0) {
		items.add("");
		insert = items.size() - 1;
	    } else {
		insert = sel[0];
		items.add(insert, "");
	    }
	    fireTableRowsInserted(insert, insert);
	    return new int[] { insert };
	}
    }
}
