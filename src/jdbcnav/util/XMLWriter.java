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

import java.io.PrintWriter;
import java.util.ArrayList;


/**
 * Utility class for writing simple XML files. VERY simple XML files!
 * (No attributes supported at present!)
 */
public class XMLWriter {
    private PrintWriter pw;
    private int indent;
    private ArrayList<String> pendingTags;

    public XMLWriter(PrintWriter pw) {
        this.pw = pw;
        indent = 0;
        pendingTags = new ArrayList<String>();
        pw.println("<?xml version=\"1.0\" ?>");
    }

    /**
     * Writes an open tag (<code>&lt;tag&gt;</code>) followed by a newline;
     * increases the indentation level.
     */
    public void openTag(String tagName) {
        writeIndent();
        writeStartTag(tagName);
        pw.println();
        indent += 2;
        pendingTags.add(tagName);
    }

    /**
     * Writes an open tag (<code>&lt;tag&gt;</code>) and increases the
     * indentation level, but does not write a newline.
     */
    public void openTagNoNewline(String tagName) {
        writeIndent();
        writeStartTag(tagName);
        indent += 2;
        pendingTags.add(tagName);
    }

    /**
     * Writes a close tag (<code>&lt;/tag&gt;</code>) to match the most
     * recent unmatched open tag, then writes a newline, and decreases
     * the indentation level.
     */
    public void closeTag() {
        String tagName = pendingTags.remove(pendingTags.size() - 1);
        indent -= 2;
        writeIndent();
        writeEndTag(tagName);
        pw.println();
    }

    /**
     * Writes a close tag (<code>&lt;/tag&gt;</code>) to match the most
     * recent unmatched open tag, then writes a newline, and decreases
     * the indentation level. The tag is written without first writing any
     * indentation whitespace; this can be used to write a close tag on the
     * same line as its open tag (i.e. after using
     * <code>openTagNoNewline()</code>).
     */
    public void closeTagNoIndent() {
        String tagName = pendingTags.remove(pendingTags.size() - 1);
        indent -= 2;
        writeEndTag(tagName);
        pw.println();
    }

    /**
     * Writes an indented open tag, followed by a value, followed by a close
     * tag, all on one line. If the value is <code>null</code>, nothing is
     * written (to distinguish this from the case of the empty string, which
     * is written as the open tag immediately followed by the close tag (which
     * is equivalent to an empty tag (i.e., <code>&lt;tag/&gt;</code>).
     * <br>
     * The string is encoded using <code>writeValue()</code>.
     */
    public void wholeTag(String tagName, Object tagValue) {
        if (tagValue == null)
            return;
        writeIndent();
        writeStartTag(tagName);
        writeValue(tagValue.toString());
        writeEndTag(tagName);
        pw.println();
    }
    
    /**
     * Writes a string value. The <code>'&lt;'</code> character is replaced by
     * <code>'&amp;lt;'</code>, <code>'&gt;'</code> by <code>'&amp;gt;'</code>,
     * <code>'&amp;'</code> by <code>'&amp;amp;'</code>, and all character
     * codes outside the range of printable ASCII codes (32 through 126) are
     * replaced by <code>'&amp;#n;'</code>, where <code>n</code> is the decimal
     * character code.
     */
    public void writeValue(String value) {
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char ch = value.charAt(i);
            if (ch == '<')
                pw.print("&lt;");
            else if (ch == '>')
                pw.print("&gt;");
            else if (ch == '&')
                pw.print("&amp;");
            else if (ch >= 32 && ch <= 126)
                pw.print(ch);
            else {
                pw.print("&#");
                pw.print((int) ch);
                pw.print(";");
            }
        }
    }

    /**
     * Writes <code>length</code> characters from the array <code>buf</code>,
     * starting at offset <code>offset</code>. This method translates
     * characters just like <code>writeValue(String)</code>.
     */
    public void writeValue(char[] buf, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            char ch = buf[i];
            if (ch == '<')
                pw.print("&lt;");
            else if (ch == '>')
                pw.print("&gt;");
            else if (ch == '&')
                pw.print("&amp;");
            else if (ch >= 32 && ch <= 126)
                pw.print(ch);
            else {
                pw.print("&#");
                pw.print((int) ch);
                pw.print(";");
            }
        }
    }

    /**
     * Writes a newline.
     */
    public void newLine() {
        pw.println();
    }

    /**
     * Writes a comment at the current indentation level.
     */
    public void writeComment(String comment) {
        writeIndent();
        pw.print("<!-- ");
        pw.print(comment);
        pw.println(" -->");
    }

    /**
     * Writes whitespace to the point of the current indentation level.
     */
    public void writeIndent() {
        for (int i = 0; i < indent; i++)
            pw.print(" ");
    }

    private void writeStartTag(String tagName) {
        pw.print("<");
        writeValue(tagName);
        pw.print(">");
    }

    private void writeEndTag(String tagName) {
        pw.print("</");
        writeValue(tagName);
        pw.print(">");
    }
}
