package jdbcnav;

import java.sql.*;


public class JDBCDatabase_MySQL extends JDBCDatabase {
    public JDBCDatabase_MySQL(String name, String driver, Connection con) {
	super(name, driver, con);
    }

    /**
     * The MySQL JDBC Driver does not return anything in
     * ResultSetMetaData.getCatalogName(), which makes it impossible (without
     * parsing SQL ourselves, anyway) to support allowTable=true in
     * Database.runQuery().
     */
    protected boolean resultSetContainsTableInfo() {
	return false;
    }
}
