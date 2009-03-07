///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2008	Thomas Okken
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
	}

	public MyFrame(String title, boolean resizable, boolean closable,
				   boolean maximizable, boolean iconifiable) {
		super(title, resizable, closable, maximizable, iconifiable);
	}

	public void setCursor(Cursor cursor) {
		// TODO: If I don't do anything, the BasicInternalFrameUI will set this
		// frame's cursor to "Default Cursor". This is a bit annoying, because
		// I need all Components' cursors to be left at null in order to be
		// able to set the "ArrowAndHourglass" cursor globally.
		// So, I override setCursor() so it does nothing. Ugly but functional.
	}

	public void showCentered() {
		JDesktopPane desktop = Main.getDesktop();
		desktop.add(this);
		Dimension ws = getSize();
		Dimension ds = desktop.getSize();
		setLocation((ds.width - ws.width) / 2, (ds.height - ws.height) / 2);
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
			Dimension ws = getSize();
			Dimension ds = desktop.getSize();
			position.x += titleBarHeight;
			if (position.x > ds.width / 2
					|| position.x + ws.width > ds.width)
				position.x = 0;
			position.y += titleBarHeight;
			if (position.y > ds.height / 2
					|| position.y + ws.height > ds.height)
				position.y = 0;
			setLocation(position);
			setVisible(true);
		}
		Main.addToWindowsMenu(this);
	}

	public void deiconifyAndRaise() {
		try {
			setIcon(false);
		} catch (java.beans.PropertyVetoException e) {}
		moveToFront();
		try {
			setSelected(true);
		} catch (java.beans.PropertyVetoException e) {}
	}

	public void dispose() {
		if (children != null)
			while (!children.isEmpty())
				((MyFrame) children.get(0)).dispose();
		if (parent != null && parent.children != null)
			parent.children.remove(this);
		Main.removeFromWindowsMenu(this);
		if (parent != null)
			parent.childDisposed(this);

		// TODO: The following is code to work around a JInternalFrame bug:
		// when a window is closed by the user, and the default close operation
		// is DO_DISPOSE_ON_CLOSE, the next lower window is activated; when the
		// default close operation is DO_NOTHING_ON_CLOSE, and an
		// InternalFrameListener is used to call dispose(), the next lower
		// window is *not* activated. This is because
		// JInternalFrame.doDefaultCloseOperation(), before calling dispose(),
		// fires a property change and sets isClosed to true; apparently, these
		// activities are instrumental in making the DesktopManager activate
		// the next window. So, just to be safe, I perform those activities
		// here.
		// I would like to perform these operations via reflection, but because
		// the involved methods and members are all protected, I can't. So,
		// here's hoping the JInternalFrame internals will never change in a
		// backward-incompatible manner...
		// TODO: If and when the JInternalFrame behavior is fixed, this code
		// should be made conditional on the JVM version.

		if (!isClosed()) {
			try {
				fireVetoableChange(IS_CLOSED_PROPERTY, Boolean.FALSE, Boolean.TRUE);
			} catch (java.beans.PropertyVetoException e) {
				//
			}
			isClosed = true;
			firePropertyChange(IS_CLOSED_PROPERTY, Boolean.FALSE, Boolean.TRUE);
		}

		// End of JInternalFrame bug work-around code

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
