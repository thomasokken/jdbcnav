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

import java.sql.*;


public class JDBCDatabase_DB2 extends JDBCDatabase {
    public JDBCDatabase_DB2(String name, String driver, Connection con) {
	super(name, driver, con);
    }
    public String makeQualifiedName(String catalog, String schema,
							String name) {
	if (schema != null)
	    schema = schema.trim();
	return super.makeQualifiedName(catalog, schema, name);
    }
}
