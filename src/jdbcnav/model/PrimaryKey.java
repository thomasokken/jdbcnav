package jdbcnav.model;

public interface PrimaryKey {
    String getName();
    int getColumnCount();
    String getColumnName(int col);
}
