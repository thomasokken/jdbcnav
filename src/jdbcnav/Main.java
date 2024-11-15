///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2023  Thomas Okken
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
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Properties;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.text.DefaultEditorKit;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import jdbcnav.javascript.JavaScriptFrame;
import jdbcnav.javascript.JavaScriptGlobal;
import jdbcnav.model.Database;
import jdbcnav.util.FileUtils;
import jdbcnav.util.MenuLayout;
import jdbcnav.util.MiscUtils;


public class Main extends JFrame {

    private static Main instance;
    private static boolean cmdLineConfigs = false;
    private static ArrayList<MyFrame> frameList = new ArrayList<MyFrame>();
    private static Cursor arrowAndHourglassCursor;

    private static BufferedImage splash;
    private static String version;
    private static String copyright;
    private static String website =
                "https://thomasokken.com/jdbcnav/";
    
    public static ContextFactory contextFactory = new ContextFactory();

    private static void initArrowAndHourglassCursor() {
        // Constructing the arrow-plus-hourglass cursor
        byte[] comps;
        if (System.getProperty("file.separator").equals("\\"))
            // Windows -- white arrow with black border
            comps = new byte[] { -1, 0, 0 };
        else
            // Unix etc. -- black arrow with white border
            comps = new byte[] { 0, -1, 0 };
        IndexColorModel colormap = new IndexColorModel(8, 3, comps, comps, comps, 2);
        byte[] bits = new byte[] {
            1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 0, 0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 1, 0, 1, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 0, 0, 0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 1, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 0, 0, 0, 0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 0, 0, 0, 0, 0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 1, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 1, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 2, 2, 2, 1, 0, 1, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 1, 0, 0, 0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 0, 0, 0, 1, 0, 0, 1, 2, 2, 2, 2, 2, 1, 0, 0, 0, 1, 0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 0, 0, 1, 1, 0, 0, 1, 2, 2, 2, 2, 2, 1, 0, 0, 1, 0, 1, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 0, 1, 2, 2, 1, 0, 0, 1, 2, 2, 2, 2, 1, 0, 1, 0, 1, 0, 1, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 1, 2, 2, 2, 1, 0, 0, 1, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 2, 2, 2, 2, 2, 1, 0, 0, 1, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 1, 0, 0, 1, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2
        };
        Image img = new JLabel().createImage(new MemoryImageSource(32, 32, colormap, bits, 0, 32));
        arrowAndHourglassCursor = Toolkit.getDefaultToolkit().createCustomCursor(img, new Point(0, 0), "ArrowAndHourglass");
    }

    static {
        InputStream is = Main.class.getResourceAsStream("images/splash.gif");
        if (is != null) {
            try {
                splash = javax.imageio.ImageIO.read(is);
            } catch (IOException e) {
                // Nothing to be done about it...
            } finally {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }

        is = Main.class.getResourceAsStream("VERSION");
        if (is != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            try {
                version = br.readLine();
            } catch (IOException e) {
                // Nothing to be done about it...
            } finally {
                try {
                    br.close();
                } catch (IOException e) {}
            }
        }
        String year;
        if (version == null) {
            version = "Could not read version information";
            year = "2017";
        } else {
            int sp = version.lastIndexOf(' ');
            year = version.substring(sp + 1);
        }
        copyright = "(C) 2001-" + year + " Thomas Okken -- thomasokken@gmail.com";
    }

    private int backgroundJobCount = 0;
    private JDesktopPane desktop;
    private Point initialLoc;
    private JavaScriptGlobal global;
    private Clipboard clipboard;
    private ClipboardFrame clipboardFrame;
    private PreferencesFrame preferencesFrame;
    private JMenu windowsMenu;
    private Database.OpenCallback opencb =
                new Database.OpenCallback() {
                    public void databaseOpened(Database db) {
                        BrowserFrame b = new BrowserFrame(db);
                        b.showStaggered();
                    }
                };

    public static void main(String[] args) {
        Preferences prefs = Preferences.getPreferences();
        
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println("Exception in thread \"" + t.getName() + "\"");
                e.printStackTrace(pw);
                pw.flush();
                String s = sw.toString();
                System.err.print(s);
                System.err.flush();
                log(1, s);
            }
        });
        
        String lafName = prefs.getLookAndFeelName();
        String lafClass = null;
        UIManager.LookAndFeelInfo laf[] = UIManager.getInstalledLookAndFeels();
        for (int i = 0; i < laf.length; i++)
            if (laf[i].getName().equals(lafName)) {
                lafClass = laf[i].getClassName();
                break;
            }
        if (lafClass == null)
            lafClass = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(lafClass);
        } catch (ClassNotFoundException e) {
            //
        } catch (InstantiationException e) {
            //
        } catch (IllegalAccessException e) {
            //
        } catch (UnsupportedLookAndFeelException e) {
            //
        }

        /* The textfield edit shortcut keys use the Ctrl modifier by default.
         * Platform-specific Look-and-Feels can override this, but since we
         * support choosing arbitrary LaFs, it's better to be safe and enforce
         * the platform-specific modifier. At the very least, this is necessary
         * to make Command-V etc. work on the Mac.
         */
        int shortcutMask = MiscUtils.getMenuShortcutKeyMask();
        if (shortcutMask != MiscUtils.CTRL_MASK) {
            InputMap im = (InputMap) UIManager.get("TextField.focusInputMap");
            im.put(KeyStroke.getKeyStroke('C', shortcutMask), DefaultEditorKit.copyAction);
            im.put(KeyStroke.getKeyStroke('V', shortcutMask), DefaultEditorKit.pasteAction);
            im.put(KeyStroke.getKeyStroke('X', shortcutMask), DefaultEditorKit.cutAction);
        }

        initArrowAndHourglassCursor();

        Main.log(1, "jdbcnav version: " + version);
        for (String item : prefs.getClassPath())
            Main.log(1, "jdbcnav classpath item \"" + item + "\"");
        Properties props = System.getProperties();
        for (Map.Entry<Object, Object> entry : props.entrySet())
            Main.log(1, "system property " + entry.getKey() + " = \"" + entry.getValue() + "\"");

        MyTable.setTypeColor(1, prefs.getPkHighlightColor());
        MyTable.setTypeColor(2, prefs.getFkHighlightColor());

        Main nav = new Main();
        nav.setVisible(true);
        new MemoryMonitor();
        
        // Open connections specified on the command line, if any
        TreeMap<Integer, Preferences.ConnectionConfig> configs = getCommandLineConnectionConfigs(args);
        for (Preferences.ConnectionConfig config : configs.values())
            JDBCDatabase.connectNonInteractive(nav.opencb, config.driver, config.url, config.username, config.password, config.name);
    }
    
    private static TreeMap<Integer, Preferences.ConnectionConfig> getCommandLineConnectionConfigs(String[] args) {
        TreeMap<Integer, Preferences.ConnectionConfig> map = new TreeMap<Integer, Preferences.ConnectionConfig>();
        for (String arg : args) {
            if (!arg.startsWith("-"))
                continue;
            arg = arg.substring(1);
            int eq = arg.indexOf('=');
            if (eq == -1)
                continue;
            String name = arg.substring(0, eq);
            String value = arg.substring(eq + 1);
            int n;
            int dot = name.lastIndexOf('.');
            if (dot == -1)
                n = 1;
            else {
                try {
                    n = Integer.parseInt(name.substring(dot + 1));
                    name = name.substring(0, dot);
                } catch (NumberFormatException e) {
                    continue;
                }
            }
            Preferences.ConnectionConfig config = map.get(n);
            boolean isNew = false;
            if (config == null) {
                config = new Preferences.ConnectionConfig(null);
                isNew = true;
            }
            if (name.equals("driver"))
                config.driver = value;
            else if (name.equals("url"))
                config.url = value;
            else if (name.equals("user"))
                config.username = value;
            else if (name.equals("pass"))
                config.password = value;
            else if (name.equals("name"))
                config.name = value;
            else
                continue;
            if (isNew)
                map.put(n, config);
        }
        for (Iterator<Map.Entry<Integer, Preferences.ConnectionConfig>> iter = map.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<Integer, Preferences.ConnectionConfig> entry = iter.next();
            Preferences.ConnectionConfig config = entry.getValue();
            if (config.driver == null || config.url == null) {
                iter.remove();
                continue;
            }
            if (config.name == null)
                config.name = "cmdline." + entry.getKey();
            if (config.username == null)
                config.username = "";
            if (config.password == null)
                config.password = "";
        }
        if (!map.isEmpty())
            cmdLineConfigs = true;
        return map;
    }
    
    public static boolean cmdLineConfigs() {
        return cmdLineConfigs;
    }

    private static final SimpleDateFormat timestampFormat =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    public static void log(int level, String s) {
        Preferences prefs = Preferences.getPreferences();
        if (level > prefs.getLogLevel())
            return;
        PrintStream ps = prefs.getLogStream();
        if (ps != null)
            ps.println(timestampFormat.format(new java.util.Date()) + ": " + s);
    }

    private static ArrayList<Runnable> callbacks = new ArrayList<Runnable>();
    public static void callWhenDesktopReady(Runnable r) {
        callbacks.add(r);
    }

    private Main() {
        super("JDBC Navigator");

        Context context = Context.enter();
        global = new JavaScriptGlobal();
        context.initStandardObjects(global);
        global.defineProperty("browsers", new BrowserList(),
                    ScriptableObject.PERMANENT | ScriptableObject.READONLY);
        global.defineProperty("global", global, ScriptableObject.PERMANENT);
        Context.exit();

        clipboard = new Clipboard();
        instance = this;

        Preferences prefs = Preferences.getPreferences();
        initialLoc = prefs.getWindowLocation();
        Dimension dim = prefs.getWindowSize();

        GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = g.getScreenDevices();

        Rectangle screenBounds = null;
        for (int i = 0; i < devices.length; i++) {
            Rectangle r = devices[i].getDefaultConfiguration().getBounds();
            if (r.contains(initialLoc)) {
                screenBounds = r;
                break;
            }
        }
        if (screenBounds == null)
            screenBounds = new Rectangle(new Point(0, 0), Toolkit.getDefaultToolkit().getScreenSize());

        // Make sure the title bar is reachable, and that there's at least
        // a bit of us visible.
        if (initialLoc.y < screenBounds.y)
            initialLoc.y = screenBounds.y;
        else if (initialLoc.y > screenBounds.y + screenBounds.height * 4 / 5)
            initialLoc.y = screenBounds.y + screenBounds.height * 4 / 5;
        if (initialLoc.x + dim.width < screenBounds.x + screenBounds.width / 5)
            initialLoc.x = screenBounds.x + screenBounds.width / 5 - dim.width;
        else if (initialLoc.x > screenBounds.x + screenBounds.width * 4 / 5)
            initialLoc.x = screenBounds.x + screenBounds.width * 4 / 5;
        setLocation(initialLoc);
        setSize(dim);

        Container c = getContentPane();
        desktop = new JDesktopPane();
        c.add(desktop);

        if (splash != null) {
            AboutGlassPane about = new AboutGlassPane();
            setGlassPane(about);
            if (prefs.getShowSplash())
                about.showAbout();
        }

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(
                new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        Component glass = getGlassPane();
                        if (glass != null && glass instanceof AboutGlassPane)
                            glass.setVisible(false);
                        nuke();
                    }
                    public void windowOpened(WindowEvent e) {
                        // I'm trying to correct the "drift" that some X11
                        // window managers introduce (that is, the discrepancy
                        // between what you tell setLocation() before mapping a
                        // window, and what getLocationOnScreen() returns after
                        // the window is mapped).
                        Point loc = getLocationOnScreen();
                        int driftx = initialLoc.x - loc.x;
                        int drifty = initialLoc.y - loc.y;
                        Preferences.getPreferences()
                                   .setDrift(new Point(driftx, drifty));
                        for (Runnable r : callbacks)
                            r.run();
                        callbacks = null;
                        if (splash != null)
                            ((AboutGlassPane) getGlassPane()).startTimeout();
                    }
                });
        JMenuBar mb = new JMenuBar();
        setJMenuBar(mb);
        JMenu m = new JMenu("Navigator");
        JMenuItem mi = new JMenuItem("Open JDBC Data Source...");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    JDBCDatabase.open(opencb);
                                }
                            });
        m.add(mi);
        mi = new JMenuItem("Open File Data Source...");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    FileDatabase.open(opencb);
                                }
                            });
        m.add(mi);
        m.addSeparator();
        mi = new JMenuItem("Enter Password...");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    setPassword(false);
                                }
                            });
        m.add(mi);
        mi = new JMenuItem("Change Password...");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    setPassword(true);
                                }
                            });
        m.add(mi);
        mi = new JMenuItem("Preferences...");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    preferences();
                                }
                            });
        m.add(mi);
        mi = new JMenuItem("About JDBC Navigator");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    about();
                                }
                            });
        m.add(mi);
        m.addSeparator();
        mi = new JMenuItem("Quit");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    nuke();
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('Q', MiscUtils.getMenuShortcutKeyMask()));
        m.add(mi);
        mb.add(m);

        m = new JMenu("Misc");
        mi = new JMenuItem("JavaScript Window");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    shell();
                                }
                            });
        m.add(mi);
        mi = new JMenuItem("Show Clipboard");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    showClipboard();
                                }
                            });
        m.add(mi);
        m.addSeparator();
        mi = new JMenuItem("New Text File");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    newTextFile();
                                }
                            });
        m.add(mi);
        mi = new JMenuItem("Open Text File...");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    openTextFile();
                                }
                            });
        m.add(mi);
        mi = new JMenuItem("New Binary File");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    newBinaryFile();
                                }
                            });
        m.add(mi);
        mi = new JMenuItem("Open Binary File...");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    openBinaryFile();
                                }
                            });
        m.add(mi);
        mb.add(m);
        
        windowsMenu = new JMenu("Windows");
        windowsMenu.getPopupMenu().setLayout(new MenuLayout());
        mi = new JMenuItem("Cycle Up");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    cycleWindows(true);
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('R', MiscUtils.getMenuShortcutKeyMask()));
        windowsMenu.add(mi);
        mi = new JMenuItem("Cycle Down");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    cycleWindows(false);
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('R', MiscUtils.SHIFT_MASK | MiscUtils.getMenuShortcutKeyMask()));
        windowsMenu.add(mi);
        windowsMenu.setEnabled(false);
        mb.add(windowsMenu);
    }

    public static JDesktopPane getDesktop() {
        return instance.desktop;
    }

    public static JavaScriptGlobal getJSGlobal() {
        return instance.global;
    }

    public static Clipboard getClipboard() {
        return instance.clipboard;
    }

    public static void addToWindowsMenu(MyFrame f) {
        addToWindowsMenu(f, true);
    }

    private static void addToWindowsMenu(MyFrame f, boolean addToFrameList) {
        JMenu windowsMenu = instance.windowsMenu;
        int count = windowsMenu.getItemCount();
        if (count == 2) {
            windowsMenu.addSeparator();
            count++;
        }
        String title = f.getTitle();
        outer: {
            for (int i = 3; i < count; i++) {
                JMenuItem mi = windowsMenu.getItem(i);
                String mtitle = mi.getText();
                if (title.compareToIgnoreCase(mtitle) <= 0) {
                    windowsMenu.insert(new WindowsMenuItem(f), i);
                    break outer;
                }
            }
            windowsMenu.add(new WindowsMenuItem(f));
        }
        windowsMenu.setEnabled(true);
        if (addToFrameList)
            frameList.add(f);
    }

    public static void removeFromWindowsMenu(MyFrame f) {
        JMenu windowsMenu = instance.windowsMenu;
        int count = windowsMenu.getItemCount();
        for (int i = 3; i < count; i++) {
            WindowsMenuItem wmi = (WindowsMenuItem) windowsMenu.getItem(i);
            if (wmi.getWindow() == f) {
                windowsMenu.remove(i);
                break;
            }
        }
        if (windowsMenu.getItemCount() == 3) {
            windowsMenu.remove(2);
            windowsMenu.setEnabled(false);
        }
        frameList.remove(f);
    }

    public static void renameInWindowsMenu(MyFrame f) {
        JMenu windowsMenu = instance.windowsMenu;
        int count = windowsMenu.getItemCount();
        for (int i = 3; i < count; i++) {
            WindowsMenuItem wmi = (WindowsMenuItem) windowsMenu.getItem(i);
            if (wmi.getWindow() == f) {
                windowsMenu.remove(i);
                addToWindowsMenu(f, false);
                break;
            }
        }
    }

    public static void backgroundJobStarted() {
        backgroundJobStateChange(true);
    }

    public static void backgroundJobEnded() {
        backgroundJobStateChange(false);
    }

    private static void backgroundJobStateChange(boolean started) {
        Runnable r = new BackgroundJobStateChange(instance, started);
        if (SwingUtilities.isEventDispatchThread())
            r.run();
        else
            SwingUtilities.invokeLater(r);
    }

    private static class BackgroundJobStateChange implements Runnable {
        private Main instance;
        private boolean started;
        public BackgroundJobStateChange(Main instance, boolean started) {
            this.instance = instance;
            this.started = started;
        }
        public void run() {
            if (started) {
                if (instance.backgroundJobCount++ == 0)
                    instance.setCursor(arrowAndHourglassCursor);
            } else {
                if (instance.backgroundJobCount > 0) {
                    instance.setCursor(Cursor.getDefaultCursor());
                    instance.backgroundJobCount--;
                }
            }
        }
    }

    private void shell() {
        JavaScriptFrame jsf = new JavaScriptFrame();
        jsf.showStaggered();
    }

    private void showClipboard() {
        if (clipboardFrame != null)
            clipboardFrame.deiconifyAndRaise();
        else {
            clipboardFrame = new ClipboardFrame(clipboard);
            clipboardFrame.addInternalFrameListener(
                    new InternalFrameAdapter() {
                        public void internalFrameClosed(InternalFrameEvent e) {
                            clipboardFrame = null;
                        }
                    });
            clipboardFrame.showStaggered();
        }
    }

    private void newTextFile() {
        TextEditorFrame tef = new TextEditorFrame("Untitled", "");
        tef.showStaggered();
    }
        
    private void openTextFile() {
        JFileChooser jfc = new MyFileChooser();
        jfc.setDialogTitle("Open File");
        if (jfc.showOpenDialog(Main.getDesktop())
                                == JFileChooser.APPROVE_OPTION) {
            String text;
            File file;
            try {
                file = jfc.getSelectedFile();
                text = FileUtils.loadTextFile(file);
            } catch (IOException e) {
                MessageBox.show("Error opening file", e);
                return;
            }
            TextEditorFrame tef = new TextEditorFrame(file, text);
            tef.showStaggered();
        }
    }

    private void newBinaryFile() {
        BinaryEditorFrame bef = new BinaryEditorFrame("Untitled", new byte[0]);
        bef.showStaggered();
    }
        
    private void openBinaryFile() {
        JFileChooser jfc = new MyFileChooser();
        jfc.setDialogTitle("Open File");
        if (jfc.showOpenDialog(Main.getDesktop())
                                == JFileChooser.APPROVE_OPTION) {
            byte[] data;
            File file;
            try {
                file = jfc.getSelectedFile();
                data = FileUtils.loadBinaryFile(file);
            } catch (IOException e) {
                MessageBox.show("Error opening file", e);
                return;
            }
            BinaryEditorFrame bef = new BinaryEditorFrame(file, data);
            bef.showStaggered();
        }
    }

    private void setPassword(boolean change) {
        Preferences prefs = Preferences.getPreferences();
        prefs.setPassword(change);
    }

    private void preferences() {
        if (preferencesFrame != null)
            preferencesFrame.deiconifyAndRaise();
        else {
            preferencesFrame = new PreferencesFrame();
            preferencesFrame.addInternalFrameListener(
                    new InternalFrameAdapter() {
                        public void internalFrameClosed(InternalFrameEvent e) {
                            preferencesFrame = null;
                        }
                    });
            preferencesFrame.showCentered();
        }
    }

    private void about() {
        if (splash == null)
            JOptionPane.showInternalMessageDialog(desktop,
                    "JDBC Navigator\n" + version + "\n" + copyright
                    + "\n" + website);
        else
            ((AboutGlassPane) getGlassPane()).showAbout();
    }

    private void cycleWindows(boolean up) {
        int fs = frameList.size();
        if (fs == 0)
            return;
        int index;
        MyFrame f;
        try {
            f = (MyFrame) desktop.getSelectedFrame();
        } catch (ClassCastException e) {
            f = null;
        }
        if (f == null)
            index = fs - 1;
        else {
            index = frameList.indexOf(f);
            if (index == -1)
                index = fs - 1;
            else if (up) {
                if (index == 0)
                    index = fs - 1;
                else
                    index--;
            } else {
                if (++index == fs)
                    index = 0;
            }
        }
        f = frameList.get(index);
        f.deiconifyAndRaise();
    }

    public void nuke() {
        JInternalFrame[] windows = desktop.getAllFrames();
        boolean dirty = false;
        for (int i = 0; i < windows.length; i++) {
            JInternalFrame w = windows[i];
            if (w instanceof MyFrame && ((MyFrame) w).isDirty()) {
                dirty = true;
                break;
            }
        }
        if (dirty) {
            Toolkit.getDefaultToolkit().beep();
            if (JOptionPane.showInternalConfirmDialog(desktop,
                    "You have unsaved/uncommitted changes in one or\n"
                    + "more windows. Quitting JDBC Navigator now would\n"
                    + "mean those changes are lost.\n"
                    + "Quit anyway?",
                    "Confirm", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION)
                return;
        }
        Preferences prefs = Preferences.getPreferences();
        Point loc = getLocationOnScreen();
        Point drift = prefs.getDrift();
        loc.x += drift.x;
        loc.y += drift.y;
        prefs.setWindowLocation(loc);
        prefs.setWindowSize(getSize());
        prefs.write();
        System.exit(0);
    }

    private static class AboutGlassPane extends JComponent {
        private Thread timeout;
        private boolean showAbout;
        public AboutGlassPane() {
            addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        setVisible(false);
                    }
                    public void mousePressed(MouseEvent e) {
                        setVisible(false);
                    }
                    public void mouseReleased(MouseEvent e) {
                        setVisible(false);
                    }
                });
            addKeyListener(new KeyAdapter() {
                    public void keyPressed(KeyEvent e) {
                        setVisible(false);
                    }
                    public void keyReleased(KeyEvent e) {
                        setVisible(false);
                    }
                    public void keyTyped(KeyEvent e) {
                        setVisible(false);
                    }
                });
        }
        public void startTimeout() {
            timeout = new Thread(new Runnable() {
                        public void run() {
                            long timeleft = 5000;
                            long endtime = new Date().getTime() + timeleft;
                            Thread thisThread = Thread.currentThread();
                            do {
                                try {
                                    Thread.sleep(timeleft);
                                } catch (InterruptedException e) {}
                                timeleft = endtime - new Date().getTime();
                            } while (timeout == thisThread && timeleft > 0);
                            if (timeout == thisThread)
                                setVisible(false);
                        }
                    });
            timeout.setDaemon(true);
            timeout.start();
        }
        public void showAbout() {
            showAbout = true;
            setVisible(true);
        }
        public void setVisible(boolean visible) {
            timeout = null;
            super.setVisible(visible);
            if (visible) {
                if (showAbout)
                    requestFocusInWindow();
            } else
                showAbout = false;
        }
        public void paint(Graphics g) {
            if (!showAbout) {
                super.paint(g);
                return;
            }
            Dimension d = getSize();
            int x = (d.width - splash.getWidth()) / 2;
            int y = (d.height - splash.getHeight()) / 2;
            // No ImageObserver needed because there's no delay drawing
            // a BufferedImage (?)
            g.drawImage(splash, x, y, null);
            paintVersionAndCopyright(g);
        }
        private void paintVersionAndCopyright(Graphics g) {
            Graphics2D g2;
            if (g == null)
                g2 = (Graphics2D) getGraphics();
            else
                g2 = (Graphics2D) g;
            Font f = new Font("SansSerif", Font.PLAIN, 9);
            g2.setFont(f);
            FontRenderContext frc = g2.getFontRenderContext();

            Rectangle2D rect = f.getStringBounds(version, frc);
            int version_w = (int) rect.getWidth();
            rect = f.getStringBounds(copyright, frc);
            int copyright_w = (int) rect.getWidth();
            rect = f.getStringBounds(website, frc);
            int website_w = (int) rect.getWidth();

            LineMetrics lm = f.getLineMetrics(version, frc);
            int version_base = (int) (lm.getDescent() + lm.getLeading());
            lm = f.getLineMetrics(copyright, frc);
            int copyright_h = (int) lm.getHeight();
            int copyright_base = (int) (lm.getDescent() + lm.getLeading());
            lm = f.getLineMetrics(version, frc);
            int website_h = (int) lm.getHeight();
            int website_base = (int) (lm.getDescent() + lm.getLeading());

            int image_w = splash.getWidth();
            int image_h = splash.getHeight();
            Dimension d = getSize();
            int image_x = (d.width - image_w) / 2;
            int image_y = (d.height - image_h) / 2;
            g2.setColor(new Color(169, 0, 248));
            int x = image_x + (image_w - version_w) / 2;
            int y = image_y + image_h - website_h - copyright_h
                                                  - version_base - 11;
            g2.drawString(version, x, y);
            x = image_x + (image_w - copyright_w) / 2;
            y = image_y + image_h - website_h - copyright_base - 11;
            g2.drawString(copyright, x, y);
            x = image_x + (image_w - website_w) / 2;
            y = image_y + image_h - website_base - 11;
            g2.drawString(website, x, y);
        }
    }

    private static class WindowsMenuItem extends JMenuItem
                                            implements ActionListener {
        private MyFrame f;
        public WindowsMenuItem(MyFrame f) {
            super(f.getTitle());
            this.f = f;
            addActionListener(this);
        }
        public MyFrame getWindow() {
            return f;
        }
        public void actionPerformed(ActionEvent e) {
            f.deiconifyAndRaise();
        }
    }

    public static void addBrowser(Scriptable b) {
        Context.enter();
        BrowserList bl = (BrowserList) instance.global.get("browsers",
                instance.global);
        bl.addBrowser(b);
        Context.exit();
    }

    public static void removeBrowser(Scriptable b) {
        Context.enter();
        BrowserList bl = (BrowserList) instance.global.get("browsers",
                instance.global);
        bl.removeBrowser(b);
        Context.exit();
    }

    private static class BrowserList implements Scriptable {
        private ArrayList<Scriptable> browsers = new ArrayList<Scriptable>();
        public void delete(int index) {
            //
        }
        public void delete(String name) {
            //
        }
        public Object get(int index, Scriptable start) {
            try {
                return browsers.get(index);
            } catch (IndexOutOfBoundsException e) {
                return NOT_FOUND;
            }
        }
        public Object get(String name, Scriptable start) {
            if (name.equals("length"))
                return browsers.size();
            else
                return NOT_FOUND;
        }
        public String getClassName() {
            return "BrowserList";
        }
        public Object getDefaultValue(Class<?> hint) {
            StringBuffer buf = new StringBuffer();
            buf.append("BrowserList[");
            buf.append(browsers.size());
            buf.append("]\n");
            for (int i = 0; i < browsers.size(); i++) {
                Scriptable jsProxy = (Scriptable) browsers.get(i);
                buf.append(i);
                buf.append(": ");
                buf.append(jsProxy.get("name", instance.global));
                buf.append("\n");
            }
            return buf.toString();
        }
        public Object[] getIds() {
            return new Object[] { "length" };
        }
        public Scriptable getParentScope() {
            return instance.global;
        }
        public Scriptable getPrototype() {
            return null;
        }
        public boolean has(int index, Scriptable start) {
            return index >= 0 && index < browsers.size();
        }
        public boolean has(String name, Scriptable start) {
            return name.equals("length");
        }
        public boolean hasInstance(Scriptable instance) {
            return instance == this;
        }
        public void put(int index, Scriptable start, Object value) {
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

        public void addBrowser(Scriptable b) {
            browsers.add(b);
        }
        public void removeBrowser(Scriptable b) {
            browsers.remove(b);
        }
    }
}
