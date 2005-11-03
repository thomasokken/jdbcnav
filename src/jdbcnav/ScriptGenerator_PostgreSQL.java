package jdbcnav;

import jdbcnav.model.TypeDescription;

public class ScriptGenerator_PostgreSQL extends ScriptGenerator {
    public TypeDescription getTypeDescription(String dbType, Integer size,
					      Integer scale) {
	// NOTE: We don't populate the part_of_key, part_of_index, and
	// native_representation here; that is left to our caller,
	// BasicTable.getTypeDescription().

	TypeDescription td = new TypeDescription();
	if (dbType.equals("bigint")) {
	    td.type = TypeDescription.FIXED;
	    td.size = 64;
	    td.size_in_bits = true;
	    td.scale = 0;
	    td.scale_in_bits = true;
	} else if (dbType.equals("integer")
		|| dbType.equals("int")
		|| dbType.equals("int4")) {
	    td.type = TypeDescription.FIXED;
	    td.size = 32;
	    td.size_in_bits = true;
	    td.scale = 0;
	    td.scale_in_bits = true;
	} else if (dbType.equals("smallint")
		|| dbType.equals("int2")) {
	    td.type = TypeDescription.FIXED;
	    td.size = 16;
	    td.size_in_bits = true;
	    td.scale = 0;
	    td.scale_in_bits = true;
	} else if (dbType.equals("real")
		|| dbType.equals("float4")) {
	    td.type = TypeDescription.FLOAT;
	    td.size = 24;
	    td.size_in_bits = true;
	    td.min_exp = -127;
	    td.max_exp = 127;
	    td.exp_of_2 = true;
	} else if (dbType.equals("double precision")
		|| dbType.equals("float8")) {
	    td.type = TypeDescription.FLOAT;
	    td.size = 54;
	    td.size_in_bits = true;
	    td.min_exp = -1023;
	    td.max_exp = 1023;
	    td.exp_of_2 = true;
	} else if (dbType.equals("numeric")
		|| dbType.equals("decimal")) {
	    if (size.intValue() == 65535 && scale.intValue() == 65531) {
		// TODO: This type does not fit in the current TypeDescription
		// model. It is an arbitrary-precision (up to 1000 digits)
		// number without scale coercion, or, to put it differently,
		// a high-precision floating-point number.
		// I choose double-precision since it's the best match in
		// terms of allowing the original type's dynamic range, if not
		// its precision.
		// TODO - Warning
		td.type = TypeDescription.FLOAT;
		td.size = 54;
		td.size_in_bits = true;
		td.min_exp = -1023;
		td.max_exp = 1023;
		td.exp_of_2 = true;
	    } else {
		td.type = TypeDescription.FIXED;
		td.size = size.intValue();
		td.size_in_bits = false;
		td.scale = scale.intValue();
		td.scale_in_bits = false;
	    }
	} else if (dbType.equals("money")) {
	    // Deprecated type, so although we recognize it, printType() will
	    // never generate it.
	    td.type = TypeDescription.FIXED;
	    td.size = 32;
	    td.size_in_bits = true;
	    td.scale = 2;
	    td.scale_in_bits = false;
	} else if (dbType.equals("date")) {
	    td.type = TypeDescription.DATE;
	} else if (dbType.equals("time")) {
	    td.type = TypeDescription.TIME;
	} else if (dbType.equals("time with time zone")) {
	    td.type = TypeDescription.TIME_TZ;
	} else if (dbType.equals("timestamp")) {
	    td.type = TypeDescription.TIMESTAMP;
	} else if (dbType.equals("timestamp with time zone")) {
	    td.type = TypeDescription.TIMESTAMP_TZ;
	} else if (dbType.equals("interval")) {
	    // Yuck; PostgreSQL does not distinguish between
	    // INTERVAL YEAR TO MONTH and INTERVAL DAY TO SECOND; it has one
	    // type that is basically INTERVAL YEAR TO SECOND. I convert this
	    // to INTERVAL DAY TO SECOND, because I don't want to lose the
	    // resolution.
	    td.type = TypeDescription.INTERVAL_DS;
	} else if (dbType.equals("bytea")) {
	    td.type = TypeDescription.LONGVARRAW;
	} else if (dbType.equals("char")
		|| dbType.equals("bpchar")
		|| dbType.equals("character")) {
	    td.type = TypeDescription.CHAR;
	    td.size = size.intValue();
	} else if (dbType.equals("varchar")
		|| dbType.equals("character varying")) {
	    if (size.intValue() == 0)
		td.type = TypeDescription.LONGVARCHAR;
	    else {
		td.type = TypeDescription.VARCHAR;
		td.size = size.intValue();
	    }
	} else if (dbType.equals("text")) {
	    td.type = TypeDescription.LONGVARCHAR;
	} else {
	    // Unsupported value, such as one of PostgreSQL's geometric data
	    // types or bit strings. Don't know how to handle them so we tag
	    // them UNKNOWN, which will cause the script generator to pass them
	    // on uninterpreted and unchanged.
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
		if (td.size_in_bits && td.scale == 0) {
		    if (td.size <= 16)
			return "smallint";
		    else if (td.size <= 32)
			return "integer";
		    else if (td.size <= 64)
			return "bigint";
		}

		int size;
		if (td.size_in_bits)
		    size = (int) Math.ceil(td.size * LOG10_2);
		else
		    size = td.size;
		if (size > 1000) {
		    // TODO - Warning
		    size = 1000;
		}

		int scale;
		if (td.scale_in_bits)
		    scale = (int) Math.ceil(td.scale * LOG10_2);
		else
		    scale = td.scale;
		if (scale > size) {
		    // TODO - Warning
		    scale = size;
		}

		if (scale == 0)
		    return "numeric(" + size + ")";
		else
		    return "numeric(" + size + ", " + scale + ")";
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
		    return "real";
		else if (size <= 54 && min_exp >= 1023 && max_exp <= 1023)
		    return "double precision";
		else
		    return "numeric";
	    }
	    case TypeDescription.CHAR:
	    case TypeDescription.NCHAR: {
		return "char(" + td.size + ")";
	    }
	    case TypeDescription.VARCHAR:
	    case TypeDescription.VARNCHAR: {
		return "varchar(" + td.size + ")";
	    }
	    case TypeDescription.LONGVARCHAR:
	    case TypeDescription.LONGVARNCHAR: {
		// TODO -- What's the difference between varchar(0) and text?
		return "text";
	    }
	    case TypeDescription.RAW:
	    case TypeDescription.VARRAW:
	    case TypeDescription.LONGVARRAW: {
		return "bytea";
	    }
	    case TypeDescription.DATE: {
		return "date";
	    }
	    case TypeDescription.TIME: {
		return "time";
	    }
	    case TypeDescription.TIME_TZ: {
		return "time with time zone";
	    }
	    case TypeDescription.TIMESTAMP: {
		return "timestamp";
	    }
	    case TypeDescription.TIMESTAMP_TZ: {
		return "timestamp with time zone";
	    }
	    case TypeDescription.INTERVAL_YM:
	    case TypeDescription.INTERVAL_DS: {
		return "interval";
	    }
	    default: {
		// TODO - Warning (internal error); should never get here
		return td.native_representation;
	    }
	}
    }
}
