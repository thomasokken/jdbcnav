///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2010  Thomas Okken
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
import jdbcnav.model.*;


public class JDBCDatabase_MS_SQL extends JDBCDatabase {
    public JDBCDatabase_MS_SQL(String name, String driver, Connection con) {
        super(name, driver, con);
    }

    protected boolean resultSetContainsTableInfo() {
        return false;
    }

    protected TypeSpec makeTypeSpec(String dbType, Integer size, Integer scale,
                                    int sqlType, String javaType) {
        TypeSpec spec = makeDefaultTypeSpec(dbType, size, scale, sqlType,
                                                                javaType);

        if (dbType.equals("char")) {
            spec.type = TypeSpec.CHAR;
            spec.size = size;
        } else if (dbType.equals("varchar")) {
            spec.type = TypeSpec.VARCHAR;
            spec.size = size;
            // TODO: varchar(max) => LONGVARCHAR
        } else if (dbType.equals("text")) {
            spec.type = TypeSpec.LONGVARCHAR;
        } else if (dbType.equals("nchar")) {
            spec.type = TypeSpec.NCHAR;
            spec.size = size;
        } else if (dbType.equals("nvarchar")) {
            spec.type = TypeSpec.VARNCHAR;
            spec.size = size;
            // TODO: nvarchar(max) => LONGVARNCHAR
        } else if (dbType.equals("ntext")) {
            spec.type = TypeSpec.LONGVARCHAR;
        } else if (dbType.equals("binary")) {
            spec.type = TypeSpec.RAW;
            spec.size = size;
        } else if (dbType.equals("varbinary")) {
            spec.type = TypeSpec.VARRAW;
            spec.size = size;
            // TODO: varbinary(max) => LONGVARRAW
        } else if (dbType.equals("image")) {
            spec.type = TypeSpec.LONGVARRAW;
        } else if (dbType.equals("bit")) {
            spec.type = TypeSpec.FIXED;
            spec.size = 1;
            spec.size_in_bits = true;
            spec.scale = 0;
            spec.scale_in_bits = true;
            spec.jdbcJavaClass = Boolean.class;
        } else if (dbType.equals("tinyint")) {
            spec.type = TypeSpec.FIXED;
            spec.size = 8;
            spec.size_in_bits = true;
            spec.scale = 0;
            spec.scale_in_bits = true;
        } else if (dbType.equals("int")) {
            spec.type = TypeSpec.FIXED;
            spec.size = 32;
            spec.size_in_bits = true;
            spec.scale = 0;
            spec.scale_in_bits = true;
        } else if (dbType.equals("bigint")) {
            spec.type = TypeSpec.FIXED;
            spec.size = 64;
            spec.size_in_bits = true;
            spec.scale = 0;
            spec.scale_in_bits = true;
        } else if (dbType.equals("decimal") || dbType.equals("numeric")) {
            spec.type = TypeSpec.FIXED;
            spec.size = size;
            spec.size_in_bits = false;
            spec.scale = scale;
            spec.scale_in_bits = false;
        } else if (dbType.equals("smallmoney")) {
            spec.type = TypeSpec.FIXED;
            spec.size = 32;
            spec.size_in_bits = true;
            spec.scale = 4;
            spec.scale_in_bits = false;
        } else if (dbType.equals("money")) {
            spec.type = TypeSpec.FIXED;
            spec.size = 64;
            spec.size_in_bits = true;
            spec.scale = 4;
            spec.scale_in_bits = false;
        } else if (dbType.equals("float")) {
            spec.type = TypeSpec.FLOAT;
            if (size <= 24) {
                spec.size = 24;
                spec.min_exp = -127;
                spec.max_exp = 127;
            } else {
                spec.size = 53;
                spec.min_exp = -1023;
                spec.max_exp = 1023;
            }
            spec.size_in_bits = true;
            spec.exp_of_2 = true;
        } else if (dbType.equals("real")) {
            spec.type = TypeSpec.FLOAT;
            spec.size = 24;
            spec.size_in_bits = true;
            spec.min_exp = -127;
            spec.max_exp = 127;
            spec.exp_of_2 = true;
        } else if (dbType.equals("datetime")) {
            spec.type = TypeSpec.TIMESTAMP;
            spec.scale = 3;
        } else if (dbType.equals("datetime2")) {
            spec.type = TypeSpec.TIMESTAMP;
            spec.scale = 7;
        } else if (dbType.equals("smalldatetime")) {
            spec.type = TypeSpec.TIMESTAMP;
            spec.scale = 0;
        } else if (dbType.equals("date")) {
            spec.type = TypeSpec.DATE;
        } else if (dbType.equals("time")) {
            spec.type = TypeSpec.TIME;
            spec.scale = 7;
        } else if (dbType.equals("datetimeoffset")) {
            spec.type = TypeSpec.TIMESTAMP_TZ;
            spec.scale = 7;
        } else if (dbType.equals("timestamp")) {
            spec.type = TypeSpec.TIMESTAMP;
            spec.scale = 7;
        } else if (dbType.equals("uniqueidentifier")) {
            spec.type = TypeSpec.VARCHAR;
            spec.size = 36;
        }

        if (dbType.equals("numeric")
                || dbType.equals("decimal")) {
            if (scale == 0)
                scale = null;
        } else if (dbType.equals("datetime")
                || dbType.equals("datetime2")
                || dbType.equals("smalldatetime")
                || dbType.equals("time")
                || dbType.equals("datetimeoffset")
                || dbType.equals("timestamp")) {
            size = scale;
            scale = null;
        } else if (dbType.equals("char")
                || dbType.equals("varchar")
                || dbType.equals("nchar")
                || dbType.equals("nvarchar")
                || dbType.equals("binary")
                || dbType.equals("varbinary")
                || dbType.equals("float")) {
            scale = null;
            // TODO: varchar(max), nvarchar(max), varbinary(max)
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
