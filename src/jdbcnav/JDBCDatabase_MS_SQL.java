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

import java.lang.reflect.Method;
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
            if (size == Integer.MAX_VALUE) {
                spec.type = TypeSpec.LONGVARCHAR;
            } else {
                spec.type = TypeSpec.VARCHAR;
                spec.size = size;
            }
        } else if (dbType.equals("text")) {
            spec.type = TypeSpec.LONGVARCHAR;
        } else if (dbType.equals("nchar")) {
            spec.type = TypeSpec.NCHAR;
            spec.size = size;
        } else if (dbType.equals("nvarchar")) {
            if (size == Integer.MAX_VALUE) {
                spec.type = TypeSpec.LONGVARNCHAR;
            } else {
                spec.type = TypeSpec.VARNCHAR;
                spec.size = size;
            }
        } else if (dbType.equals("ntext")) {
            spec.type = TypeSpec.LONGVARCHAR;
        } else if (dbType.equals("binary")) {
            spec.type = TypeSpec.RAW;
            spec.size = size;
        } else if (dbType.equals("varbinary")) {
            if (size == Integer.MAX_VALUE) {
                spec.type = TypeSpec.LONGVARRAW;
            } else {
                spec.type = TypeSpec.VARRAW;
                spec.size = size;
            }
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
        } else if (dbType.equals("smallint")) {
            spec.type = TypeSpec.FIXED;
            spec.size = 16;
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
            spec.size = 3;
        } else if (dbType.equals("datetime2")) {
            spec.type = TypeSpec.TIMESTAMP;
            spec.size = scale;
        } else if (dbType.equals("smalldatetime")) {
            spec.type = TypeSpec.TIMESTAMP;
            spec.size = 0;
        } else if (dbType.equals("date")) {
            spec.type = TypeSpec.DATE;
        } else if (dbType.equals("time")) {
            spec.type = TypeSpec.TIME;
            spec.size = scale;
        } else if (dbType.equals("datetimeoffset")) {
            spec.type = TypeSpec.TIMESTAMP_TZ;
            spec.size = scale;
        } else if (dbType.equals("timestamp")) {
            spec.type = TypeSpec.TIMESTAMP;
            spec.size = 7;
        } else if (dbType.equals("uniqueidentifier")) {
            spec.type = TypeSpec.VARCHAR;
            spec.size = 36;
        }

        if (dbType.equals("numeric")
                || dbType.equals("decimal")) {
            if (scale == 0)
                scale = null;
        } else if (dbType.equals("datetime2")
                || dbType.equals("datetimeoffset")
                || dbType.equals("time")) {
            size = spec.size;
            scale = null;
        } else if (dbType.equals("datetime")
                || dbType.equals("smalldatetime")
                || dbType.equals("timestamp")) {
            scale = null;
        } else if (dbType.equals("char")
                || dbType.equals("varchar")
                || dbType.equals("nchar")
                || dbType.equals("nvarchar")
                || dbType.equals("binary")
                || dbType.equals("varbinary")
                || dbType.equals("float")) {
            scale = null;
        } else {
            size = null;
            scale = null;
        }

        if ((dbType.equals("varchar")
                || dbType.equals("nvarchar")
                || dbType.equals("varbinary"))
                && size == Integer.MAX_VALUE)
            spec.native_representation = dbType + "(max)";
        else if (size == null
                || dbType.equals("datetime")
                || dbType.equals("smalldatetime")
                || dbType.equals("timestamp"))
            spec.native_representation = dbType;
        else if (scale == null)
            spec.native_representation = dbType + "(" + size + ")";
        else
            spec.native_representation = dbType + "(" + size + ", " + scale + ")";

        return spec;
    }

    protected Object db2nav(TypeSpec spec, Object o) {
        if (o == null)
            return null;
        if (spec.jdbcJavaType.equals("microsoft.sql.DateTimeOffset")) {
            String s = o.toString(); // YYYY-MM-DD HH:mm:ss[.fffffff] [+|-]HH:mm
            int sp = s.lastIndexOf(' ');
            s = s.substring(0, sp + 1) + "GMT" + s.substring(sp + 1);
            return new DateTime(s);
        }
        return super.db2nav(spec, o);
    }

    protected Object nav2db(TypeSpec spec, Object o) {
        if (o == null)
            return null;
        if (spec.jdbcJavaType.equals("microsoft.sql.DateTimeOffset")) {
            try {
                Class<?> c = Class.forName("microsoft.sql.DateTimeOffset");
                Method m = c.getMethod("valueOf", new Class[] { java.sql.Timestamp.class, int.class });
                DateTime dt = (DateTime) o;
                java.sql.Timestamp ts = new java.sql.Timestamp(dt.time);
                ts.setNanos(dt.nanos);
                int off = dt.tz.getRawOffset() / 60000;
                return m.invoke(null, new Object[] { ts, off });
            } catch (Exception e) {
                return o;
            }
        }
        return super.nav2db(spec, o);
    }

    public String objectToString(TypeSpec spec, Object o) {
        if (o == null)
            return null;

        if (spec.type == TypeSpec.TIMESTAMP_TZ) {
            String s = super.objectToString(spec, o);
            int g = s.lastIndexOf("GMT");
            if (g != -1)
                s = s.substring(0, g) + s.substring(g + 3);
            return s;
        }

        return super.objectToString(spec, o);
    }

    public Object stringToObject(TypeSpec spec, String s) {
        if (s == null)
            return null;

        if (spec.type == TypeSpec.TIMESTAMP_TZ) {
            // If it appears to end with a time zone offset, insert "GMT"
            int sp = s.lastIndexOf(' ');
            if (sp != -1) {
                String tz = s.substring(sp + 1);
                if (tz.startsWith("+") || tz.startsWith("-")) {
                    tz = tz.substring(1);
                    int c = tz.indexOf(':');
                    if (c != -1) {
                        String h = tz.substring(0, c);
                        String m = tz.substring(c + 1);
                        if (h.length() >= 1 && h.length() <= 2 && m.length() >= 1 && m.length() <= 2) {
                            try {
                                int hh = Integer.parseInt(h);
                                int mm = Integer.parseInt(m);
                                if (hh >= 0 && hh <= 23 && mm >= 0 && mm <= 59)
                                    s = s.substring(0, sp + 1) + "GMT" + s.substring(sp + 1);
                            } catch (Exception e) {}
                        }
                    }
                }
            }
        }

        return super.stringToObject(spec, s);
    }
}
