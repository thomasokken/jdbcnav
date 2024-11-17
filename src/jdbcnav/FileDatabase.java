///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2024  Thomas Okken
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.mozilla.javascript.Scriptable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import jdbcnav.model.BasicData;
import jdbcnav.model.BasicForeignKey;
import jdbcnav.model.BasicIndex;
import jdbcnav.model.BasicPrimaryKey;
import jdbcnav.model.BasicTable;
import jdbcnav.model.Data;
import jdbcnav.model.Database;
import jdbcnav.model.ForeignKey;
import jdbcnav.model.Index;
import jdbcnav.model.PrimaryKey;
import jdbcnav.model.Table;
import jdbcnav.model.TypeSpec;
import jdbcnav.util.FileUtils;
import jdbcnav.util.NavigatorException;
import jdbcnav.util.XMLWriter;


public class FileDatabase extends BasicDatabase {
    private File file;
    private String title;
    private String internalDriverName;
    private ArrayList<FileTable> tables;
    private static int dupCount = 0;

    public static void open(Database.OpenCallback opencb) {
        JFileChooser jfc = new MyFileChooser();
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
        tables = new ArrayList<FileTable>();
        internalDriverName = "Generic"; // fallback value in case the
                                        // <internal_driver> element is missing
        new FileDatabaseReader().read();
    }

    public FileDatabase(Collection<? extends Table> t) throws NavigatorException {
        title = "Duplicate " + (++dupCount);
        file = null;
        tables = new ArrayList<FileTable>();
        Database db = null;
        for (Table table : t) {
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

    public Collection<Table> getDirtyTables() {
        return null;
    }

    protected boolean shouldMoveToOrphanage(Table table) {
        return false;
    }

    public void commitTables(Collection<Table> tables) throws NavigatorException {
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

    protected Collection<TableSpec> getTables() throws NavigatorException {
        ArrayList<TableSpec> al = new ArrayList<TableSpec>();
        for (FileTable ft : tables) {
            TableSpec ts = new TableSpec();
            ts.catalog = ft.getCatalog();
            ts.schema = ft.getSchema();
            ts.type = ft.getType();
            ts.name = ft.getName();
            al.add(ts);
        }
        return al;
    }
    
    public boolean isCaseSensitive() {
        return true;
    }

    public Table loadTable(String qualifiedName) throws NavigatorException {
        for (FileTable t : tables)
            if (qualifiedName.compareToIgnoreCase(t.getQualifiedName()) == 0)
                return t;
        throw new NavigatorException("Table " + qualifiedName
                    + " not found in File Data Source.");
    }

    public void searchTables(Set<String> qualifiedNames, SearchParams params) throws NavigatorException {
        throw new NavigatorException("File Data Source is not "
             + "capable of searching tables.");
    }
        
    public int searchTable(String qualifiedName, SearchParams params) throws NavigatorException {
        throw new NavigatorException("File Data Source is not "
             + "capable of searching tables.");
    }
        
    public void runSearch(String qualifiedName, SearchParams params) {
        //
    }
        
    public Object runQuery(String query, boolean asynchronous,
                           boolean allowTable) throws NavigatorException {
        throw new NavigatorException("File Data Source is not "
                    + "capable of executing queries.");
    }

    public Object runQuery(String query, Object[] values) throws NavigatorException {
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

    public Scriptable prepareStatement(String statement, boolean returnGenKeys)
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
            for (FileTable ft : tables)
                dumpFileTable(xml, ft);
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
        public void setTypeSpecs(TypeSpec[] typeSpecs) { this.typeSpecs = typeSpecs; }
        public void setIsNullable(String[] isNullable) { this.isNullable = isNullable; }
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
        dumpData(xml, ft.getData(false));
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
        xml.wholeTag("column_count", t.getColumnCount());
        xml.openTag("column_names");
        for (int i = 0; i < t.getColumnNames().length; i++)
            xml.wholeTag("_" + i, t.getColumnNames()[i]);
        xml.closeTag();
        xml.openTag("type_specs");
        TypeSpec[] typeSpecs = t.getTypeSpecs();
        for (int i = 0; i < typeSpecs.length; i++)
            dumpTypeSpec(xml, typeSpecs[i]);
        xml.closeTag();
        xml.openTag("is_nullable");
        for (int i = 0; i < t.getIsNullable().length; i++)
            xml.wholeTag("_" + i, t.getIsNullable()[i]);
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
    
    private static void dumpTypeSpec(XMLWriter xml, TypeSpec spec) {
        xml.openTag("type_spec");
        xml.wholeTag("type", spec.type);
        if (spec.type != TypeSpec.UNKNOWN
                && spec.type != TypeSpec.DATE
                && spec.type != TypeSpec.LONGVARCHAR
                && spec.type != TypeSpec.LONGVARNCHAR
                && spec.type != TypeSpec.LONGVARRAW) {
            xml.wholeTag("size", spec.size);
            if (spec.type == TypeSpec.FIXED || spec.type == TypeSpec.FLOAT)
                xml.wholeTag("size_in_bits", spec.size_in_bits ? "true" : "false");
        }
        if (spec.type == TypeSpec.FIXED
                || spec.type == TypeSpec.INTERVAL_DS) {
            xml.wholeTag("scale", spec.scale);
            if (spec.type == TypeSpec.FIXED)
                xml.wholeTag("scale_in_bits", spec.scale_in_bits ? "true" : "false");
        }
        if (spec.type == TypeSpec.FLOAT) {
            xml.wholeTag("min_exp", spec.min_exp);
            xml.wholeTag("max_exp", spec.max_exp);
            xml.wholeTag("exp_of_2", spec.exp_of_2 ? "true" : "false");
        }
        xml.wholeTag("part_of_key", spec.part_of_key ? "true" : "false");
        xml.wholeTag("part_of_index", spec.part_of_index ? "true" : "false");
        xml.wholeTag("native_representation", spec.native_representation);
        xml.wholeTag("jdbc_db_type", spec.jdbcDbType);
        if (spec.jdbcSize != null)
            xml.wholeTag("jdbc_size", spec.jdbcSize.toString());
        if (spec.jdbcScale != null)
            xml.wholeTag("jdbc_scale", spec.jdbcScale.toString());
        xml.wholeTag("jdbc_sql_type", spec.jdbcSqlType);
        xml.wholeTag("jdbc_java_type", spec.jdbcJavaType);
        xml.closeTag();
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
        int rows = data.getRowCount();
        int columns = data.getColumnCount();
        TypeSpec[] specs = new TypeSpec[columns];
        for (int i = 0; i < columns; i++)
            specs[i] = data.getTypeSpec(i);
        Class<?> byteArrayClass = new byte[1].getClass();

        xml.openTag("data");
        for (int i = 0; i < rows; i++) {
            xml.openTag("row");
            for (int j = 0; j < columns; j++) {
                Object o = data.getValueAt(i, j);
                String s;
                if (o == null)
                    continue;
                Class<?> k = specs[j].jdbcJavaClass;
                if (k == String.class
                        || java.sql.Clob.class.isAssignableFrom(k))
                    s = FileUtils.encodeEntities((String) o);
                else if (k == byteArrayClass
                        || java.sql.Blob.class.isAssignableFrom(k))
                    s = FileUtils.byteArrayToBase64((byte[]) o);
                else
                    s = specs[j].objectToString(o);
                xml.openTagNoNewline("_" + j);
                xml.writeValue(s);
                xml.closeTagNoIndent();
            }
            xml.closeTag();
        }
        xml.closeTag();
    }

    private class FileDatabaseReader extends DefaultHandler {
        private final String[] STRARRAY = new String[0];

        private FileTable table;
        private int columnCount;
        private TypeSpec spec;
        private BasicPrimaryKey pk;
        private BasicForeignKey fk;
        private BasicIndex index;
        private ArrayList<TypeSpec> typeSpecs = new ArrayList<TypeSpec>();
        private ArrayList<ForeignKey> fks = new ArrayList<ForeignKey>();
        private ArrayList<ForeignKey> rks = new ArrayList<ForeignKey>();
        private ArrayList<Index> indexes = new ArrayList<Index>();
        private BasicData tr;
        private ArrayList<String> elements = new ArrayList<String>();
        private StringBuffer elementData = new StringBuffer();
        private ArrayList<Object> arrayBuffer = new ArrayList<Object>();
        private boolean inPrimaryKey;
        private boolean inTypeSpec;

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
                    || name.equals("type_specs")
                    || name.equals("is_nullable")
                    || name.equals("row"))
                arrayBuffer.clear();
            else if (name.equals("table")) {
                table = new FileTable();
                typeSpecs.clear();
                fks.clear();
                rks.clear();
                indexes.clear();
                inTypeSpec = false;
            } else if (name.equals("type_spec")) {
                spec = new TypeSpec(FileDatabase.this);
                inTypeSpec = true;
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
                tr.setColumnNames(table.getColumnNames());
                tr.setTypeSpecs(table.getTypeSpecs());
                tr.setData(new ArrayList<Object[]>());
            }
        }

        public void endElement(String namespace, String localname, String name)
                                                        throws SAXException {
            boolean error = true;
            if (!elements.isEmpty()) {
                String tos = elements.remove(elements.size() - 1);
                if (tos.equals(name))
                    error = false;
            }
            if (error)
                throw new SAXException("Mismatched closing element.");

            String data = elementData.toString();

            if (name.equals("internal_driver"))
                internalDriverName = data;
            else if (name.equals("table")) {
                table.setForeignKeys(fks.toArray(new ForeignKey[0]));
                table.setReferencingKeys(rks.toArray(new ForeignKey[0]));
                table.setIndexes(indexes.toArray(new Index[0]));
                tables.add(table);
            } else if (name.equals("catalog"))
                table.setCatalog(data);
            else if (name.equals("schema"))
                table.setSchema(data);
            else if (name.equals("name"))
                table.setName(data);
            else if (name.equals("type")) {
                if (inTypeSpec)
                    try {
                        spec.type = Integer.parseInt(data);
                    } catch (NumberFormatException e) {
                        throw new SAXException("type value \"" + data
                                            + "\" is not an integer.");
                    }
                else
                    table.setType(data);
            } else if (name.equals("remarks"))
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
                    pk.setColumns(arrayBuffer.toArray(STRARRAY));
                else
                    fk.setThisColumns(arrayBuffer.toArray(STRARRAY));
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
                fk.setThatColumns(arrayBuffer.toArray(STRARRAY));
            else if (name.equals("update_rule"))
                fk.setUpdateRule(data);
            else if (name.equals("delete_rule"))
                fk.setDeleteRule(data);
            else if (name.equals("index"))
                indexes.add(index);
            else if (name.equals("index_name"))
                index.setName(data);
            else if (name.equals("index_columns"))
                index.setColumns(arrayBuffer.toArray(STRARRAY));
            else if (name.equals("unique"))
                index.setUnique(Boolean.parseBoolean(data));
            else if (name.equals("data"))
                table.setData(tr);
            else if (name.equals("row")) {
                int n = tr.getColumnCount();
                String[] sa = arrayBuffer.toArray(new String[n]);
                Object[] oa = new Object[n];
                Class<?> byteArrayClass = new byte[1].getClass();
                for (int i = 0; i < n; i++) {
                    String s = sa[i];
                    if (s == null)
                        continue;
                    TypeSpec spec = tr.getTypeSpec(i);
                    Class<?> k = spec.jdbcJavaClass;
                    try {
                        if (k == String.class
                                || java.sql.Clob.class.isAssignableFrom(k))
                            oa[i] = FileUtils.decodeEntities(s);
                        else if (k == byteArrayClass
                                || java.sql.Blob.class.isAssignableFrom(k))
                            oa[i] = FileUtils.base64ToByteArray(s);
                        else
                            try {
                                oa[i] = spec.stringToObject(s);
                            } catch (IllegalArgumentException e) {
                                if (!spec.jdbcJavaType.startsWith("java."))
                                    // Probably a DB-specific type; we just put
                                    // the String version into the model and
                                    // hope for the best.
                                    oa[i] = s;
                                else
                                    // If instantiating a standard java class
                                    // fails, we have a bad input file, and we
                                    // really should complain.
                                    throw e;
                            }
                    } catch (IllegalArgumentException e) {
                        throw new SAXException("Bad value " + s
                                + " in column " + table.getColumnNames()[i]
                                + " of table " + table.getQualifiedName());
                    }
                }
                tr.addRow(oa);
            } else if (name.equals("column_names")) {
                table.setColumnNames(arrayBuffer.toArray(new String[columnCount]));
            } else if (name.equals("size")) {
                try {
                    spec.size = Integer.parseInt(data);
                } catch (NumberFormatException e) {
                    throw new SAXException("size value \"" + data
                                           + "\" is not an integer.");
                }
            } else if (name.equals("size_in_bits")) {
                spec.size_in_bits = Boolean.parseBoolean(data);
            } else if (name.equals("scale")) {
                try {
                    spec.scale = Integer.parseInt(data);
                } catch (NumberFormatException e) {
                    throw new SAXException("scale value \"" + data
                                           + "\" is not an integer.");
                }
            } else if (name.equals("scale_in_bits")) {
                spec.scale_in_bits = Boolean.parseBoolean(data);
            } else if (name.equals("min_exp")) {
                try {
                    spec.min_exp = Integer.parseInt(data);
                } catch (NumberFormatException e) {
                    throw new SAXException("min_exp value \"" + data
                                           + "\" is not an integer.");
                }
            } else if (name.equals("max_exp")) {
                try {
                    spec.max_exp = Integer.parseInt(data);
                } catch (NumberFormatException e) {
                    throw new SAXException("max_exp value \"" + data
                                           + "\" is not an integer.");
                }
            } else if (name.equals("exp_of_2")) {
                spec.exp_of_2 = Boolean.parseBoolean(data);
            } else if (name.equals("part_of_key")) {
                spec.part_of_key = Boolean.parseBoolean(data);
            } else if (name.equals("part_of_index")) {
                spec.part_of_index = Boolean.parseBoolean(data);
            } else if (name.equals("jdbc_db_type")) {
                spec.jdbcDbType = data;
            } else if (name.equals("jdbc_size")) {
                try {
                    spec.jdbcSize = Integer.parseInt(data);
                } catch (NumberFormatException e) {
                    throw new SAXException("jdbc_size value \"" + data
                                           + "\" is not an integer.");
                }
            } else if (name.equals("jdbc_scale")) {
                try {
                    spec.jdbcScale = Integer.parseInt(data);
                } catch (NumberFormatException e) {
                    throw new SAXException("jdbc_scale value \"" + data
                                           + "\" is not an integer.");
                }
            } else if (name.equals("jdbc_sql_type")) {
                try {
                    spec.jdbcSqlType = Integer.parseInt(data);
                } catch (NumberFormatException e) {
                    throw new SAXException("jdbc_sql_type value \"" + data
                                           + "\" is not an integer.");
                }
            } else if (name.equals("jdbc_java_type")) {
                spec.jdbcJavaType = data;
                try {
                    spec.jdbcJavaClass = Class.forName(data);
                } catch (ClassNotFoundException e) {
                    spec.jdbcJavaClass = Object.class;
                }
            } else if (name.equals("native_representation")) {
                spec.native_representation = data;
            } else if (name.equals("type_spec")) {
                arrayBuffer.add(spec);
            } else if (name.equals("type_specs")) {
                table.setTypeSpecs(arrayBuffer.toArray(new TypeSpec[columnCount]));
            } else if (name.equals("is_nullable")) {
                table.setIsNullable(arrayBuffer.toArray(new String[columnCount]));
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

            for (FileTable ft : tables) {
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

    /**
     * Overriding BasicTable.objectToString() in order to render
     * database-specific datatypes that somehow survive the transition
     * into a FileDatabase, but have no useful toString() method.
     */
    public String objectToString(TypeSpec spec, Object o) {
        if (o == null)
            return super.objectToString(spec, o);
        if (spec.jdbcJavaType.equals("oracle.sql.TIMESTAMP")) {
            try {
                Method m = spec.jdbcJavaClass.getMethod("timestampValue", (Class[]) null);
                java.sql.Timestamp ts = (java.sql.Timestamp) m.invoke(o, (Object[]) null);
                return ts.toString();
            } catch (Exception e) {}
        }
        return super.objectToString(spec, o);
    }
}
