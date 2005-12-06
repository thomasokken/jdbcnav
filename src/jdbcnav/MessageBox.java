///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2005  Thomas Okken
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
import java.io.*;
import java.util.*;
import javax.swing.*;

import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.WrappedException;
import org.xml.sax.SAXException;

import jdbcnav.util.MyGridBagConstraints;
import jdbcnav.util.MyGridBagLayout;
import jdbcnav.util.NonTabJTextArea;
import jdbcnav.util.NavigatorException;


public class MessageBox {
    
    private MessageBox() {
	// There's nothing to construct!
    }
    
    public static void show(Throwable th) {
	show(null, th);
    }
    
    public static void show(String message, Throwable th) {
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	PrintWriter pw = new PrintWriter(bos);

	ArrayList throwables = new ArrayList();
	boolean need_empty_line = false;

	if (message != null) {
	    pw.println(message);
	    need_empty_line = true;
	}

	while (th != null) {
	    String m = th.getMessage();
	    if (m != null) {
		need_empty_line = true;
		pw.println(m);
	    }
	    throwables.add(th);
	    if (th instanceof NavigatorException)
		th = ((NavigatorException) th).getRootCause();
	    else if (th instanceof SAXException)
		th = ((SAXException) th).getException();
	    else if (th instanceof JavaScriptException) {
		Object val = ((JavaScriptException) th).getValue();
		th = val instanceof Throwable ? (Throwable) val : null;
	    } else if (th instanceof WrappedException)
		th = ((WrappedException) th).getWrappedException();
	    else
		break;
	}
	
	for (Iterator iter = throwables.iterator(); iter.hasNext();) {
	    if (need_empty_line)
		pw.println();
	    else
		need_empty_line = true;
	    ((Throwable) iter.next()).printStackTrace(pw);
	}

	pw.flush();
	if (SwingUtilities.isEventDispatchThread())
	    show(bos.toString());
	else
	    SwingUtilities.invokeLater(new LaterShower(bos.toString()));
    }

    private static void show(String message) {
	MyFrame f = new MyFrame("Message");
	Container c = f.getContentPane();
	c.setLayout(new GridLayout(1, 1));
	JPanel p = new JPanel();
	p.setLayout(new MyGridBagLayout());
	c.add(p);
	MyGridBagConstraints gbc = new MyGridBagConstraints();
	gbc.gridx = 0;
	gbc.gridy = 0;
	JTextArea t = new NonTabJTextArea(message);
	t.setFont(new Font("Courier", Font.PLAIN, 12));
	Dimension dim = countRowsAndColumns(message);
	t.setRows(Math.min(dim.height, 20));
	t.setColumns(Math.min(dim.width, 80));
	t.setCaretPosition(0);
	t.setEditable(false);
	JScrollPane s = new JScrollPane(t);
	p.add(s, gbc);
	JButton b = new JButton("OK");
	b.addActionListener(new Disposer(f));
	gbc.gridy = 1;
	p.add(b, gbc);
	f.pack();
	Toolkit.getDefaultToolkit().beep();
	f.showCentered();
	b.requestFocusInWindow();
    }

    private static class LaterShower implements Runnable {
	private String message;
	public LaterShower(String message) {
	    this.message = message;
	}
	public void run() {
	    show(message);
	}
    }

    private static class Disposer implements ActionListener {
	private JInternalFrame f;
	public Disposer(JInternalFrame f) {
	    this.f = f;
	}
	public void actionPerformed(ActionEvent e) {
	    f.dispose();
	}
    }

    private static Dimension countRowsAndColumns(String message) {
	int rows = 1;
	int columns = 0;
	boolean prevWasCR = false;
	StringTokenizer tok = new StringTokenizer(message, "\n\r", true);
	while (tok.hasMoreTokens()) {
	    String s = tok.nextToken();
	    if (s.equals("\r")) {
		prevWasCR = true;
		rows++;
	    } else {
		if (s.equals("\n")) {
		    if (!prevWasCR)
			rows++;
		} else {
		    int length = s.length();
		    if (length > columns)
			columns = length;
		}
		prevWasCR = false;
	    }
	}
	return new Dimension(columns, rows);
    }
}
