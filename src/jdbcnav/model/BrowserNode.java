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
