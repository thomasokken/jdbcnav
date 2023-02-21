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

package jdbcnav;

import java.awt.Dimension;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jdbcnav.model.Database;
import jdbcnav.util.FileUtils;
import jdbcnav.util.MyGridBagConstraints;
import jdbcnav.util.NavigatorException;


public class SearchResultsFrame extends MyFrame {
    private Database db;
    private SearchParams params;
    private SearchThread searchThread;
    private JEditorPane editor;
    
    private class SearchThread extends Thread {
        private Set<String> qualifiedNames;
        public SearchThread(Set<String> qualifiedNames) {
            this.qualifiedNames = qualifiedNames;
        }
        public void run() {
            try {
                StringBuffer html = new StringBuffer();
                html.append("<html><body><font face='lucida' size='2'>\n");
                boolean noHits = true;
                for (String qn : qualifiedNames) {
                    if (Thread.interrupted())
                        return;
                    setHtml(html + "Searching " + qn + "...\n</font></body></html>");
                    int c = db.searchTable(qn, params);
                    if (c > 0) {
                        html.append(qe(qn) + ": <a href=\"q." + qu(qn) + "\">" + c + " matching " + (c == 1 ? "row" : "rows")
                                + "</a> (<a href=\"t." + qu(qn) + "\">table</a>)<br>\n");
                        noHits = false;
                    }
                }
                if (noHits)
                    html.append("No matching rows found.");
                else
                    html.append("Done.");
                setHtml(html + "</font></body></html>");
            } catch (NavigatorException e) {
                MessageBox.show(e);
            } finally {
                SearchResultsFrame.this.searchThread = null;
            }
        }
    }
    
    private class HtmlSetter implements Runnable {
        private String html;
        public HtmlSetter(String html) {
            this.html = html;
        }
        public void run() {
            SearchResultsFrame.this.editor.setText(html);
            SearchResultsFrame.this.pack();
        }
    }
    
    private void setHtml(String html) {
        SwingUtilities.invokeLater(new HtmlSetter(html));
    }

    public SearchResultsFrame(Database db, Set<String> qualifiedNames, SearchParams params) {
        super("Search Results", true, true, true, true);
        this.db = db;
        this.params = params;

        editor = new JEditorPane();
        editor.setEditable(false);
        editor.setContentType("text/html");
        editor.addHyperlinkListener(
                    new HyperlinkListener() {
                        public void hyperlinkUpdate(HyperlinkEvent e) {
                            HyperlinkEvent.EventType t = e.getEventType();
                            String link = e.getDescription();
                            if (t == HyperlinkEvent.EventType.ACTIVATED)
                                linkActivated(link);
                        }
                    });
        editor.setText("<html><body><font face='lucida' size='2'>Wait...</font></body></html>");

        MyGridBagConstraints gbc = new MyGridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = MyGridBagConstraints.BOTH;
        gbc.weighty = 1;
        getContentPane().add(editor);

        pack();

        Dimension d = getSize();
        Dimension ds = Main.getDesktop().getSize();
        if (d.height > ds.height)
            d.height = ds.height;
        if (d.width > ds.width)
            d.width = ds.width;
        setSize(d);
        
        searchThread = new SearchThread(qualifiedNames);
        searchThread.start();
    }

    public void dispose() {
        SearchThread th = searchThread;
        if (th != null) {
            th.interrupt();
            searchThread = null;
        }
        super.dispose();
    }

    private void linkActivated(String link) {
        String qualifiedName = link.substring(2);
        if (link.startsWith("q.")) {
            db.runSearch(qualifiedName, params);
        } else {
            TableFrame editFrame = db.showTableFrame(qualifiedName);
            if (editFrame != null)
                editFrame.selectRowsForSearch(params);
        }
    }

    private static String qe(String s) {
        return FileUtils.encodeEntities(s);
    }
    
    private static String qu(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Won't happen
            return "";
        }
    }
}
