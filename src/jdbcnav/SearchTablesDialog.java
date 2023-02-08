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
import javax.swing.*;

import jdbcnav.util.MyGridBagConstraints;
import jdbcnav.util.MyGridBagLayout;
import jdbcnav.util.MyTextField;


public class SearchTablesDialog extends MyFrame {
    private Callback cb;
    private JTextField searchTextTF;

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
        gbc.weightx = 1;
        c.add(new JLabel("Search for: "), gbc);

        gbc.gridy++;
        searchTextTF = new MyTextField();
        c.add(searchTextTF, gbc);

        gbc.gridy++;
        JPanel p = new JPanel();
        c.add(p, gbc);

        p.setLayout(new MyGridBagLayout());
        gbc = new MyGridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
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
    }
    
    private void ok() {
        String searchText = searchTextTF.getText();
        dispose();
        cb.invoke(searchText);
    }

    public interface Callback {
        public void invoke(String searchText);
    }
}
