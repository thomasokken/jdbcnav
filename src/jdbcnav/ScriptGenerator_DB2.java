///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2008	Thomas Okken
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
import jdbcnav.util.*;


public class ScriptGenerator_DB2 extends ScriptGenerator {
	protected String printType(TypeSpec td) {
		switch (td.type) {
			case TypeSpec.UNKNOWN: {
				return td.native_representation;
			}
			case TypeSpec.FIXED: {
				if (td.size_in_bits && td.scale == 0) {
					if (td.size <= 16)
						return "SMALLINT";
					else if (td.size <= 32)
						return "INTEGER";
					else if (td.size <= 64)
						return "BIGINT";
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
					return "DECIMAL(" + size + ")";
				else
					return "DECIMAL(" + size + ", " + scale + ")";
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
				else
					return "DOUBLE";
			}
			case TypeSpec.CHAR: {
				return "CHAR(" + td.size + ")";
			}
			case TypeSpec.NCHAR: {
				return "GRAPHIC(" + td.size + ")";
			}
			case TypeSpec.VARCHAR: {
				return "VARCHAR(" + td.size + ")";
			}
			case TypeSpec.VARNCHAR: {
				return "VARGRAPHIC(" + td.size + ")";
			}
			case TypeSpec.LONGVARCHAR: {
				return "CLOB(1G)";
			}
			case TypeSpec.LONGVARNCHAR: {
				return "DBCLOB(512M)";
			}
			case TypeSpec.RAW: {
				return "CHAR(" + td.size + ") FOR BIT DATA";
			}
			case TypeSpec.VARRAW: {
				return "VARCHAR(" + td.size + ") FOR BIT DATA";
			}
			case TypeSpec.LONGVARRAW: {
				return "BLOB(1G)";
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
				return "DECIMAL(6)";
			}
			case TypeSpec.INTERVAL_DS: {
				// TODO - Warning
				return "DECIMAL(19)";
			}
			case TypeSpec.INTERVAL_YS: {
				// TODO - Warning
				return "DECIMAL(19)";
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
		if (spec.type == TypeSpec.NCHAR
				|| spec.type == TypeSpec.VARNCHAR
				|| spec.type == TypeSpec.LONGVARNCHAR)
			return "g" + super.toSqlString(spec, obj);
		if (spec.type == TypeSpec.DATE)
			return "'" + spec.objectToString(obj) + "'";
		if (spec.type == TypeSpec.TIME
				|| spec.type == TypeSpec.TIME_TZ)
			return "'" + ((DateTime) obj).toString(TypeSpec.TIME, 0,
												   DateTime.ZONE_NONE) + "'";
		if (spec.type == TypeSpec.TIMESTAMP
				|| spec.type == TypeSpec.TIMESTAMP_TZ)
			return "'" + ((DateTime) obj).toString(TypeSpec.TIMESTAMP, 6,
												   DateTime.ZONE_NONE) + "'";
		if (obj instanceof BlobWrapper || obj instanceof byte[]) {
			byte[] ba;
			if (obj instanceof BlobWrapper)
				ba = ((BlobWrapper) obj).load();
			else
				ba = (byte[]) obj;
			String s = "x'" + FileUtils.byteArrayToHex(ba) + "'";
			if (spec.type == TypeSpec.LONGVARRAW
					&& !spec.native_representation.equals(
											"LONG VARCHAR FOR BIT DATA"))
				return "CAST(" + s + " AS BLOB)";
			else
				return s;
		}
		if (spec.type == TypeSpec.INTERVAL_DS)
			return Long.toString(((Interval) obj).nanos);
		if (spec.type == TypeSpec.INTERVAL_YM)
			return Integer.toString(((Interval) obj).months);
		if (spec.type == TypeSpec.INTERVAL_YS) {
			Interval inter = (Interval) obj;
			return Long.toString(inter.months * 2629746000000000L +inter.nanos);
		}
		return super.toSqlString(spec, obj);
	}
}
