package jdbcnav.model;

import jdbcnav.ResultSetTableModel;
import jdbcnav.TableFrame;
import jdbcnav.TableDetailsFrame;
import jdbcnav.util.NavigatorException;

public interface Table extends Comparable {
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
    String[] getDbTypes();
    Integer[] getColumnSizes();
    Integer[] getColumnScales();
    String[] getIsNullable();
    int[] getSqlTypes();
    String[] getJavaTypes();
    PrimaryKey getPrimaryKey();
    ForeignKey[] getForeignKeys();
    ForeignKey[] getReferencingKeys();
    Index[] getIndexes();
    Data getData(boolean async) throws NavigatorException;
    Data getPKValues() throws NavigatorException;

    TypeDescription getTypeDescription(int column);

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
