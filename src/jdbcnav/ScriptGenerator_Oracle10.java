package jdbcnav;

import java.text.*;
import jdbcnav.model.Index;
import jdbcnav.model.PrimaryKey;
import jdbcnav.model.Table;
import jdbcnav.model.TypeDescription;

public class ScriptGenerator_Oracle10 extends ScriptGenerator {
    private static SimpleDateFormat dateFormat =
	new SimpleDateFormat("yyyy-MM-dd");
    private static SimpleDateFormat timeFormat =
	new SimpleDateFormat("HH:mm:ss");
    private static SimpleDateFormat dateTimeFormat =
	new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
	} else if (dbType.equals("BINARY FLOAT")) {
	    td.type = TypeDescription.FLOAT;
	    td.size = 24;
	    td.size_in_bits = true;
	    td.min_exp = -127;
	    td.max_exp = 127;
	    td.exp_of_2 = true;
	} else if (dbType.equals("BINARY DOUBLE")) {
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
	} else if (dbType.equals("TIME")) {
	    td.type = TypeDescription.TIME;
	} else if (dbType.equals("TIME WITH TIME ZONE")) {
	    td.type = TypeDescription.TIME_TZ;
	} else if (dbType.equals("TIMESTAMP")) {
	    td.type = TypeDescription.TIMESTAMP;
	} else if (dbType.equals("TIMESTAMP WITH TIME ZONE")) {
	    td.type = TypeDescription.TIMESTAMP_TZ;
	} else if (dbType.equals("INTERVAL YEAR TO MONTH")) {
	    td.type = TypeDescription.INTERVAL_YM;
	} else if (dbType.equals("INTERVAL DAY TO SECOND")) {
	    td.type = TypeDescription.INTERVAL_DS;
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

	// TODO: INTERVAL etc.

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
	    td.native_representation = dbType + "(" + size + ", " + scale + ")";

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
		// TODO: Generate ANSI-type FLOAT(n) as well, where 'n' is the
		// number of bits in the mantissa. Note that Oracle's
		// implementation of FLOAT always uses an exponent with the
		// same properties as NUMBER, that is, an exponent of 10 with
		// range -130..125; the mantissa appears to be stored in
		// decimal, with the number of digits presumably being
		// ceil(n/log10(2)).
		if (oracle10types) {
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
			return "BINARY FLOAT";
		    else if (size <= 54 && min_exp >= -1023 && max_exp <= 1023)
			return "BINARY DOUBLE";
		}
		// TODO: Check for size/min_exp/max_exp out of Oracle's
		// allowed range -- meaning, size greater than 38 digits,
		// min_exp less than 10^(-130), or max_exp greater than 10^125.
		// If this is the case, emit warning.
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
		    // TODO -- Actually, is RAW even allowed in a key or
		    // in an index? Must check.
		    return "RAW(4000)";
		} else
		    return "BLOB";
	    }
	    case TypeDescription.DATE: {
		return "DATE";
	    }
	    case TypeDescription.TIME: {
		if (oracle10types)
		    return "TIME";
		else
		    return "DATE";
	    }
	    case TypeDescription.TIME_TZ: {
		if (oracle10types)
		    return "TIME WITH TIME ZONE";
		else
		    return "DATE";
	    }
	    case TypeDescription.TIMESTAMP: {
		if (oracle10types)
		    return "TIMESTAMP";
		else
		    return "DATE";
	    }
	    case TypeDescription.TIMESTAMP_TZ: {
		if (oracle10types)
		    return "TIMESTAMP WITH TIME ZONE";
		else
		    return "DATE";
	    }
	    case TypeDescription.INTERVAL_YM: {
		if (oracle10types)
		    return "INTERVAL YEAR TO MONTH";
		else
		    // TODO - Warning
		    return "NUMBER(6)";
	    }
	    case TypeDescription.INTERVAL_DS: {
		if (oracle10types)
		    return "INTERVAL DAY TO SECOND";
		else
		    // TODO - Warning
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
