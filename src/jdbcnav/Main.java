package jdbcnav;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import org.mozilla.javascript.*;

import jdbcnav.javascript.JavaScriptFrame;
import jdbcnav.javascript.JavaScriptGlobal;
import jdbcnav.model.Database;
import jdbcnav.util.FileUtils;


public class Main extends JFrame {

    private static Main instance;

    private static BufferedImage splash;
    private static String version;
    private static String copyright;

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
	if (version == null)
	    version = "Could not read version information";

	copyright = "(C) 2001-2005 Thomas Okken -- thomas_okken@hotmail.com";
    }

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
	Main nav = new Main();
	nav.setVisible(true);
    }

    private static ArrayList callbacks = new ArrayList();
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
	context.exit();

	clipboard = new Clipboard();
	instance = this;

	Preferences prefs = Preferences.getPreferences();
	initialLoc = prefs.getWindowLocation();
	Dimension dim = prefs.getWindowSize();
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	// Make sure the title bar is reachable, and that there's at least
	// a bit of us visible.
	if (initialLoc.y < 0)
	    initialLoc.y = 0;
	else if (initialLoc.y > screenSize.height * 4 / 5)
	    initialLoc.y = screenSize.height * 4 / 5;
	if (initialLoc.x + dim.width < screenSize.width / 5)
	    initialLoc.x = screenSize.width / 5 - dim.width;
	else if (initialLoc.x > screenSize.width * 4 / 5)
	    initialLoc.x = screenSize.width * 4 / 5;
	setLocation(initialLoc);
	setSize(dim);

	Container c = getContentPane();
	desktop = new JDesktopPane();
	c.add(desktop);

	if (splash != null) {
	    AboutGlassPane about = new AboutGlassPane();
	    setGlassPane(about);
	    about.showAbout();
	}

	setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	addWindowListener(
		new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {
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
			for (Iterator iter = callbacks.iterator();
							iter.hasNext();) {
			    Runnable r = (Runnable) iter.next();
			    r.run();
			}
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
	mi = new JMenuItem("Set Password...");
	mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    setPassword();
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
	mi.setAccelerator(KeyStroke.getKeyStroke('Q', Event.CTRL_MASK));
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
    
    public static void addToWindowsMenu(JInternalFrame f) {
	JMenu windowsMenu = instance.windowsMenu;
	int count = windowsMenu.getItemCount();
	String title = f.getTitle();
	outer: {
	    for (int i = 0; i < count; i++) {
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
    }

    public static void removeFromWindowsMenu(JInternalFrame f) {
	JMenu windowsMenu = instance.windowsMenu;
	int count = windowsMenu.getItemCount();
	for (int i = 0; i < count; i++) {
	    WindowsMenuItem wmi = (WindowsMenuItem) windowsMenu.getItem(i);
	    if (wmi.getWindow() == f) {
		windowsMenu.remove(i);
		break;
	    }
	}
	if (windowsMenu.getItemCount() == 0)
	    windowsMenu.setEnabled(false);
    }

    public static void renameInWindowsMenu(JInternalFrame f) {
	JMenu windowsMenu = instance.windowsMenu;
	int count = windowsMenu.getItemCount();
	for (int i = 0; i < count; i++) {
	    WindowsMenuItem wmi = (WindowsMenuItem) windowsMenu.getItem(i);
	    if (wmi.getWindow() == f) {
		windowsMenu.remove(i);
		addToWindowsMenu(f);
		break;
	    }
	}
    }

    private void shell() {
	JavaScriptFrame jsf = new JavaScriptFrame();
	jsf.showStaggered();
    }

    private void showClipboard() {
	if (clipboardFrame != null) {
	    clipboardFrame.moveToFront();
	    try {
		clipboardFrame.setSelected(true);
	    } catch (PropertyVetoException ex) {
		//
	    }
	} else {
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
	JFileChooser jfc = new JFileChooser();
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
	JFileChooser jfc = new JFileChooser();
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

    private void setPassword() {
	Preferences prefs = Preferences.getPreferences();
	prefs.setPassword();
    }

    private void preferences() {
	if (preferencesFrame != null) {
	    preferencesFrame.moveToFront();
	    try {
		preferencesFrame.setSelected(true);
	    } catch (PropertyVetoException ex) {
		//
	    }
	} else {
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
		    "JDBC Navigator\n" + version + "\n" + copyright);
	else
	    ((AboutGlassPane) getGlassPane()).showAbout();
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
		    JOptionPane.QUESTION_MESSAGE) == JOptionPane.CANCEL_OPTION)
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

	    LineMetrics lm = f.getLineMetrics(copyright, frc);
	    int copyright_h = (int) lm.getHeight();
	    int copyright_base = (int) (lm.getDescent() + lm.getLeading());
	    lm = f.getLineMetrics(version, frc);
	    int version_base = (int) (lm.getDescent() + lm.getLeading());

	    int image_w = splash.getWidth();
	    int image_h = splash.getHeight();
	    Dimension d = getSize();
	    int image_x = (d.width - image_w) / 2;
	    int image_y = (d.height - image_h) / 2;
	    g2.setColor(new Color(169, 0, 248));
	    int x = image_x + (image_w - version_w) / 2;
	    int y = image_y + image_h - copyright_h - version_base - 14;
	    g2.drawString(version, x, y);
	    x = image_x + (image_w - copyright_w) / 2;
	    y = image_y + image_h - copyright_base - 14;
	    g2.drawString(copyright, x, y);
	}
    }

    private static class WindowsMenuItem extends JMenuItem
					    implements ActionListener {
	private JInternalFrame f;
	public WindowsMenuItem(JInternalFrame f) {
	    super(f.getTitle());
	    this.f = f;
	    addActionListener(this);
	}
	public JInternalFrame getWindow() {
	    return f;
	}
	public void actionPerformed(ActionEvent e) {
	    f.moveToFront();
	    try {
		f.setSelected(true);
	    } catch (PropertyVetoException ex) {
		//
	    }
	}
    }

    public static void addBrowser(Scriptable b) {
	Context context = Context.enter();
	BrowserList bl = (BrowserList) instance.global.get("browsers",
		instance.global);
	bl.addBrowser(b);
	context.exit();
    }

    public static void removeBrowser(Scriptable b) {
	Context context = Context.enter();
	BrowserList bl = (BrowserList) instance.global.get("browsers",
		instance.global);
	bl.removeBrowser(b);
	context.exit();
    }

    private static class BrowserList implements Scriptable {
	private ArrayList browsers = new ArrayList();
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
		return new Integer(browsers.size());
	    else
		return NOT_FOUND;
	}
	public String getClassName() {
	    return "BrowserList";
	}
	public Object getDefaultValue(Class hint) {
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