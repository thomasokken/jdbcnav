package jdbcnav;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import javax.swing.*;

import jdbcnav.model.*;
import jdbcnav.util.*;

/**
 * Despite its name, this class isn't really a basic database; it's just
 * where I put the code that JDBCDatabase and FileDatabase have in common.
 */
public abstract class BasicDatabase implements Database {

    protected BrowserFrame browser;
    private ScriptGenerator sg;
    private MyNode rootNode;
    private MyNode orphanage;

    // Initialize this object using the seemingly redundant String(String)
    // constructor, in order to ensure that we have a unique instance.
    // OK, so I'm paranoid, but I want to make sure that the special-case
    // code in myStringComparator, BasicTable, and QueryResultFrame *only*
    // triggers on this particular instance of the word.
    public static final String ORPHANAGE = new String("orphanage");

    public abstract String getName();

    public BrowserNode getRootNode() {
	if (rootNode == null) {
	    rootNode = new MyNode("root");
	    reloadTree();
	}
	return rootNode;
    }

    public String[] getCommands() {
	return new String[] {
	    "Edit",
	    "Details",
	    "-",
	    "Generate Script...",
	    "Duplicate",
	    "Remove Orphans",
	    "Clear Cache",
	    "-",
	    "Reload Tree"
	};
    }

    public void executeCommand(int command) {
	switch (command) {
	    case -1: // Double-click
	    case 0:
		edit();
		break;
	    case 1:
		details();
		break;
	    case 2:
		generateScript();
		break;
	    case 3:
		duplicate();
		break;
	    case 4:
		removeOrphans();
		break;
	    case 5:
		clearCache();
		break;
	    case 6:
		reloadTree();
		break;
	}
    }

    private void edit() {
	Collection selection = browser.getSelectedNodes();
	for (Iterator iter = selection.iterator(); iter.hasNext();) {
	    Object o = iter.next();
	    if (!(o instanceof MyNode))
		continue;
	    MyNode n = (MyNode) o;
	    if (!n.isLeaf())
		continue;
	    if (n.edit != null)
		n.edit.deiconifyAndRaise();
	    else {
		try {
		    n.edit = new TableFrame(n.getTable(), browser);
		    n.edit.setParent(browser);
		    n.edit.showStaggered();
		} catch (NavigatorException e) {
		    MessageBox.show("Can't open Table window", e);
		}
	    }
	}
    }

    private void details() {
	Collection selection = browser.getSelectedNodes();
	for (Iterator iter = selection.iterator(); iter.hasNext();) {
	    Object o = iter.next();
	    if (!(o instanceof MyNode))
		continue;
	    MyNode n = (MyNode) o;
	    if (!n.isLeaf())
		continue;
	    if (n.details != null)
		n.details.deiconifyAndRaise();
	    else {
		try {
		    n.details = new TableDetailsFrame(n.getTable(), browser);
		    n.details.setParent(browser);
		    n.details.showStaggered();
		} catch (NavigatorException e) {
		    MessageBox.show("Can't open Table Details window", e);
		}
	    }
	}
    }

    private void generateScript() {
	GenerateScriptDialog.Callback cb = new GenerateScriptDialog.Callback() {
	    public void invoke(int what, boolean fqtn, BrowserFrame other,
			       String sgname) {
		sg = ScriptGenerator.getInstance(sgname);
		generateScript2(what, fqtn, other);
	    }
	};
	String sgname;
	if (sg != null)
	    sgname = sg.getName();
	else
	    sgname = "Same As Source";
	GenerateScriptDialog gsd = new GenerateScriptDialog(browser, cb, sgname);
	gsd.showCentered();
    }

    private void generateScript2(int what, boolean fqtn, BrowserFrame other) {
	Collection thisTS, otherTS;
	try {
	    thisTS = getSelectedTables();
	    otherTS = null;
	    if (other != null)
		otherTS = other.getDatabase().getSelectedTables();
	} catch (NavigatorException e) {
	    MessageBox.show("Generating script failed!", e);
	    return;
	}

	// Spawn a background thread for doing the hard work. It may take
	// quite a while, mostly because it may require a lot of data to be
	// loaded from the database.
	// TODO: progress dialog, or at least something that says work is in
	// progress (even if there's nothing to report in terms of percent
	// complete or such), and gives the user a chance to abort the
	// operation.

	Thread t = new Thread(new GenerateScript3(what, fqtn, thisTS, otherTS));
	t.setPriority(Thread.MIN_PRIORITY);
	t.setDaemon(true);
	t.start();
    }

    private class GenerateScript3 implements Runnable {
	private int what;
	private boolean fqtn;
	private Collection thisTS;
	private Collection otherTS;

	public GenerateScript3(int what, boolean fqtn, Collection thisTS,
						       Collection otherTS) {
	    this.what = what;
	    this.fqtn = fqtn;
	    this.thisTS = thisTS;
	    this.otherTS = otherTS;
	}

	public void run() {
	    String title = null;
	    StringBuffer script = new StringBuffer();
	    try {
		switch (what) {
		    case GenerateScriptDialog.DROP:
			title = "Drop Script";
			script.append(sg.drop(thisTS, fqtn));
			break;
		    case GenerateScriptDialog.CREATE:
			title = "Drop, Create Script";
			script.append(sg.drop(thisTS, fqtn));
			script.append(sg.create(thisTS, fqtn));
			script.append(sg.keys(thisTS, fqtn));
			break;
		    case GenerateScriptDialog.REBUILD:
			title = "Drop, Create, Populate Script";
			script.append(sg.drop(thisTS, fqtn));
			script.append(sg.create(thisTS, fqtn));
			script.append(sg.populate(thisTS, fqtn));
			script.append(sg.keys(thisTS, fqtn));
			break;
		    case GenerateScriptDialog.UPDATE_FROM:
			title = "Update Script";
			script.append(sg.diff(otherTS, thisTS, fqtn));
			break;
		    case GenerateScriptDialog.UPDATE_TO:
			title = "Update Script";
			script.append(sg.diff(thisTS, otherTS, fqtn));
			break;
		}
	    } catch (NavigatorException e) {
		MessageBox.show(e);
		return;
	    }

	    // The hard work is done; now, rejoin the AWT event thread
	    // to present the result to the user.
	    SwingUtilities.invokeLater(
			new GenerateScript4(script.toString(), title));
	}
    }

    private class GenerateScript4 implements Runnable {
	private String s;
	private String title;
	public GenerateScript4(String s, String title) {
	    this.s = s;
	    this.title = title;
	}
	public void run() {
	    if (s.length() > 1024 * 1024) {
		int res = JOptionPane.showInternalConfirmDialog(
			    Main.getDesktop(),
			    "The script is rather large (" + s.length()
			    + " characters).\nDisplaying it in a window"
			    + " may fail if memory is low.\n"
			    + "Would you like to save it to a file instead?",
			    "Confirm", JOptionPane.YES_NO_CANCEL_OPTION,
			    JOptionPane.QUESTION_MESSAGE);
		if (res == JOptionPane.CANCEL_OPTION)
		    return;
		else if (res == JOptionPane.YES_OPTION) {
		    File file = null;
		    while (true) {
			JFileChooser jfc = new JFileChooser();
			jfc.setDialogTitle("Save");
			if (file != null)
			    jfc.setSelectedFile(file);
			if (jfc.showSaveDialog(Main.getDesktop())
						!= JFileChooser.APPROVE_OPTION)
			    return;
			file = jfc.getSelectedFile();
			if (file.exists()) {
			    java.awt.Toolkit.getDefaultToolkit().beep();
			    int res2 = JOptionPane.showInternalConfirmDialog(
				Main.getDesktop(),
				"Overwrite existing " + file.getName() + "?",
				"Confirm", JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE);
			    if (res2 == JOptionPane.CANCEL_OPTION)
				return;
			    else if (res2 == JOptionPane.NO_OPTION)
				continue;
			    else
				break;
			} else
			    break;
		    }
		    try {
			FileUtils.saveTextFile(file, s);
		    } catch (IOException e) {
			MessageBox.show("Saving " + file.getName()
						    + " failed.", e);
		    }
		    return;
		}
	    }

	    TextEditorFrame tef = new TextEditorFrame(title, s, true, true);
	    tef.setParent(browser);
	    tef.showStaggered();
	}
    }


    protected abstract void duplicate();


    private void removeOrphans() {
	if (orphanage == null)
	    return;
	Collection selection = browser.getSelectedNodes();
	for (Iterator iter = selection.iterator(); iter.hasNext();) {
	    Object o = iter.next();
	    if (!(o instanceof MyNode))
		continue;
	    MyNode n = (MyNode) o;
	    if (n.parent != orphanage || n.edit != null || n.details != null)
		continue;
	    orphanage.removeChild(n);
	}
    }

    protected class TableSpec {
	public String catalog;
	public String schema;
	public String type;
	public String name;
    }

    private void clearCache() {
	// Find out if unloading all table models would discard uncommitted
	// edits...
	int flags = checkDirty(rootNode);
	if (flags != 0) {
	    String reason;
	    if (flags == 1)
		reason =
		    "There are cached tables with uncommitted edits.\n"
		  + "If you clear the cache now those edits will be lost.\n";
	    else if (flags == 2)
		reason =
		    "There are tables in the orphanage.\n"
		  + "Clearing the cache now will remove them.\n";
	    else
		reason =
		    "There are cached tables with uncommitted edits,\n"
		  + "and there are tables in the orphanage.\n"
		  + "If you clear the cache now those edits and tables\n"
		  + "will be lost.\n";
	    java.awt.Toolkit.getDefaultToolkit().beep();
	    int res = JOptionPane.showInternalConfirmDialog(
			Main.getDesktop(),
			reason + "Go ahead anyway?",
			"Confirm", JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE);
	    if (res == JOptionPane.CANCEL_OPTION)
		return;
	}
	unloadModels(rootNode);
    }

    /**
     * Recursively checks a tree for the presence of dirty models.
     * Returns 0 if none were found, 1 if uncommitted tables were found,
     * 2 if orphans were found, 3 if uncommitted tables and orphans were
     * found.
     */
    private int checkDirty(MyNode n) {
	int res = 0;
	if (n.table != null) {
	    ResultSetTableModel model = n.table.getModel();
	    if (model != null && model.isDirty())
		if (n.table.getQualifiedName().indexOf("...") == -1)
		    res |= 1;
		else
		    res |= 2;
	}
	Iterator iter = n.getChildren();
	if (iter != null)
	    while (res != 3 && iter.hasNext())
		res |= checkDirty((MyNode) iter.next());
	return res;
    }

    private void unloadModels(MyNode n) {
	Iterator iter = n.getChildren();
	ArrayList kidsToRemove = new ArrayList();
	if (iter != null)
	    while (iter.hasNext()) {
		MyNode child = (MyNode) iter.next();
		if (child.isLeaf()) {
		    if (child.table != null) {
			if (child.table.getQualifiedName().indexOf("...")
								    != -1) {
			    // Orphan: if no TableFrame nor TableDetailsFrame
			    // is pointed at it, delete it completely.
			    if (n.edit == null && n.details == null)
				kidsToRemove.add(child);
			} else {
			    // Regular table: if watched by a TableFrame,
			    // leave alone; if watched by a TableDetailsFrame,
			    // unload the model; if not watched at all,
			    // nuke the Table object.
			    if (n.edit == null)
				if (n.details == null) {
				    // Unloading the model is necessary, even
				    // though I am nulling the reference in
				    // the tree, because JavaScript may still
				    // hold a reference to the Table object.
				    child.table.unloadModel();
				    child.table = null;
				} else
				    child.table.unloadModel();
			}
		    }
		} else
		    unloadModels(child);
	    }
	for (iter = kidsToRemove.iterator(); iter.hasNext();)
	    n.removeChild((MyNode) iter.next());
    }

    private void reloadTree() {
	Collection tables;
	try {
	    tables = getTables();
	} catch (NavigatorException e) {
	    MessageBox.show("Could not get table list.", e);
	    return;
	}
	rootNode.markDeadRecursively();
	for (Iterator iter = tables.iterator(); iter.hasNext();) {
	    TableSpec ts = (TableSpec) iter.next();
	    MyNode n = rootNode;

	    if (showCatalogs()) {
		MyNode catalogNode = n.getChildNamed(ts.catalog);
		if (catalogNode == null) {
		    catalogNode = new MyNode(ts.catalog);
		    n.addChild(catalogNode);
		} else
		    catalogNode.dead = false;
		n = catalogNode;
	    }

	    if (showSchemas()) {
		MyNode schemaNode = n.getChildNamed(ts.schema);
		if (schemaNode == null) {
		    schemaNode = new MyNode(ts.schema);
		    n.addChild(schemaNode);
		} else
		    schemaNode.dead = false;
		n = schemaNode;
	    }

	    if (showTableTypes()) {
		MyNode tableTypeNode = n.getChildNamed(ts.type);
		if (tableTypeNode == null) {
		    tableTypeNode = new MyNode(ts.type);
		    n.addChild(tableTypeNode);
		} else
		    tableTypeNode.dead = false;
		n = tableTypeNode;
	    }

	    MyNode tableNode = n.getChildNamed(ts.name);
	    if (tableNode == null) {
		tableNode = new MyNode(ts.name);
		tableNode.qualifiedName = makeQualifiedName(ts.catalog,
							ts.schema, ts.name);
		n.addChild(tableNode);
	    } else
		tableNode.dead = false;
	}

	ArrayList newOrphans = new ArrayList();
	boolean orphanageDidNotExistYet = orphanage == null;
	rootNode.reapRecursively(newOrphans);
	if (orphanageDidNotExistYet && orphanage != null)
	    rootNode.addChild(orphanage);

	if (newOrphans.size() > 0) {
	    StringBuffer buf = new StringBuffer();
	    if (newOrphans.size() == 1)
		buf.append("The table\n\n");
	    else
		buf.append("The tables\n\n");
	    Collections.sort(newOrphans, caseInsensitiveStringComparator);
	    for (Iterator iter = newOrphans.iterator(); iter.hasNext();) {
		buf.append("    ");
		buf.append(iter.next());
		buf.append("\n");
	    }
	    if (newOrphans.size() == 1)
		buf.append(
		    "\nhas uncommitted edits, but has disappeared from"
		  + "\nthe database. It has been moved to the \"orphanage\""
		  + "\nfolder in the browser's tree view."
		  + "\nPlease be sure to save any important data from this"
		  + "\ntable, by copying it to a table that is still in the"
		  + "\ndatabase, or by exporting it to a CSV file, before"
		  + "\nremoving it from the orphanage or before quitting"
		  + "\nJDBC Navigator.");
	    else
		buf.append(
		    "\nhave uncommitted edits, but have disappeared from"
		  + "\nthe database. They have been moved to the \"orphanage\""
		  + "\nfolder in the browser's tree view."
		  + "\nPlease be sure to save any important data from these"
		  + "\ntables, by copying it to tables that are still in the"
		  + "\ndatabase, or by exporting it to CSV files, before"
		  + "\nremoving them from the orphanage or before quitting"
		  + "\nJDBC Navigator.");
	    JOptionPane.showInternalMessageDialog(Main.getDesktop(),
						  buf.toString());
	}
    }

    private static final Comparator caseInsensitiveStringComparator =
		    new Comparator() {
			public int compare(Object a, Object b) {
			    String sa = (String) a;
			    String sb = (String) b;
			    return sa.compareToIgnoreCase(sb);
			}
		    };

    /**
     * This method is called whenever a "Refresh" operation removes a table
     * node from the tree model. If it returns 'true', the table is renamed
     * and placed in the orphanage.
     */
    protected abstract boolean shouldMoveToOrphanage(Table table);

    private void moveToOrphanage(Table table) {
	if (orphanage == null)
	    orphanage = new MyNode(ORPHANAGE);
	table.makeOrphan();
	while (orphanage.getChildNamed(table.getName()) != null)
	    table.tryNextOrphanName();
	MyNode n = new MyNode(table.getName());
	n.qualifiedName = table.getQualifiedName();
	n.table = table;
	orphanage.addChild(n);
    }


    protected abstract Collection getTables() throws NavigatorException;

    public TableFrame showTableFrame(String qualifiedName) {
	MyNode n = findTableNode(qualifiedName);
	if (n == null) {
	    java.awt.Toolkit.getDefaultToolkit().beep();
	    return null;
	}
	if (n.edit != null) {
	    n.edit.deiconifyAndRaise();
	    n.show();
	    return n.edit;
	}
	try {
	    n.edit = new TableFrame(n.getTable(), browser);
	    n.edit.setParent(browser);
	    n.edit.showStaggered();
	    n.show();
	    return n.edit;
	} catch (NavigatorException e) {
	    MessageBox.show("Can't open Table window", e);
	    return null;
	}
    }

    public TableDetailsFrame showTableDetailsFrame(String qualifiedName) {
	MyNode n = findTableNode(qualifiedName);
	if (n == null) {
	    java.awt.Toolkit.getDefaultToolkit().beep();
	    return null;
	}
	if (n.details != null) {
	    n.details.deiconifyAndRaise();
	    n.show();
	    return n.details;
	}
	try {
	    n.details = new TableDetailsFrame(n.getTable(), browser);
	    n.details.setParent(browser);
	    n.details.showStaggered();
	    n.show();
	    return n.details;
	} catch (NavigatorException e) {
	    MessageBox.show("Can't open Table Details window", e);
	    return null;
	}
    }

    public void tableFrameClosed(String qualifiedName) {
	MyNode n = findTableNode(qualifiedName);
	if (n != null)
	    n.edit = null;
    }

    public void tableDetailsFrameClosed(String qualifiedName) {
	MyNode n = findTableNode(qualifiedName);
	if (n != null)
	    n.details = null;
    }

    public Table getTable(String qualifiedName) throws NavigatorException {
	MyNode n = findTableNode(qualifiedName);
	if (n == null)
	    throw new NavigatorException(
		"The table " + qualifiedName + " was not found.");
	else
	    return n.getTable();
    }

    public boolean hasOrphans() {
	if (orphanage == null)
	    return false;
	Iterator iter = orphanage.getChildren();
	return iter != null && iter.hasNext();
    }

    public Collection getSelectedTables() throws NavigatorException {
	Collection nodes = browser.getSelectedNodes();
	ArrayList tables = new ArrayList();
	Iterator iter = nodes.iterator();
	while (iter.hasNext()) {
	    Object o = iter.next();
	    if (o instanceof MyNode) {
		MyNode n = (MyNode) o;
		if (n.isLeaf())
		    tables.add(n.getTable());
	    }
	}
	return tables;
    }

    protected String getIdentifierQuoteString() {
	return " ";
    }

    public String quote(String s) {
	if (s == null)
	    return null;
	String q = getIdentifierQuoteString();
	if (q.equals(" "))
	    return s;
	else {
	    int sl = s.length();
	    for (int i = 0; i < sl; i++) {
		char c = s.charAt(i);
		if (!((c >= 'A' && c <= 'Z')
			    || (c >= 'a' && c <= 'z')
			    || (c >= '0' && c <= '9')
			    || c == '_'))
		    return q + s + q;
	    }
	    return s;
	}
    }

    public String unquote(String s) {
	if (s == null)
	    return null;
	String q = getIdentifierQuoteString();
	if (q.equals(" "))
	    return s;
	else {
	    int ql = q.length();
	    if (s.length() > 2 * ql
		    && s.substring(0, ql).equals(q)
		    && s.substring(s.length() - ql).equals(q))
		return s.substring(ql, s.length() - ql);
	    else
		return s;
	}
    }

    /**
     * Generate a name that represents a table uniquely to the RDBMS, i.e.
     * something that can be used in SQL scripts and is guaranteed not to
     * access the wrong table.
     */
    public String makeQualifiedName(String catalog, String schema,
				       String name) {
	catalog = quote(catalog);
	schema = quote(schema);
	name = quote(name);
	if (catalog != null)
	    if (schema != null)
		return catalog + "." + schema + "." + name;
	    else
		return catalog + "." + name;
	else if (schema != null)
	    return schema + "." + name;
	else
	    return name;
    }

    public String[] parseQualifiedName(String qualifiedName) {
	ArrayList al = new ArrayList();
	String q = getIdentifierQuoteString();
	if (q.equals(" ")) {
	    StringTokenizer tok = new StringTokenizer(qualifiedName, ".");
	    while (tok.hasMoreTokens())
		al.add(tok.nextToken());
	} else {
	    int ql = q.length();
	    int m = qualifiedName.length() - ql;
	    int p;
	    boolean inQuotes = false;
	    StringBuffer buf = new StringBuffer(qualifiedName);
	    int i = 0;
	    while (i <= buf.length() - ql) {
		if (buf.substring(i, i + ql).equals(q)) {
		    buf.delete(i, i + ql);
		    inQuotes = !inQuotes;
		} else if (!inQuotes && buf.charAt(i) == '.') {
		    al.add(buf.substring(0, i));
		    buf.delete(0, i + 1);
		    i = 0;
		} else
		    i++;
	    }
	    al.add(buf.toString());
	}
	for (int i = al.size(); i < 3; i++)
	    al.add(0, null);
	return (String[]) al.toArray(new String[3]);
    }

    protected abstract boolean showCatalogs();

    protected abstract boolean showSchemas();

    protected abstract boolean showTableTypes();

    private MyNode findTableNode(String qualifiedName) {
	int dots;
	if ((dots = qualifiedName.indexOf("...")) != -1) {
	    // Names containing three consecutive dots are used to distinguish
	    // orphans (tables that exist in memory but have been deleted from
	    // the database).

	    if (orphanage != null) {
		String name = qualifiedName.substring(dots + 3);
		MyNode orphan = orphanage.getChildNamed(name);
		if (orphan != null)
		    return orphan;
	    }
	    return null;
	}

	String[] parts = parseQualifiedName(qualifiedName);
	String catalog = parts[0];
	String schema = parts[1];
	String name = parts[2];

	MyNode dn = rootNode;
	if (showCatalogs()) {
	    dn = dn.getChildNamed(catalog);
	    if (dn == null)
		return null;
	}
	if (showSchemas()) {
	    dn = dn.getChildNamed(schema);
	    if (dn == null)
		return null;
	}
	if (showTableTypes()) {
	    Iterator iter = dn.getChildren();
	    while (iter.hasNext()) {
		MyNode dn2 = (MyNode) iter.next();
		MyNode tn = dn2.getChildNamed(name);
		if (tn != null)
		    return tn;
	    }
	    return null;
	} else
	    return dn.getChildNamed(name);
    }


    private static final Comparator myStringComparator =
	    new Comparator() {
		// ORPHANAGE comes last, null comes last but before
		// ORPHANAGE, and everything else is case insensitive
		// alphabetical.
		public int compare(Object a, Object b) {
		    String sa = (String) a;
		    String sb = (String) b;
		    return sa == ORPHANAGE
				? sb == ORPHANAGE
				    ? 0
				    : 1
				: sb == ORPHANAGE
				    ? -1
				    : sa == null
					? sb == null
					    ? 0
					    : 1
					: sb == null
					    ? -1
					    : sa.compareToIgnoreCase(sb);
		}
	    };

    private class MyNode implements BrowserNode {
	// Common stuff
	public MyNode parent;
	public DisplayNode displayNode;
	public String name;
	public boolean dead;
	// Node stuff
	public TreeMap children;
	// Leaf stuff
	public String qualifiedName;
	public Table table;
	public TableFrame edit;
	public TableDetailsFrame details;

	public MyNode(String name) {
	    this.name = name;
	    dead = false;
	}

	public String getName() {
	    return name == null ? "(null)" : name;
	}

	public BrowserNode getParent() {
	    return parent;
	}

	public boolean isLeaf() {
	    return qualifiedName != null;
	}

	public Table getTable() throws NavigatorException {
	    if (qualifiedName == null)
		return null;
	    if (table == null)
		table = loadTable(qualifiedName);
	    return table;
	}

	public Iterator getChildren() {
	    if (children == null)
		return null;
	    else
		return children.values().iterator();
	}

	public void setDisplayNode(DisplayNode displayNode) {
	    this.displayNode = displayNode;
	}

	public void show() {
	    if (displayNode != null)
		displayNode.show();
	}

	public boolean busy() {
	    if (children == null)
		return edit != null || details != null;
	    for (Iterator iter = children.values().iterator(); iter.hasNext();)
		if (((MyNode) iter.next()).busy())
		    return true;
	    return false;
	}

	// "Private" implementation stuff

	public void addChild(MyNode child) {
	    dead = false;
	    child.parent = this;
	    if (children == null)
		children = new TreeMap(myStringComparator);
	    children.put(child.name, child);
	    if (displayNode != null) {
		// Aargh. The keys in the TreeMap are sorted! Isn't there
		// an efficient way to find a key's index within the key
		// set? (Good thing this code does not get executed when the
		// tree is first built!)
		int index = 0;
		for (Iterator iter = children.values().iterator();
						    iter.hasNext();) {
		    if (child == iter.next()) {
			displayNode.childAddedAt(index, child);
			break;
		    }
		    index++;
		}
	    }
	}

	public void removeChild(MyNode child) {
	    if (displayNode != null) {
		// Aargh. The keys in the TreeMap are sorted! Isn't there
		// an efficient way to find a key's index within the key
		// set?
		int index = 0;
		for (Iterator iter = children.values().iterator();
						    iter.hasNext();) {
		    if (child == iter.next()) {
			displayNode.childRemovedAt(index);
			break;
		    }
		    index++;
		}
	    }
	    children.remove(child.getName());
	}

	public MyNode getChildNamed(String name) {
	    if (children == null)
		return null;
	    else
		return (MyNode) children.get(name);
	}

	public void markDeadRecursively() {
	    if (name == ORPHANAGE)
		return;
	    if (edit == null && details == null)
		dead = true;
	    if (children != null)
		for (Iterator iter = children.values().iterator(); iter.hasNext();)
		    ((MyNode) iter.next()).markDeadRecursively();
	}

	public void reapRecursively(ArrayList newOrphans) {
	    if (children != null) {
		int index = 0;
		ArrayList removed = null;
		for (Iterator iter = children.entrySet().iterator();
							iter.hasNext();) {
		    Map.Entry entry = (Map.Entry) iter.next();
		    MyNode child = (MyNode) entry.getValue();
		    child.reapRecursively(newOrphans);
		    if (child.dead) {
			if (displayNode != null)
			    displayNode.childRemovedAt(index);
			// Adding the child to be removed to a list; we'll do
			// the actual removal *after* this loop, since the
			// Iterators returned by TreeMap do not support calling
			// next() again after remove().
			if (removed == null)
			    removed = new ArrayList();
			removed.add(entry.getKey());
			if (child.table != null)
			    if (shouldMoveToOrphanage(child.table)) {
				newOrphans.add(child.table.getQualifiedName());
				moveToOrphanage(child.table);
			    }
		    } else {
			// If I have a live child, I should survive, too
			dead = false;
			index++;
		    }
		}
		if (removed != null)
		    for (Iterator r_iter = removed.iterator();
							r_iter.hasNext();)
			children.remove((String) r_iter.next());
	    }
	}
    }

    protected final class BasicTypeSpec extends TypeSpec {
	public String objectToString(Object o) {
	    return BasicDatabase.this.objectToString(this, o);
	}
	public Object stringToObject(String s) {
	    return BasicDatabase.this.stringToObject(this, s);
	}
    }

    protected String objectToString(TypeSpec spec, Object o) {
	if (o == null)
	    return null;

	Class klass = spec.jdbcJavaClass;

	if (java.sql.Date.class.isAssignableFrom(klass)
		|| java.sql.Time.class.isAssignableFrom(klass)
		|| java.sql.Timestamp.class.isAssignableFrom(klass))
	    // These three classes are subclasses of java.util.Date, but
	    // java.util.Date itself needs to be handled differently,
	    // hence this special case
	    return o.toString();

	if (java.util.Date.class.isAssignableFrom(klass)) {
	    long time = ((java.util.Date) o).getTime();
	    return new java.sql.Timestamp(time).toString();
	}

	if (Blob.class.isAssignableFrom(klass)) {
	    if (o instanceof Blob) {
		Blob blob = (Blob) o;
		try {
		    return "Blob (length = " + blob.length() + ")";
		} catch (SQLException e) {
		    return "Blob (length = ?)";
		}
	    } else {
		// Assuming byte[]; this can happen when a Blob value has been
		// edited in QueryResultFrame.
		klass = new byte[1].getClass();
	    }
	}

	if (klass == new byte[1].getClass()
		|| spec.jdbcJavaType.equals("byte[]")) {
	    byte[] barray = (byte[]) o;
	    StringBuffer buf = new StringBuffer();
	    for (int i = 0; i < barray.length; i++) {
		byte b = barray[i];
		buf.append("0123456789ABCDEF".charAt((b >> 4) & 15));
		buf.append("0123456789ABCDEF".charAt(b & 15));
	    }
	    return buf.toString();
	}

	if (Clob.class.isAssignableFrom(klass)) {
	    if (o instanceof Clob) {
		Clob clob = (Clob) o;
		try {
		    return "Clob (length = " + clob.length() + ")";
		} catch (SQLException e) {
		    return "Clob (length = ?)";
		}
	    }
	}

	return o.toString();
    }

    protected Object stringToObject(TypeSpec spec, String s) {
	if (s == null)
	    return null;

        try {
	    Class klass = spec.jdbcJavaClass;
	    if (java.sql.Time.class.isAssignableFrom(klass))
		return java.sql.Time.valueOf(s);
	    else if (java.sql.Date.class.isAssignableFrom(klass))
		return java.sql.Date.valueOf(s);
	    else if (java.sql.Timestamp.class.isAssignableFrom(klass))
		return java.sql.Timestamp.valueOf(s);
	    else if (java.util.Date.class.isAssignableFrom(klass))
		return new java.util.Date(java.sql.Timestamp.valueOf(s).getTime());
	    java.lang.reflect.Constructor cnstr =
			klass.getConstructor(new Class[] { String.class });
	    return cnstr.newInstance(new Object[] { s });
	} catch (Exception e) {
	    throw new IllegalArgumentException(e);
	}
    }
}
