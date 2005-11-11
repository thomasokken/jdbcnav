package jdbcnav;

import jdbcnav.model.TypeSpec;

public class ScriptGenerator_PostgreSQL extends ScriptGenerator {
    protected String printType(TypeSpec td) {
	switch (td.type) {
	    case TypeSpec.UNKNOWN: {
		return td.native_representation;
	    }
	    case TypeSpec.FIXED: {
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
	    case TypeSpec.FLOAT: {
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
	    case TypeSpec.CHAR:
	    case TypeSpec.NCHAR: {
		return "char(" + td.size + ")";
	    }
	    case TypeSpec.VARCHAR:
	    case TypeSpec.VARNCHAR: {
		return "varchar(" + td.size + ")";
	    }
	    case TypeSpec.LONGVARCHAR:
	    case TypeSpec.LONGVARNCHAR: {
		// TODO -- What's the difference between varchar(0) and text?
		return "text";
	    }
	    case TypeSpec.RAW:
	    case TypeSpec.VARRAW:
	    case TypeSpec.LONGVARRAW: {
		return "bytea";
	    }
	    case TypeSpec.DATE: {
		return "date";
	    }
	    case TypeSpec.TIME: {
		return "time";
	    }
	    case TypeSpec.TIME_TZ: {
		return "time with time zone";
	    }
	    case TypeSpec.TIMESTAMP: {
		return "timestamp";
	    }
	    case TypeSpec.TIMESTAMP_TZ: {
		return "timestamp with time zone";
	    }
	    case TypeSpec.INTERVAL_YM:
	    case TypeSpec.INTERVAL_DS: {
		return "interval";
	    }
	    default: {
		// TODO - Warning (internal error); should never get here
		return td.native_representation;
	    }
	}
    }
}
