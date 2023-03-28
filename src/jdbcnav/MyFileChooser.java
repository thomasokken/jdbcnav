///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2023  Thomas Okken
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

import java.awt.Component;

import javax.swing.JFileChooser;


public class MyFileChooser extends JFileChooser {

    public MyFileChooser() {
        setCurrentDirectory(Preferences.getPreferences().getCurrentDirectory());
    }

    public int showOpenDialog(Component parent) {
        int result = super.showOpenDialog(parent);
        Preferences.getPreferences().setCurrentDirectory(getCurrentDirectory());
        return result;
    }

    public int showSaveDialog(Component parent) {
        int result = super.showSaveDialog(parent);
        Preferences.getPreferences().setCurrentDirectory(getCurrentDirectory());
        return result;
    }
}
