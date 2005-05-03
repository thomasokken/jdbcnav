package jdbcnav.model;

import org.mozilla.javascript.*;

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
    public Object getDefaultValue(Class hint) {
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
