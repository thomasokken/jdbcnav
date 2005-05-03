package jdbcnav.model;

public interface ForeignKey {
    String getThisKeyName();
    String getThatKeyName();
    int getColumnCount();
    String getThatCatalog();
    String getThatSchema();
    String getThatName();
    String getThatQualifiedName();
    String getThisColumnName(int col);
    String getThatColumnName(int col);
    String getUpdateRule();
    String getDeleteRule();
}
