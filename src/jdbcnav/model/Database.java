///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2010  Thomas Okken
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

package jdbcnav.model;

import java.io.*;
import java.util.*;
import org.mozilla.javascript.*;

import jdbcnav.BrowserFrame;
import jdbcnav.TableFrame;
import jdbcnav.TableDetailsFrame;
import jdbcnav.util.NavigatorException;


public interface Database {
    interface OpenCallback {
        void databaseOpened(Database db);
    }
    void close();

    String getName();
    String getInternalDriverName();
    String about() throws NavigatorException;

    void setBrowser(BrowserFrame browser);
    BrowserFrame getBrowser();
    BrowserNode getRootNode();
    String[] getCommands();
    void executeCommand(int command);

    TableFrame showTableFrame(String qualifiedName);
    TableDetailsFrame showTableDetailsFrame(String qualifiedName);
    void tableFrameClosed(String qualifiedName);
    void tableDetailsFrameClosed(String qualifiedName);

    // NOTE: getTable() should return a Table that is consistent with
    // what is displayed on the screen; that is, it should search for
    // the table in the tree rooted at getRootNode(), and *not* fetch
    // the table from the back-end DB directly. It may use loadTable()
    // to load the table from the DB only if it was not found in the
    // tree.
    Table getTable(String qualifiedName) throws NavigatorException;

    // NOTE: loadTable() should construct a new Table using information
    // obtained directly from the back-end DB.
    Table loadTable(String qualifiedName) throws NavigatorException;

    File getFile();
    boolean save(File file) throws NavigatorException;

    boolean needsCommit();
    boolean hasOrphans();
    Collection<Table> getDirtyTables();
    void commitTables(Collection<Table> tables) throws NavigatorException;
    Collection<Table> getSelectedTables() throws NavigatorException;

    // runQuery() may return either a Data object or a Table, depending on
    // whether or not the output from the query represents an updatable view
    // of a table (meaning, the query output contains columns from one table
    // only, and all of that table's primary key columns are present).
    // Specify allowTable = false if you specifically want a Data object.
    Object runQuery(String query, boolean asynchronous, boolean allowTable)
                                                    throws NavigatorException;
    Object runQuery(String query, Object[] values) throws NavigatorException;

    int runUpdate(String query) throws NavigatorException;
    Scriptable createStatement() throws NavigatorException;
    Scriptable prepareStatement(String statement) throws NavigatorException;
    Scriptable prepareCall(String call) throws NavigatorException;

    // Utilities for quoting/unquoting identifiers
    String quote(String s);
    String unquote(String s);
    String makeQualifiedName(String catalog, String schema, String name);
    String[] parseQualifiedName(String qualifiedName);

    // TypeSpec support
    String objectToString(TypeSpec spec, Object o);
    Object stringToObject(TypeSpec spec, String s);
}
