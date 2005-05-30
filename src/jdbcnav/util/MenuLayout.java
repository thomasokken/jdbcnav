package jdbcnav.util;

import java.awt.*;

public class MenuLayout implements LayoutManager {
    private int x, y;

    public MenuLayout(int x, int y) {
	this.x = x;
	this.y = y;
    }

    public void layoutContainer(Container parent) {
	doLayout(parent, true);
    }

    public Dimension preferredLayoutSize(Container parent) {
	return doLayout(parent, false);
    }

    public Dimension minimumLayoutSize(Container parent) {
	return preferredLayoutSize(parent);
    }

    public void addLayoutComponent(String name, Component comp) {
	// Nothing to do
    }

    public void removeLayoutComponent(Component comp) {
	// Nothing to do
    }

    private Dimension doLayout(Container parent, boolean reallyDoIt) {
	Component[] kids = parent.getComponents();
	int n = kids.length;
	int[] widths = new int[n];
	int h = 0;

	for (int i = 0; i < n; i++) {
	    Dimension d = kids[i].getPreferredSize();
	    widths[i] = d.width;
	    if (d.height > h)
		h = d.height;
	}

	Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
	int availWidth = scr.width - x;
	int availHeight = scr.height - y;

	for (int attempt = 0; attempt < 2; attempt++) {
	    if (attempt == 1) {
		availWidth += x;
		availHeight += y;
	    }
	    int rows = availHeight / h;
	    if (rows == 0 && attempt == 0)
		continue;
	    int cols = (n + rows - 1) / rows;
	    int[] colwidths = new int[cols];
	    int totalwidth = 0;

	    int p = 0;
	    for (int c = 0; c < cols; c++) {
		int colwidth = 0;
		for (int r = 0; r < rows; r++) {
		    if (p >= n)
			break;
		    if (widths[p] > colwidth)
			colwidth = widths[p];
		    p++;
		}
		colwidths[c] = colwidth;
		totalwidth += colwidth;
	    }

	    if (attempt == 0 && totalwidth > availWidth)
		continue;

	    if (!reallyDoIt) {
		if (n < rows)
		    rows = n;
		return new Dimension(totalwidth, h * rows);
	    }

	    int xx = 0;
	    p = 0;
	    for (int c = 0; c < cols; c++) {
		int yy = 0;
		int w = colwidths[c];
		for (int r = 0; r < rows; r++) {
		    if (p >= n)
			break;
		    kids[p].setBounds(xx, yy, w, h);
		    yy += h;
		    p++;
		}
		xx += w;
	    }
	    return null;
	}

	// Can't get here, but the compiler doesn't understand that...
	return null;
    }
}
