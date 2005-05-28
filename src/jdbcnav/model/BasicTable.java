package jdbcnav.model;

import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import org.mozilla.javascript.*;

import jdbcnav.*;
import jdbcnav.javascript.*;
import jdbcnav.util.*;


public abstract class BasicTable implements Table, Scriptable {
    protected String catalog;
    protected String schema;
    protected String name;
    protected String type;
    protected String remarks;
    protected String qualifiedName;
    protected String[] columnNames;
    protected String[] dbTypes;
    protected Integer[] columnSizes;
    protected Integer[] columnScales;
    protected String[] isNullable;
    protected int[] sqlTypes;
    protected String[] javaTypes;
    protected PrimaryKey pk;
    protected ForeignKey[] fks;
    protected ForeignKey[] rks;
    protected Index[] indexes;
    protected ResultSetTableModel model;
    private int suffix;
    private Data pkValues;
    private int pkValuesFromModel;

    protected BasicTable() {
	// Nothing to do
    }

    protected BasicTable(Table original) {
	// NOTE: shallow cloning
	catalog = original.getCatalog();
	schema = original.getSchema();
	name = original.getName();
	type = original.getType();
	remarks = original.getRemarks();
	qualifiedName = original.getQualifiedName();
	columnNames = original.getColumnNames();
	dbTypes = original.getDbTypes();
	columnSizes = original.getColumnSizes();
	columnScales = original.getColumnScales();
	isNullable = original.getIsNullable();
	sqlTypes = original.getSqlTypes();
	javaTypes = original.getJavaTypes();
	pk = original.getPrimaryKey();
	fks = original.getForeignKeys();
	rks = original.getReferencingKeys();
	indexes = original.getIndexes();
    }

    public String getCatalog() { return catalog; }
    public String getSchema() { return schema; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getRemarks() { return remarks; }
    public String getQualifiedName() { return qualifiedName; }
    public String getQuotedName() { return getDatabase().quote(name); }
    public int getColumnCount() { return columnNames.length; }
    public String[] getColumnNames() { return columnNames; }
    public String[] getDbTypes() { return dbTypes; }
    public Integer[] getColumnSizes() { return columnSizes; }
    public Integer[] getColumnScales() { return columnScales; }
    public String[] getIsNullable() { return isNullable; }
    public int[] getSqlTypes() { return sqlTypes; }
    public String[] getJavaTypes() { return javaTypes; }
    public PrimaryKey getPrimaryKey() { return pk; }
    public ForeignKey[] getForeignKeys() { return fks; }
    public ForeignKey[] getReferencingKeys() { return rks; }
    public Index[] getIndexes() { return indexes; }
    
    public Data getPKValues() throws NavigatorException {
	if (pk == null)
	    return null;
	if (model == null) {
	    if (pkValues == null) {
		// Populate pkValues using a separate query; we don't want
		// to force creating a model since that will load the entire
		// table, which could be prohibitively expensive.
		pkValues = getPKValues2();
		pkValuesFromModel = -1;
	    }
	} else {
	    int nrows = model.getRowCount();
	    if (nrows < pkValuesFromModel) {
		// Additional rows have been loaded; update pkValues to
		// include the new rows. Since the model may have been
		// re-sorted, we have no choice but to reload pkValues
		// from scratch.
		int[] col = getPKColumns();
		int ncols = col.length;
		String[] names = new String[ncols];
		Class[] classes = new Class[ncols];
		for (int i = 0; i < ncols; i++) {
		    names[i] = columnNames[col[i]];
		    classes[i] = model.getColumnClass(col[i]);
		}
		ArrayList data = new ArrayList();
		for (int i = 0; i < nrows; i++) {
		    Object[] row = new Object[ncols];
		    for (int j = 0; j < ncols; j++)
			row[j] = model.getValueAt(i, col[j]);
		    data.add(row);
		}
		BasicData bd = new BasicData();
		bd.setColumnNames(names);
		bd.setColumnClasses(classes);
		bd.setData(data);
		pkValues = bd;
		pkValuesFromModel = nrows;
	    }
	}
	return pkValues;
    }

    protected Data getPKValues2() throws NavigatorException {
	// To be overridden by Table classes that can load PK Values,
	// e.g. by running a query
	return null;
    }

    public void updateDetails() throws NavigatorException {
	pkColumns = null;
	rkColumns = null;
	fkColumns = null;
    }

    public final void makeOrphan() {
	// For the new base name, we use the old qualified name
	if (catalog != null)
	    if (schema != null)
		name = catalog + "." + schema + "." + name;
	    else
		name = catalog + "." + name;
	else
	    if (schema != null)
		name = schema + "." + name;
	schema = null;
	catalog = null;

	// The new qualified name is constructed in such a way that it cannot
	// possibly clash with any legitimate DB object; we achieve this
	// by using lots of dots (a legit qualified name can never have
	// more than two dots).
	qualifiedName = BasicDatabase.ORPHANAGE + "..."
			+ getDatabase().makeQualifiedName(null, null, name);
    }

    /**
     * Call this if the name generated by makeOrphan() turns out to exist
     * already in the orphanage. This method will append "_1" to the name,
     * and if that still doesn't make the new name unique, just keep calling
     * this method; the suffix will be changed to "_2", "_3", etc.
     */
    public void tryNextOrphanName() {
	suffix++;
	if (suffix == 1) {
	    name += "_1";
	} else {
	    int pos = name.lastIndexOf('_');
	    name = name.substring(0, pos + 1) + suffix;
	}
	qualifiedName = BasicDatabase.ORPHANAGE + "..."
			+ getDatabase().makeQualifiedName(null, null, name);
    }

    public boolean needsCommit() {
	return model != null && model.isDirty();
    }

    public boolean isUpdatableQueryResult() {
	return false;
    }

    public synchronized ResultSetTableModel createModel()
						throws NavigatorException {
	if (model == null)
	    // From the UI, we load the table asynchronously.
	    model = new ResultSetTableModel(getData(true), this);
	return model;
    }

    public synchronized ResultSetTableModel getModel() {
	return model;
    }

    public synchronized void unloadModel() {
	model = null;
	pkValues = null;
    }

    public synchronized void reload() throws NavigatorException {
	// From the UI, we load the table asynchronously.
	if (model == null)
	    model = new ResultSetTableModel(getData(true), this);
	else
	    model.load(getData(true));
	pkValues = null;
    }

    private int[] pkColumns;
    public int[] getPKColumns() {
	if (pkColumns != null)
	    return pkColumns;
	PrimaryKey pk = getPrimaryKey();
	if (pk == null) {
	    // NOTE: Returning a fake primary key, consisting of all columns.
	    pkColumns = new int[columnNames.length];
	    for (int i = 0; i < columnNames.length; i++)
		pkColumns[i] = i;
	    return pkColumns;
	}
	int n = pk.getColumnCount();
	pkColumns = new int[n];
	for (int i = 0; i < n; i++) {
	    String name = pk.getColumnName(i);
	    pkColumns[i] = MiscUtils.arrayLinearSearch(columnNames, name);
	}
	return pkColumns;
    }

    // Note: this method returns an array of integers representing the
    // positions of the components of referenced (exported) keys within
    // a row. The positions are ordered to match the components in the
    // array returned by getPKColumns(), so that in a composite key,
    // rk[0] will be the position of the column in the foreign table
    // which matches the column in this table given by pk[0].
    // The upshot is that you can take keys returned by indexToPK()
    // and pass them to functions expecting referenced keys, without
    // having to reorder the components in between.

    private int[][] rkColumns;
    public int[] getRKColumns(int rkIndex, Table that)
						throws NavigatorException {
	if (rkColumns == null)
	    rkColumns = new int[getReferencingKeys().length][];
	if (rkColumns[rkIndex] != null)
	    return rkColumns[rkIndex];
	ForeignKey rk = getReferencingKeys()[rkIndex];
	int n = rk.getColumnCount();
	String[] rkThisColumns = new String[n];
	for (int i = 0; i < n; i++)
	    rkThisColumns[i] = rk.getThisColumnName(i);
	int[] res = new int[n];
	int[] pkColumns = getPKColumns();
	String[] thatHeaders = that.getColumnNames();
	for (int i = 0; i < pkColumns.length; i++) {
	    String thisComp = columnNames[pkColumns[i]];
	    int thisIndex = MiscUtils.arrayLinearSearch(rkThisColumns,thisComp);
	    String thatComp = rk.getThatColumnName(thisIndex);
	    int thatIndex = MiscUtils.arrayLinearSearch(thatHeaders, thatComp);
	    res[i] = thatIndex;
	}
	rkColumns[rkIndex] = res;
	return res;
    }

    // Note: this method returns an array of integers representing the
    // positions of the components of referencing (imported, foreign)
    // keys within a row. The positions are ordered to match the
    // components in the array returned by that.getPKColumns(), so that
    // in a composite key, fk[0] will be the position of the column in
    // this table which matches the column in the foreign table given
    // by pk[0].
    // The upshot is that you can construct keys from this table by
    // extracting the columns in the fk[] array in order, and pass those
    // keys to the foreign table's primary-key-expecting methods,
    // without having to reorder the components in between.

    private int[][] fkColumns;
    public int[] getFKColumns(int fkIndex, Table that)
						throws NavigatorException {
	if (fkColumns == null)
	    fkColumns = new int[getForeignKeys().length][];
	if (fkColumns[fkIndex] != null)
	    return fkColumns[fkIndex];
	ForeignKey fk = getForeignKeys()[fkIndex];
	int n = fk.getColumnCount();
	String[] fkThatColumns = new String[n];
	for (int i = 0; i < n; i++)
	    fkThatColumns[i] = fk.getThatColumnName(i);
	int[] res = new int[n];
	int[] pkColumns = that.getPKColumns();
	String[] thatHeaders = that.getColumnNames();
	for (int i = 0; i < pkColumns.length; i++) {
	    String thatComp = thatHeaders[pkColumns[i]];
	    int thatIndex = MiscUtils.arrayLinearSearch(fkThatColumns,thatComp);
	    String thisComp = fk.getThisColumnName(thatIndex);
	    int thisIndex = MiscUtils.arrayLinearSearch(columnNames, thisComp);
	    res[i] = thisIndex;
	}
	fkColumns[fkIndex] = res;
	return res;
    }


    //////////////////////
    ///// Comparable /////
    //////////////////////

    public int compareTo(Object o) {
	Table that = (Table) o;
	int res = MiscUtils.strCmp(this.getCatalog(), that.getCatalog());
	if (res != 0)
	    return res;
	res = MiscUtils.strCmp(this.getSchema(), that.getSchema());
	if (res != 0)
	    return res;
	return MiscUtils.strCmp(this.getName(), that.getName());
    }

    //////////////////////
    ///// Scriptable /////
    //////////////////////

    private class LoadFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length != 0)
		throw new EvaluatorException(
				    "BasicTable.load() takes no arguments.");
	    try {
		// From JavaScript, we load the table synchronously.
		synchronized (BasicTable.this) {
		    if (model == null)
			model = new ResultSetTableModel(getData(false),
							BasicTable.this);
		    else
			model.load(getData(false));
		}
	    } catch (NavigatorException e) {
		throw new WrappedException(e);
	    }
	    return Context.getUndefinedValue();
	}
    }

    private class AddRowFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length != 0)
		throw new EvaluatorException(
			    "BasicTable.addRow() takes no arguments.");
	    if (model == null)
		throw new EvaluatorException("Not loaded.");
	    model.insertRow(-1);
	    return Context.getUndefinedValue();
	}
    }

    private class RemoveRowFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length != 1 || !(args[0] instanceof Number))
		throw new EvaluatorException(
			"BasicTable.removeRow() takes one numeric argument.");
	    if (model == null)
		throw new EvaluatorException("Not loaded.");
	    int row = ((Number) args[0]).intValue();
	    model.deleteRow(new int[] { row }, false);
	    return Context.getUndefinedValue();
	}
    }

    private class CommitFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length != 0)
		throw new EvaluatorException(
				"BasicTable.commit() takes no arguments.");
	    if (!needsCommit())
		return Context.getUndefinedValue();
	    try {
		ArrayList al = new ArrayList();
		al.add(BasicTable.this);
		getDatabase().commitTables(al);
	    } catch (NavigatorException e) {
		throw new EvaluatorException("Commit failed.");
	    }
	    return Context.getUndefinedValue();
	}
    }

    private class RollbackFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length != 0)
		throw new EvaluatorException(
				"BasicTable.rollback() takes no arguments.");
	    if (needsCommit())
		model.rollback();
	    return Context.getUndefinedValue();
	}
    }

    private class PK2RowFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length == 0)
		throw new EvaluatorException(
			    "BasicTable.pk2row() takes an array argument, or "
			    + "one or more arguments representing the "
			    + "individual key components.");
	    PrimaryKey pk = getPrimaryKey();
	    if (pk == null)
		return null;
	    int[] pkcol = getPKColumns();
	    Object[] key;
	    if (args.length == 1) {
		if (pkcol.length == 1)
		    key = new Object[] { args[0] };
		else if (args[0] instanceof Object[])
		    key = (Object[]) args[0];
		else if (args[0] instanceof Scriptable) {
		    Scriptable arg = (Scriptable) args[0];
		    ArrayList al = new ArrayList();
		    int i = 0;
		    Object obj;
		    while ((obj = arg.get(i, arg)) != Scriptable.NOT_FOUND) {
			al.add(obj);
			i++;
		    }
		    key = al.toArray();
		} else
		    return null;
	    } else
		key = args;
	    if (key.length != pk.getColumnCount())
		return null;
	    for (int i = 0; i < key.length; i++)
		if (key[i] == null)
		    return null;
	    outerloop:
	    for (int i = 0; i < model.getRowCount(); i++) {
		for (int j = 0; j < key.length; j++)
		    if (!key[j].equals(model.getValueAt(i, pkcol[j])))
			continue outerloop;
		return new Integer(i);
	    }
	    return null;
	}
    }

    private class Row2PKFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length != 1 || !(args[0] instanceof Number))
		throw new EvaluatorException(
			    "BasicTable.row2pk() takes one numeric argument.");
	    int row = ((Number) args[0]).intValue();
	    if (model == null || row < 0 || row >= model.getRowCount())
		return null;
	    PrimaryKey pk = getPrimaryKey();
	    if (pk == null)
		return null;
	    int[] pkcol = getPKColumns();
	    Object[] key = new Object[pkcol.length];
	    for (int i = 0; i < pkcol.length; i++)
		key[i] = model.getValueAt(row, pkcol[i]);
	    return new JavaScriptArray(key);
	}
    }

    private class FK2RowsFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length < 2 || !(args[0] instanceof Number))
		throw new EvaluatorException(
			    "BasicTable.fk2row() takes a number argument, plus "
			    + "an array argument, or one or more additional "
			    + "arguments representing the individual key "
			    + "components.");
	    ForeignKey[] fks = getForeignKeys();
	    int fkn = ((Number) args[0]).intValue();
	    if (fkn < 0 || fkn >= fks.length)
		return new Integer[0];
	    ForeignKey fk = fks[fkn];
	    String thatQName = fk.getThatQualifiedName();
	    Table thatTable;
	    int[] fkcol;
	    try {
		thatTable = getDatabase().getTable(thatQName);
		fkcol = getFKColumns(fkn, thatTable);
	    } catch (NavigatorException e) {
		throw new WrappedException(e);
	    }

	    Object[] key;
	    if (args.length == 2) {
		if (fkcol.length == 1)
		    key = new Object[] { args[1] };
		else if (args[1] instanceof Object[])
		    key = (Object[]) args[1];
		else if (args[1] instanceof Scriptable) {
		    Scriptable arg = (Scriptable) args[1];
		    ArrayList al = new ArrayList();
		    int i = 0;
		    Object obj;
		    while ((obj = arg.get(i, arg)) != Scriptable.NOT_FOUND) {
			al.add(obj);
			i++;
		    }
		    key = al.toArray();
		} else
		    return new Integer[0];
	    } else {
		key = new Object[args.length - 1];
		System.arraycopy(args, 1, key, 0, args.length - 1);
	    }

	    if (key.length != fk.getColumnCount())
		return new Integer[0];
	    ArrayList matchingRows = new ArrayList();
	    outerloop:
	    for (int i = 0; i < model.getRowCount(); i++) {
		for (int j = 0; j < key.length; j++) {
		    Object o1 = key[j];
		    Object o2 = model.getValueAt(i, fkcol[j]);
		    if (o1 == null ? o2 != null : !o1.equals(o2))
			continue outerloop;
		}
		matchingRows.add(new Integer(i));
	    }
	    int n = matchingRows.size();
	    return new JavaScriptArray(matchingRows.toArray(new Integer[n]));
	}
    }

    private class Row2FKFunction extends BasicFunction {
	public Object call(Object[] args) {
	    if (args.length != 2 || !(args[0] instanceof Number)
				 || !(args[1] instanceof Number))
		throw new EvaluatorException(
			    "BasicTable.row2fk() takes two numeric arguments.");
	    ForeignKey[] fks = getForeignKeys();
	    int fkn = ((Number) args[0]).intValue();
	    if (fkn < 0 || fkn >= fks.length)
		return null;
	    ForeignKey fk = fks[fkn];
	    int row = ((Number) args[1]).intValue();
	    if (model == null || row < 0 || row >= model.getRowCount())
		return null;
	    String thatQName = fk.getThatQualifiedName();
	    Table thatTable;
	    int[] fkcol;
	    try {
		thatTable = getDatabase().getTable(thatQName);
		fkcol = getFKColumns(fkn, thatTable);
	    } catch (NavigatorException e) {
		throw new WrappedException(e);
	    }
	    Object[] key = new Object[fkcol.length];
	    for (int i = 0; i < fkcol.length; i++)
		key[i] = model.getValueAt(row, fkcol[i]);
	    return new JavaScriptArray(key);
	}
    }


    private LoadFunction loadFunction = new LoadFunction();
    private AddRowFunction addRowFunction = new AddRowFunction();
    private RemoveRowFunction removeRowFunction = new RemoveRowFunction();
    private CommitFunction commitFunction = new CommitFunction();
    private RollbackFunction rollbackFunction = new RollbackFunction();
    private PK2RowFunction pk2RowFunction = new PK2RowFunction();
    private Row2PKFunction row2PKFunction = new Row2PKFunction();
    private FK2RowsFunction fk2RowsFunction = new FK2RowsFunction();
    private Row2FKFunction row2FKFunction = new Row2FKFunction();


    public void delete(int index) {
	//
    }

    public void delete(String name) {
	//
    }

    public Object get(int index, Scriptable start) {
	if (model == null || index < 0 || index >= model.getRowCount())
	    return NOT_FOUND;
	return new Row(index);
    }

    public Object get(String name, Scriptable start) {
	if (name.equals("name")) {
	    return getName();
	} else if (name.equals("load")) {
	    return loadFunction;
	} else if (name.equals("addRow")) {
	    return addRowFunction;
	} else if (name.equals("removeRow")) {
	    return removeRowFunction;
	} else if (name.equals("commit")) {
	    return commitFunction;
	} else if (name.equals("rollback")) {
	    return rollbackFunction;
	} else if (name.equals("pk2row")) {
	    return pk2RowFunction;
	} else if (name.equals("row2pk")) {
	    return row2PKFunction;
	} else if (name.equals("fk2rows")) {
	    return fk2RowsFunction;
	} else if (name.equals("row2fk")) {
	    return row2FKFunction;
	} else if (name.equals("length")) {
	    int n = model == null ? -1 : model.getRowCount();
	    return new Integer(n);
	} else if (name.equals("width")) {
	    int n = model == null ? -1 : model.getColumnCount();
	    return new Integer(n);
	} else if (name.equals("columns")) {
	    String[] colNames;
	    if (model == null)
		colNames = new String[0];
	    else {
		int ncols = model.getColumnCount();
		colNames = new String[ncols];
		for (int i = 0; i < ncols; i++)
		    colNames[i] = model.getColumnName(i);
	    }
	    return new JavaScriptArray(colNames);
	} else if (name.equals("pk")) {
	    return getPrimaryKey();
	} else if (name.equals("rks")) {
	    return new JavaScriptArray(getReferencingKeys());
	} else if (name.equals("fks")) {
	    return new JavaScriptArray(getForeignKeys());
	} else if (name.equals("indexes")) {
	    return new JavaScriptArray(getIndexes());
	} else
	    return NOT_FOUND;
    }

    public String getClassName() {
	return getClass().getName();
    }

    public Object getDefaultValue(Class hint) {
	if (model == null)
	    return "(not loaded)";

	int ncols = model.getColumnCount();
	int nrows = model.getRowCount();
	int[] colWidth = new int[ncols];
	for (int col = 0; col < ncols; col++)
	    colWidth[col] = model.getColumnName(col).length();
	for (int row = 0; row < nrows; row++) {
	    for (int col = 0; col < ncols; col++) {
		int w = chopString(model.getValueAt(row, col)).length();
		if (w > colWidth[col])
		    colWidth[col] = w;
	    }
	}

	StringBuffer buf = new StringBuffer();
	printSeparator(buf, colWidth);
	for (int col = 0; col < ncols; col++) {
	    String s = model.getColumnName(col);
	    buf.append("| ");
	    buf.append(s);
	    buf.append("                                                                                 ".substring(0, colWidth[col] + 1 - s.length()));
	}
	buf.append("|\n");
	printSeparator(buf, colWidth);
	for (int row = 0; row < nrows; row++) {
	    for (int col = 0; col < ncols; col++) {
		String s = chopString(model.getValueAt(row, col));
		buf.append("| ");
		buf.append(s);
		buf.append("                                                                                 ".substring(0, colWidth[col] + 1 - s.length()));
	    }
	    buf.append("|\n");
	}
	if (nrows > 0)
	    printSeparator(buf, colWidth);
	return buf.toString();
    }

    public Object[] getIds() {
	return new Object[] {
	    "addRow",
	    "columns",
	    "commit",
	    "fk2rows",
	    "fks",
	    "indexes",
	    "length",
	    "load",
	    "name",
	    "pk",
	    "pk2row",
	    "removeRow",
	    "rks",
	    "rollback",
	    "row2fk",
	    "row2pk",
	    "width"
	};
    }

    public Scriptable getParentScope() {
	return null;
    }

    public Scriptable getPrototype() {
	return null;
    }

    public boolean has(int index, Scriptable start) {
	return model != null && index >= 0 && index < model.getRowCount();
    }

    public boolean has(String name, Scriptable start) {
	if (name.equals("name"))
	    return true;
	else if (name.equals("load"))
	    return true;
	else if (name.equals("addRow"))
	    return true;
	else if (name.equals("removeRow"))
	    return true;
	else if (name.equals("commit"))
	    return true;
	else if (name.equals("rollback"))
	    return true;
	else if (name.equals("length"))
	    return true;
	else if (name.equals("width"))
	    return true;
	else if (name.equals("columns"))
	    return true;
	else if (name.equals("pk"))
	    return true;
	else if (name.equals("rks"))
	    return true;
	else if (name.equals("fks"))
	    return true;
	else if (name.equals("indexes"))
	    return true;
	else if (name.equals("pk2row"))
	    return true;
	else if (name.equals("row2pk"))
	    return true;
	else if (name.equals("fk2rows"))
	    return true;
	else if (name.equals("row2fk"))
	    return true;
	else
	    return false;
    }

    public boolean hasInstance(Scriptable instance) {
	return getClass().isInstance(instance);
    }

    public void put(int index, Scriptable start, Object value) {
	//
    }

    public void put(String name, Scriptable start, Object value) {
	//
    }

    public void setParentScope(Scriptable parentScope) {
	//
    }

    public void setPrototype(Scriptable prototype) {
	//
    }

    private static void printSeparator(StringBuffer buf, int[] colWidth) {
	for (int col = 0; col < colWidth.length; col++)
	    buf.append("+----------------------------------------------------------------------------------".substring(0, colWidth[col] + 3));
	buf.append("+\n");
    }

    private static String chopString(Object o) {
	String s = String.valueOf(o);
	StringBuffer buf = new StringBuffer();
	for (int i = 0; i < s.length(); i++) {
	    char c = s.charAt(i);
	    if (c == '\\')
		buf.append("\\\\");
	    else if (c >= 32 && c <= 126)
		buf.append(c);
	    else {
		if (c == 0)
		    buf.append("\\0");
		else if (c == '\n')
		    buf.append("\\n");
		else if (c == '\r')
		    buf.append("\\r");
		else if (c == '\t')
		    buf.append("\\t");
		else {
		    String octal = Integer.toString(c, 8);
		    buf.append("\\00".substring(0, 4 - octal.length()));
		    buf.append(octal);
		}
	    }
	    if (buf.length() >= 80)
		return buf.substring(0, 80);
	}
	return buf.toString();
    }

    private class Row implements Scriptable {
	private int rowIndex;
	public Row(int index) {
	    rowIndex = index;
	}
	public void delete(int colIndex) {
	    //
	}
	public void delete(String name) {
	    //
	}
	public Object get(int colIndex, Scriptable start) {
	    if (model == null)
		return NOT_FOUND;
	    if (colIndex < 0 || colIndex >= model.getColumnCount()
		    || rowIndex < 0 || rowIndex >= model.getRowCount())
		return NOT_FOUND;
	    return model.getValueAt(rowIndex, colIndex);
	}
	public Object get(String name, Scriptable start) {
	    if (model == null)
		return NOT_FOUND;
	    int ncols = model.getColumnCount();
	    if (name.equals("length"))
		return new Integer(ncols);
	    if (rowIndex < 0 || rowIndex >= model.getRowCount())
		return NOT_FOUND;
	    for (int col = 0; col < ncols; col++)
		if (model.getColumnName(col).equalsIgnoreCase(name))
		    return model.getValueAt(rowIndex, col);
	    return NOT_FOUND;
	}
	public String getClassName() {
	    return "Row";
	}
	public Object getDefaultValue(Class hint) {
	    if (model == null)
		return "(not loaded)";
	    int ncols = model.getColumnCount();
	    StringBuffer buf = new StringBuffer();
	    buf.append("[ ");
	    for (int col = 0; col < ncols; col++) {
		buf.append(model.getValueAt(rowIndex, col));
		buf.append(" "); 
	    }
	    buf.append("]");
	    return buf.toString();
	}
	public Object[] getIds() {
	    return new Object[] { "length" };
	}
	public Scriptable getParentScope() {
	    return null;
	}
	public Scriptable getPrototype() {
	    return null;
	}
	public boolean has(int colIndex, Scriptable start) {
	    if (model == null)
		return false;
	    return colIndex >= 0 && colIndex < model.getColumnCount()
		   && rowIndex >= 0 && rowIndex < model.getRowCount();
	}
	public boolean has(String name, Scriptable start) {
	    if (model == null)
		return false;
	    if (name.equals("length"))
		return true;
	    if (rowIndex < 0 || rowIndex >= model.getRowCount())
		return false;
	    int ncols = model.getColumnCount();
	    for (int col = 0; col < ncols; col++)
		if (model.getColumnName(col).equalsIgnoreCase(name))
		    return true;
	    return false;
	}
	public boolean hasInstance(Scriptable instance) {
	    return instance instanceof Row;
	}
	public void put(int colIndex, Scriptable start, Object value) {
	    if (model == null)
		return;
	    if (colIndex >= 0 && colIndex < model.getColumnCount()
		    && rowIndex >= 0 && rowIndex < model.getRowCount())
		model.stopEditing();
		model.setValueAt(value, rowIndex, colIndex);
	}
	public void put(String name, Scriptable start, Object value) {
	    if (model == null)
		return;
	    if (rowIndex < 0 || rowIndex >= model.getRowCount())
		return;
	    int ncols = model.getColumnCount();
	    for (int col = 0; col < ncols; col++)
		if (model.getColumnName(col).equalsIgnoreCase(name)) {
		    model.stopEditing();
		    model.setValueAt(value, rowIndex, col);
		}
	}
	public void setParentScope(Scriptable parentScope) {
	    //
	}
	public void setPrototype(Scriptable prototype) {
	    //
	}
    }
}
