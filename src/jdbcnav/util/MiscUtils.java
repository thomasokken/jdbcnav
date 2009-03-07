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

package jdbcnav.util;

import java.io.*;
import java.sql.Types;
import java.util.*;

public class MiscUtils {
	public static final Comparator caseInsensitiveComparator =
				new Comparator() {
					public int compare(Object a, Object b) {
						if (a == null)
							return b == null ? 0 : -1;
						else if (b == null)
							return 1;
						else {
							String sa = (String) a;
							String sb = (String) b;
							return sa.compareToIgnoreCase(sb);
						}
					}
				};

	public static boolean strEq(String a, String b) {
		return a == null ? b == null : a.equalsIgnoreCase(b);
	}

	public static int strCmp(String a, String b) {
		if (a == null)
			return b == null ? 0 : -1;
		else if (b == null)
			return 1;
		else
			return a.compareToIgnoreCase(b);
	}

	public static int arrayLinearSearch(Object[] array, Object value) {
		for (int i = 0; i < array.length; i++)
			if (value == null ? array[i] == null : value.equals(array[i]))
				return i;
		return -1;
	}

	public static String throwableToString(Throwable th) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(bos);
		th.printStackTrace(pw);
		pw.flush();
		return bos.toString();
	}

	private static long startTime = 0;
	public static void resetTimer() {
		startTime = new Date().getTime();
		System.err.println("Timer: 0:00.0000");
	}
	public static void printTimer() {
		long split = new Date().getTime() - startTime;
		System.err.print("Timer: ");
		System.err.print(split / 60000);
		System.err.print(":");
		int msecs = (int) (split % 60000);
		int secs = msecs / 1000;
		msecs %= 1000;
		if (secs < 10)
			System.err.print("0");
		System.err.print(secs);
		System.err.print(".");
		String ms = "000" + msecs;
		System.err.println(ms.substring(ms.length() - 4));
	}

	public static int sqlTypeStringToInt(String type) {
		if (type.equalsIgnoreCase("ARRAY"))
			return Types.ARRAY;
		else if (type.equalsIgnoreCase("BIGINT"))
			return Types.BIGINT;
		else if (type.equalsIgnoreCase("BINARY"))
			return Types.BINARY;
		else if (type.equalsIgnoreCase("BIT"))
			return Types.BIT;
		else if (type.equalsIgnoreCase("BLOB"))
			return Types.BLOB;
		else if (type.equalsIgnoreCase("BOOLEAN"))
			return Types.BOOLEAN;
		else if (type.equalsIgnoreCase("CHAR"))
			return Types.CHAR;
		else if (type.equalsIgnoreCase("CLOB"))
			return Types.CLOB;
		else if (type.equalsIgnoreCase("DATALINK"))
			return Types.DATALINK;
		else if (type.equalsIgnoreCase("DATE"))
			return Types.DATE;
		else if (type.equalsIgnoreCase("DECIMAL"))
			return Types.DECIMAL;
		else if (type.equalsIgnoreCase("DISTINCT"))
			return Types.DISTINCT;
		else if (type.equalsIgnoreCase("DOUBLE"))
			return Types.DOUBLE;
		else if (type.equalsIgnoreCase("FLOAT"))
			return Types.FLOAT;
		else if (type.equalsIgnoreCase("INTEGER"))
			return Types.INTEGER;
		else if (type.equalsIgnoreCase("JAVA_OBJECT"))
			return Types.JAVA_OBJECT;
		else if (type.equalsIgnoreCase("LONGVARBINARY"))
			return Types.LONGVARBINARY;
		else if (type.equalsIgnoreCase("LONGVARCHAR"))
			return Types.LONGVARCHAR;
		else if (type.equalsIgnoreCase("NULL"))
			return Types.NULL;
		else if (type.equalsIgnoreCase("NUMERIC"))
			return Types.NUMERIC;
		else if (type.equalsIgnoreCase("OTHER"))
			return Types.OTHER;
		else if (type.equalsIgnoreCase("REAL"))
			return Types.REAL;
		else if (type.equalsIgnoreCase("REF"))
			return Types.REF;
		else if (type.equalsIgnoreCase("SMALLINT"))
			return Types.SMALLINT;
		else if (type.equalsIgnoreCase("STRUCT"))
			return Types.STRUCT;
		else if (type.equalsIgnoreCase("TIME"))
			return Types.TIME;
		else if (type.equalsIgnoreCase("TIMESTAMP"))
			return Types.TIMESTAMP;
		else if (type.equalsIgnoreCase("TINYINT"))
			return Types.TINYINT;
		else if (type.equalsIgnoreCase("VARBINARY"))
			return Types.VARBINARY;
		else if (type.equalsIgnoreCase("VARCHAR"))
			return Types.VARCHAR;
		else
			throw new IllegalArgumentException(
					  "Type must be one of \"ARRAY\", \"BIGINT\", \"BINARY\", "
					+ "\"BIT\", \"BLOB\", \"BOOLEAN\", \"CHAR\", \"CLOB\", "
					+ "\"DATALINK\", \"DATE\", \"DECIMAL\", \"DISTINCT\", "
					+ "\"DOUBLE\", \"FLOAT\", \"INTEGER\", \"JAVA_OBJECT\", "
					+ "\"LONGVARBINARY\", \"LONGVARCHAR\", \"NULL\", "
					+ "\"NUMERIC\", \"OTHER\", \"REAL\", \"REF\", "
					+ "\"SMALLINT\", \"STRUCT\", \"TIME\", \"TIMESTAMP\", "
					+ "\"TINYINT\", \"VARBINARY\", or \"VARCHAR\".");
	}

	public static String sqlTypeIntToString(int type) {
		switch (type) {
			case Types.ARRAY: return "ARRAY";
			case Types.BIGINT: return "BIGINT";
			case Types.BINARY: return "BINARY";
			case Types.BIT: return "BIT";
			case Types.BLOB: return "BLOB";
			case Types.BOOLEAN: return "BOOLEAN";
			case Types.CHAR: return "CHAR";
			case Types.CLOB: return "CLOB";
			case Types.DATALINK: return "DATALINK";
			case Types.DATE: return "DATE";
			case Types.DECIMAL: return "DECIMAL";
			case Types.DISTINCT: return "DISTINCT";
			case Types.DOUBLE: return "DOUBLE";
			case Types.FLOAT: return "FLOAT";
			case Types.INTEGER: return "INTEGER";
			case Types.JAVA_OBJECT: return "JAVA_OBJECT";
			case Types.LONGVARBINARY: return "LONGVARBINARY";
			case Types.LONGVARCHAR: return "LONGVARCHAR";
			case Types.NULL: return "NULL";
			case Types.NUMERIC: return "NUMERIC";
			case Types.OTHER: return "OTHER";
			case Types.REAL: return "REAL";
			case Types.REF: return "REF";
			case Types.SMALLINT: return "SMALLINT";
			case Types.STRUCT: return "STRUCT";
			case Types.TIME: return "TIME";
			case Types.TIMESTAMP: return "TIMESTAMP";
			case Types.TINYINT: return "TINYINT";
			case Types.VARBINARY: return "VARBINARY";
			case Types.VARCHAR: return "VARCHAR";
			default: return Integer.toString(type);
		}
	}
}
