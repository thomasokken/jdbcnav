///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2010  Thomas Okken
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

import jdbcnav.util.*;


public class ClipboardFrame extends MyFrame implements Clipboard.Listener {
    private JCheckBoxMenuItem wrapLinesMI;
    private JScrollPane scrollPane;
    private JTextArea jta;

    public ClipboardFrame(Clipboard clipboard) {
        super("Clipboard", true, true, true, true);
        getContentPane().setLayout(new GridLayout(1, 1));
        scrollPane = new JScrollPane();
        getContentPane().add(scrollPane);

        JMenuBar mb = new JMenuBar();
        JMenu m = new JMenu("Clipboard");
        wrapLinesMI = new JCheckBoxMenuItem("Wrap Lines");
        wrapLinesMI.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if (jta != null)
                                jta.setLineWrap(wrapLinesMI.getState());
                        }
                    });
        m.add(wrapLinesMI);
        JMenuItem mi = new JMenuItem("Refresh");
        mi.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            clipboardUpdated(Main.getClipboard().get());
                        }
                    });
        m.add(mi);
        mi = new JMenuItem("Close");
        mi.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            dispose();
                        }
                    });
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, MiscUtils.getMenuShortcutKeyMask()));
        m.add(mi);
        mb.add(m);
        setJMenuBar(mb);
        clipboardUpdated(clipboard.get());
        setSize(500, 300);

        Main.getClipboard().addListener(this);
    }

    public void dispose() {
        Main.getClipboard().removeListener(this);
        super.dispose();
    }

    public void clipboardUpdated(Object data) {
        jta = null;
        if (data == null) {
            wrapLinesMI.setEnabled(false);
            scrollPane.setViewportView(new JLabel(""));
        } else if (data instanceof Object[][]) {
            wrapLinesMI.setEnabled(false);
            MyTable table = new MyTable(new ArrayTableModel((Object[][]) data));
            table.setNiceSize();
            scrollPane.setViewportView(table);
        } else if (data instanceof byte[]) {
            jta = new MyTextArea();
            jta.setEditable(false);
            jta.setLineWrap(wrapLinesMI.getState());
            StringBuffer buf = new StringBuffer();
            byte[] b = (byte[]) data;
            String nybble = "0123456789abcdef";
            for (int i = 0; i < b.length; i++) {
                byte B = b[i];
                buf.append(nybble.charAt((B >> 4) & 15));
                buf.append(nybble.charAt(B & 15));
                if (i != b.length - 1) {
                    if (i % 16 == 15)
                        buf.append('\n');
                    else if (i % 8 == 7)
                        buf.append("  ");
                    else
                        buf.append(' ');
                }
            }
            jta.setText(buf.toString());
            scrollPane.setViewportView(jta);
        } else {
            jta = new MyTextArea();
            jta.setEditable(false);
            jta.setLineWrap(wrapLinesMI.getState());
            jta.setText(String.valueOf(data));
            wrapLinesMI.setEnabled(true);
            scrollPane.setViewportView(jta);
        }
    }
}
