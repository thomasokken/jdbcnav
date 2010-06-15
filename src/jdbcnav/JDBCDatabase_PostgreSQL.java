///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2010	Thomas Okken
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
import java.lang.reflect.*;
import jdbcnav.model.*;
import jdbcnav.util.NavigatorException;


public class JDBCDatabase_PostgreSQL extends JDBCDatabase {
	public JDBCDatabase_PostgreSQL(String name, String driver, Connection con) {
		super(name, driver, con);
	}

	protected String[] getJavaTypes(String qualifiedName)
													throws NavigatorException {
		// This is a bit icky. What I would *like* to do it use
		// PreparedStatement.getMetaData() to find out about a table's Java
		// type mapping without having to execute a statement, but the
		// PostgreSQL 8.0.0beta1 JDBC Driver (pgdev.305.jdbc3.jar) returns
		// 'null' from that method.
		// So, I create a query that is guaranteed to return no rows at all,
		// and run that. Hopefully this'll be reasonably efficient, too!

		// TODO: Recent versions of the PostgreSQL driver
		// (i.e., postgresql-8.2dev-500.jdbc3.jar) do implement
		// PrepraredStatement.getMetaData(), so I could make this method
		// conditional (forward to super.getJavaTypes() if the version is
		// sufficiently recent).

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(
							"select * from " + qualifiedName + " where 1 = 2");
			ResultSetMetaData rsmd = rs.getMetaData();
			int columns = rsmd.getColumnCount();
			String[] javaTypes = new String[columns];
			for (int i = 0; i < columns; i++)
				javaTypes[i] = rsmd.getColumnClassName(i + 1);
			return javaTypes;
		} catch (SQLException e) {
			throw new NavigatorException(e);
		} finally {
			if (rs != null)
				try {
					rs.close();
				} catch (SQLException e) {}
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException e) {}
		}
	}

	/**
	 * For unnamed keys, PostgreSQL returns "<unnamed>" instead of null.
	 */
	protected String unmangleKeyName(String name) {
		return "<unnamed>".equals(name) ? null : name;
	}

	/**
	 * The PostgreSQL JDBC Driver (pgdev.305.jdbc3.jar) does not return
	 * anything in ResultSetMetaData.getCatalogName(), getSchemaName(),
	 * and getTableName(), which makes it impossible (without parsing SQL
	 * ourselves, anyway) to support allowTable=true in Database.runQuery().
	 */
	protected boolean resultSetContainsTableInfo() {
		return false;
	}

	protected String qualifyName(String name) {
		return "public." + name;
	}

	protected TypeSpec makeTypeSpec(String dbType, Integer size, Integer scale,
									int sqlType, String javaType) {
		TypeSpec spec = super.makeTypeSpec(dbType, size, scale, sqlType,
																javaType);
		if (dbType.equals("bigint")) {
			spec.type = TypeSpec.FIXED;
			spec.size = 64;
			spec.size_in_bits = true;
			spec.scale = 0;
			spec.scale_in_bits = true;
		} else if (dbType.equals("integer")
				|| dbType.equals("int")
				|| dbType.equals("int4")) {
			spec.type = TypeSpec.FIXED;
			spec.size = 32;
			spec.size_in_bits = true;
			spec.scale = 0;
			spec.scale_in_bits = true;
		} else if (dbType.equals("smallint")
				|| dbType.equals("int2")) {
			spec.type = TypeSpec.FIXED;
			spec.size = 16;
			spec.size_in_bits = true;
			spec.scale = 0;
			spec.scale_in_bits = true;
		} else if (dbType.equals("real")
				|| dbType.equals("float4")) {
			spec.type = TypeSpec.FLOAT;
			spec.size = 24;
			spec.size_in_bits = true;
			spec.min_exp = -127;
			spec.max_exp = 127;
			spec.exp_of_2 = true;
		} else if (dbType.equals("double precision")
				|| dbType.equals("float8")) {
			spec.type = TypeSpec.FLOAT;
			spec.size = 54;
			spec.size_in_bits = true;
			spec.min_exp = -1023;
			spec.max_exp = 1023;
			spec.exp_of_2 = true;
		} else if (dbType.equals("numeric")
				|| dbType.equals("decimal")) {
			if (size == 65535 && scale == 65531) {
				// TODO: This type does not fit in the current TypeSpec
				// model. It is an arbitrary-precision (up to 1000 digits)
				// number without scale coercion, or, to put it differently,
				// a high-precision floating-point number.
				// I choose double-precision since it's the best match in
				// terms of allowing the original type's dynamic range, if not
				// its precision.
				// TODO - Warning
				spec.type = TypeSpec.FLOAT;
				spec.size = 54;
				spec.size_in_bits = true;
				spec.min_exp = -1023;
				spec.max_exp = 1023;
				spec.exp_of_2 = true;
			} else {
				spec.type = TypeSpec.FIXED;
				spec.size = size;
				spec.size_in_bits = false;
				spec.scale = scale;
				spec.scale_in_bits = false;
			}
		} else if (dbType.equals("money")) {
			// Deprecated type, so although we recognize it, printType() will
			// never generate it.
			spec.type = TypeSpec.FIXED;
			spec.size = 32;
			spec.size_in_bits = true;
			spec.scale = 2;
			spec.scale_in_bits = false;
		} else if (dbType.equals("date")) {
			spec.type = TypeSpec.DATE;
		} else if (dbType.equals("time")) {
			spec.type = TypeSpec.TIME;
			spec.size = scale;
		} else if (dbType.equals("time with time zone")
				|| dbType.equals("timetz")) {
			spec.type = TypeSpec.TIME_TZ;
			spec.size = scale;
		} else if (dbType.equals("timestamp")) {
			spec.type = TypeSpec.TIMESTAMP;
			spec.size = scale;
		} else if (dbType.equals("timestamp with time zone")
				|| dbType.equals("timestamptz")) {
			spec.type = TypeSpec.TIMESTAMP_TZ;
			spec.size = scale;
		} else if (dbType.equals("interval")) {
			// Yuck; PostgreSQL does not distinguish between
			// INTERVAL YEAR TO MONTH and INTERVAL DAY TO SECOND; it has one
			// type that is basically INTERVAL YEAR TO SECOND.
			spec.type = TypeSpec.INTERVAL_YS;
			spec.size = scale;
		} else if (dbType.equals("bytea")) {
			spec.type = TypeSpec.LONGVARRAW;
		} else if (dbType.equals("char")
				|| dbType.equals("bpchar")
				|| dbType.equals("character")) {
			spec.type = TypeSpec.CHAR;
			spec.size = size;
		} else if (dbType.equals("varchar")
				|| dbType.equals("character varying")) {
			if (size == 0)
				spec.type = TypeSpec.LONGVARCHAR;
			else {
				spec.type = TypeSpec.VARCHAR;
				spec.size = size;
			}
		} else if (dbType.equals("text")) {
			spec.type = TypeSpec.LONGVARCHAR;
		} else {
			// Unsupported value, such as one of PostgreSQL's geometric data
			// types or bit strings. Don't know how to handle them so we tag
			// them UNKNOWN, which will cause the script generator to pass them
			// on uninterpreted and unchanged.
			spec.type = TypeSpec.UNKNOWN;
		}

		if (dbType.equals("numeric")
				|| dbType.equals("decimal")) {
			if (size == 65535 && scale == 65531) {
				size = null;
				scale = null;
			} else if (scale == 0)
				scale = null;
		} else if (dbType.equals("interval")
				|| dbType.equals("time")
				|| dbType.equals("timetz")
				|| dbType.equals("timestamp")
				|| dbType.equals("timestamptz")) {
			size = scale;
			scale = null;
		} else if (dbType.equals("bit varying")
				|| dbType.equals("varbit")
				|| dbType.equals("bit")
				|| dbType.equals("character varying")
				|| dbType.equals("varchar")
				|| dbType.equals("character")
				|| dbType.equals("char")
				|| dbType.equals("bpchar")) {
			scale = null;
		} else {
			size = null;
			scale = null;
		}

		if ((dbType.equals("varchar")
					|| dbType.equals("character varying"))
				&& size == 0)
			size = null;

		if (dbType.equals("bpchar"))
			dbType = "char";

		if (size == null)
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
		if (spec.type == TypeSpec.INTERVAL_YS) {
			int years = 0, months = 0;
			long days = 0, hours = 0, minutes = 0;
			double seconds = 0;
			try {
				Class<?> c = o.getClass();
				Method m = c.getMethod("getYears", (Class[]) null);
				years = (Integer) m.invoke(o, (Object[]) null);
				m = c.getMethod("getMonths", (Class[]) null);
				months = (Integer) m.invoke(o, (Object[]) null);
				m = c.getMethod("getDays", (Class[]) null);
				days = (Integer) m.invoke(o, (Object[]) null);
				m = c.getMethod("getHours", (Class[]) null);
				hours = (Integer) m.invoke(o, (Object[]) null);
				m = c.getMethod("getMinutes", (Class[]) null);
				minutes = (Integer) m.invoke(o, (Object[]) null);
				m = c.getMethod("getSeconds", (Class[]) null);
				seconds = (Double) m.invoke(o, (Object[]) null);
			} catch (Exception e) {
				return o;
			}
			long nanos = days * 86400000000000L
					   + hours * 3600000000000L
					   + minutes * 60000000000L
					   + (long) (seconds * 1000000000);
			months += years * 12;
			return new Interval(months, nanos);
		}
		return super.db2nav(spec, o);
	}

	protected Object nav2db(TypeSpec spec, Object o) {
		if (o == null)
			return null;
		if (spec.type == TypeSpec.INTERVAL_YS) {
			Interval inter = (Interval) o;
			int m = inter.months;
			int years = m / 12;
			int months = m - years * 12;
			long n = inter.nanos;
			int days = (int) (n / 86400000000000L);
			n -= days * 86400000000000L;
			int hours = (int) (n / 3600000000000L);
			n -= hours * 3600000000000L;
			int minutes = (int) (n / 60000000000L);
			n -= minutes * 60000000000L;
			double seconds = n / 1000000000.0;
			try {
				Class<?> c = Class.forName("org.postgresql.util.PGInterval");
				Constructor<?> cnstr = c.getConstructor(new Class [] {
									int.class, int.class, int.class,
									int.class, int.class, double.class });
				return cnstr.newInstance(new Object[] {
									years, months, days,
									hours, minutes, seconds });
			} catch (Exception e) {
				e.printStackTrace();
				return o;
			}
		}
		return super.nav2db(spec, o);
	}
}
