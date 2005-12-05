package jdbcnav;

import java.sql.*;
import java.text.*;
import java.util.*;
import jdbcnav.model.TypeSpec;


public class JDBCDatabase_MySQL extends JDBCDatabase {
    public JDBCDatabase_MySQL(String name, String driver, Connection con) {
	super(name, driver, con);
    }

    /**
     * The MySQL JDBC Driver does not return anything in
     * ResultSetMetaData.getCatalogName(), which makes it impossible (without
     * parsing SQL ourselves, anyway) to support allowTable=true in
     * Database.runQuery().
     */
    protected boolean resultSetContainsTableInfo() {
	return false;
    }

    protected TypeSpec makeTypeSpec(String dbType, Integer size, Integer scale,
				    int sqlType, String javaType) {
	TypeSpec spec = super.makeTypeSpec(dbType, size, scale, sqlType,
								javaType);
	if (dbType.equalsIgnoreCase("bit")
		|| dbType.equalsIgnoreCase("bool")
		|| dbType.equalsIgnoreCase("boolean")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = 1;
	    spec.size_in_bits = true;
	    spec.scale = 0;
	    spec.scale_in_bits = true;
	} else if (dbType.equalsIgnoreCase("tinyint")
		|| dbType.equalsIgnoreCase("smallint")
		|| dbType.equalsIgnoreCase("int")
		|| dbType.equalsIgnoreCase("integer")
		|| dbType.equalsIgnoreCase("bigint")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = size.intValue();
	    spec.size_in_bits = false;
	    spec.scale = 0;
	    spec.scale_in_bits = false;
	} else if (dbType.equalsIgnoreCase("float")) {
	    spec.type = TypeSpec.FLOAT;
	    spec.size = 24;
	    spec.size_in_bits = true;
	    spec.min_exp = -127;
	    spec.max_exp = 127;
	    spec.exp_of_2 = true;
	} else if (dbType.equalsIgnoreCase("double")
		|| dbType.equalsIgnoreCase("double precision")
		|| dbType.equalsIgnoreCase("real")) {
	    spec.type = TypeSpec.FLOAT;
	    spec.size = 54;
	    spec.size_in_bits = true;
	    spec.min_exp = -1023;
	    spec.max_exp = 1023;
	    spec.exp_of_2 = true;
	} else if (dbType.equalsIgnoreCase("decimal")
		|| dbType.equalsIgnoreCase("dec")
		|| dbType.equalsIgnoreCase("numeric")
		|| dbType.equalsIgnoreCase("fixed")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = size.intValue();
	    spec.size_in_bits = false;
	    spec.scale = scale.intValue();
	    spec.scale_in_bits = false;
	} else if (dbType.equalsIgnoreCase("date")) {
	    spec.type = TypeSpec.DATE;
	} else if (dbType.equalsIgnoreCase("time")) {
	    spec.type = TypeSpec.TIME;
	    spec.size = 0;
	} else if (dbType.equalsIgnoreCase("datetime")) {
	    spec.type = TypeSpec.TIMESTAMP;
	    spec.size = 0;
	} else if (dbType.equalsIgnoreCase("timestamp")) {
	    // NOTE: this is a special type in MySQL for automatically
	    // timestamping rows. We map it to a regular timestamp
	    // strictly for export purposes.
	    spec.type = TypeSpec.TIMESTAMP;
	    spec.size = 0;
	} else if (dbType.equalsIgnoreCase("year")) {
	    // MySQL returns a java.sql.Date for this type. That seems like
	    // overkill when they could just as well have used something like
	    // DECIMAL(4) or DECIMAL(2). Frankly, I don't understand why this
	    // type exists at all... Anyway, I map it to a number, for
	    // export purposes.
	    spec.type = TypeSpec.FIXED;
	    spec.size = size.intValue();
	    spec.size_in_bits = false;
	    spec.scale = 0;
	    spec.scale_in_bits = false;
	} else if (dbType.equalsIgnoreCase("char")
		|| dbType.equalsIgnoreCase("character")) {
	    spec.type = TypeSpec.CHAR;
	    spec.size = size.intValue();
	} else if (dbType.equals("varchar")
		|| dbType.equals("character varying")) {
	    // Despite the richness of character types claimed in the MySQL
	    // documentation, it appears *everything* is actually converted
	    // to VARCHAR -- even BINARY! There is no way to distinguish
	    // between these types afterward. In the case of BINARY, this is
	    // especially bad -- the DESCRIBE command will tell you if a
	    // column is binary, but the regular JDBC mechanisms (Database-
	    // MetaData and ResultSetMetaData) make no distinction
	    // whatsoever. Note that this means that MySQL even returns
	    // java.lang.String for BINARY. Yuck.
	    // TODO: it is possible to run DESCRIBE over JDBC; it returns
	    // a ResultSet in which char and binary *are* distinguishable.
	    spec.type = TypeSpec.VARCHAR;
	    spec.size = size.intValue();
	} else if (dbType.equalsIgnoreCase("nchar")
		|| dbType.equalsIgnoreCase("national char")
		|| dbType.equalsIgnoreCase("national character")) {
	    spec.type = TypeSpec.NCHAR;
	    spec.size = size.intValue();
	} else if (dbType.equalsIgnoreCase("national varchar")
		|| dbType.equalsIgnoreCase("national character varying")) {
	    spec.type = TypeSpec.VARNCHAR;
	    spec.size = size.intValue();
	} else if (dbType.equalsIgnoreCase("binary")) {
	    spec.type = TypeSpec.RAW;
	    spec.size = size.intValue();
	} else if (dbType.equalsIgnoreCase("varbinary")) {
	    spec.type = TypeSpec.VARRAW;
	    spec.size = size.intValue();
	} else if (dbType.equalsIgnoreCase("tinytext")) {
	    spec.type = TypeSpec.VARCHAR;
	    spec.size = 255;
	} else if (dbType.equalsIgnoreCase("tinyblob")) {
	    spec.type = TypeSpec.VARRAW;
	    spec.size = 255;
	} else if (dbType.equalsIgnoreCase("text")
		|| dbType.equalsIgnoreCase("mediumtext")
		|| dbType.equalsIgnoreCase("longtext")) {
	    spec.type = TypeSpec.LONGVARCHAR;
	} else if (dbType.equalsIgnoreCase("blob")
		|| dbType.equalsIgnoreCase("mediumblob")
		|| dbType.equalsIgnoreCase("longblob")) {
	    spec.type = TypeSpec.LONGVARRAW;
	} else if (dbType.toLowerCase().startsWith("enum(")) {
	    spec.type = TypeSpec.VARCHAR;
	    spec.size = 255;
	} else if (dbType.toLowerCase().startsWith("set(")) {
	    spec.type = TypeSpec.VARCHAR;
	    spec.size = 255;
	} else {
	    spec.type = TypeSpec.UNKNOWN;
	}

	if (dbType.equalsIgnoreCase("float")
		|| dbType.equalsIgnoreCase("double")
		|| dbType.equalsIgnoreCase("double precision")
		|| dbType.equalsIgnoreCase("real")
		|| dbType.equalsIgnoreCase("decimal")
		|| dbType.equalsIgnoreCase("dec")
		|| dbType.equalsIgnoreCase("numeric")
		|| dbType.equalsIgnoreCase("fixed")) {
	    // size and scale both relevant
	} else if (dbType.equalsIgnoreCase("tinyint")
		|| dbType.equalsIgnoreCase("smallint")
		|| dbType.equalsIgnoreCase("mediumint")
		|| dbType.equalsIgnoreCase("int")
		|| dbType.equalsIgnoreCase("integer")
		|| dbType.equalsIgnoreCase("bigint")
		|| dbType.equalsIgnoreCase("timestamp")
		|| dbType.equalsIgnoreCase("year")
		|| dbType.equalsIgnoreCase("char")
		|| dbType.equalsIgnoreCase("character")
		|| dbType.equalsIgnoreCase("varchar")
		|| dbType.equalsIgnoreCase("character varying")
		|| dbType.equalsIgnoreCase("national char")
		|| dbType.equalsIgnoreCase("national character")
		|| dbType.equalsIgnoreCase("national varchar")
		|| dbType.equalsIgnoreCase("national character varying")
		|| dbType.equalsIgnoreCase("binary")
		|| dbType.equalsIgnoreCase("varbinary")) {
	    scale = null;
	} else {
	    size = null;
	    scale = null;
	}

	// TODO: use DESCRIBE so we can get an accurate native representation
	// for enum and set types.
	if (size == null)
	    spec.native_representation = dbType;
	else if (scale == null)
	    spec.native_representation = dbType + "(" + size + ")";
	else
	    spec.native_representation = dbType + "(" + size + ", " + scale + ")";

	return spec;
    }

    protected void fixDbTypes(String qualifiedName, ArrayList dbTypes) {
	Statement stmt = null;
	ResultSet rs = null;
	try {
	    stmt = con.createStatement();
	    rs = stmt.executeQuery("describe " + qualifiedName);
	    int col = 0;
	    while (rs.next()) {
		if (col >= dbTypes.size())
		    break;
		String type = rs.getString("Type");
		String tl = type.toLowerCase();
		if (tl.endsWith(" binary")) {
		    if (tl.startsWith("character(")
			    || tl.startsWith("char("))
			dbTypes.set(col, "binary");
		    else if (tl.startsWith("character varying(")
			    || tl.startsWith("varchar("))
			dbTypes.set(col, "varbinary");
		} else if (tl.startsWith("enum(")
			    || tl.startsWith("set("))
		    dbTypes.set(col, type);
		col++;
	    }
	} catch (SQLException e) {}
    }

    private static SimpleDateFormat y2format = new SimpleDateFormat("yy");
    private static SimpleDateFormat y4format = new SimpleDateFormat("yyyy");

    protected Object db2nav(TypeSpec spec, Object o) {
	if (o == null)
	    return null;
	if (spec.jdbcDbType.equalsIgnoreCase("year")) {
	    String s;
	    if (spec.size >= 4)
		s = y4format.format((java.util.Date) o);
	    else
		s = y2format.format((java.util.Date) o);
	    return new Integer(s);
	}
	return super.db2nav(spec, o);
    }

    protected Object nav2db(TypeSpec spec, Object o) {
	if (o == null)
	    return null;
	if (spec.jdbcDbType.equalsIgnoreCase("year")) {
	    java.util.Date d;
	    try {
		String s = o.toString();
		if (spec.size >= 4)
		    d = y4format.parse(s);
		else
		    d = y2format.parse(s);
		return new java.sql.Date(d.getTime());
	    } catch (Exception e) {
		return o;
	    }
	}
	return super.nav2db(spec, o);
    }
}
