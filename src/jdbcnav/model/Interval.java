///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2006  Thomas Okken
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

import java.util.StringTokenizer;

/**
 * This class implements a database-neutral Interval representation.
 */
public class Interval implements Comparable {
    public int months;
    public long nanos;

    public Interval(int months, long nanos) {
	this.months = months;
	this.nanos = nanos;
    }

    public Interval(TypeSpec spec, String s) {
	int days = 0;
	boolean haveNanos = false;
	boolean haveDate = false;
	s = s.trim();
	boolean negative = s.length() > 0 && s.charAt(0) == '-';
	if (negative || s.length() > 0 && s.charAt(0) == '+')
	    s = s.substring(1);

	StringTokenizer tok = new StringTokenizer(s, " ");
        int ntokens = tok.countTokens();
	while (tok.hasMoreTokens()) {
	    String t = tok.nextToken();
	    if (t.indexOf(':') != -1 || t.indexOf('.') != -1
		    || ntokens == 1
			&& spec.type == TypeSpec.INTERVAL_DS) {
		if (haveNanos || spec.type == TypeSpec.INTERVAL_YM)
		    throw new IllegalArgumentException(
					    "Malformed Interval: " + s);
		StringTokenizer tok2 = new StringTokenizer(t, ":.", true);
		boolean sawDot = false;
		while (tok2.hasMoreTokens()) {
		    String t2 = tok2.nextToken();
		    if (t2.equals(":")) {
			nanos *= 60;
			continue;
		    }
		    if (t2.equals(".")) {
			if (sawDot)
			    throw new IllegalArgumentException(
					    "Malformed Interval: " + s);
			sawDot = true;
			continue;
		    }
		    if (sawDot) {
			int t2len = t2.length();
			if (t2len > 9)
			    t2 = t2.substring(0, 9);
			else if (t2len < 9)
			    t2 += "000000000".substring(t2len);
		    }
		    long x = 0;
		    try {
			x = Integer.parseInt(t2);
		    } catch (NumberFormatException e) {
			throw new IllegalArgumentException(
					    "Malformed Interval: " + s);
		    }
		    if (sawDot)
			nanos += x;
		    else
			nanos += x * 1000000000;
		}
		haveNanos = true;
	    } else {
		if (haveDate)
		    throw new IllegalArgumentException(
					    "Malformed Interval: " + s);
		StringTokenizer tok2 = new StringTokenizer(t, "-");
		int[] comp = new int[3];
		int n = 0;
		int maxcomps = spec.type == TypeSpec.INTERVAL_YS ? 3
			     : spec.type == TypeSpec.INTERVAL_YM ? 2
			     : /* INTERVAL_DS */ 1;
		while (tok2.hasMoreTokens()) {
		    if (n == maxcomps)
			throw new IllegalArgumentException(
					    "Malformed Interval: " + s);
		    String t2 = tok2.nextToken();
		    try {
			comp[n++] = Integer.parseInt(t2);
		    } catch (NumberFormatException e) {
			throw new IllegalArgumentException(
					    "Malformed Interval: " + s);
		    }
		}
		switch (spec.type) {
		    case TypeSpec.INTERVAL_YS:
			for (int i = 0; i < n; i++) {
			    int c = comp[n - i - 1];
			    switch (i) {
				case 0: days = c; break;
				case 1: months = c; break;
				case 2: months += 12 * c; break;
			    }
			}
			break;
		    case TypeSpec.INTERVAL_YM:
			if (n == 0)
			    throw new IllegalArgumentException(
					    "Malformed Interval: " + s);
			else if (n == 1)
			    months = comp[0];
			else
			    months = 12 * comp[0] + comp[1];
			break;
		    case TypeSpec.INTERVAL_DS:
			if (n > 0)
			    days = comp[0];
			break;
		}
		haveDate = true;
	    }
	}
	nanos += days * 86400000000000L;
	if (negative) {
	    months = -months;
	    nanos = -nanos;
	}
    }

    public String toString() {
	return toString(TypeSpec.INTERVAL_YS, 9);
    }

    public String toString(TypeSpec spec) {
	int scale = spec.type == TypeSpec.INTERVAL_DS ? spec.scale : spec.size;
	return toString(spec.type, scale);
    }

    public String toString(int type, int scale) {
	StringBuffer buf = new StringBuffer();
	long n = nanos;
	if (n < 0) {
	    buf.append('-');
	    n = -n;
	} else
	    buf.append('+');
	if (type == TypeSpec.INTERVAL_YM || type == TypeSpec.INTERVAL_YS) {
	    int m = months;
	    if (m < 0)
		m = -m;
	    buf.append(m / 12);
	    buf.append('-');
	    buf.append(m % 12);
	}
	if (type == TypeSpec.INTERVAL_YS)
	    buf.append('-');
	if (type == TypeSpec.INTERVAL_DS || type == TypeSpec.INTERVAL_YS) {
	    buf.append(n / 86400000000000L);
	    buf.append(' ');
	    n %= 86400000000000L;
	    buf.append(n / 3600000000000L);
	    buf.append(':');
	    n %= 3600000000000L;
	    int m = (int) (n / 60000000000L);
	    if (m < 10)
		buf.append('0');
	    buf.append(m);
	    buf.append(':');
	    n %= 60000000000L;
	    m = (int) (n / 1000000000);
	    if (m < 10)
		buf.append('0');
	    buf.append(m);
	    if (scale > 0) {
		buf.append('.');
		n = n % 1000000000 + 1000000000;
		m = scale > 9 ? 9 : scale;
		buf.append(Long.toString(n).substring(1, 1 + m));
	    }
	}
	return buf.toString();
    }

    public boolean equals(Object o) {
	if (!(o instanceof Interval))
	    return false;
	Interval that = (Interval) o;
	return months == that.months && nanos == that.nanos;
    }

    public int compareTo(Object o) {
	Interval that = (Interval) o;
	if (months < that.months)
	    return -1;
	else if (months > that.months)
	    return 1;
	else if (nanos < that.nanos)
	    return -1;
	else if (nanos > that.nanos)
	    return 1;
	else
	    return 0;
    }
}
