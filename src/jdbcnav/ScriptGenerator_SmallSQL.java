package jdbcnav;

import jdbcnav.model.Table;
import jdbcnav.util.MiscUtils;

public class ScriptGenerator_SmallSQL extends ScriptGenerator {
    protected String printTypeNOT(Table table, int column) {
	String driver = table.getDatabase().getInternalDriverName();
	String name = table.getDbTypes()[column];
	Integer size = table.getColumnSizes()[column];
	Integer scale = table.getColumnScales()[column];

	if (driver.equals("Oracle")) {

	    if (name.equals("CHAR")
		    || name.equals("NCHAR"))
		return "CHAR(" + size + ")";
	    else if (name.equals("VARCHAR2")
		    || name.equals("NVARCHAR2"))
		return "VARCHAR(" + size + ")";
	    else if (name.equals("NUMBER")) {
		if (scale == null)
		    return "DOUBLE";
		else if (scale.intValue() == 0)
		    return "NUMERIC(" + size + ")";
		else
		    return "NUMERIC(" + size + ", " + scale + ")";
	    } else if (name.equals("FLOAT"))
		// Not a true Oracle type?
		return "DOUBLE";
	    else if (name.equals("LONG")
		    || name.equals("CLOB")
		    || name.equals("NCLOB"))
		return "LONGVARCHAR";
	    else if (name.equals("RAW")
		    || name.equals("LONG RAW")
		    || name.equals("BLOB"))
		return "LONGVARBINARY";
	    else if (name.equals("DATE"))
		return "DATETIME";
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

	    if (name.equals("biginit")
		    || name.equals("int8"))
		return "BIGINT)";
	    else if (name.equals("integer")
		    || name.equals("int")
		    || name.equals("int4"))
		return "INT";
	    else if (name.equals("smallint")
		    || name.equals("int2"))
		return "SMALLINT";
	    else if (name.equals("real")
		    || name.equals("float4"))
		return "REAL";
	    else if (name.equals("double precision")
		    || name.equals("float8"))
		return "DOUBLE";
	    else if (name.equals("numeric")
		    || name.equals("decimal")) {
		if (size.intValue() == 65535 && scale.intValue() == 65531)
		    return "DOUBLE";
		else if (scale.intValue() == 0)
		    return "NUMERIC(" + size + ")";
		else
		    return "NUMERIC(" + size + ", " + scale + ")";
	    } else if (name.equals("date")
		    || name.equals("time")
		    || name.equals("timestamp"))
		return "DATETIME";
	    else if (name.equals("bytea"))
		return "LONGVARBINARY";
	    else if (name.equals("char")
		    || name.equals("bpchar")
		    || name.equals("character"))
		return "CHAR(" + size + ")";
	    else if (name.equals("varchar")
		    || name.equals("character varying"))
		if (size.intValue() == 0)
		    return "LONGVARCHAR";
		else
		    return "VARCHAR(" + size + ")";
	    else if (name.equals("text"))
		return "LONGVARCHAR";
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

	    if (name.equals("NUMERIC")
		    || name.equals("DECIMAL")
		    || name.equals("NUMBER")
		    || name.equals("VARNUM")) {
		// 'scale' is optional, but the SmallSQL driver
		// does not distinguish between scale == null and
		// scale == 0 (null is handled as 0).
	    } else if (name.equals("CHAR")
		    || name.equals("CHARACTER")
		    || name.equals("NCHAR")
		    || name.equals("VARCHAR")
		    || name.equals("NVARCHAR")
		    || name.equals("VARCHAR2")
		    || name.equals("NVARCHAR2")) {
		scale = null;
	    } else {
		size = null;
		scale = null;
	    }

	    if (size == null)
		return name;
	    else if (scale == null)
		return name + "(" + size + ")";
	    else
		return name + "(" + size + ", " + scale + ")";

	} else { // driver = "Generic" or unknown

	    // TODO - this code simply prints generic SQL data type names;
	    // this yields scripts that SmallSQL can't digest. Generate
	    // appropriate SmallSQL equivalents instead.

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
