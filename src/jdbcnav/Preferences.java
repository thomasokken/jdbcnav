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

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import jdbcnav.util.MyGridBagConstraints;
import jdbcnav.util.MyGridBagLayout;
import jdbcnav.util.NavigatorException;
import jdbcnav.util.XMLWriter;


public class Preferences {
    private static final File PREFS_FILE = new File(
                           System.getProperty("user.home")
                         + System.getProperty("file.separator")
                         + ".jdbcnavrc");
    private static final File NEW_PREFS_FILE = new File(
                           System.getProperty("user.home")
                         + System.getProperty("file.separator")
                         + ".jdbcnavrc-new");

    public static class ConnectionConfig {
        public String name;
        public String driver;
        public String url;
        public String username;
        public String password;
        public ConnectionConfig(String name) {
            this.name = name;
        }
        public ConnectionConfig(String name, String driver, String url,
                                String username, String password) {
            this.name = name;
            this.driver = driver;
            this.url = url;
            this.username = username;
            this.password = password;
        }
        public boolean equals(Object that) {
            return (that instanceof ConnectionConfig) && name.equals(((ConnectionConfig) that).name);
        }
    }

    // Contains connection configurations, in most-recently-used to
    // least-recently-used order
    private ArrayList<ConnectionConfig> connectionConfigs = new ArrayList<ConnectionConfig>();

    // Encrypted configs read from .jdbcnavrc, which we haven't yet been
    // able to decrypt because the user hasn't deigned to tell us the
    // password yet.
    private ArrayList<byte[]> encryptedConfigs = new ArrayList<byte[]>();

    // Look-and-Feel name
    private String lafName = null;

    // Key highlight colors
    private Color pkHighlightColor = new Color(0xffff99);
    private Color fkHighlightColor = new Color(0x99ff99);

    // Contains system properties: names at even-numbered indices,
    // followed by the corresponding values. Names and values are
    // Strings.
    private ArrayList<String> systemProps = new ArrayList<String>();

    // Top-level window geometry
    private static final int WINDOW_X = 0;
    private static final int WINDOW_Y = 0;
    private static final int WINDOW_WIDTH = 900;
    private static final int WINDOW_HEIGHT = 700;
    private static final int DRIFT_HIST_LEN = 5;
    private Point windowLocation = new Point(WINDOW_X, WINDOW_Y);
    private Dimension windowSize = new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT);
    private ArrayList<Point> drift = new ArrayList<Point>();

    // Class Path
    private ArrayList<String> classPath;

    // Current directory for file choosers
    private File currentDirectory;

    // Password for encrypting connection configs in the .jdbcnavrc file
    private SecretKeySpec key;

    // Logging
    private int logLevel;
    private String logFileName;
    private PrintStream logStream;

    // Show splash screen on startup?
    private boolean showSplash = true;


    private static Preferences instance = new Preferences();
    private static Method addClassPathItemMethod;

    private Preferences() {
        try {
            Class<?> bootClass = Class.forName("jdbcnavboot.Boot");
            addClassPathItemMethod = bootClass.getMethod("addClassPathItem",
                                                new Class[] { String.class });
        } catch (ClassNotFoundException e) {
            // Looks like this is a "jdbcnav-small.jar" distro.
            // The jdbcnav.Boot class is missing, which means all the
            // "private class path" functionality will be disabled, and the
            // user needs to have all their JDBC drivers in java.class.path.
        } catch (NoSuchMethodException e) {
            // Should not happen. If jdbcnavboot.Boot exists, it
            // should have addClassPathItem(String), too.
        }

        read();

        if (classPath == null) {
            // No "classpath" element found in .jdbcnavrc file;
            // we provide a default that will do what most people
            // probably expect.
            // Note: if the user empties out the classpath, it will stay
            // empty, because "classpath" is an element we always write,
            // even if it is empty -- so this default classpath we're
            // building here only comes into play if the user did not
            // have a .jdbcnavrc file yet, or if they manually deleted
            // the "classpath" element from it.
            classPath = new ArrayList<String>();
            classPath.add("CLASSPATH");
            addClassPathItem("CLASSPATH");
        }
    }

    public static Preferences getPreferences() {
        return instance;
    }


    public String getLookAndFeelName() {
        return lafName;
    }

    public void setLookAndFeelName(String lafName) {
        this.lafName = lafName;
    }


    public Color getPkHighlightColor() {
        return pkHighlightColor;
    }

    public void setPkHighlightColor(Color c) {
        pkHighlightColor = c;
    }

    public Color getFkHighlightColor() {
        return fkHighlightColor;
    }

    public void setFkHighlightColor(Color c) {
        fkHighlightColor = c;
    }


    public Point getWindowLocation() {
        return windowLocation;
    }

    public void setWindowLocation(Point windowLocation) {
        this.windowLocation = windowLocation;
    }

    public Dimension getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(Dimension windowSize) {
        this.windowSize = windowSize;
    }

    public Point getDrift() {
        // Return the drift value that has the most occurrences
        // in the 'drift' list
        HashMap<Point, Integer> map = new HashMap<Point, Integer>();
        int maxCount = 0;
        Point maxCountedDrift = new Point(0, 0);
        for (Point dr : drift) {
            Integer count = map.get(dr);
            int c = count == null ? 1 : count + 1;
            if (c > maxCount) {
                maxCount = c;
                maxCountedDrift = dr;
            }
            map.put(dr, c);
        }
        return maxCountedDrift;
    }

    public void setDrift(Point dr) {
        drift.add(0, dr);
        while (drift.size() > DRIFT_HIST_LEN)
            drift.remove(drift.size() - 1);
    }

    public String getSystemPropertiesAsText() {
        StringBuffer buf = new StringBuffer();
        for (Iterator<String> iter = systemProps.iterator(); iter.hasNext();) {
            buf.append(iter.next());
            buf.append('=');
            buf.append(iter.next());
            buf.append('\n');
        }
        return buf.toString();
    }


    public void setSystemPropertiesAsText(String text) {
        StringTokenizer tok = new StringTokenizer(text, "\r\n");
        systemProps.clear();
        while (tok.hasMoreTokens()) {
            String line = tok.nextToken();
            int eq = line.indexOf('=');
            if (eq == -1)
                continue;
            String name = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            systemProps.add(name);
            systemProps.add(value);
        }
    }


    public Collection<ConnectionConfig> getConnectionConfigs() {
        return Collections.unmodifiableCollection(connectionConfigs);
    }

    public void putConnectionConfig(ConnectionConfig config) {
        int index = connectionConfigs.indexOf(config);
        if (index == 0) {
            connectionConfigs.set(0, config);
        } else if (index != -1) {
            connectionConfigs.remove(index);
            connectionConfigs.add(0, config);
        } else {
            connectionConfigs.add(0, config);
        }
    }

    private void addConnectionConfig(ConnectionConfig config) {
        connectionConfigs.add(config);
    }

    public void removeConnectionConfig(ConnectionConfig config) {
        int index = connectionConfigs.indexOf(config);
        if (index != -1)
            connectionConfigs.remove(index);
    }


    public ArrayList<String> getClassPath() {
        return classPath;
    }

    public void setClassPath(ArrayList<String> classPath) {
        this.classPath = classPath;
        if (addClassPathItemMethod != null) {
            for (String item : classPath)
                addClassPathItem(item);
        }
    }


    public File getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(File dir) {
        currentDirectory = dir;
    }


    public int getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(int newLevel) {
        if (newLevel < 0)
            newLevel = 0;
        else if (newLevel > 3)
            newLevel = 3;
        if (logLevel == newLevel)
            return;
        if (newLevel == 0) {
            // Closing log writer
            logLevel = newLevel;
            // Calling DriverManager.setLogStream() via reflection in order
            // to avoid the deprecation warning
            try {
                Method m = java.sql.DriverManager.class.getMethod(
                        "setLogStream", new Class[] { PrintStream.class });
                m.invoke(null, new Object[] { null });
            } catch (Exception e) {}
            try {
                Class<?> c = Class.forName("oracle.jdbc.driver.OracleLog");
                Method m = c.getMethod(
                        "setLogStream", new Class[] { PrintStream.class });
                m.invoke(null, new Object[] { null });
            } catch (Exception e) {}
            if (logStream != null) {
                logStream.flush();
                logStream.close();
                logStream = null;
            }
        } else {
            if (logStream == null && logFileName != null) {
                // Opening log writer
                try {
                    logStream = new PrintStream(new BufferedOutputStream(
                                new FileOutputStream(logFileName, true)), true);
                    logStream.println("===============================================================================");
                } catch (IOException e) {
                    MessageBox.show("Could not open log file.", e);
                    return;
                }
                // Calling DriverManager.setLogStream() via reflection in order
                // to avoid the deprecation warning
                try {
                    Method m = java.sql.DriverManager.class.getMethod(
                            "setLogStream", new Class[] { PrintStream.class });
                    m.invoke(null, new Object[] { logStream });
                } catch (Exception e) {}
                try {
                    Class<?> c = Class.forName("oracle.jdbc.driver.OracleLog");
                    Method m = c.getMethod(
                            "setLogStream", new Class[] { PrintStream.class });
                    m.invoke(null, new Object[] { logStream });
                } catch (Exception e) {}
            }
            try {
                Class<?> c = Class.forName("oracle.jdbc.driver.OracleLog");
                Method m = c.getMethod("setLogVolume", new Class[] {int.class});
                m.invoke(null, new Object[] { newLevel });
            } catch (Exception e) {}
            logLevel = newLevel;
        }
    }

    public String getLogFileName() {
        return logFileName;
    }

    public void setLogFileName(String newFileName) {
        if ("".equals(newFileName))
            newFileName = null;
        if (newFileName == null ? logFileName == null
                                : newFileName.equals(logFileName))
            return;
        logFileName = newFileName;
        if (logLevel != 0) {
            // Close existing log & open new one
            int oldLogLevel = logLevel;
            setLogLevel(0);
            setLogLevel(oldLogLevel);
        }
    }

    public PrintStream getLogStream() {
        return logStream;
    }


    public boolean getShowSplash() {
        return showSplash;
    }

    public void setShowSplash(boolean showSplash) {
        this.showSplash = showSplash;
    }


    private void read() {
        FileReader fr = null;
        try {
            fr = new FileReader(PREFS_FILE);
            char[] buf = new char[1];
            fr.read(buf);
            if (buf[0] != '<') {
                // Doesn't look like XML; assume old-style .jdbcnavrc file
                readOldStyle();
                return;
            }
        } catch (FileNotFoundException e) {
            // No preferences file found.
            return;
        } catch (IOException e) {
            // Preferences file not healthy; don't use it.
            return;
        } finally {
            if (fr != null)
                try {
                    fr.close();
                } catch (IOException e) {}
        }

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(PREFS_FILE, new XMLCallback());
        } catch (IOException e) {
            return;
        } catch (ParserConfigurationException e) {
            return;
        } catch (SAXException e) {
            return;
        }

        // If encryptedConfigs.size() > 0, we would like to ask for a
        // password, but we can't do that yet, because this method gets
        // called before the main window is initialized.
        // So, we ask Main to remind us when the time is right.
        if (encryptedConfigs.size() > 0)
            Main.callWhenDesktopReady(
                new Runnable() {
                    public void run() {
                        if (Main.cmdLineConfigs())
                            return;
                        PasswordDialog.askPassword(
                            new PasswordDialog.Callback() {
                                public void passwordEntered(char[] password, boolean change) {
                                    if (password.length > 0)
                                        makeKey(password, false);
                                    else
                                        key = null;
                                }
                            }, false);
                    }
                });
    }

    private class XMLCallback extends DefaultHandler {
        StringBuffer buf;

        boolean inConnectionConfig;
        ConnectionConfig connectionConfig;

        boolean inSystemProp;
        String systemPropName;
        String systemPropValue;

        boolean inWindowGeom;
        int windowX;
        int windowY;
        int windowWidth;
        int windowHeight;
        int[] windowXDrift;
        int[] windowYDrift;

        boolean inClassPath;

        public void characters(char[] ch, int start, int length)
                                                        throws SAXException {
            if (buf == null)
                buf = new StringBuffer();
            buf.append(ch, start, length);
        }

        public void startElement(String namespace, String localname,
                            String name, Attributes atts) throws SAXException {
            if (name.equals("connection-config")) {
                inConnectionConfig = true;
                connectionConfig = new ConnectionConfig(null, "", "", "", "");
            } else if (name.equals("system-property")) {
                inSystemProp = true;
                systemPropName = null;
                systemPropValue = null;
            } else if (name.equals("window-geometry")) {
                inWindowGeom = true;
                windowX = WINDOW_X;
                windowY = WINDOW_Y;
                windowWidth = WINDOW_WIDTH;
                windowHeight = WINDOW_HEIGHT;
                windowXDrift = null;
                windowYDrift = null;
            } else if (name.equals("classpath")) {
                inClassPath = true;
                classPath = new ArrayList<String>();
            }
            buf = null;
        }

        public void endElement(String namespace, String localname, String name)
                                                        throws SAXException {
            String value = buf == null ? "" : buf.toString();
            buf = null;

            if (inConnectionConfig) {
                if (name.equals("name"))
                    connectionConfig.name = value;
                else if (name.equals("driver"))
                    connectionConfig.driver = value;
                else if (name.equals("url"))
                    connectionConfig.url = value;
                else if (name.equals("username"))
                    connectionConfig.username = value;
                else if (name.equals("password"))
                    connectionConfig.password = value;
                else if (name.equals("connection-config")) {
                    if (connectionConfig.name != null
                            && !connectionConfig.name.equals(""))
                        addConnectionConfig(connectionConfig);
                    inConnectionConfig = false;
                }
            } else if (inSystemProp) {
                if (name.equals("name"))
                    systemPropName = value;
                else if (name.equals("value"))
                    systemPropValue = value;
                else if (name.equals("system-property")) {
                    if (systemPropName != null && systemPropValue != null) {
                        systemProps.add(systemPropName);
                        systemProps.add(systemPropValue);
                        System.setProperty(systemPropName, systemPropValue);
                    }
                    inSystemProp = false;
                }
            } else if (inWindowGeom) {
                if (name.equals("x"))
                    try {
                        windowX = Integer.parseInt(value.trim());
                    } catch (NumberFormatException e) {
                        windowX = 0;
                    }
                else if (name.equals("y"))
                    try {
                        windowY = Integer.parseInt(value.trim());
                    } catch (NumberFormatException e) {
                        windowY = 0;
                    }
                else if (name.equals("width"))
                    try {
                        windowWidth = Integer.parseInt(value.trim());
                    } catch (NumberFormatException e) {
                        windowWidth = 0;
                    }
                else if (name.equals("height"))
                    try {
                        windowHeight = Integer.parseInt(value.trim());
                    } catch (NumberFormatException e) {
                        windowHeight = 0;
                    }
                else if (name.equals("x-drift") || name.equals("y-drift")) {
                    StringTokenizer tok = new StringTokenizer(value, " ,");
                    ArrayList<Integer> list = new ArrayList<Integer>();
                    while (tok.hasMoreTokens())
                        try {
                            list.add(Integer.parseInt(tok.nextToken()));
                        } catch (NumberFormatException e) {}
                    int n = list.size();
                    int[] ia = new int[n];
                    for (int i = 0; i < n; i++)
                        ia[i] = list.get(i);
                    if (name.equals("x-drift"))
                        windowXDrift = ia;
                    else
                        windowYDrift = ia;
                } else if (name.equals("window-geometry")) {
                    windowSize = new Dimension(windowWidth, windowHeight);
                    windowLocation = new Point(windowX, windowY);
                    int driftlen;
                    if (windowXDrift == null || windowYDrift == null)
                        driftlen = 0;
                    else {
                        driftlen = windowXDrift.length;
                        if (driftlen > windowYDrift.length)
                            driftlen = windowYDrift.length;
                        if (driftlen > DRIFT_HIST_LEN)
                            driftlen = DRIFT_HIST_LEN;
                    }
                    drift.clear();
                    for (int i = 0; i < driftlen; i++)
                        drift.add(new Point(windowXDrift[i], windowYDrift[i]));
                    inWindowGeom = false;
                }
            } else if (inClassPath) {
                if (name.equals("item")) {
                    String item = value.trim();
                    classPath.add(item);
                    addClassPathItem(item);
                } else if (name.equals("classpath"))
                    inClassPath = false;
            } else if (name.equals("current-directory")) {
                String path = value.trim();
                File dir = new File(path);
                currentDirectory = dir.isDirectory() ? dir : null;
            } else if (name.equals("laf-name")) {
                lafName = value;
            } else if (name.equals("pk-highlight-color")) {
                try {
                    int c = Integer.parseInt(value, 16);
                    pkHighlightColor = new Color((c >> 16) & 255,
                            (c >> 8) & 255, c & 255);
                } catch (NumberFormatException e) {}
            } else if (name.equals("fk-highlight-color")) {
                try {
                    int c = Integer.parseInt(value, 16);
                    fkHighlightColor = new Color((c >> 16) & 255,
                            (c >> 8) & 255, c & 255);
                } catch (NumberFormatException e) {}
            } else if (name.equals("encrypted-connection-configs")) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int nybble1 = -1;
                for (int i = 0; i < value.length(); i++) {
                    char c = value.charAt(i);
                    int nybble2;
                    if (c >= '0' && c <= '9')
                        nybble2 = c - '0';
                    else if (c >= 'A' && c <= 'F')
                        nybble2 = c - 'A' + 10;
                    else if (c >= 'a' && c <= 'f')
                        nybble2 = c - 'a' + 10;
                    else
                        continue;
                    if (nybble1 == -1)
                        nybble1 = nybble2;
                    else {
                        bos.write((nybble1 << 4) | nybble2);
                        nybble1 = -1;
                    }
                }
                byte[] ec = bos.toByteArray();
                encryptedConfigs.add(ec);
            } else if (name.equals("log-level")) {
                try {
                    setLogLevel(Integer.parseInt(value));
                } catch (NumberFormatException e) {}
            } else if (name.equals("log-file-name")) {
                setLogFileName(value);
            } else if (name.equals("show-splash")) {
                showSplash = value.equalsIgnoreCase("true")
                             || value.equalsIgnoreCase("yes");
            }
        }
    }

    private void readOldStyle() {
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(PREFS_FILE));
        } catch (IOException e) {
            return;
        }
        while (true) {
            String line;
            try {
                line = br.readLine();
            } catch (IOException e) {
                break;
            }
            if (line == null)
                break;

            // Can't use StringTokenizer because it won't return empty
            // tokens -- and that's a problem if we leave a field blank.

            int pipe1 = line.indexOf('|');
            if (pipe1 == -1)
                continue;
            String name = line.substring(0, pipe1);

            int pipe2 = line.indexOf('|', pipe1 + 1);
            if (pipe1 == -1)
                continue;
            String driver = line.substring(pipe1 + 1, pipe2);

            int pipe3 = line.indexOf('|', pipe2 + 1);
            if (pipe1 == -1)
                continue;
            String url = line.substring(pipe2 + 1, pipe3);

            int pipe4 = line.indexOf('|', pipe3 + 1);
            if (pipe1 == -1)
                continue;
            String username = line.substring(pipe3 + 1, pipe4);

            // Looking for a fifth pipe char for compatibility with
            // older .jdbcnavrc files, which have the internal driver
            // name after the password (currently, we decide the
            // internal driver name based on the JDBC driver class).

            int pipe5 = line.indexOf('|', pipe4 + 1);
            String password;
            if (pipe5 == -1)
                password = line.substring(pipe4 + 1);
            else
                password = line.substring(pipe4 + 1, pipe5);

            addConnectionConfig(new ConnectionConfig(name, driver, url,
                                                       username, password));
        }
        try {
            br.close();
        } catch (IOException e) {}
    }

    public void write() {
        PrintWriter pw;
        try {
            pw = new PrintWriter(new FileWriter(NEW_PREFS_FILE));
        } catch (IOException e) {
            return;
        }

        XMLWriter xml = new XMLWriter(pw);
        xml.newLine();
        xml.writeComment("JDBC Navigator Preferences");
        xml.writeComment("This is a generated file. Do not edit!");

        xml.newLine();
        xml.openTag("prefs");

        xml.newLine();
        xml.writeComment("Window Geometry");
        xml.openTag("window-geometry");
        xml.wholeTag("x", Integer.toString(windowLocation.x));
        xml.wholeTag("y", Integer.toString(windowLocation.y));
        xml.wholeTag("width", Integer.toString(windowSize.width));
        xml.wholeTag("height", Integer.toString(windowSize.height));
        if (drift.size() > 0) {
            StringBuffer xbuf = new StringBuffer();
            StringBuffer ybuf = new StringBuffer();
            boolean comma = false;
            for (Point dr : drift) {
                if (comma) {
                    xbuf.append(",");
                    ybuf.append(",");
                } else
                    comma = true;
                xbuf.append(dr.x);
                ybuf.append(dr.y);
            }
            xml.wholeTag("x-drift", xbuf.toString());
            xml.wholeTag("y-drift", ybuf.toString());
        }
        xml.closeTag();

        if (lafName != null) {
            xml.newLine();
            xml.writeComment("Swing Look and Feel");
            xml.wholeTag("laf-name", lafName);
        }

        xml.newLine();
        xml.writeComment("Key Highlight Colors");
        xml.wholeTag("pk-highlight-color",
                Integer.toHexString(pkHighlightColor.getRGB() & 0xFFFFFF));
        xml.wholeTag("fk-highlight-color",
                Integer.toHexString(fkHighlightColor.getRGB() & 0xFFFFFF));

        if (systemProps.size() > 0) {
            xml.newLine();
            xml.writeComment("System Properties; these are passed to "
                             + "System.setProperty()");
            xml.openTag("system-properties");
            for (Iterator<String> iter = systemProps.iterator(); iter.hasNext();) {
                String name = iter.next();
                String value = iter.next();
                xml.openTag("system-property");
                xml.wholeTag("name", name);
                xml.wholeTag("value", value);
                xml.closeTag();
            }
            xml.closeTag();
        }

        if (connectionConfigs.size() > 0 || encryptedConfigs.size() > 0) {
            xml.newLine();
            xml.writeComment("JDBC Connection Configurations");

            byte[] buf = null;

            if (connectionConfigs.size() > 0) {
                ByteArrayOutputStream bos;
                PrintWriter pw2;
                XMLWriter xml2;

                if (key == null) {
                    bos = null;
                    pw2 = null;
                    xml2 = xml;
                } else {
                    bos = new ByteArrayOutputStream();
                    pw2 = new PrintWriter(bos);
                    xml2 = new XMLWriter(pw2);
                }

                xml2.openTag("connection-configs");
                for (ConnectionConfig config : connectionConfigs) {
                    xml2.openTag("connection-config");
                    xml2.wholeTag("name", config.name);
                    xml2.wholeTag("driver", config.driver);
                    xml2.wholeTag("url", config.url);
                    xml2.wholeTag("username", config.username);
                    xml2.wholeTag("password", config.password);
                    xml2.closeTag();
                }
                xml2.closeTag();

                if (key != null) {
                    pw2.flush();
                    try {
                        buf = encrypt(bos.toByteArray());
                    } catch (NavigatorException e) {
                        MessageBox.show(
                                "An Exception occurred while encrypting\n"
                                + "JDBC connection configurations.", e);
                    }
                    if (buf != null)
                        encryptedConfigs.add(0, buf);
                }
            }

            for (byte[] ba : encryptedConfigs) {
                xml.openTag("encrypted-connection-configs");
                StringBuffer sbuf = new StringBuffer();
                for (int i = 0; i < ba.length; i++) {
                    byte b = ba[i];
                    sbuf.append("0123456789abcdef".charAt((b >> 4) & 15));
                    sbuf.append("0123456789abcdef".charAt(b & 15));
                }
                String s = sbuf.toString();
                int slen = s.length();
                for (int i = 0; i < slen; i += 64) {
                    int j = i + 64;
                    if (j > slen)
                        j = slen;
                    xml.writeIndent();
                    xml.writeValue(s.substring(i, j));
                    xml.newLine();
                }
                xml.closeTag();
            }

            if (buf != null)
                encryptedConfigs.remove(0);

        }

        xml.newLine();
        xml.writeComment("Class Path:");
        xml.writeComment("A list of directories and jar/zip files from   ");
        xml.writeComment("which JDBC Navigator will load JDBC drivers.   ");
        xml.writeComment("You may also use the word \"CLASSPATH\" anywhere ");
        xml.writeComment("in this list, to indicate that the value of the");
        xml.writeComment("CLASSPATH environment variable should be       ");
        xml.writeComment("inserted at that point in the sequence.        ");
        xml.openTag("classpath");
        for (String item : classPath)
            xml.wholeTag("item", item);
        xml.closeTag();

        xml.newLine();
        xml.writeComment("Current Directory for File Choosers:");
        xml.wholeTag("current-directory", currentDirectory == null ? "" : currentDirectory.getAbsolutePath());

        xml.newLine();
        xml.writeComment("Log Level and File:");
        xml.writeComment("Specifies the file to write log messages to; the");
        xml.writeComment("log level is an integer between 0 and 3, where 0");
        xml.writeComment("means no logging, and higher numbers indicate   ");
        xml.writeComment("increasing levels of verbosity.                 ");
        xml.wholeTag("log-level", Integer.toString(logLevel));
        if (logFileName != null)
            xml.wholeTag("log-file-name", logFileName);

        if (!showSplash) {
            xml.newLine();
            xml.writeComment("Show splash screen on startup?");
            xml.wholeTag("show-splash", showSplash ? "true" : "false");
        }

        xml.newLine();
        xml.closeTag();
        pw.flush();
        pw.close();

        // Once we get here, we can assume that writing the .jdbcnavrc-new file
        // was successful; now, we rename it to .jdbcnavrc
        if (!PREFS_FILE.exists() || PREFS_FILE.delete())
            NEW_PREFS_FILE.renameTo(PREFS_FILE);
    }


    private void makeKey(char[] password, boolean change) {
        byte[] b = new byte[24];
        for (int i = 0; i < 24; i++)
            b[i] = (byte) password[i % password.length];
        SecretKeySpec oldKey = key;
        key = new SecretKeySpec(b, "DESede");

        boolean tears = false;
        boolean joy = false;
        for (Iterator<byte[]> iter = encryptedConfigs.iterator(); iter.hasNext();) {
            byte[] encryptedConfig = iter.next();
            boolean success = false;
            try {
                byte[] decryptedConfig = decrypt(encryptedConfig);
                InputSource is = new InputSource(
                                new ByteArrayInputStream(decryptedConfig));
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser parser = factory.newSAXParser();
                parser.parse(is, new XMLCallback());
                success = true;
            } catch (NavigatorException e) {
                // Nothing to do.
            } catch (IOException e) {
                // Nothing to do.
            } catch (ParserConfigurationException e) {
                // Nothing to do.
            } catch (SAXException e) {
                // Nothing to do.
            }
            if (success) {
                // The configs from the encrypted XML chunk have been added
                // to the configs list, so we can get rid of the encrypted
                // data now.
                iter.remove();
                joy = true;
            } else
                tears = true;
        }
        if (tears && joy) {
            JOptionPane.showInternalMessageDialog(Main.getDesktop(),
                "Some encrypted configurations were successfully\n"
                + "decrypted, but there are others in the .jdbcnavrc\n"
                + "file that have been encrypted using a different\n"
                + "password. They will remain encrypted until you\n"
                + "enter that password as well.");
        } else if (tears && !change) {
            JOptionPane.showInternalMessageDialog(Main.getDesktop(),
                "The password you entered did not match all the encrypted\n"
                + "configurations that were read from the .jdbcnavrc file\n"
                + "on startup. The non-matching configurations will\n"
                + "remain encrypted until you enter the correct password.");
        } else if (!joy && !change) {
            JOptionPane.showInternalMessageDialog(Main.getDesktop(),
                "There were no encrypted configurations left to decrypt.\n"
                + "The password was not changed. Use \"Change Password\"\n"
                + "to enter a completely new password.");
        }
        if (!change && !joy)
            key = oldKey;
    }


    private static SecureRandom random;

    private byte[] encrypt(byte[] input) throws NavigatorException {
        return encrypt_or_decrypt(input, true);
    }

    private byte[] decrypt(byte[] input) throws NavigatorException {
        return encrypt_or_decrypt(input, false);
    }

    private byte[] encrypt_or_decrypt(byte[] input, boolean encrypt)
                                                throws NavigatorException {
        try {
            Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            int mode = encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
            byte[] iv = new byte[] { 3, 1, 4, 1, 5, 9, 2, 6 };
            IvParameterSpec ivspec = new IvParameterSpec(iv);
            cipher.init(mode, key, ivspec);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] tmp;
            if (encrypt) {
                // First, 8 bytes of randomness, to protect against
                // "known plaintext attacks" (which we are otherwise very
                // vulnerable to because of the XML markup).
                byte[] rnd = new byte[8];
                if (random == null)
                    random = SecureRandom.getInstance("SHA1PRNG");
                random.nextBytes(rnd);
                tmp = cipher.update(rnd, 0, 8);
                if (tmp != null)
                    bos.write(tmp, 0, tmp.length);
            }
            tmp = cipher.update(input, 0, input.length);
            if (tmp != null)
                bos.write(tmp, 0, tmp.length);
            tmp = cipher.doFinal();
            if (tmp != null)
                bos.write(tmp, 0, tmp.length);
            tmp = bos.toByteArray();
            if (encrypt)
                return tmp;
            else {
                // Get rid of the randomness
                int tmplen = tmp.length;
                byte[] tmp2 = new byte[tmplen - 8];
                System.arraycopy(tmp, 8, tmp2, 0, tmplen - 8);
                return tmp2;
            }
        } catch (NoSuchAlgorithmException e) {
            // From Cipher.getInstance()
            throw new NavigatorException(e);
        } catch (NoSuchPaddingException e) {
            // From Cipher.getInstance()
            throw new NavigatorException(e);
        } catch (InvalidKeyException e) {
            // From Cipher.init()
            throw new NavigatorException(e);
        } catch (InvalidAlgorithmParameterException e) {
            // From Cipher.init()
            throw new NavigatorException(e);
        } catch (IllegalBlockSizeException e) {
            // From Cipher.doFinal()
            throw new NavigatorException(e);
        } catch (BadPaddingException e) {
            // From Cipher.doFinal()
            throw new NavigatorException(e);
        }
    }

    public void setPassword(boolean change) {
        PasswordDialog.askPassword(
                new PasswordDialog.Callback() {
                    public void passwordEntered(char[] password, boolean change) {
                        if (password.length > 0) {
                            makeKey(password, change);
                            JDBCDatabase.reloadConnectionConfigs();
                        } else
                            key = null;
                        write();
                    }
                }, change);
    }

    private static class PasswordDialog extends MyFrame {
        public interface Callback {
            public void passwordEntered(char[] password, boolean change);
        }

        public static void askPassword(Callback callback, boolean change) {
            if (instance != null)
                instance.dispose();
            instance = new PasswordDialog(change);
            instance.callback = callback;
            instance.showCentered();
        }

        private boolean change;
        private JLabel label;
        private JPasswordField passwd;
        private Callback callback;
        private char[] firstPwd;
        private static PasswordDialog instance;

        private PasswordDialog(boolean change) {
            super((change ? "Change" : "Enter") + " Password", false, true, false, false);
            this.change = change;
            Container c = getContentPane();
            c.setLayout(new MyGridBagLayout());

            label = new JLabel(
                    change ?
                        ("<html>Please enter a new password for protecting JDBC"
                        + "<br>connection configurations in the .jdbcnavrc file."
                        + "<br>A zero-length password will disable encryption."
                        + "</html>")
                    :
                        ("<html>Please enter the password for decrypting JDBC"
                        + "<br>connection configurations from the .jdbcnavrc file."
                        + "</html>")
                );
            
            MyGridBagConstraints gbc = new MyGridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            c.add(label, gbc);
            
            if (!change)
                label = null;

            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (label != null) {
                        label.setText("<html>Re-enter the new password</html>");
                        label = null;
                        firstPwd = passwd.getPassword();
                        passwd.setText("");
                        return;
                    }
                    char[] pwd = passwd.getPassword();
                    if (PasswordDialog.this.change && !Arrays.equals(pwd, firstPwd)) {
                        dispose();
                        JOptionPane.showInternalMessageDialog(Main.getDesktop(),
                                "Passwords do not match.");
                        return;
                    }
                    Callback cb = callback;
                    dispose();
                    cb.passwordEntered(pwd, PasswordDialog.this.change);
                }
            };
            
            passwd = new JPasswordField(24);
            passwd.addActionListener(listener);
            gbc.gridy++;
            c.add(passwd, gbc);
            
            JPanel p = new JPanel(new GridLayout(1, 2));
            JButton button = new JButton("OK");
            button.addActionListener(listener);
            p.add(button);
            button = new JButton("Cancel");
            button.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                dispose();
                            }
                        });
            p.add(button);
            gbc.gridy++;
            c.add(p, gbc);

            pack();
        }
        
        public void dispose() {
            instance = null;
            super.dispose();
        }
    }

    /**
     * PreferencesFrame can use this method to decide whether or not to show
     * the controls for configuring the classPath.
     * Even if this method returns 'false', the "classpath" element will be
     * preserved in the .jdbcnavrc file, but it won't affect the behavior of
     * the application in any way, so there's not much point in cluttering up
     * the preferences dialog with it, especially given that it's just going
     * to confuse the user.
     */
    public boolean usingSneakyClassLoader() {
        return addClassPathItemMethod != null;
    }

    private static void addClassPathItem(String item) {
        if (addClassPathItemMethod != null) {
            try {
                addClassPathItemMethod.invoke(null, new Object[] { item });
            } catch (IllegalAccessException e) {
                // Should not happen
            } catch (InvocationTargetException e) {
                // Should not happen
            }
        }
    }
}
