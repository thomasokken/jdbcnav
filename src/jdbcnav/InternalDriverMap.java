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

package jdbcnav;

import java.sql.*;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * This class is used to locate the appropriate JDBC Navigator internal driver
 * for a given JDBC driver class and Connection, and to resolve the actual
 * JDBCDatabase and ScriptGenerator subclasses for a given driver name.
 */
public class InternalDriverMap {
    /**
     * Finds the JDBC Navigator internal driver name for a given JDBC driver
     * class and Connection. The Connection may be needed if there are multiple
     * JDBC Navigator internal drivers that match the same JDBC Driver class
     * (e.g. oracle.jdbc.driver.OracleDriver matches the Oracle 8 and Oracle 10
     * drivers); in such cases, this method may try to query the
     * DatabaseMetaData to find out which version of the product we're talking
     * to.
     */
    public static String getDriverName(String driverClassName, Connection con) {
	if (driverClassName == null) {
	    return "Generic";
	} else if (driverClassName.equals("com.mysql.jdbc.Driver")) {
	    return "MySQL";
	} else if (driverClassName.equals("oracle.jdbc.driver.OracleDriver")) {
	    if (con == null)
		return "Oracle 10";
	    try {
		DatabaseMetaData dbmd = con.getMetaData();
		String v = dbmd.getDatabaseProductVersion();
		StringBuffer buf = new StringBuffer();
		boolean in_num = false;
		int n = 0;
		for (int i = 0; i < v.length(); i++) {
		    char c = v.charAt(i);
		    if (c >= '0' && c <= '9') {
			in_num = true;
			n = n * 10 + c - '0';
		    } else {
			if (in_num)
			    break;
		    }
		}
		if (n <= 8)
		    return "Oracle 8";
		else if (n == 9)
		    return "Oracle 9";
		else
		    return "Oracle 10";
	    } catch (SQLException e) {
		return "Oracle 10";
	    }
	} else if (driverClassName.equals("org.postgresql.Driver")) {
	    return "PostgreSQL";
	} else if (driverClassName.equals("smallsql.server.SSDriver")) {
	    return "SmallSQL";
	} else if (driverClassName.equals("transbase.jdbc.Driver")) {
	    return "Transbase";
	} else {
	    return "Generic";
	}
    }

    private static TreeMap databaseMap;
    static {
	databaseMap = new TreeMap();
	databaseMap.put("Generic", "jdbcnav.JDBCDatabase");
	databaseMap.put("MySQL", "jdbcnav.JDBCDatabase_MySQL");
	databaseMap.put("Oracle 8", "jdbcnav.JDBCDatabase_Oracle");
	databaseMap.put("Oracle 9", "jdbcnav.JDBCDatabase_Oracle");
	databaseMap.put("Oracle 10", "jdbcnav.JDBCDatabase_Oracle");
	databaseMap.put("PostgreSQL", "jdbcnav.JDBCDatabase_PostgreSQL");
	databaseMap.put("SmallSQL", "jdbcnav.JDBCDatabase_SmallSQL");
	databaseMap.put("Transbase", "jdbcnav.JDBCDatabase_Transbase");
    }

    public static String getDatabaseClassName(String driverName) {
	return (String) databaseMap.get(driverName);
    }

    private static TreeMap scriptGenMap;
    static {
	scriptGenMap = new TreeMap();
	scriptGenMap.put("Generic", "jdbcnav.ScriptGenerator");
	scriptGenMap.put("MySQL", "jdbcnav.ScriptGenerator_MySQL");
	scriptGenMap.put("Oracle 8", "jdbcnav.ScriptGenerator_Oracle8");
	scriptGenMap.put("Oracle 9", "jdbcnav.ScriptGenerator_Oracle9");
	scriptGenMap.put("Oracle 10", "jdbcnav.ScriptGenerator_Oracle");
	scriptGenMap.put("PostgreSQL", "jdbcnav.ScriptGenerator_PostgreSQL");
	scriptGenMap.put("SmallSQL", "jdbcnav.ScriptGenerator_SmallSQL");
	scriptGenMap.put("Transbase", "jdbcnav.ScriptGenerator_Transbase");
    }

    public static String getScriptGeneratorClassName(String driverName) {
	return (String) scriptGenMap.get(driverName);
    }

    public static String[] getScriptGeneratorNames() {
	ArrayList list = new ArrayList(scriptGenMap.keySet());
	list.remove("Generic");
	list.add(0, "Generic");
	return (String[]) list.toArray(new String[list.size()]);
    }
}
