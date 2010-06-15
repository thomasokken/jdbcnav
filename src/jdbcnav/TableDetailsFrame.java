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

import jdbcnav.model.*;
import jdbcnav.util.*;


public class TableDetailsFrame extends MyFrame {
	private Table dbTable;
	private PrimaryKey pk;
	private ForeignKey[] fks;
	private ForeignKey[] rks;
	private Index[] indexes;

	private MyTable table;
	private JEditorPane editor;

	public TableDetailsFrame(Table dbTable, BrowserFrame browser) {
		super(browser.getTitle() + "/" + dbTable.getName() 
						+ (dbTable.isUpdatableQueryResult() ? " (query)" : "")
						+ " Details", true, true, true, true);
		this.dbTable = dbTable;
		pk = dbTable.getPrimaryKey();
		fks = dbTable.getForeignKeys();
		rks = dbTable.getReferencingKeys();
		indexes = dbTable.getIndexes();

		JPanel p = new JPanel(new MyGridBagLayout());
		MyGridBagConstraints gbc = new MyGridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = MyGridBagConstraints.BOTH;
		gbc.weighty = 1;
		getContentPane().add(p);

		JMenuBar mb = new JMenuBar();
		JMenu m = new JMenu("Details");
		JMenuItem mi = new JMenuItem("Edit Table");
		mi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									editTable();
								}
							});
		m.add(mi);
		mi = new JMenuItem("Reload");
		mi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									reload();
								}
							});
		m.add(mi);
		mi = new JMenuItem("Close");
		mi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									dispose();
								}
							});
		mi.setAccelerator(KeyStroke.getKeyStroke('W', Event.CTRL_MASK));
		m.add(mi);
		mb.add(m);
		setJMenuBar(mb);

		TableModel tm = buildTableModel();
		table = new MyTable(tm);
		table.setNiceSize();
		gbc.weightx = 0;
		p.add(new JScrollPane(table,
							  JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
							  JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS),
			  gbc);

		String html = buildHtmlDetails();
		editor = new JEditorPane();
		editor.setEditable(false);
		editor.setContentType("text/html");
		editor.addHyperlinkListener(
					new HyperlinkListener() {
						public void hyperlinkUpdate(HyperlinkEvent e) {
							HyperlinkEvent.EventType t = e.getEventType();
							String link = e.getDescription();
							if (t == HyperlinkEvent.EventType.ACTIVATED)
								linkActivated(link);
						}
					});
		editor.setText(html);
		gbc.gridx = 1;
		gbc.weightx = 1;
		p.add(new JScrollPane(editor,
							  JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
							  JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
			  gbc);

		pack();

		// aaargh...
		Dimension d = getSize();
		d.height += table.getTableHeader().getPreferredSize().height;
		Dimension ds = Main.getDesktop().getSize();
		if (d.height > ds.height)
			d.height = ds.height;
		if (d.width > ds.width)
			d.width = ds.width;
		setSize(d);
	}

	public void dispose() {
		if (!dbTable.isUpdatableQueryResult())
			dbTable.getDatabase().tableDetailsFrameClosed(
												dbTable.getQualifiedName());
		super.dispose();
	}

	public void updateTitle() {
		setTitle(getParentTitle() + "/" + dbTable.getName() 
						+ (dbTable.isUpdatableQueryResult() ? " (query)" : "")
						+ " Details");
	}

	public void reload() {
		try {
			dbTable.updateDetails();
		} catch (NavigatorException e) {
			MessageBox.show("Could not reload table details.", e);
			return;
		}
		pk = dbTable.getPrimaryKey();
		fks = dbTable.getForeignKeys();
		rks = dbTable.getReferencingKeys();
		indexes = dbTable.getIndexes();
		table.setModel(buildTableModel());
		table.setNiceSize();
		editor.setText(buildHtmlDetails());
		updateTitle();
	}

	private TableModel buildTableModel() {
		String[] names = new String[] { "#", "Name", "Type", "Size",
								"Scale", "Nulls", "SQL Type", "Java Type" };
		Class<?>[] classes = new Class[] { Integer.class, String.class,
								String.class, Integer.class, Integer.class,
								String.class, String.class, String.class };
		int columns = dbTable.getColumnCount();
		Object[][] data = new Object[columns][8];
		for (int i = 0; i < columns; i++) {
			TypeSpec spec = dbTable.getTypeSpecs()[i];
			data[i][0] = i + 1;
			data[i][1] = dbTable.getColumnNames()[i];
			data[i][2] = spec.jdbcDbType;
			data[i][3] = spec.jdbcSize;
			data[i][4] = spec.jdbcScale;
			data[i][5] = dbTable.getIsNullable()[i];
			data[i][6] = MiscUtils.sqlTypeIntToString(spec.jdbcSqlType);
			data[i][7] = spec.jdbcJavaType;
		}
		return new ArrayTableModel(names, classes, null, data);
	}

	private String buildHtmlDetails() {
		StringBuffer buf = new StringBuffer();
		buf.append("<html><body><font face='lucida' size='2'>\n");
		buf.append("<b><a name='top'>Table</a>: <a href='table'>");
		buf.append(q(dbTable.getQualifiedName()) + "</a></b>\n");
		buf.append("<p>\n");

		String remarks = dbTable.getRemarks();
		if (remarks != null && !remarks.equals("")) {
			buf.append("Remarks:\n");
			buf.append("<p>\n");
			// The following is my Poor Man's <pre> tag.
			// JEditorPane does support <pre>, but I think it looks awful:
			// the fixed-width font is too big, and after the </pre> tag
			// it does not switch back to the font that was in effect before
			// the <pre> tag. Ugh. Never mind!
			StringTokenizer tok = new StringTokenizer(remarks, " \t\n\r", true);
			int pos = 0;
			while (tok.hasMoreTokens()) {
				String t = tok.nextToken();
				if (t.equals(" ")) {
					pos++;
					if (pos > 64) {
						buf.append("\n<br>\n");
						pos = 0;
					} else
						buf.append("&nbsp;");
				} else if (t.equals("\t")) {
					int sp = 8 - pos % 8;
					pos += sp;
					if (pos > 64) {
						buf.append("\n<br>\n");
						pos = 0;
					} else {
						while (sp-- > 0)
							buf.append("&nbsp;");
					}
				} else if (t.equals("\n") || t.equals("\r")) {
					buf.append("\n<br>\n");
					pos = 0;
				} else {
					if (pos + t.length() > 64) {
						buf.append("\n<br>\n");
						pos = 0;
					}
					buf.append(q(t));
					pos += t.length();
				}
			}
			buf.append("\n<p>\n");
		}

		if (pk != null) {
			buf.append("Primary Key:\n");
			buf.append("<ul>\n");
			if (pk.getName() != null)
				buf.append("<li>Name: <a href='pk'>"
							+ q(pk.getName()) + "</a>\n");
			buf.append("<li>Columns: ");
			for (int i = 0; i < pk.getColumnCount(); i++) {
				if (i != 0)
					buf.append(", ");
				buf.append("<a href='pk." + i + "'>"
							+ q(pk.getColumnName(i)) + "</a>");
			}
			buf.append("\n");
			for (int i = 0; i < rks.length; i++) {
				ForeignKey rk = rks[i];
				buf.append("<li type='square'><b>Referenced by: "
							+ "<a href='rtable." + i + "'>");
				buf.append(q(rk.getThatQualifiedName()) + "</a></b>\n");
				if (rk.getThatKeyName() != null)
					buf.append("<li type='circle'>Name: <a href='rk." + i
								+ "'>" + q(rk.getThatKeyName()) + "</a>\n");
				buf.append("<li type='circle'>Columns: ");
				for (int j = 0; j < rk.getColumnCount(); j++) {
					if (j != 0)
						buf.append(", ");
					buf.append("<a href='rk." + i + "." + j
								+ "'>" + q(rk.getThatColumnName(j))
								+ "</a>");
				}
				buf.append("\n");
				buf.append("<li type='circle'>On update: "
							+ q(rk.getUpdateRule()) + "\n");
				buf.append("<li type='circle'>On delete: "
							+ q(rk.getDeleteRule()) + "\n");
			}
			buf.append("</ul>\n");
		}

		for (int i = 0; i < fks.length; i++) {
			ForeignKey fk = fks[i];
			buf.append("Foreign Key:\n");
			buf.append("<ul>\n");
			if (fk.getThisKeyName() != null)
				buf.append("<li>Name: <a href='fk." + i
							+ "'>" + q(fk.getThisKeyName()) + "</a>\n");
			buf.append("<li>Columns: ");
			for (int j = 0; j < fk.getColumnCount(); j++) {
				if (j != 0)
					buf.append(", ");
				buf.append("<a href='fk." + i + "." + j
							+ "'>" + q(fk.getThisColumnName(j)) + "</a>");
			}
			buf.append("\n");
			buf.append("<li type='square'><b>References: "
						+ "<a href='table." + i + "'>");
			buf.append(q(fk.getThatQualifiedName()) + "</a></b>\n");
			if (fk.getThatKeyName() != null)
				buf.append("<li type='circle'>Name: <a href='ref." + i
							+ "'>" + q(fk.getThatKeyName()) + "</a>\n");
			buf.append("<li type='circle'>Columns: ");
			for (int j = 0; j < fk.getColumnCount(); j++) {
				if (j != 0)
					buf.append(", ");
				buf.append("<a href='ref." + i + "." + j
							+ "'>" + q(fk.getThatColumnName(j)) + "</a>");
			}
			buf.append("\n");
			buf.append("<li type='circle'>On update: "
						+ q(fk.getUpdateRule()) + "\n");
			buf.append("<li type='circle'>On delete: "
						+ q(fk.getDeleteRule()) + "\n");
			buf.append("</ul>\n");
		}

		for (int i = 0; i < indexes.length; i++) {
			Index index = indexes[i];
			buf.append("Index:\n");
			buf.append("<ul>\n");
			buf.append("<li>Name: <a href='index." + i
						+ "'>" + q(index.getName()) + "</a>\n");
			buf.append("<li>Columns: ");
			for (int j = 0; j < index.getColumnCount(); j++) {
				if (j != 0)
					buf.append(", ");
				buf.append("<a href='index." + i + "." + j
							+ "'>" + q(index.getColumnName(j)) + "</a>");
			}
			buf.append("\n");
			buf.append("<li type='circle'>" + (index.isUnique() ? "Unique"
						: "Non-Unique") + "\n");
			buf.append("</ul>\n");
		}

		buf.append("</font></body></html>");
		return buf.toString();
	}

	private void linkActivated(String link) {
		String what;
		int arg1 = -1;
		int arg2 = -1;
		StringTokenizer tok = new StringTokenizer(link, ".");
		what = tok.nextToken();
		if (tok.hasMoreTokens()) {
			arg1 = Integer.parseInt(tok.nextToken());
			if (tok.hasMoreTokens())
				arg2 = Integer.parseInt(tok.nextToken());
		}

		if (what.equals("pk")) {
			if (pk != null)
				if (arg1 == -1) {
					int n = pk.getColumnCount();
					String[] colnames = new String[n];
					for (int i = 0; i < n; i++)
						colnames[i] = pk.getColumnName(i);
					selectRows(colnames);
				} else
					selectRows(pk.getColumnName(arg1));
		} else if (what.equals("table")) {
			if (arg1 == -1) {
				String qname = dbTable.getQualifiedName();
				dbTable.getDatabase().showTableFrame(qname);
			} else if (arg1 < fks.length) {
				String qname = fks[arg1].getThatQualifiedName();
				dbTable.getDatabase().showTableFrame(qname);
			}
		} else if (what.equals("rtable")) {
			if (arg1 < rks.length) {
				String qname = rks[arg1].getThatQualifiedName();
				dbTable.getDatabase().showTableFrame(qname);
			}
		} else if (what.equals("fk")) {
			if (arg1 < fks.length)
				if (arg2 == -1) {
					int n = fks[arg1].getColumnCount();
					String[] cols = new String[n];
					for (int i = 0; i < n; i++)
						cols[i] = fks[arg1].getThisColumnName(i);
					selectRows(cols);
				} else if (arg2 < fks[arg1].getColumnCount())
					selectRows(fks[arg1].getThisColumnName(arg2));
		} else if (what.equals("ref")) {
			String qname = fks[arg1].getThatQualifiedName();
			TableDetailsFrame tdf = dbTable.getDatabase()
										   .showTableDetailsFrame(qname);
			if (tdf != null) {
				if (arg1 < fks.length)
					if (arg2 == -1) {
						int n = fks[arg1].getColumnCount();
						String[] cols = new String[n];
						for (int i = 0; i < n; i++)
							cols[i] = fks[arg1].getThatColumnName(i);
						tdf.selectRows(cols);
					} else if (arg2 < fks[arg1].getColumnCount())
						tdf.selectRows(fks[arg1].getThatColumnName(arg2));
			}
		} else if (what.equals("rk")) {
			String qname = rks[arg1].getThatQualifiedName();
			TableDetailsFrame tdf = dbTable.getDatabase()
										   .showTableDetailsFrame(qname);
			if (tdf != null) {
				if (arg1 < rks.length)
					if (arg2 == -1) {
						int n = rks[arg1].getColumnCount();
						String[] cols = new String[n];
						for (int i = 0; i < n; i++)
							cols[i] = rks[arg1].getThatColumnName(i);
						tdf.selectRows(cols);
					} else if (arg2 < rks[arg1].getColumnCount())
						tdf.selectRows(rks[arg1].getThatColumnName(arg2));
			}
		} else if (what.equals("index")) {
			if (arg1 < indexes.length)
				if (arg2 == -1) {
					int n = indexes[arg1].getColumnCount();
					String[] cols = new String[n];
					for (int i = 0; i < n; i++)
						cols[i] = indexes[arg1].getColumnName(i);
					selectRows(cols);
				} else if (arg2 < indexes[arg1].getColumnCount())
					selectRows(indexes[arg1].getColumnName(arg2));
		}
	}
	
	private void editTable() {
		dbTable.getDatabase().showTableFrame(dbTable.getQualifiedName());
	}

	private void selectRows(String name) {
		selectRows(new String[] { name });
	}

	private void selectRows(String[] names) {
		table.clearSelection();
		TableModel tm = table.getModel();
		int rows = tm.getRowCount();
		for (int i = 0; i < names.length; i++) {
			String name = names[i];
			for (int j = 0; j < rows; j++)
				if (tm.getValueAt(j, 1).equals(name))
					table.addRowSelectionInterval(j, j);
		}
	}

	private static String q(String s) {
		return FileUtils.encodeEntities(s);
	}
}
