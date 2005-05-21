package jdbcnav;

import java.sql.*;


public class JDBCDatabase_SmallSQL extends JDBCDatabase {
    public JDBCDatabase_SmallSQL(String name, Connection con) {
	super(name, con);
    }

    /**
     * The SmallSQL JDBC Driver does not return anything in
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
	return false;
    }

    protected boolean showTableTypes() {
	return true;
    }
}
