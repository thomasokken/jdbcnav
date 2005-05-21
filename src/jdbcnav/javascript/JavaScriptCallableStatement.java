package jdbcnav.javascript;

import java.sql.*;
import org.mozilla.javascript.*;

import jdbcnav.util.*;


public class JavaScriptCallableStatement implements Scriptable {
    private CallableStatement cstmt;
    private CloseFunction closeFunction = new CloseFunction();
    private ExecuteFunction executeFunction = new ExecuteFunction();
    private RegisterFunction registerFunction = new RegisterFunction();
    private SetFunction setFunction = new SetFunction();
    private GetFunction getFunction = new GetFunction();

    public JavaScriptCallableStatement(CallableStatement cstmt) {
	this.cstmt = cstmt;
    }

    private class CloseFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length != 0)
		throw new EvaluatorException(
			    "CallableStatement.close() requires no arguments.");
	    try {
		cstmt.close();
	    } catch (SQLException e) {
		throw new WrappedException(e);
	    }
	    return Context.getUndefinedValue();
	}
    }

    private class ExecuteFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length != 0)
		throw new EvaluatorException(
			"CallableStatement.execute() requires no arguments.");
	    try {
		boolean hasResultSet = cstmt.execute();
		if (hasResultSet)
		    return new JavaScriptResultSet(cstmt.getResultSet());
		else
		    return new Integer(cstmt.getUpdateCount());
	    } catch (SQLException e) {
		throw new WrappedException(e);
	    }
	}
    }

    private class RegisterFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length < 2 || args.length > 3)
		throw new EvaluatorException(
				    "CallableStatement.registerOutParameter()"
				    + " requires 2 or 3 arguments.");
	    if ((args[0] instanceof Number || args[0] instanceof String)
		    && (args[1] instanceof Number || args[1] instanceof String)
		    && (args.length == 2 ||
		    args[2] instanceof Number || args[2] instanceof String)) {
		int type;
		if (args[1] instanceof Number)
		    type = ((Number) args[1]).intValue();
		else
		    try {
			type = MiscUtils.sqlTypeStringToInt((String) args[1]);
		    } catch (IllegalArgumentException e) {
			throw new WrappedException(e);
		    }
		try {
		    if (args.length == 2)
			if (args[0] instanceof Number)
			    cstmt.registerOutParameter(
					((Number) args[0]).intValue(),
					type);
			else
			    cstmt.registerOutParameter(
					(String) args[0],
					type);
		    else
			if (args[0] instanceof Number)
			    if (args[2] instanceof Number)
				cstmt.registerOutParameter(
					    ((Number) args[0]).intValue(),
					    type,
					    ((Number) args[2]).intValue());
			    else
				cstmt.registerOutParameter(
					    ((Number) args[0]).intValue(),
					    type,
					    (String) args[2]);
			else
			    if (args[2] instanceof Number)
				cstmt.registerOutParameter(
					    (String) args[0],
					    type,
					    ((Number) args[2]).intValue());
			    else
				cstmt.registerOutParameter(
					    (String) args[0],
					    type,
					    (String) args[2]);
		} catch (SQLException e) {
		    throw new WrappedException(e);
		}
	    } else
		throw new EvaluatorException(
			    "CallableStatement.registerOutParameter() "
			    + "requires Numbers or Strings as parameters.");
	    return Context.getUndefinedValue();
	}
    }

    private class SetFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length < 2 || args.length > 4)
		throw new EvaluatorException(
				    "CallableStatement.setObject()"
				    + " requires 2, 3, or 4 arguments.");
	    if ((args[0] instanceof Number || args[0] instanceof String)
		    && (args.length < 3 ||
			args[2] instanceof Number || args[2] instanceof String)
		    && (args.length < 4 || args[3] instanceof Number)) {
		try {
		    if (args.length == 2)
			if (args[0] instanceof Number)
			    cstmt.setObject(((Number) args[0]).intValue(),
					    args[1]);
			else
			    cstmt.setObject((String) args[0],
					    args[1]);
		    else {
			int type;
			if (args[2] instanceof Number)
			    type = ((Number) args[2]).intValue();
			else
			    try {
				type = MiscUtils.sqlTypeStringToInt(
							    (String) args[2]);
			    } catch (IllegalArgumentException e) {
				throw new WrappedException(e);
			    }
			if (args.length == 3)
			    if (args[0] instanceof Number)
				cstmt.setObject(((Number) args[0]).intValue(),
						args[1],
						type);
			    else
				cstmt.setObject((String) args[0],
						args[1],
						type);
			else
			    if (args[0] instanceof Number)
				cstmt.setObject(((Number) args[0]).intValue(),
						args[1],
						type,
						((Number) args[3]).intValue());
			    else
				cstmt.setObject((String) args[0],
						args[1],
						type,
						((Number) args[3]).intValue());
		    }
		} catch (SQLException e) {
		    throw new WrappedException(e);
		}
	    } else
		throw new EvaluatorException("CallableStatement.setObject() "
					   + "requires numbers or strings as "
					   + "its first and optional third, "
					   + "and a number as its optional "
					   + "fourth parameter.");
	    return Context.getUndefinedValue();
	}
    }

    private class GetFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length == 1 && (args[0] instanceof Number
				 || args[0] instanceof String)) {
		try {
		    if (args[0] instanceof Number)
			return cstmt.getObject(((Number) args[0]).intValue());
		    else
			return cstmt.getObject((String) args[0]);
		} catch (SQLException e) {
		    throw new WrappedException(e);
		}
	    } else {
		throw new EvaluatorException("CallableStatement.getObject() "
					   + "requires a single number or "
					   + "string parameter.");
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
	else if (name.equals("registerOutParameter"))
	    return registerFunction;
	else if (name.equals("setObject"))
	    return setFunction;
	else if (name.equals("getObject"))
	    return getFunction;
	else
	    return NOT_FOUND;
    }

    public String getClassName() {
	return "CallableStatement";
    }

    public Object getDefaultValue(Class hint) {
	return "CallableStatement";
    }

    public Object[] getIds() {
	return new Object[] {
	    "close",
	    "execute",
	    "getObject",
	    "registerOutParameter",
	    "setObject"
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
	if (name.equals("execute"))
	    return true;
	else if (name.equals("close"))
	    return true;
	else if (name.equals("registerOutParameter"))
	    return true;
	else if (name.equals("setObject"))
	    return true;
	else if (name.equals("getObject"))
	    return true;
	else
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

    public void setParentScope(Scriptable parentScope) {
	//
    }

    public void setPrototype(Scriptable prototype) {
	//
    }
}