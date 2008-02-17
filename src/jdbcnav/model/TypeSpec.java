///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2008  Thomas Okken
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License, version 2,
// as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
///////////////////////////////////////////////////////////////////////////////

package jdbcnav.model;

/**
 * This class is used as an intermediate representation of a column type.
 * It is used in preference to SQL in order to avoid any ambiguities; this
 * class captures all the information needed to accurately replicate a
 * data type without information loss. The ScriptGenerator class, when
 * generating CREATE TABLE statements, calls Table.getTypeSpec() to get an
 * accurate description of column types; it then calls
 * ScriptGenerator.printType() to find the appropriate DB-specific type.
 */
public class TypeSpec {
    // For the FIXED_x_y and FLOAT_x_y types, 'x' is the representation of the
    // number or mantissa (and 'size' is the number of bits or digits); 'y' is
    // the number that is raised to the scale (or exponent) to scale the
    // number. The scale (FIXED) or exponent range (FLOAT) is given in 'scale'
    // or 'min_exp'..'max_exp', respectively.

    public static final int CLASS = -1;
    public static final int UNKNOWN = 0;
    public static final int FIXED = 1;
    public static final int FLOAT = 2;
    public static final int CHAR = 3;
    public static final int VARCHAR = 4;
    public static final int LONGVARCHAR = 5;
    public static final int NCHAR = 6;
    public static final int VARNCHAR = 7;
    public static final int LONGVARNCHAR = 8;
    public static final int RAW = 9;
    public static final int VARRAW = 10;
    public static final int LONGVARRAW = 11;
    public static final int DATE = 12;
    public static final int TIME = 13;
    public static final int TIME_TZ = 14;
    public static final int TIMESTAMP = 15;
    public static final int TIMESTAMP_TZ = 16;
    public static final int INTERVAL_YM = 17;
    public static final int INTERVAL_DS = 18;
    public static final int INTERVAL_YS = 19;

    public Database db;

    public int type;
    public int size; // chars, digits, or bits; for FLOAT, mantissa size
    public boolean size_in_bits; // for FIXED & FLOAT
    public int scale; // for FIXED, INTERVAL_DS
    public boolean scale_in_bits;
    public int min_exp, max_exp; // for FLOAT
    public boolean exp_of_2;

    public boolean part_of_key;
    public boolean part_of_index;
    public String native_representation;

    public String jdbcDbType;
    public Integer jdbcSize;
    public Integer jdbcScale;
    public int jdbcSqlType;
    public String jdbcJavaType;
    public Class jdbcJavaClass;

    public TypeSpec(Database db) {
	this.db = db;
    }

    public String objectToString(Object o) {
	return db.objectToString(this, o);
    }

    public Object stringToObject(String s) {
	return db.stringToObject(this, s);
    }

    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append(super.toString());
	buf.append("[type=");
	switch (type) {
	    case CLASS: buf.append("CLASS"); break;
	    case UNKNOWN: buf.append("UNKNOWN"); break;
	    case FIXED: buf.append("FIXED"); break;
	    case FLOAT: buf.append("FLOAT"); break;
	    case CHAR: buf.append("CHAR"); break;
	    case VARCHAR: buf.append("VARCHAR"); break;
	    case LONGVARCHAR: buf.append("LONGVARCHAR"); break;
	    case NCHAR: buf.append("NCHAR"); break;
	    case VARNCHAR: buf.append("VARNCHAR"); break;
	    case LONGVARNCHAR: buf.append("LONGVARNCHAR"); break;
	    case RAW: buf.append("RAW"); break;
	    case VARRAW: buf.append("VARRAW"); break;
	    case LONGVARRAW: buf.append("LONGVARRAW"); break;
	    case DATE: buf.append("DATE"); break;
	    case TIME: buf.append("TIME"); break;
	    case TIME_TZ: buf.append("TIME_TZ"); break;
	    case TIMESTAMP: buf.append("TIMESTAMP"); break;
	    case TIMESTAMP_TZ: buf.append("TIMESTAMP_TZ"); break;
	    case INTERVAL_YM: buf.append("INTERVAL_YM"); break;
	    case INTERVAL_DS: buf.append("INTERVAL_DS"); break;
	    default: buf.append("?"); break;
	}
	buf.append(", size=" + size + " " + (size_in_bits ? "bits" : "digits")
		+ ", scale=" + scale + " " + (scale_in_bits ? "bits" : "digits")
		+ ", min_exp=" + min_exp + ", max_exp=" + max_exp
		+ ", exp_of_2=" + exp_of_2 + ", part_of_key=" + part_of_key
		+ ", part_of_index=" + part_of_index + ", jdbcDbType="
		+ jdbcDbType + ", jdbcSize=" + jdbcSize + ", jdbcScale="
		+ jdbcScale + ", jdbcSqlType="
		+ jdbcnav.util.MiscUtils.sqlTypeIntToString(jdbcSqlType)
		+ ", jdbcJavaType=" + jdbcJavaType + ", jdbcJavaClass="
		+ jdbcJavaClass.getName() + "]");
	return buf.toString();
    }
}
