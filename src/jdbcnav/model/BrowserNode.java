package jdbcnav.model;

import java.util.Iterator;
import jdbcnav.util.NavigatorException;

public interface BrowserNode {
    String getName();
    BrowserNode getParent();
    boolean isLeaf();
    Iterator getChildren();
    void setDisplayNode(DisplayNode dn);
    Table getTable() throws NavigatorException;
    boolean busy();

    interface DisplayNode {
	void childAddedAt(int index, BrowserNode kid);
	void childRemovedAt(int index);
	void show();
    }
}
