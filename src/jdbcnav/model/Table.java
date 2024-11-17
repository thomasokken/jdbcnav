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

package jdbcnav.model;

import jdbcnav.ResultSetTableModel;
import jdbcnav.util.NavigatorException;


public interface Table extends Comparable<Table> {
    Database getDatabase();
    String getCatalog();
    String getSchema();
    String getName();
    String getType();
    String getRemarks();
    String getQualifiedName();
    String getQuotedName();
    int getColumnCount();
    String[] getColumnNames();
    TypeSpec[] getTypeSpecs();
    String[] getIsNullable();
    String[] getDefaults();
    String[] getIsGenerated();
    PrimaryKey getPrimaryKey();
    ForeignKey[] getForeignKeys();
    ForeignKey[] getReferencingKeys();
    Index[] getIndexes();
    Data getData(boolean async) throws NavigatorException;
    Data getPKValues() throws NavigatorException;


    void updateDetails() throws NavigatorException;
    void makeOrphan();
    void tryNextOrphanName();

    boolean isEditable();
    boolean needsCommit();
    boolean isUpdatableQueryResult();
    ResultSetTableModel createModel() throws NavigatorException;
    ResultSetTableModel getModel();
    void unloadModel();
    void reload() throws NavigatorException;

    int[] getPKColumns();
    int[] getRKColumns(int rkIndex, Table that) throws NavigatorException;
    int[] getFKColumns(int fkIndex, Table that) throws NavigatorException;
}
