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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jdbcnav.util.MyGridBagConstraints;
import jdbcnav.util.MyGridBagLayout;
import jdbcnav.util.MyTextField;


public class SearchTablesDialog extends MyFrame {
    private Callback cb;
    private JTextField searchTextTF;
    private JTextField intervalTF;
    private JCheckBox matchSubstringCB;
    
    private static String lastSearchText;
    private static String lastInterval;
    private static boolean lastMatchSubstring;

    public SearchTablesDialog(BrowserFrame bf, Callback cb) {
        super("Search Tables");
        this.cb = cb;
        Container c = getContentPane();

        c.setLayout(new GridLayout(1, 1));
        JPanel jp = new JPanel();
        c.add(jp);
        c = jp;

        c.setLayout(new MyGridBagLayout());
        MyGridBagConstraints gbc = new MyGridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = MyGridBagConstraints.WEST;
        gbc.gridwidth = 1;
        gbc.fill = MyGridBagConstraints.HORIZONTAL;
        gbc.weightx = 4;
        c.add(new JLabel("Search for: "), gbc);

        gbc.gridy++;
        searchTextTF = new MyTextField(16);
        searchTextTF.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    ok();
                                }
                            });
        c.add(searchTextTF, gbc);
        
        gbc.gridx++;
        gbc.weightx = 0;
        c.add(new JLabel("\u00b1"), gbc);
        
        gbc.gridx++;
        gbc.weightx = 1;
        intervalTF = new MyTextField(4);
        intervalTF.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    ok();
                                }
                            });
        c.add(intervalTF, gbc);
        
        gbc.gridx++;
        gbc.weightx = 0;
        JButton helpB = new JButton("?");
        helpB.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JOptionPane.showInternalMessageDialog(
                            Main.getDesktop(),
                            "You can use the \u00b1 field to specify a search interval. The value\n"
                            + "in this field is interpreted as a floating-point number.\n"
                            + "When searching floating-point fields, this value is subtracted\n"
                            + "and added to the search value as is, and when searching date/time\n"
                            + "fields, it is interpreted as a number of days.\n"
                            + "date/time intervals can be specified in d.ddd or d:h:m:s.sss formats.\n"
                            + "For integer and date fields, only the integer part of this value is used.\n"
                            + "Leave this field blank to search for exact matches.");
                    }
                });
        c.add(helpB, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 4;
        gbc.weightx = 1;
        matchSubstringCB = new JCheckBox("Match Substring");
        matchSubstringCB.setSelected(true);
        c.add(matchSubstringCB, gbc);

        gbc.gridy++;
        JPanel p = new JPanel();
        c.add(p, gbc);

        p.setLayout(new MyGridBagLayout());
        gbc = new MyGridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        JButton okB = new JButton("OK");
        okB.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    ok();
                                }
                            });
        p.add(okB, gbc);
        JButton cancelB = new JButton("Cancel");
        cancelB.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    dispose();
                                }
                            });
        gbc.gridx++;
        p.add(cancelB, gbc);
        pack();
        
        // Restore last accepted state
        String initialSearchText = null;
        String initialInterval = null;
        boolean initialMatchSubstring = false;;
        synchronized (getClass()) {
            initialSearchText = lastSearchText;
            initialInterval = lastInterval;
            initialMatchSubstring = lastMatchSubstring;
        }
        if (initialSearchText != null) {
            searchTextTF.setText(initialSearchText);
            intervalTF.setText(initialInterval);
            matchSubstringCB.setSelected(initialMatchSubstring);
        }
    }
    
    private void ok() {
        String searchText = searchTextTF.getText();
        String intervalText = intervalTF.getText().trim();
        boolean matchSubstring = matchSubstringCB.isSelected();
        SearchParams params;
        try {
            params = new SearchParams(searchText, intervalText, matchSubstring);
        } catch (NumberFormatException e) {
            MessageBox.show("Invalid interval \"" + intervalText + "\"", null);
            return;
        }
        // Save state to use with next instance
        synchronized (getClass()) {
            lastSearchText = searchText;
            lastInterval = intervalText;
            lastMatchSubstring = matchSubstring;
        }
        dispose();
        cb.invoke(params);
    }

    public interface Callback {
        public void invoke(SearchParams params);
    }
}
