package jdbcnav;

import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import jdbcnav.model.Data;
import jdbcnav.model.ForeignKey;
import jdbcnav.model.Index;
import jdbcnav.model.PrimaryKey;
import jdbcnav.model.Table;
import jdbcnav.model.TypeSpec;
import jdbcnav.util.MiscUtils;
import jdbcnav.util.NavigatorException;


public class ScriptGenerator {

    protected static final double LOG10_2 = Math.log(2) / Math.log(10);

    private String name;

    ////////////////////////
    ///// Overridables /////
    ////////////////////////

    /**
     * Oracle needs "set scan off;" at the start of SQL scripts; otherwise,
     * if you feed the scripts to SQL*Plus, it will interpret certain entities
     * as variables and substitute them.
     */
    protected String getSQLPreamble() {
	return "";
    }

    /**
     * Oracle does not allow "on update" clauses
     */
    protected String onUpdateString(String upd) {
	return upd;
    }

    /**
     * Oracle does not allow "on delete" clauses other than "on delete cascade"
     */
    protected String onDeleteString(String del) {
	return del;
    }

    /**
     * Every database has its own needs when it comes to representing its
     * data types in SQL scripts... printType() should take a table's
     * originating driver (table.getDatabase().getIntenalDriverName()) into
     * account when trying to find the best way to represent the originating
     * data type in terms of the target's SQL dialect.
     */
    protected String printType(TypeSpec td) {
	switch (td.type) {
	    case TypeSpec.UNKNOWN: {
		return td.native_representation;
	    }
	    case TypeSpec.FIXED: {
		if (td.size_in_bits && td.scale == 0) {
		    if (td.size <= 16)
			return "SMALLINT";
		    else if (td.size <= 32)
			return "INTEGER";
		    else if (td.size <= 64)
			return "BIGINT";
		}

		int size;
		if (td.size_in_bits)
		    size = (int) Math.ceil(td.size * LOG10_2);
		else
		    size = td.size;

		int scale;
		if (td.scale_in_bits)
		    scale = (int) Math.ceil(td.scale * LOG10_2);
		else
		    scale = td.scale;

		if (scale == 0)
		    return "NUMERIC(" + size + ")";
		else
		    return "NUMERIC(" + size + ", " + scale + ")";
	    }
	    case TypeSpec.FLOAT: {
		// TODO: Generate ANSI-type FLOAT(n) as well, where
		// 'n' is the number of bits in the mantissa.
		// (Not sure if that number includes the sign bit or not.)
		if (td.size > (td.size_in_bits ? 24 : 7))
		    return "DOUBLE PRECISION";
		if (td.exp_of_2) {
		    if (td.min_exp < -127 || td.max_exp > 127)
			return "DOUBLE PRECISION";
		} else {
		    if (td.min_exp < -38 || td.max_exp > 38)
			return "DOUBLE PRECISION";
		}
		return "REAL";
	    }
	    case TypeSpec.CHAR: {
		return "CHAR(" + td.size + ")";
	    }
	    case TypeSpec.VARCHAR: {
		return "CHAR VARYING(" + td.size + ")";
	    }
	    case TypeSpec.LONGVARCHAR: {
		// TODO: is this SQL92?
		return "CLOB";
	    }
	    case TypeSpec.NCHAR: {
		return "NCHAR(" + td.size + ")";
	    }
	    case TypeSpec.VARNCHAR: {
		return "NCHAR VARYING(" + td.size + ")";
	    }
	    case TypeSpec.LONGVARNCHAR: {
		// TODO: is this SQL92?
		return "NCLOB";
	    }
	    case TypeSpec.RAW:
	    case TypeSpec.VARRAW: {
		// TODO: is this SQL92?
		return "RAW(" + td.size + ")";
	    }
	    case TypeSpec.LONGVARRAW: {
		// TODO: is this SQL92?
		return "BLOB";
	    }
	    case TypeSpec.DATE: {
		return "DATE";
	    }
	    case TypeSpec.TIME: {
		return "TIME(" + td.size + ")";
	    }
	    case TypeSpec.TIME_TZ: {
		return "TIME(" + td.size + ") WITH TIME ZONE";
	    }
	    case TypeSpec.TIMESTAMP: {
		return "TIMESTAMP(" + td.size + ")";
	    }
	    case TypeSpec.TIMESTAMP_TZ: {
		return "TIMESTAMP(" + td.size + ") WITH TIME ZONE";
	    }
	    case TypeSpec.INTERVAL_YM: {
		return "INTERVAL YEAR(" + td.size + ") TO MONTH";
	    }
	    case TypeSpec.INTERVAL_DS: {
		return "INTERVAL DAY(" +td.size+ ") TO SECOND(" +td.scale+ ")";
	    }
	    default: {
		// TODO - Warning (internal error); should never get here
		return td.native_representation;
	    }
	}
    }


    //////////////////////////
    ///// Public methods /////
    //////////////////////////

    public String drop(Collection coll, boolean fqtn) {
	if (coll.isEmpty())
	    return "";
	TreeSet set = new TreeSet(coll);
	StringBuffer buf = new StringBuffer();
	for (Iterator iter = set.iterator(); iter.hasNext();) {
	    Table table = (Table) iter.next();
	    ForeignKey[] rks = table.getReferencingKeys();
	    for (int i = 0; i < rks.length; i++) {
		ForeignKey rk = rks[i];
		Table t2 = findTable(set, rk.getThatCatalog(),
					  rk.getThatSchema(),
					  rk.getThatName());
		if (t2 == null) {
		    buf.append("alter table ");
		    if (fqtn)
			buf.append(rk.getThatQualifiedName());
		    else
			buf.append(rk.getThatName());
		    buf.append(" drop constraint ");
		    buf.append(rk.getThatKeyName());
		    buf.append(";\n");
		}
	    }
	}
	while (!set.isEmpty()) {
	    Table table = (Table) set.first();
	    set.remove(table);
	    drop2(table, set, buf, fqtn);
	}
	return buf.toString();
    }

    private void drop2(Table table, TreeSet set, StringBuffer buf,
							boolean fqtn) {
	ForeignKey[] rks = table.getReferencingKeys();
	for (int i = 0; i < rks.length; i++) {
	    ForeignKey rk = rks[i];
	    Table t2 = findTable(set, rk.getThatCatalog(),
				      rk.getThatSchema(),
				      rk.getThatName());
	    if (t2 == null)
		continue;
	    if (set.contains(t2)) {
		set.remove(t2);
		drop2(t2, set, buf, fqtn);
	    }
	}
	buf.append("drop table ");
	if (fqtn)
	    buf.append(table.getQualifiedName());
	else
	    buf.append(table.getQuotedName());
	buf.append(";\n");
    }

    public String create(Collection coll, boolean fqtn) {
	if (coll.isEmpty())
	    return "";
	TreeSet set = new TreeSet(coll);
	StringBuffer buf = new StringBuffer();
	while (!set.isEmpty()) {
	    Table table = (Table) set.first();
	    set.remove(table);
	    create2(table, set, buf, fqtn);
	}
	return buf.toString();
    }

    private void create2(Table table, TreeSet set, StringBuffer buf,
			    boolean fqtn) {
	ForeignKey[] fks = table.getForeignKeys();
	for (int i = 0; i < fks.length; i++) {
	    ForeignKey fk = fks[i];
	    Table t2 = findTable(set, fk.getThatCatalog(),
				      fk.getThatSchema(),
				      fk.getThatName());
	    if (t2 == null)
		continue;
	    if (set.contains(t2)) {
		set.remove(t2);
		create2(t2, set, buf, fqtn);
	    }
	}
	buf.append("create table ");
	if (fqtn)
	    buf.append(table.getQualifiedName());
	else
	    buf.append(table.getQuotedName());
	buf.append("\n(");

	int columns = table.getColumnCount();
	boolean comma = false;
	for (int i = 0; i < columns; i++) {
	    if (comma)
		buf.append(",");
	    else
		comma = true;
	    buf.append("\n    ");
	    buf.append(table.getColumnNames()[i]);
	    buf.append(" ");
	    buf.append(printType(table.getTypeSpecs()[i]));
	    if (!"YES".equals(table.getIsNullable()[i]))
		buf.append(" not null");
	}

	PrimaryKey pk = table.getPrimaryKey();
	if (pk != null) {
	    buf.append(",\n    ");
	    if (pk.getName() != null) {
		buf.append("constraint ");
		buf.append(pk.getName());
		buf.append(" ");
	    }
	    buf.append("primary key (");
	    for (int i = 0; i < pk.getColumnCount(); i++) {
		if (i > 0)
		    buf.append(", ");
		buf.append(pk.getColumnName(i));
	    }
	    buf.append(")");
	}
	// Still have foreign keys from way back at the start of this
	// method
	for (int i = 0; i < fks.length; i++) {
	    ForeignKey fk = fks[i];
	    buf.append(",\n    ");
	    if (fk.getThisKeyName() != null) {
		buf.append("constraint ");
		buf.append(fk.getThisKeyName());
		buf.append(" ");
	    }
	    buf.append("foreign key (");
	    for (int j = 0; j < fk.getColumnCount(); j++) {
		if (j > 0)
		    buf.append(", ");
		buf.append(fk.getThisColumnName(j));
	    }
	    buf.append(")\n        references ");
	    if (fqtn)
		buf.append(fk.getThatQualifiedName());
	    else
		buf.append(fk.getThatName());
	    buf.append("(");
	    for (int j = 0; j < fk.getColumnCount(); j++) {
		if (j > 0)
		    buf.append(", ");
		buf.append(fk.getThatColumnName(j));
	    }
	    buf.append(")");
	    String upd = onUpdateString(fk.getUpdateRule());
	    String del = onDeleteString(fk.getDeleteRule());
	    if (upd != null || del != null) {
		buf.append("\n       ");
		if (upd != null) {
		    buf.append(" on update ");
		    buf.append(upd);
		}
		if (del != null) {
		    buf.append(" on delete ");
		    buf.append(del);
		}
	    }
	}
	buf.append("\n);\n");
	Index[] indexes = table.getIndexes();
	for (int i = 0; i < indexes.length; i++) {
	    Index index = indexes[i];
	    buf.append("create ");
	    if (index.isUnique())
		buf.append("unique ");
	    buf.append("index ");
	    buf.append(index.getName());
	    buf.append(" on ");
	    if (fqtn)
		buf.append(table.getQualifiedName());
	    else
		buf.append(table.getQuotedName());
	    buf.append("(");
	    for (int j = 0; j < index.getColumnCount(); j++) {
		if (j > 0)
		    buf.append(", ");
		buf.append(index.getColumnName(j));
	    }
	    buf.append(");\n");
	}
    }

    public String keys(Collection coll, boolean fqtn) {
	if (coll.isEmpty())
	    return "";
	TreeSet set = new TreeSet(coll);
	StringBuffer buf = new StringBuffer();
	for (Iterator iter = set.iterator(); iter.hasNext();) {
	    Table table = (Table) iter.next();
	    ForeignKey[] rks = table.getReferencingKeys();
	    for (int i = 0; i < rks.length; i++) {
		ForeignKey rk = rks[i];
		Table t2 = findTable(set, rk.getThatCatalog(),
					  rk.getThatSchema(),
					  rk.getThatName());
		if (t2 == null) {
		    buf.append("alter table ");
		    if (fqtn)
			buf.append(rk.getThatQualifiedName());
		    else
			buf.append(rk.getThatName());
		    buf.append(" add");
		    if (rk.getThatKeyName() != null) {
			buf.append(" constraint ");
			buf.append(rk.getThatKeyName());
		    }
		    buf.append(" foreign key (");
		    for (int j = 0; j < rk.getColumnCount(); j++) {
			if (j > 0)
			    buf.append(", ");
			buf.append(rk.getThatColumnName(j));
		    }
		    buf.append(")\n    references ");
		    if (fqtn)
			buf.append(table.getQualifiedName());
		    else
			buf.append(table.getQuotedName());
		    buf.append("(");
		    for (int j = 0; j < rk.getColumnCount(); j++) {
			if (j > 0)
			    buf.append(", ");
			buf.append(rk.getThisColumnName(j));
		    }
		    buf.append(")");
		    String upd = onUpdateString(rk.getUpdateRule());
		    String del = onDeleteString(rk.getDeleteRule());
		    if (upd != null || del != null) {
			buf.append("\n       ");
			if (upd != null) {
			    buf.append(" on update ");
			    buf.append(upd);
			}
			if (del != null) {
			    buf.append(" on delete ");
			    buf.append(del);
			}
		    }
		    buf.append(";\n");
		}
	    }
	}
	return buf.toString();
    }

    /**
     * NOTE: this method may cause synchronous loading of tables, or
     * it may wait for tables that are in the process of being loaded
     * asynchronously to finish. Do not call this method from the AWT
     * event thread, or your UI may freeze for a long time.
     */
    public String populate(Collection tables, boolean fqtn)
						throws NavigatorException {
	DiffCallback dcb = new DiffCallback(fqtn);
	MultiTableDiff.populate(dcb, tables, true);
	return dcb.toString();
    }

    /**
     * NOTE: this method may cause synchronous loading of tables, or
     * it may wait for tables that are in the process of being loaded
     * asynchronously to finish. Do not call this method from the AWT
     * event thread, or your UI may freeze for a long time.
     */
    public String diff(Collection oldtables, Collection newtables,
				    boolean fqtn) throws NavigatorException {
	DiffCallback dcb = new DiffCallback(fqtn);
	MultiTableDiff.diff(dcb, oldtables, newtables, true);
	return dcb.toString();
    }


    ////////////////////////
    ///// DiffCallback /////
    ////////////////////////

    private class DiffCallback implements TableChangeHandler {
	private boolean fqtn;
	private boolean postmortem;
	private StringBuffer buf;

	public DiffCallback(boolean fqtn) {
	    this.fqtn = fqtn;
	    postmortem = false;
	    buf = new StringBuffer();
	    buf.append(getSQLPreamble());
	}

	public void insertRow(Table table, Object[] row)
						    throws NavigatorException {
	    StringBuffer buf = new StringBuffer();
	    buf.append("insert into ");
	    if (fqtn)
		buf.append(table.getQualifiedName());
	    else
		buf.append(table.getQuotedName());
	    buf.append("(");
	    for (int i = 0; i < row.length; i++) {
		if (i > 0)
		    buf.append(", ");
		buf.append(table.getColumnNames()[i]);
	    }
	    buf.append(") values (");
	    for (int i = 0; i < row.length; i++) {
		if (i > 0)
		    buf.append(", ");
		buf.append(toSqlString(table.getTypeSpecs()[i], row[i]));
	    }
	    buf.append(");\n");
	    if (postmortem)
		this.buf.append("-- ");
	    this.buf.append(limitLineLength(buf.toString(), 1000));
	}

	public void deleteRow(Table table, Object[] key)
						    throws NavigatorException {
	    StringBuffer buf = new StringBuffer();
	    String[] headers = table.getColumnNames();
	    buf.append("delete from ");
	    if (fqtn)
		buf.append(table.getQualifiedName());
	    else
		buf.append(table.getQuotedName());
	    buf.append(" where");
	    int[] pkColumns = table.getPKColumns();
	    for (int i = 0; i < key.length; i++) {
		if (i > 0)
		    buf.append(" and");
		buf.append(" ");
		int col = pkColumns[i];
		buf.append(headers[col]);
		// null is possible if this is a surrogate primary key
		String s = toSqlString(table.getTypeSpecs()[col], key[i]);
		if (s.equals("null"))
		    buf.append(" is null");
		else {
		    buf.append(" = ");
		    buf.append(s);
		}
	    }
	    buf.append(";\n");
	    if (postmortem)
		this.buf.append("-- ");
	    this.buf.append(limitLineLength(buf.toString(), 1000));
	}

	public void updateRow(Table table, Object[] oldRow, Object[] newRow)
						    throws NavigatorException {
	    StringBuffer buf = new StringBuffer();
	    String[] headers = table.getColumnNames();
	    buf.append("update ");
	    if (fqtn)
		buf.append(table.getQualifiedName());
	    else
		buf.append(table.getQuotedName());
	    buf.append(" set");
	    boolean comma = false;
	    for (int i = 0; i < newRow.length; i++)
		if (newRow[i] == null ? oldRow[i] != null
				: !newRow[i].equals(oldRow[i])) {
		    if (comma)
			buf.append(",");
		    else
			comma = true;
		    buf.append(" ");
		    buf.append(headers[i]);
		    buf.append(" = ");
		    buf.append(toSqlString(table.getTypeSpecs()[i], newRow[i]));
		}
	    buf.append(" where");
	    int[] pkColumns = table.getPKColumns();
	    for (int i = 0; i < pkColumns.length; i++) {
		if (i > 0)
		    buf.append(" and");
		int col = pkColumns[i];
		buf.append(" ");
		buf.append(headers[col]);
		// No need to deal with null since we never perform updates
		// on tables with surrogate primary keys
		buf.append(" = ");
		buf.append(toSqlString(table.getTypeSpecs()[col], oldRow[col]));
	    }
	    buf.append(";\n");
	    if (postmortem)
		this.buf.append("-- ");
	    this.buf.append(limitLineLength(buf.toString(), 1000));
	}

	public boolean continueAfterError() {

	    buf.insert(0, "------------------------------------------\n"
			+ "--   An internal error has occurred.    --\n"
			+ "--      This script is NOT valid!       --\n"
			+ "-- Use it only to debug JDBC Navigator. --\n"
			+ "------------------------------------------\n"
			+ "\n");

	    buf.append("\n");
	    buf.append("-----------------------------------------\n");
	    buf.append("-- Post-failure part of script follows --\n");
	    buf.append("-----------------------------------------\n");
	    buf.append("\n");

	    // Returning 'true' means continue; we want a script for postmortem
	    // debugging.

	    postmortem = true;
	    return true;
	}

	public String toString() {
	    if (postmortem) {
		buf.append("\n"
			 + "------------------------------------------\n"
			 + "--   An internal error has occurred.    --\n"
			 + "--      This script is NOT valid!       --\n"
			 + "-- Use it only to debug JDBC Navigator. --\n"
			 + "------------------------------------------\n");
	    }
	    return buf.toString();
	}
    }


    /////////////////////////////////
    ///// Private utility stuff /////
    /////////////////////////////////

    protected String toSqlString(TypeSpec spec, Object obj) {
	if (obj == null)
	    return "null";
	else if (spec.type == TypeSpec.FIXED || spec.type == TypeSpec.FLOAT)
	    return spec.objectToString(obj);
	else
	    return quote(spec.objectToString(obj));
    }

    protected String quote(String s) {
	if (s == null)
	    return "null";
	StringTokenizer tok = new StringTokenizer(s, "'\t\n\r", true);
	StringBuffer buf = new StringBuffer();
	boolean inLiteral = false;
	while (tok.hasMoreTokens()) {
	    String t = tok.nextToken();
	    if (t.equals("'")) {
		if (!inLiteral) {
		    if (buf.length() > 0)
			buf.append("||");
		    buf.append("'");
		    inLiteral = true;
		}
		buf.append("''");
	    } else if (t.equals("\t") || t.equals("\n") || t.equals("\r")) {
		if (inLiteral) {
		    buf.append("'||");
		    inLiteral = false;
		} else if (buf.length() > 0)
		    buf.append("||");
		buf.append("chr(");
		buf.append(((int) t.charAt(0)));
		buf.append(")");
	    } else {
		if (!inLiteral) {
		    if (buf.length() > 0)
			buf.append("||");
		    buf.append("'");
		    inLiteral = true;
		}
		buf.append(t);
	    }
	}
	if (inLiteral)
	    buf.append("'");
	return buf.toString();
    }

    // Oracle chokes on lines that are more than 2499 characters long.
    // Hence, a function to chop long commands into several lines.

    private String limitLineLength(String s, int maxlen) {
	if (s.length() <= maxlen)
	    return s;
	StringTokenizer tok = new StringTokenizer(s, "'", true);
	StringBuffer buf = new StringBuffer();
	// State: 0 = outside literal, 1 = inside literal, 2 = maybe inside
	// literal (previous state was 1 but we just read a single quote;
	// if the next char is another single quote, we go back to state 1,
	// and otherwise we go to state 0).
	int state = 0;
	int linelen = 0;
	while (tok.hasMoreTokens()) {
	    String t = tok.nextToken();
	    if (t.equals("'")) {
		switch (state) {
		    case 0:
			if (linelen > maxlen - 4) {
			    buf.append("\n");
			    linelen = 0;
			}
			buf.append(t);
			linelen++;
			state = 1;
			break;
		    case 1:
			state = 2;
			break;
		    case 2:
			if (linelen > maxlen - 3) {
			    buf.append("'\n||'''");
			    linelen = 5;
			} else {
			    buf.append("''");
			    linelen += 2;
			}
			state = 1;
			break;
		}
	    } else {
		switch (state) {
		    case 2:
			buf.append("'");
			linelen++;
			state = 0;
			// Fall through to case 0
		    case 0:
			// Note: we're assuming that tokens outside literals
			// are never too long to fit on a line; this may not
			// always be true (imagine a horrendously long
			// expression like chr(10)||chr(10)||...) but I'm not
			// dealing with it right now because it could be tricky
			// to handle (where can you insert a line break safely?
			// you'd have to know where SQL tokens begin and end...
			// Sigh.) So, that's a TODO.
			if (linelen + t.length() > maxlen) {
			    buf.append("\n");
			    linelen = 0;
			}
			buf.append(t);
			linelen += t.length();
			break;
		    case 1:
			if (linelen + t.length() < maxlen) {
			    // Let's get the easy case out of the way
			    // first
			    buf.append(t);
			    linelen += t.length();
			    break;
			}
			int n = maxlen - linelen - 1;
			buf.append(t.substring(0, n));
			buf.append("'\n||'");
			linelen = 3;
			int pos = n;
			int remaining = t.length() - n;
			while (remaining > 0) {
			    n = remaining;
			    if (n > maxlen - 4)
				n = maxlen - 4;
			    buf.append(t.substring(pos, pos + n));
			    linelen += n;
			    remaining -= n;
			    pos += n;
			    if (remaining > 0) {
				buf.append("'\n||'");
				linelen = 3;
			    }
			}
			break;
		}
	    }
	}
	if (state == 2)
	    buf.append("'");
	return buf.toString();
    }

    private static Table findTable(TreeSet set, String catalog, String schema,
								String name) {
	for (Iterator iter = set.iterator(); iter.hasNext();) {
	    Table t = (Table) iter.next();
	    if (MiscUtils.strEq(catalog, t.getCatalog())
		    && MiscUtils.strEq(schema, t.getSchema())
		    && MiscUtils.strEq(name, t.getName()))
		return t;
	}
	return null;
    }


    ////////////////////////////////////////////////////////////
    ///// Methods for finding and loading ScriptGenerators /////
    ////////////////////////////////////////////////////////////

    private static WeakHashMap instances = new WeakHashMap();

    public static ScriptGenerator getInstance(String name) {
	ScriptGenerator instance = (ScriptGenerator) instances.get(name);
	if (instance != null)
	    return instance;
	String className = null;
	try {
	    className = InternalDriverMap.getScriptGeneratorClassName(name);
	    instance = (ScriptGenerator) Class.forName(className).newInstance();
	    instance.name = name;
	    instances.put(name, instance);
	    return instance;
	} catch (Exception e) {
	    // Should never happen -- we can only get called with
	    // internal driver names that match an existing ScriptGenerator
	    // class.
	    MessageBox.show("Could not load ScriptGenerator \"" + name
		    + "\" (class " + className + ").", e);
	    return null;
	}
    }

    public final String getName() {
	return name;
    }
}
