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

import java.awt.Toolkit;
//import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.*;
//import java.awt.datatransfer.StringSelection;
//import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.util.*;

public class Clipboard {
    public static final Object EMPTY = new Object();
    private Object data;
    private ArrayList listeners;
    private java.awt.datatransfer.Clipboard sysClip;
    private Transferable sysData;

    public Clipboard() {
	sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
	try {
	    sysData = sysClip.getContents(null);
	} catch (IllegalStateException e) {
	    sysData = null;
	}
	String s = tr2str(sysData);
	data = s == null ? EMPTY : s;
	listeners = new ArrayList();
    }

    public void put(Object data) {
	this.data = data;
	try {
	    Transferable tr = data == null ? null :
			      data == EMPTY ? null :
				new StringSelection(data.toString());
	    sysClip.setContents(tr, null);
	} catch (IllegalStateException e) {}
	notifyListeners();
    }

    public Object get() {
	try {
	    Transferable tr = sysClip.getContents(null);
	    if (tr != sysData)
		// System clipboard changed behind our back
		sys2nav();
	} catch (IllegalStateException e) {}
	return data;
    }

    public void addListener(Listener listener) {
	listeners.add(listener);
    }

    public void removeListener(Listener listener) {
	listeners.remove(listener);
    }

    public void sys2nav() {
	try {
	    sysData = sysClip.getContents(null);
	} catch (IllegalStateException e) {
	    return;
	}
	String s = tr2str(sysData);
	data = s == null ? EMPTY : s;
	notifyListeners();
    }

    public interface Listener {
	void clipboardUpdated(Object data);
    }

    private void notifyListeners() {
	for (Iterator iter = listeners.iterator(); iter.hasNext();) {
	    Listener listener = (Listener) iter.next();
	    listener.clipboardUpdated(data);
	}
    }

    private static String tr2str(Transferable tr) {
	if (tr == null)
	    return null;
	try {
	    return (String) tr.getTransferData(DataFlavor.stringFlavor);
	} catch (UnsupportedFlavorException e) {
	    return null;
	} catch (IOException e) {
	    return null;
	}
    }
}
