///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2024  Thomas Okken
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

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrappedException;

import jdbcnav.util.MiscUtils;


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
                    return cstmt.getUpdateCount();
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
            if ((args[0] instanceof Number || args[0] instanceof CharSequence)
                    && (args[1] instanceof Number || args[1] instanceof CharSequence)
                    && (args.length == 2 ||
                    args[2] instanceof Number || args[2] instanceof CharSequence)) {
                int type;
                if (args[1] instanceof Number)
                    type = ((Number) args[1]).intValue();
                else
                    try {
                        type = MiscUtils.sqlTypeStringToInt(args[1].toString());
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
                                        args[0].toString(),
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
                                            args[2].toString());
                        else
                            if (args[2] instanceof Number)
                                cstmt.registerOutParameter(
                                            args[0].toString(),
                                            type,
                                            ((Number) args[2]).intValue());
                            else
                                cstmt.registerOutParameter(
                                            args[0].toString(),
                                            type,
                                            args[2].toString());
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
            if ((args[0] instanceof Number || args[0] instanceof CharSequence)
                    && (args.length < 3 ||
                        args[2] instanceof Number || args[2] instanceof CharSequence)
                    && (args.length < 4 || args[3] instanceof Number)) {
                try {
                    if (args.length == 2)
                        if (args[0] instanceof Number)
                            cstmt.setObject(((Number) args[0]).intValue(),
                                            args[1]);
                        else
                            cstmt.setObject(args[0].toString(),
                                            args[1]);
                    else {
                        int type;
                        if (args[2] instanceof Number)
                            type = ((Number) args[2]).intValue();
                        else
                            try {
                                type = MiscUtils.sqlTypeStringToInt(
                                                            args[2].toString());
                            } catch (IllegalArgumentException e) {
                                throw new WrappedException(e);
                            }
                        if (args.length == 3)
                            if (args[0] instanceof Number)
                                cstmt.setObject(((Number) args[0]).intValue(),
                                                args[1],
                                                type);
                            else
                                cstmt.setObject(args[0].toString(),
                                                args[1],
                                                type);
                        else
                            if (args[0] instanceof Number)
                                cstmt.setObject(((Number) args[0]).intValue(),
                                                args[1],
                                                type,
                                                ((Number) args[3]).intValue());
                            else
                                cstmt.setObject(args[0].toString(),
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
                                 || args[0] instanceof CharSequence)) {
                try {
                    if (args[0] instanceof Number)
                        return cstmt.getObject(((Number) args[0]).intValue());
                    else
                        return cstmt.getObject(args[0].toString());
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

    public Object getDefaultValue(Class<?> hint) {
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
