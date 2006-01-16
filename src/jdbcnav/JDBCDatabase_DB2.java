///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2006  Thomas Okken
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


public class JDBCDatabase_DB2 extends JDBCDatabase {
    public JDBCDatabase_DB2(String name, String driver, Connection con) {
	super(name, driver, con);
    }

    public String makeQualifiedName(String catalog, String schema,
							String name) {
	if (schema != null)
	    schema = schema.trim();
	return super.makeQualifiedName(catalog, schema, name);
    }

    protected String qualifyName(String name) {
	// TODO: You can qualify a name by prepending the current user ID;
	// unfortunately, I don't know how to get it. (Oracle has a USER
	// function but I haven't found anything like that in the DB2 SQL
	// docs.) I could get it done by recording the username in the
	// JDBCDatabase object; that being done, the Oracle implementation
	// of qualifyName() could be simplified also.
	// Update: what I'm looking for is the CURRENT_USER function.
	// Next question: what's the DB2 equivalent of Oracle's
	// SELECT <foo> FROM DUAL?
	return name;
    }

    protected TypeSpec makeTypeSpec(String dbType, Integer size, Integer scale,
				    int sqlType, String javaType) {
	TypeSpec spec = super.makeTypeSpec(dbType, size, scale, sqlType,
								javaType);

	if (dbType.equals("SMALLINT")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = 16;
	    spec.size_in_bits = true;
	    spec.scale = 0;
	    spec.scale_in_bits = true;
	} else if (dbType.equals("INTEGER")) {
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
	} else if (dbType.equals("DECIMAL")) {
	    spec.type = TypeSpec.FIXED;
	    spec.size = size.intValue();
	    spec.size_in_bits = false;
	    spec.scale = scale.intValue();
	    spec.scale_in_bits = false;
	} else if (dbType.equals("REAL")) {
	    spec.type = TypeSpec.FLOAT;
	    spec.size = 24;
	    spec.size_in_bits = true;
	    spec.min_exp = -127;
	    spec.max_exp = 127;
	    spec.exp_of_2 = true;
	} else if (dbType.equals("DOUBLE")) {
	    spec.type = TypeSpec.FLOAT;
	    spec.size = 54;
	    spec.size_in_bits = true;
	    spec.min_exp = -1023;
	    spec.max_exp = 1023;
	    spec.exp_of_2 = true;
	} else if (dbType.equals("CHAR")) {
	    spec.type = TypeSpec.CHAR;
	    spec.size = size.intValue();
	} else if (spec.jdbcSqlType == Types.LONGVARCHAR) {
	    spec.type = TypeSpec.LONGVARCHAR;
	} else if (spec.jdbcSqlType == Types.BINARY) {
	    spec.type = TypeSpec.RAW;
	    spec.size = size.intValue();
	} else if (spec.jdbcSqlType == Types.VARBINARY) {
	    spec.type = TypeSpec.VARRAW;
	    spec.size = size.intValue();
	} else if (spec.jdbcSqlType == Types.LONGVARBINARY) {
	    spec.type = TypeSpec.LONGVARRAW;
	} else if (dbType.equals("VARCHAR")) {
	    spec.type = TypeSpec.VARCHAR;
	    spec.size = size.intValue();
	} else if (dbType.equals("CLOB")) {
	    spec.type = TypeSpec.LONGVARCHAR;
	} else if (dbType.equals("GRAPHIC")) {
	    spec.type = TypeSpec.NCHAR;
	    spec.size = size.intValue() / 2;
	    size = new Integer(spec.size);
	} else if (dbType.equals("VARGRAPHIC")) {
	    spec.type = TypeSpec.VARNCHAR;
	    spec.size = size.intValue() / 2;
	    size = new Integer(spec.size);
	} else if (dbType.equals("DBCLOB")) {
	    spec.type = TypeSpec.LONGVARNCHAR;
	} else if (dbType.equals("BLOB")) {
	    spec.type = TypeSpec.LONGVARRAW;
	} else if (dbType.equals("DATE")) {
	    spec.type = TypeSpec.DATE;
	} else if (dbType.equals("TIME")) {
	    spec.type = TypeSpec.TIME;
	    spec.size = 0;
	} else if (dbType.equals("TIMESTAMP")) {
	    spec.type = TypeSpec.TIMESTAMP;
	    spec.size = 6;
	} else {
	    spec.type = TypeSpec.UNKNOWN;
	}

	if (dbType.equals("DECIMAL")) {
	    // 'size' and 'scale' both relevant
	} else if (dbType.equals("CHAR")
		|| dbType.equals("VARCHAR")
		|| spec.type == TypeSpec.RAW
		|| spec.type == TypeSpec.VARRAW
		|| dbType.equals("GRAPHIC")
		|| dbType.equals("VARGRAPHIC")
		|| dbType.equals("BLOB")
		|| dbType.equals("CLOB")
		|| dbType.equals("DBCLOB")) {
	    scale = null;
	} else {
	    size = null;
	    scale = null;
	}
	
	if (spec.type == TypeSpec.RAW)
	    spec.native_representation = "CHAR(" + size + ") FOR BIT DATA";
	else if (spec.type == TypeSpec.VARRAW)
	    spec.native_representation = "VARCHAR(" + size + ") FOR BIT DATA";
	else if (dbType.equals("BLOB")
		|| dbType.equals("CLOB")
		|| dbType.equals("DBCLOB")) {
	    StringBuffer buf = new StringBuffer();
	    int sz = size.intValue();
	    if (dbType.equals("DBCLOB"))
		sz /= 2;
	    buf.append(dbType);
	    buf.append('(');
	    if ((sz & 1073741823) == 0) {
		buf.append(sz / 1073741824);
		buf.append('G');
	    } else if ((sz & 1048575) == 0) {
		buf.append(sz / 1048576);
		buf.append('M');
	    } else if ((sz & 1023) == 0) {
		buf.append(sz / 1024);
		buf.append('K');
	    } else
		buf.append(sz);
	    buf.append(')');
	    spec.native_representation = buf.toString();
	} else if (size == null)
	    spec.native_representation = dbType;
	else if (scale == null)
	    spec.native_representation = dbType + "(" + size + ")";
	else
	    spec.native_representation = dbType + "(" + size + ", " + scale + ")";

	return spec;
    }
}
