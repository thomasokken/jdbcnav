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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.WeakHashMap;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrappedException;

import jdbcnav.javascript.BasicFunction;
import jdbcnav.model.BrowserNode;
import jdbcnav.model.Database;
import jdbcnav.model.Table;
import jdbcnav.util.IteratorEnumeration;
import jdbcnav.util.MiscUtils;
import jdbcnav.util.NavigatorException;


public class BrowserFrame extends MyFrame {
    private Database db;
    private JPopupMenu popupMenu;
    private JTree tree;
    private TreeSelectionModel selectionModel;
    private static ArrayList<BrowserFrame> instances = new ArrayList<BrowserFrame>();
    private static WeakHashMap<MyCBM, Object> models = new WeakHashMap<MyCBM, Object>();
    private File file;

    public BrowserFrame(Database db) {
        super(db.getName(), true, true, true, true);
        this.db = db;
        db.setBrowser(this);
        file = db.getFile();

        setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
        addInternalFrameListener(new InternalFrameAdapter() {
                public void internalFrameClosing(InternalFrameEvent e) {
                    nuke();
                }
            });
        
        Container c = getContentPane();
        c.setLayout(new GridLayout(1, 1));
        MyTreeNode n = new MyTreeNode(null, db.getRootNode());
        tree = new JTree(n);
        selectionModel = tree.getSelectionModel();
        selectionModel.setSelectionMode(
                        TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.addTreeWillExpandListener(
                new TreeWillExpandListener() {
                    public void treeWillExpand(TreeExpansionEvent e)
                                            throws ExpandVetoException {
                        MyTreeNode nn = (MyTreeNode)
                                        e.getPath().getLastPathComponent();
                        nn.preExpand();
                    }
                    public void treeWillCollapse(TreeExpansionEvent e)
                                            throws ExpandVetoException {
                        //
                    }
                });
        tree.addTreeExpansionListener(
                new TreeExpansionListener() {
                    public void treeExpanded(TreeExpansionEvent e) {
                        JTree tree = (JTree) e.getSource();
                        JInternalFrame frame = (JInternalFrame)
                                SwingUtilities.getAncestorOfClass(
                                                JInternalFrame.class, tree);
                        // We try to grow the frame (if necessary) to be as
                        // wide as the tree wants to be, plus 30 pixels, and
                        // as tall as the tree wants to be, plus 60 pixels.
                        // The extra pixels are to account for the window
                        // decorations and the scroll bar.
                        Dimension fs = frame.getSize();
                        Dimension ts = tree.getPreferredSize();
                        ts.width += 30;
                        ts.height += 60;
                        Dimension ds = Main.getDesktop().getSize();
                        if (ts.width > ds.width)
                            ts.width = ds.width;
                        if (ts.height > ds.height)
                            ts.height = ds.height;
                        boolean resize = false;
                        if (fs.width < ts.width) {
                            fs.width = ts.width;
                            resize = true;
                        }
                        if (fs.height < ts.height) {
                            fs.height = ts.height;
                            resize = true;
                        }
                        if (resize)
                            frame.setSize(fs);
                    }
                    public void treeCollapsed(TreeExpansionEvent e) {
                        MyTreeNode nn = (MyTreeNode)
                                        e.getPath().getLastPathComponent();
                        nn.postCollapse();
                    }
                });
        tree.addMouseListener(
                new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON3) {
                            if (popupMenu != null)
                                popupMenu.show(e.getComponent(),
                                               e.getX(), e.getY());
                        } else {
                            if (e.getClickCount() != 2)
                                return;
                            JTree tt = (JTree) e.getSource();
                            TreePath path = tt.getPathForLocation(e.getX(),
                                                                  e.getY());
                            if (path != null) {
                                Object o = path.getLastPathComponent();
                                if (o instanceof MyTreeNode) {
                                    MyTreeNode n = (MyTreeNode) o;
                                    ArrayList<BrowserNode> al = new ArrayList<BrowserNode>();
                                    al.add(n.getBrowserNode());
                                    BrowserFrame.this.db.executeCommand(-1);
                                }
                            }
                        }
                    }
                });

        tree.setRootVisible(false);
        JScrollPane js = new JScrollPane(tree);
        c.add(js);
        
        JMenuBar mb = new JMenuBar();
        JMenu m = new JMenu("Connection");
        JMenuItem mi = new JMenuItem("SQL Window");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    SQLFrame sqlf =
                                            new SQLFrame(BrowserFrame.this);
                                    sqlf.setParent(BrowserFrame.this);
                                    sqlf.showStaggered();
                                }
                            });
        m.add(mi);
        mi = new JMenuItem("About this Data Source");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    about();
                                }
                            });
        m.add(mi);
        mi = new JMenuItem("Save As...");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    save();
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('S', MiscUtils.getMenuShortcutKeyMask()));
        m.add(mi);
        mi = new JMenuItem("Commit...");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    commit();
                                }
                            });
        m.add(mi);
        mi = new JMenuItem("Rollback...");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    rollback();
                                }
                            });
        m.add(mi);
        if (db instanceof JDBCDatabase) {
            mi = new JMenuItem("Reconnect");
            mi.addActionListener(new ActionListener() {
                                    public void actionPerformed(ActionEvent e) {
                                        ((JDBCDatabase) BrowserFrame.this.db).reconnect();
                                    }
                                });
            m.add(mi);
        }
        mi.setAccelerator(KeyStroke.getKeyStroke('R', MiscUtils.SHIFT_MASK | MiscUtils.getMenuShortcutKeyMask()));
        mi = new JMenuItem("Close");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    nuke();
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('W', MiscUtils.getMenuShortcutKeyMask()));
        m.add(mi);
        mb.add(m);

        String[] commands = db.getCommands();
        if (commands != null && commands.length != 0) {
            popupMenu = new JPopupMenu();
            m = new JMenu("Command");
            int cmdNo = 0;
            for (int i = 0; i < commands.length; i++) {
                String cmdStr = commands[i];
                if (cmdStr.equals("-")) {
                    popupMenu.addSeparator();
                    m.addSeparator();
                } else {
                    ActionListener listener = new CommandMenuListener(cmdNo++);
                    mi = new JMenuItem(cmdStr);
                    mi.addActionListener(listener);
                    popupMenu.add(mi);
                    mi = new JMenuItem(cmdStr);
                    mi.addActionListener(listener);
                    if (cmdStr.toLowerCase().contains("reload"))
                        mi.setAccelerator(KeyStroke.getKeyStroke('L', MiscUtils.getMenuShortcutKeyMask()));
                    m.add(mi);
                }
            }
            mb.add(m);
        }

        setJMenuBar(mb);

        pack();
        Main.addBrowser(n);
        instances.add(this);
        for (MyCBM cbm : models.keySet())
            cbm.addElement(this);
    }

    private class CommandMenuListener implements ActionListener {
        private int command;
        public CommandMenuListener(int command) {
            this.command = command;
        }
        public void actionPerformed(ActionEvent e) {
            db.executeCommand(command);
        }
    }
    
    public String toString() {
        return getTitle();
    }

    public ComboBoxModel<BrowserFrame> getOtherInstances() {
        MyCBM cbm = new MyCBM();
        for (BrowserFrame bf : instances)
            cbm.addElement(bf);
        models.put(cbm, null);
        return cbm;
    }

    public Database getDatabase() {
        return db;
    }

    public boolean isDirty() {
        return db.needsCommit() || db.hasOrphans() || super.isDirty();
    }

    private static final String TABLES_FILES_ORPHANS =
                "You have uncommitted changes on one or more tables,\n"
                + "unsaved changes in one or more file editors,\n"
                + "and one or more edited tables left in the orphanage.";
    private static final String TABLES_FILES =
                "You have uncommitted changes on one or more tables\n"
                + "and unsaved changes in one or more file editors.\n";
    private static final String TABLES_ORPHANS =
                "You have uncommitted changes on one or more tables\n"
                + "and one or more edited tables left in the orphanage.";
    private static final String TABLES =
                "You have uncommitted changes on one or more tables.\n";
    private static final String FILES =
                "You have unsaved changes in one or more file editors.\n";
    private static final String FILES_ORPHANS =
                "You have unsaved changes in one or more file editors\n"
                + "and one or more edited tables left in the orphanage.";
    private static final String ORPHANS =
                "You have one or more edited tables left in the orphanage.";

    public void nuke() {
        if (isDirty()) {
            String reason;
            if (db.needsCommit())
                if (super.isDirty())
                    if (db.hasOrphans())
                        reason = TABLES_FILES_ORPHANS;
                            else
                        reason = TABLES_FILES;
                else
                    if (db.hasOrphans())
                        reason = TABLES_ORPHANS;
                    else
                        reason = TABLES;
            else
                if (super.isDirty())
                    if (db.hasOrphans())
                        reason = FILES_ORPHANS;
                            else
                        reason = FILES;
                else
                    reason = ORPHANS;
            Toolkit.getDefaultToolkit().beep();
            if (JOptionPane.showInternalConfirmDialog(
                    Main.getDesktop(),
                    reason
                    + "Closing this connection now would mean those changes\n"
                    + "are lost.\n"
                    + "Close the connection anyway?",
                    "Confirm", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION)
                return;
        }
        dispose();
    }
    
    public void dispose() {
        Main.removeBrowser((Scriptable) tree.getModel().getRoot());
        instances.remove(this);
        for (MyCBM cbm : models.keySet())
            cbm.removeElement(this);
        db.close();
        super.dispose();
    }

    public Collection<BrowserNode> getSelectedNodes() {
        ArrayList<BrowserNode> selection = new ArrayList<BrowserNode>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null)
            return selection;
        for (int i = 0; i < paths.length; i++) {
            TreePath path = paths[i];
            Object o = path.getLastPathComponent();
            if (!(o instanceof MyTreeNode))
                continue;
            BrowserNode bn = ((MyTreeNode) o).getBrowserNode();
            selection.add(bn);
        }
        return selection;
    }

    private void commit() {
        Collection<Table> dirty = db.getDirtyTables();
        if (dirty == null || dirty.isEmpty()) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showInternalMessageDialog(Main.getDesktop(),
                                "There is nothing to commit!");
        } else {
            MultiCommitDialog mcd = new MultiCommitDialog(null, dirty, true);
            mcd.setParent(this);
            mcd.showCentered();
        }
    }

    private void rollback() {
        Collection<Table> dirty = db.getDirtyTables();
        if (dirty == null || dirty.isEmpty()) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showInternalMessageDialog(Main.getDesktop(),
                                "There is nothing to roll back!");
        } else {
            MultiCommitDialog mcd = new MultiCommitDialog(null, dirty, false);
            mcd.setParent(this);
            mcd.showCentered();
        }
    }

    private void about() {
        try {
            String txt = db.about();
            JOptionPane.showInternalMessageDialog(Main.getDesktop(),
                    txt);
        } catch (NavigatorException e) {
            MessageBox.show("Could not get database information.", e);
        }
    }

    private void save() {
        JFileChooser jfc = new MyFileChooser();
        jfc.setDialogTitle("Save File Data Source");
        if (file != null)
            jfc.setSelectedFile(file);
        if (jfc.showSaveDialog(Main.getDesktop())
                                    != JFileChooser.APPROVE_OPTION)
            return;
        File newFile = jfc.getSelectedFile();
        if (newFile.exists()) { 
            Toolkit.getDefaultToolkit().beep();
            if (JOptionPane.showInternalConfirmDialog(
                            Main.getDesktop(),
                            "Overwrite existing " + newFile.getName() + "?",
                            "Confirm", JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE)
                                    != JOptionPane.OK_OPTION)
                return;
        }
        try {
            if (db instanceof FileDatabase) {
                if (db.save(newFile)) {
                    String newName = newFile.getName();
                    setTitle(newName);
                    file = newFile;
                }
            } else {
                if (db.save(newFile))
                    file = newFile;
            }
        } catch (NavigatorException e) {
            MessageBox.show("Error while saving File Data Source.", e);
        }
    }

    private class MyCBM extends DefaultComboBoxModel<BrowserFrame> {
        public void addElement(BrowserFrame element) {
            if (element != BrowserFrame.this)
                super.addElement(element);
        }
    }

    
    private class CreateStatementFunction extends BasicFunction {
        public Object call(Object[] args) {
            if (args.length != 0)
                throw new EvaluatorException(
                    "createStatement() requires no arguments.");
            try {
                return db.createStatement();
            } catch (NavigatorException e) {
                throw new WrappedException(e);
            }
        }
    }

    private class PrepareStatementFunction extends BasicFunction {
        public Object call(Object[] args) {
            if (!(args.length == 1 && args[0] instanceof CharSequence)
                    && !(args.length == 2 && args[0] instanceof CharSequence && args[1] instanceof Boolean))
                throw new EvaluatorException(
                    "prepareStatement() requires one String and an optional Boolean argument.");
            try {
                boolean returnGenKeys = args.length == 2 && args[1].equals(Boolean.TRUE);
                return db.prepareStatement(args[0].toString(), returnGenKeys);
            } catch (NavigatorException e) {
                throw new WrappedException(e);
            }
        }
    }

    private class PrepareCallFunction extends BasicFunction {
        public Object call(Object[] args) {
            if (args.length != 1 || !(args[0] instanceof CharSequence))
                throw new EvaluatorException(
                    "prepareCall() requires a single String argument.");
            try {
                return db.prepareCall(args[0].toString());
            } catch (NavigatorException e) {
                throw new WrappedException(e);
            }
        }
    }

    private class CommitFunction extends BasicFunction {
        public Object call(Object[] args) {
            ArrayList<Table> tables = new ArrayList<Table>();
            for (int i = 0; i < args.length; i++) {
                if (!(args[i] instanceof Table))
                    throw new EvaluatorException(
                            "arg[" + i + "] is not a Table.");
                Table t = (Table) args[i];
                if (t.getDatabase() != db)
                    throw new EvaluatorException(
                            "arg[" + i + "] belongs to a different browser.");
                tables.add(t);
            }
            try {
                db.commitTables(tables);
            } catch (NavigatorException e) {
                throw new WrappedException(e);
            }
            return (int) args.length;
        }
    }

    private CreateStatementFunction createStatementFunction =
                                            new CreateStatementFunction();
    private PrepareStatementFunction prepareStatementFunction =
                                            new PrepareStatementFunction();
    private PrepareCallFunction prepareCallFunction =
                                            new PrepareCallFunction();
    private CommitFunction commitFunction = new CommitFunction();


    private class MyTreeNode implements TreeNode,
                                        BrowserNode.DisplayNode, Scriptable {

        private MyTreeNode parent;
        private ArrayList<TreeNode> kids;
        private BrowserNode browserNode;

        public MyTreeNode(MyTreeNode parent, BrowserNode browserNode) {
            this.parent = parent;
            this.browserNode = browserNode;
            browserNode.setDisplayNode(this);
        }

        public BrowserNode getBrowserNode() {
            return browserNode;
        }

        ////////////////////
        ///// TreeNode /////
        ////////////////////

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public Enumeration children() {
            return new IteratorEnumeration<TreeNode>(kids().iterator());
        }
        
        public boolean getAllowsChildren() {
            return !browserNode.isLeaf();
        }
        
        public TreeNode getChildAt(int index) {
            return kids().get(index);
        }
        
        public int getChildCount() {
            return kids().size();
        }
        
        public int getIndex(TreeNode child) {
            return kids().indexOf(child);
        }
        
        public TreeNode getParent() {
            return parent;
        }
        
        public boolean isLeaf() {
            return browserNode.isLeaf();
        }

        ///////////////////////////////////
        ///// BrowserNode.DisplayNode /////
        ///////////////////////////////////

        public void childAddedAt(int index, BrowserNode kid) {
            if (kids == null)
                return;
            MyTreeNode n = new MyTreeNode(this, kid);
            kids.add(index, n);
            ((DefaultTreeModel) tree.getModel()).nodesWereInserted(
                                                    this,
                                                    new int[] { index });
        }

        public void childRemovedAt(int index) {
            if (kids == null)
                return;
            TreeNode deadKid = kids.remove(index);
            ((DefaultTreeModel) tree.getModel()).nodesWereRemoved(
                                                    this,
                                                    new int[] { index },
                                                    new Object[] { deadKid });
        }

        public void show() {
            ArrayList<Object> al = new ArrayList<Object>();
            MyTreeNode n = this;
            while (n != null) {
                al.add(0, n);
                n = n.parent;
            }
            TreePath path = new TreePath(al.toArray());
            tree.makeVisible(path);
            int row = tree.getRowForPath(path);
            if (row != -1) {
                tree.scrollRowToVisible(row);
                tree.setSelectionRow(row);
            }
        }

        //////////////////////
        ///// MyTreeNode /////
        //////////////////////

        public void preExpand() {
            // Currently not used.
        }

        public void postCollapse() {
            // Currently not used.
        }

        public String toString() {
            return browserNode.getName();
        }

        private ArrayList<TreeNode> kids() {
            if (kids == null) {
                kids = new ArrayList<TreeNode>();
                Iterator<? extends BrowserNode> iter = browserNode.getChildren();
                if (iter != null)
                    while (iter.hasNext()) {
                        BrowserNode bn = iter.next();
                        kids.add(new MyTreeNode(this, bn));
                    }
            }
            return kids;
        }

        ////////////////////////////////
        ///// 'Scriptable' methods /////
        ////////////////////////////////

        public void delete(int index) {
            //
        }
        public void delete(String name) {
            //
        }
        public Object get(int index, Scriptable start) {
            try {
                Object kid = kids().get(index);
                try {
                    Table t = ((MyTreeNode) kid).browserNode.getTable();
                    if (t == null)
                        return kid;
                    else
                        return t;
                } catch (NavigatorException e) {
                    throw new WrappedException(e);
                }
            } catch (IndexOutOfBoundsException e) {
                return NOT_FOUND;
            }
        }
        public Object get(String name, Scriptable start) {
            if (name.equals("length"))
                return (int) kids().size();
            else if (name.equals("name"))
                return toString();
            else if (name.equals("createStatement"))
                return createStatementFunction;
            else if (name.equals("prepareStatement"))
                return prepareStatementFunction;
            else if (name.equals("prepareCall"))
                return prepareCallFunction;
            else if (name.equals("commit"))
                return commitFunction;
            else {
                for (TreeNode kid : kids) {
                    if (kid.toString().equalsIgnoreCase(name)) {
                        try {
                            Table t = ((MyTreeNode) kid).browserNode.getTable();
                            if (t == null)
                                return kid;
                            else
                                return t;
                        } catch (NavigatorException e) {
                            throw new WrappedException(e);
                        }
                    }
                }
                return NOT_FOUND;
            }
        }
        public String getClassName() {
            return getClass().getName();
        }
        public Object getDefaultValue(Class<?> hint) {
            if (isLeaf())
                return toString();

            StringBuffer buf = new StringBuffer();
            buf.append("[");
            for (int i = 0; i < kids().size(); i++) {
                buf.append(" ");
                buf.append(kids().get(i).toString());
            }
            buf.append(" ]");
            return buf.toString();
        }
        public Object[] getIds() {
            return new Object[] {
                "commit",
                "createStatement",
                "length",
                "name",
                "prepareCall",
                "prepareStatement"
            };
        }
        public Scriptable getParentScope() {
            return null;
        }
        public Scriptable getPrototype() {
            return null;
        }
        public boolean has(int index, Scriptable start) {
            return index >= 0 && index < kids().size();
        }
        public boolean has(String name, Scriptable start) {
            if (name.equals("length"))
                return true;
            else if (name.equals("name"))
                return true;
            else if (name.equals("createStatement"))
                return true;
            else if (name.equals("prepareStatement"))
                return true;
            else if (name.equals("prepareCall"))
                return true;
            else if (name.equals("commit"))
                return true;
            else {
                for (TreeNode kid : kids)
                    if (kid.toString().equalsIgnoreCase(name))
                        return true;
                return false;
            }
        }
        public boolean hasInstance(Scriptable instance) {
            return getClass().isInstance(instance);
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
    }
}
