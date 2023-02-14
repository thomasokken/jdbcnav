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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jdbcnav.util.MyGridBagConstraints;
import jdbcnav.util.MyGridBagLayout;


public class SequenceDialog extends MyFrame {
    private String[] items;
    private Listener listener;
    private JList<String> list;
    private Model model;
    private JButton upB;
    private JButton downB;

    public SequenceDialog(String[] items_p, Listener listener_p) {
        super("Column Sequence");
        items = items_p;
        listener = listener_p;
        Container c = getContentPane();
        c.setLayout(new GridLayout(1, 1));
        JPanel p1 = new JPanel(new MyGridBagLayout());
        c.add(p1);
        MyGridBagConstraints gbc = new MyGridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = MyGridBagConstraints.WEST;
        p1.add(new JLabel("Column order in input:"), gbc);

        model = new Model();
        list = new JList<String>(model);
        list.setVisibleRowCount(5);
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.addListSelectionListener(new ListSelectionListener() {
                            public void valueChanged(ListSelectionEvent e) {
                                int[] sel = list.getSelectedIndices();
                                if (sel.length == 0) {
                                    upB.setEnabled(false);
                                    downB.setEnabled(false);
                                } else {
                                    upB.setEnabled(sel[0] != 0);
                                    downB.setEnabled(sel[sel.length - 1]
                                                        != items.length - 1);
                                }
                            }
                        });
        JScrollPane sp = new JScrollPane(list);
        sp.setMinimumSize(new Dimension(100, 0));
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = MyGridBagConstraints.BOTH;
        gbc.anchor = MyGridBagConstraints.CENTER;
        p1.add(sp, gbc);

        JPanel p2 = new JPanel(new MyGridBagLayout());
        upB = new JButton("Up");
        upB.setEnabled(false);
        upB.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    model.up();
                                }
                            });
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = MyGridBagConstraints.HORIZONTAL;
        p2.add(upB, gbc);

        downB = new JButton("Down");
        downB.setEnabled(false);
        downB.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    model.down();
                                }
                            });
        gbc.gridy = 1;
        p2.add(downB, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = MyGridBagConstraints.NONE;
        p1.add(p2, gbc);

        p2 = new JPanel(new MyGridBagLayout());
        JButton b = new JButton("OK");
        b.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    dispose();
                                    listener.done(items);
                                }
                            });
        gbc.gridx = 0;
        gbc.gridy = 0;
        p2.add(b, gbc);

        b = new JButton("Cancel");
        b.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    dispose();
                                }
                            });
        gbc.gridx = 1;
        p2.add(b, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        p1.add(p2, gbc);
        pack();
    }

    private class Model extends AbstractListModel<String> {
        public int getSize() {
            return items.length;
        }
        public String getElementAt(int index) {
            return items[index];
        }
        public void up() {
            int[] sel = list.getSelectedIndices();
            if (sel.length == 0)
                return;
            int first = sel[0];
            int last = sel[sel.length - 1];
            if (first == 0) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            String temp = items[first - 1];
            for (int i = first - 1; i < last; i++)
                items[i] = items[i + 1];
            items[last] = temp;
            fireContentsChanged(this, first - 1, last);
            list.setSelectionInterval(first - 1, last - 1);
        }
        public void down() {
            int[] sel = list.getSelectedIndices();
            if (sel.length == 0)
                return;
            int first = sel[0];
            int last = sel[sel.length - 1];
            if (last == items.length - 1) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            String temp = items[last + 1];
            for (int i = last + 1; i > first; i--)
                items[i] = items[i - 1];
            items[first] = temp;
            fireContentsChanged(this, first, last + 1);
            list.setSelectionInterval(first + 1, last + 1);
        }
    }

    public interface Listener {
        public void done(String[] items);
    }
}
