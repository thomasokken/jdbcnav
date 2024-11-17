///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2024  Thomas Okken
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

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class SearchParams {
    public String text;
    public boolean matchSubstring;
    // Note: when used as an interval for dates and timestamps, the search text
    // is interpreted as a number of days, but to get an exact representation,
    // we use intervalSeconds/intervalNanos, where the value is interpreted as
    // a number of seconds. That is, we multiply by 86400 so values with
    // 1-second resolution can be represented exactly, and values with
    // sub-second resolution can be represented exactly down to the
    // nanoseconds.
    public double interval;
    public long intervalSeconds;
    public int intervalNanos;
    
    public SearchParams(String searchText, String intervalText, boolean matchSubstring) {
        text = searchText;
        this.matchSubstring = matchSubstring;
        // This accepts intervals formatted like d.ddd or d:h:m:s.sss,
        // or an empty string, which is considered as an interval of zero.
        if (intervalText.length() == 0) {
            interval = 0;
            intervalSeconds = 0;
            intervalNanos = 0;
        } else if (!intervalText.contains(":")) {
            interval = Double.parseDouble(intervalText);
            if (interval < 0)
                throw new NumberFormatException("Negative interval");
            double seconds = interval * 86400;
            intervalSeconds = (long) seconds;
            intervalNanos = (int) ((seconds - intervalSeconds) * 1000000000);
            if (intervalNanos > 999999999)
                intervalNanos = 999999999;
        } else {
            StringTokenizer tok = new StringTokenizer(intervalText, ":", true);
            try {
                String t = tok.nextToken();
                if (t.equals(":"))
                    throw new NumberFormatException("Malformed interval");
                long days = Long.parseLong(t);
                if (days < 0)
                    throw new NumberFormatException("Negative interval");
                interval = days;
                intervalSeconds = days * 86400;
                intervalNanos = 0;
            } catch (NoSuchElementException e) {
                throw new NumberFormatException("Malformed interval");
            }
            try {
                String t = tok.nextToken();
                if (!t.equals(":"))
                    throw new NumberFormatException("Malformed interval");
                t = tok.nextToken();
                int hours = Integer.parseInt(t);
                if (hours < 0 || hours >= 24)
                    throw new NumberFormatException("Malformed interval");
                interval += hours / 24.0;
                intervalSeconds += hours * 3600;
                t = tok.nextToken();
                if (!t.equals(":"))
                    throw new NumberFormatException("Malformed interval");
                t = tok.nextToken();
                int minutes = Integer.parseInt(t);
                if (minutes < 0 || minutes >= 60)
                    throw new NumberFormatException("Malformed interval");
                interval += minutes / 1440.0;
                intervalSeconds += minutes * 60;
                t = tok.nextToken();
                if (!t.equals(":"))
                    throw new NumberFormatException("Malformed interval");
                t = tok.nextToken();
                double seconds = Double.parseDouble(t);
                if (seconds < 0 || seconds >= 60)
                    throw new NumberFormatException("Malformed interval");
                interval += seconds / 86400.0;
                int iseconds = (int) seconds;
                intervalSeconds += iseconds;
                intervalNanos = (int) ((seconds - iseconds) * 1000000000);
                t = tok.nextToken();
                // If the preceding line *doesn't* throw an exception, we have extraneous junk, and that's an error
                throw new NumberFormatException("Malformed interval");
            } catch (NoSuchElementException e) {}
        }
    }
}
