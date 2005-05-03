package jdbcnav.javascript;

import java.sql.*;
import org.mozilla.javascript.*;

import jdbcnav.util.*;


public class JavaScriptPreparedStatement implements Scriptable {
    private PreparedStatement pstmt;
    private CloseFunction closeFunction = new CloseFunction();
    private ExecuteFunction executeFunction = new ExecuteFunction();
    private SetFunction setFunction = new SetFunction();

    public JavaScriptPreparedStatement(PreparedStatement pstmt) {
	this.pstmt = pstmt;
    }

    private class CloseFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length != 0)
		throw new EvaluatorException(
			    "PreparedStatement.close() requires no arguments.");
	    try {
		pstmt.close();
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
			"PreparedStatement.execute() requires no arguments.");
	    try {
		boolean hasResultSet = pstmt.execute();
		if (hasResultSet)
		    return new JavaScriptResultSet(pstmt.getResultSet());
		else
		    return new Integer(pstmt.getUpdateCount());
	    } catch (SQLException e) {
		throw new WrappedException(e);
	    }
	}
    }

    private class SetFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length < 2 || args.length > 4)
		throw new EvaluatorException(
				    "PreparedStatement.setObject()"
				    + " requires 2, 3, or 4 arguments.");
	    if (args[0] instanceof Number
		    && (args.length < 3 ||
			args[2] instanceof Number || args[2] instanceof String)
		    && (args.length < 4 || args[3] instanceof Number)) {
		try {
		    if (args.length == 2)
			pstmt.setObject(((Number) args[0]).intValue(), args[1]);
		    else {
			int type;
			if (args[2] instanceof Number)
			    type = ((Number) args[2]).intValue();
			else
			    type = typeStringToInt((String) args[2]);
			if (args.length == 3)
			    pstmt.setObject(((Number) args[0]).intValue(),
					    args[1],
					    type);
			else
			    pstmt.setObject(((Number) args[0]).intValue(),
					    args[1],
					    type,
					    ((Number) args[3]).intValue());
		    }
		} catch (SQLException e) {
		    throw new WrappedException(e);
		}
	    } else
		throw new EvaluatorException("CallableStatement.setObject() "
					   + "requires numbers as its first "
					   + "and optional fourth parameters, "
					   + "and a number or a string as its "
					   + "optional third parameter.");
	    return Context.getUndefinedValue();
	}
    }

    private static int typeStringToInt(String type) {
	if (type.equalsIgnoreCase("ARRAY"))
	    return Types.ARRAY;
	else if (type.equalsIgnoreCase("BIGINT"))
	    return Types.BIGINT;
	else if (type.equalsIgnoreCase("BINARY"))
	    return Types.BINARY;
	else if (type.equalsIgnoreCase("BIT"))
	    return Types.BIT;
	else if (type.equalsIgnoreCase("BLOB"))
	    return Types.BLOB;
	else if (type.equalsIgnoreCase("BOOLEAN"))
	    return Types.BOOLEAN;
	else if (type.equalsIgnoreCase("CHAR"))
	    return Types.CHAR;
	else if (type.equalsIgnoreCase("CLOB"))
	    return Types.CLOB;
	else if (type.equalsIgnoreCase("DATALINK"))
	    return Types.DATALINK;
	else if (type.equalsIgnoreCase("DATE"))
	    return Types.DATE;
	else if (type.equalsIgnoreCase("DECIMAL"))
	    return Types.DECIMAL;
	else if (type.equalsIgnoreCase("DISTINCT"))
	    return Types.DISTINCT;
	else if (type.equalsIgnoreCase("DOUBLE"))
	    return Types.DOUBLE;
	else if (type.equalsIgnoreCase("FLOAT"))
	    return Types.FLOAT;
	else if (type.equalsIgnoreCase("INTEGER"))
	    return Types.INTEGER;
	else if (type.equalsIgnoreCase("JAVA_OBJECT"))
	    return Types.JAVA_OBJECT;
	else if (type.equalsIgnoreCase("LONGVARBINARY"))
	    return Types.LONGVARBINARY;
	else if (type.equalsIgnoreCase("LONGVARCHAR"))
	    return Types.LONGVARCHAR;
	else if (type.equalsIgnoreCase("NULL"))
	    return Types.NULL;
	else if (type.equalsIgnoreCase("NUMERIC"))
	    return Types.NUMERIC;
	else if (type.equalsIgnoreCase("OTHER"))
	    return Types.OTHER;
	else if (type.equalsIgnoreCase("REAL"))
	    return Types.REAL;
	else if (type.equalsIgnoreCase("REF"))
	    return Types.REF;
	else if (type.equalsIgnoreCase("SMALLINT"))
	    return Types.SMALLINT;
	else if (type.equalsIgnoreCase("STRUCT"))
	    return Types.STRUCT;
	else if (type.equalsIgnoreCase("TIME"))
	    return Types.TIME;
	else if (type.equalsIgnoreCase("TIMESTAMP"))
	    return Types.TIMESTAMP;
	else if (type.equalsIgnoreCase("TINYINT"))
	    return Types.TINYINT;
	else if (type.equalsIgnoreCase("VARBINARY"))
	    return Types.VARBINARY;
	else if (type.equalsIgnoreCase("VARCHAR"))
	    return Types.VARCHAR;
	else
	    throw new EvaluatorException(
		      "Type must be one of \"ARRAY\", \"BIGINT\", \"BINARY\", "
		    + "\"BIT\", \"BLOB\", \"BOOLEAN\", \"CHAR\", \"CLOB\", "
		    + "\"DATALINK\", \"DATE\", \"DECIMAL\", \"DISTINCT\", "
		    + "\"DOUBLE\", \"FLOAT\", \"INTEGER\", \"JAVA_OBJECT\", "
		    + "\"LONGVARBINARY\", \"LONGVARCHAR\", \"NULL\", "
		    + "\"NUMERIC\", \"OTHER\", \"REAL\", \"REF\", "
		    + "\"SMALLINT\", \"STRUCT\", \"TIME\", \"TIMESTAMP\", "
		    + "\"TINYINT\", \"VARBINARY\", or \"VARCHAR\".");
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
	else if (name.equals("setObject"))
	    return setFunction;
	else
	    return NOT_FOUND;
    }

    public String getClassName() {
	return "PreparedStatement";
    }

    public Object getDefaultValue(Class hint) {
	return "PreparedStatement";
    }

    public Object[] getIds() {
	return new Object[] {
	    "close",
	    "execute",
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
	else if (name.equals("setObject"))
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
