package jdbcnav.model;

import java.util.*;

/**
 * This class implements a database-neutral Date/Time representation.
 * Theoretically, that would be the job of java.sql.Date, java.sql.Time,
 * and java.sql.Timestamp, but those don't handle time zones properly.
 */
public class DateTime {
    private static HashMap zoneMap;
    static {
	zoneMap = new HashMap();
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

    public DateTime(long time, int nanos, TimeZone tz) {
	this.time = time;
	this.nanos = nanos;
	this.tz = tz;
    }

    public DateTime(String s) {
	StringTokenizer tok = new StringTokenizer(s, " ");
	int year = 0;
	int month = 0, day = 0, hour = 0, minute = 0, second = 0;
	StringBuffer tzname = new StringBuffer();
	while (tok.hasMoreTokens()) {
	    String p = tok.nextToken();
	    if (p.indexOf('-') != -1) {
		// Date
		StringTokenizer t2 = new StringTokenizer(p, "-");
		try {
		    year = Integer.parseInt(t2.nextToken());
		    month = Integer.parseInt(t2.nextToken());
		    day = Integer.parseInt(t2.nextToken());
		} catch (Exception e) {
		    throw new IllegalArgumentException(
					    "Malformed date (" + p + ")");
		}
	    } else if (p.indexOf('.') != -1 || p.indexOf(':') != -1) {
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
	GregorianCalendar cal = new GregorianCalendar(
				    tz == null ? TimeZone.getDefault() : tz);
	cal.set(year, month - 1, day, hour, minute, second);
	time = cal.getTimeInMillis();
	if (tzname.length() > 0)
	    tz = (TimeZone) zoneMap.get(tzname.toString());
    }

    public String toString(TypeSpec spec) {
	GregorianCalendar cal = new GregorianCalendar(
				    tz == null ? TimeZone.getDefault() : tz);
	cal.setTimeInMillis(time);
	StringBuffer buf = new StringBuffer();

	if (spec.type == TypeSpec.DATE
		|| spec.type == TypeSpec.TIMESTAMP
		|| spec.type == TypeSpec.TIMESTAMP_TZ) {
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

	if (spec.type == TypeSpec.TIME
		|| spec.type == TypeSpec.TIME_TZ
		|| spec.type == TypeSpec.TIMESTAMP
		|| spec.type == TypeSpec.TIMESTAMP_TZ) {
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
	    if (spec.size > 0) {
		buf.append('.');
		String n = Integer.toString(1000000000 + nanos);
		buf.append(n.substring(1, 1 + spec.size));
	    }
	}

	if ((spec.type == TypeSpec.TIME_TZ
		|| spec.type == TypeSpec.TIMESTAMP_TZ)
		&& tz != null) {
	    if (buf.length() > 0)
		buf.append(' ');
	    boolean dst = tz.inDaylightTime(new java.util.Date(time));
	    buf.append(tz.getDisplayName(dst, TimeZone.LONG));
	}

	return buf.toString();
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
}
