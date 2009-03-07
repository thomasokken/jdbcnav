///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2009	Thomas Okken
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
					return stmt.getUpdateCount();
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

	@SuppressWarnings(value={"unchecked"})
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
