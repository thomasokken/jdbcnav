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

    private static TreeMap databaseClassMap;
    static {
	databaseClassMap = new TreeMap();
	databaseClassMap.put("Generic", "jdbcnav.JDBCDatabase");
	databaseClassMap.put("Oracle 8", "jdbcnav.JDBCDatabase_Oracle");
	databaseClassMap.put("Oracle 9", "jdbcnav.JDBCDatabase_Oracle");
	databaseClassMap.put("Oracle 10", "jdbcnav.JDBCDatabase_Oracle");
	databaseClassMap.put("PostgreSQL", "jdbcnav.JDBCDatabase_PostgreSQL");
	databaseClassMap.put("SmallSQL", "jdbcnav.JDBCDatabase_SmallSQL");
	databaseClassMap.put("Transbase", "jdbcnav.JDBCDatabase_Transbase");
    }

    public static String getDatabaseClassName(String driverName) {
	return (String) databaseClassMap.get(driverName);
    }

    private static TreeMap scriptGeneratorClassMap;
    static {
	scriptGeneratorClassMap = new TreeMap();
	scriptGeneratorClassMap.put("Generic", "jdbcnav.ScriptGenerator");
	scriptGeneratorClassMap.put("Oracle 8", "jdbcnav.ScriptGenerator_Oracle8");
	scriptGeneratorClassMap.put("Oracle 9", "jdbcnav.ScriptGenerator_Oracle9");
	scriptGeneratorClassMap.put("Oracle 10", "jdbcnav.ScriptGenerator_Oracle");
	scriptGeneratorClassMap.put("PostgreSQL", "jdbcnav.ScriptGenerator_PostgreSQL");
	scriptGeneratorClassMap.put("SmallSQL", "jdbcnav.ScriptGenerator_SmallSQL");
	//Don't show Transbase yet, since its script generator doesn't actually
	//do anything useful yet.
	//scriptGeneratorClassMap.put("Transbase", "jdbcnav.ScriptGenerator");
    }

    public static String getScriptGeneratorClassName(String driverName) {
	return (String) scriptGeneratorClassMap.get(driverName);
    }

    public static String[] getScriptGeneratorNames() {
	ArrayList list = new ArrayList(scriptGeneratorClassMap.keySet());
	list.remove("Generic");
	list.add(0, "Generic");
	return (String[]) list.toArray(new String[list.size()]);
    }
}
