///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2023  Thomas Okken
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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
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
        } else if (driverClassName.endsWith(".DB2Driver")) {
            return "DB2";
        } else if (driverClassName.startsWith("org.apache.derby.")) {
            return "Derby";
        } else if (driverClassName.equals("com.mysql.jdbc.Driver")) {
            return "MySQL";
        } else if (driverClassName.equals("com.microsoft.sqlserver.jdbc.SQLServerDriver")) {
            return "MS_SQL";
        } else if (driverClassName.equals("oracle.jdbc.driver.OracleDriver")
                || driverClassName.equals("oracle.jdbc.OracleDriver")) {
            if (con == null)
                return "Oracle 10";
            try {
                DatabaseMetaData dbmd = con.getMetaData();
                String v = dbmd.getDatabaseProductVersion();
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
        } else if (driverClassName.equals("smallsql.server.SSDriver")
                || driverClassName.equals("smallsql.database.SSDriver")) {
            return "SmallSQL";
        } else if (driverClassName.equals("transbase.jdbc.Driver")) {
            return "Transbase";
        } else {
            return "Generic";
        }
    }

    private static TreeMap<String, String> databaseMap;
    static {
        databaseMap = new TreeMap<String, String>();
        databaseMap.put("Generic", "jdbcnav.JDBCDatabase");
        databaseMap.put("DB2", "jdbcnav.JDBCDatabase_DB2");
        databaseMap.put("Derby", "jdbcnav.JDBCDatabase_Derby");
        databaseMap.put("MySQL", "jdbcnav.JDBCDatabase_MySQL");
        databaseMap.put("MS_SQL", "jdbcnav.JDBCDatabase_MS_SQL");
        databaseMap.put("Oracle 8", "jdbcnav.JDBCDatabase_Oracle");
        databaseMap.put("Oracle 9", "jdbcnav.JDBCDatabase_Oracle");
        databaseMap.put("Oracle 10", "jdbcnav.JDBCDatabase_Oracle");
        databaseMap.put("PostgreSQL", "jdbcnav.JDBCDatabase_PostgreSQL");
        databaseMap.put("SmallSQL", "jdbcnav.JDBCDatabase_SmallSQL");
        databaseMap.put("Transbase", "jdbcnav.JDBCDatabase_Transbase");
    }

    public static String getDatabaseClassName(String driverName) {
        return databaseMap.get(driverName);
    }

    private static TreeMap<String, String> scriptGenMap;
    static {
        scriptGenMap = new TreeMap<String, String>();
        scriptGenMap.put("Generic", "jdbcnav.ScriptGenerator");
        scriptGenMap.put("DB2", "jdbcnav.ScriptGenerator_DB2");
        scriptGenMap.put("Derby", "jdbcnav.ScriptGenerator_Derby");
        scriptGenMap.put("MySQL", "jdbcnav.ScriptGenerator_MySQL");
        scriptGenMap.put("MS_SQL", "jdbcnav.ScriptGenerator_MS_SQL");
        scriptGenMap.put("Oracle 8", "jdbcnav.ScriptGenerator_Oracle8");
        scriptGenMap.put("Oracle 9", "jdbcnav.ScriptGenerator_Oracle9");
        scriptGenMap.put("Oracle 10", "jdbcnav.ScriptGenerator_Oracle");
        scriptGenMap.put("PostgreSQL", "jdbcnav.ScriptGenerator_PostgreSQL");
        scriptGenMap.put("SmallSQL", "jdbcnav.ScriptGenerator_SmallSQL");
        scriptGenMap.put("Transbase", "jdbcnav.ScriptGenerator_Transbase");
    }

    public static String getScriptGeneratorClassName(String driverName) {
        return scriptGenMap.get(driverName);
    }

    public static String[] getScriptGeneratorNames() {
        ArrayList<String> list = new ArrayList<String>(scriptGenMap.keySet());
        list.remove("Generic");
        list.add(0, "Generic");
        return list.toArray(new String[list.size()]);
    }
}
