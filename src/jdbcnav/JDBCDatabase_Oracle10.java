package jdbcnav;

import java.io.*;
import java.sql.*;
import java.util.*;

import jdbcnav.model.*;
import jdbcnav.util.*;


public class JDBCDatabase_Oracle10 extends JDBCDatabase {
    public JDBCDatabase_Oracle10(String name, Connection con) {
	super(name, con);
    }

    /**
     * The Oracle 8i JDBC Driver (classes12.zip) does not handle nulls
     * properly in 'where' clauses in PreparedStatements. That is,
     * 'where foo = ?' with a value of null does not work; you must use
     * 'where foo is null'.
     */
    protected boolean needsIsNull() {
	return true;
    }

    /**
     * The Oracle 8i JDBC Driver (classes12.zip) does not return anything in
     * ResultSetMetaData.getCatalogName(), getSchemaName(), and getTableName(),
     * which makes it impossible (without parsing SQL ourselves, anyway) to
     * support allowTable=true in Database.runQuery().
     */
    protected boolean resultSetContainsTableInfo() {
	return false;
    }

    /**
     * The Oracle 8i JDBC Driver (classes12.zip) freaks out on
     * PreparedStatement.getMetaData(), which we would like to use to find a
     * table's Java types when creating the Table object.
     */
    protected String[] getJavaTypes(String qualifiedName)
						    throws NavigatorException {
	// This is a bit icky. What I would *like* to do it use
	// PreparedStatement.getMetaData() to find out about a table's Java
	// type mapping without having to execute a statement, but the Oracle
	// 8i JDBC driver freaks out when I try to do that.
	// So, I create a query that is guaranteed to return no rows at all,
	// and run that. Hopefully this'll be reasonably efficient, too!

	Statement stmt = null;
	ResultSet rs = null;
	try {
	    stmt = con.createStatement();
	    rs = stmt.executeQuery(
			    "select * from " + qualifiedName + " where 1 = 2");
	    ResultSetMetaData rsmd = rs.getMetaData();
	    int columns = rsmd.getColumnCount();
	    String[] javaTypes = new String[columns];
	    for (int i = 0; i < columns; i++)
		javaTypes[i] = rsmd.getColumnClassName(i + 1);
	    return javaTypes;
	} catch (SQLException e) {
	    throw new NavigatorException(e);
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

    /**
     * JDBCDatabase.getIndexes() uses DatabaseMetaData.getIndexInfo(),
     * which, with Oracle, causes the table to be analyzed. This has two
     * undesirable side effects: first, it can be slow, and second, it can
     * totally mess up query performance in the case where you don't usually
     * analyze tables and prefer to rely on the RBO. Once even one of the
     * tables involved in a multi-table query has been analyzed, the CBO is
     * used, and the CBO will always use un-analyzed tables last. Because of
     * this, you should generally either analyze all your tables, or none of
     * them, and the behavior of DBMD.getIndexInfo() can mess you up big time
     * if you prefer not to analyze tables generally.
     * So, we override JDBCDatabase.getIndexes() and use an Oracle-specific
     * way of getting the index info we need, *without* any side effects.
     * <br>
     * By the way, in case you have table statistics that were accidentally
     * generated and are messing up your query performance, the way to get
     * rid of this information is 'ANALYZE TABLE <table> DELETE STATISTICS'.
     */
    protected Index[] getIndexes(Table t) throws NavigatorException {
	JDBCTable table = (JDBCTable) t;
	PreparedStatement stmt = null;
	ResultSet rs = null;
	try {
	    String tableName = table.getName();
	    String schema = table.getSchema();
	    PrimaryKey pk = table.getPrimaryKey();
	    String pkName = pk == null ? null : pk.getName();
	    ArrayList maybeIndexes = new ArrayList();

	    stmt = con.prepareStatement("select index_name, uniqueness from user_indexes where table_owner = ? and table_name = ? order by index_name");
	    stmt.setString(1, schema);
	    stmt.setString(2, tableName);
	    rs = stmt.executeQuery();
	    while (rs.next()) {
		String indexName = rs.getString(1);
		if (indexName == null || pkName != null && pkName.equals(indexName))
		    continue;
		BasicIndex index = new BasicIndex();
		index.setName(indexName);
		index.setUnique("UNIQUE".equals(rs.getString(2)));
		maybeIndexes.add(index);
	    }
	    rs.close();
	    rs = null;
	    stmt.close();
	    stmt = null;

	    stmt = con.prepareStatement("select column_name from user_ind_columns where index_name = ? order by column_position");
	    ArrayList indexes = new ArrayList();
	    for (Iterator iter = maybeIndexes.iterator(); iter.hasNext();) {
		BasicIndex index = (BasicIndex) iter.next();
		ArrayList columns = new ArrayList();
		stmt.setString(1, index.getName());
		rs = stmt.executeQuery();
		while (rs.next())
		    columns.add(rs.getString(1));
		rs.close();
		rs = null;
		// If there are no columns, it's probably a BLOB or CLOB
		// index. Dunno what those are, actually, but we don't want
		// them reported as indexes in the table details window.
		if (!columns.isEmpty()) {
		    index.setColumns((String[]) columns.toArray(new String[0]));
		    indexes.add(index);
		}
	    }
	    stmt.close();
	    stmt = null;
	    return (Index[]) indexes.toArray(new Index[0]);
	} catch (SQLException e) {
	    throw new NavigatorException(e);
	} finally {
	    if (rs != null)
		try {
		    rs.close();
		} catch (SQLException e) {
		    //
		}
	    if (stmt != null)
		try {
		    stmt.close();
		} catch (SQLException e) {
		    //
		}
	}
    }

    private boolean[] makeNeedsStreaming(String catalog, String schema,
		    String name, String[] columns) throws NavigatorException {
	ResultSet rs = null;
	boolean[] res = new boolean[columns.length];
	boolean[] done = new boolean[columns.length];
	int leftToDo = columns.length;

	TreeMap map = new TreeMap();
	for (int i = 0; i < columns.length; i++)
	    map.put(columns[i].toUpperCase(), new Integer(i));

	try {
	    DatabaseMetaData dbmd = con.getMetaData();
	    rs = dbmd.getColumns(catalog, schema, name, null);
	    while (rs.next()) {
		String colname = rs.getString("COLUMN_NAME");
		Integer col = (Integer) map.get(colname.toUpperCase());
		if (col != null) {
		    int c = col.intValue();
		    if (!done[c]) {
			String type = rs.getString("TYPE_NAME");
			res[c] = type.equals("LONG")
			      || type.equals("LONG RAW")
			      || type.equals("CLOB")
			      || type.equals("BLOB");
			done[c] = true;
			if (--leftToDo == 0)
			    return res;
		    }
		}
	    }
	} catch (SQLException e) {
	    throw new NavigatorException(e);
	} finally {
	    if (rs != null)
		try {
		    rs.close();
		} catch (SQLException e) {}
	}

	StringBuffer message = new StringBuffer();
	message.append("Column");
	if (leftToDo > 1)
	    message.append("s have");
	else
	    message.append(" has");
	message.append(" gone missing: ");
	boolean comma = false;
	for (int i = 0; i < columns.length; i++)
	    if (!done[i]) {
		if (comma)
		    message.append(", ");
		else
		    comma = true;
		message.append(columns[i]);
	    }
	throw new NavigatorException(message.toString());
    }

    private interface HasStreamingInfo {
	boolean[] getNeedsStreaming();
    }

    private class OracleTable extends JDBCTable implements HasStreamingInfo {
	private boolean[] needsStreaming;
	public OracleTable(String qualifiedName) throws NavigatorException {
	    super(qualifiedName);
	    needsStreaming = makeNeedsStreaming(getCatalog(), getSchema(),
						getName(), getColumnNames());
	}
	public boolean[] getNeedsStreaming() {
	    return needsStreaming;
	}
    }

    private class OraclePartialTable extends PartialTable
						implements HasStreamingInfo {
	private boolean[] needsStreaming;
	public OraclePartialTable(String q, Table t, Data d)
						throws NavigatorException {
	    super(q, t, d);
	    needsStreaming = makeNeedsStreaming(getCatalog(), getSchema(),
						getName(), getColumnNames());
	}
	public boolean[] getNeedsStreaming() {
	    return needsStreaming;
	}
    }

    protected JDBCTable newJDBCTable(String qualifiedName)
						throws NavigatorException {
	return new OracleTable(qualifiedName);
    }

    protected PartialTable newPartialTable(String q, Table t, Data d)
						throws NavigatorException {
	return new OraclePartialTable(q, t, d);
    }

    protected void setObject(PreparedStatement stmt, int index,
			     int dbtable_col, Object o,
			     Table table) throws SQLException {
	if (((HasStreamingInfo) table).getNeedsStreaming()[dbtable_col]) {
	    if (o == null) {
		// Oracle returns 1111 (OTHER) for BLOB and CLOB columns;
		// passing this value to PreparedStatement.setObject(int index,
		// Object o, int sqlType) causes the JDBC driver to throw a
		// SQLException with the message "Invalid column type".
		// I avoid this error by calling setString() or setBytes()
		// instead of setObject().
		// This code also executes for LONG and LONG RAW columns, which
		// is just as well, since the code below does not handle NULL
		// anyway.
		String dbType = table.getDbTypes()[dbtable_col];
		if ("CLOB".equals(dbType) || "LONG".equals(dbType))
		    stmt.setString(index, null);
		else
		    stmt.setBytes(index, null);
	    } else if (o instanceof String) {
		String s = (String) o;
		stmt.setCharacterStream(index, new StringReader(s), s.length());
	    } else if (o instanceof byte[]) {
		byte[] ba = (byte[]) o;
		stmt.setBinaryStream(index, new ByteArrayInputStream(ba),
								ba.length);
	    } else
		super.setObject(stmt, index, dbtable_col, o, table);
	} else
	    super.setObject(stmt, index, dbtable_col, o, table);
    }

    /**
     * Attempt to find a table's fully qualified name, given only its
     * bare name. With Oracle databases, we should be OK by prepending
     * the session user.
     */
    protected String qualifyName(String name) {
	Statement stmt = null;
	ResultSet rs = null;
	try {
	    stmt = con.createStatement();
	    rs = stmt.executeQuery("select user from dual");
	    if (rs.next())
		return rs.getString(1) + "." + name;
	    else
		return name;
	} catch (SQLException e) {
	    return name;
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

    protected boolean showCatalogs() {
	return false;
    }

    protected boolean showSchemas() {
	return true;
    }

    protected boolean showTableTypes() {
	return true;
    }
}
