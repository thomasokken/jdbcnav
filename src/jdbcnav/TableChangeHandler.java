package jdbcnav;

import jdbcnav.model.Table;
import jdbcnav.util.NavigatorException;

public interface TableChangeHandler {
    void insertRow(Table table, Object[] row) throws NavigatorException;
    void deleteRow(Table table, Object[] key) throws NavigatorException;
    void updateRow(Table table, Object[] oldRow, Object[] newRow)
					      throws NavigatorException;

    // Return 'true' if you want postmortem debugging (only useful
    // when generating a script). MultiTableDiff.diff() calls this
    // function when it encounters an internal error. If this function
    // returns 'false', it aborts and throws an exception.
    boolean continueAfterError();
}
