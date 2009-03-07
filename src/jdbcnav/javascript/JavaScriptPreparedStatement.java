///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2008	Thomas Okken
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
							try {
								type = MiscUtils.sqlTypeStringToInt(
														(String) args[2]);
							} catch (IllegalArgumentException e) {
								throw new EvaluatorException(e.getMessage());
							}
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
				throw new EvaluatorException("PreparedStatement.setObject() "
										   + "requires numbers as its first "
										   + "and optional fourth parameters, "
										   + "and a number or a string as its "
										   + "optional third parameter.");
			return Context.getUndefinedValue();
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
