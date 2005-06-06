package jdbcnav.util;

import java.awt.*;

public class MenuLayout implements LayoutManager {
    public void layoutContainer(Container parent) {
	Component[] kids = parent.getComponents();
	int h = Toolkit.getDefaultToolkit().getScreenSize().height - 4;
	int n = kids.length;
	Dimension[] size = new Dimension[n];

	int colStart = 0;
	int colHeight = 0;
	int colWidth = 0;
	int colX = 0;
	for (int i = 0; i <= n; i++) {
	    Dimension d = null;
	    if (i < n)
		size[i] = d = kids[i].getPreferredSize();
	    if (i == n || (colHeight > 0 && colHeight + d.height > h)) {
		// Finish current column
		int colY = 0;
		for (int j = colStart; j < i; j++) {
		    int kh = size[j].height;
		    kids[j].setBounds(colX + 2, colY + 2, colWidth, kh);
		    colY += kh;
		}
		colX += colWidth;
		colStart = i;
		colHeight = 0;
		colWidth = 0;
	    }
	    if (i == n)
		break;
	    if (colWidth < d.width)
		colWidth = d.width;
	    colHeight += d.height;
	}
    }

    public Dimension preferredLayoutSize(Container parent) {
	Component[] kids = parent.getComponents();
	int h = Toolkit.getDefaultToolkit().getScreenSize().height - 4;
	int n = kids.length;
	int pw = 0;
	int ph = 0;

	int colHeight = 0;
	int colWidth = 0;
	for (int i = 0; i <= n; i++) {
	    Dimension d = i < n ? kids[i].getPreferredSize() : null;
	    if (i == n || colHeight > 0 && colHeight + d.height > h) {
		if (ph < colHeight)
		    ph = colHeight;
		pw += colWidth;
		colHeight = 0;
		colWidth = 0;
	    }
	    if (i == n)
		break;
	    if (colWidth < d.width)
		colWidth = d.width;
	    colHeight += d.height;
	}

	return new Dimension(pw + 4, ph + 4);
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
}
