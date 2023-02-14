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

package jdbcnav.model;

import org.mozilla.javascript.Scriptable;

import jdbcnav.javascript.JavaScriptArray;


public class BasicPrimaryKey implements PrimaryKey, Scriptable,
                                                    JavaScriptArray.Element {
    private String keyName;
    private String[] columns;

    public void setName(String keyName) {
        this.keyName = keyName;
    }

    public void setColumns(String[] columns) {
        this.columns = columns;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(super.toString());
        buf.append(" keyName=");
        buf.append(keyName);
        buf.append(" columns=");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0)
                buf.append(",");
            buf.append(columns[i]);
        }
        return buf.toString();
    }

    //////////////////////
    ///// PrimaryKey /////
    //////////////////////

    public String getName() {
        return keyName;
    }

    public int getColumnCount() {
        return columns.length;
    }

    public String getColumnName(int col) {
        return columns[col];
    }

    ///////////////////////////////////
    ///// JavaScriptArray.Element /////
    ///////////////////////////////////

    public String jsString() {
        return keyName;
    }

    //////////////////////
    ///// Scriptable /////
    //////////////////////
    
    public void delete(int colIndex) {
        //
    }
    public void delete(String name) {
        //
    }
    public Object get(int index, Scriptable start) {
        return NOT_FOUND;
    }
    public Object get(String name, Scriptable start) {
        if (name.equals("name")) {
            return keyName;
        } else if (name.equals("columns")) {
            return new JavaScriptArray(columns);
        } else
            return NOT_FOUND;
    }
    public String getClassName() {
        return "PrimaryKey";
    }
    public Object getDefaultValue(Class<?> hint) {
        StringBuffer buf = new StringBuffer();
        if (keyName != null) {
            buf.append(keyName);
            buf.append(" ");
        }
        buf.append("[ ");
        for (int i = 0; i < columns.length; i++) {
            buf.append(columns[i]);
            buf.append(" ");
        }
        buf.append("]");
        return buf.toString();
    }
    public Object[] getIds() {
        return new Object[] {
            "columns",
            "name"
        };
    }
    public Scriptable getParentScope() {
        return null;
    }
    public Scriptable getPrototype() {
        return null;
    }
    public boolean has(int colIndex, Scriptable start) {
        return false;
    }
    public boolean has(String name, Scriptable start) {
        if (name.equals("name"))
            return true;
        else if (name.equals("columns"))
            return true;
        else if (name.equals("hello"))
            return true;
        else
            return false;
    }
    public boolean hasInstance(Scriptable instance) {
        return instance instanceof PrimaryKey;
    }
    public void put(int colIndex, Scriptable start, Object value) {
        //
    }
    public void put(String name, Scriptable start, Object value) {
        //
    }
    public void setParentScope(Scriptable parentScope) {
        //
    }
    public void setPrototype(Scriptable prototype) {
        //
    }
}
