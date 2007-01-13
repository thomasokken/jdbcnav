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

package jdbcnav.util;

import java.io.*;
import java.util.*;


public class FileUtils {

    public static String loadTextFile(File file) throws IOException {
	BufferedReader br = null;
	try {
	    br = new BufferedReader(new FileReader(file));
	    char[] cbuf = new char[8192];
	    StringBuffer buf = new StringBuffer();
	    int n;
	    while ((n = br.read(cbuf)) != -1)
		buf.append(cbuf, 0, n);
	    return buf.toString();
	} finally {
	    if (br != null)
		try {
		    br.close();
		} catch (IOException e) {
		    //
		}
	}
    }

    public static void saveTextFile(File file, String text) throws IOException {
	PrintWriter pw = new PrintWriter(new FileOutputStream(file));
	pw.print(text);
	pw.close();
    }

    public static byte[] loadBinaryFile(File file) throws IOException {
	int length = (int) file.length();
	byte[] data = new byte[length];
	FileInputStream is = null;
	try {
	    is = new FileInputStream(file);
	    int readSoFar = 0;
	    while (true) {
		int readThisTime = is.read(data, readSoFar, length - readSoFar);
		// Contrary to what the API docs say, FileInputStream never
		// returns -1, even if EOF is reached. So, I test for 0 and
		// -1, to be safe (if you call a broken implementation "safe").
		if (readThisTime == 0 || readThisTime == -1)
		    break;
		readSoFar += readThisTime;
	    }
	    if (readSoFar != length)
		throw new IOException("Only managed to read " + readSoFar
				    + " bytes; should have read " + length);
	    return data;
	} finally {
	    if (is != null)
		try {
		    is.close();
		} catch (IOException e) {
		    //
		}
	}
    }

    public static void saveBinaryFile(File file, byte[] data)
							throws IOException {
	FileOutputStream os = null;
	try {
	    os = new FileOutputStream(file);
	    os.write(data);
	} finally {
	    if (os != null)
		try {
		    os.close();
		} catch (IOException e) {
		    //
		}
	}
    }

    private static HashMap entityMap;
    private static String[] entityTable;

    static {
	// These are all from "Webmaster in a Nutshell" (1st Edition)
	// except for the ones flagged "Non-standard"
	// (which is not to imply that the others *are* all standard!).
	entityMap = new HashMap();
	entityMap.put("quot", "\"");
	entityMap.put("amp", "&");
	entityMap.put("lt", "<");
	entityMap.put("gt", ">");
	entityMap.put("nbsp", "\240");
	entityMap.put("iexcl", "\241");
	entityMap.put("cent", "\242");
	entityMap.put("pound", "\243");
	entityMap.put("curren", "\244");
	entityMap.put("yen", "\245");
	entityMap.put("brvbar", "\246");
	entityMap.put("sect", "\247");
	entityMap.put("uml", "\250");
	entityMap.put("copy", "\251");
	entityMap.put("ordf", "\252");
	entityMap.put("laquo", "\253");
	entityMap.put("not", "\254");
	entityMap.put("shy", "\255");
	entityMap.put("reg", "\256");
	entityMap.put("macr", "\257");
	entityMap.put("deg", "\260");
	entityMap.put("plusmn", "\261");
	entityMap.put("sup2", "\262");
	entityMap.put("sup3", "\263");
	entityMap.put("acute", "\264");
	entityMap.put("micro", "\265");
	entityMap.put("para", "\266");
	entityMap.put("middot", "\267");
	entityMap.put("cedil", "\270");
	entityMap.put("sup1", "\271");
	entityMap.put("ordm", "\272");
	entityMap.put("raquo", "\273");
	entityMap.put("frac14", "\274");
	entityMap.put("frac12", "\275");
	entityMap.put("frac34", "\276");
	entityMap.put("iquest", "\277");
	entityMap.put("Agrave", "\300");
	entityMap.put("Aacute", "\301");
	entityMap.put("Acirc", "\302");
	entityMap.put("Atilde", "\303");
	entityMap.put("Auml", "\304");
	entityMap.put("Aring", "\305");
	entityMap.put("AElig", "\306");
	entityMap.put("Ccedil", "\307");
	entityMap.put("Egrave", "\310");
	entityMap.put("Eacute", "\311");
	entityMap.put("Ecirc", "\312");
	entityMap.put("Euml", "\313");
	entityMap.put("Igrave", "\314");
	entityMap.put("Iacute", "\315");
	entityMap.put("Icirc", "\316");
	entityMap.put("Iuml", "\317");
	entityMap.put("ETH", "\320");
	entityMap.put("Ntilde", "\321");
	entityMap.put("Ograve", "\322");
	entityMap.put("Oacute", "\323");
	entityMap.put("Ocirc", "\324");
	entityMap.put("Otilde", "\325");
	entityMap.put("Ouml", "\326");
	entityMap.put("times", "\327");
	entityMap.put("Oslash", "\330");
	entityMap.put("Ugrave", "\331");
	entityMap.put("Uacute", "\332");
	entityMap.put("Ucirc", "\333");
	entityMap.put("Uuml", "\334");
	entityMap.put("Yacute", "\335");
	entityMap.put("THORN", "\336");
	entityMap.put("szlig", "\337");
	entityMap.put("agrave", "\340");
	entityMap.put("aacute", "\341");
	entityMap.put("acirc", "\342");
	entityMap.put("atile", "\343");
	entityMap.put("auml", "\344");
	entityMap.put("aring", "\345");
	entityMap.put("aelig", "\346");
	entityMap.put("ccedil", "\347");
	entityMap.put("egrave", "\350");
	entityMap.put("eacute", "\351");
	entityMap.put("ecirc", "\352");
	entityMap.put("euml", "\353");
	entityMap.put("igrave", "\354");
	entityMap.put("iacute", "\355");
	entityMap.put("icirc", "\356");
	entityMap.put("iuml", "\357");
	entityMap.put("eth", "\360");
	entityMap.put("ntilde", "\361");
	entityMap.put("ograve", "\362");
	entityMap.put("oacute", "\363");
	entityMap.put("ocirc", "\364");
	entityMap.put("otilde", "\365");
	entityMap.put("ouml", "\366");
	entityMap.put("divide", "\367");
	entityMap.put("oslash", "\370");
	entityMap.put("ugrave", "\371");
	entityMap.put("uacute", "\372");
	entityMap.put("ucirc", "\373");
	entityMap.put("uuml", "\374");
	entityMap.put("yacute", "\375");
	entityMap.put("thorn", "\376");
	entityMap.put("yuml", "\377");

	// Now initialize an array for the reverse mapping
	entityTable = new String[256];
	for (Iterator iter = entityMap.entrySet().iterator(); iter.hasNext();) {
	    Map.Entry me = (Map.Entry) iter.next();
	    String entity = (String) me.getKey();
	    char c = ((String) me.getValue()).charAt(0);
	    entityTable[c] = entity;
	}

	// Add non-standard entities (we add them after constructing
	// the reverse mapping, so we use them for decoding, but not
	// for encoding).
	entityMap.put("apos", "'");
	entityMap.put("trade", "\231");
    }

    /**
     * Takes a String and returns it with single and double quotes,
     * ampersands, angle brackets, trade mark symbols, and all ASCII code
     * from 160 to 255 translated to HTML entity strings.
     */
    public static String encodeEntities(String text) {
	StringBuffer buf = new StringBuffer();
	int len = text.length();
	for (int i = 0; i < len; i++) {
	    char c = text.charAt(i);
	    if (entityTable[c] != null) {
		buf.append("&");
		buf.append(entityTable[c]);
		buf.append(";");
	    } else
		buf.append(c);
	}
	return buf.toString();
    }

    /**
     * Takes a string and decodes HTML entities in it to their ASCII equivalents
     * (i.e., Unicode characters in the range 0000..00FF).
     * <br>
     * Note: this function is stupid about unterminated entities; once it finds
     * an ampersand, it will look for the closing semicolon indefinitely,
     * instead of giving up as soon as it is clear that the entity is
     * malformed, which is always known after 7 characters at the most.
     */
    public static String decodeEntities(String text) {
	StringBuffer buf = new StringBuffer();
	StringBuffer entityBuf = new StringBuffer();
	boolean inEntity = false;
	int len = text.length();
	for (int i = 0; i < len; i++) {
	    char c = text.charAt(i);
	    if (inEntity) {
		if (c == ';') {
		    // Ready to translate an entity. The charaters between the
		    // opening '&' and the closing ';' must either match
		    // exactly one of the keys in entityMap, or must be a
		    // '#' followed by a decimal number in the range 0..255.
		    // Everything else passed on untranslated.
		    String e = entityBuf.toString();
		    if (e.startsWith("#")) {
			try {
			    int n = Integer.parseInt(e.substring(1));
			    if (n < 0 || n > 255) {
				buf.append("&");
				buf.append(e);
				buf.append(";");
			    } else
				buf.append((char) n);
			} catch (NumberFormatException ex) {
			    buf.append("&");
			    buf.append(e);
			    buf.append(";");
			}
		    } else {
			String s = (String) entityMap.get(e);
			if (s != null)
			    buf.append(s);
			else {
			    buf.append("&");
			    buf.append(e);
			    buf.append(";");
			}
		    }
		    entityBuf.delete(0, entityBuf.length());
		    inEntity = false;
		} else
		    entityBuf.append(c);
	    } else {
		if (c == '&')
		    inEntity = true;
		else
		    buf.append(c);
	    }
	}
	return buf.toString();
    }

    /**
     * Translates the given byte array into a Base64 string.
     */
    public static String byteArrayToBase64(byte[] a) {
        int aLen = a.length;
        int numFullGroups = aLen/3;
        int numBytesInPartialGroup = aLen - 3*numFullGroups;
        int resultLen = 4*((aLen + 2)/3);
        StringBuffer result = new StringBuffer(resultLen);

        // Translate all full groups from byte array elements to Base64
        int inCursor = 0;
        for (int i=0; i<numFullGroups; i++) {
            int byte0 = a[inCursor++] & 0xff;
            int byte1 = a[inCursor++] & 0xff;
            int byte2 = a[inCursor++] & 0xff;
            result.append(intToBase64[byte0 >> 2]);
            result.append(intToBase64[(byte0 << 4)&0x3f | (byte1 >> 4)]);
            result.append(intToBase64[(byte1 << 2)&0x3f | (byte2 >> 6)]);
            result.append(intToBase64[byte2 & 0x3f]);
        }

        // Translate partial group if present
        if (numBytesInPartialGroup != 0) {
            int byte0 = a[inCursor++] & 0xff;
            result.append(intToBase64[byte0 >> 2]);
            if (numBytesInPartialGroup == 1) {
                result.append(intToBase64[(byte0 << 4) & 0x3f]);
                result.append("==");
            } else {
                // assert numBytesInPartialGroup == 2;
                int byte1 = a[inCursor++] & 0xff;
                result.append(intToBase64[(byte0 << 4)&0x3f | (byte1 >> 4)]);
                result.append(intToBase64[(byte1 << 2)&0x3f]);
                result.append('=');
            }
        }
        // assert inCursor == a.length;
        // assert result.length() == resultLen;
        return result.toString();
    }

    /**
     * This array is a lookup table that translates 6-bit positive integer
     * index values into their "Base64 Alphabet" equivalents as specified 
     * in Table 1 of RFC 2045.
     */
    private static final char intToBase64[] = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
    };

    /**
     * Translates the specified Base64 string into a byte array.
     * 
     * @throws IllegalArgumentException if <tt>s</tt> is not a valid Base64
     *        string.
     */
    public static byte[] base64ToByteArray(String s) {
        int sLen = s.length();
        int numGroups = sLen/4;
        if (4*numGroups != sLen)
            throw new IllegalArgumentException(
                "String length must be a multiple of four.");
        int missingBytesInLastGroup = 0;
        int numFullGroups = numGroups;
        if (sLen != 0) {
            if (s.charAt(sLen-1) == '=') {
                missingBytesInLastGroup++;
                numFullGroups--;
            }
            if (s.charAt(sLen-2) == '=')
                missingBytesInLastGroup++;
        }
        byte[] result = new byte[3*numGroups - missingBytesInLastGroup];

        // Translate all full groups from base64 to byte array elements
        int inCursor = 0, outCursor = 0;
        for (int i=0; i<numFullGroups; i++) {
            int ch0 = base64toInt(s.charAt(inCursor++));
            int ch1 = base64toInt(s.charAt(inCursor++));
            int ch2 = base64toInt(s.charAt(inCursor++));
            int ch3 = base64toInt(s.charAt(inCursor++));
            result[outCursor++] = (byte) ((ch0 << 2) | (ch1 >> 4));
            result[outCursor++] = (byte) ((ch1 << 4) | (ch2 >> 2));
            result[outCursor++] = (byte) ((ch2 << 6) | ch3);
        }

        // Translate partial group, if present
        if (missingBytesInLastGroup != 0) {
            int ch0 = base64toInt(s.charAt(inCursor++));
            int ch1 = base64toInt(s.charAt(inCursor++));
            result[outCursor++] = (byte) ((ch0 << 2) | (ch1 >> 4));

            if (missingBytesInLastGroup == 1) {
                int ch2 = base64toInt(s.charAt(inCursor++));
                result[outCursor++] = (byte) ((ch1 << 4) | (ch2 >> 2));
            }
        }
        // assert inCursor == s.length()-missingBytesInLastGroup;
        // assert outCursor == result.length;
        return result;
    }

    /**
     * Translates the specified character, which is assumed to be in the
     * "Base 64 Alphabet" into its equivalent 6-bit positive integer.
     *
     * @throw IllegalArgumentException or ArrayOutOfBoundsException if
     *        c is not in the Base64 Alphabet.
     */
    private static int base64toInt(char c) {
        int result = base64ToInt[c];
        if (result < 0)
            throw new IllegalArgumentException("Illegal character " + c);
        return result;
    }

    /**
     * This array is a lookup table that translates unicode characters
     * drawn from the "Base64 Alphabet" (as specified in Table 1 of RFC 2045)
     * into their 6-bit positive integer equivalents.  Characters that
     * are not in the Base64 alphabet but fall within the bounds of the
     * array are translated to -1.
     */
    private static final byte base64ToInt[] = {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54,
        55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4,
        5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
        24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34,
        35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51
    };

    public static String byteArrayToHex(byte[] ba) {
	StringBuffer buf = new StringBuffer();
	for (int i = 0; i < ba.length; i++) {
	    byte b = ba[i];
	    buf.append("0123456789ABCDEF".charAt((b >> 4) & 15));
	    buf.append("0123456789ABCDEF".charAt(b & 15));
	}
	return buf.toString();
    }
}
