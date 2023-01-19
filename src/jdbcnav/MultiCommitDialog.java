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

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import jdbcnav.model.ForeignKey;
import jdbcnav.model.Table;
import jdbcnav.util.MyGridBagConstraints;
import jdbcnav.util.MyGridBagLayout;
import jdbcnav.util.NavigatorException;

public class MultiCommitDialog extends MyFrame {
    private Table[] dirty;
    private JCheckBox[] checkboxes;
    private boolean doCommit;

    public MultiCommitDialog(Table tt, Collection<Table> dirtyColl, boolean doCommit) {
        super(doCommit ? "Commit Tables" : "Roll Back Tables");
        this.dirty = dirtyColl.toArray(new Table[0]);
        this.doCommit = doCommit;

        JPanel p = new JPanel(new MyGridBagLayout());
        checkboxes = new JCheckBox[dirty.length];
        MyGridBagConstraints gbc = new MyGridBagConstraints();
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.fill = MyGridBagConstraints.HORIZONTAL;

        for (int i = 0; i < dirty.length; i++) {
            Table t = dirty[i];
            JCheckBox check = new JCheckBox(t.getQualifiedName());
            checkboxes[i] = check;
            if (tt == null || t == tt)
                check.setSelected(true);

            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 1;
            p.add(check, gbc);

            JButton button = new JButton("Edit");
            button.addActionListener(new EditHandler(t));

            gbc.gridx = 1;
            gbc.weightx = 0;
            p.add(button, gbc);
        }

        Container c = getContentPane();
        c.setLayout(new MyGridBagLayout());

        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = MyGridBagConstraints.BOTH;
        c.add(new JLabel(doCommit ? "Select tables to commit:"
                                  : "Select tables to roll back:"), gbc);

        gbc.gridy = 1;
        gbc.weighty = 1;
        JScrollPane scroll = new JScrollPane(p);
        c.add(scroll, gbc);

        JPanel bp = new JPanel(new GridLayout(1, 4));
        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        ok();
                    }
                });
        bp.add(ok);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        cancel();
                    }
                });
        bp.add(cancel);
        JButton all = new JButton("All");
        all.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        all();
                    }
                });
        bp.add(all);
        JButton none = new JButton("None");
        none.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        none();
                    }
                });
        bp.add(none);

        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        c.add(bp, gbc);

        JButton selectRelated = new JButton("Select Related");
        selectRelated.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        selectRelated();
                    }
                });
        gbc.gridx = 1;
        gbc.weightx = 1;
        c.add(selectRelated, gbc);

        pack();
    }

    private void ok() {
        if (doCommit) {
            ArrayList<Table> selected = new ArrayList<Table>();
            for (int i = 0; i < checkboxes.length; i++)
                if (checkboxes[i].isSelected())
                    selected.add(dirty[i]);
            if (selected.size() > 0)
                try {
                    dirty[0].getDatabase().commitTables(selected);
                    dispose();
                } catch (NavigatorException e) {
                    MessageBox.show("Commit failed!", e);
                }
            else
                dispose();
        } else {
            for (int i = 0; i < checkboxes.length; i++)
                if (checkboxes[i].isSelected()) {
                    ResultSetTableModel model = dirty[i].getModel();
                    if (model != null)
                        model.rollback();
                }
            dispose();
        }
    }

    private void cancel() {
        dispose();
    }

    private void all() {
        for (int i = 0; i < checkboxes.length; i++)
            checkboxes[i].setSelected(true);
    }

    private void none() {
        for (int i = 0; i < checkboxes.length; i++)
            checkboxes[i].setSelected(false);
    }

    private void selectRelated() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        boolean[] checked = new boolean[dirty.length];
        HashMap<String, Integer> tableMap = new HashMap<String, Integer>();

        for (int i = 0; i < dirty.length; i++) {
            Integer ii = i;
            if (checkboxes[i].isSelected()) {
                checked[i] = true;
                list.add(ii);
            }
            tableMap.put(dirty[i].getQualifiedName(), ii);
        }

        while (!list.isEmpty()) {
            int n = list.remove(0);
            Table t = dirty[n];
            ForeignKey[] fks = t.getForeignKeys();
            for (int i = 0; i < fks.length; i++) {
                String qname = fks[i].getThatQualifiedName();
                Integer ff = tableMap.get(qname);
                if (ff != null) {
                    int f = ff;
                    if (!checked[f]) {
                        checkboxes[f].setSelected(true);
                        checked[f] = true;
                        list.add(ff);
                    }
                }
            }
            ForeignKey[] rks = t.getReferencingKeys();
            for (int i = 0; i < rks.length; i++) {
                String qname = rks[i].getThatQualifiedName();
                Integer rr = tableMap.get(qname);
                if (rr != null) {
                    int r = rr;
                    if (!checked[r]) {
                        checkboxes[r].setSelected(true);
                        checked[r] = true;
                        list.add(rr);
                    }
                }
            }
        }
    }

    private static class EditHandler implements ActionListener {
        private Table table;
        public EditHandler(Table table) {
            this.table = table;
        }
        public void actionPerformed(ActionEvent e) {
            table.getDatabase().showTableFrame(table.getQualifiedName());
        }
    }
}
