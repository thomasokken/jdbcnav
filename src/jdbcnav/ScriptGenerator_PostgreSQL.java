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

import java.text.DecimalFormat;

import jdbcnav.model.BlobWrapper;
import jdbcnav.model.Interval;
import jdbcnav.model.TypeSpec;


public class ScriptGenerator_PostgreSQL extends ScriptGenerator {
    protected String printType(TypeSpec td) {
        switch (td.type) {
            case TypeSpec.UNKNOWN: {
                return td.native_representation;
            }
            case TypeSpec.FIXED: {
                if (td.size_in_bits && td.scale == 0) {
                    if (td.size <= 16)
                        return "smallint";
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
                if (size > 1000) {
                    // TODO - Warning
                    size = 1000;
                }

                int scale;
                if (td.scale_in_bits)
                    scale = (int) Math.ceil(td.scale * LOG10_2);
                else
                    scale = td.scale;
                if (scale > size) {
                    // TODO - Warning
                    scale = size;
                }

                if (scale == 0)
                    return "numeric(" + size + ")";
                else
                    return "numeric(" + size + ", " + scale + ")";
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
                    return "real";
                else if (size <= 54 && min_exp >= 1023 && max_exp <= 1023)
                    return "double precision";
                else
                    return "numeric";
            }
            case TypeSpec.CHAR:
            case TypeSpec.NCHAR: {
                return "char(" + td.size + ")";
            }
            case TypeSpec.VARCHAR:
            case TypeSpec.VARNCHAR: {
                return "varchar(" + td.size + ")";
            }
            case TypeSpec.LONGVARCHAR:
            case TypeSpec.LONGVARNCHAR: {
                // TODO -- What's the difference between varchar(0) and text?
                return "text";
            }
            case TypeSpec.RAW:
            case TypeSpec.VARRAW:
            case TypeSpec.LONGVARRAW: {
                return "bytea";
            }
            case TypeSpec.DATE: {
                return "date";
            }
            case TypeSpec.TIME: {
                return "time(" + td.size + ")";
            }
            case TypeSpec.TIME_TZ: {
                return "time(" + td.size + ") with time zone";
            }
            case TypeSpec.TIMESTAMP: {
                return "timestamp(" + td.size + ")";
            }
            case TypeSpec.TIMESTAMP_TZ: {
                return "timestamp(" + td.size + ") with time zone";
            }
            case TypeSpec.INTERVAL_YM: {
                return "interval(0)";
            }
            case TypeSpec.INTERVAL_DS: {
                return "interval(" + td.scale + ")";
            }
            case TypeSpec.INTERVAL_YS: {
                return "interval(" + td.size + ")";
            }
            default: {
                // TODO - Warning (internal error); should never get here
                return td.native_representation;
            }
        }
    }

    private static DecimalFormat df = new DecimalFormat("0.#########");

    protected String toSqlString(TypeSpec spec, Object obj) {
        if (obj == null)
            return super.toSqlString(spec, obj);
        if (spec.type == TypeSpec.INTERVAL_YS
                || spec.type == TypeSpec.INTERVAL_YM
                || spec.type == TypeSpec.INTERVAL_DS) {
            Interval inter = (Interval) obj;
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
            StringBuffer buf = new StringBuffer();
            buf.append('\'');
            if (years != 0) {
                buf.append(years);
                buf.append(" year");
            }
            if (months != 0) {
                if (buf.length() != 1)
                    buf.append(' ');
                buf.append(months);
                buf.append(" month");
            }
            if (days != 0) {
                if (buf.length() != 1)
                    buf.append(' ');
                buf.append(days);
                buf.append(" day");
            }
            if (hours != 0) {
                if (buf.length() != 1)
                    buf.append(' ');
                buf.append(hours);
                buf.append(" hour");
            }
            if (minutes != 0) {
                if (buf.length() != 1)
                    buf.append(' ');
                buf.append(minutes);
                buf.append(" minute");
            }
            if (seconds != 0 || buf.length() == 1) {
                if (buf.length() != 1)
                    buf.append(' ');
                String s;
                synchronized (df) {
                    s = df.format(seconds);
                }
                buf.append(s);
                buf.append(" second");
            }
            buf.append('\'');
            return buf.toString();
        }
        if (obj instanceof BlobWrapper || obj instanceof byte[]) {
            byte[] ba;
            if (obj instanceof BlobWrapper)
                ba = ((BlobWrapper) obj).load();
            else
                ba = (byte[]) obj;
            StringBuffer buf = new StringBuffer();
            buf.append('\'');
            for (int i = 0; i < ba.length; i++) {
                int c = ba[i];
                if (c < 0)
                    c += 256;
                else if (c >= 32 && c <= 126 && c != '\'' && c != '\\')
                    buf.append((char) c);
                else {
                    buf.append("\\\\");
                    buf.append('0' + (c >> 6));
                    buf.append('0' + ((c >> 3) & 7));
                    buf.append('0' + (c & 7));
                }
            }
            buf.append('\'');
            return buf.toString();
        } else
            return super.toSqlString(spec, obj);
    }
}
