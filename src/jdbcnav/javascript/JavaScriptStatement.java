package jdbcnav.javascript;

import java.sql.*;
import org.mozilla.javascript.*;

import jdbcnav.util.*;


public class JavaScriptStatement implements Scriptable {
    private Statement stmt;
    private CloseFunction closeFunction = new CloseFunction();
    private ExecuteFunction executeFunction = new ExecuteFunction();

    public JavaScriptStatement(Statement stmt) {
	this.stmt = stmt;
    }

    private class CloseFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length != 0)
		throw new EvaluatorException(
				"Statement.close() requires no arguments.");
	    try {
		stmt.close();
	    } catch (SQLException e) {
		throw new WrappedException(e);
	    }
	    return Context.getUndefinedValue();
	}
    }

    private class ExecuteFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length != 1 || !(args[0] instanceof String))
		throw new EvaluatorException(
			"Statement.execute() requires one String argument.");
	    try {
		boolean hasResultSet = stmt.execute((String) args[0]);
		if (hasResultSet)
		    return new JavaScriptResultSet(stmt.getResultSet());
		else
		    return new Integer(stmt.getUpdateCount());
	    } catch (SQLException e) {
		throw new WrappedException(e);
	    }
	}
    }

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
	if (name.equals("execute"))
	    return executeFunction;
	else if (name.equals("close"))
	    return closeFunction;
	else
	    return NOT_FOUND;
    }

    public String getClassName() {
	return "Statement";
    }

    public Object getDefaultValue(Class hint) {
	return "Statement";
    }

    public Object[] getIds() {
	return new Object[] {
	    "close",
	    "execute"
	};
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
	return name.equals("execute") || name.equals("close");
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

    public void setParentScope(Scriptable parentScope) {
	//
    }

    public void setPrototype(Scriptable prototype) {
	//
    }
}
