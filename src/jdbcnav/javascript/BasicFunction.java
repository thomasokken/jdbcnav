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
