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
import jdbcnav.model.TypeSpec;


public class JDBCDatabase_Derby extends JDBCDatabase {
    public JDBCDatabase_Derby(String name, String driver, Connection con) {
	super(name, driver, con);
    }

    protected TypeSpec makeTypeSpec(String dbType, Integer size, Integer scale,
				    int sqlType, String javaType) {
	if (dbType.equals("BLOB"))
	    javaType = "java.sql.Blob";

	return super.makeTypeSpec(dbType, size, scale, sqlType, javaType);
    }
}
