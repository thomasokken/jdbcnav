package jdbcnav.model;

public interface Data {
    int getRowCount();
    int getColumnCount();
    String getColumnName(int col);
    Class getColumnClass(int col);
    Object getValueAt(int row, int col);

    static final int LOADING = 0;
    static final int PAUSED = 1;
    static final int FINISHED = 2;
    void setState(int state);
    int getState();
    void addStateListener(StateListener listener);
    void removeStateListener(StateListener listener);
    interface StateListener {
	void stateChanged(int state, int rows);
    }
}
