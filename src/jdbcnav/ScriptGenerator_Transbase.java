///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2008  Thomas Okken
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

import java.util.StringTokenizer;
import jdbcnav.model.*;
import jdbcnav.util.*;

public class ScriptGenerator_Transbase extends ScriptGenerator {
    protected String printType(TypeSpec td) {
	switch (td.type) {
	    case TypeSpec.UNKNOWN: {
		return td.native_representation;
	    }
	    case TypeSpec.FIXED: {
		if (td.size_in_bits && td.scale == 0) {
		    if (td.size == 1)
			return "BOOL";
		    else if (td.size <= 8)
			return "TINYINT";
		    else if (td.size <= 16)
			return "SMALLINT";
		    else if (td.size <= 32)
			return "INTEGER";
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
		    return "FLOAT";
		else
		    return "DOUBLE";
	    }
	    case TypeSpec.CHAR:
	    case TypeSpec.NCHAR: {
		return "CHAR(" + td.size + ")";
	    }
	    case TypeSpec.VARCHAR:
	    case TypeSpec.VARNCHAR: {
		return "VARCHAR(" + td.size + ")";
	    }
	    case TypeSpec.LONGVARCHAR:
	    case TypeSpec.LONGVARNCHAR: {
		return "CHAR(*)";
	    }
	    case TypeSpec.RAW:
	    case TypeSpec.VARRAW: {
		return "BINCHAR(" + td.size + ")";
	    }
	    case TypeSpec.LONGVARRAW: {
		return "BINCHAR(*)";
	    }
	    case TypeSpec.DATE: {
		return "DATETIME[YY:DD]";
	    }
	    case TypeSpec.TIME:
	    case TypeSpec.TIME_TZ: {
		if (td.size == 0)
		    return "DATETIME[HH:SS]";
		else
		    return "DATETIME[HH:MS]";
	    }
	    case TypeSpec.TIMESTAMP:
	    case TypeSpec.TIMESTAMP_TZ: {
		if (td.size == 0)
		    return "DATETIME[YY:SS]";
		else
		    return "DATETIME[YY:MS]";
	    }
	    case TypeSpec.INTERVAL_YM: {
		return "TIMESPAN[YY:MM]";
	    }
	    case TypeSpec.INTERVAL_DS:
	    case TypeSpec.INTERVAL_YS: {
		if (td.size == 0)
		    return "TIMESPAN[DD:SS]";
		else
		    return "TIMESPAN[DD:MS]";
	    }
	    default: {
		// TODO - Warning (internal error); should never get here
		return td.native_representation;
	    }
	}
    }

    private static class TimeRange {
	public int high, low;
	public String r1, r2;
	public TimeRange(String type) {
	    type = type.toUpperCase();
	    int pos1 = type.indexOf('[');
	    int pos2 = type.indexOf(':', pos1 + 1);
	    int pos3 = type.indexOf(']', pos2 + 1);
	    r1 = type.substring(pos1 + 1, pos2);
	    r2 = type.substring(pos2 + 1, pos3);
	    high = "MS SS MI HH DD MO YY".indexOf(r1) / 3;
	    low = "MS SS MI HH DD MO YY".indexOf(r2) / 3;
	}
	public String format(String time) {
	    // 'time' should be formatted as "YYYY-MM-DD hh:mm:ss.fff"
	    StringTokenizer tok = new StringTokenizer(time, "-:. ", true);
	    StringBuffer buf = new StringBuffer();
	    buf.append("[");
	    buf.append(r1);
	    buf.append(':');
	    buf.append(r2);
	    buf.append("](");
	    int f = 6;
	    while (tok.hasMoreTokens()) {
		String t = tok.nextToken();
		if (t.length() == 1 && "-:. ".indexOf(t) != -1) {
		    if (f < low)
			break;
		    if (f <= high) {
			if (f < high)
			    buf.append(t);
			buf.append(tok.nextToken());
		    } else
			tok.nextToken();
		    f--;
		} else {
		    // Can only happen first time through the loop, since we
		    // know that the result of the DateTime.toString() call
		    // looks like "YYYY-MM-DD hh:mm:ss.fff" -- in other words,
		    // every second token is a separator.
		    if (f <= high)
			buf.append(t);
		    f--;
		}
	    }
	    buf.append(")");
	    return buf.toString();
	}
    }

    private static final TimeRange dateRange = new TimeRange("[YY:DD]");
    private static final TimeRange timeRange = new TimeRange("[HH:SS]");
    private static final TimeRange timeRangeMS = new TimeRange("[HH:MS]");
    private static final TimeRange timestampRange = new TimeRange("[YY:SS]");
    private static final TimeRange timestampRangeMS = new TimeRange("[YY:MS]");
    private static final TimeRange intervalYmRange = new TimeRange("[YY:MO]");
    private static final TimeRange intervalDsRange = new TimeRange("[DD:SS]");
    private static final TimeRange intervalDsRangeMS = new TimeRange("[DD:MS]");

    protected String toSqlString(TypeSpec spec, Object obj) {
	if (obj == null)
	    return super.toSqlString(spec, obj);
	if (spec.type == TypeSpec.TIME
		|| spec.type == TypeSpec.TIME_TZ
		|| spec.type == TypeSpec.TIMESTAMP
		|| spec.type == TypeSpec.TIMESTAMP_TZ
		|| spec.type == TypeSpec.DATE) {
	    TimeRange range = timestampRange;
	    if (spec.jdbcDbType.startsWith("DATETIME[")) {
		// Looks like Transbase; generate only the appropriate fields
		range = new TimeRange(spec.jdbcDbType);
	    } else {
		switch (spec.type) {
		    case TypeSpec.DATE:
			range = dateRange;
			break;
		    case TypeSpec.TIME:
		    case TypeSpec.TIME_TZ:
			if (spec.size == 0)
			    range = timeRange;
			else
			    range = timeRangeMS;
			break;
		    case TypeSpec.TIMESTAMP:
		    case TypeSpec.TIMESTAMP_TZ:
			if (spec.size == 0)
			    range = timestampRange;
			else
			    range = timestampRangeMS;
			break;
		}
	    }
	    DateTime dt = (DateTime) obj;
	    String s = dt.toString(TypeSpec.TIMESTAMP, 3, DateTime.ZONE_NONE);
	    return "DATETIME" + range.format(s);
	}
	if (spec.type == TypeSpec.INTERVAL_YS
		|| spec.type == TypeSpec.INTERVAL_YM
		|| spec.type == TypeSpec.INTERVAL_DS) {
	    TimeRange range = timestampRange;
	    Interval inter = (Interval) obj;
	    if (spec.jdbcDbType.startsWith("TIMESPAN[")) {
		// Looks like Transbase; generate only the appropriate fields
		range = new TimeRange(spec.jdbcDbType);
	    } else {
		switch (spec.type) {
		    case TypeSpec.INTERVAL_YS:
			inter = new Interval(0, inter.months * 2629746000000000L + inter.nanos);
			if (spec.size == 0)
			    range = intervalDsRange;
			else
			    range = intervalDsRangeMS;
			break;
		    case TypeSpec.INTERVAL_DS:
			if (spec.scale == 0)
			    range = intervalDsRange;
			else
			    range = intervalDsRangeMS;
			break;
		    case TypeSpec.INTERVAL_YM:
			range = intervalYmRange;
			break;
		}
	    }
	    String s = inter.toString(TypeSpec.INTERVAL_YS, 3);
	    String sign = s.substring(0, 1);
	    if (sign.equals("+"))
		sign = "";
	    else
		sign = "- ";
	    s = s.substring(1);
	    return sign + "TIMESPAN" + range.format(s);
	}
	if (obj instanceof BlobWrapper || obj instanceof byte[]) {
	    byte[] ba;
	    if (obj instanceof BlobWrapper)
		ba = ((BlobWrapper) obj).load();
	    else
		ba = (byte[]) obj;
	    return "0x" + FileUtils.byteArrayToHex(ba);
	}
	return super.toSqlString(spec, obj);
    }
}
