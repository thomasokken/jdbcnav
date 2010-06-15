///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2010	Thomas Okken
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
import javax.swing.event.*;
import javax.swing.table.*;
import jdbcnav.util.*;


public class PreferencesFrame extends MyFrame {
	private Preferences prefs = Preferences.getPreferences();
	private UIManager.LookAndFeelInfo[] laf =
								UIManager.getInstalledLookAndFeels();
	private JComboBox lafNameCB;
	private JLabel pkHighColL;
	private JLabel fkHighColL;
	private JTextArea systemPropsTA;
	private JCheckBox showSplashCB;
	private JTextField logFileNameTF;
	private JComboBox logLevelCB;

	JTable classPathTable;
	ClassPathTableModel classPathModel;
	JButton cpUp;
	JButton cpDown;
	JButton cpInsert;
	JButton cpRemove;

	Color pkHighC;
	Color fkHighC;

	private static ColorChooser pkColorChooser = null;
	private static ColorChooser fkColorChooser = null;
	private static ArrayList<Component> highlightColorChangeListeners = new ArrayList<Component>();

	public PreferencesFrame() {
		super("Preferences", true, true, true, true);
		Container c = getContentPane();
		c.setLayout(new MyGridBagLayout());
		MyGridBagConstraints gbc = new MyGridBagConstraints();
		int totalwidth = 1;


		///////////////////////////////
		///// Look And Feel panel /////
		///////////////////////////////

		JPanel p = new JPanel(new MyGridBagLayout());
		p.setBorder(BorderFactory.createTitledBorder("Look and Feel"));

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = MyGridBagConstraints.WEST;
		gbc.gridwidth = 3;
		gbc.weightx = 1;
		JPanel p2 = new JPanel();
		p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS));
		p2.add(new JLabel("Swing Look-and-Feel Name: "));
		String[] lafName = new String[laf.length];
		for (int i = 0; i < laf.length; i++)
			lafName[i] = laf[i].getName();
		Arrays.sort(lafName);
		lafNameCB = new JComboBox(lafName);
		lafNameCB.setSelectedItem(prefs.getLookAndFeelName());
		p2.add(lafNameCB);
		p.add(p2, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		p.add(new JLabel("PK Highlight Color:"), gbc);

		gbc.gridx = 1;
		pkHighC = prefs.getPkHighlightColor();
		pkHighColL = new JLabel(new SolidColorIcon(pkHighC, 40, 20));
		gbc.insets = new Insets(0, 10, 0, 10);
		p.add(pkHighColL, gbc);

		gbc.gridx = 2;
		gbc.insets = new Insets(0, 0, 0, 0);
		JButton b = new JButton("Change");
		b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					changePkHighCol();
				}
			});
		p.add(b, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		p.add(new JLabel("FK Highlight Color:"), gbc);

		gbc.gridx = 1;
		fkHighC = prefs.getFkHighlightColor();
		fkHighColL = new JLabel(new SolidColorIcon(fkHighC, 40, 20));
		gbc.insets = new Insets(0, 10, 0, 10);
		p.add(fkHighColL, gbc);

		gbc.gridx = 2;
		gbc.insets = new Insets(0, 0, 0, 0);
		b = new JButton("Change");
		b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					changeFkHighCol();
				}
			});
		p.add(b, gbc);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = MyGridBagConstraints.HORIZONTAL;
		c.add(p, gbc);


		///////////////////////////////////////
		///// Some miscellaneous settings /////
		///////////////////////////////////////

		p = new JPanel(new MyGridBagLayout());
		p.setBorder(BorderFactory.createTitledBorder("Miscellaneous"));

		MyGridBagConstraints gbc2 = new MyGridBagConstraints();

		gbc2.gridx = 0;
		gbc2.gridy = 0;
		gbc2.anchor = MyGridBagConstraints.WEST;
		gbc2.fill = MyGridBagConstraints.NONE;
		gbc2.gridwidth = 2;
		showSplashCB = new JCheckBox("Show splash screen on startup");
		showSplashCB.setSelected(prefs.getShowSplash());
		p.add(showSplashCB, gbc2);


		gbc2.gridy = 1;
		gbc2.gridwidth = 1;
		p.add(new JLabel("Log File: "), gbc2);

		gbc2.gridy = 2;
		p.add(new JLabel("Log Level: "), gbc2);

		gbc2.gridx = 1;
		gbc2.gridy = 1;
		gbc2.weightx = 1;
		gbc2.fill = MyGridBagConstraints.HORIZONTAL;
		logFileNameTF = new MyTextField();
		String s = prefs.getLogFileName();
		if (s != null)
			logFileNameTF.setText(s);
		p.add(logFileNameTF, gbc2);
		
		gbc2.gridy = 2;
		gbc2.fill = MyGridBagConstraints.NONE;
		logLevelCB = new JComboBox(new Object[] { "None", "Low", "Medium", "High" });
		logLevelCB.setSelectedIndex(prefs.getLogLevel());
		p.add(logLevelCB, gbc2);

		gbc.gridy++;
		c.add(p, gbc);
		

		///////////////////////////////////
		///// System Properties panel /////
		///////////////////////////////////

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


		////////////////////////////
		///// Class Path panel /////
		////////////////////////////

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
			scroll = new JScrollPane(classPathTable);
			scroll.setPreferredSize(new Dimension(300, 200));
			p.add(scroll);
			p2 = new JPanel();
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
			gbc.fill = MyGridBagConstraints.HORIZONTAL;
			gbc.anchor = MyGridBagConstraints.NORTH;
			gbc.gridx = 1;
			int y = gbc.gridy;
			gbc.gridy = 0;
			gbc.gridheight = y + 1;
			c.add(p, gbc);

			gbc.gridx = 0;
			gbc.gridheight = 1;
			gbc.gridy = y;
			totalwidth = 2;
		}


		///////////////////////
		///// OK & Cancel /////
		///////////////////////

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
						cancel();
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

	public static void addHighlightColorChangeListener(Component hccl) {
		highlightColorChangeListeners.add(hccl);
	}

	public static void removeHighlightColorChangeListener(Component hccl) {
		highlightColorChangeListeners.remove(hccl);
	}

	private void changePkHighCol() {
		if (pkColorChooser == null) {
			pkColorChooser = new ColorChooser("PK Highlight Color", pkHighC);
			pkColorChooser.setListener(new ColorChooser.Listener() {
					public void apply(Color c) {
						pkHighColChanged(c);
					}
					public void close() {
						pkColorChooser = null;
					}
				});
			pkColorChooser.showCentered();
		} else {
			pkColorChooser.deiconifyAndRaise();
		}
	}

	private void pkHighColChanged(Color c) {
		pkHighC = c;
		pkHighColL.setIcon(new SolidColorIcon(c, 40, 20));
		MyTable.setTypeColor(1, c);
		for (Component hccl : highlightColorChangeListeners)
			hccl.repaint();
	}

	private void changeFkHighCol() {
		if (fkColorChooser == null) {
			fkColorChooser = new ColorChooser("FK Highlight Color", fkHighC);
			fkColorChooser.setListener(new ColorChooser.Listener() {
					public void apply(Color c) {
						fkHighColChanged(c);
					}
					public void close() {
						fkColorChooser = null;
					}
				});
			fkColorChooser.showCentered();
		} else {
			fkColorChooser.deiconifyAndRaise();
		}
	}

	private void fkHighColChanged(Color c) {
		fkHighC = c;
		fkHighColL.setIcon(new SolidColorIcon(c, 40, 20));
		MyTable.setTypeColor(2, c);
		for (Component hccl : highlightColorChangeListeners)
			hccl.repaint();
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

		prefs.setPkHighlightColor(pkHighC);
		prefs.setFkHighlightColor(fkHighC);
		prefs.setSystemPropertiesAsText(systemPropsTA.getText());
		if (prefs.usingSneakyClassLoader()) {
			if (classPathTable.isEditing()) {
				int row = classPathTable.getEditingRow();
				int column = classPathTable.getEditingColumn();
				TableCellEditor editor = classPathTable.getCellEditor(row, column);
				if (!editor.stopCellEditing())
					editor.cancelCellEditing();
			}
			prefs.setClassPath(classPathModel.getItems());
		}
		prefs.setShowSplash(showSplashCB.isSelected());
		prefs.setLogFileName(logFileNameTF.getText());
		prefs.setLogLevel(logLevelCB.getSelectedIndex());
		prefs.write();
		dispose();
	}

	private void cancel() {
		Color c = prefs.getPkHighlightColor();
		if (!c.equals(pkHighC))
			pkHighColChanged(c);
		c = prefs.getFkHighlightColor();
		if (!c.equals(fkHighC))
			fkHighColChanged(c);
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
		private ArrayList<String> items;
		@SuppressWarnings(value={"unchecked"})
		public ClassPathTableModel(ArrayList<String> items) {
			this.items = (ArrayList<String>) items.clone();
		}
		public ArrayList<String> getItems() {
			return items;
		}
		public int getRowCount() {
			return items.size();
		}
		public int getColumnCount() {
			return 1;
		}
		public Class<?> getColumnClass(int column) {
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
			String oldVal = items.get(row);
			if (!newVal.equals(oldVal)) {
				items.set(row, newVal);
				fireTableCellUpdated(row, column);
			}
		}
		public int[] up(int[] sel) {
			int remove = sel[0] - 1;
			int insert = sel[sel.length - 1];
			String s = items.remove(remove);
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
			String s = items.remove(remove);
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

	private static class SolidColorIcon implements Icon {
		private Color color;
		private int width, height;
		public SolidColorIcon(Color color, int width, int height) {
			this.color = color;
			this.width = width;
			this.height = height;
		}
		public int getIconWidth() {
			return width;
		}
		public int getIconHeight() {
			return height;
		}
		public void paintIcon(Component c, Graphics g, int x, int y) {
			g.setColor(color);
			g.fillRect(x, y, width, height);
		}
	}
}
