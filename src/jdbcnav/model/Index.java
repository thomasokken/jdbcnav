package jdbcnav.model;

public interface Index {
    String getName();
    int getColumnCount();
    String getColumnName(int col);
    boolean isUnique();
}
