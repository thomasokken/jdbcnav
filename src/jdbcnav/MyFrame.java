package jdbcnav;

import java.awt.*;
import java.util.*;
import javax.swing.*;


public class MyFrame extends JInternalFrame {
    private static int titleBarHeight = -1;
    private static Point position = null;
    private MyFrame parent;
    private ArrayList children;

    public MyFrame(String title) {
	super(title);
	init();
    }

    public MyFrame(String title, boolean resizable, boolean closable,
		   boolean maximizable, boolean iconifiable) {
	super(title, resizable, closable, maximizable, iconifiable);
	init();
    }

    private void init() {
	if (this instanceof Clipboard.Listener)
	    Main.getClipboard().addListener((Clipboard.Listener) this);
    }

    public void showCentered() {
	JDesktopPane desktop = Main.getDesktop();
	desktop.add(this);
	Dimension d1 = getSize();
	Dimension d2 = desktop.getSize();
	setLocation((d2.width - d1.width) / 2, (d2.height - d1.height) / 2);
	setVisible(true);
	Main.addToWindowsMenu(this);
    }
    
    public void showStaggered() {
	JDesktopPane desktop = Main.getDesktop();
	desktop.add(this);
	if (position == null) {
	    position = new Point(0, 0);
	    setLocation(position);
	    setVisible(true);
	    Point outer = getLocationOnScreen();
	    Point inner = getRootPane().getLocationOnScreen();
	    titleBarHeight = inner.y - outer.y;
	} else {
	    Dimension d = desktop.getSize();
	    position.x += titleBarHeight;
	    if (position.x > d.width / 2)
		position.x = 0;
	    position.y += titleBarHeight;
	    if (position.y > d.height / 2)
		position.y = 0;
	    setLocation(position);
	    setVisible(true);
	}
	Main.addToWindowsMenu(this);
    }

    public void dispose() {
	if (children != null)
	    while (!children.isEmpty())
		((MyFrame) children.get(0)).dispose();
	if (parent != null && parent.children != null)
	    parent.children.remove(this);
	Main.removeFromWindowsMenu(this);
	if (this instanceof Clipboard.Listener)
	    Main.getClipboard().removeListener((Clipboard.Listener) this);
	if (parent != null)
	    parent.childDisposed(this);
	super.dispose();
    }

    public void childDisposed(MyFrame child) {
	//
    }

    public void setTitle(String title) {
	super.setTitle(title);
	Main.renameInWindowsMenu(this);
	if (children != null)
	    for (Iterator iter = children.iterator(); iter.hasNext();)
		((MyFrame) iter.next()).updateTitle();
    }

    public void setParent(MyFrame parent) {
	this.parent = parent;
	if (parent.children == null)
	    parent.children = new ArrayList();
	parent.children.add(this);
    }

    public void updateTitle() {
	//
    }

    public String getParentTitle() {
	return parent.getTitle();
    }

    public boolean isDirty() {
	if (children != null)
	    for (Iterator iter = children.iterator(); iter.hasNext();)
		if (((MyFrame) iter.next()).isDirty())
		    return true;
	return false;
    }
}
