package jdbcnav;

import javax.swing.table.*;


public interface SortedTableModel extends TableModel {
    public void sortColumn(int i);
    public int getSortedColumn();
    public void selectionFromViewToModel(int[] selection);
    public void selectionFromModelToView(int[] selection);
}
