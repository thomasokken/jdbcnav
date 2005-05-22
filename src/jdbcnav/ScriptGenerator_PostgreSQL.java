package jdbcnav;

import jdbcnav.model.Table;
import jdbcnav.util.MiscUtils;

public class ScriptGenerator_PostgreSQL extends ScriptGenerator {
    protected String printType(Table table, int column) {
	String driver = table.getDatabase().getInternalDriverName();
	String name = table.getDbTypes()[column];
	Integer size = table.getColumnSizes()[column];
	Integer scale = table.getColumnScales()[column];

	if (driver.equals("Oracle")) {

	    if (name.equals("CHAR"))
		return "char(" + size + ")";
	    else if (name.equals("VARCHAR2"))
		return "varchar(" + size + ")";
	    else if (name.equals("NCHAR"))
		return "char(" + size + ")";
	    else if (name.equals("NVARCHAR2"))
		return"varchar(" + size + ")"; 
	    else if (name.equals("NUMBER")) {
		if (scale == null)
		    return "numeric";
		else if (scale.intValue() == 0)
		    return "numeric(" + size + ")";
		else
		    return "numeric(" + size + ", " + scale + ")";
	    } else if (name.equals("FLOAT"))
		// Not a true Oracle type?
		return "float8";
	    else if (name.equals("LONG"))
		return "text";
	    else if (name.equals("RAW"))
		return "bytea";
	    else if (name.equals("LONG RAW"))
		return "bytea";
	    else if (name.equals("DATE"))
		return "timestamp";
	    else if (name.equals("BLOB"))
		return "bytea";
	    else if (name.equals("CLOB"))
		return "text";
	    else if (name.equals("NCLOB"))
		return "text";
	    else {
		// Unsupported value... Print as is and hope the user can
		// straighten out the SQL script manually.
		if (size == null)
		    return name;
		else if (scale == null)
		    return name + "(" + size + ")";
		else
		    return name + "(" + size + ", " + scale + ")";
	    }

	} else if (driver.equals("PostgreSQL")) {

	    if (name.equals("numeric")
		    || name.equals("decimal")) {
		if (size.intValue() == 65535 && scale.intValue() == 65531) {
		    size = null;
		    scale = null;
		} else if (scale.intValue() == 0)
		    scale = null;
	    } else if (name.equals("bit varying")
		    || name.equals("varbit")
		    || name.equals("character varying")
		    || name.equals("varchar")
		    || name.equals("character")
		    || name.equals("char")
		    || name.equals("bpchar")
		    || name.equals("interval")
		    || name.equals("time")
		    || name.equals("timetz")
		    || name.equals("timestamp")
		    || name.equals("timestamptz")) {
		scale = null;
	    } else {
		size = null;
		scale = null;
	    }

	    if ((name.equals("varchar")
		    || name.equals("character varying"))
		    && size.intValue() == 0)
		size = null;

	    if (name.equals("bpchar"))
		name = "char";

	    if (size == null)
		return name;
	    else if (scale == null)
		return name + "(" + size + ")";
	    else
		return name + "(" + size + ", " + scale + ")";

	} else if (driver.equals("SmallSQL")) {

	    if (name.equals("BIT")
		    || name.equals("BOOLEAN"))
		return "boolean";
	    else if (name.equals("TINYINT")
		    || name.equals("BYTE")
		    || name.equals("SMALLINT"))
		return "smallint";
	    else if (name.equals("INT"))
		return "integer";
	    else if (name.equals("BIGINT"))
		return "bigint";
	    else if (name.equals("REAL"))
		return "real";
	    else if (name.equals("DOUBLE")
		    || name.equals("FLOAT"))
		return "double precision";
	    else if (name.equals("MONEY"))
		return "numeric(19, 4)";
	    else if (name.equals("SMALLMONEY"))
		return "numeric(10, 4)";
	    else if (name.equals("NUMERIC")
		    || name.equals("DECIMAL")
		    || name.equals("NUMBER")
		    || name.equals("VARNUM")) {
		if (scale == null)
		    return "numeric(" + size + ")";
		else
		    return "numeric(" + size + ", " + scale + ")";
	    } else if (name.equals("CHAR")
		    || name.equals("CHARACTER")
		    || name.equals("NCHAR"))
		return "character(" + size + ")";
	    else if (name.equals("VARCHAR")
		    || name.equals("NVARCHAR")
		    || name.equals("VARCHAR2")
		    || name.equals("NVARCHAR2"))
		return "character varying(" + size + ")";
	    else if (name.equals("LONGVARCHAR")
		    || name.equals("TEXT")
		    || name.equals("LONGNVARCHAR")
		    || name.equals("NTEXT")
		    || name.equals("LONG")
		    || name.equals("CLOB"))
		return "character varying(0)"; // text?
	    // The following are types not mentioned in the SmallSQL doc,
	    // but which do occur in the sample database...
	    else if (name.equals("BINARY")
		    || name.equals("VARBINARY")
		    || name.equals("LONGVARBINARY"))
		return "bytea";
	    else if (name.equals("DATETIME")
		    || name.equals("SMALLDATETIME"))
		return "timestamp";
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
	    // this yields scripts that PostgreSQL can't digest. Generate
	    // appropriate PostgreSQL equivalents instead.

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
}
