///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2005  Thomas Okken
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

package jdbcnav;

import java.sql.*;
import jdbcnav.model.TypeSpec;


public class JDBCDatabase_SmallSQL extends JDBCDatabase {
    public JDBCDatabase_SmallSQL(String name, String driver, Connection con) {
	super(name, driver, con);
    }

    protected boolean showCatalogs() {
	return false;
    }

    protected TypeSpec makeTypeSpec(String dbType, Integer size, Integer scale,
				    int sqlType, String javaType) {
	TypeSpec spec = super.makeTypeSpec(dbType, size, scale, sqlType,
								javaType);
	if (dbType.equals("BIT")
		|| dbType.equals("BOOLEAN")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = 1;
	    spec.size_in_bits = true;
	    spec.scale = 0;
	    spec.scale_in_bits = true;
	} else if (dbType.equals("TINYINT")
		|| dbType.equals("BYTE")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = 8;
	    spec.size_in_bits = true;
	    spec.scale = 0;
	    spec.scale_in_bits = true;
	} else if (dbType.equals("SMALLINT")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = 16;
	    spec.size_in_bits = true;
	    spec.scale = 0;
	    spec.scale_in_bits = true;
	} else if (dbType.equals("INT")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = 32;
	    spec.size_in_bits = true;
	    spec.scale = 0;
	    spec.scale_in_bits = true;
	} else if (dbType.equals("BIGINT")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = 64;
	    spec.size_in_bits = true;
	    spec.scale = 0;
	    spec.scale_in_bits = true;
	} else if (dbType.equals("REAL")) {
	    spec.type = TypeSpec.FLOAT;
	    spec.size = 24;
	    spec.size_in_bits = true;
	    spec.min_exp = -127;
	    spec.max_exp = 127;
	    spec.exp_of_2 = true;
	} else if (dbType.equals("DOUBLE")
		|| dbType.equals("FLOAT")) {
	    spec.type = TypeSpec.FLOAT;
	    spec.size = 54;
	    spec.size_in_bits = true;
	    spec.min_exp = -1023;
	    spec.max_exp = 1023;
	    spec.exp_of_2 = true;
	} else if (dbType.equals("MONEY")) {
	    // TODO - verify
	    spec.type = TypeSpec.FIXED;
	    spec.size = 64;
	    spec.size_in_bits = true;
	    spec.scale = 4;
	    spec.scale_in_bits = false;
	} else if (dbType.equals("SMALLMONEY")) {
	    // TODO - verify
	    spec.type = TypeSpec.FIXED;
	    spec.size = 32;
	    spec.size_in_bits = true;
	    spec.scale = 4;
	    spec.scale_in_bits = false;
	} else if (dbType.equals("NUMERIC")
		|| dbType.equals("DECIMAL")
		|| dbType.equals("NUMBER")
		|| dbType.equals("VARNUM")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = size.intValue();
	    spec.size_in_bits = false;
	    spec.scale = scale.intValue();
	    spec.scale_in_bits = false;
	} else if (dbType.equals("CHAR")
		|| dbType.equals("CHARACTER")) {
	    spec.type = TypeSpec.CHAR;
	    spec.size = size.intValue();
	} else if (dbType.equals("NCHAR")) {
	    spec.type = TypeSpec.NCHAR;
	    spec.size = size.intValue();
	} else if (dbType.equals("VARCHAR")
		|| dbType.equals("VARCHAR2")) {
	    spec.type = TypeSpec.VARCHAR;
	    spec.size = size.intValue();
	} else if (dbType.equals("NVARCHAR")
		|| dbType.equals("NVARCHAR2")) {
	    spec.type = TypeSpec.VARNCHAR;
	    spec.size = size.intValue();
	} else if (dbType.equals("LONGVARCHAR")
		|| dbType.equals("TEXT")
		|| dbType.equals("LONG")
		|| dbType.equals("CLOB")) {
	    spec.type = TypeSpec.LONGVARCHAR;
	} else if (dbType.equals("LONGNVARCHAR")
		|| dbType.equals("NTEXT")
		|| dbType.equals("NCLOB")) {
	    spec.type = TypeSpec.LONGVARNCHAR;
	// The following are types not mentioned in the SmallSQL doc,
	// but which do occur in the sample database...
	} else if (dbType.equals("BINARY")) {
	    spec.type = TypeSpec.VARRAW;
	    spec.size = size.intValue();
	} else if (dbType.equals("VARBINARY")
		|| dbType.equals("RAW")) {
	    spec.type = TypeSpec.VARRAW;
	    spec.size = size.intValue();
	} else if (dbType.equals("LONGVARBINARY")
		|| dbType.equals("IMAGE")
		|| dbType.equals("LONG RAW")
		|| dbType.equals("BLOB")) {
	    spec.type = TypeSpec.LONGVARRAW;
	} else if (dbType.equals("DATE")) {
	    spec.type = TypeSpec.DATE;
	} else if (dbType.equals("TIME")) {
	    spec.type = TypeSpec.TIME;
	    spec.size = 0;
	} else if (dbType.equals("DATETIME")
		|| dbType.equals("TIMESTAMP")) {
	    spec.type = TypeSpec.TIMESTAMP;
	    spec.size = 3;
	} else if (dbType.equals("SMALLDATETIME")) {
	    spec.type = TypeSpec.TIMESTAMP;
	    spec.size = 0; // Actually, precision is minutes
	} else {
	    // Unexpected/unsupported value. Don't know how to handle it so
	    // we tag it UNKNOWN, which will cause the script generator to pass
	    // it on uninterpreted and unchanged.
	    spec.type = TypeSpec.UNKNOWN;
	}

	if (dbType.equals("NUMERIC")
		|| dbType.equals("DECIMAL")
		|| dbType.equals("NUMBER")
		|| dbType.equals("VARNUM")) {
	    // 'scale' is optional, but the SmallSQL driver
	    // does not distinguish between scale == null and
	    // scale == 0 (null is handled as 0).
	} else if (dbType.equals("CHAR")
		|| dbType.equals("CHARACTER")
		|| dbType.equals("NCHAR")
		|| dbType.equals("VARCHAR")
		|| dbType.equals("NVARCHAR")
		|| dbType.equals("VARCHAR2")
		|| dbType.equals("NVARCHAR2")
		|| dbType.equals("BINARY")
		|| dbType.equals("VARBINARY")) {
	    scale = null;
	} else {
	    size = null;
	    scale = null;
	}

	if (size == null)
	    spec.native_representation = dbType;
	else if (scale == null)
	    spec.native_representation = dbType + "(" + size + ")";
	else
	    spec.native_representation = dbType + "(" + size + ", " + scale + ")";
	
	return spec;
    }
}
