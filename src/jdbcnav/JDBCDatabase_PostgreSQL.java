package jdbcnav;

import java.sql.*;
import jdbcnav.util.NavigatorException;


public class JDBCDatabase_PostgreSQL extends JDBCDatabase {
    public JDBCDatabase_PostgreSQL(String name, String driver, Connection con) {
	super(name, driver, con);
    }

    protected String[] getJavaTypes(String qualifiedName)
						    throws NavigatorException {
	// This is a bit icky. What I would *like* to do it use
	// PreparedStatement.getMetaData() to find out about a table's Java
	// type mapping without having to execute a statement, but the
	// PostgreSQL 8.0.0beta1 JDBC Driver (pgdev.305.jdbc3.jar) returns
	// 'null' from that method.
	// So, I create a query that is guaranteed to return no rows at all,
	// and run that. Hopefully this'll be reasonably efficient, too!

	Statement stmt = null;
	ResultSet rs = null;
	try {
	    stmt = con.createStatement();
	    rs = stmt.executeQuery(
			    "select * from " + qualifiedName + " where 1 = 2");
	    ResultSetMetaData rsmd = rs.getMetaData();
	    int columns = rsmd.getColumnCount();
	    String[] javaTypes = new String[columns];
	    for (int i = 0; i < columns; i++)
		javaTypes[i] = rsmd.getColumnClassName(i + 1);
	    return javaTypes;
	} catch (SQLException e) {
	    throw new NavigatorException(e);
	} finally {
	    if (rs != null)
		try {
		    rs.close();
		} catch (SQLException e) {}
	    if (stmt != null)
		try {
		    stmt.close();
		} catch (SQLException e) {}
	}
    }

    /**
     * For unnamed keys, PostgreSQL returns "<unnamed>" instead of null.
     */
    protected String unmangleKeyName(String name) {
	return "<unnamed>".equals(name) ? null : name;
    }

    /**
     * The PostgreSQL JDBC Driver (pgdev.305.jdbc3.jar) does not return
     * anything in ResultSetMetaData.getCatalogName(), getSchemaName(),
     * and getTableName(), which makes it impossible (without parsing SQL
     * ourselves, anyway) to support allowTable=true in Database.runQuery().
     */
    protected boolean resultSetContainsTableInfo() {
	return false;
    }

    protected String qualifyName(String name) {
	return "public." + name;
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
