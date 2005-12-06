///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2005  Thomas Okken
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

import java.util.*;

public class Clipboard {
    public static final Object EMPTY = new Object();
    private Object data = EMPTY;
    private ArrayList listeners = new ArrayList();

    public void put(Object data) {
	this.data = data;
	for (Iterator iter = listeners.iterator(); iter.hasNext();) {
	    Listener listener = (Listener) iter.next();
	    listener.clipboardUpdated(data);
	}
    }

    public Object get() {
	return data;
    }

    public void addListener(Listener listener) {
	listeners.add(listener);
    }

    public void removeListener(Listener listener) {
	listeners.remove(listener);
    }

    public interface Listener {
	void clipboardUpdated(Object data);
    }
}
