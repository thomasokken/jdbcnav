package jdbcnav;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import javax.xml.parsers.*;
import org.mozilla.javascript.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import jdbcnav.model.*;
import jdbcnav.util.*;


public class FileDatabase extends BasicDatabase {
    private File file;
    private String title;
    private String internalDriverName;
    private ArrayList tables;
    private static int dupCount = 0;

    public static void open(Database.OpenCallback opencb) {
	JFileChooser jfc = new JFileChooser();
	jfc.setDialogTitle("Open File Data Source");
	if (jfc.showOpenDialog(Main.getDesktop())
					    != JFileChooser.APPROVE_OPTION)
	    return;
	try {
	    Database db = new FileDatabase(jfc.getSelectedFile());
	    opencb.databaseOpened(db);
	} catch (Exception e) {
	    MessageBox.show("Could not open File Data Source.", e);
	}
    }

    private FileDatabase(File file) throws NavigatorException {
	this.file = file;
	title = file.getName();
	tables = new ArrayList();
	internalDriverName = "Generic"; // fallback value in case the
					// <internal_driver> element is missing
	new FileDatabaseReader().read();
    }

    public FileDatabase(Collection t) throws NavigatorException {
	title = "Duplicate " + (++dupCount);
	file = null;
	tables = new ArrayList();
	Database db = null;
	for (Iterator iter = t.iterator(); iter.hasNext();) {
	    Table table = (Table) iter.next();
	    Database tdb = table.getDatabase();
	    if (db == null) {
		db = tdb;
		internalDriverName = tdb.getInternalDriverName();
	    } else if (db != tdb)
		throw new NavigatorException(
			"All tables must come from the same data source.");
	    FileTable ft = new FileTable(table);
	    ft.setData(new BasicData(table.getData(false)));
	    tables.add(ft);
	}
    }

    public File getFile() {
	return file;
    }

    public void close() {
	//
    }

    public String getName() {
	return file == null ? title : file.getName();
    }

    public String getInternalDriverName() {
	return internalDriverName;
    }

    public String about() {
	if (file == null)
	    return "JDBC Navigator File Data Source\n"
		    + "Not saved to a file yet.";
	else
	    return "JDBC Navigator File Data Source\n"
		    + "Saved in file "
		    + file.getAbsolutePath();
    }

    public void setBrowser(BrowserFrame browser) {
	this.browser = browser;
    }

    public BrowserFrame getBrowser() {
	return browser;
    }

    public boolean needsCommit() {
	return file == null;
    }

    public Collection getDirtyTables() {
	return null;
    }

    protected boolean shouldMoveToOrphanage(Table table) {
	return false;
    }

    public void commitTables(Collection tables) throws NavigatorException {
	// Nothing to do -- FileDatabase is read-only so there's never
	// anything to commit
    }

    protected void duplicate() {
	try {
	    FileDatabase fd = new FileDatabase(tables);
	    BrowserFrame bf = new BrowserFrame(fd);
	    bf.showStaggered();
	} catch (NavigatorException e) {
	    MessageBox.show(e);
	}
    }

    protected Collection getTables() throws NavigatorException {
	ArrayList al = new ArrayList();
	for (Iterator iter = tables.iterator(); iter.hasNext();) {
	    FileTable ft = (FileTable) iter.next();
	    TableSpec ts = new TableSpec();
	    ts.catalog = ft.getCatalog();
	    ts.schema = ft.getSchema();
	    ts.type = ft.getType();
	    ts.name = ft.getName();
	    al.add(ts);
	}
	return al;
    }

    public Table loadTable(String qualifiedName) throws NavigatorException {
	for (Iterator iter = tables.iterator(); iter.hasNext();) {
	    Table t = (Table) iter.next();
	    if (qualifiedName.compareToIgnoreCase(t.getQualifiedName()) == 0)
		return t;
	}
	throw new NavigatorException("Table " + qualifiedName
		    + " not found in File Data Source.");
    }

    public Object runQuery(String query, boolean asynchronous,
			   boolean allowTable) throws NavigatorException {
	throw new NavigatorException("File Data Source is not "
		    + "capable of executing queries.");
    }

    public int runUpdate(String query) throws NavigatorException {
	throw new NavigatorException("File Data Source is not "
		    + "capable of executing updates.");
    }

    public Scriptable createStatement() throws NavigatorException {
	throw new NavigatorException("File Data Source is not "
		    + "capable of creating statements.");
    }

    public Scriptable prepareStatement(String statement)
						    throws NavigatorException {
	throw new NavigatorException("File Data Source is not "
		    + "capable of preparing statements.");
    }

    public Scriptable prepareCall(String call) throws NavigatorException {
	throw new NavigatorException("File Data Source is not "
		    + "capable of preparing call.");
    }

    public boolean save(File file) {
	PrintWriter pw = null;
	try {
	    pw = new PrintWriter(new FileOutputStream(file));
	    XMLWriter xml = new XMLWriter(pw);
	    xml.openTag("database");
	    xml.wholeTag("internal_driver", internalDriverName);
	    for (Iterator iter = tables.iterator(); iter.hasNext();) {
		FileTable ft = (FileTable) iter.next();
		dumpFileTable(xml, ft);
	    }
	    xml.closeTag();
	    pw.flush();
	    if (pw.checkError())
		throw new IOException("PrintWriter signalled an error.");
	    this.file = file;
	    return true;
	} catch (IOException e) {
	    MessageBox.show("Saving File Data Source failed.", e);
	    return false;
	} catch (NavigatorException e) {
	    MessageBox.show("Saving File Data Source failed.", e);
	    return false;
	} finally {
	    if (pw != null)
		pw.close();
	}
    }


    private class FileTable extends BasicTable {
	protected Data data;

	public void setCatalog(String catalog) { this.catalog = catalog; }
	public void setSchema(String schema) { this.schema = schema; }
	public void setName(String name) { this.name = name; }
	public void setType(String type) { this.type = type; }
	public void setRemarks(String remarks) { this.remarks = remarks; }
	public void setQualifiedName(String qualifiedName) { this.qualifiedName = qualifiedName; }
	public void setColumnNames(String[] columnNames) { this.columnNames = columnNames; }
	public void setDbTypes(String[] dbTypes) { this.dbTypes = dbTypes; }
	public void setColumnSizes(Integer[] columnSizes) { this.columnSizes = columnSizes; }
	public void setColumnScales(Integer[] columnScales) { this.columnScales = columnScales; }
	public void setIsNullable(String[] isNullable) { this.isNullable = isNullable; }
	public void setSqlTypes(int[] sqlTypes) { this.sqlTypes = sqlTypes; }
	public void setJavaTypes(String[] javaTypes) { this.javaTypes = javaTypes; }
	public void setPrimaryKey(PrimaryKey pk) { this.pk = pk; }
	public void setForeignKeys(ForeignKey[] fks) { this.fks = fks; }
	public void setReferencingKeys(ForeignKey[] rks) { this.rks = rks; }
	public void setIndexes(Index[] indexes) { this.indexes = indexes; }
	public void setData(Data data) { this.data = data; }

	public FileTable() {
	    // Nothing to do
	}

	public FileTable(Table original) {
	    super(original);
	}

	public boolean isEditable() {
	    return false;
	}

	public Database getDatabase() {
	    return FileDatabase.this;
	}

	public Data getData(boolean async) throws NavigatorException {
	    return data;
	}
    }


    private static void dumpFileTable(XMLWriter xml, FileTable ft)
						    throws NavigatorException {
	xml.openTag("table");
	dumpTable(xml, ft);
	xml.openTag("data");
	dumpData(xml, ft.getData(false));
	xml.closeTag();
	xml.closeTag();
    }

    private static void dumpTable(XMLWriter xml, Table t)
						    throws NavigatorException {
	xml.wholeTag("catalog", t.getCatalog());
	xml.wholeTag("schema", t.getSchema());
	xml.wholeTag("name", t.getName());
	xml.wholeTag("type", t.getType());
	xml.wholeTag("remarks", t.getRemarks());
	xml.wholeTag("qualified_name", t.getQualifiedName());
	xml.wholeTag("column_count", Integer.toString(t.getColumnCount()));
	xml.openTag("column_names");
	for (int i = 0; i < t.getColumnNames().length; i++)
	    xml.wholeTag("_" + i, t.getColumnNames()[i]);
	xml.closeTag();
	xml.openTag("db_types");
	for (int i = 0; i < t.getDbTypes().length; i++)
	    xml.wholeTag("_" + i, t.getDbTypes()[i]);
	xml.closeTag();
	xml.openTag("column_sizes");
	for (int i = 0; i < t.getColumnSizes().length; i++)
	    if (t.getColumnSizes()[i] != null)
		xml.wholeTag("_" + i, t.getColumnSizes()[i].toString());
	xml.closeTag();
	xml.openTag("column_scales");
	for (int i = 0; i < t.getColumnScales().length; i++)
	    if (t.getColumnScales()[i] != null)
		xml.wholeTag("_" + i, t.getColumnScales()[i].toString());
	xml.closeTag();
	xml.openTag("is_nullable");
	for (int i = 0; i < t.getIsNullable().length; i++)
	    xml.wholeTag("_" + i, t.getIsNullable()[i]);
	xml.closeTag();
	xml.openTag("sql_types");
	for (int i = 0; i < t.getSqlTypes().length; i++)
	    xml.wholeTag("_" + i, MiscUtils.sqlTypeIntToString(
						    t.getSqlTypes()[i]));
	xml.closeTag();
	xml.openTag("java_types");
	for (int i = 0; i < t.getJavaTypes().length; i++)
	    xml.wholeTag("_" + i, t.getJavaTypes()[i]);
	xml.closeTag();
	PrimaryKey pk = t.getPrimaryKey();
	if (pk != null)
	    dumpPrimaryKey(xml, pk);
	ForeignKey[] fks = t.getForeignKeys();
	for (int i = 0; i < fks.length; i++)
	    dumpForeignKey(xml, fks[i], true);
	ForeignKey[] rks = t.getReferencingKeys();
	for (int i = 0; i < rks.length; i++)
	    dumpForeignKey(xml, rks[i], false);
	Index[] indexes = t.getIndexes();
	for (int i = 0; i < indexes.length; i++)
	    dumpIndex(xml, indexes[i]);
    }
    
    private static void dumpPrimaryKey(XMLWriter xml, PrimaryKey pk) {
	xml.openTag("primary_key");
	xml.wholeTag("key_name", pk.getName());
	xml.openTag("key_columns");
	for (int i = 0; i < pk.getColumnCount(); i++)
	    xml.wholeTag("_" + i, pk.getColumnName(i));
	xml.closeTag();
	xml.closeTag();
    }

    private static void dumpForeignKey(XMLWriter xml, ForeignKey fk,
					boolean imported) {
	xml.openTag(imported ? "foreign_key" : "referencing_key");
	xml.wholeTag("key_name", fk.getThisKeyName());
	xml.openTag("key_columns");
	for (int i = 0; i < fk.getColumnCount(); i++)
	    xml.wholeTag("_" + i, fk.getThisColumnName(i));
	xml.closeTag();
	xml.wholeTag(imported ? "foreign_catalog" : "referencing_catalog",
		     fk.getThatCatalog());
	xml.wholeTag(imported ? "foreign_schema" : "referencing_schema",
		     fk.getThatSchema());
	xml.wholeTag(imported ? "foreign_name" : "referencing_name",
		     fk.getThatName());
	xml.wholeTag(imported ? "foreign_qualified_name" : "referencing_qualified_name",
		     fk.getThatQualifiedName());
	xml.wholeTag(imported ? "foreign_key_name" : "referencing_key_name",
		     fk.getThatKeyName());
	xml.openTag(imported ? "foreign_columns" : "referencing_columns");
	for (int i = 0; i < fk.getColumnCount(); i++)
	    xml.wholeTag("_" + i, fk.getThatColumnName(i));
	xml.closeTag();
	xml.wholeTag("update_rule", fk.getUpdateRule());
	xml.wholeTag("delete_rule", fk.getDeleteRule());
	xml.closeTag();
    }

    private static void dumpIndex(XMLWriter xml, Index index) {
	xml.openTag("index");
	xml.wholeTag("index_name", index.getName());
	xml.wholeTag("unique", index.isUnique() ? "true" : "false");
	xml.openTag("index_columns");
	for (int i = 0; i < index.getColumnCount(); i++)
	    xml.wholeTag("_" + i, index.getColumnName(i));
	xml.closeTag();
	xml.closeTag();
    }

    private static void dumpData(XMLWriter xml, Data data) {
	xml.openTag("names");
	for (int i = 0; i < data.getColumnCount(); i++)
	    xml.wholeTag("_" + i, data.getColumnName(i));
	xml.closeTag();
	xml.openTag("classes");
	for (int i = 0; i < data.getColumnCount(); i++)
	    if (data.getColumnClass(i) != null)
		xml.wholeTag("_" + i, data.getColumnClass(i).getName());
	xml.closeTag();
	for (int i = 0; i < data.getRowCount(); i++) {
	    xml.openTag("row");
	    for (int j = 0; j < data.getColumnCount(); j++) {
		Object o = data.getValueAt(i, j);
		if (o == null)
		    continue;
		if (o.getClass() == java.util.Date.class)
		    o = new java.sql.Timestamp(((java.util.Date) o).getTime());
		xml.openTagNoNewline("_" + j);
		xml.writeValue(o.toString());
		xml.closeTagNoIndent();
	    }
	    xml.closeTag();
	}
    }

    private class FileDatabaseReader extends DefaultHandler {
	private final String[] STRARRAY = new String[0];
	private final Class[] STR_ARGLIST = new Class[] { String.class };

	private FileTable table;
	private int columnCount;
	private BasicPrimaryKey pk;
	private BasicForeignKey fk;
	private BasicIndex index;
	private ArrayList fks = new ArrayList();
	private ArrayList rks = new ArrayList();
	private ArrayList indexes = new ArrayList();
	private BasicData tr;
	private ArrayList elements = new ArrayList();
	private StringBuffer elementData = new StringBuffer();
	private ArrayList arrayBuffer = new ArrayList();
	private boolean inPrimaryKey;

	public FileDatabaseReader() {
	    //
	}

	public void read() throws NavigatorException{
	    try {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		parser.parse(file, this);
	    } catch (IOException e) {
		throw new NavigatorException(e);
	    } catch (SAXParseException e) {
		String message = "At line " + e.getLineNumber() + ", column "
				+ e.getColumnNumber() + ":";
		throw new NavigatorException(message, e);
	    } catch (ParserConfigurationException e) {
		throw new NavigatorException(e);
	    } catch (SAXException e) {
		throw new NavigatorException(e);
	    }
	}

	// ContentHandler methods

	public void characters(char[] ch, int start, int length)
							throws SAXException {
	    elementData.append(ch, start, length);
	}

	public void startElement(String namespace, String localname,
			String name, Attributes atts) throws SAXException {
	    elements.add(name);

	    // Originally, instead of re-creating the StringBuffer, I used only
	    // one instance and cleared it using delete().
	    // Bad idea! Memory is consumed like candy -- not sure why; prob.
	    // because Strings with needlessly large, mostly unused, underlying
	    // char[] arrays are created?
	    elementData = new StringBuffer();

	    if (name.equals("key_columns")
		    || name.equals("foreign_columns")
		    || name.equals("referencing_columns")
		    || name.equals("key_columns")
		    || name.equals("index_columns")
		    || name.equals("column_names")
		    || name.equals("db_types")
		    || name.equals("column_sizes")
		    || name.equals("column_scales")
		    || name.equals("is_nullable")
		    || name.equals("sql_types")
		    || name.equals("java_types")
		    || name.equals("names")
		    || name.equals("classes")
		    || name.equals("row"))
		arrayBuffer.clear();
	    else if (name.equals("table")) {
		table = new FileTable();
		fks.clear();
		rks.clear();
		indexes.clear();
	    } else if (name.equals("primary_key")) {
		pk = new BasicPrimaryKey();
		inPrimaryKey = true;
	    } else if (name.equals("foreign_key")
		    || name.equals("referencing_key")) {
		fk = new BasicForeignKey();
		inPrimaryKey = false;
	    } else if (name.equals("index")) {
		index = new BasicIndex();
	    } else if (name.equals("data")) {
		tr = new BasicData();
		tr.setData(new ArrayList());
	    }
	}

	public void endElement(String namespace, String localname, String name)
							throws SAXException {
	    boolean error = true;
	    if (!elements.isEmpty()) {
		String tos = (String) elements.remove(elements.size() - 1);
		if (tos.equals(name))
		    error = false;
	    }
	    if (error)
		throw new SAXException("Mismatched closing element.");

	    String data = elementData.toString();

	    if (name.equals("internal_driver"))
		internalDriverName = data;
	    else if (name.equals("table")) {
		table.setForeignKeys((ForeignKey[]) fks.toArray(new ForeignKey[0]));
		table.setReferencingKeys((ForeignKey[]) rks.toArray(new ForeignKey[0]));
		table.setIndexes((Index[]) indexes.toArray(new Index[0]));
		tables.add(table);
	    } else if (name.equals("catalog"))
		table.setCatalog(data);
	    else if (name.equals("schema"))
		table.setSchema(data);
	    else if (name.equals("name"))
		table.setName(data);
	    else if (name.equals("type"))
		table.setType(data);
	    else if (name.equals("remarks"))
		table.setRemarks(data);
	    else if (name.equals("qualified_name"))
		table.setQualifiedName(data);
	    else if (name.equals("column_count"))
		try {
		    columnCount = Integer.parseInt(data);
		} catch (NumberFormatException e) {
		    throw new SAXException("column_count value \"" + data
					   + "\" is not an integer.");
		}
	    else if (name.equals("primary_key"))
		table.setPrimaryKey(pk);
	    else if (name.equals("key_name")) {
		if (inPrimaryKey)
		    pk.setName(data);
		else
		    fk.setThisKeyName(data);
	    } else if (name.equals("key_columns")) {
		if (inPrimaryKey)
		    pk.setColumns((String[]) arrayBuffer.toArray(STRARRAY));
		else
		    fk.setThisColumns((String[]) arrayBuffer.toArray(STRARRAY));
	    } else if (name.equals("foreign_key"))
		fks.add(fk);
	    else if (name.equals("referencing_key"))
		rks.add(fk);
	    else if (name.equals("foreign_catalog")
		    || name.equals("referencing_catalog"))
		fk.setThatCatalog(data);
	    else if (name.equals("foreign_schema")
		    || name.equals("referencing_schema"))
		fk.setThatSchema(data);
	    else if (name.equals("foreign_name")
		    || name.equals("referencing_name"))
		fk.setThatName(data);
	    else if (name.equals("foreign_qualified_name")
		    || name.equals("referencing_qualified_name"))
		fk.setThatQualifiedName(data);
	    else if (name.equals("foreign_key_name")
		    || name.equals("referencing_key_name"))
		fk.setThatKeyName(data);
	    else if (name.equals("foreign_columns")
		    || name.equals("referencing_columns"))
		fk.setThatColumns((String[]) arrayBuffer.toArray(STRARRAY));
	    else if (name.equals("update_rule"))
		fk.setUpdateRule(data);
	    else if (name.equals("delete_rule"))
		fk.setDeleteRule(data);
	    else if (name.equals("index"))
		indexes.add(index);
	    else if (name.equals("index_name"))
		index.setName(data);
	    else if (name.equals("index_columns"))
		index.setColumns((String[]) arrayBuffer.toArray(STRARRAY));
	    else if (name.equals("unique"))
		index.setUnique(Boolean.getBoolean(data));
	    else if (name.equals("data"))
		table.setData(tr);
	    else if (name.equals("names")) {
		String[] s = (String[]) arrayBuffer.toArray(
						    new String[columnCount]);
		tr.setColumnNames(s);
	    } else if (name.equals("classes")) {
		String[] classNames = (String[]) arrayBuffer.toArray(
						    new String[columnCount]);
		int n = classNames.length;
		Class[] columnClasses = new Class[n];
		for (int i = 0; i < n; i++) {
		    try {
			columnClasses[i] = Class.forName(classNames[i]);
		    } catch (ClassNotFoundException e) {
			throw new SAXException("Can't load column class "
						+ classNames[i]);
		    }
		}
		tr.setColumnClasses(columnClasses);
	    } else if (name.equals("row")) {
		int n = tr.getColumnCount();
		String[] sa = (String[]) arrayBuffer.toArray(new String[n]);
		Object[] oa = new Object[n];
		for (int i = 0; i < n; i++) {
		    Class c = tr.getColumnClass(i);
		    String value = sa[i];
		    if (value == null || c == null || c == String.class
						|| c == Object.class)
			oa[i] = value;
		    else if (c == java.sql.Time.class) {
			try {
			    oa[i] = java.sql.Time.valueOf(value);
			} catch (IllegalArgumentException e) {
			    throw new SAXException("Non-Time value "
				    + value + " in java.sql.Time column.");
			}
		    } else if (c == java.sql.Date.class) {
			try {
			    oa[i] = java.sql.Date.valueOf(value);
			} catch (IllegalArgumentException e) {
			    throw new SAXException("Non-Date value "
				    + value + " in java.sql.Date column.");
			}
		    } else if (c == java.sql.Timestamp.class) {
			try {
			    oa[i] = java.sql.Timestamp.valueOf(value);
			} catch (IllegalArgumentException e) {
			    throw new SAXException("Non-Timestamp value "
				    + value + " in java.sql.Timestamp column.");
			}
		    } else if (c == java.util.Date.class) {
			try {
			    oa[i] = new java.util.Date(
				java.sql.Timestamp.valueOf(value).getTime());
			} catch (IllegalArgumentException e) {
			    throw new SAXException("Non-Date value "
				    + value + " in java.util.Date column.");
			}
		    } else if (c.getName().equals("oracle.sql.CLOB")) {
			// Note how I'm not doing c == oracle.sql.CLOB.class;
			// that would require having the Oracle JDBC driver in
			// the classpath, which is annoying if you are not an
			// Oracle user...
			oa[i] = value;
		    } else {
			try {
			    Constructor cnstr = c.getConstructor(STR_ARGLIST);
			    oa[i] = cnstr.newInstance(new Object[] { value });
			} catch (Exception e) {
			    // Could be NoSuchMethodException or
			    // SecurityException while trying to get the
			    // constructor, or InstantiationException,
			    // IllegalAccessException, IllegalArgumentException,
			    // or InvocationTargetException while invoking the
			    // constructor.
			    throw new SAXException("Can't construct " +
				c.getName() + " object from \"" + value + "\"");
			}
		    }
		}
		tr.addRow(oa);
	    } else if (name.equals("column_names")) {
		table.setColumnNames((String[]) arrayBuffer.toArray(
						    new String[columnCount]));
	    } else if (name.equals("db_types")) {
		table.setDbTypes((String[]) arrayBuffer.toArray(
						    new String[columnCount]));
	    } else if (name.equals("column_sizes")) {
		String[] sa = (String[]) arrayBuffer.toArray(
						    new String[columnCount]);
		int n = sa.length;
		Integer[] columnSizes = new Integer[n];
		for (int i = 0; i < n; i++)
		    if (sa[i] != null)
			try {
			    columnSizes[i] = new Integer(sa[i]);
			} catch (NumberFormatException e) {
			    throw new SAXException("column_sizes value "
				    + sa[i] + " is not an integer.");
			}
		table.setColumnSizes(columnSizes);
	    } else if (name.equals("column_scales")) {
		String[] sa = (String[]) arrayBuffer.toArray(
						    new String[columnCount]);
		int n = sa.length;
		Integer[] columnScales = new Integer[n];
		for (int i = 0; i < n; i++)
		    if (sa[i] != null)
			try {
			    columnScales[i] = new Integer(sa[i]);
			} catch (NumberFormatException e) {
			    throw new SAXException("column_scales value "
				    + sa[i] + " is not an integer.");
			}
		table.setColumnScales(columnScales);
	    } else if (name.equals("is_nullable")) {
		table.setIsNullable((String[]) arrayBuffer.toArray(
						    new String[columnCount]));
	    } else if (name.equals("sql_types")) {
		String[] sa = (String[]) arrayBuffer.toArray(
						    new String[columnCount]);
		int n = sa.length;
		int[] sqlTypes = new int[n];
		for (int i = 0; i < n; i++)
		    if (sa[i] != null)
			try {
			    sqlTypes[i] = MiscUtils.sqlTypeStringToInt(sa[i]);
			} catch (IllegalArgumentException e) {
			    throw new SAXException("Unknown sql_types value "
				    + sa[i] + ".");
			}
		table.setSqlTypes(sqlTypes);
	    } else if (name.equals("java_types")) {
		table.setJavaTypes((String[]) arrayBuffer.toArray(
						    new String[columnCount]));
	    } else if (name.startsWith("_")) {
		int n;
		try {
		    n = Integer.parseInt(name.substring(1));
		} catch (NumberFormatException e) {
		    throw new SAXException("Bad element name " + name);
		}
		for (int i = arrayBuffer.size(); i <= n; i++)
		    arrayBuffer.add(null);
		arrayBuffer.set(n, data);
	    }
	}
    }


    private boolean weKnowWhatToShow = false;
    private boolean showCatalogs;
    private boolean showSchemas;
    private boolean showTableTypes;

    private void findOutWhatToShow() {
	if (!weKnowWhatToShow) {

	    showCatalogs = false;
	    showSchemas = false;
	    showTableTypes = false;

	    for (Iterator iter = tables.iterator(); iter.hasNext();) {
		FileTable ft = (FileTable) iter.next();
		if (ft.getCatalog() != null)
		    showCatalogs = true;
		if (ft.getSchema() != null)
		    showSchemas = true;
		if (ft.getType() != null)
		    showTableTypes = true;
		if (showCatalogs && showSchemas && showTableTypes)
		    // No reason to look further
		    break;
	    }

	    weKnowWhatToShow = true;
	}
    }

    protected boolean showCatalogs() {
	findOutWhatToShow();
	return showCatalogs;
    }

    protected boolean showSchemas() {
	findOutWhatToShow();
	return showSchemas;
    }

    protected boolean showTableTypes() {
	findOutWhatToShow();
	return showTableTypes;
    }
}
