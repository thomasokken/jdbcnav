package jdbcnav;

import java.io.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

import jdbcnav.model.*;
import jdbcnav.util.*;


public class JDBCDatabase_Oracle extends JDBCDatabase {
    public JDBCDatabase_Oracle(String name, String driver, Connection con) {
	super(name, driver, con);
	/* The following code won't work because reflection won't let
	 * me call OracleConnection.setSessionTimeZone(), despite the fact
	 * that that is a public method. Go figure.
	 * So, I have to access the class explicitly, which means you
	 * can no longer compile this code without having the Oracle
	 * JDBC driver in the classpath. Sigh.
	try {
	    Method m = con.getClass().getMethod("setSessionTimeZone",
			    new Class[] { String.class });
	    m.invoke(con, new Object[] { TimeZone.getDefault().getID() });
	} catch (Exception e) {
	    e.printStackTrace();
	}
	*/
	try {
	    ((oracle.jdbc.OracleConnection) con).setSessionTimeZone(
					    TimeZone.getDefault().getID());
	} catch (SQLException e) {
	    e.printStackTrace();
	} catch (NoSuchMethodError e) {
	    // Older version, which does not support TIMESTAMPLTZ anyway,
	    // so we don't care.
	}
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
		String dbType = table.getTypeSpecs()[dbtable_col].jdbcDbType;
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

    protected TypeSpec makeTypeSpec(String dbType, Integer size, Integer scale,
				    int sqlType, String javaType) {
	if (javaType == null) {
	    if (dbType == null) {
		// UROWID, from ResultSetMetaData
		dbType = "UROWID";
		sqlType = Types.OTHER;
		javaType = "oracle.sql.ROWID";
	    } else if (dbType.equals("BINARY_FLOAT")) {
		sqlType = Types.OTHER;
		javaType = "java.lang.Float";
	    } else if (dbType.equals("BINARY_DOUBLE")) {
		sqlType = Types.OTHER;
		javaType = "java.lang.Double";
	    } else {
		// UROWID (?)
		sqlType = Types.OTHER;
		javaType = "oracle.sql.ROWID";
	    }
	}

	TypeSpec spec = super.makeTypeSpec(dbType, size, scale, sqlType,
								javaType);

	if (spec.jdbcJavaType.equals("byte[]"))
	    spec.jdbcJavaClass = new byte[1].getClass();

	if (dbType.equals("CHAR")) {
	    spec.type = TypeSpec.CHAR;
	    spec.size = size.intValue();
	} else if (dbType.equals("VARCHAR2")) {
	    spec.type = TypeSpec.VARCHAR;
	    spec.size = size.intValue();
	} else if (dbType.equals("NCHAR")) {
	    spec.type = TypeSpec.NCHAR;
	    spec.size = size.intValue();
	} else if (dbType.equals("NVARCHAR2")) {
	    spec.type = TypeSpec.VARNCHAR;
	    spec.size = size.intValue();
	} else if (dbType.equals("NUMBER")) {
	    if (scale == null
		    || scale.intValue() == -127 && size.intValue() == 0) {
		spec.type = TypeSpec.FLOAT;
		// TODO: Is it really 38 decimal digits,
		// or is it actually 126 bits?
		spec.size = 38;
		spec.size_in_bits = false;
		spec.min_exp = -130;
		spec.max_exp = 125;
		spec.exp_of_2 = false;
	    } else if (scale.intValue() == -127) {
		// FLOAT and NUMBER as returned by ResultSetMetaData
		spec.type = TypeSpec.FLOAT;
		spec.size = size.intValue();
		spec.size_in_bits = true;
		spec.min_exp = -130;
		spec.max_exp = 125;
		spec.exp_of_2 = false;
	    } else {
		spec.type = TypeSpec.FIXED;
		spec.size = size.intValue();
		spec.size_in_bits = false;
		spec.scale = scale.intValue();
		spec.scale_in_bits = false;
	    }
	} else if (dbType.equals("FLOAT")) {
	    spec.type = TypeSpec.FLOAT;
	    spec.size = size.intValue();
	    spec.size_in_bits = true;
	    spec.min_exp = -130;
	    spec.max_exp = 125;
	    spec.exp_of_2 = false;
	} else if (dbType.equals("BINARY_FLOAT")) {
	    spec.type = TypeSpec.FLOAT;
	    spec.size = 24;
	    spec.size_in_bits = true;
	    spec.min_exp = -127;
	    spec.max_exp = 127;
	    spec.exp_of_2 = true;
	} else if (dbType.equals("BINARY_DOUBLE")) {
	    spec.type = TypeSpec.FLOAT;
	    spec.size = 54;
	    spec.size_in_bits = true;
	    spec.min_exp = -1023;
	    spec.max_exp = 1023;
	    spec.exp_of_2 = true;
	} else if (dbType.equals("LONG")) {
	    spec.type = TypeSpec.LONGVARCHAR;
	} else if (dbType.equals("LONG RAW")) {
	    spec.type = TypeSpec.LONGVARRAW;
	} else if (dbType.equals("RAW")) {
	    spec.type = TypeSpec.VARRAW;
	    spec.size = size.intValue();
	} else if (javaType.equals("java.sql.Timestamp")) {
	    spec.type = TypeSpec.TIMESTAMP;
	    spec.size = 0;
	} else if (javaType.equals("oracle.sql.TIMESTAMP")
		|| javaType.equals("oracle.sql.TIMESTAMPLTZ")) {
	    spec.type = TypeSpec.TIMESTAMP;
	    spec.size = scale.intValue();
	} else if (javaType.equals("oracle.sql.TIMESTAMPTZ")) {
	    spec.type = TypeSpec.TIMESTAMP_TZ;
	    spec.size = scale.intValue();
	} else if (dbType.equals("INTERVALYM")
		|| dbType.startsWith("INTERVAL YEAR")) {
	    spec.type = TypeSpec.INTERVAL_YM;
	    spec.size = size.intValue();
	} else if (dbType.equals("INTERVALDS")
		|| dbType.startsWith("INTERVAL DAY")) {
	    spec.type = TypeSpec.INTERVAL_DS;
	    spec.size = size.intValue();
	    spec.scale = scale.intValue();
	} else if (dbType.equals("BLOB")) {
	    spec.type = TypeSpec.LONGVARRAW;
	} else if (dbType.equals("CLOB")) {
	    spec.type = TypeSpec.LONGVARCHAR;
	} else if (dbType.equals("NCLOB")) {
	    spec.type = TypeSpec.LONGVARNCHAR;
	} else {
	    // BFILE, ROWID, UROWID, or something new.
	    // Don't know how to handle them so we tag them UNKNOWN,
	    // which will cause the script generator to pass them on
	    // uninterpreted and unchanged.
	    spec.type = TypeSpec.UNKNOWN;
	}

	if (dbType.startsWith("INTERVAL YEAR"))
	    spec.native_representation = "INTERVAL YEAR(" + size + ") TO MONTH";
	else if (dbType.startsWith("INTERVAL DAY"))
	    spec.native_representation = "INTERVAL DAY(" + size
		+ ") TO SECOND(" + scale + ")";
	else if (javaType.startsWith("oracle.sql.TIMESTAMP")) {
	    spec.native_representation = "TIMESTAMP(" + scale + ")";
	    if (javaType.endsWith("LTZ"))
		spec.native_representation += " WITH LOCAL TIME ZONE";
	    else if (javaType.endsWith("TZ"))
		spec.native_representation += " WITH TIME ZONE";
	} else {
	    if (!dbType.equals("NUMBER")
		    && !dbType.equals("CHAR")
		    && !dbType.equals("VARCHAR2")
		    && !dbType.equals("NCHAR")
		    && !dbType.equals("NVARCHAR2")
		    && !dbType.equals("RAW")
		    && !dbType.equals("FLOAT")) {
		size = null;
		scale = null;
	    } else if (dbType.equals("NUMBER")) {
		if (scale == null)
		    size = null;
		else if (scale.intValue() == 0)
		    scale = null;
	    }
	    if (size == null)
		spec.native_representation = dbType;
	    else if (scale == null)
		spec.native_representation = dbType + "(" + size + ")";
	    else
		spec.native_representation = dbType + "(" + size + ", "
							  + scale + ")";
	}

	return spec;
    }

    public String objectToString(TypeSpec spec, Object o) {
	if (o == null)
	    return super.objectToString(spec, o);

	String className = spec.jdbcJavaType;

	if (className.equals("oracle.sql.BFILE")) {
	    try {
		Class bfileClass = o.getClass();
		Method m = bfileClass.getMethod("getDirAlias", null);
		String dir = (String) m.invoke(o, null);
		m = bfileClass.getMethod("getName", null);
		String name = (String) m.invoke(o, null);
		return "Bfile ('" + dir + "', '" + name + "')";
	    } catch (NoSuchMethodException e) {
		// From Class.getMethod()
		// Should not happen
		return o.toString();
	    } catch (IllegalAccessException e) {
		// From Method.invoke()
		// Should not happen
		return o.toString();
	    } catch (InvocationTargetException e) {
		// From Method.invoke()
		// Should be a SQLException from BFILE.getName()
		return "Bfile (name = ?)";
	    }
	}

	return super.objectToString(spec, o);
    }

    protected Object db2nav(TypeSpec spec, Object o) {
	if (o == null)
	    return null;
	if (spec.jdbcJavaClass == Timestamp.class) {
	    Timestamp ts = (Timestamp) o;
	    return new DateTime(ts.getTime(), 0, null);
	}
	Class klass = o.getClass();
	if (spec.jdbcJavaType.equals("oracle.sql.TIMESTAMP")) {
	    try {
		Method m = klass.getMethod("getBytes", null);
		byte[] b = (byte[]) m.invoke(o, null);
		m = klass.getMethod("toString",
			new Class[] { new byte[1].getClass() });
		String s = (String) m.invoke(null, new Object[] { b });
		return new DateTime(s);
	    } catch (Exception e) {
		return o;
	    }
	}
	if (spec.jdbcJavaType.equals("oracle.sql.TIMESTAMPTZ")) {
	    try {
		Method m = klass.getMethod("getBytes", null);
		byte[] b = (byte[]) m.invoke(o, null);
		m = klass.getMethod("toString",
		    new Class[] { Connection.class, new byte[1].getClass() });
		String s = (String) m.invoke(null, new Object[] { con, b });
		return new DateTime(s);
	    } catch (Exception e) {
		return o;
	    }
	}
	if (spec.jdbcJavaType.equals("oracle.sql.TIMESTAMPLTZ")) {
	    try {
		Method m = klass.getMethod("getBytes", null);
		byte[] b = (byte[]) m.invoke(o, null);
		m = klass.getMethod("toString",
		    new Class[] { Connection.class, new byte[1].getClass() });
		String s = (String) m.invoke(null, new Object[] { con, b });
		return new DateTime(s);
	    } catch (Exception e) {
		return o;
	    }
	}
	if (spec.jdbcJavaType.equals("oracle.sql.INTERVALDS")) {
	    String s = o.toString();
	    // The fractional-seconds part is a number, not a fraction; this
	    // means that '0 0:0:0.1' is one nanosecond, NOT one-tenth of a
	    // second!
	    // The jdbcnav.model.Interval(String) constructor uses a more
	    // normal approach... So I have to fix the string by left-padding
	    // the fraction part before passing it on.
	    int dot = s.lastIndexOf('.');
	    if (dot != -1) {
		int fracdigits = s.length() - dot - 1;
		if (fracdigits < 9)
		    s = s.substring(0, dot + 1)
			+ "000000000".substring(fracdigits)
			+ s.substring(dot + 1);
	    }
	    return new Interval(spec, s);
	}
	if (spec.jdbcJavaType.equals("oracle.sql.INTERVALYM")) {
	    return new Interval(spec, o.toString());
	}
	return super.db2nav(spec, o);
    }

    protected Object nav2db(TypeSpec spec, Object o) {
	if (o == null)
	    return null;
	if (spec.jdbcJavaClass == Timestamp.class) {
	    DateTime dt = (DateTime) o;
	    Timestamp ts = new Timestamp(dt.time);
	    ts.setNanos(dt.nanos);
	    return ts;
	}
	if (spec.jdbcJavaType.equals("oracle.sql.TIMESTAMP")) {
	    DateTime dt = (DateTime) o;
	    Timestamp ts = new Timestamp(dt.time);
	    ts.setNanos(dt.nanos);
	    try {
		Class c = Class.forName("oracle.sql.TIMESTAMP");
		Constructor cnstr = c.getConstructor(new Class[] { Timestamp.class });
		return cnstr.newInstance(new Object[] { ts });
	    } catch (Exception e) {
		return o;
	    }
	}
	if (spec.jdbcJavaType.equals("oracle.sql.TIMESTAMPTZ")) {
	    DateTime dt = (DateTime) o;
	    Timestamp ts = new Timestamp(dt.time);
	    ts.setNanos(dt.nanos);
	    Calendar cal = new GregorianCalendar(dt.tz);
	    try {
		Class c = Class.forName("oracle.sql.TIMESTAMPTZ");
		Constructor cnstr = c.getConstructor(new Class[] { Connection.class, Timestamp.class, Calendar.class });
		return cnstr.newInstance(new Object[] { con, ts, cal });
	    } catch (Exception e) {
		return o;
	    }
	}
	if (spec.jdbcJavaType.equals("oracle.sql.TIMESTAMPLTZ")) {
	    DateTime dt = (DateTime) o;
	    Timestamp ts = new Timestamp(dt.time);
	    ts.setNanos(dt.nanos);
	    try {
		Class c = Class.forName("oracle.sql.TIMESTAMPLTZ");
		Constructor cnstr = c.getConstructor(new Class[] { Connection.class, Timestamp.class });
		return cnstr.newInstance(new Object[] { con, ts });
	    } catch (Exception e) {
		return o;
	    }
	}
	if (spec.jdbcJavaType.equals("oracle.sql.INTERVALDS")) {
	    Interval inter = (Interval) o;
	    // The oracle.sql.INTERVALDS class interprets the fractional-seconds
	    // part like a number, not like a fraction. This can lead to
	    // unexpected results: "0 0:0:0.1" means one nanosecond, NOT one-
	    // tenth of a second!
	    // Because of this, I pad the fraction part to 9 digits before
	    // passing it to the INTERVALDS constructor.
	    String s = inter.toString(spec);
	    int dot = s.lastIndexOf('.');
	    if (dot != -1) {
		int fracdigits = s.length() - dot - 1;
		if (fracdigits < 9)
		    s += "000000000".substring(fracdigits);
	    }
	    try {
		Class c = Class.forName("oracle.sql.INTERVALDS");
		Constructor cnstr = c.getConstructor(new Class[] { String.class });
		return cnstr.newInstance(new Object[] { s });
	    } catch (Exception e) {
		return o;
	    }
	}
	if (spec.jdbcJavaType.equals("oracle.sql.INTERVALYM")) {
	    Interval inter = (Interval) o;
	    try {
		Class c = Class.forName("oracle.sql.INTERVALYM");
		Constructor cnstr = c.getConstructor(new Class[] { String.class });
		return cnstr.newInstance(new Object[] { inter.toString(spec) });
	    } catch (Exception e) {
		return o;
	    }
	}
	return super.nav2db(spec, o);
    }
}
