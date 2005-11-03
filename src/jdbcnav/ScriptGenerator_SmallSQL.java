package jdbcnav;

import jdbcnav.model.TypeDescription;

public class ScriptGenerator_SmallSQL extends ScriptGenerator {
    public TypeDescription getTypeDescription(String dbType, Integer size,
					      Integer scale) {
	// NOTE: We don't populate the part_of_key, part_of_index, and
	// native_representation here; that is left to our caller,
	// BasicTable.getTypeDescription().

	TypeDescription td = new TypeDescription();

	if (dbType.equals("BIT")
		|| dbType.equals("BOOLEAN")) {
	    td.type = TypeDescription.FIXED;
	    td.size = 1;
	    td.size_in_bits = true;
	    td.scale = 0;
	    td.scale_in_bits = true;
	} else if (dbType.equals("TINYINT")
		|| dbType.equals("BYTE")) {
	    td.type = TypeDescription.FIXED;
	    td.size = 8;
	    td.size_in_bits = true;
	    td.scale = 0;
	    td.scale_in_bits = true;
	} else if (dbType.equals("SMALLINT")) {
	    td.type = TypeDescription.FIXED;
	    td.size = 16;
	    td.size_in_bits = true;
	    td.scale = 0;
	    td.scale_in_bits = true;
	} else if (dbType.equals("INT")) {
	    td.type = TypeDescription.FIXED;
	    td.size = 32;
	    td.size_in_bits = true;
	    td.scale = 0;
	    td.scale_in_bits = true;
	} else if (dbType.equals("BIGINT")) {
	    td.type = TypeDescription.FIXED;
	    td.size = 64;
	    td.size_in_bits = true;
	    td.scale = 0;
	    td.scale_in_bits = true;
	} else if (dbType.equals("REAL")) {
	    td.type = TypeDescription.FLOAT;
	    td.size = 24;
	    td.size_in_bits = true;
	    td.min_exp = -127;
	    td.max_exp = 127;
	    td.exp_of_2 = true;
	} else if (dbType.equals("DOUBLE")
		|| dbType.equals("FLOAT")) {
	    td.type = TypeDescription.FLOAT;
	    td.size = 54;
	    td.size_in_bits = true;
	    td.min_exp = -1023;
	    td.max_exp = 1023;
	    td.exp_of_2 = true;
	} else if (dbType.equals("MONEY")) {
	    // TODO - verify
	    td.type = TypeDescription.FIXED;
	    td.size = 64;
	    td.size_in_bits = true;
	    td.scale = 4;
	    td.scale_in_bits = false;
	} else if (dbType.equals("SMALLMONEY")) {
	    // TODO - verify
	    td.type = TypeDescription.FIXED;
	    td.size = 32;
	    td.size_in_bits = true;
	    td.scale = 4;
	    td.scale_in_bits = false;
	} else if (dbType.equals("NUMERIC")
		|| dbType.equals("DECIMAL")
		|| dbType.equals("NUMBER")
		|| dbType.equals("VARNUM")) {
	    td.type = TypeDescription.FIXED;
	    td.size = size.intValue();
	    td.size_in_bits = false;
	    td.scale = scale.intValue();
	    td.scale_in_bits = false;
	} else if (dbType.equals("CHAR")
		|| dbType.equals("CHARACTER")) {
	    td.type = TypeDescription.CHAR;
	    td.size = size.intValue();
	} else if (dbType.equals("NCHAR")) {
	    td.type = TypeDescription.NCHAR;
	    td.size = size.intValue();
	} else if (dbType.equals("VARCHAR")
		|| dbType.equals("VARCHAR2")) {
	    td.type = TypeDescription.VARCHAR;
	    td.size = size.intValue();
	} else if (dbType.equals("NVARCHAR")
		|| dbType.equals("NVARCHAR2")) {
	    td.type = TypeDescription.VARNCHAR;
	    td.size = size.intValue();
	} else if (dbType.equals("LONGVARCHAR")
		|| dbType.equals("TEXT")
		|| dbType.equals("LONG")
		|| dbType.equals("CLOB")) {
	    td.type = TypeDescription.LONGVARCHAR;
	} else if (dbType.equals("LONGNVARCHAR")
		|| dbType.equals("NTEXT")) {
	    td.type = TypeDescription.LONGVARNCHAR;
	// The following are types not mentioned in the SmallSQL doc,
	// but which do occur in the sample database...
	} else if (dbType.equals("BINARY")) {
	    td.type = TypeDescription.VARRAW;
	    td.size = size.intValue();
	} else if (dbType.equals("VARBINARY")) {
	    td.type = TypeDescription.VARRAW;
	    td.size = size.intValue();
	} else if (dbType.equals("LONGVARBINARY")) {
	    td.type = TypeDescription.LONGVARRAW;
	} else if (dbType.equals("DATETIME")
		|| dbType.equals("SMALLDATETIME"))
	    td.type = TypeDescription.TIMESTAMP;
	else {
	    // Unexpected/unsupported value. Don't know how to handle it so
	    // we tag it UNKNOWN, which will cause the script generator to pass
	    // it on uninterpreted and unchanged.
	    td.type = TypeDescription.UNKNOWN;
	}

	return td;
    }

    protected String printType(TypeDescription td) {
	switch (td.type) {
	    case TypeDescription.UNKNOWN: {
		return td.native_representation;
	    }
	    case TypeDescription.FIXED: {
		if (td.size_in_bits) {
		    // Look for best match within SmallSQL's binary types.
		    if (td.scale == 0) {
			if (td.size == 1)
			    return "BIT";
			else if (td.size <= 8)
			    return "TINYINT";
			else if (td.size <= 16)
			    return "SMALLINT";
			else if (td.size <= 32)
			    return "INT";
			else if (td.size <= 64)
			    return "BIGINT";
		    } else if (!td.scale_in_bits && td.scale == 4) {
			if (td.size <= 32)
			    return "SMALLMONEY";
			else if (td.size <= 64)
			    return "MONEY";
		    }
		}

		int size;
		if (td.size_in_bits)
		    size = (int) Math.ceil(td.size * LOG10_2);
		else
		    size = td.size;
		// TODO - check limits on size

		int scale;
		if (td.scale_in_bits)
		    scale = (int) Math.ceil(td.scale * LOG10_2);
		else
		    scale = td.scale;
		// TODO - check limits on scale

		if (scale == 0)
		    return "NUMERIC(" + size + ")";
		else
		    return "NUMERIC(" + size + ", " + scale + ")";
	    }
	    case TypeDescription.FLOAT: {
		int size;
		if (td.size_in_bits)
		    size = td.size;
		else
		    size = (int) Math.ceil(td.size / LOG10_2);
		
		int min_exp, max_exp;
		if (td.exp_of_2) {
		    min_exp = td.min_exp;
		    max_exp = td.max_exp;
		} else {
		    min_exp = (int) -Math.ceil(-td.min_exp / LOG10_2);
		    max_exp = (int) Math.ceil(td.max_exp / LOG10_2);
		}
		
		if (size <= 24 && min_exp >= -127 && max_exp <= 127)
		    return "REAL";
		if (size > 54 || min_exp < -1023 || max_exp > 1023)
		    /* TODO - Warning */;
		return "DOUBLE";
	    }
	    case TypeDescription.CHAR: {
		return "CHAR(" + td.size + ")";
	    }
	    case TypeDescription.NCHAR: {
		return "NCHAR(" + td.size + ")";
	    }
	    case TypeDescription.VARCHAR: {
		return "VARCHAR(" + td.size + ")";
	    }
	    case TypeDescription.VARNCHAR: {
		return "NVARCHAR(" + td.size + ")";
	    }
	    case TypeDescription.LONGVARCHAR: {
		return "LONGVARCHAR";
	    }
	    case TypeDescription.LONGVARNCHAR: {
		return "LONGNVARCHAR";
	    }
	    case TypeDescription.RAW: {
		return "BINARY(" + td.size + ")";
	    }
	    case TypeDescription.VARRAW: {
		return "VARBINARY(" + td.size + ")";
	    }
	    case TypeDescription.LONGVARRAW: {
		return "LONGVARBINARY";
	    }
	    case TypeDescription.DATE: {
		return "DATE";
	    }
	    case TypeDescription.TIME:
	    case TypeDescription.TIME_TZ: {
		return "TIME";
	    }
	    case TypeDescription.TIMESTAMP:
	    case TypeDescription.TIMESTAMP_TZ: {
		return "TIMESTAMP";
	    }
	    case TypeDescription.INTERVAL_YM: {
		// TODO - Warning
		return "NUMERIC(6)";
	    }
	    case TypeDescription.INTERVAL_DS: {
		// TODO - Warning
		return "NUMERIC(8)";
	    }
	    default: {
		// TODO - Warning (internal error); should never get here
		return td.native_representation;
	    }
	}
    }
}
