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
import jdbcnav.util.MiscUtils;
import jdbcnav.util.NavigatorException;


public class ScriptGenerator {

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
     * Oracle has its own, nonstandard ideas of how dates should look...
     */
    protected String dateToString(java.util.Date date) {
	return date.toString();
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
    protected String printType(Table table, int column) {
	String driver = table.getDatabase().getInternalDriverName();
	String name = table.getDbTypes()[column];
	Integer size = table.getColumnSizes()[column];
	Integer scale = table.getColumnScales()[column];

	if (driver.equals("Oracle")) {

	    if (name.equals("CHAR"))
		return "CHARACTER(" + size + ")";
	    else if (name.equals("VARCHAR2"))
		return "CHARACTER VARYING(" + size + ")";
	    else if (name.equals("NCHAR"))
		return "NATIONAL CHARACTER(" + size + ")";
	    else if (name.equals("NVARCHAR2"))
		return"NATIONAL CHARACTER VARYING(" + size + ")"; 
	    else if (name.equals("NUMBER")) {
		if (scale == null)
		    return "DOUBLE PRECISION";
		else if (scale.intValue() == 0)
		    return "NUMERIC(" + size + ")";
		else
		    return "NUMERIC(" + size + ", " + scale + ")";
	    } else if (name.equals("FLOAT"))
		// Not a true Oracle type?
		return "FLOAT(" + size + ")";
	    else if (name.equals("LONG"))
		return "LONGVARCHAR";
	    else if (name.equals("RAW"))
		return "VARBINARY(" + size + ")";
	    else if (name.equals("LONG RAW"))
		return "LONGVARBINARY";
	    else if (name.equals("DATE"))
		return "TIMESTAMP";
	    else if (name.equals("BLOB"))
		return "BLOB";
	    else if (name.equals("CLOB"))
		return "CLOB";
	    else if (name.equals("NCLOB"))
		// I'm just making this one up, but who knows, java.sql.Types
		// doesn't mention NATIONAL CHARACTER either. Who's lying,
		// java.sql.Types or the Oracle SQL reference, page 2-5? Eh.
		return "NATIONAL CLOB";
	    else if (name.equals("BFILE"))
		// I doubt if ANSI even has something like this... But I have
		// to return something.
		return "BFILE";
	    else if (name.equals("ROWID"))
		return "ROWID";
	    else if (name.equals("UROWID"))
		return "UROWID(" + size + ")";
	    else {
		// Unexpected value... Print as is and hope the user can
		// straighten out the SQL script manually.
		if (size == null)
		    return name;
		else if (scale == null)
		    return name + "(" + size + ")";
		else
		    return name + "(" + size + ", " + scale + ")";
	    }

	} else if (driver.equals("PostgreSQL")) {

	    if (name.equals("biginit")
		    || name.equals("int8"))
		return "BIGINT";
	    else if (name.equals("integer")
		    || name.equals("int")
		    || name.equals("int4"))
		return "INTEGER";
	    else if (name.equals("smallint")
		    || name.equals("int2"))
		return "SMALLINT";
	    else if (name.equals("real")
		    || name.equals("float4"))
		return "REAL";
	    else if (name.equals("double precision")
		    || name.equals("float8"))
		return "DOUBLE PRECISION";
	    else if (name.equals("numeric")
		    || name.equals("decimal")) {
		if (size.intValue() == 65535 && scale.intValue() == 65531)
		    // Is there an ANSI way to specify NUMERIC
		    // without scale coercion?
		    return "DOUBLE PRECISION";
		else if (scale.intValue() == 0)
		    return "NUMERIC(" + size + ")";
		else
		    return "NUMERIC(" + size + ", " + scale + ")";
	    } else if (name.equals("date"))
		return "DATE";
	    else if (name.equals("time"))
		return "TIME";
	    else if (name.equals("timestamp"))
		return "TIMESTAMP";
	    else if (name.equals("bytea"))
		return "BLOB"; // VARBINARY? LONGVARBINARY?
	    else if (name.equals("char")
		    || name.equals("character"))
		return "CHARACTER(" + size + ")";
	    else if (name.equals("varchar")
		    || name.equals("character varying"))
		if (size.intValue() == 0)
		    return "CLOB"; // LONGVARCHAR?
		else
		    return "CHARACTER VARYING(" + size + ")";
	    else if (name.equals("text"))
		return "CLOB"; // LONGVARCHAR?
	    else {
		// Unsupported value, such as one of PostgreSQL's geometric
		// data types... Return what we can and let the user try to
		// figure it out.
		if (size == null)
		    return name;
		else if (scale == null)
		    return name + "(" + size + ")";
		else
		    return name + "(" + size + ", " + scale + ")";
	    }

	} else if (driver.equals("SmallSQL")) {

	    if (name.equals("BIT")
		    || name.equals("BOOLEAN"))
		return "BOOLEAN";
	    else if (name.equals("TINYINT")
		    || name.equals("BYTE"))
		return "TINYINT";
	    else if (name.equals("SMALLINT"))
		return "SMALLINT";
	    else if (name.equals("INT"))
		return "INTEGER";
	    else if (name.equals("BIGINT"))
		return "BIGINT";
	    else if (name.equals("REAL"))
		return "REAL";
	    else if (name.equals("DOUBLE")
		    || name.equals("FLOAT"))
		return "DOUBLE PRECISION";
	    else if (name.equals("MONEY"))
		return "NUMERIC(19, 4)";
	    else if (name.equals("SMALLMONEY"))
		return "NUMERIC(10, 4)";
	    else if (name.equals("NUMERIC")
		    || name.equals("DECIMAL")
		    || name.equals("NUMBER")
		    || name.equals("VARNUM")) {
		if (scale == null)
		    return "NUMERIC(" + size + ")";
		else
		    return "NUMERIC(" + size + ", " + scale + ")";
	    } else if (name.equals("CHAR")
		    || name.equals("CHARACTER")
		    || name.equals("NCHAR"))
		return "CHARACTER(" + size + ")";
	    else if (name.equals("VARCHAR")
		    || name.equals("NVARCHAR")
		    || name.equals("VARCHAR2")
		    || name.equals("NVARCHAR2"))
		return "CHARACTER VARYING(" + size + ")";
	    else if (name.equals("LONGVARCHAR")
		    || name.equals("TEXT")
		    || name.equals("LONG"))
		return "LONGVARCHAR";
	    else if (name.equals("LONGNVARCHAR")
		    || name.equals("NTEXT"))
		return "LONGNVARCHAR"; // NATIONAL CLOB? (See the Oracle case)
	    else if (name.equals("CLOB"))
		return "CLOB";
	    // The following are types not mentioned in the SmallSQL doc,
	    // but which do occur in the sample database...
	    else if (name.equals("BINARY"))
		return "BINARY(" + size + ")"; // Is this SQL?
	    else if (name.equals("VARBINARY"))
		return "VARBINARY(" + size + ")";
	    else if (name.equals("LONGVARBINARY"))
		return "LONGVARBINARY";
	    else if (name.equals("DATETIME")
		    || name.equals("SMALLDATETIME"))
		return "TIMESTAMP";
	    else {
		// Unexpected value... Print as is and hope the user can
		// straighten out the SQL script manually.
		System.out.println("Unrecognized: \"" + name + "\"");
		if (size == null)
		    return name;
		else if (scale == null)
		    return name + "(" + size + ")";
		else
		    return name + "(" + size + ", " + scale + ")";
	    }

	} else { // driver = "Generic" or unknown

	    int sqlType = table.getSqlTypes()[column];
	    String sqlName = MiscUtils.sqlTypeIntToString(sqlType);
	    if (size == null)
		return sqlName;
	    else if (scale == null)
		return sqlName + "(" + size + ")";
	    else
		return sqlName + "(" + size + ", " + scale + ")";
	
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
	    buf.append(printType(table, i));
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
		if (row[i] == null)
		    buf.append("null");
		else
		    buf.append(toSqlString(row[i]));
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
		buf.append(headers[pkColumns[i]]);
		// null is possible if this is a surrogate primary key
		if (key[i] == null)
		    buf.append(" is null");
		else {
		    buf.append(" = ");
		    buf.append(toSqlString(key[i]));
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
		    if (newRow[i] == null)
			buf.append("null");
		    else
			buf.append(toSqlString(newRow[i]));
		}
	    buf.append(" where");
	    int[] pkColumns = table.getPKColumns();
	    for (int i = 0; i < pkColumns.length; i++) {
		if (i > 0)
		    buf.append(" and");
		buf.append(" ");
		buf.append(headers[pkColumns[i]]);
		// No need to deal with null since we never perform updates
		// on tables with surrogate primary keys
		buf.append(" = ");
		buf.append(toSqlString(oldRow[pkColumns[i]]));
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

    private String toSqlString(Object obj) {
	if (obj instanceof Number)
	    return obj.toString();
	if (obj instanceof java.util.Date)
	    return dateToString((java.util.Date) obj);
	StringTokenizer tok = new StringTokenizer(obj.toString(),
						  "'\t\n\r", true);
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
    private static ScriptGenerator genericInstance = new ScriptGenerator();

    public static String[] getNames() {
	return new String[] { "Generic", /*"DBTypes",*/ "Oracle",
			      "PostgreSQL", "SmallSQL" };
    }

    public static ScriptGenerator getInstance(String name) {
	if (name.equals("Generic"))
	    return genericInstance;

	ScriptGenerator instance = (ScriptGenerator) instances.get(name);
	if (instance != null)
	    return instance;
	try {
	    instance = (ScriptGenerator) Class.forName(
		    "jdbcnav.ScriptGenerator_"
		    + name).newInstance();
	    instances.put(name, instance);
	    return instance;
	} catch (Exception e) {
	    MessageBox.show("Could not load ScriptGenerator \"" + name
			    + "\";\n" + "using \"Generic\" instead.", e);
	    return genericInstance;
	}
    }

    public final String getName() {
	if (getClass() == ScriptGenerator.class)
	    return "Generic";
	else {
	    String fullName = getClass().getName();
	    return fullName.substring(fullName.lastIndexOf("_") + 1);
	}
    }
}
