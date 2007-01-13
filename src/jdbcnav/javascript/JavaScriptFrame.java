///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2007  Thomas Okken
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

package jdbcnav.javascript;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import org.mozilla.javascript.*;

import jdbcnav.*;
import jdbcnav.util.*;


public class JavaScriptFrame extends TextEditorFrame {
    private int outputSelectionStart = -1;
    private int outputSelectionEnd = -1;
    private Thread jsThread;

    public JavaScriptFrame() {
	super("JavaScript", "", false, false);
	InputMap im = textA.getInputMap();
	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, InputEvent.CTRL_MASK),
	       "javascript_interrupt");
	ActionMap am = textA.getActionMap();
	am.put("javascript_interrupt",
	       new AbstractAction() {
		    public void actionPerformed(ActionEvent e) {
			Thread thr = jsThread;
			if (thr != null)
			    thr.interrupt();
		    }
		});
    }

    protected boolean wantToHandleReturn() {
	return true;
    }

    protected void handleReturn() {
	if (jsThread != null) {
	    JOptionPane.showInternalMessageDialog(Main.getDesktop(),
				"You can't run two commands at once in one "
			      + "JavaScript window.\nYou can use Ctrl-. to "
			      + "abort the running command.");
	    return;
	}

	String text = textA.getText();
	int start = textA.getSelectionStart();
	int end = textA.getSelectionEnd();

	if (start == outputSelectionStart && end == outputSelectionEnd) {
	    // A little ugly convenience hackery: after evaluating
	    // a JavaScript snippet, the output is highlighted; hitting
	    // return again makes the highlight go away and places the
	    // cursor at the end of the output.
	    textA.setSelectionStart(end);
	    outputSelectionStart = -1;
	    outputSelectionEnd = -1;
	    return;
	}

	boolean noSelection = start == end;
	if (noSelection) {
	    // No selection; find the start and end of the current line,
	    // and use those as the selection boundaries instead.
	    while (start > 0 && text.charAt(start - 1) != '\n'
			     && text.charAt(start - 1) != '\r')
		start--;
	    while (end < text.length() && text.charAt(end) != '\n'
				       && text.charAt(end) != '\r')
		end++;
	}

	jsThread = new JSThread(text.substring(start, end), end);
	jsThread.start();
    }
    
    private class JSThread extends Thread {
	private String cmd;
	private int pos;
	public JSThread(String command, int outputPos) {
	    cmd = command;
	    pos = outputPos;
	    setPriority(Thread.MIN_PRIORITY);
	    setDaemon(true);
	}

	public void run() {
	    try {
		setTitle("JavaScript - running");
		int start = pos;
		Document doc = textA.getDocument();
		synchronized (doc) {
		    String text = textA.getText();
		    if (pos > text.length())
			pos = text.length();
		    try {
			if (pos > 0 && text.charAt(pos - 1) != '\n'
				    && text.charAt(pos - 1) != '\r')
			    doc.insertString(pos++, "\n", null);
			doc.insertString(pos, "\n", null);
		    } catch (BadLocationException e) {
			MessageBox.show(e);
		    }
		}

		Context ctx = Context.enter(new InterruptableContext());
		JSPipe pipe = new JSPipe(pos);
		try {
		    JavaScriptGlobal global = Main.getJSGlobal();
		    global.setOut(pipe);
		    Object resObj = ctx.evaluateString(global, cmd,
						       "<stdin>", 0, null);
		    if (resObj != Context.getUndefinedValue())
			pipe.println(Context.toString(resObj));
		} catch (JavaScriptException e) {
		    // Probably won't happen any more; JavaScriptException is
		    // flagged as 'deprecated' in Rhino 1.5r5, and it doesn't
		    // look like Rhino itself throws it anywhere... And I
		    // removed all occurrences of JavaScriptException from
		    // JDBC Navigator on 8/23/2004. Using only
		    // EvaluatorException and subclasses of same from within
		    // my JavaScript handlers (and yes, JSInterruptedException
		    // is also an EvaluatorException as of 8/23/2004.
		    pipe.println("JavaScriptException:");
		    pipe.println(MiscUtils.throwableToString(e));
		    pipe.println("Value: " + Context.toString(e.getValue()));
		} catch (JSInterruptedException e) {
		    // This happens when we throw the JSIE from the
		    // observeInstructionCount() method -- the exception is
		    // not wrapped in a JavaScriptException in this case.
		    pipe.println();
		    pipe.println("Interrupted.");
		} catch (Exception e) {
		    MessageBox.show("Exception in JavaScript interpreter", e);
		    pipe.println("Exception.");
		}
		Context.exit();

		int end = pipe.getPos() + 1;
		outputSelectionStart = start;
		outputSelectionEnd = end;
		textA.setSelectionStart(start);
		textA.setSelectionEnd(end);
	    } finally {
		jsThread = null;
		setTitle("JavaScript");
	    }
	}
    }

    private class JSPipe implements JavaScriptGlobal.Pipe {
	private int pos;
	public JSPipe(int outputPos) {
	    pos = outputPos;
	}
	public int getPos() {
	    return pos;
	}
	public void println(String s) {
	    print(s + "\n");
	}
	public void println() {
	    print("\n");
	}
	public void print(String s) {
	    Document doc = textA.getDocument();
	    synchronized (doc) {
		int length = doc.getLength();
		if (pos > length)
		    pos = length;
		try {
		    doc.insertString(pos, s, null);
		    pos += s.length();
		    textA.setCaretPosition(pos);
		} catch (BadLocationException e) {
		    MessageBox.show(e);
		} catch (Error e) {
		    // PlainDocument.insertString() throws an Error if it is
		    // interrupted while trying to get the write lock. It would
		    // be nice if it would throw an actual documented Exception
		    // (compare InterruptedIOException), but it doesn't.
		    // In order to try and at least avoid catching other
		    // Errors, e.g. OutOfMemoryError, I check that the class
		    // is exactly Error. Hopefully this will actually work
		    // across JDK versions (using 1.4.0-beta2 at the moment).
		    // 8/23/2004 Update: still OK with JDK 1.5.0-beta2.
		    if (e.getClass() == Error.class)
			throw new JSInterruptedException();
		    else
			throw e;
		}
	    }
	}
    }

    private static class InterruptableContext extends Context {
	public InterruptableContext() {
	    setOptimizationLevel(-1);
	    setInstructionObserverThreshold(1000);
	}
	protected void observeInstructionCount(int count) {
	    if (Thread.interrupted())
		throw new JSInterruptedException();
	}
    }

    private static class JSInterruptedException extends EvaluatorException {
	public JSInterruptedException() {
	    super("JavaScript interrupted.");
	}
    }
}
