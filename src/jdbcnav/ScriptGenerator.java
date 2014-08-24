///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2010  Thomas Okken
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

import java.text.*;
import java.util.*;

import jdbcnav.model.*;
import jdbcnav.util.MiscUtils;
import jdbcnav.util.NavigatorException;


public class ScriptGenerator {

    protected static final double LOG10_2 = Math.log(2) / Math.log(10);

    protected static final SimpleDateFormat dateFormat =
        new SimpleDateFormat("yyyy-MM-dd");
    protected static final SimpleDateFormat timeFormat =
        new SimpleDateFormat("HH:mm:ss");
    protected static final SimpleDateFormat dateTimeFormat =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
            case TypeSpec.INTERVAL_YS: {
                // Not a standard SQL type, so we use DAY TO SECOND instead,
                // and convert months to days by multiplying with 30.436875
                return "INTERVAL DAY(9) TO SECOND(" + td.size + ")";
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

    public String drop(Collection<Table> coll, boolean fqtn) {
        if (coll.isEmpty())
            return "";
        TreeSet<Table> set = new TreeSet<Table>(coll);
        StringBuffer buf = new StringBuffer();
        for (Table table : set) {
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
            Table table = set.first();
            set.remove(table);
            drop2(table, set, buf, fqtn);
        }
        return buf.toString();
    }

    private void drop2(Table table, TreeSet<Table> set, StringBuffer buf,
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

    public String create(Collection<Table> coll, boolean fqtn) {
        if (coll.isEmpty())
            return "";
        TreeSet<Table> set = new TreeSet<Table>(coll);
        StringBuffer buf = new StringBuffer();
        while (!set.isEmpty()) {
            Table table = set.first();
            set.remove(table);
            create2(table, set, buf, fqtn);
        }
        return buf.toString();
    }

    private void create2(Table table, TreeSet<Table> set, StringBuffer buf,
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
        boolean sameAsSource = name.equals(table.getDatabase()
                                                .getInternalDriverName());
        for (int i = 0; i < columns; i++) {
            if (comma)
                buf.append(",");
            else
                comma = true;
            buf.append("\n    ");
            buf.append(table.getColumnNames()[i]);
            buf.append(" ");
            if (sameAsSource)
                buf.append(table.getTypeSpecs()[i].native_representation);
            else
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

    public String keys(Collection<Table> coll, boolean fqtn) {
        if (coll.isEmpty())
            return "";
        TreeSet<Table> set = new TreeSet<Table>(coll);
        StringBuffer buf = new StringBuffer();
        for (Table table : set) {
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
    public String populate(Collection<Table> tables, boolean fqtn)
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
    public String diff(Collection<Table> oldtables, Collection<Table> newtables,
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
            this.buf.append(limitLineLength(buf.toString()));
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
            this.buf.append(limitLineLength(buf.toString()));
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
            this.buf.append(limitLineLength(buf.toString()));
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
        if (spec.type == TypeSpec.DATE) {
            return "date '" + spec.objectToString(obj) + "'";
        } else if (spec.type == TypeSpec.TIME) {
            return "time(" + spec.size + ") '"
                + spec.objectToString(obj) + "'";
        } else if (spec.type == TypeSpec.TIMESTAMP) {
            return "timestamp(" + spec.size + ") '"
                + spec.objectToString(obj) + "'";
        } else if (spec.type == TypeSpec.TIME_TZ) {
            // Not using spec.objectToString() here, because it displays the
            // time zone name in a human-readable format; for SQL code, we want
            // to print the zone offset instead.
            // TODO: What kind of time zone specifiers does SQL allow? It would
            // be nice to use an ID or name, rather than an offset.
            DateTime dt = (DateTime) obj;
            return "time(" + spec.size + ") with time zone '"
                + dt.toString(spec, DateTime.ZONE_OFFSET) + "'";
        } else if (spec.type == TypeSpec.TIMESTAMP_TZ) {
            // Not using spec.objectToString() here, because it displays the
            // time zone name in a human-readable format; for SQL code, we want
            // to print the zone offset instead.
            // TODO: What kind of time zone specifiers does SQL allow? It would
            // be nice to use an ID or name, rather than an offset.
            DateTime dt = (DateTime) obj;
            return "timestamp(" + spec.size + ") with time zone '"
                + dt.toString(spec, DateTime.ZONE_OFFSET) + "'";
        } else if (obj instanceof java.sql.Time) {
            // Just a fallback -- better use jdbcnav.model.DateTime
            return spec.native_representation
                + " '" + timeFormat.format((java.util.Date) obj) + "'";
        } else if (obj instanceof java.sql.Timestamp) {
            // Just a fallback -- better use jdbcnav.model.DateTime
            return spec.native_representation
                + " '" + dateTimeFormat.format((java.util.Date) obj) + "'";
        } else if (obj instanceof java.sql.Date) {
            // Just a fallback -- better use jdbcnav.model.DateTime
            return spec.native_representation
                + " '" + dateFormat.format((java.util.Date) obj) + "'";
        } else if (obj instanceof java.util.Date) {
            // Just a fallback -- better use jdbcnav.model.DateTime
            return spec.native_representation
                + " '" + dateTimeFormat.format((java.util.Date) obj) + "'";
        } else if (spec.type == TypeSpec.INTERVAL_YS) {
            Interval inter = (Interval) obj;
            long nanos = inter.months * 2629746000000000L + inter.nanos;
            inter = new Interval(0, nanos);
            return quote(inter.toString(TypeSpec.INTERVAL_DS, spec.size));
        } else if (obj instanceof ClobWrapper) {
            return quote(((ClobWrapper) obj).load());
        } else if (spec.type == TypeSpec.FIXED
                || spec.type == TypeSpec.FLOAT
                || obj instanceof Number) {
            return spec.objectToString(obj);
        } else {
            return quote(spec.objectToString(obj));
        }
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

    private static final String wordChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_$";

    // Oracle chokes on lines that are more than 2499 characters long.
    // Hence, a function to chop long commands into several lines.

    protected int maxLineLength() {
        // If we're ever going to override maxLineLength() in
        // ScriptGenerator_MySQL or ScriptGenerator_PostgreSQL,
        // limitLineLength() will need to handle PostgreSQL and MySQL escape
        // sequences -- these are generated for binary literals (e.g. bytea).
        // They look like \\nnn (3 octal digits) (PostgreSQL), or \c (where c
        // is one of 0'"bnrtZ\) (MySQL); there should never be a line break
        // within such a sequence.
        return -1;
    }

    private String limitLineLength(String s) {
        int maxlen = maxLineLength();
        if (maxlen == -1 || s.length() <= maxlen)
            return s;
        StringBuffer buf = new StringBuffer();
        StringBuffer wordbuf = new StringBuffer();
        // State: 0 = base, 1 = maybe number (just read '-'), 2 = inside
        // number, 3 = inside number just after 'e', 4 = inside word,
        // 5 = inside double-quoted word, 6 = inside string, 7 = maybe inside
        // string (previous state was 6 but we just read a single quote; if
        // the next char is another single quote, we append a single quote and
        // go back to state 6, and otherwise we go to state 0).
        int state = 0;
        int linelen = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (state) {
                case 0:
                    // Base state
                    if (c == '-') {
                        wordbuf.append(c);
                        state = 1;
                    } else if (c >= '0' && c <= '9') {
                        wordbuf.append(c);
                        state = 2;
                    } else if (wordChars.indexOf(c) != -1) {
                        wordbuf.append(c);
                        state = 4;
                    } else if (c == '"') {
                        wordbuf.append(c);
                        state = 5;
                    } else if (c == '\'') {
                        if (linelen > maxlen - 4) {
                            // I don't want to start a string literal if
                            // there's less than 4 positions left on the line;
                            // the idea is I want to avoid having to break the
                            // string while it's still empty. Consider the case
                            // of a string literal that starts with a single
                            // quote (i.e. '''Hello!''' -- what we would write
                            // "'Hello'" in Java).
                            buf.append('\n');
                            linelen = 0;
                        }
                        buf.append(c);
                        linelen++;
                        state = 6;
                    } else if (c != ' ' || linelen > 0) {
                        if (linelen == maxlen) {
                            buf.append('\n');
                            linelen = 0;
                        }
                        buf.append(c);
                        linelen++;
                    }
                    break;

                case 1:
                    // Maybe inside number (last char was '-')
                    if (c >= '0' && c <= '9' || c == '.') {
                        wordbuf.append(c);
                        state = 2;
                        break;
                    }
                    // Not a number; write the '-' that made us go into
                    // state 1 to begin with, and fall through to the base
                    // state.
                    if (linelen == maxlen) {
                        buf.append('\n');
                        linelen = 0;
                    }
                    buf.append('-');
                    linelen++;
                    wordbuf.setLength(0);
                    // Fall through to base state...
                    state = 0;
                    i--;
                    continue;

                case 2:
                    // Inside number
                    if (c >= '0' && c <= '9' || c == '.') {
                        wordbuf.append(c);
                    } else if (c == 'e' || c == 'E') {
                        wordbuf.append(c);
                        state = 3;
                    } else {
                        if (linelen + wordbuf.length() > maxlen) {
                            buf.append('\n');
                            linelen = 0;
                        }
                        buf.append(wordbuf);
                        linelen += wordbuf.length();
                        wordbuf.setLength(0);
                        // fall through to base state
                        state = 0;
                        i--;
                        continue;
                    }
                    break;
                        
                case 3:
                    // Inside number, just after 'e'
                    if (c >= '0' && c <= '9' || c == '.'
                                                || c == '-' || c == '+') {
                        // Resume number state
                        state = 2;
                        i--;
                        continue;
                    } else {
                        if (linelen + wordbuf.length() > maxlen) {
                            buf.append('\n');
                            linelen = 0;
                        }
                        buf.append(wordbuf);
                        linelen += wordbuf.length();
                        wordbuf.setLength(0);
                        // fall through to base state
                        state = 0;
                        i--;
                        continue;
                    }

                case 4:
                    // Inside word
                    if (wordChars.indexOf(c) != -1) {
                        wordbuf.append(c);
                    } else {
                        if (linelen + wordbuf.length() > maxlen) {
                            buf.append('\n');
                            linelen = 0;
                        }
                        buf.append(wordbuf);
                        linelen += wordbuf.length();
                        wordbuf.setLength(0);
                        // fall through to base state
                        state = 0;
                        i--;
                        continue;
                    }
                    break;

                case 5:
                    // Inside double-quoted word
                    wordbuf.append(c);
                    if (c == '"') {
                        if (linelen + wordbuf.length() > maxlen) {
                            buf.append('\n');
                            linelen = 0;
                        }
                        buf.append(wordbuf);
                        linelen += wordbuf.length();
                        wordbuf.setLength(0);
                        state = 0;
                    }
                    break;

                case 6:
                    // Inside string
                    if (c == '\'') {
                        state = 7;
                    } else {
                        if (linelen >= maxlen - 1) {
                            buf.append("'\n||'");
                            linelen = 3;
                        }
                        buf.append(c);
                        linelen++;
                    }
                    break;

                case 7:
                    // Maybe inside string (prev char was '\'')
                    if (c == '\'') {
                        if (linelen >= maxlen - 1) {
                            buf.append("'\n||'");
                            linelen = 3;
                        }
                        buf.append("''");
                        linelen += 2;
                        state = 6;
                    } else {
                        buf.append('\'');
                        linelen++;
                        // fall through to state 0
                        state = 0;
                        i--;
                        continue;
                    }
                    break;
            }
        }

        // Clean up...
        switch (state) {
            case 0:
                // Nothing to do
                break;
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                // Flush word buffer
                if (linelen + wordbuf.length() > maxlen)
                    buf.append('\n');
                buf.append(wordbuf);
                break;
            case 6:
                // Nothing to do
                break;
            case 7:
                // Write that last quote
                buf.append('\'');
                break;
        }

        return buf.toString();
    }

    private static Table findTable(TreeSet<Table> set, String catalog, String schema,
                                                                String name) {
        for (Table t : set)
            if (MiscUtils.strEq(catalog, t.getCatalog())
                    && MiscUtils.strEq(schema, t.getSchema())
                    && MiscUtils.strEq(name, t.getName()))
                return t;
        return null;
    }


    ////////////////////////////////////////////////////////////
    ///// Methods for finding and loading ScriptGenerators /////
    ////////////////////////////////////////////////////////////

    private static WeakHashMap<String, ScriptGenerator> instances = new WeakHashMap<String, ScriptGenerator>();

    public static ScriptGenerator getInstance(String name) {
        ScriptGenerator instance = instances.get(name);
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
