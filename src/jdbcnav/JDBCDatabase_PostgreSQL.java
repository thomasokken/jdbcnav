package jdbcnav;

import java.sql.*;
import jdbcnav.model.*;
import jdbcnav.util.NavigatorException;


public class JDBCDatabase_PostgreSQL extends JDBCDatabase {
    public JDBCDatabase_PostgreSQL(String name, String driver, Connection con) {
	super(name, driver, con);
    }

    protected String[] getJavaTypes(String qualifiedName)
						    throws NavigatorException {
	// This is a bit icky. What I would *like* to do it use
	// PreparedStatement.getMetaData() to find out about a table's Java
	// type mapping without having to execute a statement, but the
	// PostgreSQL 8.0.0beta1 JDBC Driver (pgdev.305.jdbc3.jar) returns
	// 'null' from that method.
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
     * For unnamed keys, PostgreSQL returns "<unnamed>" instead of null.
     */
    protected String unmangleKeyName(String name) {
	return "<unnamed>".equals(name) ? null : name;
    }

    /**
     * The PostgreSQL JDBC Driver (pgdev.305.jdbc3.jar) does not return
     * anything in ResultSetMetaData.getCatalogName(), getSchemaName(),
     * and getTableName(), which makes it impossible (without parsing SQL
     * ourselves, anyway) to support allowTable=true in Database.runQuery().
     */
    protected boolean resultSetContainsTableInfo() {
	return false;
    }

    protected String qualifyName(String name) {
	return "public." + name;
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

    protected TypeSpec makeTypeSpec(String dbType, Integer size, Integer scale,
				    int sqlType, String javaType) {
	TypeSpec spec = super.makeTypeSpec(dbType, size, scale, sqlType,
								javaType);
	if (dbType.equals("bigint")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = 64;
	    spec.size_in_bits = true;
	    spec.scale = 0;
	    spec.scale_in_bits = true;
	} else if (dbType.equals("integer")
		|| dbType.equals("int")
		|| dbType.equals("int4")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = 32;
	    spec.size_in_bits = true;
	    spec.scale = 0;
	    spec.scale_in_bits = true;
	} else if (dbType.equals("smallint")
		|| dbType.equals("int2")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = 16;
	    spec.size_in_bits = true;
	    spec.scale = 0;
	    spec.scale_in_bits = true;
	} else if (dbType.equals("real")
		|| dbType.equals("float4")) {
	    spec.type = TypeSpec.FLOAT;
	    spec.size = 24;
	    spec.size_in_bits = true;
	    spec.min_exp = -127;
	    spec.max_exp = 127;
	    spec.exp_of_2 = true;
	} else if (dbType.equals("double precision")
		|| dbType.equals("float8")) {
	    spec.type = TypeSpec.FLOAT;
	    spec.size = 54;
	    spec.size_in_bits = true;
	    spec.min_exp = -1023;
	    spec.max_exp = 1023;
	    spec.exp_of_2 = true;
	} else if (dbType.equals("numeric")
		|| dbType.equals("decimal")) {
	    if (size.intValue() == 65535 && scale.intValue() == 65531) {
		// TODO: This type does not fit in the current TypeSpec
		// model. It is an arbitrary-precision (up to 1000 digits)
		// number without scale coercion, or, to put it differently,
		// a high-precision floating-point number.
		// I choose double-precision since it's the best match in
		// terms of allowing the original type's dynamic range, if not
		// its precision.
		// TODO - Warning
		spec.type = TypeSpec.FLOAT;
		spec.size = 54;
		spec.size_in_bits = true;
		spec.min_exp = -1023;
		spec.max_exp = 1023;
		spec.exp_of_2 = true;
	    } else {
		spec.type = TypeSpec.FIXED;
		spec.size = size.intValue();
		spec.size_in_bits = false;
		spec.scale = scale.intValue();
		spec.scale_in_bits = false;
	    }
	} else if (dbType.equals("money")) {
	    // Deprecated type, so although we recognize it, printType() will
	    // never generate it.
	    spec.type = TypeSpec.FIXED;
	    spec.size = 32;
	    spec.size_in_bits = true;
	    spec.scale = 2;
	    spec.scale_in_bits = false;
	} else if (dbType.equals("date")) {
	    spec.type = TypeSpec.DATE;
	} else if (dbType.equals("time")) {
	    spec.type = TypeSpec.TIME;
	    spec.size = size.intValue();
	} else if (dbType.equals("time with time zone")
		|| dbType.equals("timetz")) {
	    spec.type = TypeSpec.TIME_TZ;
	    spec.size = size.intValue();
	} else if (dbType.equals("timestamp")) {
	    spec.type = TypeSpec.TIMESTAMP;
	    spec.size = size.intValue();
	} else if (dbType.equals("timestamp with time zone")
		|| dbType.equals("timestamptz")) {
	    spec.type = TypeSpec.TIMESTAMP_TZ;
	    spec.size = size.intValue();
	} else if (dbType.equals("interval")) {
	    // Yuck; PostgreSQL does not distinguish between
	    // INTERVAL YEAR TO MONTH and INTERVAL DAY TO SECOND; it has one
	    // type that is basically INTERVAL YEAR TO SECOND. I convert this
	    // to INTERVAL DAY TO SECOND, because I don't want to lose the
	    // resolution.
	    spec.type = TypeSpec.INTERVAL_DS;
	    spec.size = 11;
	    spec.scale = size.intValue();
	} else if (dbType.equals("bytea")) {
	    spec.type = TypeSpec.LONGVARRAW;
	} else if (dbType.equals("char")
		|| dbType.equals("bpchar")
		|| dbType.equals("character")) {
	    spec.type = TypeSpec.CHAR;
	    spec.size = size.intValue();
	} else if (dbType.equals("varchar")
		|| dbType.equals("character varying")) {
	    if (size.intValue() == 0)
		spec.type = TypeSpec.LONGVARCHAR;
	    else {
		spec.type = TypeSpec.VARCHAR;
		spec.size = size.intValue();
	    }
	} else if (dbType.equals("text")) {
	    spec.type = TypeSpec.LONGVARCHAR;
	} else {
	    // Unsupported value, such as one of PostgreSQL's geometric data
	    // types or bit strings. Don't know how to handle them so we tag
	    // them UNKNOWN, which will cause the script generator to pass them
	    // on uninterpreted and unchanged.
	    spec.type = TypeSpec.UNKNOWN;
	}

	if (dbType.equals("numeric")
		|| dbType.equals("decimal")) {
	    if (size.intValue() == 65535 && scale.intValue() == 65531) {
		size = null;
		scale = null;
	    } else if (scale.intValue() == 0)
		scale = null;
	} else if (dbType.equals("bit varying")
		|| dbType.equals("varbit")
		|| dbType.equals("bit")
		|| dbType.equals("character varying")
		|| dbType.equals("varchar")
		|| dbType.equals("character")
		|| dbType.equals("char")
		|| dbType.equals("bpchar")
		|| dbType.equals("interval")
		|| dbType.equals("time")
		|| dbType.equals("timetz")
		|| dbType.equals("timestamp")
		|| dbType.equals("timestamptz")) {
	    scale = null;
	} else {
	    size = null;
	    scale = null;
	}

	if ((dbType.equals("varchar")
		    || dbType.equals("character varying"))
		&& size.intValue() == 0)
	    size = null;

	if (dbType.equals("bpchar"))
	    dbType = "char";

	if (size == null)
	    spec.native_representation = dbType;
	else if (scale == null)
	    spec.native_representation = dbType + "(" + size + ")";
	else
	    spec.native_representation = dbType + "(" + size + ", " + scale + ")";

	return spec;
    }
}
