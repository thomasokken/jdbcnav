///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2023  Thomas Okken
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

import jdbcnav.model.DateTime;
import jdbcnav.model.Interval;
import jdbcnav.model.TypeSpec;


public class JDBCDatabase_Transbase extends JDBCDatabase {
    public JDBCDatabase_Transbase(String name, String driver, Connection con) {
        super(name, driver, con);
    }

    /**
     * The Transbase JDBC Driver does not return anything in
     * ResultSetMetaData.getCatalogName(), getSchemaName(), and getTableName(),
     * which makes it impossible (without parsing SQL ourselves, anyway) to
     * support allowTable=true in Database.runQuery().
     */
    protected boolean resultSetContainsTableInfo() {
        return false;
    }

    protected TypeSpec makeTypeSpec(String dbType, Integer size, Integer scale,
                                    int sqlType, String javaType) {
        TypeSpec spec = makeDefaultTypeSpec(dbType, size, scale, sqlType,
                                                                javaType);
        if (dbType.equals("TINYINT")) {
            spec.type = TypeSpec.FIXED;
            spec.size = 8;
            spec.size_in_bits = true;
            spec.scale = 0;
            spec.scale_in_bits = true;
            spec.jdbcJavaClass = Integer.class;
        } else if (dbType.equals("SMALLINT")) {
            spec.type = TypeSpec.FIXED;
            spec.size = 16;
            spec.size_in_bits = true;
            spec.scale = 0;
            spec.scale_in_bits = true;
            spec.jdbcJavaClass = Integer.class;
        } else if (dbType.equals("INTEGER")) {
            spec.type = TypeSpec.FIXED;
            spec.size = 32;
            spec.size_in_bits = true;
            spec.scale = 0;
            spec.scale_in_bits = true;
            spec.jdbcJavaClass = Integer.class;
        } else if (dbType.equals("NUMERIC")
                || dbType.equals("DECIMAL")) {
            spec.type = TypeSpec.FIXED;
            spec.size = size;
            spec.size_in_bits = false;
            spec.scale = scale;
            spec.scale_in_bits = false;
            spec.jdbcJavaClass = java.math.BigDecimal.class;
        } else if (dbType.equals("FLOAT")) {
            spec.type = TypeSpec.FLOAT;
            spec.size = 24;
            spec.size_in_bits = true;
            spec.min_exp = -127;
            spec.max_exp = 127;
            spec.exp_of_2 = true;
            spec.jdbcJavaClass = Float.class;
        } else if (dbType.equals("DOUBLE")
                || dbType.equals("REAL")) {
            spec.type = TypeSpec.FLOAT;
            spec.size = 54;
            spec.size_in_bits = true;
            spec.min_exp = -1023;
            spec.max_exp = 1023;
            spec.exp_of_2 = true;
            spec.jdbcJavaClass = Double.class;
        } else if (dbType.equals("CHAR")) {
            if (spec.jdbcSqlType == Types.VARCHAR) {
                spec.type = TypeSpec.LONGVARCHAR;
            } else {
                spec.type = TypeSpec.CHAR;
                spec.size = size;
            }
            spec.jdbcJavaClass = String.class;
        } else if (dbType.equals("VARCHAR")) {
            spec.type = TypeSpec.VARCHAR;
            spec.size = size;
            spec.jdbcJavaClass = String.class;
        } else if (dbType.equals("BINCHAR")) {
            if (spec.jdbcSqlType == Types.VARCHAR) {
                spec.type = TypeSpec.LONGVARRAW;
            } else {
                spec.type = TypeSpec.RAW;
                spec.size = size;
            }
            spec.jdbcJavaClass = String.class;
        } else if (dbType.equals("BITS")
                || dbType.equals("BITS2")) {
            spec.type = TypeSpec.RAW;
            spec.size = (size + 7) / 8;
            try {
                spec.jdbcJavaClass = Class.forName("transbase.tbx.types.TBBits");
            } catch (ClassNotFoundException e) {
                spec.jdbcJavaClass = Object.class;
            }
        } else if (dbType.equals("BOOL")) {
            spec.type = TypeSpec.FIXED;
            spec.size = 1;
            spec.size_in_bits = true;
            spec.scale = 0;
            spec.scale_in_bits = true;
            spec.jdbcJavaClass = Boolean.class;
        } else if (dbType.equals("BLOB")) {
            spec.type = TypeSpec.LONGVARRAW;
            spec.jdbcJavaClass = new byte[1].getClass();
        } else if (dbType.equals("DATETIME")) {
            if (spec.jdbcSqlType == Types.DATE) {
                spec.jdbcDbType = "DATETIME[YY:DD]";
                spec.type = TypeSpec.DATE;
                spec.jdbcJavaClass = java.sql.Date.class;
            } else if (spec.jdbcSqlType == Types.TIME) {
                spec.jdbcDbType = "DATETIME[HH:SS]";
                spec.type = TypeSpec.TIME;
                spec.jdbcJavaClass = java.sql.Time.class;
            } else if (spec.jdbcSqlType == Types.TIMESTAMP) {
                spec.jdbcDbType = "DATETIME[YY:MS]";
                spec.type = TypeSpec.TIMESTAMP;
                spec.jdbcJavaClass = java.sql.Timestamp.class;
            } else { // spec.jdbcSqlType == Types.OTHER
                spec.jdbcDbType = "DATETIME[??:??]";
                spec.type = TypeSpec.TIMESTAMP;
                try {
                    spec.jdbcJavaClass = Class.forName("transbase.tbx.types.TBDatetime");
                } catch (ClassNotFoundException e) {
                    spec.jdbcJavaClass = Object.class;
                }
            }
        } else if (dbType.equals("TIMESPAN")) {
            spec.jdbcDbType = "TIMESPAN[??:??]";
            spec.type = TypeSpec.INTERVAL_YS;
            spec.size = 3;
            try {
                spec.jdbcJavaClass = Class.forName("transbase.tbx.types.TBTimespan");
            } catch (ClassNotFoundException e) {
                spec.jdbcJavaClass = Object.class;
            }
        }

        spec.jdbcJavaType = spec.jdbcJavaClass.getName();

        if (dbType.equals("NUMERIC")
                || dbType.equals("DECIMAL")) {
            // size and scale both relevant
        } else if (dbType.equals("CHAR")
                || dbType.equals("VARCHAR")
                || dbType.equals("BINCHAR")
                || dbType.equals("BITS")
                || dbType.equals("BITS2")) {
            scale = null;
        } else {
            size = null;
            scale = null;
        }

        if (spec.type == TypeSpec.LONGVARCHAR)
            spec.native_representation = "CHAR(*)";
        else if (spec.type == TypeSpec.LONGVARRAW && spec.jdbcSqlType == Types.VARCHAR)
            spec.native_representation = "BINCHAR(*)";
        else if (size == null)
            spec.native_representation = spec.jdbcDbType;
        else if (scale == null)
            spec.native_representation = spec.jdbcDbType + "(" + size + ")";
        else
            spec.native_representation = spec.jdbcDbType + "(" + size + ", " + scale + ")";

        return spec;
    }

    protected void fixTypeSpecs(String qualifiedName, TypeSpec[] specs) {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery("select a.ctype from syscolumn a, systable b where b.tname = '" + qualifiedName + "' and a.tsegno = b.segno order by a.cpos");
            int col = 0;
            while (rs.next()) {
                if (col >= specs.length)
                    break;
                String type = rs.getString(1);
                String tl = type.toLowerCase();
                String tu = type.toUpperCase();
                if (tl.startsWith("datetime")) {
                    specs[col].jdbcDbType = tu;
                    specs[col].native_representation = tu;
                } else if (tl.startsWith("timespan")) {
                    specs[col].jdbcDbType = tu;
                    specs[col].native_representation = tu;
                    int pos1 = tl.indexOf(':');
                    int pos2 = tl.indexOf(']', pos1 + 1);
                    String r = tl.substring(pos1 + 1, pos2);
                    if (r.equals("yy") || r.equals("mo")) {
                        specs[col].type = TypeSpec.INTERVAL_YM;
                        specs[col].size = 4;
                    } else {
                        specs[col].type = TypeSpec.INTERVAL_DS;
                        specs[col].size = 3;
                        specs[col].scale = r.equals("ms") ? 3 : 0;
                    }
                } else if (tl.equals("bits(*)")
                        || tl.equals("bits2(*)")) {
                    specs[col].native_representation = tu;
                    specs[col].type = TypeSpec.LONGVARRAW;
                }
                col++;
            }
        } catch (SQLException e) {}
    }

    protected Object db2nav(TypeSpec spec, Object o) {
        if (o == null)
            return null;
        if (spec.jdbcJavaType.equals("transbase.tbx.types.TBBits")) {
            try {
                Class<?> c = o.getClass();
                Method m = c.getMethod("isNull", (Class[]) null);
                Boolean b = (Boolean) m.invoke(o, (Object[]) null);
                if (b != null && b.booleanValue() == true)
                    return null;
                m = c.getMethod("getBitArray", (Class[]) null);
                return m.invoke(o, (Object[]) null);
            } catch (Exception e) {
                e.printStackTrace();
                return o;
            }
        }
        if (spec.jdbcJavaType.equals("transbase.tbx.types.TBDatetime")) {
            try {
                Method m = spec.jdbcJavaClass.getMethod("getTimestamp", (Class[]) null);
                Timestamp ts = (Timestamp) m.invoke(o, (Object[]) null);
                int nanos = ts.getNanos();
                long time = ts.getTime() - nanos / 1000000;
                return new DateTime(time, nanos, null);
            } catch (Exception e) {
                e.printStackTrace();
                return o;
            }
        }
        if (spec.jdbcJavaType.equals("transbase.tbx.types.TBTimespan")) {
            try {
                Method m = spec.jdbcJavaClass.getMethod("getHighField", (Class[]) null);
                int high = (Integer) m.invoke(o, (Object[]) null);
                m = spec.jdbcJavaClass.getMethod("getLowField", (Class[]) null);
                int low = (Integer) m.invoke(o, (Object[]) null);
                m = spec.jdbcJavaClass.getMethod("getField", new Class[] { int.class });
                long[] fields = new long[7];
                Object[] args = new Object[1];
                for (int i = low; i <= high; i++) {
                    args[0] = i;
                    fields[i] = (Long) m.invoke(o, args);
                }
                int months = 0;
                long nanos = 0;
                if (high > 4)
                    // INTERVAL_YM
                    months = (int) (fields[6] * 12 + fields[5]);
                else
                    // INTERVAL_DS
                    nanos = fields[4] * 86400000000000L
                          + fields[3] * 3600000000000L
                          + fields[2] * 60000000000L
                          + fields[1] * 1000000000L
                          + fields[0] * 1000000L;
                return new Interval(months, nanos);
            } catch (Exception e) {
                e.printStackTrace();
                return o;
            }
        }
        return super.db2nav(spec, o);
    }

    protected Object nav2db(TypeSpec spec, Object o) {
        if (spec.jdbcJavaType.equals("transbase.tbx.types.TBBits")) {
            // TODO: This doesn't work. Contact the good folks at
            // Transbase Inc. and ask them how to do this.
            Class<?> c = spec.jdbcJavaClass;
            try {
                Constructor<?> cnstr = c.getConstructor((Class[]) null);
                Object tbbits = cnstr.newInstance((Object[]) null);
                if (o == null) {
                    Method m = c.getMethod("setNull", (Class[]) null);
                    m.invoke(tbbits, (Object[]) null);
                    return tbbits;
                } else {
                    byte[] ba = (byte[]) o;
                    Object[] args = new Object[1];
                    Method m = c.getMethod("addSingleBit", new Class[] { int.class });
                    int pos = 0;
                    for (int i = 0; i < ba.length; i++) {
                        for (int j = 0; j < 8; j++) {
                            if ((j & 128) != 0) {
                                args[0] = pos;
                                m.invoke(tbbits, args);
                            }
                            pos++;
                        }
                    }
                    return tbbits;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return o;
            }
        }
        if (o == null)
            return null;
        if (spec.jdbcJavaType.equals("transbase.tbx.types.TBDatetime")) {
            DateTime dt = (DateTime) o;
            Timestamp ts = new Timestamp(dt.time);
            ts.setNanos(dt.nanos);
            try {
                Constructor<?> c = spec.jdbcJavaClass.getConstructor((Class[]) null);
                Object tbdt = c.newInstance((Object[]) null);
                Method m = spec.jdbcJavaClass.getMethod("setTimestamp", new Class[] { Timestamp.class });
                m.invoke(tbdt, new Object[] { ts });
                return tbdt;
            } catch (Exception e) {
                e.printStackTrace();
                return o;
            }
        }
        if (spec.jdbcJavaType.equals("transbase.tbx.types.TBTimespan")) {
            // TODO: This doesn't work. Contact the good folks at
            // Transbase Inc. and ask them how to do this.
            Interval inter = (Interval) o;
            try {
                Constructor<?> c = spec.jdbcJavaClass.getConstructor((Class[]) null);
                Object tbint = c.newInstance((Object[]) null);
                Method m = spec.jdbcJavaClass.getMethod("setField", new Class[] { int.class, long.class });
                if (spec.type == TypeSpec.INTERVAL_YM) {
                    long years = inter.months / 12;
                    long months = inter.months - years * 12;
                    m.invoke(tbint, new Object[] { 6, years });
                    m.invoke(tbint, new Object[] { 5, months });
                } else {
                    long nanos = inter.nanos;
                    if (spec.type == TypeSpec.INTERVAL_YS)
                        // Should never happen, but what the heck...
                        nanos += inter.months * 2629746000000000L;
                    long days = nanos / 86400000000000L;
                    nanos -= days * 86400000000000L;
                    long hours = nanos / 3600000000000L;
                    nanos -= hours * 3600000000000L;
                    long minutes = nanos / 60000000000L;
                    nanos -= minutes * 60000000000L;
                    long seconds = nanos / 1000000000L;
                    nanos -= seconds * 1000000000L;
                    long milliseconds = nanos / 1000000L;
                    m.invoke(tbint, new Object[] { 4, days });
                    m.invoke(tbint, new Object[] { 3, hours });
                    m.invoke(tbint, new Object[] { 2, minutes });
                    m.invoke(tbint, new Object[] { 1, seconds });
                    m.invoke(tbint, new Object[] { 0, milliseconds });
                }
                return tbint;
            } catch (Exception e) {
                e.printStackTrace();
                return o;
            }
        }
        return super.nav2db(spec, o);
    }
}
