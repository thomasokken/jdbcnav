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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import org.mozilla.javascript.*;

import jdbcnav.javascript.*;
import jdbcnav.model.*;
import jdbcnav.util.*;


public class JDBCDatabase extends BasicDatabase {

    private String name;
    private String internalDriverName;
    private String jdbcDriver;
    private String url;
    private String username;
    private String password;
    protected Connection con;
    private ArrayList<Table> editedTables = new ArrayList<Table>();
    private boolean caseSensitive = true;


    public static void open(Database.OpenCallback opencb) {
        LoginDialog.activate(opencb);
    }

    public JDBCDatabase(String name, String driver, Connection con) {
        this.name = name;
        this.internalDriverName = driver;
        this.con = con;
        
        ResultSet rs = null;
        try {
            DatabaseMetaData dbmd = con.getMetaData();
            rs = dbmd.getTypeInfo();
            while (rs.next()) {
                if (rs.getInt("DATA_TYPE") == Types.CHAR) {
                    caseSensitive = rs.getBoolean("CASE_SENSITIVE");
                    break;
                }
            }
        } catch (SQLException e) {
            //
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (SQLException e) {}
        }
    }

    private static JDBCDatabase create(String driver, String url, String username,
                                       String password, String name, Connection con) {
        String internalDriverName =
            InternalDriverMap.getDriverName(driver, con);
        String className =
            InternalDriverMap.getDatabaseClassName(internalDriverName);

        try {
            Class<?> klass = Class.forName(className);
            Constructor<?> cnstr = klass.getConstructor(
                new Class[] { String.class, String.class, Connection.class });
            JDBCDatabase db = (JDBCDatabase) cnstr.newInstance(
                new Object[] { name, internalDriverName, con });
            db.jdbcDriver = driver;
            db.url = url;
            db.username = username;
            db.password = password;
            return db;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            // Should never happen; we're loading a class that is part
            // of JDBCNavigator itself
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            // Should never happen; we're loading a class that is part
            // of JDBCNavigator itself, and if we've done our homework,
            // it will have a public constructor that takes a String and
            // a JDBC Connection
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            // Yawn. Should never happen. We wrote the class ourselves
            // and of course we made sure that the constructor is public.
        } catch (InstantiationException e) {
            e.printStackTrace();
            // Yawn. Should never happen. We wrote the class ourselves
            // and of course we made sure that the class is not abstract.
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            // Yawn. Should never happen. We wrote the class ourselves
            // and of course we made sure that the constructor throws no
            // Exceptions.
        }

        // *should* never get here
        return null;
    }

    public void reconnect() {
        try {
            try {
                con.close();
            } catch (SQLException e) {}
            con = doConnect(jdbcDriver, url, username, password);
        } catch (SQLException e) {
            MessageBox.show("Could not reconnect to the database.", e);
        }
    }

    public void close() {
        try {
            con.close();
        } catch (SQLException e) {
            MessageBox.show("An unexpected error occurred while "
                            + "closing the JDBC Connection", e);
        }
    }

    public String getName() {
        return name;
    }

    public final String getInternalDriverName() {
        return internalDriverName;
    }

    public String about() throws NavigatorException {
        try {
            DatabaseMetaData dbmd = con.getMetaData();
            StringBuffer buf = new StringBuffer();
            buf.append("Database product name: ");
            buf.append(dbmd.getDatabaseProductName());
            buf.append("\nDatabase product version: ");
            buf.append(dbmd.getDatabaseProductVersion());
            buf.append("\nJDBC driver name: ");
            buf.append(dbmd.getDriverName());
            buf.append("\nJDBC driver version: ");
            buf.append(dbmd.getDriverVersion());
            buf.append("\nJDBC Navigator Internal Driver: ");
            buf.append(getInternalDriverName());
            return buf.toString();
        } catch (SQLException e) {
            throw new NavigatorException(e);
        }
    }

    public void setBrowser(BrowserFrame browser) {
        this.browser = browser;
    }

    public BrowserFrame getBrowser() {
        return browser;
    }

    public boolean needsCommit() {
        for (Table table : editedTables)
            if (table.needsCommit())
                return true;
        return false;
    }

    public Collection<Table> getDirtyTables() {
        ArrayList<Table> dirty = new ArrayList<Table>();
        for (Table table : editedTables)
            if (table.needsCommit())
                dirty.add(table);
        Collections.sort(dirty);
        return dirty;
    }

    protected boolean shouldMoveToOrphanage(Table table) {
        for (Iterator<Table> iter = editedTables.iterator(); iter.hasNext();) {
            Table t = iter.next();
            if (table == t) {
                iter.remove();
                return table.needsCommit();
            }
        }
        return false;
    }

    public void commitTables(Collection<Table> tables) throws NavigatorException {
        boolean autoCommit = true;
        NavigatorException we = null;
        DiffCallback dcb = new DiffCallback();

        // Pause any in-progress table loads before committing; without
        // this precaution, MultiTableDiff.diff() could try to insert
        // spurious rows (this will happen if rows are loaded into a table
        // between the moment that the 'original' and the 'current' snapshots
        // are made (also note that the moment the OriginalTable object is
        // constructed is *not* the moment the snapshot is made; that object
        // is merely a vehicle for a reference to the model's 'original'
        // array)).

        ArrayList<Table> pausedTables = new ArrayList<Table>();
        for (Table table : tables) {
            ResultSetTableModel model = table.getModel();
            if (model.getState() == Data.LOADING) {
                model.setState(Data.PAUSED);
                pausedTables.add(table);
            }
        }

        try {
            autoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            if (tables.size() == 1) {
                Table table = tables.iterator().next();
                table.getModel().commit(dcb);
            } else {
                ArrayList<Table> oldtables = new ArrayList<Table>();
                for (Table t : tables)
                    oldtables.add(new OriginalTable(t));
                MultiTableDiff.diff(dcb, oldtables, tables, false);
            }
            con.commit();
        } catch (SQLException e) {
            we = new NavigatorException(e);
        } catch (Exception e) {
            we = new NavigatorException("An unexpected exception occurred!", e);
        }

        dcb.cleanup();
        for (Table table : pausedTables) {
            ResultSetTableModel model = table.getModel();
            model.setState(Data.LOADING);
        }

        if (we != null) {
            try {
                con.rollback();
            } catch (SQLException e) {}
            if (autoCommit)
                try {
                    con.setAutoCommit(true);
                } catch (SQLException e) {}
            throw new NavigatorException("Commit failed!", we);
        }

        if (autoCommit)
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {}

        // All went well; tell all the table models to discard their 'undo'
        // state.
        for (Table t : tables)
            t.getModel().postCommit();
    }

    private class OriginalTable extends BasicTable {
        private Data data;

        public OriginalTable(Table table) {
            super(table);
            // If the following line throws a NullPointerException, I screwed
            // up somewhere. The fact that we're here should imply that
            // 'table' was edited, so it should have a model. Note that I'm
            // NOT calling createModel(): if there is no model, I *want* to
            // get an exception; creating a model on the fly would only mask
            // a pretty serious error.
            data = table.getModel().getOriginalData();
        }

        public ResultSetTableModel createModel() throws NavigatorException {
            throw new NavigatorException("OriginalTable can't be edited!");
        }

        public boolean isEditable() {
            return false;
        }

        public Database getDatabase() {
            return JDBCDatabase.this;
        }

        public Data getData(boolean async) {
            return data;
        }
    }

    protected class PartialTable extends BasicTable {
        private String query;
        private Object[] values;
        private Data data;

        public PartialTable(String query, Object[] values, Table t, Data data) {
            // We leave some fields unpopulated -- they should never be
            // needed because all we're going to be used for is to be
            // displayed in TableFrame, and Database.commitTables().

            catalog = t.getCatalog();
            schema = t.getSchema();
            name = t.getName();
            qualifiedName = t.getQualifiedName();
            type = t.getType();
            remarks = "This view was generated by the query:\n";
            if (values == null) {
                remarks += query;
            } else {
                int idx = 0;
                for (StringTokenizer tok = new StringTokenizer(query, "?", true); tok.hasMoreTokens();) {
                    String tk = tok.nextToken();
                    if (tk.equals("?") && idx < values.length) {
                        Object o = values[idx++];
                        if (o == null)
                            remarks += "null";
                        else if (Number.class.isAssignableFrom(o.getClass()))
                            remarks += o;
                        else
                            remarks += "'" + o.toString().replace("'", "''") + "'";
                    } else {
                        remarks += tk;
                    }
                }
            }

            int columns = data.getColumnCount();
            int tColumns = t.getColumnCount();

            int[] columnMap = new int[columns];
            int[] reverseColumnMap = new int[tColumns];
            for (int i = 0; i < tColumns; i++)
                reverseColumnMap[i] = -1;

            columnNames = new String[columns];
            typeSpecs = new TypeSpec[columns];
            isNullable = new String[columns];
            isGenerated = new String[columns];

            for (int i = 0; i < columns; i++) {
                String colname = data.getColumnName(i);
                for (int j = 0; j < tColumns; j++) {
                    String tColname = t.getColumnNames()[j];
                    if (colname.equalsIgnoreCase(tColname)) {
                        columnMap[i] = j;
                        reverseColumnMap[j] = i;
                        columnNames[i] = t.getColumnNames()[j];
                        typeSpecs[i] = t.getTypeSpecs()[j];
                        isNullable[i] = t.getIsNullable()[j];
                        isGenerated[i] = t.getIsGenerated()[j];
                    }
                }
            }

            /*
               // Note to self --
               // Why did I go to all this trouble to clone the original
               // table's primary key, instead of simply copying its reference?
               // Also, why did I not copy the original table's imported and
               // exported keys all along? These things really are pretty nice
               // to have, to make the pointer-chasing functionality work in
               // PartialTables (a.k.a. updatable query results).

            PrimaryKey tpk = t.getPrimaryKey();
            if (tpk != null) {
                BasicPrimaryKey bpk = new BasicPrimaryKey();
                bpk.setName("fake_key");
                int pklength = tpk.getColumnCount();
                String[] pkcol = new String[pklength];
                for (int i = 0; i < pklength; i++)
                    pkcol[i] = tpk.getColumnName(i);
                bpk.setColumns(pkcol);
                pk = bpk;
            }

            fks = new ForeignKey[0];
            rks = new ForeignKey[0];
            indexes = new Index[0];
            */

            pk = t.getPrimaryKey();
            fks = t.getForeignKeys();
            rks = t.getReferencingKeys();
            indexes = new Index[0];

            this.data = data;
            this.query = query;
            this.values = values;
        }

        public boolean isEditable() {
            return true;
        }

        public boolean isUpdatableQueryResult() {
            return true;
        }

        public Database getDatabase() {
            return JDBCDatabase.this;
        }

        public Data getData(boolean async) throws NavigatorException {
            // The first time getData() is called, we return the Data
            // object that was passed to our constructor; all following
            // times, we re-run the query.
            if (data != null) {
                Data ret = data;
                data = null;
                return ret;
            } else if (values == null)
                return (Data) runQuery(query, async, false);
            else
                return (Data) runQuery(query, values, async, false);
        }
    }


    private class DiffCallback implements TableChangeHandler {
        private TreeMap<Table, PreparedStatement> insertStatements = new TreeMap<Table, PreparedStatement>();
        private TreeMap<Table, PreparedStatement> deleteStatements = new TreeMap<Table, PreparedStatement>();

        public DiffCallback() {
            //
        }

        public void insertRow(Table table, Object[] row)
                                                    throws NavigatorException {
            PreparedStatement insertStatement = insertStatements.get(table);
            String[] names = table.getColumnNames();
            String[] isGenerated = table.getIsGenerated();
            int columns = names.length;
            if (insertStatement == null) {
                StringBuffer buf = new StringBuffer();
                buf.append("insert into ");
                buf.append(table.getQualifiedName());
                buf.append("(");
                for (int i = 0; i < columns; i++) {
                    if ("YES".equals(isGenerated[i]))
                        continue;
                    if (i > 0)
                        buf.append(", ");
                    buf.append(quote(names[i]));
                }
                buf.append(") values (");
                for (int i = 0; i < columns; i++) {
                    if ("YES".equals(isGenerated[i]))
                        continue;
                    if (i == 0)
                        buf.append("?");
                    else
                        buf.append(", ?");
                }
                buf.append(")");
                try {
                    insertStatement = con.prepareStatement(buf.toString());
                    insertStatements.put(table, insertStatement);
                } catch (SQLException e) {
                    throw new NavigatorException(e);
                }
            }
            try {
                TypeSpec[] specs = table.getTypeSpecs();
                int n = 1;
                for (int i = 0; i < columns; i++) {
                    if ("YES".equals(isGenerated[i]))
                        continue;
                    Object o = nav2db(specs[i], row[i]);
                    setObject(insertStatement, n++, i, o, table);
                }
                insertStatement.executeUpdate();
            } catch (SQLException e) {
                throw new NavigatorException(e);
            }
        }

        public void deleteRow(Table table, Object[] key)
                                                    throws NavigatorException {
            String[] names = table.getColumnNames();
            int[] keyIndexes = table.getPKColumns();
            if (needsIsNull() && table.getPrimaryKey() == null) {
                StringBuffer buf = new StringBuffer();
                buf.append("delete from ");
                buf.append(table.getQualifiedName());
                buf.append(" where ");
                for (int i = 0; i < key.length; i++) {
                    if (i > 0)
                        buf.append(" and ");
                    buf.append(quote(names[keyIndexes[i]]));
                    if (key[i] == null)
                        buf.append(" is null");
                    else
                        buf.append(" = ?");
                }
                PreparedStatement s = null;
                try {
                    s = con.prepareStatement(buf.toString());
                    TypeSpec[] specs = table.getTypeSpecs();
                    int p = 1;
                    for (int i = 0; i < key.length; i++)
                        if (key[i] != null) {
                            Object o = nav2db(specs[keyIndexes[i]], key[i]);
                            setObject(s, p++, i, o, table);
                        }
                    s.executeUpdate();
                } catch (SQLException e) {
                    throw new NavigatorException(e);
                } finally {
                    if (s != null)
                        try {
                            s.close();
                        } catch (SQLException e) {}
                }
            } else {
                PreparedStatement deleteStatement = deleteStatements.get(table);
                if (deleteStatement == null) {
                    StringBuffer buf = new StringBuffer();
                    buf.append("delete from ");
                    buf.append(table.getQualifiedName());
                    buf.append(" where ");
                    for (int i = 0; i < key.length; i++) {
                        if (i > 0)
                            buf.append(" and ");
                        buf.append(quote(names[keyIndexes[i]]));
                        buf.append(" = ?");
                    }
                    try {
                        deleteStatement = con.prepareStatement(buf.toString());
                        deleteStatements.put(table, deleteStatement);
                    } catch (SQLException e) {
                        throw new NavigatorException(e);
                    }
                }
                try {
                    TypeSpec[] specs = table.getTypeSpecs();
                    for (int i = 0; i < key.length; i++) {
                        Object o = nav2db(specs[keyIndexes[i]], key[i]);
                        setObject(deleteStatement, i + 1, i, o, table);
                    }
                    deleteStatement.executeUpdate();
                } catch (SQLException e) {
                    throw new NavigatorException(e);
                }
            }
        }

        public void updateRow(Table table, Object[] oldRow, Object[] newRow)
                                                    throws NavigatorException {
            String[] names = table.getColumnNames();
            String[] isGenerated = table.getIsGenerated();
            int[] keyIndexes = table.getPKColumns();
            int columns = names.length;
            StringBuffer buf = new StringBuffer();
            buf.append("update ");
            buf.append(table.getQualifiedName());
            buf.append(" set ");
            boolean first = true;
            for (int i = 0; i < columns; i++) {
                if ("YES".equals(isGenerated[i]))
                    continue;
                if (oldRow[i] == null ? newRow[i] != null
                                    : !oldRow[i].equals(newRow[i])) {
                    if (first)
                        first = false;
                    else
                        buf.append(", ");
                    buf.append(quote(names[i]));
                    buf.append(" = ?");
                }
            }
            buf.append(" where ");
            for (int i = 0; i < keyIndexes.length; i++) {
                if (i > 0)
                    buf.append(" and ");
                buf.append(quote(names[keyIndexes[i]]));

                // I'm doing the hack to work around the Oracle bug in '= ?'
                // with null values in where clauses here. Unlike in deleteRow,
                // the hack is unconditional here, since caching update
                // statements is a bit trickier than I am in the mood for right
                // now. (The problem is that, unlike in insertRow and
                // deleteRow, you don't want to specify the same columns for
                // each updated row... For example, if only one value was
                // changed in a table row that contains 1000 values, you don't
                // want to pump the 999 unchanged values across the wire.
                // So, instead, I waste time building new statements for each
                // update. This *might* actually perform *worse*...)

                if (oldRow[keyIndexes[i]] == null)
                    buf.append(" is null");
                else
                    buf.append(" = ?");
            }
            PreparedStatement s = null;
            try {
                s = con.prepareStatement(buf.toString());
                TypeSpec[] specs = table.getTypeSpecs();
                int p = 1;
                for (int i = 0; i < columns; i++) {
                    if ("YES".equals(isGenerated[i]))
                        continue;
                    if (oldRow[i] == null ? newRow[i] != null
                                        : !oldRow[i].equals(newRow[i])) {
                        Object o = nav2db(specs[i], newRow[i]);
                        setObject(s, p++, i, o, table);
                    }
                }
                for (int i = 0; i < keyIndexes.length; i++) {
                    int idx = keyIndexes[i];
                    if (oldRow[idx] != null) {
                        Object o = nav2db(specs[idx], oldRow[idx]);
                        setObject(s, p++, idx, o, table);
                    }
                }
                s.executeUpdate();
            } catch (SQLException e) {
                throw new NavigatorException(e);
            } finally {
                if (s != null)
                    try {
                        s.close();
                    } catch (SQLException e) {}
            }
        }

        public boolean continueAfterError() {
            return false;
        }

        public void cleanup() {
            for (PreparedStatement pstmt : insertStatements.values())
                try {
                    pstmt.close();
                } catch (SQLException e) {}
            for (PreparedStatement pstmt : deleteStatements.values())
                try {
                    pstmt.close();
                } catch (SQLException e) {}
        }
    }


    protected void duplicate() {
        try {
            FileDatabase fd = new FileDatabase(getSelectedTables());
            BrowserFrame bf = new BrowserFrame(fd);
            bf.showStaggered();
        } catch (NavigatorException e) {
            MessageBox.show(e);
        }
    }

    protected Collection<TableSpec> getTables() throws NavigatorException {
        ArrayList<TableSpec> tables = new ArrayList<TableSpec>();
        ResultSet rs = null;
        try {
            DatabaseMetaData dbmd = con.getMetaData();
            rs = dbmd.getTables(null, null, null, null);
            while (rs.next()) {
                TableSpec ts = new TableSpec();
                ts.catalog = rs.getString("TABLE_CAT");
                ts.schema = rs.getString("TABLE_SCHEM");
                ts.type = rs.getString("TABLE_TYPE");
                ts.name = rs.getString("TABLE_NAME");
                tables.add(ts);
            }
        } catch (SQLException e) {
            throw new NavigatorException(e);
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (SQLException e) {}
        }
        return tables;
    }
    
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public Table loadTable(String qualifiedName) throws NavigatorException {
        return newJDBCTable(qualifiedName);
    }

    private PrimaryKey getPrimaryKey(String qname) throws NavigatorException {
        Main.log(3, "JDBCDatabase.getPrimaryKey(\"" + qname + "\")");
        String[] parts = parseQualifiedName(qname);
        ResultSet rs = null;
        try {
            DatabaseMetaData dbmd = con.getMetaData();
            rs = dbmd.getPrimaryKeys(parts[0], parts[1], parts[2]);
            String keyName = null;
            ArrayList<String> colName = new ArrayList<String>();
            while (rs.next()) {
                String n = rs.getString("COLUMN_NAME");
                short i = rs.getShort("KEY_SEQ");
                if (i < 1)
                    // SQLite workaround
                    continue;
                keyName = rs.getString("PK_NAME");
                for (int j = colName.size(); j < i; j++)
                    colName.add(null);
                colName.set(i - 1, n);
            }
            for (int i = colName.size() - 1; i >= 0; i--)
                if (colName.get(i) == null)
                    colName.remove(i);
            if (colName.size() != 0) {
                BasicPrimaryKey pk = new BasicPrimaryKey();
                pk.setName(keyName);
                pk.setColumns(colName.toArray(new String[colName.size()]));
                return pk;
            } else
                return null;
        } catch (SQLException e) {
            throw new NavigatorException(e);
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (SQLException e) {}
        }
    }

    protected Index[] getIndexes(Table table) throws NavigatorException {
        Main.log(3, "JDBCDatabase.getIndexes(\"" + table.getQualifiedName()
                                                                + "\")");
        PrimaryKey pk = table.getPrimaryKey();
        ResultSet rs = null;
        try {
            DatabaseMetaData dbmd = con.getMetaData();
            rs = dbmd.getIndexInfo(table.getCatalog(),
                                table.getSchema(),
                                table.getName(),
                                false,
                                false);
            String prevName = null;
            ArrayList<Index> indexes = new ArrayList<Index>();
            ArrayList<String> columns = null;
            BasicIndex index = null;
            while (rs.next()) {
                String name = rs.getString("INDEX_NAME");
                if (name == null || pk != null && name.equals(pk.getName()))
                    continue;
                if (!name.equals(prevName)) {
                    if (index != null) {
                        index.setColumns(columns.toArray(new String[0]));
                        indexes.add(index);
                    }
                    index = new BasicIndex();
                    columns = new ArrayList<String>();
                    index.setName(name);
                    index.setUnique(!rs.getBoolean("NON_UNIQUE"));
                }
                columns.add(rs.getString("COLUMN_NAME"));
                prevName = name;
            }
            if (index != null) {
                index.setColumns(columns.toArray(new String[0]));
                indexes.add(index);
            }
            return indexes.toArray(new Index[0]);
        } catch (SQLException e) {
            throw new NavigatorException(e);
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (SQLException e) {}
        }
    }

    private ForeignKey[] getFK(String qualifiedName, boolean imported)
                                                    throws NavigatorException {
        Main.log(3, "JDBCDatabase.getFK(\"" + qualifiedName + "\", "
                            + (imported ? "imported" : "exported") + ")");
        String[] parts = parseQualifiedName(qualifiedName);
        String catalog = parts[0];
        String schema = parts[1];
        String name = parts[2];
        ResultSet rs = null;
        ArrayList<FKResultSetRow> results = new ArrayList<FKResultSetRow>();
        try {
            DatabaseMetaData dbmd = con.getMetaData();
            if (imported)
                rs = dbmd.getImportedKeys(catalog, schema, name);
            else
                rs = dbmd.getExportedKeys(catalog, schema, name);
            if (rs == null)
                return new ForeignKey[0];

            // Now for some fun. There is some serious brain damage in the JDBC
            // spec for DatabaseMetaData.getImportedKeys() and
            // getExportedKeys().
            // The problem is that the result sets are sorted by the 'other'
            // table's catalog, schema, name, and key sequence -- and this
            // means that if two foreign keys reference the same table, their
            // components will get shuffled together! (You will get component 1
            // of key 1, then component 1 of key 2, then component 2 of key 1,
            // then component 2 of key 2...)
            // There is no sure-fire way to fix or even detect this problem.
            // However, as long as foreign keys have unique names (and I expect
            // most RDBMSes will assign a unique name if the user does not
            // specify one) we can re-sort the result set, this time by
            // catalog, schema, name, key name, and key sequence, and that will
            // fix the problem. If you have a DB with no, or non-unique,
            // foreign key names, you lose. Sorry!

            while (rs.next()) {
                FKResultSetRow rsr = new FKResultSetRow();
                if (imported) {
                    rsr.thatCatalog = rs.getString("PKTABLE_CAT");
                    rsr.thatSchema = rs.getString("PKTABLE_SCHEM");
                    rsr.thatName = rs.getString("PKTABLE_NAME");
                    rsr.thatKeyName = unmangleKeyName(
                            rs.getString("PK_NAME"));
                    rsr.thatColumn = rs.getString("PKCOLUMN_NAME");
                    rsr.thisKeyName = unmangleKeyName(
                            rs.getString("FK_NAME"));
                    rsr.thisColumn = rs.getString("FKCOLUMN_NAME");
                } else {
                    rsr.thatCatalog = rs.getString("FKTABLE_CAT");
                    rsr.thatSchema = rs.getString("FKTABLE_SCHEM");
                    rsr.thatName = rs.getString("FKTABLE_NAME");
                    rsr.thatKeyName = unmangleKeyName(
                            rs.getString("FK_NAME"));
                    rsr.thatColumn = rs.getString("FKCOLUMN_NAME");
                    rsr.thisKeyName = unmangleKeyName(
                            rs.getString("PK_NAME"));
                    rsr.thisColumn = rs.getString("PKCOLUMN_NAME");
                }
                rsr.updateRule = rs.getShort("UPDATE_RULE");
                rsr.deleteRule = rs.getShort("DELETE_RULE");
                rsr.keySeq = rs.getShort("KEY_SEQ");
                results.add(rsr);
            }
        } catch (SQLException e) {
            throw new NavigatorException(e);
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (SQLException e) {}
        }

        if (imported)
            Collections.sort(results, FKResultSetRowComparator.forImport);
        else
            Collections.sort(results, FKResultSetRowComparator.forExport);

        ArrayList<ForeignKey> fks = new ArrayList<ForeignKey>();
        ArrayList<FKPart> currFk = new ArrayList<FKPart>();

        short prevKeySeq = 0;
        Iterator<FKResultSetRow> iter = results.iterator();
        FKResultSetRow rsr = null;

        while (true) {
            boolean done = !iter.hasNext();
            FKResultSetRow nextRsr = done ? null : iter.next();
            boolean flushKey = done;

            if (!done) {
                if (nextRsr.keySeq != prevKeySeq + 1)
                    flushKey = true;
            }

            if (flushKey && currFk.size() > 0) {
                BasicForeignKey fk = new BasicForeignKey();
                int keySize = currFk.size();
                String[] thisColumns = new String[keySize];
                String[] thatColumns = new String[keySize];
                for (int i = 0; i < currFk.size(); i++) {
                    FKPart fkp = currFk.get(i);
                    thisColumns[i] = fkp.thisColumn;
                    thatColumns[i] = fkp.thatColumn;
                }
                fk.setThisColumns(thisColumns);
                fk.setThatColumns(thatColumns);
                currFk.clear();
                fk.setThisKeyName(rsr.thisKeyName);
                fk.setThatKeyName(rsr.thatKeyName);
                fk.setThatCatalog(showCatalogs() ? rsr.thatCatalog : null);
                fk.setThatSchema(showSchemas() ? rsr.thatSchema : null);
                fk.setThatName(rsr.thatName);
                fk.setThatQualifiedName(makeQualifiedName(rsr.thatCatalog,
                                                          rsr.thatSchema,
                                                          rsr.thatName));
                fk.setUpdateRule(ruleString(rsr.updateRule));
                fk.setDeleteRule(ruleString(rsr.deleteRule));
                fks.add(fk);
            }

            if (done)
                break;
            else {
                rsr = nextRsr;
                currFk.add(new FKPart(rsr.thatColumn, rsr.thisColumn));
                prevKeySeq = rsr.keySeq;
            }
        }

        int n = fks.size();
        if (n == 0)
            return new ForeignKey[0];
        else
            return fks.toArray(new ForeignKey[n]);
    }

    private static String ruleString(short rule) {
        switch (rule) {
            case DatabaseMetaData.importedKeyNoAction:
                return "no action";
            case DatabaseMetaData.importedKeyCascade:
                return "cascade";
            case DatabaseMetaData.importedKeySetNull:
                return "set null";
            case DatabaseMetaData.importedKeySetDefault:
                return "set default";
            case DatabaseMetaData.importedKeyRestrict:
                return "restrict";
            default:
                return "unknown";
        }
    }

    private static class FKResultSetRow {
        String thatCatalog;
        String thatSchema;
        String thatName;
        String thatKeyName;
        String thatColumn;
        String thisKeyName;
        String thisColumn;
        short updateRule;
        short deleteRule;
        short keySeq;
    }

    private static class FKResultSetRowComparator implements Comparator<FKResultSetRow> {
        public static final FKResultSetRowComparator forImport
                                    = new FKResultSetRowComparator(true);
        public static final FKResultSetRowComparator forExport
                                    = new FKResultSetRowComparator(false);
        private boolean impord;
        private FKResultSetRowComparator(boolean impord) {
            this.impord = impord;
        }
        public int compare(FKResultSetRow a, FKResultSetRow b) {
            int res = strcmp(a.thatCatalog, b.thatCatalog);
            if (res != 0)
                return res;
            res = strcmp(a.thatSchema, b.thatSchema);
            if (res != 0)
                return res;
            res = a.thatName.compareTo(b.thatName);
            if (res != 0)
                return res;
            if (impord)
                res = strcmp(a.thisKeyName, b.thisKeyName);
            else
                res = strcmp(a.thatKeyName, b.thatKeyName);
            if (res != 0)
                return res;
            return a.keySeq - b.keySeq;
        }
        private static int strcmp(String a, String b) {
            return a == null ? b == null ? 0 : 1
                             : b == null ? -1 : a.compareTo(b);
        }
    }

    private static class FKPart {
        public String thatColumn;
        public String thisColumn;

        public FKPart(String thatColumn, String thisColumn) {
            this.thatColumn = thatColumn;
            this.thisColumn = thisColumn;
        }
    }


    /**
     * This method is provided so that drivers can convert date, time, and
     * interval types to JDBC Navigator's private versions of those types
     * (jdbcnav.model.DateTime, IntervalDS, IntervalYM, IntervalYS).
     */
    protected Object db2nav(TypeSpec spec, Object o) {
        if (o == null)
            return null;
        if (Timestamp.class.isAssignableFrom(spec.jdbcJavaClass)) {
            Timestamp ts = (Timestamp) o;
            int nanos = ts.getNanos();
            long time = ts.getTime() - nanos / 1000000;
            return new DateTime(time, nanos, null);
        }
        if (java.util.Date.class.isAssignableFrom(spec.jdbcJavaClass)) {
            java.util.Date d = (java.util.Date) o;
            return new DateTime(d.getTime(), 0, null);
        }
        return o;
    }

    /**
     * This method is provided so that drivers can convert JDBC Navigator's
     * private date, time, and interval types (jdbcnav.model.DateTime,
     * IntervalDS, IntervalYM) to the DB-specific versions of those types.
     */
    protected Object nav2db(TypeSpec spec, Object o) {
        if (o == null)
            return null;
        if (Timestamp.class.isAssignableFrom(spec.jdbcJavaClass)) {
            DateTime dt = (DateTime) o;
            Timestamp ts = new Timestamp(dt.time);
            ts.setNanos(dt.nanos);
            return ts;
        }
        if (Time.class.isAssignableFrom(spec.jdbcJavaClass)) {
            DateTime dt = (DateTime) o;
            Time t = new Time(dt.time + dt.nanos / 1000000);
            return t;
        }
        if (java.sql.Date.class.isAssignableFrom(spec.jdbcJavaClass)) {
            DateTime dt = (DateTime) o;
            java.sql.Date d = new java.sql.Date(dt.time);
            return d;
        }
        if (java.util.Date.class.isAssignableFrom(spec.jdbcJavaClass)) {
            DateTime dt = (DateTime) o;
            java.util.Date d = new java.util.Date(dt.time + dt.nanos / 1000000);
            return d;
        }
        return o;
    }

    /**
     * This method is provided so that drivers that need to perform special
     * contortions to set values can override it (e.g., Oracle needs to do
     * streaming to get around size limits on LONG, LONG RAW, and [BC]LOB).
     */
    protected void setObject(PreparedStatement stmt, int index, int
                             dbtable_col, Object o, Table table)
                                                        throws SQLException {
        int type = table.getTypeSpecs()[dbtable_col].jdbcSqlType;
        if (type == Types.OTHER)
            stmt.setObject(index, o);
        else
            stmt.setObject(index, o, type);
    }

    /**
     * This method is provided so that drivers that need to subclass
     * JDBCTable can provide their own factory, which will be used by
     * JDBCDatabase also.
     */
    protected JDBCTable newJDBCTable(String qualifiedName)
                                                    throws NavigatorException {
        return new JDBCTable(qualifiedName);
    }

    /**
     * This method is provided so that drivers that need to subclass
     * PartialTable can provide their own factory, which will be used by
     * JDBCDatabase also.
     */
    protected PartialTable newPartialTable(String q, Object[] v, Table t, Data d)
                                                    throws NavigatorException {
        return new PartialTable(q, v, t, d);
    }

    /**
     * This method returns a TypeSpec object for a given column description.
     * Subclasses should override it to provide database-specific type details.
     * <br>
     * This default implementation supports a subset of ANSI SQL types:
     * CHAR, VARCHAR, INTEGER, FLOAT, DECIMAL, TIME, and DATE, and a few
     * variants and synonyms for these. This implementation is merely intended
     * as a fall-back to allow some quick editing on unsupported databases;
     * for proper support for all of a database's types, the JDBCDatabase and
     * ScriptGenerator classes should be subclassed, and the new subclasses
     * added to the InternalDriverMap.
     */
    protected TypeSpec makeTypeSpec(String dbType, Integer size, Integer scale,
                                    int sqlType, String javaType) {
        TypeSpec spec = makeDefaultTypeSpec(dbType, size, scale, sqlType, javaType);
        dbType = dbType.toLowerCase();
        if (dbType.equals("char")
                || dbType.equals("character")) {
            spec.type = TypeSpec.CHAR;
            spec.size = size;
        } else if (dbType.equals("nchar")
                || dbType.equals("national character")) {
            spec.type = TypeSpec.NCHAR;
            spec.size = size;
        } else if (dbType.equals("varchar")
                || dbType.equals("character varying")) {
            spec.type = TypeSpec.VARCHAR;
            spec.size = size;
        } else if (dbType.equals("nvarchar")
                || dbType.equals("national character varying")) {
            spec.type = TypeSpec.VARNCHAR;
            spec.size = size;
        } else if (dbType.equals("numeric")
                || dbType.equals("decimal")) {
            spec.type = TypeSpec.FIXED;
            spec.size = size;
            spec.size_in_bits = false;
            spec.scale = scale;
            spec.scale_in_bits = false;
        } else if (dbType.equals("float") && size <= 24
                || dbType.equals("real")) {
            spec.type = TypeSpec.FLOAT;
            spec.size = 24;
            spec.size_in_bits = true;
            spec.min_exp = -127;
            spec.max_exp = 127;
            spec.exp_of_2 = true;
        } else if (dbType.equals("float") && size > 24
                || dbType.equals("double")
                || dbType.equals("double precision")) {
            spec.type = TypeSpec.FLOAT;
            spec.size = 54;
            spec.size_in_bits = true;
            spec.min_exp = -1023;
            spec.max_exp = 1023;
            spec.exp_of_2 = true;
        } else if (dbType.equals("smallint")) {
            spec.type = TypeSpec.FIXED;
            spec.size = 16;
            spec.size_in_bits = true;
            spec.scale = 0;
            spec.scale_in_bits = true;
        } else if (dbType.equals("integer")) {
            spec.type = TypeSpec.FIXED;
            spec.size = 32;
            spec.size_in_bits = true;
            spec.scale = 0;
            spec.scale_in_bits = true;
        } else if (dbType.equals("date")) {
            spec.type = TypeSpec.DATE;
        } else if (dbType.equals("time")) {
            spec.type = TypeSpec.TIME;
            spec.size = 0;
        } else if (dbType.equals("timestamp")) {
            spec.type = TypeSpec.TIMESTAMP;
            spec.size = 3;
        }
        return spec;
    }
    
    /**
     * This method returns a basic TypeSpec object for a given column description.
     * These should be fleshed out by makeTypeSpec().
     */
    protected TypeSpec makeDefaultTypeSpec(String dbType, Integer size, Integer scale,
                                    int sqlType, String javaType) {
        TypeSpec spec = new TypeSpec(this);
        spec.type = TypeSpec.UNKNOWN;
        if (size == null)
            spec.native_representation = dbType;
        else if (scale == null)
            spec.native_representation = dbType + "(" + size + ")";
        else
            spec.native_representation = dbType + "(" + size + ", " + scale+")";
        spec.jdbcDbType = dbType;
        spec.jdbcSize = size;
        spec.jdbcScale = scale;
        spec.jdbcSqlType = sqlType;
        if (javaType == null) {
            spec.jdbcJavaType = "java.lang.Object";
            spec.jdbcJavaClass = Object.class;
        } else {
            if (javaType.equals("byte[]"))
                javaType = "[B";
            spec.jdbcJavaType = javaType;
            try {
                spec.jdbcJavaClass = Class.forName(javaType);
            } catch (ClassNotFoundException e) {
                spec.jdbcJavaClass = Object.class;
            }
        }
        return spec;
    }

    /**
     * This method is defined so that subclasses can do db-specific magic to
     * populate the database-specific type name (because some JDBC drivers do
     * not provide sufficient information in DatabaseMetaData.getColumns()).
     */
    protected void fixTypeSpecs(String qualifiedName, TypeSpec[] specs) {
        // No-op
    }

    /**
     * This method is defined so that subclasses can do db-specific magic to
     * identify generated columns, for cases where they aren't identified by
     * IS_GENERATEDCOLUMN.
     */
    protected void fixIsGenerated(TypeSpec[] specs, String[] isGenerated) {
        // No-op
    }

    public void searchTables(Set<String> qualifiedNames, String searchText) throws NavigatorException {
        SearchResultsFrame srf = new SearchResultsFrame(this, qualifiedNames, searchText);
        srf.setParent(browser);
        srf.showStaggered();
    }

    public int searchTable(String qualifiedName, String searchText) throws NavigatorException {
        Table table = getTable(qualifiedName);
        StringBuffer query = new StringBuffer();
        List<Object> args = new ArrayList<Object>();
        query.append("select count(*) from " + table.getQualifiedName() + " where ");
        boolean first = true;
        for (int i = 0; i < table.getColumnCount(); i++) {
            TypeSpec spec = table.getTypeSpecs()[i];
            Object value;
            try {
                value = spec.stringToObject(searchText);
                value = nav2db(spec, value);
            } catch (Exception e) {
                continue;
            }
            if (first)
                first = false;
            else
                query.append(" or ");
            query.append(quote(table.getColumnNames()[i]));
            if (value instanceof String) {
                query.append(" like ?");
                args.add("%" + value + "%");
            } else {
                query.append(" = ?");
                args.add(value);
            }
        }
        if (args.isEmpty())
            return 0;
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(query.toString());
            int idx = 1;
            for (Object o : args)
                stmt.setObject(idx++, o);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new NavigatorException("Search Table failed", e);
        } finally {
            if (stmt != null)
                try {
                    stmt.close();
                } catch (SQLException e) {}
        }
    }
    
    public void runSearch(String qualifiedName, String searchText) {
        try {
            Table table = getTable(qualifiedName);
            StringBuffer query = new StringBuffer();
            List<Object> args = new ArrayList<Object>();
            query.append("select * from " + table.getQualifiedName() + " where ");
            boolean first = true;
            for (int i = 0; i < table.getColumnCount(); i++) {
                TypeSpec spec = table.getTypeSpecs()[i];
                Object value;
                try {
                    value = spec.stringToObject(searchText);
                    value = nav2db(spec, value);
                } catch (Exception e) {
                    continue;
                }
                if (first)
                    first = false;
                else
                    query.append(" or ");
                query.append(quote(table.getColumnNames()[i]));
                if (value instanceof String) {
                    query.append(" like ?");
                    args.add("%" + value + "%");
                } else {
                    query.append(" = ?");
                    args.add(value);
                }
            }
            Object[] argsArray = args.toArray(new Object[args.size()]);
            table = (Table) runQuery(query.toString(), argsArray);
            TableFrame tf = new TableFrame(table, browser);
            tf.setParent(browser);
            tf.showStaggered();
        } catch (Exception e) {
            MessageBox.show(e);
            return;
        }
    }

    private Object runQuery(String query, Object[] values, boolean asynchronous,
                           boolean allowTable) throws NavigatorException {

        Main.log(3, "JDBCDatabase.runQuery(\"" + query + "\", "
                            + (asynchronous ? "async" : "sync") + ")");
        Table table = null;

        if (!resultSetContainsTableInfo()) {
            // Great. We're dealing with a JDBC driver that doesn't implement
            // ResultSetMetaData.getCatalogName(), getSchemaName(), and
            // getTableName().
            // If we're going to return the query result as a PartialTable,
            // rather than a base Data (or BackgroundLoadData, whatever),
            // we need to know which table columns the result set columns
            // correspond to. Without help from the JDBC driver, the only
            // way to find that out is by parsing the SQL ourselves.
            // Since I don't feel like implementing a full-blown SQL parser
            // here (not right now, anyway, plenty TODO already!), I restrict
            // myself to these useful special cases:
            //
            //     select * from <table>
            //     select a, b, c from <table>
            //     select foo.* from [...] <table> foo [...]
            //     select foo.a, foo.b, foo.c from [...] <table> foo [...]
            //
            // The idea is that all the selected columns must come from the
            // same table; if the column names are unqualified, the only way
            // to be sure of this is to require that the from clause contain
            // only one table.

            StringTokenizer tok = new StringTokenizer(query, " \t\n\r,", true);
            int state = 0;
            String identifier = "";
            boolean firstCol = true;
            String name = null;
            boolean success = false;
            scanner:
            while (tok.hasMoreTokens()) {
                String t = tok.nextToken();
                if (t.equals(" ") || t.equals("\t")
                        || t.equals("\n") || t.equals("\r"))
                    continue;
                switch (state) {
                    case 0:
                        // Looking for "select"
                        if (!t.equalsIgnoreCase("select"))
                            break scanner;
                        state = 1;
                        break;
                    case 1:
                        // Looking for optional "distinct"
                        state = 2;
                        if (t.equalsIgnoreCase("distinct"))
                            break;
                        // fall through
                    case 2: {
                        // Looking for column names or "*", optionally qualified
                        if (t.equals(","))
                            continue;
                        if (t.equalsIgnoreCase("from"))
                            if (firstCol)
                                break scanner;
                            else {
                                state = 3;
                                break;
                            }
                        int dot = t.indexOf('.');
                        String id;
                        if (dot == -1)
                            id = "";
                        else {
                            id = t.substring(0, dot);
                            t = t.substring(dot + 1);
                        }
                        if (t.contains("("))
                            // Don't allow functions
                            break scanner;
                        if (firstCol) {
                            identifier = id;
                            firstCol = false;
                        } else if (!id.equals(identifier))
                            break scanner;
                        break;
                    }
                    case 3:
                        // Looking for table names
                        if (t.equals(",") || t.equalsIgnoreCase("where") || t.equalsIgnoreCase("join"))
                            break scanner;
                        name = t;
                        state = 4;
                        break;
                    case 4:
                        // After table name
                        if (identifier.equals("")) {
                            // We have a "select <columns> from", where all the
                            // columns were unqualified, so the first word
                            // after "from" should be the table name, and the
                            // first word after the table name should be
                            // "where" or "join"; else we have a construct we
                            // can't handle.
                            if (t.equalsIgnoreCase("where") || t.equalsIgnoreCase("join"))
                                success = true;
                            state = 6;
                            break scanner;
                        } else {
                            // We have a "select foo.* from", and the previous
                            // word was a table name. If the current word
                            // matches "foo", we've found our table and are
                            // done; otherwise, we move on to look for a comma.
                            if (t.equalsIgnoreCase("where") || t.equalsIgnoreCase("join"))
                                break scanner;
                            if (t.equalsIgnoreCase(identifier)) {
                                success = true;
                                break scanner;
                            } else {
                                state = 5;
                                break;
                            }
                        }
                    case 5:
                        // Waiting for comma
                        if (!t.equals(","))
                            break scanner;
                        state = 3;
                        break;
                }
            }
            if (state == 4 && identifier.equals(""))
                // We ran out of tokens just after having found a table name
                // in a "select * from <tablename>" query.
                success = true;
            if (success) {
                // If the table name looks to be unqualified, we try to
                // qualify it ourselves (in some DB-specific manner)
                if (name.indexOf('.') == -1)
                    name = qualifyName(name);
                String target = getSynonymTarget(name);
                if (target != null)
                    name = target;
                try {
                    table = getTable(name);
                } catch (NavigatorException e) {
                    // Well, we tried. This is not a fatal error; the user
                    // will just have to make do with a non-editable
                    // QueryResultFrame.
                }
            }
        }

        Statement s = null;
        ResultSet rs = null;
        try {
            Main.log(3, "executing query...");
            if (values == null) {
                s = con.createStatement();
                rs = s.executeQuery(query);
            } else {
                PreparedStatement ps = con.prepareStatement(query);
                for (int i = 0; i < values.length; i++) {
                    Object v = values[i];
                    ps.setObject(i + 1, v);
                }
                s = ps;
                rs = ps.executeQuery();
            }

            Main.log(3, "reading metadata...");
            ResultSetMetaData rsmd = rs.getMetaData();
            int columns = rsmd.getColumnCount();
            String[] columnNames = new String[columns];
            TypeSpec[] typeSpecs = new TypeSpec[columns];

            String catalog = null;
            String schema = null;
            String name = null;
            boolean canDoTable = resultSetContainsTableInfo();

            for (int i = 0; i < columns; i++) {
                columnNames[i] = rsmd.getColumnName(i + 1);

                String dbType = rsmd.getColumnTypeName(i + 1);
                int scale = rsmd.getScale(i + 1);
                int sqlType = rsmd.getColumnType(i + 1);
                String javaType = rsmd.getColumnClassName(i + 1);
                int size;
                if ("oracle.sql.BLOB".equals(javaType)
                        || "oracle.sql.CLOB".equals(javaType))
                    size = Integer.MAX_VALUE;
                else
                    size = rsmd.getPrecision(i + 1);
                typeSpecs[i] = makeTypeSpec(dbType, size,
                                        scale, sqlType, javaType);

                if (resultSetContainsTableInfo()) {
                    String catalog2 = rsmd.getCatalogName(i + 1);
                    String schema2 = rsmd.getSchemaName(i + 1);
                    String name2 = rsmd.getTableName(i + 1);
                    if (i == 0) {
                        catalog = catalog2;
                        schema = schema2;
                        name = name2;
                    } else {
                        if (!MiscUtils.strEq(catalog, catalog2)
                                || !MiscUtils.strEq(schema, schema2)
                                || !MiscUtils.strEq(name, name2))
                            canDoTable = false;
                    }
                }
            }

            if (canDoTable) {
                String qname = makeQualifiedName(catalog, schema, name);
                try {
                    table = getTable(qname);
                } catch (NavigatorException e) {
                    // Well, we tried. This is not a fatal error; the user
                    // will just have to make do with a non-editable
                    // QueryResultFrame.
                }
            }

            if (table != null) {
                // We check if all the primary key components are present
                // in the result set; if there is no primary key, we make
                // sure that *all* columns are present.
                // The idea is to prevent the occurrence that someone
                // deletes a row from a partial view, and inadvertently
                // deletes many more rows from the actual table. By
                // ensuring the complete primary key is present, we
                // guarantee that deleting one row from a partial view
                // deletes exactly that one row from the actual table.
                PrimaryKey pk = table.getPrimaryKey();
                int pkSize;
                String[] pkColumns;
                if (pk != null) {
                    pkSize = pk.getColumnCount();
                    pkColumns = new String[pkSize];
                    for (int i = 0; i < pkSize; i++)
                        pkColumns[i] = pk.getColumnName(i);
                } else {
                    pkSize = table.getColumnCount();
                    pkColumns = table.getColumnNames();
                }
                for (int i = 0; i < pkSize; i++) {
                    String n = pkColumns[i];
                    boolean found = false;
                    for (int j = 0; j < columnNames.length; j++)
                        if (columnNames[j].equalsIgnoreCase(n)) {
                            found = true;
                            break;
                        }
                    if (!found) {
                        // Primary key component missing in ResultSet;
                        // don't allow table in this case
                        table = null;
                        break;
                    }
                }
            }

            if (asynchronous) {
                Main.log(3, "creating background loader");
                BackgroundLoadData bld =
                            new BackgroundLoadData(columnNames, typeSpecs);
                Thread ldr = new Thread(
                        new BackgroundLoader(bld, s, rs, table, columnNames));
                ldr.setPriority(Thread.MIN_PRIORITY);
                ldr.setDaemon(true);
                ldr.start();

                // Prevent 'finally' clause from closing Statement & ResultSet
                s = null;
                rs = null;
                if (allowTable && table != null)
                    return newPartialTable(query, values, table, bld);
                else
                    return bld;
            }

            BasicData bd = new BasicData();
            bd.setColumnNames(columnNames);
            bd.setTypeSpecs(typeSpecs);

            ArrayList<Object[]> data = new ArrayList<Object[]>();
            boolean noClone = lobsOutliveResultSets() || table == null;
            while (rs.next()) {
                Object[] row = new Object[columns];
                for (int i = 0; i < columns; i++)
                    row[i] = rs.getObject(i + 1);
                Object[] orig_row = noClone ? null : row.clone();
                for (int i = 0; i < columns; i++) {
                    Object o = row[i];
                    row[i] = wrapLob(table, columnNames, orig_row, i,
                                                        bd.getTypeSpec(i), o);
                }
                data.add(row);
            }

            bd.setData(data);

            if (allowTable && table != null)
                return newPartialTable(query, values, table, bd);
            else
                return bd;
        } catch (SQLException e) {
            throw new NavigatorException(e);
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (SQLException e) {}
            if (s != null)
                try {
                    s.close();
                } catch (SQLException e) {}
        }
    }

    public Object runQuery(String query, boolean asynchronous,
            boolean allowTable) throws NavigatorException {
        return runQuery(query, null, asynchronous, allowTable);
    }

    public Object runQuery(String query, Object[] values) throws NavigatorException {
        return runQuery(query, values, false, true);
    }

    public int runUpdate(String query) throws NavigatorException {
        Statement s = null;
        try {
            s = con.createStatement();
            return s.executeUpdate(query);
        } catch (SQLException e) {
            throw new NavigatorException(e);
        } finally {
            if (s != null)
                try {
                    s.close();
                } catch (SQLException e) {}
        }
    }

    public Scriptable createStatement() throws NavigatorException {
        try {
            Statement stmt = con.createStatement();
            return new JavaScriptStatement(stmt);
        } catch (SQLException e) {
            throw new NavigatorException(e);
        }
    }

    public Scriptable prepareStatement(String statement)
                                                    throws NavigatorException {
        try {
            PreparedStatement pstmt = con.prepareStatement(statement);
            return new JavaScriptPreparedStatement(pstmt);
        } catch (SQLException e) {
            throw new NavigatorException(e);
        }
    }

    public Scriptable prepareCall(String call) throws NavigatorException {
        try {
            CallableStatement cstmt = con.prepareCall(call);
            return new JavaScriptCallableStatement(cstmt);
        } catch (SQLException e) {
            throw new NavigatorException(e);
        }
    }

    public File getFile() {
        return null;
    }

    public boolean save(File file) throws NavigatorException {
        new FileDatabase(getSelectedTables()).save(file);
        return true;
    }

    protected class JDBCTable extends BasicTable {
        public JDBCTable(String qualifiedName) throws NavigatorException {
            this.qualifiedName = qualifiedName;
            updateDetails();
        }

        public boolean isEditable() {
            return true;
        }

        public ResultSetTableModel createModel() throws NavigatorException {
            ResultSetTableModel model = super.createModel();
            if (qualifiedName.indexOf("...") == -1 // No orphans
                    && !editedTables.contains(this))
                editedTables.add(this);
            return model;
        }

        public void unloadModel() {
            editedTables.remove(this);
            super.unloadModel();
        }

        public Database getDatabase() {
            return JDBCDatabase.this;
        }

        protected Data getPKValues2() throws NavigatorException {
            if (pk == null)
                return null;
            int ncols = pk.getColumnCount();
            StringBuffer buf = new StringBuffer();
            buf.append("select ");
            for (int i = 0; i < ncols; i++) {
                if (i > 0)
                    buf.append(", ");
                buf.append(quote(pk.getColumnName(i)));
            }
            buf.append(" from ");
            buf.append(qualifiedName);
            return (Data) runQuery(buf.toString(), false, false);
        }

        public Data getData(boolean async) throws NavigatorException {
            return (Data) runQuery("select * from " + qualifiedName,
                                   async, false);
        }

        public void updateDetails() throws NavigatorException {
            // Create backups of everything, in case we need to roll back
            // after catching an exception
            Main.log(3, "JDBCDatabase.JDBCTable(\"" + qualifiedName
                                                + "\").updateDetails()");
            String oldCatalog = catalog;
            String oldSchema = schema;
            String oldName = name;
            String oldType = type;
            String oldRemarks = remarks;
            String[] oldColumnNames = columnNames;
            TypeSpec[] oldTypeSpecs = typeSpecs;
            String[] oldIsNullable = isNullable;
            String[] oldDefaults = defaults;
            String[] oldIsGenerated = isGenerated;
            PrimaryKey oldPk = pk;
            ForeignKey[] oldFks = fks;
            ForeignKey[] oldRks = rks;
            Index[] oldIndexes = indexes;

            boolean rollback = true;

            try {
                String[] parts = parseQualifiedName(qualifiedName);
                catalog = showCatalogs() ? parts[0] : null;
                schema = showSchemas() ? parts[1] : null;
                name = parts[2];

                ArrayList<String> columnNamesList = new ArrayList<String>();
                ArrayList<String> dbTypesList = new ArrayList<String>();
                ArrayList<Integer> columnSizesList = new ArrayList<Integer>();
                ArrayList<Integer> columnScalesList = new ArrayList<Integer>();
                ArrayList<Integer> sqlTypesList = new ArrayList<Integer>();
                ArrayList<String> isNullableList = new ArrayList<String>();
                ArrayList<String> defaultsList = new ArrayList<String>();
                ArrayList<String> isGeneratedList = new ArrayList<String>();

                ResultSet rs = null;
                DatabaseMetaData dbmd = null;
                try {
                    dbmd = con.getMetaData();

                    rs = dbmd.getTables(catalog, schema, name, null);
                    if (rs.next()) {
                        type = rs.getString("TABLE_TYPE");
                        try {
                            remarks = rs.getString("REMARKS");
                        } catch (SQLException e) {
                            // Older versions of Oracle erroneously call this
                            // TABLE_REMARKS
                            remarks = rs.getString("TABLE_REMARKS");
                        }
                    } else
                        throw new NavigatorException("Table "
                                        + qualifiedName + " does not exist.");
                } catch (SQLException e) {
                    throw new NavigatorException(e);
                } finally {
                    if (rs != null)
                        try {
                            rs.close();
                        } catch (SQLException e) {}
                }

                rs = null;
                int cols = 0;
                try {
                    rs = dbmd.getColumns(catalog, schema, name, null);
                    while (rs.next()) {
                        columnNamesList.add(rs.getString("COLUMN_NAME"));
                        sqlTypesList.add(rs.getInt("DATA_TYPE"));
                        dbTypesList.add(rs.getString("TYPE_NAME"));
                        int size = rs.getInt("COLUMN_SIZE");
                        if (rs.wasNull())
                            columnSizesList.add(null);
                        else
                            columnSizesList.add(size);
                        int scale = rs.getInt("DECIMAL_DIGITS");
                        if (rs.wasNull())
                            columnScalesList.add(null);
                        else
                            columnScalesList.add(scale);
                        defaultsList.add(rs.getString("COLUMN_DEF"));
                        isNullableList.add(rs.getString("IS_NULLABLE"));
                        isGeneratedList.add(rs.getString("IS_GENERATEDCOLUMN"));
                        cols++;
                    }
                } catch (SQLException e) {
                    throw new NavigatorException(e);
                } finally {
                    if (rs != null)
                        try {
                            rs.close();
                        } catch (SQLException e) {}
                }

                columnNames = columnNamesList.toArray(new String[cols]);

                typeSpecs = new TypeSpec[cols];
                String[] javaTypes = JDBCDatabase.this.getJavaTypes(
                                                            qualifiedName);
                for (int i = 0; i < cols; i++) {
                    String dbType = dbTypesList.get(i);
                    Integer size = columnSizesList.get(i);
                    Integer scale = columnScalesList.get(i);
                    int sqlType = sqlTypesList.get(i);
                    typeSpecs[i] = makeTypeSpec(dbType, size, scale, sqlType,
                                                                javaTypes[i]);
                }
                fixTypeSpecs(qualifiedName, typeSpecs);

                isNullable = isNullableList.toArray(new String[cols]);
                defaults = defaultsList.toArray(new String[cols]);
                isGenerated = isGeneratedList.toArray(new String[cols]);
                fixIsGenerated(typeSpecs, isGenerated);

                pk = JDBCDatabase.this.getPrimaryKey(qualifiedName);
                fks = JDBCDatabase.this.getFK(qualifiedName, true);
                rks = JDBCDatabase.this.getFK(qualifiedName, false);
                indexes = JDBCDatabase.this.getIndexes(this);

                TreeSet<String> keyColumns = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
                if (pk != null) {
                    int n = pk.getColumnCount();
                    for (int i = 0; i < n; i++)
                        keyColumns.add(pk.getColumnName(i));
                }
                for (int i = 0; i < fks.length; i++) {
                    ForeignKey fk = fks[i];
                    int n = fk.getColumnCount();
                    for (int j = 0; j < n; j++)
                        keyColumns.add(fk.getThisColumnName(j));
                }
                TreeSet<String> indexColumns = new TreeSet<String>(
                                                String.CASE_INSENSITIVE_ORDER);
                for (int i = 0; i < indexes.length; i++) {
                    Index idx = indexes[i];
                    int n = idx.getColumnCount();
                    for (int j = 0; j < n; j++)
                        indexColumns.add(idx.getColumnName(j));
                }

                for (int i = 0; i < cols; i++) {
                    String name = columnNames[i];
                    if (keyColumns.contains(name))
                        typeSpecs[i].part_of_key = true;
                    if (indexColumns.contains(name))
                        typeSpecs[i].part_of_index = true;
                }

                super.updateDetails();

                // Whew! It worked.
                rollback = false;
            } finally {
                if (rollback) {
                    catalog = oldCatalog;
                    schema = oldSchema;
                    name = oldName;
                    type = oldType;
                    remarks = oldRemarks;
                    columnNames = oldColumnNames;
                    typeSpecs = oldTypeSpecs;
                    isNullable = oldIsNullable;
                    defaults = oldDefaults;
                    isGenerated = oldIsGenerated;
                    pk = oldPk;
                    fks = oldFks;
                    rks = oldRks;
                    indexes = oldIndexes;
                }
            }
        }
    }


    /**
     * This method is called by Preferences when Preferences.setPassword()
     * was called and the user entered a password -- when this happens, the
     * list of configs may have changed, so we should reload them.
     * <br>
     * This is a bit ugly, and using a nice callback mechanism would be
     * cleaner, but there is the issue that *most* of the changes to the
     * list of configs are actually caused *here* (by LoginDialog). The
     * real answer, I think, would be for Preferences to provide us with
     * a ComboBoxModel, and to hide everything inside Preferences.
     * <br>
     * Sigh. Too much work for such a minor issue.
     */
    public static void reloadConnectionConfigs() {
        LoginDialog.reloadConnectionConfigs();
    }

    private static class LoginDialog extends MyFrame {
        private static LoginDialog instance;

        private ConfigList configs;
        private JComboBox<String> configNameCB;
        private JTextField driverTF;
        private JTextField urlTF;
        private JTextField usernameTF;
        private JPasswordField passwordPF;
        private JButton connectB;
        private JButton saveB;
        private JButton deleteB;
        private JButton cancelB;
        private ConnectThread connectThread;
        private Database.OpenCallback opencb;

        private LoginDialog(Database.OpenCallback opencb) {
            super("Open JDBC Data Source");
            Container c = getContentPane();
            c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
            JPanel p1 = new JPanel();
            p1.setLayout(new MyGridBagLayout());
            MyGridBagConstraints gbc = new MyGridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = MyGridBagConstraints.EAST;
            p1.add(new JLabel("Name:"), gbc);
            gbc.gridy++;
            p1.add(new JLabel("Driver Class:"), gbc);
            gbc.gridy++;
            p1.add(new JLabel("URL:"), gbc);
            gbc.gridy++;
            p1.add(new JLabel("User Name:"), gbc);
            gbc.gridy++;
            p1.add(new JLabel("Password:"), gbc);
            configs = new ConfigList();
            configNameCB = new JComboBox<String>(configs);
            configNameCB.setEditable(true);
            configNameCB.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            configNameChanged();
                        }
                    });
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.gridwidth = 3;
            gbc.anchor = MyGridBagConstraints.WEST;
            p1.add(configNameCB, gbc);
            driverTF = new MyTextField(30);
            gbc.gridy++;
            p1.add(driverTF, gbc);
            urlTF = new MyTextField(40);
            gbc.gridy++;
            p1.add(urlTF, gbc);
            usernameTF = new MyTextField(10);
            gbc.gridy++;
            gbc.gridwidth = 1;
            p1.add(usernameTF, gbc);
            passwordPF = new JPasswordField(10);
            gbc.gridy++;
            p1.add(passwordPF, gbc);

            c.add(p1);

            JPanel p2 = new JPanel();
            p2.setLayout(new MyGridBagLayout());
            connectB = new JButton("Connect");
            connectB.addActionListener(new ActionListener() {
                                    public void actionPerformed(ActionEvent e) {
                                        connect();
                                    }
                                });
            gbc.insets = new Insets(0, 0, 0, 10);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = MyGridBagConstraints.CENTER;
            p2.add(connectB, gbc);
            saveB = new JButton("Save");
            saveB.addActionListener(new ActionListener() {
                                    public void actionPerformed(ActionEvent e) {
                                        save();
                                    }
                                });
            gbc.insets = new Insets(0, 0, 0, 0);
            gbc.gridx++;
            p2.add(saveB, gbc);
            deleteB = new JButton("Delete");
            deleteB.addActionListener(new ActionListener() {
                                    public void actionPerformed(ActionEvent e) {
                                        delete();
                                    }
                                });
            gbc.gridx++;
            p2.add(deleteB, gbc);
            cancelB = new JButton("Cancel");
            cancelB.addActionListener(new ActionListener() {
                                    public void actionPerformed(ActionEvent e) {
                                        cancel();
                                    }
                                });
            gbc.gridx++;
            p2.add(cancelB, gbc);
            c.add(p2);
            pack();
            configNameChanged();
            this.opencb = opencb;
        }

        public void dispose() {
            instance = null;
            super.dispose();
        }

        public static void activate(Database.OpenCallback opencb) {
            if (instance == null) {
                instance = new LoginDialog(opencb);
                instance.showCentered();
            } else {
                instance.opencb = opencb;
                instance.deiconifyAndRaise();
            }
        }

        public static void reloadConnectionConfigs() {
            if (instance != null)
                instance.configs.reload();
        }

        private void configNameChanged() {
            String name = (String) configNameCB.getSelectedItem();
            Preferences.ConnectionConfig config = configs.get(name);
            if (config != null) {
                driverTF.setText(config.driver);
                urlTF.setText(config.url);
                usernameTF.setText(config.username);
                passwordPF.setText(config.password);
            }
        }

        private void save() {
            String configName = (String) configNameCB.getSelectedItem();
            if (configName.equals("")) {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showInternalMessageDialog(
                        Main.getDesktop(),
                        "You have to specify a name before you can\n"
                        + "save a connection configuration.");
                return;
            }
            String driver = driverTF.getText();
            String url = urlTF.getText();
            String username = usernameTF.getText();
            String password = new String(passwordPF.getPassword());
            configs.put(new Preferences.ConnectionConfig(configName, driver, url,
                                                         username, password));
        }

        private void delete() {
            String configName = (String) configNameCB.getSelectedItem();
            configNameCB.setSelectedItem("");
            configs.remove(configName);
        }

        private void connect() {
            String name = (String) configNameCB.getSelectedItem();
            if (name.equals("")) {
                // Generate a name of the form "Unnamed #<n>", with the lowest
                // value of n that does not clash with any existing configs...
                int n = 0;
                do {
                    name = "Unnamed #" + (++n);
                } while (configs.get(name) != null);
                configNameCB.setSelectedItem(name);
            }
            save();

            String driver = driverTF.getText();
            String url = urlTF.getText();
            String username = usernameTF.getText();
            String password = new String(passwordPF.getPassword());

            try {
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showInternalMessageDialog(
                        Main.getDesktop(),
                        "Driver \"" + driver + "\" was not found.");
                return;
            }

            synchronized (this) {
                connectB.setEnabled(false);
                saveB.setEnabled(false);
                deleteB.setEnabled(false);
                cancelB.requestFocusInWindow();
                // Use a separate thread to establish the connection.
                // DriverManager.getConnection() can take a long time,
                // and we don't want to freeze awt during the wait; also,
                // we want to allow the user to cancel the operation.
                connectThread = new ConnectThread(name, url, driver,
                                                  username, password);
                Main.backgroundJobStarted();
                Thread thr = new Thread(connectThread);
                thr.setPriority(Thread.MIN_PRIORITY);
                thr.setDaemon(true);
                thr.start();
            }
        }

        private void cancel() {
            synchronized (this) {
                if (connectThread != null) {
                    Main.backgroundJobEnded();
                    connectThread = null;
                    connectB.setEnabled(true);
                    saveB.setEnabled(true);
                    deleteB.setEnabled(true);
                } else
                    dispose();
            }
        }

        private class ConnectThread implements Runnable {
            private String name;
            private String url;
            private String driver;
            private String username;
            private String password;

            public ConnectThread(String name, String url, String driver,
                                 String username, String password) {
                this.name = name;
                this.url = url;
                this.driver = driver;
                this.username = username;
                this.password = password;
            }

            public void run() {
                Connection con = null;
                try {
                    con = doConnect(driver, url, username, password);
                } catch (SQLException e) {
                    synchronized (LoginDialog.this) {
                        // if this != connectThread, the operation was
                        // cancelled, and so we should not report errors about
                        // it to the user, nor change the state of the buttons
                        // (the frame may have been destroyed already!).
                        if (this == connectThread) {
                            MessageBox.show("Could not open JDBC connection.",
                                            e);
                            connectB.setEnabled(true);
                            saveB.setEnabled(true);
                            deleteB.setEnabled(true);
                            connectThread = null;
                            Main.backgroundJobEnded();
                        }
                    }
                    return;
                }

                synchronized (LoginDialog.this) {
                    if (this != connectThread) {
                        // User cancelled the operation.
                        try {
                            con.close();
                        } catch (SQLException e) {}
                        return;
                    } else
                        connectThread = null;
                }

                // Open the browser, and dispose the login dialog, on
                // the awt event thread; doing it here causes the awt
                // repaint manager to get confused and throw an exception
                // (which appears to be harmless, but still...).
                SwingUtilities.invokeLater(
                            new BrowserOpener1(name, con, driver, url, username, password));
            }
        }

        private class BrowserOpener1 implements Runnable {
            private String name;
            private Connection con;
            private String driver;
            private String url;
            private String username;
            private String password;

            public BrowserOpener1(String name, Connection con, String driver,
                                  String url, String username, String password) {
                this.name = name;
                this.con = con;
                this.driver = driver;
                this.url = url;
                this.username = username;
                this.password = password;
            }

            public void run() {
                dispose();
                MyFrame waitDlg = new WaitDialog();
                waitDlg.showCentered();
                Thread thr = new Thread(new BrowserOpener2(name, con, driver, url, username, password, waitDlg));
                thr.setPriority(Thread.MIN_PRIORITY);
                thr.setDaemon(true);
                thr.start();
            }
        }

        private class BrowserOpener2 implements Runnable {
            private String name;
            private Connection con;
            private String driver;
            private String url;
            private String username;
            private String password;
            private MyFrame waitDlg;

            public BrowserOpener2(String name, Connection con, String driver,
                                  String url, String username, String password,
                                  MyFrame waitDlg) {
                this.name = name;
                this.con = con;
                this.driver = driver;
                this.url = url;
                this.username = username;
                this.password = password;
                this.waitDlg = waitDlg;
            }

            public void run() {
                JDBCDatabase db = JDBCDatabase.create(driver, url, username, password, name, con);
                // The BrowserFrame constructor is going to call
                // Database.getRootNode(), which can be a very slow operation.
                // Better to call it here first, in a background thread, so
                // that this operation doesn't block the UI.
                db.getRootNode();
                SwingUtilities.invokeLater(new BrowserOpener3(waitDlg, db));
            }
        }

        private class BrowserOpener3 implements Runnable {
            private MyFrame waitDlg;
            private JDBCDatabase db;

            public BrowserOpener3(MyFrame waitDlg, JDBCDatabase db) {
                this.waitDlg = waitDlg;
                this.db = db;
            }

            public void run() {
                opencb.databaseOpened(db);
                waitDlg.dispose();
                Main.backgroundJobEnded();
            }
        }

        private static class WaitDialog extends MyFrame {
            public WaitDialog() {
                super("Opening JDBC Connection");
                Container c = getContentPane();
                c.setLayout(new MyGridBagLayout());
                MyGridBagConstraints gbc = new MyGridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.insets = new Insets(10, 20, 10, 20);
                c.add(new JLabel("Loading object information; please wait..."), gbc);
                pack();
            }
        }

        private static class ConfigList extends AbstractListModel<String>
                                        implements ComboBoxModel<String> {
            private Preferences prefs = Preferences.getPreferences();
            private ArrayList<Preferences.ConnectionConfig> configs = new ArrayList<Preferences.ConnectionConfig>();
            private String selectedName;

            public ConfigList() {
                load();
                if (configs.isEmpty())
                    selectedName = "";
                else
                    selectedName = configs.get(0).name;
            }
            
            public void reload() {
                int sz = configs.size();
                configs.clear();
                if (sz > 0)
                    fireIntervalRemoved(this, 0, sz - 1);
                load();
                sz = configs.size();
                if (sz > 0)
                    fireIntervalAdded(this, 0, sz - 1);
            }

            private void load() {
                for (Preferences.ConnectionConfig config : prefs.getConnectionConfigs())
                    configs.add(config);
            }

            // ListModel methods
            public int getSize() {
                return configs.size();
            }

            public String getElementAt(int index) {
                return configs.get(index).name;
            }

            // ComboBoxModel methods
            public void setSelectedItem(Object item) {
                selectedName = (String) item;
            }

            public Object getSelectedItem() {
                return selectedName;
            }

            // My methods
            public void put(Preferences.ConnectionConfig config) {
                int index = configs.indexOf(config);
                if (index == 0) {
                    configs.set(0, config);
                    fireContentsChanged(this, 0, 0);
                } else if (index != -1) {
                    configs.remove(index);
                    fireIntervalRemoved(this, index, index);
                    configs.add(0, config);
                    fireIntervalAdded(this, 0, 0);
                } else {
                    configs.add(0, config);
                    fireIntervalAdded(this, 0, 0);
                }
                prefs.putConnectionConfig(config);
                prefs.write();
            }

            public Preferences.ConnectionConfig get(String name) {
                int index = configs.indexOf(new Preferences.ConnectionConfig(name));
                if (index == -1)
                    return null;
                else
                    return configs.get(index);
            }

            public void remove(String name) {
                Preferences.ConnectionConfig tempConfig = new Preferences.ConnectionConfig(name);
                int index = configs.indexOf(tempConfig);
                if (index != -1) {
                    configs.remove(index);
                    fireIntervalRemoved(this, index, index);
                }
                prefs.removeConnectionConfig(tempConfig);
                prefs.write();
            }
        }
    }

    private static Connection doConnect(String driver, String url, String username, String password) throws SQLException {
        if (driver.startsWith("oracle.")) {
            Properties props = new Properties();
            props.put("user", username);
            props.put("password", password);
            // This is to stop Oracle >= 9 drivers from returning
            // java.sql.Date objects for DATE columns, but stick with
            // java.sql.Timestamp like Oracle 8i.
            props.put("oracle.jdbc.V8Compatible", "true");
            return DriverManager.getConnection(url, props);
        } else {
            return DriverManager.getConnection(url, username, password);
        }
    }

    ///////////////////////
    ///// LOB support /////
    ///////////////////////

    protected Object wrapLob(Table table, String[] cnames, Object[] values,
                             int index, TypeSpec spec, Object o) {
        boolean simple = lobsOutliveResultSets() || table == null;
        if (o instanceof Blob) {
            Blob blob = (Blob) o;
            if (simple)
                return new SimpleBlobWrapper(blob);
            else
                return new QueryBlobWrapper(table, cnames, values, index, blob);
        } else if (o instanceof Clob) {
            Clob clob = (Clob) o;
            if (simple)
                return new SimpleClobWrapper(clob);
            else
                return new QueryClobWrapper(table, cnames, values, index, clob);
        } else
            return db2nav(spec, o);
    }

    private static String lobString(Object o) {
        StringBuffer buf = new StringBuffer();
        if (o instanceof Blob)
            buf.append("Blob");
        else
            buf.append("Clob");
        buf.append(" (length = ");
        try {
            if (o instanceof Blob)
                buf.append(((Blob) o).length());
            else
                buf.append(((Clob) o).length());
        } catch (SQLException e) {
            buf.append("?");
        }
        buf.append(")");
        return buf.toString();
    }

    private static class SimpleBlobWrapper implements BlobWrapper {
        private Blob blob;
        private String s;
        public SimpleBlobWrapper(Blob blob) {
            this.blob = blob;
            s = lobString(blob);
        }
        public String toString() {
            return s;
        }
        public byte[] load() {
            return loadBlob(blob);
        }
    }

    private static class SimpleClobWrapper implements ClobWrapper {
        private Clob clob;
        private String s;
        public SimpleClobWrapper(Clob clob) {
            this.clob = clob;
            s = lobString(clob);
        }
        public String toString() {
            return s;
        }
        public String load() {
            return loadClob(clob);
        }
    }

    private class QueryLobWrapper {
        private Table table;
        private String[] columnNames;
        private Object[] values;
        int index;
        private String s;
        public QueryLobWrapper(Table table, String[] columnNames,
                               Object[] values, int index, Object o) {
            this.table = table;
            this.columnNames = columnNames;
            this.values = values;
            this.index = index;
            s = lobString(o);
        }
        public String toString() {
            return s;
        }
        protected Object load2() {
            // NOTE: This method assumes that columnNames contains all the
            // table's primary key columns. If the table does not have a PK,
            // all the table's columns should be present.
            // This relies on runQuery() to only populate 'table' if those
            // conditions are met; it has to do that anyway in order to be able
            // to decide whether to return the result in a Data or Table object.
            StringBuffer buf = new StringBuffer();
            buf.append("select ");
            buf.append(quote(columnNames[index]));
            buf.append(" from ");
            buf.append(table.getQualifiedName());
            buf.append(" where ");

            PrimaryKey pk = table.getPrimaryKey();
            String[] pkColNames;
            if (pk != null) {
                int n = pk.getColumnCount();
                pkColNames = new String[n];
                for (int i = 0; i < n; i++)
                    pkColNames[i] = pk.getColumnName(i);
            } else
                pkColNames = table.getColumnNames();

            ArrayList<Integer> keyIndexInTable = new ArrayList<Integer>();
            ArrayList<Integer> keyIndexInData = new ArrayList<Integer>();
            TypeSpec[] specs = table.getTypeSpecs();
            boolean first = true;
            String[] colNames = table.getColumnNames();

            for (int i = 0; i < pkColNames.length; i++) {
                String name = pkColNames[i];
                int j = 0;
                while (j < specs.length)
                    if (name.equalsIgnoreCase(colNames[j]))
                        break;
                    else
                        j++;
                if (j == specs.length)
                    continue;

                TypeSpec spec = specs[j];
                if (spec.type == TypeSpec.LONGVARCHAR
                        || spec.type == TypeSpec.LONGVARNCHAR
                        || spec.type == TypeSpec.LONGVARRAW)
                    continue;

                if (first)
                    first = false;
                else
                    buf.append(" and ");
                buf.append(quote(name));
                Object obj = values[j];
                if (obj == null && needsIsNull())
                    buf.append(" is null");
                else {
                    buf.append(" = ?");
                    keyIndexInTable.add(i);
                    keyIndexInData.add(j);
                }
                break;
            }

            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                stmt = con.prepareStatement(buf.toString());
                for (int i = 0; i < keyIndexInTable.size(); i++) {
                    int ti = keyIndexInTable.get(i);
                    int di = keyIndexInData.get(i);
                    setObject(stmt, ti + 1, di, values[di], table);
                }
                rs = stmt.executeQuery();
                if (rs.next()) {
                    Object o = rs.getObject(1);
                    if (o instanceof Blob)
                        return loadBlob((Blob) o);
                    else if (o instanceof Clob)
                        return loadClob((Clob) o);
                    else
                        return o;
                } else {
                    MessageBox.show(
                            new Exception("LOB's table row was deleted."));
                    return null;
                }
            } catch (SQLException e) {
                MessageBox.show("Could not load LOB.", e);
                return null;
            } finally {
                if (rs != null)
                    try {
                        rs.close();
                    } catch (SQLException e) {}
                if (stmt != null)
                    try {
                        stmt.close();
                    } catch (SQLException e) {}
            }
        }
    }

    private class QueryBlobWrapper extends QueryLobWrapper
                                   implements BlobWrapper {
        public QueryBlobWrapper(Table table, String[] columnNames,
                               Object[] values, int index, Blob blob) {
            super(table, columnNames, values, index, blob);
        }
        public byte[] load() {
            return (byte[]) load2();
        }
    }

    private class QueryClobWrapper extends QueryLobWrapper
                                   implements ClobWrapper {
        public QueryClobWrapper(Table table, String[] columnNames,
                               Object[] values, int index, Clob clob) {
            super(table, columnNames, values, index, clob);
        }
        public String load() {
            return (String) load2();
        }
    }

    private static byte[] loadBlob(Blob blob) {
        InputStream is = null;
        try {
            is = blob.getBinaryStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            try {
                int bytesRead;
                while ((bytesRead = is.read(buf)) > 0)
                    bos.write(buf, 0, bytesRead);
            } catch (IOException e) {
                MessageBox.show("I/O error while reading Blob value!", e);
            }
            return bos.toByteArray();
        } catch (SQLException e) {
            MessageBox.show("Reading Blob value failed!", e);
            return new byte[0];
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {}
        }
    }

    private static String loadClob(Clob clob) {
        Reader r = null;
        try {
            r = clob.getCharacterStream();
            StringBuffer sbuf = new StringBuffer();
            char[] cbuf = new char[4096];
            try {
                int n;
                while ((n = r.read(cbuf)) != -1)
                    sbuf.append(cbuf, 0, n);
            } catch (IOException e) {
                MessageBox.show("I/O error while reading Clob value!", e);
            }
            return sbuf.toString();
        } catch (SQLException e) {
            MessageBox.show("Reading Clob value failed!", e);
            return "";
        } finally {
            if (r != null)
                try {
                    r.close();
                } catch (IOException e) {}
        }
    }


    /////////////////////////////////////////////////////////////////////
    ///// Miscellaneous stuff; formerly InteractiveMetaDriver stuff /////
    /////////////////////////////////////////////////////////////////////

    protected String unmangleKeyName(String name) {
        return name;
    }

    protected boolean needsIsNull() {
        return false;
    }

    protected boolean resultSetContainsTableInfo() {
        return true;
    }

    protected boolean lobsOutliveResultSets() {
        return true;
    }

    protected String[] getJavaTypes(String qualifiedName)
                                                    throws NavigatorException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("select * from " + qualifiedName + " where 1 = 2");
            ResultSetMetaData rsmd = stmt.getMetaData();
            int columns = rsmd.getColumnCount();
            String[] javaTypes = new String[columns];
            for (int i = 0; i < columns; i++)
                javaTypes[i] = rsmd.getColumnClassName(i + 1);
            return javaTypes;
        } catch (SQLException e) {
            throw new NavigatorException(e);
        } finally {
            if (stmt != null)
                try {
                    stmt.close();
                } catch (SQLException e) {}
        }
    }

    private String q = null;
    protected String getIdentifierQuoteString() {
        if (q == null)
            try {
                q = con.getMetaData().getIdentifierQuoteString();
            } catch (SQLException e) {
                q = " ";
            }
        return q;
    }

    /**
     * Guess a table's qualified name, given nothing but a bare name.
     */
    protected String qualifyName(String name) {
        return name;
    }

    boolean weKnowWhatToShow = false;
    boolean showCatalogs;
    boolean showSchemas;

    private void findOutWhatToShow() {
        if (weKnowWhatToShow)
            return;
        try {
            DatabaseMetaData dbmd = con.getMetaData();
            try {
                showCatalogs = dbmd.supportsCatalogsInDataManipulation();
            } catch (SQLException e) {
                showCatalogs = true;
            }
            try {
                showSchemas = dbmd.supportsSchemasInDataManipulation();
            } catch (SQLException e) {
                showSchemas = true;
            }
        } catch (SQLException e) {
            showCatalogs = true;
            showSchemas = true;
        }
        weKnowWhatToShow = true;
    }

    protected boolean showCatalogs() {
        findOutWhatToShow();
        return showCatalogs;
    }

    protected boolean showSchemas() {
        findOutWhatToShow();
        return showSchemas;
    }

    protected boolean showTableTypes() {
        return true;
    }

    private class BackgroundLoader implements Runnable, Data.StateListener {
        private BackgroundLoadData data;
        private Statement stmt;
        private ResultSet rs;
        private Table table;
        private String[] columnNames;
        private int state;

        public BackgroundLoader(BackgroundLoadData data,
                                Statement stmt, ResultSet rs,
                                Table table, String[] columnNames) {
            this.data = data;
            this.stmt = stmt;
            this.rs = rs;
            this.table = table;
            this.columnNames = columnNames;
            state = Data.LOADING;
            data.addStateListener(this);
        }

        public void run() {
            Main.log(3, "background loader started");
            try {
                int columns = data.getColumnCount();
                boolean noClone = lobsOutliveResultSets() || table == null;
                while (rs.next()) {
                    synchronized (this) {
                        while (state == Data.PAUSED)
                            try {
                                wait();
                            } catch (InterruptedException e) {}
                        if (state == Data.FINISHED)
                            break;
                    }
                    Object[] row = new Object[columns];
                    for (int i = 0; i < columns; i++)
                        row[i] = rs.getObject(i + 1);
                    Object[] orig_row = noClone ? null : row.clone();
                    for (int i = 0; i < columns; i++) {
                        Object o = row[i];
                        row[i] = wrapLob(table, columnNames, orig_row, i,
                                                        data.getTypeSpec(i), o);
                    }
                    data.addRow(row);
                }
            } catch (SQLException e) {
                MessageBox.show("An exception occurred while "
                                + "loading a query result:", e);
            }
            Main.log(3, Integer.toString(data.getRowCount()) + " rows loaded");

            if (rs != null)
                try {
                    rs.close();
                } catch (SQLException e) {}
            if (stmt != null)
                try {
                    stmt.close();
                } catch (SQLException e) {}

            data.removeStateListener(this);
            data.setState(Data.FINISHED);
        }

        public void stateChanged(int state, int rows) {
            synchronized (this) {
                if (this.state == Data.FINISHED || this.state == state)
                    return;
                if (this.state == Data.PAUSED) {
                    this.state = state;
                    notify();
                } else
                    this.state = state;
            }
        }
    }
}
