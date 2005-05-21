package jdbcnav;

import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.text.*;
import org.mozilla.javascript.*;

import jdbcnav.model.Data;
import jdbcnav.model.Database;
import jdbcnav.model.Table;
import jdbcnav.util.NavigatorException;


public class SQLFrame extends TextEditorFrame {
    private BrowserFrame browser;
    private StringBuffer cmdBuf;
    private int outputSelectionStart = -1;
    private int outputSelectionEnd = -1;

    public SQLFrame(BrowserFrame browser) {
	super(browser.getTitle() + "/sql", "", false, false);
	this.browser = browser;
    }

    public void updateTitle() {
	setTitle(getParentTitle() + "/sql");
    }

    protected boolean wantToHandleReturn() {
	return true;
    }

    protected void handleReturn() {
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

	String cmd = text.substring(start, end);
	StringBuffer cmdBuf = new StringBuffer();
	StringTokenizer tok = new StringTokenizer(cmd, "';", true);
	boolean quote = false;
	boolean notdone = true;
	boolean firstresponse = true;

	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	PrintWriter pw = new PrintWriter(bos);

	while (notdone) {
	    notdone = tok.hasMoreTokens();
	    if (notdone) {
		String t = tok.nextToken();
		if (t.equals("'")) {
		    cmdBuf.append(t);
		    quote = !quote;
		    continue;
		} else if (t.equals(";")) {
		    if (quote) {
			cmdBuf.append(t);
			continue;
		    }
		} else {
		    cmdBuf.append(t);
		    continue;
		}
	    }

	    cmd = cmdBuf.toString().trim();
	    cmdBuf = new StringBuffer();
	    if (cmd.equals(""))
		continue;

	    try {
		Database db = browser.getDatabase();
		if (cmd.toLowerCase().startsWith("select")) {
		    Object queryOutput = db.runQuery(cmd, true, true);
		    if (queryOutput instanceof Data) {
			QueryResultFrame qrf = new QueryResultFrame(browser,
						    cmd, (Data) queryOutput);
			qrf.setParent(browser);
			qrf.showStaggered();
		    } else {
			TableFrame tf = new TableFrame((Table) queryOutput,
						       browser);
			tf.setParent(browser);
			tf.showStaggered();
		    }
		} else {
		    int count = db.runUpdate(cmd);
		    if (firstresponse)
			firstresponse = false;
		    else
			pw.println();
		    pw.print("count = " + count);
		}
	    } catch (NavigatorException e) {
		MessageBox.show(e);
		break;
	    }
	}

	pw.flush();
	String out = bos.toString();
	pw.close();

	while (end < text.length() && text.charAt(end) != '\r'
				   && text.charAt(end) != '\n')
	    end++;
	if (end == text.length())
	    out = "\n" + out;
	else
	    end++;
	if (!out.endsWith("\n"))
	    out += "\n";

	Document doc = textA.getDocument();
	try {
	    doc.insertString(end, out, null);
	} catch (BadLocationException e) {
	    MessageBox.show(e);
	}
	outputSelectionStart = end;
	outputSelectionEnd = end + out.length();
	textA.setSelectionStart(outputSelectionStart);
	textA.setSelectionEnd(outputSelectionEnd);
    }
}