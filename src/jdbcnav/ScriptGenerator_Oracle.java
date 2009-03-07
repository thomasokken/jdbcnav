///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2009	Thomas Okken
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

import jdbcnav.model.*;
import jdbcnav.util.FileUtils;

public class ScriptGenerator_Oracle extends ScriptGenerator {
	protected boolean oracle9types = true;
	protected boolean oracle10types = true;

	protected String getSQLPreamble() {
		return "set scan off;\n";
	}
	protected String onUpdateString(String upd) {
		return null;
	}
	protected String onDeleteString(String del) {
		return del.equals("cascade") ? del : null;
	}
	protected String printType(TypeSpec td) {
		switch (td.type) {
			case TypeSpec.UNKNOWN: {
				return td.native_representation;
			}
			case TypeSpec.FIXED: {
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
			case TypeSpec.FLOAT: {
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
			case TypeSpec.CHAR: {
				if (td.size > 2000) {
					// TODO - Warning
					td.size = 2000;
				}
				return "CHAR(" + td.size + ")";
			}
			case TypeSpec.VARCHAR: {
				if (td.size > 4000) {
					// TODO - Warning
					td.size = 4000;
				}
				return "VARCHAR2(" + td.size + ")";
			}
			case TypeSpec.LONGVARCHAR: {
				if (td.part_of_key || td.part_of_index) {
					// TODO - Warning
					return "VARCHAR2(4000)";
				} else
					return "CLOB";
			}
			case TypeSpec.NCHAR: {
				if (td.size > 2000) {
					// TODO - Warning
					td.size = 2000;
				}
				return "NCHAR(" + td.size + ")";
			}
			case TypeSpec.VARNCHAR: {
				if (td.size > 4000) {
					// TODO - Warning
					td.size = 4000;
				}
				return "NVARCHAR2(" + td.size + ")";
			}
			case TypeSpec.LONGVARNCHAR: {
				if (td.part_of_key || td.part_of_index) {
					// TODO - Warning
					return "NVARCHAR2(4000)";
				} else
					return "NCLOB";
			}
			case TypeSpec.RAW:
			case TypeSpec.VARRAW: {
				if (td.size > 4000) {
					// TODO - Warning
					td.size = 4000;
				}
				return "RAW(" + td.size + ")";
			}
			case TypeSpec.LONGVARRAW: {
				if (td.part_of_key || td.part_of_index) {
					// TODO - Warning
					return "RAW(4000)";
				} else
					return "BLOB";
			}
			case TypeSpec.DATE: {
				return "DATE";
			}
			case TypeSpec.TIME: {
				// TODO - Warning
				if (oracle9types)
					return "TIMESTAMP(" + td.size + ")";
				else
					return "DATE";
			}
			case TypeSpec.TIME_TZ: {
				// TODO - Warning
				if (oracle9types)
					return "TIMESTAMP(" + td.size + ") WITH TIME ZONE";
				else
					return "DATE";
			}
			case TypeSpec.TIMESTAMP: {
				if (oracle9types)
					return "TIMESTAMP(" + td.size + ")";
				else
					return "DATE";
			}
			case TypeSpec.TIMESTAMP_TZ: {
				if (oracle9types)
					return "TIMESTAMP(" + td.size + ") WITH TIME ZONE";
				else
					return "DATE";
			}
			case TypeSpec.INTERVAL_YM: {
				if (oracle10types)
					return "INTERVAL YEAR(" + td.size + ") TO MONTH";
				else
					return "NUMBER(6)";
			}
			case TypeSpec.INTERVAL_DS: {
				if (oracle10types)
					return "INTERVAL DAY(" + td.size
								+ ") TO SECOND(" + td.scale + ")";
				else
					return "NUMBER(19)";
			}
			case TypeSpec.INTERVAL_YS: {
				if (oracle10types)
					return "INTERVAL DAY(9) TO SECOND(" + td.size + ")";
				else
					return "NUMBER(19)";
			}
			default: {
				// TODO - Warning (internal error); should never get here
				return td.native_representation;
			}
		}
	}
	protected String toSqlString(TypeSpec spec, Object obj) {
		if (obj == null)
			return super.toSqlString(spec, obj);
		if (spec.jdbcJavaType.equals("oracle.sql.TIMESTAMPLTZ")) {
			return "cast(to_timestamp('" + spec.objectToString(obj)
									+ "', 'YYYY-MM-DD HH24:MI:SS.FF')"
									+ " as timestamp with local time zone)";
		} else if (spec.type == TypeSpec.DATE) {
			return "to_date('" + spec.objectToString(obj)
								+ "', 'YYYY-MM-DD')";
		} else if (spec.type == TypeSpec.TIME) {
			return "to_timestamp('" + spec.objectToString(obj)
								+ "', 'HH24:MI:SS.FF')";
		} else if (spec.type == TypeSpec.TIMESTAMP) {
			return "to_timestamp('" + spec.objectToString(obj)
								+ "', 'YYYY-MM-DD HH24:MI:SS.FF')";
		} else if (spec.type == TypeSpec.TIME_TZ) {
			// Not using spec.objectToString() here, because it displays the
			// time zone name in a human-readable format; for Oracle, we want
			// to print the zone ID instead.
			DateTime dt = (DateTime) obj;
			return "to_timestamp_tz('" + dt.toString(spec, DateTime.ZONE_ID)
								+ "', 'HH24:MI:SS.FF TZR')";
		} else if (spec.type == TypeSpec.TIMESTAMP_TZ) {
			// Not using spec.objectToString() here, because it displays the
			// time zone name in a human-readable format; for Oracle, we want
			// to print the zone ID instead.
			DateTime dt = (DateTime) obj;
			return "to_timestamp_tz('" + dt.toString(spec, DateTime.ZONE_ID)
									+ "', 'YYYY-MM-DD HH24:MI:SS.FF TZR')";
		} else if (obj instanceof java.sql.Time) {
			// TODO: handle fractional seconds
			return "to_date('" + timeFormat.format((java.util.Date) obj)
							   + "', 'HH24:MI:SS')";
		} else if (obj instanceof java.sql.Timestamp) {
			// TODO: handle fractional seconds
			return "to_date('" + dateTimeFormat.format((java.util.Date) obj)
							   + "', 'YYYY-MM-DD HH24:MI:SS')";
		} else if (obj instanceof java.sql.Date) {
			return "to_date('" + dateFormat.format((java.util.Date) obj)
							   + "', 'YYYY-MM-DD')";
		} else if (obj instanceof java.util.Date) {
			// TODO: handle fractional seconds
			return "to_date('" + dateTimeFormat.format((java.util.Date) obj)
							   + "', 'YYYY-MM-DD HH24:MI:SS')";
		} else if (obj instanceof BlobWrapper || obj instanceof byte[]) {
			byte[] ba;
			if (obj instanceof BlobWrapper)
				ba = ((BlobWrapper) obj).load();
			else
				ba = (byte[]) obj;
			return "hextoraw('" + FileUtils.byteArrayToHex(ba) + "')";
		} else if (spec.type == TypeSpec.INTERVAL_DS
				|| spec.type == TypeSpec.INTERVAL_YM
				|| spec.type == TypeSpec.INTERVAL_YS) {
			if (oracle10types)
				return super.toSqlString(spec, obj);
			else {
				Interval inter = (Interval) obj;
				if (spec.type == TypeSpec.INTERVAL_DS)
					return Long.toString(inter.nanos);
				else if (spec.type == TypeSpec.INTERVAL_YM)
					return Integer.toString(inter.months);
				else // INTERVAL_YS
					return Long.toString(inter.months * 2629746000000000L + inter.nanos);
			}
		} else if (obj instanceof BfileWrapper) {
			return ((BfileWrapper) obj).sqlString();
		} else
			return super.toSqlString(spec, obj);
	}
	protected int maxLineLength() {
		return 1000;
	}
}
