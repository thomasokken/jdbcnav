package jdbcnav.javascript;

import org.mozilla.javascript.*;

public class JavaScriptArray implements Scriptable {
    private Object[] array;
    public JavaScriptArray(Object[] array) {
	this.array = array;
    }
    public void delete(int colIndex) {
	//
    }
    public void delete(String name) {
	//
    }
    public Object get(int index, Scriptable start) {
	if (index < 0 || index >= array.length)
	    return NOT_FOUND;
	return array[index];
    }
    public Object get(String name, Scriptable start) {
	if (name.equals("length"))
	    return new Integer(array.length);
	return NOT_FOUND;
    }
    public String getClassName() {
	return "JavaScriptArray";
    }
    public Object getDefaultValue(Class hint) {
	StringBuffer buf = new StringBuffer();
	buf.append("[ ");
	for (int i = 0; i < array.length; i++) {
	    Object o = array[i];
	    if (o instanceof Element)
		buf.append(((Element) o).jsString());
	    else
		buf.append(o);
	    buf.append(" "); 
	}
	buf.append("]");
	return buf.toString();
    }
    public Object[] getIds() {
	return new Object[] { "length" };
    }
    public Scriptable getParentScope() {
	return null;
    }
    public Scriptable getPrototype() {
	return null;
    }
    public boolean has(int index, Scriptable start) {
	return index >= 0 && index < array.length;
    }
    public boolean has(String name, Scriptable start) {
	return name.equals("length");
    }
    public boolean hasInstance(Scriptable instance) {
	return instance instanceof JavaScriptArray;
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

    public interface Element {
	public String jsString();
    }
}
