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
		int version = dbmd.getDatabaseMajorVersion();
		if (version >= 10)
		    return "Oracle 10";
		else
		    return "Oracle 8";
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
	scriptGeneratorClassMap.put("Same As Source", "jdbcnav.ScriptGenerator_SameAsSource");
	scriptGeneratorClassMap.put("Oracle 8", "jdbcnav.ScriptGenerator_Oracle8");
	scriptGeneratorClassMap.put("Oracle 10", "jdbcnav.ScriptGenerator_Oracle");
	scriptGeneratorClassMap.put("PostgreSQL", "jdbcnav.ScriptGenerator_PostgreSQL");
	scriptGeneratorClassMap.put("SmallSQL", "jdbcnav.ScriptGenerator_SmallSQL");
	scriptGeneratorClassMap.put("Transbase", "jdbcnav.ScriptGenerator");
    }

    public static String getScriptGeneratorClassName(String driverName) {
	return (String) scriptGeneratorClassMap.get(driverName);
    }

    public static String[] getScriptGeneratorNames() {
	ArrayList list = new ArrayList(scriptGeneratorClassMap.keySet());
	list.remove("Generic");
	list.remove("Same As Source");
	list.add(0, "Generic");
	list.add(1, "Same As Source");
	return (String[]) list.toArray(new String[list.size()]);
    }
}
