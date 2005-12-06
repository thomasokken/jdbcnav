///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2005  Thomas Okken
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
