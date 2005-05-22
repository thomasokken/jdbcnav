package jdbcnav;

import java.text.*;
import jdbcnav.model.Index;
import jdbcnav.model.PrimaryKey;
import jdbcnav.model.Table;
import jdbcnav.util.MiscUtils;

public class ScriptGenerator_Oracle extends ScriptGenerator {
    private static SimpleDateFormat dateFormat =
	new SimpleDateFormat("yyyy-MM-dd");
    private static SimpleDateFormat timeFormat =
	new SimpleDateFormat("HH:mm:ss");
    private static SimpleDateFormat dateTimeFormat =
	new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    protected String getSQLPreamble() {
	return "set scan off;\n";
    }
    protected String dateToString(java.util.Date date) {
	if (date.getClass() == java.sql.Date.class)
	    return "to_date('" + dateFormat.format(date)
			       + "', 'YYYY-MM-DD')";
	else if (date.getClass() == java.sql.Time.class)
	    return "to_date('" + timeFormat.format(date)
			       + "', 'HH24:MI:SS')";
	else
	    return "to_date('" + dateTimeFormat.format(date)
			       + "', 'YYYY-MM-DD HH24:MI:SS')";
    }
    protected String onUpdateString(String upd) {
	return null;
    }
    protected String onDeleteString(String del) {
	return del.equals("cascade") ? del : null;
    }
    protected String printType(Table table, int column) {
	String driver = table.getDatabase().getInternalDriverName();
	String name = table.getDbTypes()[column];
	Integer size = table.getColumnSizes()[column];
	Integer scale = table.getColumnScales()[column];

	if (driver.equals("Oracle")) {

	    if (!name.equals("NUMBER")
		    && !name.equals("CHAR")
		    && !name.equals("VARCHAR2")
		    && !name.equals("NCHAR")
		    && !name.equals("NVARCHAR2")
		    && !name.equals("RAW")
		    && !name.equals("FLOAT")) {
		size = null;
		scale = null;
	    } else if (name.equals("NUMBER")) {
		if (scale == null)
		    size = null;
		else if (scale.intValue() == 0)
		    scale = null;
	    }
	    if (size == null)
		return name;
	    else if (scale == null)
		return name + "(" + size + ")";
	    else
		return name + "(" + size + ", " + scale + ")";

	} else if (driver.equals("PostgreSQL")) {

	    if (name.equals("biginit")
		    || name.equals("int8"))
		return "NUMBER(20)";
	    else if (name.equals("integer")
		    || name.equals("int")
		    || name.equals("int4"))
		return "NUMBER(10)";
	    else if (name.equals("smallint")
		    || name.equals("int2"))
		return "NUMBER(5)";
	    else if (name.equals("real")
		    || name.equals("float4"))
		return "NUMBER";
	    else if (name.equals("double precision")
		    || name.equals("float8"))
		return "NUMBER";
	    else if (name.equals("numeric")
		    || name.equals("decimal")) {
		if (size.intValue() == 65535 && scale.intValue() == 65531)
		    return "NUMBER";
		else if (scale.intValue() == 0)
		    return "NUMBER(" + size + ")";
		else
		    return "NUMBER(" + size + ", " + scale + ")";
	    } else if (name.equals("date"))
		return "DATE";
	    else if (name.equals("time"))
		return "DATE";
	    else if (name.equals("timestamp"))
		return "DATE";
	    else if (name.equals("bytea"))
		return "BLOB";
	    else if (name.equals("char")
		    || name.equals("bpchar")
		    || name.equals("character"))
		return "CHAR(" + size + ")";
	    else if (name.equals("varchar")
		    || name.equals("character varying"))
		if (size.intValue() == 0)
		    // Unlimited size text... But we don't want to use CLOB
		    // or LONG because of the restrictions with those types.
		    // So, make it a maximum-size VARCHAR2.
		    // TODO: add a mechanism where we can insert a comment into
		    // the generated SQL to warn users of what's going on (and
		    // that they should probably analyze their application data
		    // model to find a more appropriate column size).
		    return "VARCHAR2(4000)";
		else
		    return "VARCHAR2(" + size + ")";
	    else if (name.equals("text"))
		// I'm not sure what the difference is in PostgreSQL between
		// "varchar" with no size specification, and "text"... But they
		// are distinct data types. I try to take advantage by taking
		// "text" to be something we can safely substitute an Oracle
		// "CLOB" for (unless the column is part of an index or a
		// primary key, in which case Oracle doesn't allow LOBs).
		if (columnIsPartOfIndex(table, column))
		    // TODO -- emit comment containing warning
		    return "VARCHAR2(4000)";
		else
		    return "CLOB";
	    else {
		// Unsupported value, such as one of PostgreSQL's geometric
		// data types... Return what we can and let the user try to
		// figure it out.
		if (size == null)
		    return name;
		else if (scale == null)
		    return name + "(" + size + ")";
		else
		    return name + "(" + size + ", " + scale + ")";
	    }

	} else if (driver.equals("SmallSQL")) {

	    if (name.equals("BIT")
		    || name.equals("BOOLEAN"))
		return "NUMBER(1)";
	    else if (name.equals("TINYINT")
		    || name.equals("BYTE"))
		return "NUMBER(3)";
	    else if (name.equals("SMALLINT"))
		return "NUMBER(5)";
	    else if (name.equals("INT"))
		return "NUMBER(10)";
	    else if (name.equals("BIGINT"))
		return "NUMBER(19)";
	    else if (name.equals("REAL")
		    || name.equals("DOUBLE")
		    || name.equals("FLOAT"))
		return "NUMBER";
	    else if (name.equals("MONEY"))
		return "NUMBER(19, 4)";
	    else if (name.equals("SMALLMONEY"))
		return "NUMBER(10, 4)";
	    else if (name.equals("NUMERIC")
		    || name.equals("DECIMAL")
		    || name.equals("NUMBER")
		    || name.equals("VARNUM")) {
		if (scale == null || scale.intValue() == 0)
		    return "NUMBER(" + size + ")";
		else
		    return "NUMBER(" + size + ", " + scale + ")";
	    } else if (name.equals("CHAR")
		    || name.equals("CHARACTER"))
		return "CHAR(" + size + ")";
	    else if (name.equals("NCHAR"))
		return "NCHAR(" + size + ")";
	    else if (name.equals("VARCHAR")
		    || name.equals("VARCHAR2"))
		return "VARCHAR2(" + size + ")";
	    else if (name.equals("NVARCHAR")
		    || name.equals("NVARCHAR2"))
		return "NVARCHAR2(" + size + ")";
	    else if (name.equals("LONGVARCHAR")
		    || name.equals("TEXT")
		    || name.equals("LONG")
		    || name.equals("CLOB")) {
		if (columnIsPartOfIndex(table, column))
		    // TODO -- emit comment containing warning
		    return "VARCHAR2(4000)";
		else
		    return "CLOB";
	    } else if (name.equals("LONGNVARCHAR")
		    || name.equals("NTEXT")) {
		if (columnIsPartOfIndex(table, column))
		    // TODO -- emit comment containing warning
		    return "NVARCHAR2(4000)";
		else
		    return "NCLOB";
	    // The following are types not mentioned in the SmallSQL doc,
	    // but which do occur in the sample database...
	    } else if (name.equals("BINARY")
		    || name.equals("VARBINARY"))
		return "RAW(" + size + ")";
	    else if (name.equals("LONGVARBINARY"))
		return "BLOB";
	    else if (name.equals("DATETIME")
		    || name.equals("SMALLDATETIME"))
		return "DATE";
	    else {
		// Unexpected value... Print as is and hope the user can
		// straighten out the SQL script manually.
		System.out.println("Unrecognized: \"" + name + "\"");
		if (size == null)
		    return name;
		else if (scale == null)
		    return name + "(" + size + ")";
		else
		    return name + "(" + size + ", " + scale + ")";
	    }

	} else { // driver = "Generic" or unknown

	    // TODO - this code simply prints generic SQL data type names;
	    // this yields scripts that Oracle can't digest. Generate
	    // appropriate Oracle equivalents instead.

	    int sqlType = table.getSqlTypes()[column];
	    String sqlName = MiscUtils.sqlTypeIntToString(sqlType);
	    if (size == null)
		return sqlName;
	    else if (scale == null)
		return sqlName + "(" + size + ")";
	    else
		return sqlName + "(" + size + ", " + scale + ")";
	
	}
    }

    private boolean columnIsPartOfIndex(Table table, int column) {
	String name = table.getColumnNames()[column];
	PrimaryKey pk = table.getPrimaryKey();
	if (pk != null)
	    for (int i = 0; i < pk.getColumnCount(); i++)
		if (name.equalsIgnoreCase(pk.getColumnName(i)))
		    return true;
	Index[] indexes = table.getIndexes();
	for (int i = 0; i < indexes.length; i++) {
	    Index index = indexes[i];
	    for (int j = 0; j < index.getColumnCount(); j++)
		if (name.equalsIgnoreCase(index.getColumnName(j)))
		    return true;
	}
	return false;
    }
}
