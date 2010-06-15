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

import java.text.*;
import java.util.*;
import jdbcnav.model.*;
import jdbcnav.util.FileUtils;

public class ScriptGenerator_MySQL extends ScriptGenerator {
	protected String printType(TypeSpec td) {
		switch (td.type) {
			case TypeSpec.UNKNOWN: {
				return td.native_representation;
			}
			case TypeSpec.FIXED: {
				if (td.size_in_bits && td.scale == 0) {
					if (td.size == 1)
						return "BIT";
					else if (td.size <= 8)
						return "tinyint";
					else if (td.size <= 16)
						return "smallint";
					else if (td.size <= 24)
						return "mediumint";
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

				int scale;
				if (td.scale_in_bits)
					scale = (int) Math.ceil(td.scale * LOG10_2);
				else
					scale = td.scale;

				if (scale == 0)
					return "decimal(" + size + ")";
				else
					return "decimal(" + size + ", " + scale + ")";
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
					return "float";
				else
					return "double";
			}
			case TypeSpec.CHAR: {
				return "char(" + td.size + ")";
			}
			case TypeSpec.NCHAR: {
				return "nchar(" + td.size + ")";
			}
			case TypeSpec.VARCHAR: {
				return "varchar(" + td.size + ")";
			}
			case TypeSpec.VARNCHAR: {
				return "nvarchar(" + td.size + ")";
			}
			case TypeSpec.LONGVARCHAR:
			case TypeSpec.LONGVARNCHAR: {
				return "longtext";
			}
			case TypeSpec.RAW: {
				return "binary";
			}
			case TypeSpec.VARRAW: {
				return "varbinary";
			}
			case TypeSpec.LONGVARRAW: {
				return "longblob";
			}
			case TypeSpec.DATE: {
				return "date";
			}
			case TypeSpec.TIME:
			case TypeSpec.TIME_TZ: {
				return "time";
			}
			case TypeSpec.TIMESTAMP:
			case TypeSpec.TIMESTAMP_TZ: {
				return "timestamp";
			}
			case TypeSpec.INTERVAL_YM: {
				return "decimal(6)";
			}
			case TypeSpec.INTERVAL_DS: {
				return "decimal(19)";
			}
			case TypeSpec.INTERVAL_YS: {
				return "decimal(19)";
			}
			default: {
				// TODO - Warning (internal error); should never get here
				return td.native_representation;
			}
		}
	}

	private static final SimpleDateFormat ts6format =
					new SimpleDateFormat("yyMMdd");
	private static final SimpleDateFormat ts8format =
					new SimpleDateFormat("yyyyMMdd");
	private static final SimpleDateFormat ts12format =
					new SimpleDateFormat("yyMMddHHmmss");
	private static final SimpleDateFormat ts14format =
					new SimpleDateFormat("yyyyMMddHHmmss");

	protected String toSqlString(TypeSpec spec, Object obj) {
		if (obj == null) {
			return super.toSqlString(spec, obj);
		} else if (spec.type == TypeSpec.DATE
				|| spec.type == TypeSpec.TIME) {
			return "'" + spec.objectToString(obj) + "'";
		} else if (spec.type == TypeSpec.TIMESTAMP) {
			if (spec.db.getClass().getName().equals("jdbcnav.JDBCDatabase_MySQL")
					&& spec.jdbcDbType.equalsIgnoreCase("timestamp")) {
				int sz = spec.jdbcSize;
				DateTime dt = (DateTime) obj;
				java.util.Date d = new java.util.Date(dt.time + dt.nanos / 1000000);
				String s;
				if (sz >= 14)
					s = ts14format.format(d);
				else if (sz >= 12)
					s = ts12format.format(d);
				else if (sz >= 8)
					s = ts8format.format(d);
				else
					s = ts6format.format(d);
				return "'" + s + "'";
			} else
				return "'" + spec.objectToString(obj) + "'";
		} else if (spec.type == TypeSpec.TIME_TZ
				|| spec.type == TypeSpec.TIMESTAMP_TZ) {
			// Not using spec.objectToString() here, because it displays the
			// time zone; we want to suppress that here.
			DateTime dt = (DateTime) obj;
			return "'" + dt.toString(spec, DateTime.ZONE_NONE) + "'";
		} else if (obj instanceof java.sql.Time) {
			// TODO: handle fractional seconds
			return "'" + timeFormat.format((java.util.Date) obj) + "'";
		} else if (obj instanceof java.sql.Timestamp) {
			// TODO: handle fractional seconds
			return "'" + dateTimeFormat.format((java.util.Date) obj) + "'";
		} else if (obj instanceof java.sql.Date) {
			return "'" + dateFormat.format((java.util.Date) obj) + "'";
		} else if (obj instanceof java.util.Date) {
			return "'" + dateTimeFormat.format((java.util.Date) obj) + "'";
		} else if (spec.type == TypeSpec.INTERVAL_DS) {
			return Long.toString(((Interval) obj).nanos);
		} else if (spec.type == TypeSpec.INTERVAL_YM) {
			return Integer.toString(((Interval) obj).months);
		} else if (spec.type == TypeSpec.INTERVAL_YS) {
			Interval inter = (Interval) obj;
			return Long.toString(inter.months * 2629746000000000L + inter.nanos);
		} else if (obj instanceof BlobWrapper || obj instanceof byte[]) {
			// MySQL >= 4.0 supports SQL x'DEADBEEF' syntax; for compatibility
			// with older versions, we use ODBC 0xDEADBEEF syntax instead.
			byte[] ba;
			if (obj instanceof BlobWrapper)
				ba = ((BlobWrapper) obj).load();
			else
				ba = (byte[]) obj;
			return "0x" + FileUtils.byteArrayToHex(ba);
		} else
			return super.toSqlString(spec, obj);
	}

	protected String quote(String s) {
		if (s == null)
			return "null";
		StringTokenizer tok = new StringTokenizer(s, "'\"\\\000\010\032\t\n\r", true);
		StringBuffer buf = new StringBuffer();
		buf.append("'");
		while (tok.hasMoreTokens()) {
			String t = tok.nextToken();
			if (t.equals("'"))
				buf.append("\\'");
			else if (t.equals("\""))
				buf.append("\\\"");
			else if (t.equals("\\"))
				buf.append("\\\\");
			else if (t.equals("\000"))
				buf.append("\\0");
			else if (t.equals("\010"))
				buf.append("\\b");
			else if (t.equals("\032"))
				buf.append("\\Z");
			else if (t.equals("\t"))
				buf.append("\\t");
			else if (t.equals("\n"))
				buf.append("\\n");
			else if (t.equals("\r"))
				buf.append("\\r");
			else
				buf.append(t);
		}
		buf.append("'");
		return buf.toString();
	}
}
