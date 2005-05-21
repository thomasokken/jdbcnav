package jdbcnav.util;

import java.util.*;


public class CSVTokenizer {
    // Tokenizer states
    private static final int HAVE_MORE = 0;
    private static final int UNKNOWN = 1;
    private static final int FINISHED = 2;

    // States within a token
    private static final int UNQUOTED = 0;
    private static final int QUOTED = 1;
    private static final int QUOTED_PENDING = 2;

    private String s;
    private int pos;
    private String token;
    private int state = UNKNOWN;

    public CSVTokenizer(String s) {
	this.s = s;
	pos = 0;
    }

    public boolean hasMoreTokens() {
	if (state == UNKNOWN)
	    readNextToken();
	return state == HAVE_MORE;
    }

    public String nextToken() {
	if (state == UNKNOWN)
	    readNextToken();
	if (state == HAVE_MORE) {
	    state = UNKNOWN;
	    return token;
	} else
	    throw new NoSuchElementException();
    }

    private void readNextToken() {
	if (pos == s.length()) {
	    state = FINISHED;
	    return;
	}
	StringBuffer buf = new StringBuffer();
	int tokstate = UNQUOTED;
	int rawLength = 0;
	while (pos < s.length()) {
	    char ch = s.charAt(pos++);
	    if (ch == '"') {
		if (tokstate == UNQUOTED)
		    tokstate = QUOTED;
		else if (tokstate == QUOTED)
		    tokstate = QUOTED_PENDING;
		else { // tokstate == QUOTED_PENDING
		    buf.append(ch);
		    tokstate = QUOTED;
		}
	    } else {
		if (tokstate == QUOTED_PENDING) {
		    // You should only get here if ch == ','
		    tokstate = QUOTED;
		    if (ch == ',')
			break;
		} else if (tokstate == UNQUOTED && ch == ',')
		    break;
		else
		    buf.append(ch);
	    }
	    rawLength++;
	}
	token = rawLength == 0 ? null : buf.toString();
	state = HAVE_MORE;
    }
}