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

package jdbcnav.util;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

import jdbcnav.MyFrame;


public class ColorChooser extends MyFrame {
    private JColorChooser jcc;
    private Listener listener;

    public interface Listener {
        void apply(Color c);
        void close();
    }

    public ColorChooser(String title, Color color) {
        super(title, false, true, false, false);
        Container c = getContentPane();
        c.setLayout(new MyGridBagLayout());
        MyGridBagConstraints gbc = new MyGridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = MyGridBagConstraints.BOTH;
        jcc = new JColorChooser(color);
        c.add(jcc, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = MyGridBagConstraints.NONE;
        gbc.anchor = MyGridBagConstraints.CENTER;
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(1, 3));
        JButton b = new JButton("OK");
        b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    listener.apply(jcc.getColor());
                    dispose();
                }
            });
        p.add(b);
        b = new JButton("Apply");
        b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    listener.apply(jcc.getColor());
                }
            });
        p.add(b);
        b = new JButton("Close");
        b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
        p.add(b);
        c.add(p, gbc);

        pack();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void dispose() {
        listener.close();
        super.dispose();
    }
}
