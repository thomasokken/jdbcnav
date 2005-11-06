package jdbcnav;

import java.sql.*;


public class JDBCDatabase_Transbase extends JDBCDatabase {
    public JDBCDatabase_Transbase(String name, String driver, Connection con) {
	super(name, driver, con);
    }

    /**
     * The Transbase JDBC Driver does not return anything in
     * ResultSetMetaData.getCatalogName(), getSchemaName(), and getTableName(),
     * which makes it impossible (without parsing SQL ourselves, anyway) to
     * support allowTable=true in Database.runQuery().
     */
    protected boolean resultSetContainsTableInfo() {
	return false;
    }

    protected boolean showCatalogs() {
	return false;
    }

    protected boolean showSchemas() {
	return true;
    }

    protected boolean showTableTypes() {
	return true;
    }
}
