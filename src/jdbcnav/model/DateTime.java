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

package jdbcnav.model;

import java.util.*;

/**
 * This class implements a database-neutral Date/Time representation.
 * Theoretically, that would be the job of java.sql.Date, java.sql.Time,
 * and java.sql.Timestamp, but those don't handle time zones properly.
 */
public class DateTime implements Comparable<DateTime> {
    public static final int ZONE_ID = 0;
    public static final int ZONE_SHORT = 1;
    public static final int ZONE_LONG = 2;
    public static final int ZONE_OFFSET = 3;
    public static final int ZONE_NONE = 4;

    private static HashMap<String, TimeZone> zoneMap;
    static {
        zoneMap = new HashMap<String, TimeZone>();
        String[] ids = TimeZone.getAvailableIDs();
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            TimeZone tz = TimeZone.getTimeZone(id);
            zoneMap.put(id, tz);
            zoneMap.put(tz.getDisplayName(false, TimeZone.SHORT), tz);
            zoneMap.put(tz.getDisplayName(false, TimeZone.LONG), tz);
            zoneMap.put(tz.getDisplayName(true, TimeZone.SHORT), tz);
            zoneMap.put(tz.getDisplayName(true, TimeZone.LONG), tz);
        }
    }

    /**
     * The 'time' field is the time in milliseconds since 1970-01-01 00:00:00
     * GMT. The time is a multiple of 1000; milliseconds go in the 'nanos'
     * field.
     */
    public long time;

    /**
     * Sub-second time information; the actual time, in seconds, is
     * time / 1000.0 + nanos / 1000000000.0
     */
    public int nanos;

    /**
     * Time Zone; if null, use the default time zone.
     */
    public java.util.TimeZone tz;

    public DateTime(long time, int nanos, TimeZone tz) {
        this.time = (time / 1000) * 1000;
        int millis = (int) (time - this.time);
        if (millis < 0) {
            millis += 1000;
            this.time -= 1000;
        }

        this.nanos = nanos + millis * 1000000;
        if (this.nanos > 1000000000) {
            this.nanos -= 1000000000;
            this.time += 1000;
        }

        this.tz = tz;
        java.sql.Timestamp ts = new java.sql.Timestamp(this.time);
        ts.setNanos(this.nanos);
    }

    public DateTime(String s) {
        StringTokenizer tok = new StringTokenizer(s, " ");
        int year = 0;
        int month = 0, day = 0, hour = 0, minute = 0, second = 0;
        StringBuffer tzname = new StringBuffer();
        boolean haveDate = false;
        boolean haveTime = false;
        while (tok.hasMoreTokens()) {
            String p = tok.nextToken();
            if (!haveDate && p.indexOf('-') != -1) {
                // Date
                StringTokenizer t2 = new StringTokenizer(p, "-");
                try {
                    year = Integer.parseInt(t2.nextToken());
                    month = Integer.parseInt(t2.nextToken());
                    day = Integer.parseInt(t2.nextToken());
                    haveDate = true;
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                                            "Malformed date (" + p + ")");
                }
            } else if (!haveTime && (p.indexOf('.') != -1 || p.indexOf(':') != -1)) {
                // Time
                StringTokenizer t2 = new StringTokenizer(p, ":.");
                try {
                    hour = Integer.parseInt(t2.nextToken());
                    minute = Integer.parseInt(t2.nextToken());
                    try {
                        second = Integer.parseInt(t2.nextToken());
                        String n = t2.nextToken();
                        n += "00000000".substring(n.length() - 1);
                        nanos = Integer.parseInt(n);
                    } catch (NoSuchElementException e) {}
                    haveTime = true;
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                                            "Malformed time (" + p + ")");
                }
            } else {
                if (tzname.length() > 0)
                    tzname.append(' ');
                tzname.append(p);
            }
        }
        if (tzname.length() > 0) {
            String n = tzname.toString();
            if (n.startsWith("GMT+") || n.startsWith("GMT-"))
                tz = TimeZone.getTimeZone(n);
            else
                tz = zoneMap.get(n);
        }
        GregorianCalendar cal = new GregorianCalendar(
                                    tz == null ? TimeZone.getDefault() : tz);
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        time = cal.getTimeInMillis();
    }

    public String toString() {
        return toString(TypeSpec.TIMESTAMP_TZ, 9, ZONE_ID);
    }

    public String toString(TypeSpec spec) {
        return toString(spec.type, spec.size, ZONE_ID);
    }

    public String toString(TypeSpec spec, int zoneStyle) {
        return toString(spec.type, spec.size, zoneStyle);
    }

    public String toString(int type, int size, int zoneStyle) {
        GregorianCalendar cal = new GregorianCalendar(
                                    tz == null ? TimeZone.getDefault() : tz);
        cal.setTimeInMillis(time);
        StringBuffer buf = new StringBuffer();

        if (type == TypeSpec.DATE
                || type == TypeSpec.TIMESTAMP
                || type == TypeSpec.TIMESTAMP_TZ) {
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;
            int day = cal.get(Calendar.DAY_OF_MONTH);
            buf.append(Integer.toString(year));
            buf.append('-');
            if (month < 10)
                buf.append('0');
            buf.append(Integer.toString(month));
            buf.append('-');
            if (day < 10)
                buf.append('0');
            buf.append(Integer.toString(day));
        }

        if (type == TypeSpec.TIME
                || type == TypeSpec.TIME_TZ
                || type == TypeSpec.TIMESTAMP
                || type == TypeSpec.TIMESTAMP_TZ) {
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int minute = cal.get(Calendar.MINUTE);
            int second = cal.get(Calendar.SECOND);
            if (buf.length() > 0)
                buf.append(' ');
            if (hour < 10)
                buf.append('0');
            buf.append(Integer.toString(hour));
            buf.append(':');
            if (minute < 10)
                buf.append('0');
            buf.append(Integer.toString(minute));
            buf.append(':');
            if (second < 10)
                buf.append('0');
            buf.append(Integer.toString(second));
            if (size > 0) {
                buf.append('.');
                String n = Integer.toString(1000000000 + nanos);
                int sz = size > 9 ? 9 : size;
                buf.append(n.substring(1, 1 + sz));
            }
        }

        if (zoneStyle != ZONE_NONE
                && (type == TypeSpec.TIME_TZ
                || type == TypeSpec.TIMESTAMP_TZ)) {
            TimeZone zone = tz;
            if (zone == null)
                zone = TimeZone.getDefault();
            if (buf.length() > 0)
                buf.append(' ');
            boolean dst = zone.inDaylightTime(new java.util.Date(time));
            switch (zoneStyle) {
                case ZONE_ID:
                    buf.append(zone.getID());
                    break;
                case ZONE_SHORT:
                    buf.append(zone.getDisplayName(dst, TimeZone.SHORT));
                    break;
                case ZONE_LONG:
                    buf.append(zone.getDisplayName(dst, TimeZone.LONG));
                    break;
                case ZONE_OFFSET:
                    int offset = zone.getOffset(time) / 60000;
                    if (offset < 0) {
                        buf.append('-');
                        offset = -offset;
                    } else
                        buf.append('+');
                    buf.append(offset / 60);
                    buf.append(':');
                    offset %= 60;
                    if (offset < 10)
                        buf.append('0');
                    buf.append(offset);
                    break;
            }
        }

        return buf.toString();
    }

    public boolean equals(Object o) {
        if (!(o instanceof DateTime))
            return false;
        DateTime that = (DateTime) o;
        return time == that.time && nanos == that.nanos
            && (tz == null ? that.tz == null : tz.equals(that.tz));
    }

    public int compareTo(DateTime that) {
        if (time < that.time)
            return -1;
        else if (time > that.time)
            return 1;
        else if (nanos < that.nanos)
            return -1;
        else if (nanos > that.nanos)
            return 1;
        else
            return 0;
    }
}
