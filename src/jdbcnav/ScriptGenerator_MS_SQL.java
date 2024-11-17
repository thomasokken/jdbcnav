///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2024  Thomas Okken
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

import jdbcnav.model.TypeSpec;


public class ScriptGenerator_MS_SQL extends ScriptGenerator {
    protected String toSqlString(TypeSpec spec, Object obj) {
        if (obj == null)
            return "null";
        if (spec.type == TypeSpec.DATE) {
            return "'" + spec.objectToString(obj) + "'";
        } else if (spec.type == TypeSpec.TIME) {
            return "'" + spec.objectToString(obj) + "'";
        } else if (spec.type == TypeSpec.TIMESTAMP) {
            return "'" + spec.objectToString(obj) + "'";
        } else if (spec.type == TypeSpec.TIMESTAMP_TZ) {
            return "'" + spec.objectToString(obj) + "'";
        } else {
            return super.toSqlString(spec, obj);
        }
    }
}
