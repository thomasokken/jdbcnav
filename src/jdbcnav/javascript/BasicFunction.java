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

package jdbcnav.javascript;

import org.mozilla.javascript.*;


public abstract class BasicFunction implements Function {
    public abstract Object call(Object[] args);

    public Object call(Context ctx, Scriptable scope, Scriptable thisObj,
							    Object[] args) {
	return call(args);
    }
    public Scriptable construct(Context ctx, Scriptable scope,
							    Object[] args) {
	throw new EvaluatorException(
				"Can't just construct a " + getClassName());
    }
    // Scriptable stuff
    public void delete(int index) {
	//
    }
    public void delete(String name) {
	//
    }
    public Object get(int index, Scriptable start) {
	return NOT_FOUND;
    }
    public Object get(String name, Scriptable start) {
	return NOT_FOUND;
    }
    public String getClassName() {
	return getClass().getName();
    }
    public Object getDefaultValue(Class hint) {
	return toString();
    }
    public Object[] getIds() {
	return new Object[0];
    }
    public Scriptable getParentScope() {
	return null;
    }
    public Scriptable getPrototype() {
	return null;
    }
    public boolean has(int index, Scriptable start) {
	return false;
    }
    public boolean has(String name, Scriptable start) {
	return false;
    }
    public boolean hasInstance(Scriptable instance) {
	return getClass().isInstance(instance);
    }
    public void put(int index, Scriptable start, Object value) {
	//
    }
    public void put(String name, Scriptable start, Object value) {
	//
    }
    public void setParentScope(Scriptable parent) {
	//
    }
    public void setPrototype(Scriptable prototype) {
	//
    }
}
