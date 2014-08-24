///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2010  Thomas Okken
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


/**
 * The <code>ClobWrapper</code> interface is a DB-neutral wrapper around BLOBs.
 * It is used to hide the details of loading BLOBs, in order to help support
 * DBs where BLOBs become invalid after the ResultSet that produced them is
 * closed.
 */
public interface ClobWrapper {
    /**
     * This method returns the representation to be displayed in a table view.
     */
    String toString();

    /**
     * This method loads the actual data; if it returns successfully, the
     * ClobWrapper object should be replaced with this return value.
     */
    String load();
}
