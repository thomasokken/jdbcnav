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

package jdbcnav.util;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.Document;


public class NonTabJTextArea extends JTextArea {
    private static TreeSet forwardSet;
    private static TreeSet backwardSet;
    static {
	forwardSet = new TreeSet();
	forwardSet.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0));
	backwardSet = new TreeSet();
	backwardSet.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,
						    InputEvent.SHIFT_MASK));
    }

    public NonTabJTextArea() {
	super();
	init();
    }

    public NonTabJTextArea(String s) {
	super(s);
	init();
    }

    public NonTabJTextArea(String s, int width, int height) {
	super(s, width, height);
	init();
    }

    public NonTabJTextArea(Document doc, String s, int width, int height) {
	super(doc, s, width, height);
	init();
    }

    private void init() {
	setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
			      forwardSet);
	setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
			      backwardSet);
    }
}
