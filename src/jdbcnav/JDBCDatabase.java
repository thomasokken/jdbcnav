package jdbcnav;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import javax.swing.*;
import org.mozilla.javascript.*;

import jdbcnav.javascript.*;
import jdbcnav.model.*;
import jdbcnav.util.*;


public class JDBCDatabase extends BasicDatabase {

    private String name;
    private String internalDriverName;
    protected Connection con;
    private ArrayList editedTables = new ArrayList();


    public static void open(Database.OpenCallback opencb) {
	LoginDialog.activate(opencb);
    }

    public JDBCDatabase(String name, String driver, Connection con) {
	this.name = name;
	this.internalDriverName = driver;
	this.con = con;
    }

    private static JDBCDatabase create(String driver, String name,
				       Connection con) {
	String internalDriverName =
	    InternalDriverMap.getDriverName(driver, con);
	String className =
	    InternalDriverMap.getDatabaseClassName(internalDriverName);

	try {
	    Class klass = Class.forName(className);
	    Constructor cnstr = klass.getConstructor(
		new Class[] { String.class, String.class, Connection.class });
	    return (JDBCDatabase) cnstr.newInstance(
		new Object[] { name, internalDriverName, con });
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	    // Should never happen; we're loading a class that is part
	    // of JDBCNavigator itself
	} catch (NoSuchMethodException e) {
	    e.printStackTrace();
	    // Should never happen; we're loading a class that is part
	    // of JDBCNavigator itself, and if we've done our homework,
	    // it will have a public constructor that takes a String and
	    // a JDBC Connection
	} catch (IllegalAccessException e) {
	    e.printStackTrace();
	    // Yawn. Should never happen. We wrote the class ourselves
	    // and of course we made sure that the constructor is public.
	} catch (InstantiationException e) {
	    e.printStackTrace();
	    // Yawn. Should never happen. We wrote the class ourselves
	    // and of course we made sure that the class is not abstract.
	} catch (InvocationTargetException e) {
	    e.printStackTrace();
	    // Yawn. Should never happen. We wrote the class ourselves
	    // and of course we made sure that the constructor throws no
	    // Exceptions.
	}

	// *should* never get here
	return null;
    }

    public void close() {
	try {
	    con.close();
	} catch (SQLException e) {}
    }

    public String getName() {
	return name;
    }

    public final String getInternalDriverName() {
	return internalDriverName;
    }

    public String about() throws NavigatorException {
	try {
	    DatabaseMetaData dbmd = con.getMetaData();
	    StringBuffer buf = new StringBuffer();
	    buf.append("Database product name: ");
	    buf.append(dbmd.getDatabaseProductName());
	    buf.append("\nDatabase product version: ");
	    buf.append(dbmd.getDatabaseProductVersion());
	    buf.append("\nJDBC driver name: ");
	    buf.append(dbmd.getDriverName());
	    buf.append("\nJDBC driver version: ");
	    buf.append(dbmd.getDriverVersion());
	    buf.append("\nJDBC Navigator Internal Driver: ");
	    buf.append(getInternalDriverName());
	    return buf.toString();
	} catch (SQLException e) {
	    throw new NavigatorException(e);
	}
    }

    public void setBrowser(BrowserFrame browser) {
	this.browser = browser;
    }

    public BrowserFrame getBrowser() {
	return browser;
    }

    public boolean needsCommit() {
	for (Iterator iter = editedTables.iterator(); iter.hasNext();) {
	    Table table = (Table) iter.next();
	    if (table.needsCommit())
		return true;
	}
	return false;
    }

    public Collection getDirtyTables() {
	ArrayList dirty = new ArrayList();
	for (Iterator iter = editedTables.iterator(); iter.hasNext();) {
	    Table table = (Table) iter.next();
	    if (table.needsCommit())
		dirty.add(table);
	}
	Collections.sort(dirty);
	return dirty;
    }

    protected boolean shouldMoveToOrphanage(Table table) {
	for (Iterator iter = editedTables.iterator(); iter.hasNext();) {
	    Table t = (Table) iter.next();
	    if (table == t) {
		iter.remove();
		return table.needsCommit();
	    }
	}
	return false;
    }

    public void commitTables(Collection tables) throws NavigatorException {
	boolean autoCommit = true;
	NavigatorException we = null;
	DiffCallback dcb = new DiffCallback();

	// Pause any in-progress table loads before committing; without
	// this precaution, MultiTableDiff.diff() could try to insert
	// spurious rows (this will happen if rows are loaded into a table
	// between the moment that the 'original' and the 'current' snapshots
	// are made (also note that the moment the OriginalTable object is
	// constructed is *not* the moment the snapshot is made; that object
	// is merely a vehicle for a reference to the model's 'original'
	// array)).

	ArrayList pausedTables = new ArrayList();
	for (Iterator iter = tables.iterator(); iter.hasNext();) {
	    Table table = (Table) iter.next();
	    ResultSetTableModel model = table.getModel();
	    if (model.getState() == Data.LOADING) {
		model.setState(Data.PAUSED);
		pausedTables.add(table);
	    }
	}

	try {
	    autoCommit = con.getAutoCommit();
	    con.setAutoCommit(false);
	    if (tables.size() == 1) {
		Table table = (Table) tables.iterator().next();
		table.getModel().commit(dcb);
	    } else {
		ArrayList oldtables = new ArrayList();
		for (Iterator iter = tables.iterator(); iter.hasNext();) {
		    Table t = (Table) iter.next();
		    oldtables.add(new OriginalTable(t));
		}
		MultiTableDiff.diff(dcb, oldtables, tables, false);
	    }
	    con.commit();
	} catch (SQLException e) {
	    we = new NavigatorException(e);
	} catch (Exception e) {
	    we = new NavigatorException("An unexpected exception occurred!", e);
	}

	dcb.cleanup();
	for (Iterator iter = pausedTables.iterator(); iter.hasNext();) {
	    Table table = (Table) iter.next();
	    ResultSetTableModel model = table.getModel();
	    model.setState(Data.LOADING);
	}

	if (we != null) {
	    try {
		con.rollback();
	    } catch (SQLException e) {}
	    if (autoCommit)
		try {
		    con.setAutoCommit(true);
		} catch (SQLException e) {}
	    throw new NavigatorException("Commit failed!", we);
	}

	if (autoCommit)
	    try {
		con.setAutoCommit(true);
	    } catch (SQLException e) {}

	// All went well; tell all the table models to discard their 'undo'
	// state.
	for (Iterator iter = tables.iterator(); iter.hasNext();) {
	    Table t = (Table) iter.next();
	    t.getModel().postCommit();
	}
    }

    private class OriginalTable extends BasicTable {
	private Data data;

	public OriginalTable(Table table) {
	    super(table);
	    // If the following line throws a NullPointerException, I screwed
	    // up somewhere. The fact that we're here should imply that
	    // 'table' was edited, so it should have a model. Note that I'm
	    // NOT calling createModel(): if there is no model, I *want* to
	    // get an exception; creating a model on the fly would only mask
	    // a pretty serious error.
	    data = table.getModel().getOriginalData();
	}

	public ResultSetTableModel createModel() throws NavigatorException {
	    throw new NavigatorException("OriginalTable can't be edited!");
	}

	public boolean isEditable() {
	    return false;
	}

	public Database getDatabase() {
	    return JDBCDatabase.this;
	}

	public Data getData(boolean async) {
	    return data;
	}
    }

    protected class PartialTable extends BasicTable {
	private String query;
	private Data data;

	public PartialTable(String query, Table t, Data data) {
	    // We leave some fields unpopulated -- they should never be
	    // needed because all we're going to be used for is to be
	    // displayed in TableFrame, and Database.commitTables().

	    catalog = t.getCatalog();
	    schema = t.getSchema();
	    name = t.getName();
	    qualifiedName = t.getQualifiedName();
	    type = t.getType();
	    remarks = "This view was generated by the query:\n" + query;

	    int columns = data.getColumnCount();
	    int tColumns = t.getColumnCount();

	    int[] columnMap = new int[columns];
	    int[] reverseColumnMap = new int[tColumns];
	    for (int i = 0; i < tColumns; i++)
		reverseColumnMap[i] = -1;

	    columnNames = new String[columns];
	    dbTypes = new String[columns];
	    columnSizes = new Integer[columns];
	    columnScales = new Integer[columns];
	    isNullable = new String[columns];
	    sqlTypes = new int[columns];
	    javaTypes = new String[columns];

	    for (int i = 0; i < columns; i++) {
		String colname = data.getColumnName(i);
		for (int j = 0; j < tColumns; j++) {
		    String tColname = t.getColumnNames()[j];
		    if (colname.equalsIgnoreCase(tColname)) {
			columnMap[i] = j;
			reverseColumnMap[j] = i;
			columnNames[i] = t.getColumnNames()[j];
			dbTypes[i] = t.getDbTypes()[j];
			columnSizes[i] = t.getColumnSizes()[j];
			columnScales[i] = t.getColumnScales()[j];
			isNullable[i] = t.getIsNullable()[j];
			sqlTypes[i] = t.getSqlTypes()[j];
			javaTypes[i] = t.getJavaTypes()[j];
		    }
		}
	    }

	    PrimaryKey tpk = t.getPrimaryKey();
	    if (tpk != null) {
		BasicPrimaryKey bpk = new BasicPrimaryKey();
		bpk.setName("fake_key");
		int pklength = tpk.getColumnCount();
		String[] pkcol = new String[pklength];
		for (int i = 0; i < pklength; i++)
		    pkcol[i] = tpk.getColumnName(i);
		bpk.setColumns(pkcol);
		pk = bpk;
	    }

	    fks = new ForeignKey[0];
	    rks = new ForeignKey[0];
	    indexes = new Index[0];

	    this.data = data;
	    this.query = query;
	}

	public boolean isEditable() {
	    return true;
	}

	public boolean isUpdatableQueryResult() {
	    return true;
	}

	public Database getDatabase() {
	    return JDBCDatabase.this;
	}

	public Data getData(boolean async) throws NavigatorException {
	    // The first time getData() is called, we return the Data
	    // object that was passed to our constructor; all following
	    // times, we re-run the query.
	    if (data != null) {
		Data ret = data;
		data = null;
		return ret;
	    } else
		return (Data) runQuery(query, async, false);
	}
    }


    private class DiffCallback implements TableChangeHandler {
	private TreeMap insertStatements = new TreeMap();
	private TreeMap deleteStatements = new TreeMap();

	public DiffCallback() {
	    //
	}

	public void insertRow(Table table, Object[] row)
						    throws NavigatorException {
	    PreparedStatement insertStatement =
			    (PreparedStatement) insertStatements.get(table);
	    String[] names = table.getColumnNames();
	    int columns = names.length;
	    if (insertStatement == null) {
		StringBuffer buf = new StringBuffer();
		buf.append("insert into ");
		buf.append(table.getQualifiedName());
		buf.append("(");
		for (int i = 0; i < columns; i++) {
		    if (i > 0)
			buf.append(", ");
		    buf.append(names[i]);
		}
		buf.append(") values (");
		for (int i = 0; i < columns; i++) {
		    if (i == 0)
			buf.append("?");
		    else
			buf.append(", ?");
		}
		buf.append(")");
		try {
		    insertStatement = con.prepareStatement(buf.toString());
		    insertStatements.put(table, insertStatement);
		} catch (SQLException e) {
		    throw new NavigatorException(e);
		}
	    }
	    try {
		for (int i = 0; i < columns; i++)
		    setObject(insertStatement, i + 1, i, row[i], table);
		insertStatement.executeUpdate();
	    } catch (SQLException e) {
		throw new NavigatorException(e);
	    }
	}

	public void deleteRow(Table table, Object[] key)
						    throws NavigatorException {
	    String[] names = table.getColumnNames();
	    int[] keyIndexes = table.getPKColumns();
	    if (needsIsNull() && table.getPrimaryKey() == null) {
		StringBuffer buf = new StringBuffer();
		buf.append("delete from ");
		buf.append(table.getQualifiedName());
		buf.append(" where ");
		for (int i = 0; i < key.length; i++) {
		    if (i > 0)
			buf.append(" and ");
		    buf.append(names[keyIndexes[i]]);
		    if (key[i] == null)
			buf.append(" is null");
		    else
			buf.append(" = ?");
		}
		PreparedStatement s = null;
		try {
		    s = con.prepareStatement(buf.toString());
		    int p = 1;
		    for (int i = 0; i < key.length; i++)
			if (key[i] != null)
			    setObject(s, p++, i, key[i], table);
		    s.executeUpdate();
		} catch (SQLException e) {
		    throw new NavigatorException(e);
		} finally {
		    if (s != null)
			try {
			    s.close();
			} catch (SQLException e) {}
		}
	    } else {
		PreparedStatement deleteStatement =
			(PreparedStatement) deleteStatements.get(table);
		if (deleteStatement == null) {
		    StringBuffer buf = new StringBuffer();
		    buf.append("delete from ");
		    buf.append(table.getQualifiedName());
		    buf.append(" where ");
		    for (int i = 0; i < key.length; i++) {
			if (i > 0)
			    buf.append(" and ");
			buf.append(names[keyIndexes[i]]);
			buf.append(" = ?");
		    }
		    try {
			deleteStatement = con.prepareStatement(buf.toString());
			deleteStatements.put(table, deleteStatement);
		    } catch (SQLException e) {
			throw new NavigatorException(e);
		    }
		}
		try {
		    for (int i = 0; i < key.length; i++)
			setObject(deleteStatement, i + 1, i, key[i], table);
		    deleteStatement.executeUpdate();
		} catch (SQLException e) {
		    throw new NavigatorException(e);
		}
	    }
	}

	public void updateRow(Table table, Object[] oldRow, Object[] newRow)
						    throws NavigatorException {
	    String[] names = table.getColumnNames();
	    int[] keyIndexes = table.getPKColumns();
	    int columns = names.length;
	    StringBuffer buf = new StringBuffer();
	    buf.append("update ");
	    buf.append(table.getQualifiedName());
	    buf.append(" set ");
	    boolean first = true;
	    for (int i = 0; i < columns; i++) {
		if (oldRow[i] == null ? newRow[i] != null
				    : !oldRow[i].equals(newRow[i])) {
		    if (first)
			first = false;
		    else
			buf.append(", ");
		    buf.append(names[i]);
		    buf.append(" = ?");
		}
	    }
	    buf.append(" where ");
	    for (int i = 0; i < keyIndexes.length; i++) {
		if (i > 0)
		    buf.append(" and ");
		buf.append(names[keyIndexes[i]]);

		// I'm doing the hack to work around the Oracle bug in '= ?'
		// with null values in where clauses here. Unlike in deleteRow,
		// the hack is unconditional here, since caching update
		// statements is a bit trickier than I am in the mood for right
		// now. (The problem is that, unlike in insertRow and
		// deleteRow, you don't want to specify the same columns for
		// each updated row... For example, if only one value was
		// changed in a table row that contains 1000 values, you don't
		// want to pump the 999 unchanged values across the wire.
		// So, instead, I waste time building new statements for each
		// update. This *might* actually perform *worse*...)

		if (oldRow[keyIndexes[i]] == null)
		    buf.append(" is null");
		else
		    buf.append(" = ?");
	    }
	    PreparedStatement s = null;
	    try {
		s = con.prepareStatement(buf.toString());
		int p = 1;
		for (int i = 0; i < columns; i++)
		    if (oldRow[i] == null ? newRow[i] != null
					: !oldRow[i].equals(newRow[i]))
			setObject(s, p++, i, newRow[i], table);
		for (int i = 0; i < keyIndexes.length; i++)
		    if (oldRow[keyIndexes[i]] != null)
			setObject(s, p++, keyIndexes[i],
					    oldRow[keyIndexes[i]], table);
		s.executeUpdate();
	    } catch (SQLException e) {
		throw new NavigatorException(e);
	    } finally {
		if (s != null)
		    try {
			s.close();
		    } catch (SQLException e) {}
	    }
	}

	public boolean continueAfterError() {
	    return false;
	}

	public void cleanup() {
	    Iterator iter = insertStatements.values().iterator();
	    while (iter.hasNext()) {
		PreparedStatement pstmt = (PreparedStatement) iter.next();
		try {
		    pstmt.close();
		} catch (SQLException e) {}
	    }
	    iter = deleteStatements.values().iterator();
	    while (iter.hasNext()) {
		PreparedStatement pstmt = (PreparedStatement) iter.next();
		try {
		    pstmt.close();
		} catch (SQLException e) {}
	    }
	}
    }


    protected void duplicate() {
	try {
	    FileDatabase fd = new FileDatabase(getSelectedTables());
	    BrowserFrame bf = new BrowserFrame(fd);
	    bf.showStaggered();
	} catch (NavigatorException e) {
	    MessageBox.show(e);
	}
    }

    protected Collection getTables() throws NavigatorException {
	ArrayList tables = new ArrayList();
	ResultSet rs = null;
	try {
	    DatabaseMetaData dbmd = con.getMetaData();
	    rs = dbmd.getTables(null, null, null, null);
	    while (rs.next()) {
		TableSpec ts = new TableSpec();
		ts.catalog = rs.getString("TABLE_CAT");
		ts.schema = rs.getString("TABLE_SCHEM");
		ts.type = rs.getString("TABLE_TYPE");
		ts.name = rs.getString("TABLE_NAME");
		tables.add(ts);
	    }
	} catch (SQLException e) {
	    throw new NavigatorException(e);
	} finally {
	    if (rs != null)
		try {
		    rs.close();
		} catch (SQLException e) {}
	}
	return tables;
    }

    public Table loadTable(String qualifiedName) throws NavigatorException {
	return newJDBCTable(qualifiedName);
    }

    private PrimaryKey getPrimaryKey(String qname) throws NavigatorException {
	String[] parts = parseQualifiedName(qname);
	ResultSet rs = null;
        try {
	    DatabaseMetaData dbmd = con.getMetaData();
	    rs = dbmd.getPrimaryKeys(parts[0], parts[1], parts[2]);
	    String keyName = null;
	    ArrayList colName = new ArrayList();
	    while (rs.next()) {
		String n = rs.getString("COLUMN_NAME");
		short i = rs.getShort("KEY_SEQ");
		keyName = rs.getString("PK_NAME");
		for (int j = colName.size(); j < i; j++)
		    colName.add(null);
		colName.set(i - 1, n);
	    }
	    boolean first = true;
	    for (int i = colName.size() - 1; i >= 0; i--)
		if (colName.get(i) == null)
		    colName.remove(i);
	    if (colName.size() != 0) {
		BasicPrimaryKey pk = new BasicPrimaryKey();
		pk.setName(keyName);
		pk.setColumns((String[]) colName.toArray(
					new String[colName.size()]));
		return pk;
	    } else
		return null;
	} catch (SQLException e) {
	    throw new NavigatorException(e);
	} finally {
	    if (rs != null)
		try {
		    rs.close();
		} catch (SQLException e) {}
	}
    }

    protected Index[] getIndexes(Table table) throws NavigatorException {
	PrimaryKey pk = table.getPrimaryKey();
	ResultSet rs = null;
	try {
	    DatabaseMetaData dbmd = con.getMetaData();
	    rs = dbmd.getIndexInfo(table.getCatalog(),
				table.getSchema(),
				table.getName(),
				false,
				false);
	    String prevName = null;
	    ArrayList indexes = new ArrayList();
	    ArrayList columns = null;
	    BasicIndex index = null;
	    while (rs.next()) {
		String name = rs.getString("INDEX_NAME");
		if (name == null || pk != null && name.equals(pk.getName()))
		    continue;
		if (!name.equals(prevName)) {
		    if (index != null) {
			index.setColumns((String[]) columns.toArray(
								new String[0]));
			indexes.add(index);
		    }
		    index = new BasicIndex();
		    columns = new ArrayList();
		    index.setName(name);
		    index.setUnique(!rs.getBoolean("NON_UNIQUE"));
		}
		columns.add(rs.getString("COLUMN_NAME"));
		prevName = name;
	    }
	    if (index != null) {
		index.setColumns((String[]) columns.toArray(new String[0]));
		indexes.add(index);
	    }
	    return (Index[]) indexes.toArray(new Index[0]);
	} catch (SQLException e) {
	    throw new NavigatorException(e);
	} finally {
	    if (rs != null)
		try {
		    rs.close();
		} catch (SQLException e) {}
	}
    }

    private ForeignKey[] getFK(String qualifiedName, boolean imported)
						    throws NavigatorException {
	String[] parts = parseQualifiedName(qualifiedName);
	String catalog = parts[0];
	String schema = parts[1];
	String name = parts[2];
	ResultSet rs = null;
	ArrayList results = new ArrayList();
	try {
	    DatabaseMetaData dbmd = con.getMetaData();
	    if (imported)
		rs = dbmd.getImportedKeys(catalog, schema, name);
	    else
		rs = dbmd.getExportedKeys(catalog, schema, name);
	    if (rs == null)
		return new ForeignKey[0];

	    // Now for some fun. There is some serious brain damage in the JDBC
	    // spec for DatabaseMetaData.getImportedKeys() and
	    // getExportedKeys().
	    // The problem is that the result sets are sorted by the 'other'
	    // table's catalog, schema, name, and key sequence -- and this
	    // means that if two foreign keys reference the same table, their
	    // components will get shuffled together! (You will get component 1
	    // of key 1, then component 1 of key 2, then component 2 of key 1,
	    // then component 2 of key 2...)
	    // There is no sure-fire way to fix or even detect this problem.
	    // However, as long as foreign keys have unique names (and I expect
	    // most RDBMSes will assign a unique name if the user does not
	    // specify one) we can re-sort the result set, this time by
	    // catalog, schema, name, key name, and key sequence, and that will
	    // fix the problem. If you have a DB with no, or non-unique,
	    // foreign key names, you lose. Sorry!

	    while (rs.next()) {
		FKResultSetRow rsr = new FKResultSetRow();
		if (imported) {
		    rsr.thatCatalog = rs.getString("PKTABLE_CAT");
		    rsr.thatSchema = rs.getString("PKTABLE_SCHEM");
		    rsr.thatName = rs.getString("PKTABLE_NAME");
		    rsr.thatKeyName = unmangleKeyName(
			    rs.getString("PK_NAME"));
		    rsr.thatColumn = rs.getString("PKCOLUMN_NAME");
		    rsr.thisKeyName = unmangleKeyName(
			    rs.getString("FK_NAME"));
		    rsr.thisColumn = rs.getString("FKCOLUMN_NAME");
		} else {
		    rsr.thatCatalog = rs.getString("FKTABLE_CAT");
		    rsr.thatSchema = rs.getString("FKTABLE_SCHEM");
		    rsr.thatName = rs.getString("FKTABLE_NAME");
		    rsr.thatKeyName = unmangleKeyName(
			    rs.getString("FK_NAME"));
		    rsr.thatColumn = rs.getString("FKCOLUMN_NAME");
		    rsr.thisKeyName = unmangleKeyName(
			    rs.getString("PK_NAME"));
		    rsr.thisColumn = rs.getString("PKCOLUMN_NAME");
		}
		rsr.updateRule = rs.getShort("UPDATE_RULE");
		rsr.deleteRule = rs.getShort("DELETE_RULE");
		rsr.keySeq = rs.getShort("KEY_SEQ");
		results.add(rsr);
	    }
	} catch (SQLException e) {
	    throw new NavigatorException(e);
	} finally {
	    if (rs != null)
		try {
		    rs.close();
		} catch (SQLException e) {}
	}

	if (imported)
	    Collections.sort(results, FKResultSetRowComparator.forImport);
	else
	    Collections.sort(results, FKResultSetRowComparator.forExport);

	ArrayList fks = new ArrayList();
	ArrayList currFk = new ArrayList();

	short prevKeySeq = 0;
	Iterator iter = results.iterator();
	FKResultSetRow rsr = null;

	while (true) {
	    boolean done = !iter.hasNext();
	    FKResultSetRow nextRsr = done ? null : (FKResultSetRow) iter.next();
	    boolean flushKey = done;

	    if (!done) {
		if (nextRsr.keySeq != prevKeySeq + 1)
		    flushKey = true;
	    }

	    if (flushKey && currFk.size() > 0) {
		BasicForeignKey fk = new BasicForeignKey();
		int keySize = currFk.size();
		String[] thisColumns = new String[keySize];
		String[] thatColumns = new String[keySize];
		for (int i = 0; i < currFk.size(); i++) {
		    FKPart fkp = (FKPart) currFk.get(i);
		    thisColumns[i] = fkp.thisColumn;
		    thatColumns[i] = fkp.thatColumn;
		}
		fk.setThisColumns(thisColumns);
		fk.setThatColumns(thatColumns);
		currFk.clear();
		fk.setThisKeyName(rsr.thisKeyName);
		fk.setThatKeyName(rsr.thatKeyName);
		fk.setThatCatalog(rsr.thatCatalog);
		fk.setThatSchema(rsr.thatSchema);
		fk.setThatName(rsr.thatName);
		fk.setThatQualifiedName(makeQualifiedName(rsr.thatCatalog,
							  rsr.thatSchema,
							  rsr.thatName));
		fk.setUpdateRule(ruleString(rsr.updateRule));
		fk.setDeleteRule(ruleString(rsr.deleteRule));
		fks.add(fk);
	    }

	    if (done)
		break;
	    else {
		rsr = nextRsr;
		currFk.add(new FKPart(rsr.thatColumn, rsr.thisColumn));
		prevKeySeq = rsr.keySeq;
	    }
	}

	int n = fks.size();
	if (n == 0)
	    return new ForeignKey[0];
	else
	    return (ForeignKey[]) fks.toArray(new ForeignKey[n]);
    }

    private static String ruleString(short rule) {
	switch (rule) {
	    case DatabaseMetaData.importedKeyNoAction:
		return "no action";
	    case DatabaseMetaData.importedKeyCascade:
		return "cascade";
	    case DatabaseMetaData.importedKeySetNull:
		return "set null";
	    case DatabaseMetaData.importedKeySetDefault:
		return "set default";
	    case DatabaseMetaData.importedKeyRestrict:
		return "restrict";
	    default:
		return "unknown";
	}
    }

    private static class FKResultSetRow {
	String thatCatalog;
	String thatSchema;
	String thatName;
	String thatKeyName;
	String thatColumn;
	String thisKeyName;
	String thisColumn;
	short updateRule;
	short deleteRule;
	short keySeq;
    }

    private static class FKResultSetRowComparator implements Comparator {
	public static final FKResultSetRowComparator forImport
				    = new FKResultSetRowComparator(true);
	public static final FKResultSetRowComparator forExport
				    = new FKResultSetRowComparator(false);
	private boolean impord;
	private FKResultSetRowComparator(boolean impord) {
	    this.impord = impord;
	}
	public int compare(Object a, Object b) {
	    FKResultSetRow ra = (FKResultSetRow) a;
	    FKResultSetRow rb = (FKResultSetRow) b;
	    int res = strcmp(ra.thatCatalog, rb.thatCatalog);
	    if (res != 0)
		return res;
	    res = strcmp(ra.thatSchema, rb.thatSchema);
	    if (res != 0)
		return res;
	    res = ra.thatName.compareTo(rb.thatName);
	    if (res != 0)
		return res;
	    if (impord)
		res = strcmp(ra.thisKeyName, rb.thisKeyName);
	    else
		res = strcmp(ra.thatKeyName, rb.thatKeyName);
	    if (res != 0)
		return res;
	    return ra.keySeq - rb.keySeq;
	}
	private static int strcmp(String a, String b) {
	    return a == null ? b == null ? 0 : 1
			     : b == null ? -1 : a.compareTo(b);
	}
    }

    private static class FKPart {
	public String thatColumn;
	public String thisColumn;

	public FKPart(String thatColumn, String thisColumn) {
	    this.thatColumn = thatColumn;
	    this.thisColumn = thisColumn;
	}
    }


    /**
     * This method is provided so that drivers that need to perform special
     * contortions to set values can override it (e.g., Oracle needs to do
     * streaming to get around size limits on LONG, LONG RAW, and [BC]LOB).
     * It is only called for non-key objects, the assumption being that columns
     * that are part of a primary key can always be set the standard way.
     */
    protected void setObject(PreparedStatement stmt, int index, int
			     dbtable_col, Object o, Table table)
							throws SQLException {
	int type = table.getSqlTypes()[dbtable_col];
	if (type == Types.OTHER)
	    stmt.setObject(index, o);
	else
	    stmt.setObject(index, o, table.getSqlTypes()[dbtable_col]);
    }

    /**
     * This method is provided so that drivers that need to subclass
     * JDBCTable can provide their own factory, which will be used by
     * JDBCDatabase also.
     */
    protected JDBCTable newJDBCTable(String qualifiedName)
						    throws NavigatorException {
	return new JDBCTable(qualifiedName);
    }

    /**
     * This method is provided so that drivers that need to subclass
     * PartialTable can provide their own factory, which will be used by
     * JDBCDatabase also.
     */
    protected PartialTable newPartialTable(String q, Table t, Data d)
						    throws NavigatorException {
	return new PartialTable(q, t, d);
    }

    public Object runQuery(String query, boolean asynchronous,
			   boolean allowTable) throws NavigatorException {

	Table table = null;

	if (allowTable && !resultSetContainsTableInfo()) {
	    // Great. We're dealing with a JDBC driver that doesn't implement
	    // ResultSetMetaData.getCatalogName(), getSchemaName(), and
	    // getTableName().
	    // If we're going to return the query result as a PartialTable,
	    // rather than a base Data (or BackgroundLoadData, whatever),
	    // we need to know which table columns the result set columns
	    // correspond to. Without help from the JDBC driver, the only
	    // way to find that out is by parsing the SQL ourselves.
	    // Since I don't feel like implementing a full-blown SQL parser
	    // here (not right now, anyway, plenty TODO already!), I restrict
	    // myself to two extremely useful special cases:
	    //
	    //     select * from <table> where [...]
	    //     select foo.* from [...] <table> foo [...] where [...]

	    // First of all, set 'allowTable' to false; this disables the code
	    // later on that attempts to use the aforementioned
	    // ResultSetMetaData methods...
	    allowTable = false;

	    StringTokenizer tok = new StringTokenizer(query, " \t\n\r,", true);
	    int state = 0;
	    String identifier = null;
	    String name = null;
	    boolean success = false;
	    scanner:
	    while (tok.hasMoreTokens()) {
		String t = tok.nextToken();
		if (t.equals(" ") || t.equals("\t")
			|| t.equals("\n") || t.equals("\r"))
		    continue;
		switch (state) {
		    case 0:
			// Looking for "select"
			if (!t.equalsIgnoreCase("select"))
			    break scanner;
			state = 1;
			break;
		    case 1:
			// Looking for "*" or "identifier.*"
			if (t.equals("*"))
			    identifier = null;
			else if (t.length() > 2 && t.endsWith(".*"))
			    identifier = t.substring(0, t.length() - 2);
			else
			    break scanner;
			state = 2;
			break;
		    case 2:
			// Looking for "from"
			if (!t.equalsIgnoreCase("from"))
			    break scanner;
			state = 3;
			break;
		    case 3:
			// Looking for table name
			if (t.equals(",") || t.equalsIgnoreCase("where"))
			    break scanner;
			name = t;
			state = 4;
			break;
		    case 4:
			// After table name
			if (identifier == null) {
			    // We have a "select * from", so the first word
			    // after "from" was the table name, and the first
			    // word after the table name should be "where";
			    // else we have a construct we can't handle.
			    if (t.equalsIgnoreCase("where"))
				success = true;
			    break scanner;
			} else {
			    // We have a "select foo.* from", and the previous
			    // word was a table name. If the current word
			    // matches "foo", we've found our table and are
			    // done; otherwise, we move on to look for a comma.
			    if (t.equalsIgnoreCase("where"))
				break scanner;
			    if (t.equalsIgnoreCase(identifier)) {
				success = true;
				break scanner;
			    } else {
				state = 5;
				break;
			    }
			}
		    case 5:
			// Waiting for comma
			if (!t.equals(","))
			    break scanner;
			state = 3;
			break;
		}
	    }
	    if (success) {
		// If the table name looks to be unqualified, we try to
		// qualify it ourselves (in some DB-specific manner)
		if (name.indexOf('.') == -1)
		    name = qualifyName(name);
		try {
		    table = getTable(name);
		} catch (NavigatorException e) {
		    // Well, we tried. This is not a fatal error; the user
		    // will just have to make do with a non-editable
		    // QueryResultFrame.
		}
	    }
	}

	Statement s = null;
	ResultSet rs = null;
	try {
	    s = con.createStatement();
	    rs = s.executeQuery(query);

	    ResultSetMetaData rsmd = rs.getMetaData();
	    int columns = rsmd.getColumnCount();
	    String[] columnNames = new String[columns];
	    Class[] columnClasses = new Class[columns];
	    boolean isClob[] = new boolean[columns];

	    String catalog = null;
	    String schema = null;
	    String name = null;

	    for (int i = 0; i < columns; i++) {
		columnNames[i] = rsmd.getColumnName(i + 1);
		try {
		    Class c = Class.forName(rsmd.getColumnClassName(i + 1));
		    columnClasses[i] = c;
		    isClob[i] = Clob.class.isAssignableFrom(c);
		} catch (ClassNotFoundException e) {
		    columnClasses[i] = null;
		} catch (Exception e) {
		    // TODO: This is just a workaround for PostgreSQL, which
		    // throws an exception in this method.
		    columnClasses[i] = Object.class;
		}
		if (allowTable) {
		    String catalog2 = rsmd.getCatalogName(i + 1);
		    String schema2 = rsmd.getSchemaName(i + 1);
		    String name2 = rsmd.getTableName(i + 1);
		    if (i == 0) {
			catalog = catalog2;
			schema = schema2;
			name = name2;
		    } else {
			if (!MiscUtils.strEq(catalog, catalog2)
				|| !MiscUtils.strEq(schema, schema2)
				|| !MiscUtils.strEq(name, name2))
			    allowTable = false;
		    }
		}
	    }

	    if (allowTable) {
		String qname = makeQualifiedName(catalog, schema, name);
		table = getTable(qname);
		if (table != null) {
		    // We check if all the primary key components are present
		    // in the result set; if there is no primary key, we make
		    // sure that *all* columns are present.
		    // The idea is to prevent the occurrence that someone
		    // deletes a row from a partial view, and inadvertently
		    // deletes many more rows from the actual table. By
		    // ensuring the complete primary key is matches, we
		    // guarantee that deleting one row from a partial view
		    // deletes exactly that one row from the actual table.
		    PrimaryKey pk = table.getPrimaryKey();
		    int pkSize;
		    String[] pkColumns;
		    if (pk != null) {
			pkSize = pk.getColumnCount();
			pkColumns = new String[pkSize];
			for (int i = 0; i < pkSize; i++)
			    pkColumns[i] = pk.getColumnName(i);
		    } else {
			pkSize = table.getColumnCount();
			pkColumns = table.getColumnNames();
		    }
		    for (int i = 0; i < pkSize; i++) {
			String n = pkColumns[i];
			boolean found = false;
			for (int j = 0; j < columnNames.length; j++)
			    if (columnNames[j].equalsIgnoreCase(n)) {
				found = true;
				break;
			    }
			if (!found) {
			    // Primary key component missing in ResultSet;
			    // don't allow table in this case
			    table = null;
			    break;
			}
		    }
		}
	    }

	    if (asynchronous) {
		BackgroundLoadData bld = new BackgroundLoadData(columnNames,
								columnClasses);
		Thread ldr = new Thread(
			    new BackgroundLoader(bld, isClob, s, rs));
		ldr.setPriority(Thread.MIN_PRIORITY);
		ldr.setDaemon(true);
		ldr.start();

		// Prevent 'finally' clause from closing Statement & ResultSet
		s = null;
		rs = null;
		if (table != null)
		    return newPartialTable(query, table, bld);
		else
		    return bld;
	    }

	    BasicData bd = new BasicData();
	    bd.setColumnNames(columnNames);
	    bd.setColumnClasses(columnClasses);

	    ArrayList data = new ArrayList();
	    char[] cbuf = new char[4096];
	    while (rs.next()) {
		Object[] row = new Object[columns];
		for (int i = 0; i < columns; i++) {
		    Object o = rs.getObject(i + 1);
		    if (isClob[i]) {
			// TODO -- why special case handling for Clobs?
			// Why not treat them exactly like Blobs, i.e.,
			// put the Clob object in the Data object, and
			// leave it to the table cell editor to load the
			// value?
			Clob clob = (Clob) o;
			Reader r = clob.getCharacterStream();
			int n;
			StringBuffer sbuf = new StringBuffer();
			try {
			    while ((n = r.read(cbuf)) != -1)
				sbuf.append(cbuf, 0, n);
			} catch (IOException e) {
			    throw new NavigatorException(e);
			} finally {
			    try {
				r.close();
			    } catch (IOException e) {}
			}
			row[i] = sbuf.toString();
		    } else
			row[i] = o;
		}
		data.add(row);
	    }

	    bd.setData(data);

	    if (table != null)
		return newPartialTable(query, table, bd);
	    else
		return bd;
	} catch (SQLException e) {
	    throw new NavigatorException(e);
	} finally {
	    if (rs != null)
		try {
		    rs.close();
		} catch (SQLException e) {}
	    if (s != null)
		try {
		    s.close();
		} catch (SQLException e) {}
	}
    }

    public int runUpdate(String query) throws NavigatorException {
	Statement s = null;
	int count;
	try {
	    s = con.createStatement();
	    return s.executeUpdate(query);
	} catch (SQLException e) {
	    throw new NavigatorException(e);
	} finally {
	    if (s != null)
		try {
		    s.close();
		} catch (SQLException e) {}
	}
    }

    public Scriptable createStatement() throws NavigatorException {
	try {
	    Statement stmt = con.createStatement();
	    return new JavaScriptStatement(stmt);
	} catch (SQLException e) {
	    throw new NavigatorException(e);
	}
    }

    public Scriptable prepareStatement(String statement)
						    throws NavigatorException {
	try {
	    PreparedStatement pstmt = con.prepareStatement(statement);
	    return new JavaScriptPreparedStatement(pstmt);
	} catch (SQLException e) {
	    throw new NavigatorException(e);
	}
    }

    public Scriptable prepareCall(String call) throws NavigatorException {
	try {
	    CallableStatement cstmt = con.prepareCall(call);
	    return new JavaScriptCallableStatement(cstmt);
	} catch (SQLException e) {
	    throw new NavigatorException(e);
	}
    }

    public File getFile() {
	return null;
    }

    public boolean save(File file) throws NavigatorException {
	new FileDatabase(getSelectedTables()).save(file);
	return true;
    }

    protected class JDBCTable extends BasicTable {
	public JDBCTable(String qualifiedName) throws NavigatorException {
	    this.qualifiedName = qualifiedName;
	    updateDetails();
	}

	public boolean isEditable() {
	    return true;
	}

	public ResultSetTableModel createModel() throws NavigatorException {
	    ResultSetTableModel model = super.createModel();
	    if (qualifiedName.indexOf("...") == -1 // No orphans
		    && !editedTables.contains(this))
		editedTables.add(this);
	    return model;
	}

	public void unloadModel() {
	    editedTables.remove(this);
	    super.unloadModel();
	}

	public Database getDatabase() {
	    return JDBCDatabase.this;
	}

	protected Data getPKValues2() throws NavigatorException {
	    if (pk == null)
		return null;
	    int ncols = pk.getColumnCount();
	    StringBuffer buf = new StringBuffer();
	    buf.append("select ");
	    for (int i = 0; i < ncols; i++) {
		if (i > 0)
		    buf.append(", ");
		buf.append(pk.getColumnName(i));
	    }
	    buf.append(" from ");
	    buf.append(qualifiedName);
	    return (Data) runQuery(buf.toString(), false, false);
	}

	public Data getData(boolean async) throws NavigatorException {
	    return (Data) runQuery("select * from " + qualifiedName,
				   async, false);
	}

	public void updateDetails() throws NavigatorException {
	    // Create backups of everything, in case we need to roll back
	    // after catching an exception
	    String oldCatalog = catalog;
	    String oldSchema = schema;
	    String oldName = name;
	    String oldType = type;
	    String oldRemarks = remarks;
	    String[] oldColumnNames = columnNames;
	    String[] oldDbTypes = dbTypes;
	    Integer[] oldColumnSizes = columnSizes;
	    Integer[] oldColumnScales = columnScales;
	    int[] oldSqlTypes = sqlTypes;
	    String[] oldIsNullable = isNullable;
	    String[] oldJavaTypes = javaTypes;
	    PrimaryKey oldPk = pk;
	    ForeignKey[] oldFks = fks;
	    ForeignKey[] oldRks = rks;
	    Index[] oldIndexes = indexes;

	    boolean rollback = true;

	    try {
		String[] parts = parseQualifiedName(qualifiedName);
		catalog = parts[0];
		schema = parts[1];
		name = parts[2];

		ArrayList columnNamesList = new ArrayList();
		ArrayList dbTypesList = new ArrayList();
		ArrayList columnSizesList = new ArrayList();
		ArrayList columnScalesList = new ArrayList();
		ArrayList sqlTypesList = new ArrayList();
		ArrayList isNullableList = new ArrayList();

		ResultSet rs = null;
		DatabaseMetaData dbmd = null;
		try {
		    dbmd = con.getMetaData();

		    rs = dbmd.getTables(catalog, schema, name, null);
		    if (rs.next()) {
			type = rs.getString("TABLE_TYPE");
			try {
			    remarks = rs.getString("REMARKS");
			} catch (SQLException e) {
			    // Older versions of Oracle erroneously call this
			    // TABLE_REMARKS
			    remarks = rs.getString("TABLE_REMARKS");
			}
		    } else
			throw new NavigatorException("Table "
					+ qualifiedName + " does not exist.");
		} catch (SQLException e) {
		    throw new NavigatorException(e);
		} finally {
		    if (rs != null)
			try {
			    rs.close();
			} catch (SQLException e) {}
		}

		rs = null;
		try {
		    rs = dbmd.getColumns(catalog, schema, name, null);
		    while (rs.next()) {
			columnNamesList.add(rs.getString("COLUMN_NAME"));
			dbTypesList.add(rs.getString("TYPE_NAME"));
			int size = rs.getInt("COLUMN_SIZE");
			if (rs.wasNull())
			    columnSizesList.add(null);
			else
			    columnSizesList.add(new Integer(size));
			int scale = rs.getInt("DECIMAL_DIGITS");
			if (rs.wasNull())
			    columnScalesList.add(null);
			else
			    columnScalesList.add(new Integer(scale));
			sqlTypesList.add(new Integer(rs.getInt("DATA_TYPE")));
			isNullableList.add(rs.getString("IS_NULLABLE"));
		    }
		} catch (SQLException e) {
		    throw new NavigatorException(e);
		} finally {
		    if (rs != null)
			try {
			    rs.close();
			} catch (SQLException e) {}
		}

		String[] sa = new String[0];
		Integer[] ia = new Integer[0];
		columnNames = (String[]) columnNamesList.toArray(sa);
		dbTypes = (String[]) dbTypesList.toArray(sa);
		columnSizes = (Integer[]) columnSizesList.toArray(ia);
		columnScales = (Integer[]) columnScalesList.toArray(ia);
		int cols = columnNames.length;
		sqlTypes = new int[cols];
		for (int i = 0; i < cols; i++)
		    sqlTypes[i] = ((Integer) sqlTypesList.get(i)).intValue();
		isNullable = (String[]) isNullableList.toArray(sa);

		javaTypes = JDBCDatabase.this.getJavaTypes(qualifiedName);
		pk = JDBCDatabase.this.getPrimaryKey(qualifiedName);
		fks = JDBCDatabase.this.getFK(qualifiedName, true);
		rks = JDBCDatabase.this.getFK(qualifiedName, false);
		indexes = JDBCDatabase.this.getIndexes(this);

		super.updateDetails();

		// Whew! It worked.
		rollback = false;
	    } finally {
		if (rollback) {
		    catalog = oldCatalog;
		    schema = oldSchema;
		    name = oldName;
		    type = oldType;
		    remarks = oldRemarks;
		    columnNames = oldColumnNames;
		    dbTypes = oldDbTypes;
		    columnSizes = oldColumnSizes;
		    columnScales = oldColumnScales;
		    sqlTypes = oldSqlTypes;
		    isNullable = oldIsNullable;
		    javaTypes = oldJavaTypes;
		    pk = oldPk;
		    fks = oldFks;
		    rks = oldRks;
		    indexes = oldIndexes;
		}
	    }
	}
    }


    /**
     * This method is called by Preferences when Preferences.setPassword()
     * was called and the user entered a password -- when this happens, the
     * list of configs may have changed, so we should reload them.
     * <br>
     * This is a bit ugly, and using a nice callback mechanism would be
     * cleaner, but there is the issue that *most* of the changes to the
     * list of configs are actually caused *here* (by LoginDialog). The
     * real answer, I think, would be for Preferences to provide us with
     * a ComboBoxModel, and to hide everything inside Preferences.
     * <br>
     * Sigh. Too much work for such a minor issue.
     */
    public static void reloadConnectionConfigs() {
	LoginDialog.reloadConnectionConfigs();
    }

    private static class LoginDialog extends MyFrame {
	private static LoginDialog instance;

	private ConfigList configs;
	private JComboBox configNameCB;
	private JTextField driverTF;
	private JTextField urlTF;
	private JTextField usernameTF;
	private JPasswordField passwordPF;
	private JButton connectB;
	private JButton saveB;
	private JButton deleteB;
	private JButton cancelB;
	private ConnectThread connectThread;
	private Database.OpenCallback opencb;

	private LoginDialog(Database.OpenCallback opencb) {
	    super("Open JDBC Data Source");
	    Container c = getContentPane();
	    c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
	    JPanel p1 = new JPanel();
	    p1.setLayout(new MyGridBagLayout());
	    MyGridBagConstraints gbc = new MyGridBagConstraints();
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = MyGridBagConstraints.EAST;
	    p1.add(new JLabel("Name:"), gbc);
	    gbc.gridy++;
	    p1.add(new JLabel("Driver Class:"), gbc);
	    gbc.gridy++;
	    p1.add(new JLabel("URL:"), gbc);
	    gbc.gridy++;
	    p1.add(new JLabel("User Name:"), gbc);
	    gbc.gridy++;
	    p1.add(new JLabel("Password:"), gbc);
	    configs = new ConfigList();
	    configNameCB = new JComboBox(configs);
	    configNameCB.setEditable(true);
	    configNameCB.addActionListener(
		    new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    configNameChanged();
			}
		    });
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.gridwidth = 3;
	    gbc.anchor = MyGridBagConstraints.WEST;
	    p1.add(configNameCB, gbc);
	    driverTF = new JTextField(20);
	    gbc.gridy++;
	    p1.add(driverTF, gbc);
	    urlTF = new JTextField(40);
	    gbc.gridy++;
	    p1.add(urlTF, gbc);
	    usernameTF = new JTextField(10);
	    gbc.gridy++;
	    gbc.gridwidth = 1;
	    p1.add(usernameTF, gbc);
	    passwordPF = new JPasswordField(10);
	    gbc.gridy++;
	    p1.add(passwordPF, gbc);

	    c.add(p1);

	    JPanel p2 = new JPanel();
	    p2.setLayout(new MyGridBagLayout());
	    connectB = new JButton("Connect");
	    connectB.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					connect();
				    }
				});
	    gbc.insets = new Insets(0, 0, 0, 10);
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = MyGridBagConstraints.CENTER;
	    p2.add(connectB, gbc);
	    saveB = new JButton("Save");
	    saveB.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					save();
				    }
				});
	    gbc.insets = new Insets(0, 0, 0, 0);
	    gbc.gridx++;
	    p2.add(saveB, gbc);
	    deleteB = new JButton("Delete");
	    deleteB.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					delete();
				    }
				});
	    gbc.gridx++;
	    p2.add(deleteB, gbc);
	    cancelB = new JButton("Cancel");
	    cancelB.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					cancel();
				    }
				});
	    gbc.gridx++;
	    p2.add(cancelB, gbc);
	    c.add(p2);
	    pack();
	    configNameChanged();
	    this.opencb = opencb;
	}

	public void dispose() {
	    instance = null;
	    super.dispose();
	}

	public static void activate(Database.OpenCallback opencb) {
	    if (instance == null) {
		instance = new LoginDialog(opencb);
		instance.showCentered();
	    } else {
		instance.opencb = opencb;
		instance.deiconifyAndRaise();
	    }
	}

	public static void reloadConnectionConfigs() {
	    if (instance != null)
		instance.configs.reload();
	}

	private void configNameChanged() {
	    String name = (String) configNameCB.getSelectedItem();
	    Preferences.ConnectionConfig config =
			    (Preferences.ConnectionConfig) configs.get(name);
	    if (config != null) {
		driverTF.setText(config.driver);
		urlTF.setText(config.url);
		usernameTF.setText(config.username);
		passwordPF.setText(config.password);
	    }
	}

	private void save() {
	    String configName = (String) configNameCB.getSelectedItem();
	    if (configName.equals("")) {
		Toolkit.getDefaultToolkit().beep();
		JOptionPane.showInternalMessageDialog(
			Main.getDesktop(),
			"You have to specify a name before you can\n"
			+ "save a connection configuration.");
		return;
	    }
	    String driver = driverTF.getText();
	    String url = urlTF.getText();
	    String username = usernameTF.getText();
	    String password = new String(passwordPF.getPassword());
	    configs.put(configName,
			new Preferences.ConnectionConfig(driver, url,
							 username, password));
	}

	private void delete() {
	    String configName = (String) configNameCB.getSelectedItem();
	    configNameCB.setSelectedItem("");
	    configs.remove(configName);
	}

	private void connect() {
	    String name = (String) configNameCB.getSelectedItem();
	    if (name.equals("")) {
		// Generate a name of the form "Unnamed #<n>", with the lowest
		// value of n that does not clash with any existing configs...
		int n = 0;
		do {
		    name = "Unnamed #" + (++n);
		} while (configs.get(name) != null);
		configNameCB.setSelectedItem(name);
	    }
	    save();

	    String driver = driverTF.getText();
	    String url = urlTF.getText();
	    String username = usernameTF.getText();
	    String password = new String(passwordPF.getPassword());

	    try {
		Class.forName(driver);
	    } catch (ClassNotFoundException e) {
		Toolkit.getDefaultToolkit().beep();
		JOptionPane.showInternalMessageDialog(
			Main.getDesktop(),
			"Driver \"" + driver + "\" was not found.");
		return;
	    }

	    synchronized (this) {
		connectB.setEnabled(false);
		saveB.setEnabled(false);
		deleteB.setEnabled(false);
		cancelB.requestFocusInWindow();
		// Use a separate thread to establish the connection.
		// DriverManager.getConnection() can take a long time,
		// and we don't want to freeze awt during the wait; also,
		// we want to allow the user to cancel the operation.
		connectThread = new ConnectThread(name, url, driver,
						  username, password);
		new Thread(connectThread).start();
	    }
	}

	private void cancel() {
	    synchronized (this) {
		if (connectThread != null) {
		    connectThread = null;
		    connectB.setEnabled(true);
		    saveB.setEnabled(true);
		    deleteB.setEnabled(true);
		} else
		    dispose();
	    }
	}

	private class ConnectThread implements Runnable {
	    private String name;
	    private String url;
	    private String driver;
	    private String username;
	    private String password;

	    public ConnectThread(String name, String url, String driver,
				 String username, String password) {
		this.name = name;
		this.url = url;
		this.driver = driver;
		this.username = username;
		this.password = password;
	    }

	    public void run() {
		Connection con;
		try {
		    if (username.equals("") && password.equals(""))
			con = DriverManager.getConnection(url);
		    else
			con = DriverManager.getConnection(url,
							username, password);
		} catch (SQLException e) {
		    synchronized (LoginDialog.this) {
			// if this != connectThread, the operation was
			// cancelled, and so we should not report errors about
			// it to the user, nor change the state of the buttons
			// (the frame may have been destroyed already!).
			if (this == connectThread) {
			    MessageBox.show("Could not open JDBC connection.",
					    e);
			    connectB.setEnabled(true);
			    saveB.setEnabled(true);
			    deleteB.setEnabled(true);
			    connectThread = null;
			}
		    }
		    return;
		}

		synchronized (LoginDialog.this) {
		    if (this != connectThread) {
			// User cancelled the operation.
			try {
			    con.close();
			} catch (SQLException e) {}
			return;
		    } else
			connectThread = null;
		}

		// Open the browser, and dispose the login dialog, on
		// the awt event thread; doing it here causes the awt
		// repaint manager to get confused and throw an exception
		// (which appears to be harmless, but still...).
		SwingUtilities.invokeLater(
			    new BrowserOpener(name, con, driver));
	    }
	}

	private class BrowserOpener implements Runnable {
	    private String name;
	    private Connection con;
	    private String driver;

	    public BrowserOpener(String name, Connection con,
				 String driver) {
		this.name = name;
		this.con = con;
		this.driver = driver;
	    }

	    public void run() {
		dispose();
		JDBCDatabase db = JDBCDatabase.create(driver, name, con);
		opencb.databaseOpened(db);
	    }
	}

	private static class ConfigList extends AbstractListModel
					implements ComboBoxModel {
	    private Preferences prefs = Preferences.getPreferences();
	    private ArrayList names = new ArrayList();
	    private ArrayList configs = new ArrayList();
	    private String selectedName;

	    public ConfigList() {
		load();
		if (names.isEmpty())
		    selectedName = "";
		else
		    selectedName = (String) names.get(0);
	    }
	    
	    public void reload() {
		int n = configs.size() / 2 - 1;
		configs.clear();
		if (n >= 0)
		    fireIntervalRemoved(this, 0, n);
		load();
		n = configs.size() / 2 - 1;
		if (n >= 0)
		    fireIntervalAdded(this, 0, n);
	    }

	    private void load() {
		Collection pconfigs = prefs.getConnectionConfigs();
		for (Iterator iter = pconfigs.iterator(); iter.hasNext();) {
		    String name = (String) iter.next();
		    Preferences.ConnectionConfig config =
				(Preferences.ConnectionConfig) iter.next();
		    names.add(name);
		    configs.add(config);
		}
	    }

	    // ListModel methods
	    public int getSize() {
		return names.size();
	    }

	    public Object getElementAt(int index) {
		return names.get(index);
	    }

	    // ComboBoxModel methods
	    public void setSelectedItem(Object item) {
		selectedName = (String) item;
	    }

	    public Object getSelectedItem() {
		return selectedName;
	    }

	    // My methods
	    public void put(String name, Preferences.ConnectionConfig config) {
		int index = names.indexOf(name);
		if (index == 0) {
		    configs.set(0, config);
		    fireContentsChanged(this, 0, 0);
		} else if (index != -1) {
		    names.remove(index);
		    configs.remove(index);
		    fireIntervalRemoved(this, index, index);
		    names.add(0, name);
		    configs.add(0, config);
		    fireIntervalAdded(this, 0, 0);
		} else {
		    names.add(0, name);
		    configs.add(0, config);
		    fireIntervalAdded(this, 0, 0);
		}
		prefs.putConnectionConfig(name, config);
		prefs.write();
	    }

	    public Preferences.ConnectionConfig get(String name) {
		int index = names.indexOf(name);
		if (index == -1)
		    return null;
		else
		    return (Preferences.ConnectionConfig) configs.get(index);
	    }

	    public void remove(String name) {
		int index = names.indexOf(name);
		if (index != -1) {
		    names.remove(index);
		    configs.remove(index);
		    fireIntervalRemoved(this, index, index);
		}
		prefs.removeConnectionConfig(name);
		prefs.write();
	    }
	}
    }


    /////////////////////////////////////////////////////////////////////
    ///// Miscellaneous stuff; formerly InteractiveMetaDriver stuff /////
    /////////////////////////////////////////////////////////////////////

    protected String unmangleKeyName(String name) {
	return name;
    }

    protected boolean needsIsNull() {
	return false;
    }

    protected boolean resultSetContainsTableInfo() {
	return true;
    }

    protected String[] getJavaTypes(String qualifiedName)
						    throws NavigatorException {
	PreparedStatement stmt = null;
	try {
	    stmt = con.prepareStatement("select * from " + qualifiedName);
	    ResultSetMetaData rsmd = stmt.getMetaData();
	    int columns = rsmd.getColumnCount();
	    String[] javaTypes = new String[columns];
	    for (int i = 0; i < columns; i++)
		javaTypes[i] = rsmd.getColumnClassName(i + 1);
	    return javaTypes;
	} catch (SQLException e) {
	    throw new NavigatorException(e);
	} finally {
	    if (stmt != null)
		try {
		    stmt.close();
		} catch (SQLException e) {}
	}
    }

    private String q = null;
    protected String getIdentifierQuoteString() {
	if (q == null)
	    try {
		q = con.getMetaData().getIdentifierQuoteString();
	    } catch (SQLException e) {
		q = " ";
	    }
	return q;
    }

    /**
     * Guess a table's qualified name, given nothing but a bare name.
     */
    protected String qualifyName(String name) {
	return name;
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

    private static class BackgroundLoader implements Runnable,
					    Data.StateListener {
	private BackgroundLoadData data;
	private boolean[] isClob;
	private Statement stmt;
	private ResultSet rs;
	private int state;

	public BackgroundLoader(BackgroundLoadData data, boolean[] isClob,
				Statement stmt, ResultSet rs) {
	    this.data = data;
	    this.isClob = isClob;
	    this.stmt = stmt;
	    this.rs = rs;
	    state = Data.LOADING;
	    data.addStateListener(this);
	}

	public void run() {
	    try {
		char[] cbuf = new char[4096];
		int columns = data.getColumnCount();
		while (rs.next()) {
		    synchronized (this) {
			while (state == Data.PAUSED)
			    try {
				wait();
			    } catch (InterruptedException e) {}
			if (state == Data.FINISHED)
			    break;
		    }
		    Object[] row = new Object[columns];
		    for (int i = 0; i < columns; i++) {
			Object o = rs.getObject(i + 1);
			if (isClob[i] && o != null) {
			    // TODO -- why special case handling for Clobs?
			    // Why not treat them exactly like Blobs, i.e.,
			    // put the Clob object in the Data object, and
			    // leave it to the table cell editor to load the
			    // value?
			    Clob clob = (Clob) o;
			    Reader r = clob.getCharacterStream();
			    int n;
			    StringBuffer sbuf = new StringBuffer();
			    try {
				while ((n = r.read(cbuf)) != -1)
				    sbuf.append(cbuf, 0, n);
			    } finally {
				try {
				    r.close();
				} catch (IOException e) {}
			    }
			    row[i] = sbuf.toString();
			} else
			    row[i] = o;
		    }
		    data.addRow(row);
		}
	    } catch (SQLException e) {
		MessageBox.show("An exception occurred while "
				+ "loading a query result:", e);
	    } catch (IOException e) {
		MessageBox.show("An exception occurred while "
				+ "loading a query result:", e);
	    }

	    if (rs != null)
		try {
		    rs.close();
		} catch (SQLException e) {}
	    if (stmt != null)
		try {
		    stmt.close();
		} catch (SQLException e) {}

	    data.removeStateListener(this);
	    data.setState(Data.FINISHED);
	}

	public void stateChanged(int state, int rows) {
	    synchronized (this) {
		if (this.state == Data.FINISHED || this.state == state)
		    return;
		if (this.state == Data.PAUSED) {
		    this.state = state;
		    notify();
		} else
		    this.state = state;
	    }
	}
    }
}
