///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2024  Thomas Okken
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

import java.util.NoSuchElementException;


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
        boolean quoted = false;
        int rawLength = 0;
        boolean escape = false;
        while (pos < s.length()) {
            char ch = s.charAt(pos++);
            if (escape) {
                if (ch == '\\')
                    buf.append(ch);
                else if (ch == 'r')
                    buf.append('\r');
                else if (ch == 'n')
                    buf.append('\n');
                else
                    buf.append(ch);
                escape = false;
                rawLength++;
            } else if (ch == '\\') {
                escape = true;
            } else if (ch == '"') {
                if (tokstate == UNQUOTED) {
                    tokstate = QUOTED;
                    quoted = true;
                } else if (tokstate == QUOTED) {
                    tokstate = QUOTED_PENDING;
                } else { // tokstate == QUOTED_PENDING
                    buf.append(ch);
                    tokstate = QUOTED;
                    rawLength++;
                }
            } else {
                if (tokstate == QUOTED_PENDING) {
                    // You should only get here if ch == ','
                    tokstate = QUOTED;
                    if (ch == ',')
                        break;
                } else if (tokstate == UNQUOTED && ch == ',') {
                    break;
                } else {
                    buf.append(ch);
                    rawLength++;
                }
            }
        }
        token = !quoted && rawLength == 0 ? null : buf.toString();
        state = HAVE_MORE;
    }
}
