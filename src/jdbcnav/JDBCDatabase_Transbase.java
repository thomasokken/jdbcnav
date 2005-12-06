package jdbcnav;

import java.sql.*;
import jdbcnav.model.*;


public class JDBCDatabase_Transbase extends JDBCDatabase {
    public JDBCDatabase_Transbase(String name, String driver, Connection con) {
	super(name, driver, con);
    }

    /**
     * The Transbase JDBC Driver does not return anything in
     * ResultSetMetaData.getCatalogName(), getSchemaName(), and getTableName(),
     * which makes it impossible (without parsing SQL ourselves, anyway) to
     * support allowTable=true in Database.runQuery().
     */
    protected boolean resultSetContainsTableInfo() {
	return false;
    }

    protected TypeSpec makeTypeSpec(String dbType, Integer size, Integer scale,
				    int sqlType, String javaType) {
	TypeSpec spec = super.makeTypeSpec(dbType, size, scale, sqlType,
								javaType);
	if (dbType.equals("TINYINT")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = 8;
	    spec.size_in_bits = true;
	    spec.scale = 0;
	    spec.scale_in_bits = true;
	    spec.jdbcJavaClass = Integer.class;
	} else if (dbType.equals("SMALLINT")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = 16;
	    spec.size_in_bits = true;
	    spec.scale = 0;
	    spec.scale_in_bits = true;
	    spec.jdbcJavaClass = Integer.class;
	} else if (dbType.equals("INTEGER")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = 32;
	    spec.size_in_bits = true;
	    spec.scale = 0;
	    spec.scale_in_bits = true;
	    spec.jdbcJavaClass = Integer.class;
	} else if (dbType.equals("NUMERIC")
		|| dbType.equals("DECIMAL")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = size.intValue();
	    spec.size_in_bits = false;
	    spec.scale = scale.intValue();
	    spec.scale_in_bits = false;
	    spec.jdbcJavaClass = java.math.BigDecimal.class;
	} else if (dbType.equals("FLOAT")) {
	    spec.type = TypeSpec.FLOAT;
	    spec.size = 24;
	    spec.size_in_bits = true;
	    spec.min_exp = -127;
	    spec.max_exp = 127;
	    spec.exp_of_2 = true;
	    spec.jdbcJavaClass = Float.class;
	} else if (dbType.equals("DOUBLE")
		|| dbType.equals("REAL")) {
	    spec.type = TypeSpec.FLOAT;
	    spec.size = 54;
	    spec.size_in_bits = true;
	    spec.min_exp = -1023;
	    spec.max_exp = 1023;
	    spec.exp_of_2 = true;
	    spec.jdbcJavaClass = Double.class;
	} else if (dbType.equals("CHAR")) {
	    if (spec.jdbcSqlType == Types.VARCHAR) {
		spec.type = TypeSpec.LONGVARCHAR;
	    } else {
		spec.type = TypeSpec.CHAR;
		spec.size = size.intValue();
	    }
	    spec.jdbcJavaClass = String.class;
	} else if (dbType.equals("VARCHAR")) {
	    spec.type = TypeSpec.VARCHAR;
	    spec.size = size.intValue();
	    spec.jdbcJavaClass = String.class;
	} else if (dbType.equals("BINCHAR")) {
	    if (spec.jdbcSqlType == Types.VARCHAR) {
		spec.type = TypeSpec.LONGVARRAW;
	    } else {
		spec.type = TypeSpec.RAW;
		spec.size = size.intValue();
	    }
	    spec.jdbcJavaClass = new byte[1].getClass();
	} else if (dbType.equals("BITS")
		|| dbType.equals("BITS2")) {
	    spec.type = TypeSpec.RAW;
	    spec.size = (size.intValue() + 7) / 8;
	    spec.jdbcJavaClass = new byte[1].getClass();
	} else if (dbType.equals("BLOB")) {
	    spec.type = TypeSpec.LONGVARRAW;
	    spec.jdbcJavaClass = new byte[1].getClass();
	} else if (dbType.equals("DATETIME")) {
	    if (spec.jdbcSqlType == Types.DATE) {
		spec.type = TypeSpec.DATE;
		spec.jdbcJavaClass = java.sql.Date.class;
	    } else if (spec.jdbcSqlType == Types.TIME) {
		spec.type = TypeSpec.TIME;
		spec.jdbcJavaClass = java.sql.Time.class;
	    } else if (spec.jdbcSqlType == Types.TIMESTAMP) {
		spec.type = TypeSpec.TIMESTAMP;
		spec.jdbcJavaClass = java.sql.Timestamp.class;
	    } else { // spec.jdbcSqlType == Types.OTHER
		// TODO: This is a DATETIME with range other than [YY:MS];
		// is there a way to find out which?
		spec.type = TypeSpec.TIMESTAMP;
		try {
		    spec.jdbcJavaClass = Class.forName("transbase.tbx.types.TBDatetime");
		} catch (ClassNotFoundException e) {
		    spec.jdbcJavaClass = Object.class;
		}
	    }
	} else if (dbType.equals("TIMESPAN")) {
	    // TODO: how can we find out the details?
	    spec.type = TypeSpec.INTERVAL_YS;
	    try {
		spec.jdbcJavaClass = Class.forName("transbase.tbx.types.TBDatetime");
	    } catch (ClassNotFoundException e) {
		spec.jdbcJavaClass = Object.class;
	    }
	}

	spec.jdbcJavaType = spec.jdbcJavaClass.getName();

	if (dbType.equals("NUMERIC")
		|| dbType.equals("DECIMAL")) {
	    // size and scale both relevant
	} else if (dbType.equals("CHAR")
		|| dbType.equals("VARCHAR")
		|| dbType.equals("BINCHAR")
		|| dbType.equals("BITS")
		|| dbType.equals("BITS2")) {
	    scale = null;
	} else {
	    size = null;
	    scale = null;
	}

	if (dbType.equals("DATETIME")) {
	    if (spec.jdbcSqlType == Types.DATE)
		spec.native_representation = "DATE";
	    else if (spec.jdbcSqlType == Types.TIME)
		spec.native_representation = "TIME";
	    else
		// TODO: figure out the exact type!
		spec.native_representation = "DATETIME[YY:MS]";
	} else if (dbType.equals("TIMESPAN"))
	    // TODO: figure out the exact type!
	    spec.native_representation = "TIMESPAN[YY:MS]";
	else if (spec.type == TypeSpec.LONGVARCHAR)
	    spec.native_representation = "CHAR(*)";
	else if (spec.type == TypeSpec.LONGVARRAW && spec.jdbcSqlType == Types.VARCHAR)
	    spec.native_representation = "BINCHAR(*)";
	// TODO: BITS(*) and BITS2(*); also needs to be handled above
	// by setting spec.type to LONGVARRAW
	else if (size == null)
	    spec.native_representation = dbType;
	else if (scale == null)
	    spec.native_representation = dbType + "(" + size + ")";
	else
	    spec.native_representation = dbType + "(" + size + ", " + scale + ")";

	return spec;
    }
}
