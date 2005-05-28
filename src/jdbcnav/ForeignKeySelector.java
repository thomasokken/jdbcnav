package jdbcnav;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import jdbcnav.model.Data;
import jdbcnav.util.MyGridBagLayout;
import jdbcnav.util.MyGridBagConstraints;

public class ForeignKeySelector extends MyFrame {
    private int tableRow;
    private ResultSetTableModel model;
    private MyTable table;
    private Callback callback;

    public interface Callback {
	void select(int row, String[] names, Object[] values);
    }

    public ForeignKeySelector(int tableRow, Data fkValues) {
	super("Select FK Value");
	this.tableRow = tableRow;
	model = new ResultSetTableModel(fkValues, null);
	table = new MyTable(model);

	ListSelectionModel lsm = table.getSelectionModel();
	lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	table.addMouseListener(
		new MouseAdapter() {
		    public void mousePressed(MouseEvent e) {
			if (e.getClickCount() == 2)
			    select();
		    }
		});

	Container c = getContentPane();
	c.setLayout(new MyGridBagLayout());
	MyGridBagConstraints gbc = new MyGridBagConstraints();

	gbc.gridx = 0;
	gbc.gridy = 0;
	gbc.fill = MyGridBagConstraints.BOTH;
	table.setNiceSize();
	JScrollPane scroll = new JScrollPane(table,
				    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				    JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	c.add(scroll,  gbc);

	JPanel p = new JPanel(new GridLayout(1, 2));
	JButton b = new JButton("Select");
	b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    select();
		}
	    });
	p.add(b);
	b = new JButton("Cancel");
	b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    dispose();
		}
	    });
	p.add(b);

	gbc.gridx = 0;
	gbc.gridy = 1;
	gbc.fill = MyGridBagConstraints.NONE;
	gbc.anchor = MyGridBagConstraints.CENTER;
	c.add(p, gbc);
	
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
    }

    public void setCallback(Callback callback) {
	this.callback = callback;
    }

    private void select() {
	int row = table.getSelectedRow();
	if (callback != null && row != -1) {
	    int ncols = model.getColumnCount();
	    String[] names = new String[ncols];
	    Object[] values = new Object[ncols];
	    for (int i = 0; i < ncols; i++) {
		names[i] = model.getColumnName(i);
		values[i] = model.getValueAt(row, i);
	    }
	    callback.select(tableRow, names, values);
	}
	dispose();
    }
}
