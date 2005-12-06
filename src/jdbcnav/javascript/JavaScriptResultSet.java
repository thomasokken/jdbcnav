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

package jdbcnav.javascript;

import java.sql.*;
import java.util.*;
import org.mozilla.javascript.*;

import jdbcnav.util.*;


public class JavaScriptResultSet implements Scriptable {
    private ResultSet rs;
    private CloseFunction closeFunction = new CloseFunction();
    private NextFunction nextFunction = new NextFunction();
    private int columnCount;
    private TreeMap columnMap;
    private String[] columnNames;
    private String[] dbTypes;
    private String[] sqlTypes;
    private String[] javaTypes;
    private boolean nextNotYetCalled = true;

    public JavaScriptResultSet(ResultSet rs) {
	this.rs = rs;
	columnMap = new TreeMap();
	try {
	    ResultSetMetaData rsmd = rs.getMetaData();
	    columnCount = rsmd.getColumnCount();
	    columnNames = new String[columnCount];
	    dbTypes = new String[columnCount];
	    sqlTypes = new String[columnCount];
	    javaTypes = new String[columnCount];
	    for (int i = 1; i <= columnCount; i++) {
		String name = rsmd.getColumnName(i);
		columnNames[i - 1] = name;
		columnMap.put(name.toUpperCase(), new Integer(i));
		dbTypes[i - 1] = rsmd.getColumnTypeName(i);
		sqlTypes[i - 1] = MiscUtils.sqlTypeIntToString(
						    rsmd.getColumnType(i));
		javaTypes[i - 1] = rsmd.getColumnClassName(i);
	    }
	} catch (SQLException e) {
	    columnCount = 0;
	    columnMap.clear();
	    columnNames = new String[0];
	    dbTypes = new String[0];
	    sqlTypes = new String[0];
	    javaTypes = new String[0];
	}
    }

    private class CloseFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length != 0)
		throw new EvaluatorException(
			    "ResultSet.close() requires no arguments.");
	    try {
		rs.close();
	    } catch (SQLException e) {
		throw new WrappedException(e);
	    }
	    return Context.getUndefinedValue();
	}
    }

    private class NextFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length != 0)
		throw new EvaluatorException(
			"ResultSet.next() requires no arguments.");
	    nextNotYetCalled = false;
	    try {
		return rs.next() ? Boolean.TRUE : Boolean.FALSE;
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
	// Using 0-based indexing... The combination of square bracket
	// indexing and JDBC-style 1-based column numbers just feels
	// a bit too weird.
	if (index < 0 || index >= columnCount)
	    return NOT_FOUND;
	else
	    try {
		return rs.getObject(index + 1);
	    } catch (SQLException e) {
		throw new EvaluatorException(MiscUtils.throwableToString(e));
	    }
    }

    public Object get(String name, Scriptable start) {
	if (name.equals("close"))
	    return closeFunction;
	else if (name.equals("next"))
	    return nextFunction;
	else if (name.equals("columns"))
	    return new JavaScriptArray(columnNames);
	else if (name.equals("dbtypes"))
	    return new JavaScriptArray(dbTypes);
	else if (name.equals("sqltypes"))
	    return new JavaScriptArray(sqlTypes);
	else if (name.equals("javatypes"))
	    return new JavaScriptArray(javaTypes);
	else if (name.equals("length"))
	    return new Integer(columnCount);
	else {
	    // I know, I know, you can simply pass the column name to
	    // ResultSet.getObject(). The reason I don't do that is because
	    // I want to return NOT_FOUND if a nonexistent column name is
	    // specified, and not throw an exception; if I pass the name
	    // without looking at it first, I can't tell the SQLException
	    // thrown because the column was not found from a SQLException
	    // thrown for some other reason.
	    Integer i = (Integer) columnMap.get(name.toUpperCase());
	    if (i == null)
		return NOT_FOUND;
	    else
		try {
		    return rs.getObject(i.intValue());
		} catch (SQLException e) {
		    throw new EvaluatorException(
					MiscUtils.throwableToString(e));
		}
	}
    }

    public String getClassName() {
	return "ResultSet";
    }

    public Object getDefaultValue(Class hint) {
	StringBuffer buf = new StringBuffer();
	buf.append("[ ");
	for (int col = 1; col <= columnCount; col++) {
	    if (nextNotYetCalled)
		buf.append(columnNames[col - 1]);
	    else
		try {
		    buf.append(rs.getObject(col));
		} catch (SQLException e) {
		    buf.append("???");
		}
	    buf.append(" ");
	}
	buf.append("]");
	return buf.toString();
    }

    public Object[] getIds() {
	return new Object[] {
	    "close",
	    "columns",
	    "dbtypes",
	    "javatypes",
	    "length",
	    "next",
	    "sqltypes"
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
	if (name.equals("next"))
	    return true;
	else if (name.equals("close"))
	    return true;
	else if (name.equals("columns"))
	    return true;
	else if (name.equals("dbtypes"))
	    return true;
	else if (name.equals("sqltypes"))
	    return true;
	else if (name.equals("javatypes"))
	    return true;
	else if (name.equals("length"))
	    return true;
	else
	    return columnMap.containsKey(name.toUpperCase());
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
