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
