///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2007  Thomas Okken
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License, version 2,
// as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
///////////////////////////////////////////////////////////////////////////////

package jdbcnav.util;

import java.awt.*;

public class MenuLayout implements LayoutManager {
    public void layoutContainer(Container parent) {
	Component[] kids = parent.getComponents();
	Insets insets = parent.getInsets();
	int h = Toolkit.getDefaultToolkit().getScreenSize().height
			- insets.top - insets.bottom;
	int n = kids.length;
	Dimension[] size = new Dimension[n];

	int colStart = 0;
	int colHeight = 0;
	int colWidth = 0;
	int colX = insets.left;
	for (int i = 0; i <= n; i++) {
	    Dimension d = null;
	    if (i < n)
		size[i] = d = kids[i].getPreferredSize();
	    if (i == n || (colHeight > 0 && colHeight + d.height > h)) {
		// Finish current column
		int colY = insets.top;
		for (int j = colStart; j < i; j++) {
		    int kh = size[j].height;
		    kids[j].setBounds(colX, colY, colWidth, kh);
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
	Insets insets = parent.getInsets();
	int h = Toolkit.getDefaultToolkit().getScreenSize().height
			- insets.top - insets.bottom;
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

	return new Dimension(pw + insets.left + insets.right,
			     ph + insets.top + insets.bottom);
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
