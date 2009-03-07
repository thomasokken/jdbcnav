///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2009	Thomas Okken
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

import org.mozilla.javascript.*;

import jdbcnav.javascript.JavaScriptArray;


public class BasicForeignKey implements ForeignKey, Scriptable,
													JavaScriptArray.Element {
	private String thisKeyName;
	private String[] thisColumns;
	private String thatCatalog;
	private String thatSchema;
	private String thatName;
	private String thatQualifiedName;
	private String thatKeyName;
	private String[] thatColumns;
	private String updateRule;
	private String deleteRule;

	public void setThisKeyName(String thisKeyName) {
		this.thisKeyName = thisKeyName;
	}

	public void setThisColumns(String[] thisColumns) {
		this.thisColumns = thisColumns;
	}
	
	public void setThatCatalog(String thatCatalog) {
		this.thatCatalog = thatCatalog;
	}

	public void setThatSchema(String thatSchema) {
		this.thatSchema = thatSchema;
	}

	public void setThatName(String thatName) {
		this.thatName = thatName;
	}

	public void setThatQualifiedName(String thatQualifiedName) {
		this.thatQualifiedName = thatQualifiedName;
	}

	public void setThatKeyName(String thatKeyName) {
		this.thatKeyName = thatKeyName;
	}

	public void setThatColumns(String[] thatColumns) {
		this.thatColumns = thatColumns;
	}

	public void setUpdateRule(String updateRule) {
		this.updateRule = updateRule;
	}

	public void setDeleteRule(String deleteRule) {
		this.deleteRule = deleteRule;
	}

	public String getThatQualifiedName() {
		return thatQualifiedName;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(super.toString());
		buf.append(" thisKeyName=");
		buf.append(thisKeyName);
		buf.append(" thisColumns=");
		for (int i = 0; i < thisColumns.length; i++) {
			if (i > 0)
				buf.append(",");
			buf.append(thisColumns[i]);
		}
		buf.append(" thatCatalog=");
		buf.append(thatCatalog);
		buf.append(" thatSchema=");
		buf.append(thatSchema);
		buf.append(" thatName=");
		buf.append(thatName);
		buf.append(" thatKeyName=");
		buf.append(thatKeyName);
		buf.append(" thatColumns=");
		for (int i = 0; i < thatColumns.length; i++) {
			if (i > 0)
				buf.append(",");
			buf.append(thatColumns[i]);
		}
		buf.append(" updateRule=");
		buf.append(updateRule);
		buf.append(" deleteRule=");
		buf.append(deleteRule);
		return buf.toString();
	}

	//////////////////////
	///// ForeignKey /////
	//////////////////////

	public String getThisKeyName() {
		return thisKeyName;
	}

	public String getThatKeyName() {
		return thatKeyName;
	}

	public int getColumnCount() {
		return thisColumns.length;
		// Or thatColumns.length -- should always match
	}

	public String getThatCatalog() {
		return thatCatalog;
	}

	public String getThatSchema() {
		return thatSchema;
	}

	public String getThatName() {
		return thatName;
	}

	public String getThisColumnName(int col) {
		return thisColumns[col];
	}

	public String getThatColumnName(int col) {
		return thatColumns[col];
	}

	public String getUpdateRule() {
		return updateRule;
	}

	public String getDeleteRule() {
		return deleteRule;
	}

	///////////////////////////////////
	///// JavaScriptArray.Element /////
	///////////////////////////////////

	public String jsString() {
		return thisKeyName;
	}

	//////////////////////
	///// Scriptable /////
	//////////////////////

	public void delete(int colIndex) {
		//
	}
	public void delete(String name) {
		//
	}
	public Object get(int index, Scriptable start) {
		return NOT_FOUND;
	}
	public Object get(String name, Scriptable start) {
		if (name.equals("name")) {
			return thisKeyName;
		} else if (name.equals("columns")) {
			return new JavaScriptArray(thisColumns);
		} else if (name.equals("reftable")) {
			return getThatQualifiedName();
		} else if (name.equals("refname")) {
			return thatKeyName;
		} else if (name.equals("refcolumns")) {
			return new JavaScriptArray(thatColumns);
		} else
			return NOT_FOUND;
	}
	public String getClassName() {
		return "ForeignKey";
	}
	@SuppressWarnings(value={"unchecked"})
	public Object getDefaultValue(Class hint) {
		StringBuffer buf = new StringBuffer();
		if (thisKeyName != null) {
			buf.append(thisKeyName);
			buf.append(" ");
		}
		buf.append("[ ");
		for (int i = 0; i < thisColumns.length; i++) {
			buf.append(thisColumns[i]);
			buf.append(" ");
		}
		buf.append("], ref: ");
		if (thatKeyName != null) {
			buf.append(thatKeyName);
			buf.append(" ");
		}
		buf.append(getThatQualifiedName());
		buf.append(" [ ");
		for (int i = 0; i < thatColumns.length; i++) {
			buf.append(thatColumns[i]);
			buf.append(" ");
		}
		buf.append("]");
		return buf.toString();
	}
	public Object[] getIds() {
		return new Object[] {
			"columns",
			"name",
			"refcolumns",
			"refname",
			"reftable"
		};
	}
	public Scriptable getParentScope() {
		return null;
	}
	public Scriptable getPrototype() {
		return null;
	}
	public boolean has(int colIndex, Scriptable start) {
		return false;
	}
	public boolean has(String name, Scriptable start) {
		if (name.equals("name"))
			return true;
		else if (name.equals("columns"))
			return true;
		else if (name.equals("reftable"))
			return true;
		else if (name.equals("refname"))
			return true;
		else if (name.equals("refcolumns"))
			return true;
		else
			return false;
	}
	public boolean hasInstance(Scriptable instance) {
		return instance instanceof ForeignKey;
	}
	public void put(int colIndex, Scriptable start, Object value) {
		//
	}
	public void put(String name, Scriptable start, Object value) {
		//
	}
	public void setParentScope(Scriptable parentScope) {
		//
	}
	public void setPrototype(Scriptable prototype) {
		//
	}
}
