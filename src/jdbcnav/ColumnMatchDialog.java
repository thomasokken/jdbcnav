///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2024  Thomas Okken
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
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jdbcnav.util.MyGridBagConstraints;
import jdbcnav.util.MyGridBagLayout;
import jdbcnav.util.MyTextArea;


public class ColumnMatchDialog extends MyFrame {
    private String[] clipCols;
    private String[] tableCols;
    private JList<String> clipList;
    private JList<String> tableList;
    private boolean ignoreTableListSelectionChange;
    private String[] mapping;
    private JTextArea summaryArea;
    private JRadioButton leaveUnchangedButton;
    private Listener listener;

    public ColumnMatchDialog(String[] clipCols, String[] tableCols, Listener listener_p) {
        super("Match Columns");
        this.clipCols = clipCols;
        this.tableCols = tableCols;
        listener = listener_p;

        Container c = getContentPane();
        c.setLayout(new MyGridBagLayout());

        JLabel label = new JLabel("Clipboard:");
        MyGridBagConstraints gbc = new MyGridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = MyGridBagConstraints.BOTH;
        c.add(label, gbc);

        label = new JLabel("Destination:");
        gbc.gridx++;
        c.add(label, gbc);

        clipList = new JList<String>(clipCols);
        clipList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clipList.addListSelectionListener(new ListSelectionListener() {
                        public void valueChanged(ListSelectionEvent e) {
                            clipListSelectionChanged();
                        }
                    });
        gbc.gridx = 0;
        gbc.gridy++;
        c.add(new JScrollPane(clipList), gbc);

        tableList = new JList<String>(tableCols);
        tableList.addListSelectionListener(new ListSelectionListener() {
                        public void valueChanged(ListSelectionEvent e) {
                            tableListSelectionChanged();
                        }
                    });
        gbc.gridx++;
        c.add(new JScrollPane(tableList), gbc);

        label = new JLabel("Summary:");
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        c.add(label, gbc);

        summaryArea = new MyTextArea(8, 80);
        summaryArea.setFont(new Font("Courier", Font.PLAIN, 12));
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setEditable(false);
        gbc.gridy++;
        c.add(new JScrollPane(summaryArea), gbc);
        
        JPanel p = new JPanel();
        p.setLayout(new MyGridBagLayout());
        gbc.gridy++;
        c.add(p, gbc);

        MyGridBagConstraints gbc2 = new MyGridBagConstraints();
        gbc2.gridx = 0;
        gbc2.gridy = 0;
        label = new JLabel("Unmatched destination columns: ");
        p.add(label, gbc2);
        ButtonGroup group = new ButtonGroup();
        leaveUnchangedButton = new JRadioButton("Leave unchanged", true);
        group.add(leaveUnchangedButton);
        gbc2.gridx++;
        p.add(leaveUnchangedButton, gbc2);
        JRadioButton radio = new JRadioButton("Set to null");
        group.add(radio);
        gbc2.gridx++;
        p.add(radio, gbc2);

        p = new JPanel();
        p.setLayout(new MyGridBagLayout());
        gbc.gridy++;
        c.add(p, gbc);

        JButton button = new JButton("OK");
        button.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            dispose();
                            listener.done(mapping,
                                !leaveUnchangedButton.isSelected());
                        }
                    });
        gbc = new MyGridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        p.add(button, gbc);

        button = new JButton("Cancel");
        button.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            dispose();
                        }
                    });
        gbc.gridx++;
        p.add(button, gbc);

        initializeMapping();
        updateSummary();
        clipList.addSelectionInterval(0, 0);

        pack();
    }

    public interface Listener {
        public void done(String[] destCols, boolean setNull);
    }

    private void initializeMapping() {
        int tlen = tableCols.length;
        mapping = new String[tlen];
        for (int t = 0; t < tlen; t++)
            for (int c = 0; c < clipCols.length; c++)
                if (tableCols[t].equalsIgnoreCase(clipCols[c])) {
                    mapping[t] = clipCols[c];
                    break;
                }
    }

    private void clipListSelectionChanged() {
        ignoreTableListSelectionChange = true;
        tableList.clearSelection();
        int c = clipList.getSelectedIndex();
        if (c != -1) {
            String clipName = clipCols[c];
            for (int t = 0; t < tableCols.length; t++)
                if (mapping[t] == clipName)
                    tableList.addSelectionInterval(t, t);
            int firstSel = tableList.getMinSelectionIndex();
            if (firstSel != -1) {
                int lastSel = tableList.getMaxSelectionIndex();
                int firstVis = tableList.getFirstVisibleIndex();
                int lastVis = tableList.getLastVisibleIndex();
                if (firstSel < firstVis || lastSel > lastVis) {
                    int toVis = firstSel + lastVis - firstVis;
                    if (toVis > lastSel)
                        toVis = lastSel;
                    Rectangle r = tableList.getCellBounds(firstSel, toVis);
                    tableList.scrollRectToVisible(r);
                }
            }
        }
        ignoreTableListSelectionChange = false;
    }

    private void tableListSelectionChanged() {
        if (ignoreTableListSelectionChange)
            return;
        int c = clipList.getSelectedIndex();
        if (c == -1)
            return;
        int[] tt = tableList.getSelectedIndices();
        for (int t = 0; t < mapping.length; t++)
            if (mapping[t] == clipCols[c])
                mapping[t] = null;
        for (int i = 0; i < tt.length; i++)
            mapping[tt[i]] = clipCols[c];
        updateSummary();
    }

    private void updateSummary() {
        StringBuffer buf = new StringBuffer();
        for (int c = 0; c < clipCols.length; c++) {
            if (c > 0)
                buf.append("\n");
            String cs = clipCols[c];
            buf.append(cs);
            buf.append(" =>");
            boolean first = true;
            for (int t = 0; t < tableCols.length; t++)
                if (cs == mapping[t]) {
                    if (first) {
                        buf.append(" ");
                        first = false;
                    } else
                        buf.append(", ");
                    buf.append(tableCols[t]);
                }
        }
        String oldText = summaryArea.getText();
        String newText = buf.toString();
        if (!oldText.equals(newText)) {
            int oldLength = oldText.length();
            int newLength = newText.length();
            int minLength = oldLength < newLength ? oldLength : newLength;
            int i;
            for (i = 0; i < minLength; i++)
                if (oldText.charAt(i) != newText.charAt(i))
                    break;
            summaryArea.setText(newText);
            summaryArea.setCaretPosition(i);
        }
    }
}
