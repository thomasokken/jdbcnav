package jdbcnav;

import java.text.*;
import jdbcnav.model.Index;
import jdbcnav.model.PrimaryKey;
import jdbcnav.model.Table;
import jdbcnav.model.TypeDescription;

public class ScriptGenerator_Oracle extends ScriptGenerator {
    private static SimpleDateFormat dateFormat =
	new SimpleDateFormat("yyyy-MM-dd");
    private static SimpleDateFormat timeFormat =
	new SimpleDateFormat("HH:mm:ss");
    private static SimpleDateFormat dateTimeFormat =
	new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    protected boolean oracle9types = true;
    protected boolean oracle10types = true;

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
    public TypeDescription getTypeDescription(String dbType, Integer size,
					      Integer scale) {
	// NOTE: We don't populate the part_of_key and part_of_index
	// that is left to our caller, BasicTable.getTypeDescription().
	// Populating native_representation is optional.

	TypeDescription td = new TypeDescription();
	if (dbType.equals("CHAR")) {
	    td.type = TypeDescription.CHAR;
	    td.size = size.intValue();
	} else if (dbType.equals("VARCHAR2")) {
	    td.type = TypeDescription.VARCHAR;
	    td.size = size.intValue();
	} else if (dbType.equals("NCHAR")) {
	    td.type = TypeDescription.NCHAR;
	    td.size = size.intValue();
	} else if (dbType.equals("NVARCHAR2")) {
	    td.type = TypeDescription.VARNCHAR;
	    td.size = size.intValue();
	} else if (dbType.equals("NUMBER")) {
	    if (scale == null) {
		td.type = TypeDescription.FLOAT;
		// TODO: Is it really 38 decimal digits,
		// or is it actually 126 bits?
		td.size = 38;
		td.size_in_bits = false;
		td.min_exp = -130;
		td.max_exp = 125;
		td.exp_of_2 = false;
	    } else {
		td.type = TypeDescription.FIXED;
		td.size = size.intValue();
		td.size_in_bits = false;
		td.scale = scale.intValue();
		td.scale_in_bits = false;
	    }
	} else if (dbType.equals("FLOAT")) {
	    td.type = TypeDescription.FLOAT;
	    td.size = size.intValue();
	    td.size_in_bits = true;
	    td.min_exp = -130;
	    td.max_exp = 125;
	    td.exp_of_2 = false;
	} else if (dbType.equals("BINARY_FLOAT")) {
	    td.type = TypeDescription.FLOAT;
	    td.size = 24;
	    td.size_in_bits = true;
	    td.min_exp = -127;
	    td.max_exp = 127;
	    td.exp_of_2 = true;
	} else if (dbType.equals("BINARY_DOUBLE")) {
	    td.type = TypeDescription.FLOAT;
	    td.size = 54;
	    td.size_in_bits = true;
	    td.min_exp = -1023;
	    td.max_exp = 1023;
	    td.exp_of_2 = true;
	} else if (dbType.equals("LONG")) {
	    td.type = TypeDescription.LONGVARCHAR;
	} else if (dbType.equals("LONG RAW")) {
	    td.type = TypeDescription.LONGVARRAW;
	} else if (dbType.equals("RAW")) {
	    td.type = TypeDescription.VARRAW;
	    td.size = size.intValue();
	} else if (dbType.equals("DATE")) {
	    td.type = TypeDescription.TIMESTAMP;
	    td.size = 0;
	} else if (dbType.startsWith("TIMESTAMP")) {
	    if (dbType.endsWith("WITH LOCAL TIME ZONE"))
		td.type = TypeDescription.TIMESTAMP;
	    else if (dbType.endsWith("WITH TIME ZONE"))
		td.type = TypeDescription.TIMESTAMP_TZ;
	    else
		td.type = TypeDescription.TIMESTAMP;
	    td.size = scale.intValue();
	} else if (dbType.startsWith("INTERVAL YEAR")) {
	    td.type = TypeDescription.INTERVAL_YM;
	    td.size = size.intValue();
	} else if (dbType.startsWith("INTERVAL DAY")) {
	    td.type = TypeDescription.INTERVAL_DS;
	    td.size = size.intValue();
	    td.scale = scale.intValue();
	} else if (dbType.equals("BLOB")) {
	    td.type = TypeDescription.LONGVARRAW;
	} else if (dbType.equals("CLOB")) {
	    td.type = TypeDescription.LONGVARCHAR;
	} else if (dbType.equals("NCLOB")) {
	    td.type = TypeDescription.LONGVARNCHAR;
	} else {
	    // BFILE, ROWID, UROWID, or something new.
	    // Don't know how to handle them so we tag them UNKNOWN,
	    // which will cause the script generator to pass them on
	    // uninterpreted and unchanged.
	    td.type = TypeDescription.UNKNOWN;
	}

	// Populate native_representation for the benefit of the SameAsSource
	// script generator.

	if (dbType.startsWith("INTERVAL YEAR"))
	    td.native_representation = "INTERVAL YEAR(" + size + ") TO MONTH";
	else if (dbType.startsWith("INTERVAL DAY"))
	    td.native_representation = "INTERVAL DAY(" + size
		+ ") TO SECOND(" + scale + ")";
	else if (dbType.startsWith("TIMESTAMP")) {
	    td.native_representation = "TIMESTAMP(" + scale + ")";
	    if (dbType.endsWith("WITH LOCAL TIME ZONE"))
		td.native_representation += " WITH LOCAL TIME ZONE";
	    else if (dbType.endsWith("WITH TIME ZONE"))
		td.native_representation += " WITH TIME ZONE";
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
		td.native_representation = dbType;
	    else if (scale == null)
		td.native_representation = dbType + "(" + size + ")";
	    else
		td.native_representation = dbType + "(" + size + ", "
							+ scale + ")";
	}

	return td;
    }

    protected String printType(TypeDescription td) {
	switch (td.type) {
	    case TypeDescription.UNKNOWN: {
		return td.native_representation;
	    }
	    case TypeDescription.FIXED: {
		int size;
		if (td.size_in_bits)
		    size = (int) Math.ceil(td.size * LOG10_2);
		else
		    size = td.size;
		if (size > 38) {
		    // TODO - Warning
		    size = 38;
		}

		int scale;
		if (td.scale_in_bits)
		    scale = (int) Math.ceil(td.scale * LOG10_2);
		else
		    scale = td.scale;
		if (scale < -84) {
		    // TODO - Warning
		    scale = -84;
		} else if (scale > 127) {
		    // TODO - Warning
		    scale = 127;
		}

		if (scale == 0)
		    return "NUMBER(" + size + ")";
		else
		    return "NUMBER(" + size + ", " + scale + ")";
	    }
	    case TypeDescription.FLOAT: {
		int size;
		if (td.size_in_bits)
		    size = td.size;
		else
		    size = (int) Math.ceil(td.size / LOG10_2);

		// If possible, use binary float/double
		if (oracle10types) {
		    int min_exp, max_exp;
		    if (td.exp_of_2) {
			min_exp = td.min_exp;
			max_exp = td.max_exp;
		    } else {
			min_exp = (int) -Math.ceil(-td.min_exp / LOG10_2);
			max_exp = (int) Math.ceil(td.max_exp / LOG10_2);
		    }
		    if (size <= 24 && min_exp >= -127 && max_exp <= 127)
			return "BINARY_FLOAT";
		    else if (size <= 54 && min_exp >= -1023 && max_exp <= 1023)
			return "BINARY_DOUBLE";
		}

		int min_exp, max_exp;
		if (td.exp_of_2) {
		    min_exp = (int) -Math.ceil(-td.min_exp * LOG10_2);
		    max_exp = (int) Math.ceil(td.max_exp * LOG10_2);
		} else {
		    min_exp = td.min_exp;
		    max_exp = td.max_exp;
		}
		if (min_exp < -130 || max_exp > 125)
		    /* TODO - Warning */;

		if (size <= 126)
		    return "FLOAT(" + size + ")";
		else
		    // TODO - Warning
		    return "NUMBER";
	    }
	    case TypeDescription.CHAR: {
		if (td.size > 2000) {
		    // TODO - Warning
		    td.size = 2000;
		}
		return "CHAR(" + td.size + ")";
	    }
	    case TypeDescription.VARCHAR: {
		if (td.size > 4000) {
		    // TODO - Warning
		    td.size = 4000;
		}
		return "VARCHAR2(" + td.size + ")";
	    }
	    case TypeDescription.LONGVARCHAR: {
		if (td.part_of_key || td.part_of_index) {
		    // TODO - Warning
		    return "VARCHAR2(4000)";
		} else
		    return "CLOB";
	    }
	    case TypeDescription.NCHAR: {
		if (td.size > 2000) {
		    // TODO - Warning
		    td.size = 2000;
		}
		return "NCHAR(" + td.size + ")";
	    }
	    case TypeDescription.VARNCHAR: {
		if (td.size > 4000) {
		    // TODO - Warning
		    td.size = 4000;
		}
		return "NVARCHAR2(" + td.size + ")";
	    }
	    case TypeDescription.LONGVARNCHAR: {
		if (td.part_of_key || td.part_of_index) {
		    // TODO - Warning
		    return "NVARCHAR2(4000)";
		} else
		    return "NCLOB";
	    }
	    case TypeDescription.RAW:
	    case TypeDescription.VARRAW: {
		if (td.size > 4000) {
		    // TODO - Warning
		    td.size = 4000;
		}
		return "RAW(" + td.size + ")";
	    }
	    case TypeDescription.LONGVARRAW: {
		if (td.part_of_key || td.part_of_index) {
		    // TODO - Warning
		    return "RAW(4000)";
		} else
		    return "BLOB";
	    }
	    case TypeDescription.DATE: {
		return "DATE";
	    }
	    case TypeDescription.TIME: {
		// TODO - Warning
		if (oracle9types)
		    return "TIMESTAMP(" + td.size + ")";
		else
		    return "DATE";
	    }
	    case TypeDescription.TIME_TZ: {
		// TODO - Warning
		if (oracle9types)
		    return "TIMESTAMP(" + td.size + ") WITH TIME ZONE";
		else
		    return "DATE";
	    }
	    case TypeDescription.TIMESTAMP: {
		if (oracle9types)
		    return "TIMESTAMP(" + td.size + ")";
		else
		    return "DATE";
	    }
	    case TypeDescription.TIMESTAMP_TZ: {
		if (oracle9types)
		    return "TIMESTAMP(" + td.size + ") WITH TIME ZONE";
		else
		    return "DATE";
	    }
	    case TypeDescription.INTERVAL_YM: {
		if (oracle10types)
		    return "INTERVAL YEAR(" + td.size + ") TO MONTH";
		else
		    // TODO - Warning
		    // TODO - Take 'size' into account
		    return "NUMBER(6)";
	    }
	    case TypeDescription.INTERVAL_DS: {
		if (oracle10types)
		    return "INTERVAL DAY(" + td.size
				+ ") TO SECOND(" + td.scale + ")";
		else
		    // TODO - Warning
		    // TODO - Take 'size' and 'scale' into account
		    return "NUMBER(8)";
	    }
	    default: {
		// TODO - Warning (internal error); should never get here
		return td.native_representation;
	    }
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
