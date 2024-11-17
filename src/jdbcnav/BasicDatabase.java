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

package jdbcnav;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jdbcnav.model.BlobWrapper;
import jdbcnav.model.BrowserNode;
import jdbcnav.model.Database;
import jdbcnav.model.DateTime;
import jdbcnav.model.Interval;
import jdbcnav.model.Table;
import jdbcnav.model.TypeSpec;
import jdbcnav.util.FileUtils;
import jdbcnav.util.NavigatorException;


/**
 * Despite its name, this class isn't really a basic database; it's just
 * where I put the code that JDBCDatabase and FileDatabase have in common.
 */
public abstract class BasicDatabase implements Database {

    protected BrowserFrame browser;
    private ScriptGenerator sg;
    private MyNode rootNode;
    private MyNode orphanage;

    // Initialize this object using the seemingly redundant String(String)
    // constructor, in order to ensure that we have a unique instance.
    // OK, so I'm paranoid, but I want to make sure that the special-case
    // code in myStringComparator, BasicTable, and QueryResultFrame *only*
    // triggers on this particular instance of the word.
    public static final String ORPHANAGE = new String("orphanage");

    public abstract String getName();

    public BrowserNode getRootNode() {
        if (rootNode == null) {
            rootNode = new MyNode("root");
            reloadTree();
        }
        return rootNode;
    }

    public String[] getCommands() {
        return new String[] {
            "Edit",
            "Details",
            "Search Tables...",
            "-",
            "Generate Script...",
            "Duplicate",
            "Remove Orphans",
            "Clear Cache",
            "-",
            "Reload Tree"
        };
    }

    public void executeCommand(int command) {
        switch (command) {
            case -1: // Double-click
            case 0:
                edit();
                break;
            case 1:
                details();
                break;
            case 2:
                searchTables();
                break;
            case 3:
                generateScript();
                break;
            case 4:
                duplicate();
                break;
            case 5:
                removeOrphans();
                break;
            case 6:
                clearCache();
                break;
            case 7:
                reloadTree();
                break;
        }
    }

    private void edit() {
        Collection<BrowserNode> selection = browser.getSelectedNodes();
        for (BrowserNode bn : selection) {
            if (!(bn instanceof MyNode))
                continue;
            MyNode n = (MyNode) bn;
            if (!n.isLeaf())
                continue;
            MyNode target;
            if ((target = n.getTarget()) != null)
                n = target;
            if (n.edit != null)
                n.edit.deiconifyAndRaise();
            else {
                try {
                    Main.backgroundJobStarted();
                    n.edit = new TableFrame(n.getTable(), browser);
                    n.edit.setParent(browser);
                    n.edit.showStaggered();
                } catch (NavigatorException e) {
                    MessageBox.show("Can't open Table window", e);
                } finally {
                    Main.backgroundJobEnded();
                }
            }
        }
    }

    private void details() {
        Collection<BrowserNode> selection = browser.getSelectedNodes();
        for (BrowserNode bn : selection) {
            if (!(bn instanceof MyNode))
                continue;
            MyNode n = (MyNode) bn;
            if (!n.isLeaf())
                continue;
            MyNode target;
            if ((target = n.getTarget()) != null)
                n = target;
            if (n.details != null)
                n.details.deiconifyAndRaise();
            else {
                try {
                    Main.backgroundJobStarted();
                    n.details = new TableDetailsFrame(n.getTable(), browser);
                    n.details.setParent(browser);
                    n.details.showStaggered();
                } catch (NavigatorException e) {
                    MessageBox.show("Can't open Table Details window", e);
                } finally {
                    Main.backgroundJobEnded();
                }
            }
        }
    }
    
    private void searchTables() {
        SearchTablesDialog.Callback cb = new SearchTablesDialog.Callback() {
            public void invoke(SearchParams params) {
                searchTables2(params);
            }
        };
        SearchTablesDialog std = new SearchTablesDialog(browser, cb);
        std.showCentered();
    }
    
    private void searchTables2(SearchParams params) {
        Collection<BrowserNode> selection = browser.getSelectedNodes();
        Set<String> tables = new TreeSet<String>();
        for (BrowserNode bn : selection) {
            if (!(bn instanceof MyNode))
                continue;
            MyNode n = (MyNode) bn;
            if (!n.isLeaf())
                continue;
            MyNode target;
            if ((target = n.getTarget()) != null)
                n = target;
            tables.add(n.qualifiedName);
        }
        if (!tables.isEmpty()) 
            try {
                Main.backgroundJobStarted();
                searchTables(tables, params);
            } catch (NavigatorException e) {
                MessageBox.show("Search Tables failed", e);
            } finally {
                Main.backgroundJobEnded();
            }
    }

    private void generateScript() {
        GenerateScriptDialog.Callback cb = new GenerateScriptDialog.Callback() {
            public void invoke(int what, boolean fqtn, BrowserFrame other,
                               String sgname) {
                sg = ScriptGenerator.getInstance(sgname);
                generateScript2(what, fqtn, other);
            }
        };
        String sgname;
        if (sg != null)
            sgname = sg.getName();
        else
            sgname = getInternalDriverName();
        GenerateScriptDialog gsd = new GenerateScriptDialog(browser, cb, sgname);
        gsd.showCentered();
    }

    private void generateScript2(int what, boolean fqtn, BrowserFrame other) {
        Collection<Table> thisTS, otherTS;
        try {
            thisTS = getSelectedTables();
            otherTS = null;
            if (other != null)
                otherTS = other.getDatabase().getSelectedTables();
        } catch (NavigatorException e) {
            MessageBox.show("Generating script failed!", e);
            return;
        }

        // Spawn a background thread for doing the hard work. It may take
        // quite a while, mostly because it may require a lot of data to be
        // loaded from the database.
        // TODO: progress dialog, or at least something that says work is in
        // progress (even if there's nothing to report in terms of percent
        // complete or such), and gives the user a chance to abort the
        // operation.

        Thread t = new Thread(new GenerateScript3(what, fqtn, thisTS, otherTS));
        t.setPriority(Thread.MIN_PRIORITY);
        t.setDaemon(true);
        t.start();
    }

    private class GenerateScript3 implements Runnable {
        private int what;
        private boolean fqtn;
        private Collection<Table> thisTS;
        private Collection<Table> otherTS;

        public GenerateScript3(int what, boolean fqtn, Collection<Table> thisTS,
                                                       Collection<Table> otherTS) {
            this.what = what;
            this.fqtn = fqtn;
            this.thisTS = thisTS;
            this.otherTS = otherTS;
        }

        public void run() {
            String title = null;
            StringBuffer script = new StringBuffer();
            try {
                switch (what) {
                    case GenerateScriptDialog.DROP:
                        title = "Drop Script";
                        script.append(sg.drop(thisTS, fqtn));
                        break;
                    case GenerateScriptDialog.CREATE:
                        title = "Drop, Create Script";
                        script.append(sg.drop(thisTS, fqtn));
                        script.append(sg.create(thisTS, fqtn));
                        script.append(sg.keys(thisTS, fqtn));
                        break;
                    case GenerateScriptDialog.REBUILD:
                        title = "Drop, Create, Populate Script";
                        script.append(sg.drop(thisTS, fqtn));
                        script.append(sg.create(thisTS, fqtn));
                        script.append(sg.populate(thisTS, fqtn));
                        script.append(sg.keys(thisTS, fqtn));
                        break;
                    case GenerateScriptDialog.UPDATE_FROM:
                        title = "Update Script";
                        script.append(sg.diff(otherTS, thisTS, fqtn));
                        break;
                    case GenerateScriptDialog.UPDATE_TO:
                        title = "Update Script";
                        script.append(sg.diff(thisTS, otherTS, fqtn));
                        break;
                }
            } catch (NavigatorException e) {
                MessageBox.show(e);
                return;
            }

            // The hard work is done; now, rejoin the AWT event thread
            // to present the result to the user.
            SwingUtilities.invokeLater(
                        new GenerateScript4(script.toString(), title));
        }
    }

    private class GenerateScript4 implements Runnable {
        private String s;
        private String title;
        public GenerateScript4(String s, String title) {
            this.s = s;
            this.title = title;
        }
        public void run() {
            if (s.length() > 1024 * 1024) {
                int res = JOptionPane.showInternalConfirmDialog(
                            Main.getDesktop(),
                            "The script is rather large (" + s.length()
                            + " characters).\nDisplaying it in a window"
                            + " may fail if memory is low.\n"
                            + "Would you like to save it to a file instead?",
                            "Confirm", JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                if (res == JOptionPane.CANCEL_OPTION || res == JOptionPane.CLOSED_OPTION)
                    return;
                else if (res == JOptionPane.YES_OPTION) {
                    File file = null;
                    while (true) {
                        JFileChooser jfc = new MyFileChooser();
                        jfc.setDialogTitle("Save");
                        if (file != null)
                            jfc.setSelectedFile(file);
                        if (jfc.showSaveDialog(Main.getDesktop())
                                                != JFileChooser.APPROVE_OPTION)
                            return;
                        file = jfc.getSelectedFile();
                        if (file.exists()) {
                            java.awt.Toolkit.getDefaultToolkit().beep();
                            int res2 = JOptionPane.showInternalConfirmDialog(
                                Main.getDesktop(),
                                "Overwrite existing " + file.getName() + "?",
                                "Confirm", JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                            if (res2 == JOptionPane.CANCEL_OPTION || res2 == JOptionPane.CLOSED_OPTION)
                                return;
                            else if (res2 == JOptionPane.NO_OPTION)
                                continue;
                            else
                                break;
                        } else
                            break;
                    }
                    try {
                        FileUtils.saveTextFile(file, s);
                    } catch (IOException e) {
                        MessageBox.show("Saving " + file.getName()
                                                    + " failed.", e);
                    }
                    return;
                }
            }

            TextEditorFrame tef = new TextEditorFrame(title, s, true, true);
            tef.setParent(browser);
            tef.showStaggered();
        }
    }


    protected abstract void duplicate();


    private void removeOrphans() {
        if (orphanage == null)
            return;
        Collection<BrowserNode> selection = browser.getSelectedNodes();
        for (BrowserNode bn : selection) {
            if (!(bn instanceof MyNode))
                continue;
            MyNode n = (MyNode) bn;
            if (n.parent != orphanage || n.edit != null || n.details != null)
                continue;
            orphanage.removeChild(n);
        }
    }

    protected class TableSpec {
        public String catalog;
        public String schema;
        public String type;
        public String name;
    }

    private void clearCache() {
        // Find out if unloading all table models would discard uncommitted
        // edits...
        int flags = checkDirty(rootNode);
        if (flags != 0) {
            String reason;
            if (flags == 1)
                reason =
                    "There are cached tables with uncommitted edits.\n"
                  + "If you clear the cache now those edits will be lost.\n";
            else if (flags == 2)
                reason =
                    "There are tables in the orphanage.\n"
                  + "Clearing the cache now will remove them.\n";
            else
                reason =
                    "There are cached tables with uncommitted edits,\n"
                  + "and there are tables in the orphanage.\n"
                  + "If you clear the cache now those edits and tables\n"
                  + "will be lost.\n";
            java.awt.Toolkit.getDefaultToolkit().beep();
            int res = JOptionPane.showInternalConfirmDialog(
                        Main.getDesktop(),
                        reason + "Go ahead anyway?",
                        "Confirm", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
            if (res == JOptionPane.CANCEL_OPTION || res == JOptionPane.CLOSED_OPTION)
                return;
        }
        unloadModels(rootNode);
    }

    /**
     * Recursively checks a tree for the presence of dirty models.
     * Returns 0 if none were found, 1 if uncommitted tables were found,
     * 2 if orphans were found, 3 if uncommitted tables and orphans were
     * found.
     */
    private int checkDirty(MyNode n) {
        int res = 0;
        if (n.table != null) {
            ResultSetTableModel model = n.table.getModel();
            if (model != null && model.isDirty())
                if (n.table.getQualifiedName().indexOf("...") == -1)
                    res |= 1;
                else
                    res |= 2;
        }
        Iterator<MyNode> iter = n.getChildren();
        if (iter != null)
            while (res != 3 && iter.hasNext())
                res |= checkDirty(iter.next());
        return res;
    }

    private void unloadModels(MyNode n) {
        Iterator<MyNode> iter = n.getChildren();
        ArrayList<MyNode> kidsToRemove = new ArrayList<MyNode>();
        if (iter != null)
            while (iter.hasNext()) {
                MyNode child = iter.next();
                if (child.isLeaf()) {
                    if (child.table != null) {
                        if (child.table.getQualifiedName().indexOf("...")
                                                                    != -1) {
                            // Orphan: if no TableFrame nor TableDetailsFrame
                            // is pointed at it, delete it completely.
                            if (n.edit == null && n.details == null)
                                kidsToRemove.add(child);
                        } else {
                            // Regular table: if watched by a TableFrame,
                            // leave alone; if watched by a TableDetailsFrame,
                            // unload the model; if not watched at all,
                            // nuke the Table object.
                            if (n.edit == null)
                                if (n.details == null) {
                                    // Unloading the model is necessary, even
                                    // though I am nulling the reference in
                                    // the tree, because JavaScript may still
                                    // hold a reference to the Table object.
                                    child.table.unloadModel();
                                    child.table = null;
                                } else
                                    child.table.unloadModel();
                        }
                    }
                } else
                    unloadModels(child);
            }
        for (MyNode child : kidsToRemove)
            n.removeChild(child);
    }

    private void reloadTree() {
        Main.log(3, "BasicDatabase.reloadTree()");
        Collection<TableSpec> tables;
        try {
            tables = getTables();
        } catch (NavigatorException e) {
            MessageBox.show("Could not get table list.", e);
            return;
        }
        rootNode.markDeadRecursively();
        for (TableSpec ts : tables) {
            Main.log(3, "catalog=\"" + ts.catalog + "\" schema=\"" + ts.schema + "\" type=\"" + ts.type + "\" name=\"" + ts.name + "\"");
            MyNode n = rootNode;

            if (showCatalogs()) {
                MyNode catalogNode = n.getChildNamed(ts.catalog);
                if (catalogNode == null) {
                    catalogNode = new MyNode(ts.catalog);
                    n.addChild(catalogNode);
                } else
                    catalogNode.dead = false;
                n = catalogNode;
            }

            if (showSchemas()) {
                MyNode schemaNode = n.getChildNamed(ts.schema);
                if (schemaNode == null) {
                    schemaNode = new MyNode(ts.schema);
                    n.addChild(schemaNode);
                } else
                    schemaNode.dead = false;
                n = schemaNode;
            }

            if (showTableTypes()) {
                MyNode tableTypeNode = n.getChildNamed(ts.type);
                if (tableTypeNode == null) {
                    tableTypeNode = new MyNode(ts.type);
                    n.addChild(tableTypeNode);
                } else
                    tableTypeNode.dead = false;
                n = tableTypeNode;
            }

            MyNode tableNode = n.getChildNamed(ts.name);
            if (tableNode == null) {
                tableNode = new MyNode(ts.name);
                tableNode.qualifiedName = makeQualifiedName(ts.catalog,
                                                        ts.schema, ts.name);
                n.addChild(tableNode);
            } else
                tableNode.dead = false;
        }

        ArrayList<String> newOrphans = new ArrayList<String>();
        boolean orphanageDidNotExistYet = orphanage == null;
        rootNode.reapRecursively(newOrphans);
        if (orphanageDidNotExistYet && orphanage != null)
            rootNode.addChild(orphanage);

        if (newOrphans.size() > 0) {
            StringBuffer buf = new StringBuffer();
            if (newOrphans.size() == 1)
                buf.append("The table\n\n");
            else
                buf.append("The tables\n\n");
            Collections.sort(newOrphans, String.CASE_INSENSITIVE_ORDER);
            for (String orphanName : newOrphans) {
                buf.append("    ");
                buf.append(orphanName);
                buf.append("\n");
            }
            if (newOrphans.size() == 1)
                buf.append(
                    "\nhas uncommitted edits, but has disappeared from"
                  + "\nthe database. It has been moved to the \"orphanage\""
                  + "\nfolder in the browser's tree view."
                  + "\nPlease be sure to save any important data from this"
                  + "\ntable, by copying it to a table that is still in the"
                  + "\ndatabase, or by exporting it to a CSV file, before"
                  + "\nremoving it from the orphanage or before quitting"
                  + "\nJDBC Navigator.");
            else
                buf.append(
                    "\nhave uncommitted edits, but have disappeared from"
                  + "\nthe database. They have been moved to the \"orphanage\""
                  + "\nfolder in the browser's tree view."
                  + "\nPlease be sure to save any important data from these"
                  + "\ntables, by copying it to tables that are still in the"
                  + "\ndatabase, or by exporting it to CSV files, before"
                  + "\nremoving them from the orphanage or before quitting"
                  + "\nJDBC Navigator.");
            JOptionPane.showInternalMessageDialog(Main.getDesktop(),
                                                  buf.toString());
        }
    }

    /**
     * This method is called whenever a "Refresh" operation removes a table
     * node from the tree model. If it returns 'true', the table is renamed
     * and placed in the orphanage.
     */
    protected abstract boolean shouldMoveToOrphanage(Table table);

    private void moveToOrphanage(Table table) {
        if (orphanage == null)
            orphanage = new MyNode(ORPHANAGE);
        table.makeOrphan();
        while (orphanage.getChildNamed(table.getName()) != null)
            table.tryNextOrphanName();
        MyNode n = new MyNode(table.getName());
        n.qualifiedName = table.getQualifiedName();
        n.table = table;
        orphanage.addChild(n);
    }


    protected abstract Collection<TableSpec> getTables() throws NavigatorException;

    public TableFrame showTableFrame(String qualifiedName) {
        MyNode n = findTableNode(qualifiedName);
        if (n == null) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            return null;
        }
        if (n.edit != null) {
            n.edit.deiconifyAndRaise();
            n.show();
            return n.edit;
        }
        try {
            n.edit = new TableFrame(n.getTable(), browser);
            n.edit.setParent(browser);
            n.edit.showStaggered();
            n.show();
            return n.edit;
        } catch (NavigatorException e) {
            MessageBox.show("Can't open Table window", e);
            return null;
        }
    }

    public TableDetailsFrame showTableDetailsFrame(String qualifiedName) {
        MyNode n = findTableNode(qualifiedName);
        if (n == null) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            return null;
        }
        if (n.details != null) {
            n.details.deiconifyAndRaise();
            n.show();
            return n.details;
        }
        try {
            n.details = new TableDetailsFrame(n.getTable(), browser);
            n.details.setParent(browser);
            n.details.showStaggered();
            n.show();
            return n.details;
        } catch (NavigatorException e) {
            MessageBox.show("Can't open Table Details window", e);
            return null;
        }
    }

    public void tableFrameClosed(String qualifiedName) {
        MyNode n = findTableNode(qualifiedName);
        if (n != null)
            n.edit = null;
    }

    public void tableDetailsFrameClosed(String qualifiedName) {
        MyNode n = findTableNode(qualifiedName);
        if (n != null)
            n.details = null;
    }

    public Table getTable(String qualifiedName) throws NavigatorException {
        MyNode n = findTableNode(qualifiedName);
        if (n == null)
            throw new NavigatorException(
                "The table " + qualifiedName + " was not found.");
        else
            return n.getTable();
    }

    public boolean hasOrphans() {
        if (orphanage == null)
            return false;
        Iterator<MyNode> iter = orphanage.getChildren();
        return iter != null && iter.hasNext();
    }

    public Collection<Table> getSelectedTables() throws NavigatorException {
        Collection<BrowserNode> nodes = browser.getSelectedNodes();
        ArrayList<Table> tables = new ArrayList<Table>();
        for (BrowserNode bn : nodes) {
            if (bn instanceof MyNode) {
                MyNode n = (MyNode) bn;
                if (n.isLeaf())
                    tables.add(n.getTable());
            }
        }
        return tables;
    }

    protected String getIdentifierQuoteString() {
        return " ";
    }

    private static Set<String> SQL_WORDS;

    static {
        SQL_WORDS = new HashSet<String>();
        String[] w = new String[] {
            "ABORT", "ALL", "ALLOCATE", "ANALYSE", "ANALYZE", "AND",
            "ANY", "AS", "ASC", "BETWEEN", "BINARY", "BIT", "BOTH",
            "CASE", "CAST", "CHAR", "CHARACTER", "CHECK", "CLUSTER",
            "COALESCE", "COLLATE", "COLLATION", "COLUMN", "CONSTRAINT",
            "COPY", "CROSS", "CURRENT", "CURRENT_CATALOG", "CURRENT_DATE",
            "CURRENT_DB", "CURRENT_SCHEMA", "CURRENT_SID", "CURRENT_TIME",
            "CURRENT_TIMESTAMP", "CURRENT_USER", "CURRENT_USERID",
            "CURRENT_USEROID", "DEALLOCATE", "DEC", "DECIMAL", "DECODE",
            "DEFAULT", "DESC", "DISTINCT", "DISTRIBUTE", "DO", "ELSE", "END",
            "EXCEPT", "EXCLUDE", "EXISTS", "EXPLAIN", "EXPRESS", "EXTEND",
            "EXTERNAL", "EXTRACT", "FALSE", "FIRST", "FLOAT", "FOLLOWING",
            "FOR", "FOREIGN", "FROM", "FULL", "FUNCTION", "GENSTATS",
            "GLOBAL", "GROUP", "HAVING", "IDENTIFIER_CASE", "ILIKE", "IN",
            "INDEX", "INITIALLY", "INNER", "INOUT", "INTERSECT", "INTERVAL",
            "INTO", "KEY", "LEADING", "LEFT", "LIKE", "LIMIT", "LOAD",
            "LOCAL", "LOCK", "MINUS", "MOVE", "NATURAL", "NCHAR", "NEW",
            "NOT", "NOTNULL", "NULL", "NULLS", "NUMERIC", "NVL", "NVL2",
            "OFF", "OFFSET", "OLD", "ON", "ONLINE", "ONLY", "OR", "ORDER",
            "OTHERS", "OUT", "OUTER", "OVER", "OVERLAPS", "PARTITION",
            "POSITION", "PRECEDING", "PRECISION", "PRESERVE", "PRIMARY",
            "RESET", "RESET", "REUSE", "REUSE", "RIGHT", "ROWS", "SELECT",
            "SESSION_USER", "SETOF", "SHOW", "SOME", "TABLE", "THEN",
            "TIES", "TIME", "TIMESTAMP", "TO", "TRAILING", "TRANSACTION",
            "TRIGGER", "TRIM", "TRUE", "UNBOUNDED", "UNION", "UNIQUE",
            "USER", "USING", "VACUUM", "VARCHAR", "VERBOSE", "VERSION",
            "VIEW", "WHEN", "WHERE", "WITH", "WRITE"
        };
        for (String s : w)
            SQL_WORDS.add(s);
    }

    public String quote(String s) {
        if (s == null)
            return null;
        String q = getIdentifierQuoteString();
        if (q.equals(" "))
            return s;
        else if (SQL_WORDS.contains(s.toUpperCase()))
            return q + s + q;
        else {
            int sl = s.length();
            for (int i = 0; i < sl; i++) {
                char c = s.charAt(i);
                if (!((c >= 'A' && c <= 'Z')
                            || (c >= 'a' && c <= 'z')
                            || (c >= '0' && c <= '9')
                            || c == '_'))
                    return q + s + q;
            }
            return s;
        }
    }

    public String unquote(String s) {
        if (s == null)
            return null;
        String q = getIdentifierQuoteString();
        if (q.equals(" "))
            return s;
        else {
            int ql = q.length();
            if (s.length() > 2 * ql
                    && s.substring(0, ql).equals(q)
                    && s.substring(s.length() - ql).equals(q))
                return s.substring(ql, s.length() - ql);
            else
                return s;
        }
    }

    /**
     * Generate a name that represents a table uniquely to the RDBMS, i.e.
     * something that can be used in SQL scripts and is guaranteed not to
     * access the wrong table.
     */
    public String makeQualifiedName(String catalog, String schema,
                                       String name) {
        catalog = showCatalogs() ? quote(catalog) : null;
        schema = showSchemas() ? quote(schema) : null;
        name = quote(name);
        if (catalog != null)
            if (schema != null)
                return catalog + "." + schema + "." + name;
            else
                return catalog + "." + name;
        else if (schema != null)
            return schema + "." + name;
        else
            return name;
    }

    public String[] parseQualifiedName(String qualifiedName) {
        ArrayList<String> al = new ArrayList<String>();
        String q = getIdentifierQuoteString();
        if (q.equals(" ")) {
            StringTokenizer tok = new StringTokenizer(qualifiedName, ".");
            while (tok.hasMoreTokens())
                al.add(tok.nextToken());
        } else {
            int ql = q.length();
            boolean inQuotes = false;
            StringBuffer buf = new StringBuffer(qualifiedName);
            int i = 0;
            while (i <= buf.length() - ql) {
                if (buf.substring(i, i + ql).equals(q)) {
                    buf.delete(i, i + ql);
                    inQuotes = !inQuotes;
                } else if (!inQuotes && buf.charAt(i) == '.') {
                    al.add(buf.substring(0, i));
                    buf.delete(0, i + 1);
                    i = 0;
                } else
                    i++;
            }
            al.add(buf.toString());
        }

        int i = al.size();
        String name, schema, catalog;
        if (i > 0)
            name = al.get(--i);
        else
            name = null;
        if (showSchemas() && i > 0)
            schema = al.get(--i);
        else
            schema = null;
        if (showCatalogs() && i > 0)
            catalog = al.get(--i);
        else
            catalog = null;

        return new String[] { catalog, schema, name };
    }

    protected abstract boolean showCatalogs();

    protected abstract boolean showSchemas();

    protected abstract boolean showTableTypes();

    private MyNode findTableNode(String qualifiedName) {
        Main.log(3, "BasicDatabase.findTableNode(\"" + qualifiedName + "\")");
        int dots;
        if ((dots = qualifiedName.indexOf("...")) != -1) {
            // Names containing three consecutive dots are used to distinguish
            // orphans (tables that exist in memory but have been deleted from
            // the database).

            if (orphanage != null) {
                String name = qualifiedName.substring(dots + 3);
                MyNode orphan = orphanage.getChildNamed(name);
                if (orphan != null)
                    return orphan;
            }
            return null;
        }

        String[] parts = parseQualifiedName(qualifiedName);
        String catalog = parts[0];
        String schema = parts[1];
        String name = parts[2];

        if (showCatalogs() && catalog == null
                || showSchemas() && schema == null) {
            // We have an unqualified table name, presumably the result of the
            // user executing a query in SQLFrame. We do a brute-force scan of
            // the tree.
            return findTableNodeBruteForce(catalog, schema, name, rootNode);
        }

        MyNode dn = rootNode;
        if (showCatalogs()) {
            dn = dn.getChildNamed(catalog);
            if (dn == null)
                return null;
        }
        if (showSchemas()) {
            dn = dn.getChildNamed(schema);
            if (dn == null)
                return null;
        }
        if (showTableTypes()) {
            Iterator<MyNode> iter = dn.getChildren();
            while (iter.hasNext()) {
                MyNode dn2 = iter.next();
                MyNode tn = dn2.getChildNamed(name);
                if (tn != null)
                    return tn;
            }
            return null;
        } else
            return dn.getChildNamed(name);
    }

    private MyNode findTableNodeBruteForce(String catalog, String schema,
                                            String name, MyNode n) {
        for (Iterator<MyNode> iter = n.getChildren(); iter.hasNext();) {
            MyNode c = iter.next();
            if (c.isLeaf()) {
                if (!c.name.equalsIgnoreCase(name))
                    continue;
                if (catalog == null && schema == null)
                    return c;
                String[] parts = parseQualifiedName(c.qualifiedName);
                if (catalog != null && !catalog.equalsIgnoreCase(parts[0]))
                    continue;
                if (schema != null && !schema.equalsIgnoreCase(parts[1]))
                    continue;
                return c;
            } else {
                MyNode cc = findTableNodeBruteForce(catalog, schema, name, c);
                if (cc != null)
                    return cc;
            }
        }
        return null;
    }

    protected String getSynonymTarget(String qualifiedName) {
        return null;
    }

    private static final Comparator<String> myStringComparator =
            new Comparator<String>() {
                // ORPHANAGE comes last, null comes last but before
                // ORPHANAGE, and everything else is case insensitive
                // alphabetical.
                public int compare(String a, String b) {
                    return a == ORPHANAGE
                                ? b == ORPHANAGE
                                    ? 0
                                    : 1
                                : b == ORPHANAGE
                                    ? -1
                                    : a == null
                                        ? b == null
                                            ? 0
                                            : 1
                                        : b == null
                                            ? -1
                                            : a.compareToIgnoreCase(b);
                }
            };

    private class MyNode implements BrowserNode {
        // Common stuff
        public MyNode parent;
        public DisplayNode displayNode;
        public String name;
        public boolean dead;
        // Node stuff
        public TreeMap<String, MyNode> children;
        // Leaf stuff
        public String qualifiedName;
        public Table table;
        public TableFrame edit;
        public TableDetailsFrame details;

        public MyNode(String name) {
            this.name = name;
            dead = false;
        }

        public String getName() {
            return name == null ? "(null)" : name;
        }

        public BrowserNode getParent() {
            return parent;
        }

        public boolean isLeaf() {
            return qualifiedName != null;
        }

        public MyNode getTarget() {
            String name = getSynonymTarget(qualifiedName);
            return name == null ? null : findTableNode(name);
        }

        public Table getTable() throws NavigatorException {
            if (qualifiedName == null)
                return null;
            if (table == null)
                table = loadTable(qualifiedName);
            return table;
        }

        public Iterator<MyNode> getChildren() {
            if (children == null)
                return null;
            else
                return children.values().iterator();
        }

        public void setDisplayNode(DisplayNode displayNode) {
            this.displayNode = displayNode;
        }

        public void show() {
            if (displayNode != null)
                displayNode.show();
        }

        public boolean busy() {
            if (children == null)
                return edit != null || details != null;
            for (MyNode n : children.values())
                if (n.busy())
                    return true;
            return false;
        }

        // "Private" implementation stuff

        public void addChild(MyNode child) {
            dead = false;
            child.parent = this;
            if (children == null)
                children = new TreeMap<String, MyNode>(myStringComparator);
            children.put(child.name, child);
            if (displayNode != null) {
                // Aargh. The keys in the TreeMap are sorted! Isn't there
                // an efficient way to find a key's index within the key
                // set? (Good thing this code does not get executed when the
                // tree is first built!)
                int index = 0;
                for (MyNode n : children.values()) {
                    if (child == n) {
                        displayNode.childAddedAt(index, child);
                        break;
                    }
                    index++;
                }
            }
        }

        public void removeChild(MyNode child) {
            if (displayNode != null) {
                // Aargh. The keys in the TreeMap are sorted! Isn't there
                // an efficient way to find a key's index within the key
                // set?
                int index = 0;
                for (MyNode n : children.values()) {
                    if (child == n) {
                        displayNode.childRemovedAt(index);
                        break;
                    }
                    index++;
                }
            }
            children.remove(child.getName());
        }

        public MyNode getChildNamed(String name) {
            if (children == null)
                return null;
            else
                return children.get(name);
        }

        public void markDeadRecursively() {
            if (name == ORPHANAGE)
                return;
            if (edit == null && details == null)
                dead = true;
            if (children != null)
                for (MyNode n : children.values())
                    n.markDeadRecursively();
        }

        public void reapRecursively(ArrayList<String> newOrphans) {
            if (children != null) {
                int index = 0;
                ArrayList<String> removed = null;
                for (Map.Entry<String, MyNode> entry : children.entrySet()) {
                    MyNode child = entry.getValue();
                    child.reapRecursively(newOrphans);
                    if (child.dead) {
                        if (displayNode != null)
                            displayNode.childRemovedAt(index);
                        // Adding the child to be removed to a list; we'll do
                        // the actual removal *after* this loop, since the
                        // Iterators returned by TreeMap do not support calling
                        // next() again after remove().
                        if (removed == null)
                            removed = new ArrayList<String>();
                        removed.add(entry.getKey());
                        if (child.table != null)
                            if (shouldMoveToOrphanage(child.table)) {
                                newOrphans.add(child.table.getQualifiedName());
                                moveToOrphanage(child.table);
                            }
                    } else {
                        // If I have a live child, I should survive, too
                        dead = false;
                        index++;
                    }
                }
                if (removed != null)
                    for (String childName : removed)
                        children.remove(childName);
            }
        }
    }

    public String objectToString(TypeSpec spec, Object o) {
        if (o == null)
            return null;

        if (spec.type == TypeSpec.DATE
                || spec.type == TypeSpec.TIME
                || spec.type == TypeSpec.TIME_TZ
                || spec.type == TypeSpec.TIMESTAMP
                || spec.type == TypeSpec.TIMESTAMP_TZ)
            return ((DateTime) o).toString(spec);

        if (spec.type == TypeSpec.INTERVAL_DS
                || spec.type == TypeSpec.INTERVAL_YM
                || spec.type == TypeSpec.INTERVAL_YS)
            return ((Interval) o).toString(spec);
    
        Class<?> klass = spec.jdbcJavaClass;

        if (java.sql.Date.class.isAssignableFrom(klass)
                || java.sql.Time.class.isAssignableFrom(klass)
                || java.sql.Timestamp.class.isAssignableFrom(klass))
            // These three classes are subclasses of java.util.Date, but
            // java.util.Date itself needs to be handled differently,
            // hence this special case
            return o.toString();

        if (java.util.Date.class.isAssignableFrom(klass)) {
            long time = ((java.util.Date) o).getTime();
            return new java.sql.Timestamp(time).toString();
        }

        if (java.sql.Blob.class.isAssignableFrom(klass)
                && !(o instanceof BlobWrapper)) {
            // Assuming byte[]; this can happen when a Blob value has been
            // edited in QueryResultFrame.
            klass = new byte[1].getClass();
        }

        if (klass == new byte[1].getClass()
                || spec.jdbcJavaType.equals("[B")
                || o instanceof byte[])
            return FileUtils.byteArrayToHex((byte[]) o);

        return o.toString();
    }

    public Object stringToObject(TypeSpec spec, String s) {
        if (s == null)
            return null;

        try {
            if (spec.type == TypeSpec.CHAR
                    || spec.type == TypeSpec.VARCHAR
                    || spec.type == TypeSpec.LONGVARCHAR
                    || spec.type == TypeSpec.NCHAR
                    || spec.type == TypeSpec.VARNCHAR
                    || spec.type == TypeSpec.LONGVARNCHAR)
                return s;
            if (spec.type == TypeSpec.DATE
                    || spec.type == TypeSpec.TIME
                    || spec.type == TypeSpec.TIME_TZ
                    || spec.type == TypeSpec.TIMESTAMP
                    || spec.type == TypeSpec.TIMESTAMP_TZ)
                return new DateTime(s);
            if (spec.type == TypeSpec.INTERVAL_DS
                    || spec.type == TypeSpec.INTERVAL_YM
                    || spec.type == TypeSpec.INTERVAL_YS)
                return new Interval(spec, s);

            Class<?> klass = spec.jdbcJavaClass;
            if (java.sql.Time.class.isAssignableFrom(klass))
                return java.sql.Time.valueOf(s);
            if (java.sql.Date.class.isAssignableFrom(klass))
                return java.sql.Date.valueOf(s);
            if (java.sql.Timestamp.class.isAssignableFrom(klass))
                return java.sql.Timestamp.valueOf(s);
            if (java.util.Date.class.isAssignableFrom(klass))
                return new java.util.Date(java.sql.Timestamp.valueOf(s).getTime());

            if (Boolean.class.isAssignableFrom(klass))
                if (s.equalsIgnoreCase("true"))
                    return Boolean.TRUE;
                else if (s.equalsIgnoreCase("false"))
                    return Boolean.FALSE;
                else
                    throw new NumberFormatException("Invalid boolean \"" + s + "\"");

            java.lang.reflect.Constructor<?> cnstr =
                        klass.getConstructor(new Class[] { String.class });
            return cnstr.newInstance(new Object[] { s });
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
