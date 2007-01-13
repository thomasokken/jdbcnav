///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2007  Thomas Okken
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

package jdbcnav.javascript;

import org.mozilla.javascript.*;

import jdbcnav.MessageBox;


public class JavaScriptGlobal extends ScriptableObject {
    private Pipe pipe;

    public JavaScriptGlobal() {
	String[] names = { "print", "println" };
	try {
	    defineFunctionProperties(names, JavaScriptGlobal.class,
				     ScriptableObject.DONTENUM
				     + ScriptableObject.PERMANENT);
	    defineProperty("clipboard", JavaScriptGlobal.class,
			   ScriptableObject.PERMANENT);
	} catch (PropertyException e) {
	    MessageBox.show("Problem constructing JavaScriptGlobal", e);
	}
    }

    public String getClassName() {
	return "global";
    }

    public void setOut(Pipe pipe) {
	this.pipe = pipe;
    }

    public static void print(Context ctx, Scriptable thisObj, Object[] args,
			     Function funObj) {
	Pipe pipe = ((JavaScriptGlobal) thisObj).pipe;
	for (int i = 0; i < args.length; i++) {
	    if (i > 0)
		pipe.print(" ");
	    pipe.print(Context.toString(args[i]));
	}
    }

    public static void println(Context ctx, Scriptable thisObj, Object[] args,
			     Function funObj) {
	Pipe pipe = ((JavaScriptGlobal) thisObj).pipe;
	for (int i = 0; i < args.length; i++) {
	    if (i > 0)
		pipe.print(" ");
	    pipe.print(Context.toString(args[i]));
	}
	pipe.println();
    }

    public void setClipboard(Object o) {
	jdbcnav.Main.getClipboard().put(o);
    }

    public Object getClipboard() {
	return jdbcnav.Main.getClipboard().get();
    }

    public interface Pipe {
	void print(String s);
	void println(String s);
	void println();
    }
}
