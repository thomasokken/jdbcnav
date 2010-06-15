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

import jdbcnav.model.*;
import jdbcnav.util.FileUtils;

public class ScriptGenerator_SmallSQL extends ScriptGenerator {
	protected String printType(TypeSpec td) {
		switch (td.type) {
			case TypeSpec.UNKNOWN: {
				return td.native_representation;
			}
			case TypeSpec.FIXED: {
				if (td.size_in_bits) {
					// Look for best match within SmallSQL's binary types.
					if (td.scale == 0) {
						if (td.size == 1)
							return "BIT";
						else if (td.size <= 8)
							return "TINYINT";
						else if (td.size <= 16)
							return "SMALLINT";
						else if (td.size <= 32)
							return "INT";
						else if (td.size <= 64)
							return "BIGINT";
					} else if (!td.scale_in_bits && td.scale == 4) {
						if (td.size <= 32)
							return "SMALLMONEY";
						else if (td.size <= 64)
							return "MONEY";
					}
				}

				int size;
				if (td.size_in_bits)
					size = (int) Math.ceil(td.size * LOG10_2);
				else
					size = td.size;
				// TODO - check limits on size

				int scale;
				if (td.scale_in_bits)
					scale = (int) Math.ceil(td.scale * LOG10_2);
				else
					scale = td.scale;
				// TODO - check limits on scale

				if (scale == 0)
					return "NUMERIC(" + size + ")";
				else
					return "NUMERIC(" + size + ", " + scale + ")";
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
					return "REAL";
				if (size > 54 || min_exp < -1023 || max_exp > 1023)
					/* TODO - Warning */;
				return "DOUBLE";
			}
			case TypeSpec.CHAR: {
				return "CHAR(" + td.size + ")";
			}
			case TypeSpec.NCHAR: {
				return "NCHAR(" + td.size + ")";
			}
			case TypeSpec.VARCHAR: {
				return "VARCHAR(" + td.size + ")";
			}
			case TypeSpec.VARNCHAR: {
				return "NVARCHAR(" + td.size + ")";
			}
			case TypeSpec.LONGVARCHAR: {
				return "LONGVARCHAR";
			}
			case TypeSpec.LONGVARNCHAR: {
				return "LONGNVARCHAR";
			}
			case TypeSpec.RAW: {
				return "BINARY(" + td.size + ")";
			}
			case TypeSpec.VARRAW: {
				return "VARBINARY(" + td.size + ")";
			}
			case TypeSpec.LONGVARRAW: {
				return "LONGVARBINARY";
			}
			case TypeSpec.DATE: {
				return "DATE";
			}
			case TypeSpec.TIME:
			case TypeSpec.TIME_TZ: {
				return "TIME";
			}
			case TypeSpec.TIMESTAMP:
			case TypeSpec.TIMESTAMP_TZ: {
				return "TIMESTAMP";
			}
			case TypeSpec.INTERVAL_YM: {
				// TODO - Warning
				return "NUMERIC(6)";
			}
			case TypeSpec.INTERVAL_DS: {
				// TODO - Warning
				return "NUMERIC(19)";
			}
			case TypeSpec.INTERVAL_YS: {
				// TODO - Warning
				return "NUMERIC(19)";
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
		if (spec.type == TypeSpec.DATE) {
			return "{ d '" + spec.objectToString(obj) + "' }";
		} else if (spec.type == TypeSpec.TIME) {
			return "{ t '" + spec.objectToString(obj) + "' }";
		} else if (spec.type == TypeSpec.TIMESTAMP) {
			return "{ ts '" + spec.objectToString(obj) + "' }";
		} else if (spec.type == TypeSpec.TIME_TZ) {
			// Not using spec.objectToString() here, because it displays the
			// time zone name in a human-readable format; for SQL code, we want
			// to print the zone offset instead.
			// TODO: What kind of time zone specifiers does SQL allow? It would
			// be nice to use an ID or name, rather than an offset.
			DateTime dt = (DateTime) obj;
			return "{ t '" + dt.toString(spec, DateTime.ZONE_NONE) + "' }";
		} else if (spec.type == TypeSpec.TIMESTAMP_TZ) {
			// Not using spec.objectToString() here, because it displays the
			// time zone name in a human-readable format; for SQL code, we want
			// to print the zone offset instead.
			// TODO: What kind of time zone specifiers does SQL allow? It would
			// be nice to use an ID or name, rather than an offset.
			DateTime dt = (DateTime) obj;
			return "{ ts '" + dt.toString(spec, DateTime.ZONE_NONE) + "' }";
		} else if (obj instanceof java.sql.Time) {
			return "{ t '" + timeFormat.format((java.util.Date) obj) + "' }";
		} else if (obj instanceof java.sql.Timestamp) {
			return "{ ts '" + dateTimeFormat.format((java.util.Date) obj) + "' }";
		} else if (obj instanceof java.sql.Date) {
			return "{ d '" + dateFormat.format((java.util.Date) obj) + "' }";
		} else if (obj instanceof java.util.Date) {
			return "{ ts '" + dateTimeFormat.format((java.util.Date) obj) + "' }";
		} else if (spec.type == TypeSpec.INTERVAL_DS) {
			return Long.toString(((Interval) obj).nanos);
		} else if (spec.type == TypeSpec.INTERVAL_YM) {
			return Integer.toString(((Interval) obj).months);
		} else if (spec.type == TypeSpec.INTERVAL_YS) {
			Interval inter = (Interval) obj;
			return Long.toString(inter.months * 2629746000000000L + inter.nanos);
		} else if (obj instanceof BlobWrapper || obj instanceof byte[]) {
			byte[] ba;
			if (obj instanceof BlobWrapper)
				ba = ((BlobWrapper) obj).load();
			else
				ba = (byte[]) obj;
			return "0x" + FileUtils.byteArrayToHex(ba);
		} else {
			return super.toSqlString(spec, obj);
		}
	}
}
