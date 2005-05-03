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
}
