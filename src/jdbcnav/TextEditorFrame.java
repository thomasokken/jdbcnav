///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2010  Thomas Okken
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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.undo.*;

import jdbcnav.util.FileUtils;
import jdbcnav.util.MyTextArea;


public class TextEditorFrame extends MyFrame {
    protected JTextArea textA;
    private boolean unsaved;
    private boolean allowRenaming;
    private File file;
    private UndoManager undoManager = new UndoManager();
    private TableModel model;
    private int row, column;
    private boolean isCellEditor;
    private FindReplaceDialog findReplaceDialog;

    public TextEditorFrame(String name, String text) {
        this(null, name, text, false, true, null, 0, 0);
    }

    public TextEditorFrame(String name, String text, boolean needsSaving,
                           boolean allowRenaming) {
        this(null, name, text, needsSaving, allowRenaming, null, 0, 0);
    }

    public TextEditorFrame(File file, String text) {
        this(file, file.getName(), text, false, true, null, 0, 0);
    }

    public TextEditorFrame(String name, String text, TableModel model,
                           int row, int column) {
        this(null, name, text, false, false, model, row, column);
        isCellEditor = true;
    }

    private TextEditorFrame(File file, String name, String text,
                            boolean needsSaving, boolean allowRenaming,
                            TableModel model, int row, int column) {
        super(name, true, true, true, true);
        unsaved = needsSaving;
        this.allowRenaming = allowRenaming;
        this.file = file;
        this.model = model;
        this.row = row;
        this.column = column;

        setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
        addInternalFrameListener(new InternalFrameAdapter() {
                public void internalFrameClosing(InternalFrameEvent e) {
                    nuke();
                }
            });
        
        Container c = getContentPane();
        c.setLayout(new GridLayout(1, 1));
        textA = new MyTextArea(text, 24, 80);
        if (wantToHandleReturn()) {
            InputMap im = textA.getInputMap();
            KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
            KeyStroke shift_enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                                                InputEvent.SHIFT_MASK);
            Object enterAction = im.get(enter);
            im.put(shift_enter, enterAction);
            im.put(enter, "jdbcnav_handle_return");
            ActionMap am = textA.getActionMap();
            am.put("jdbcnav_handle_return",
                   new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            handleReturn();
                        }
                    });
        }
        textA.setFont(new Font("Courier", Font.PLAIN, 12));
        textA.setCaretPosition(0);
        textA.getDocument().addUndoableEditListener(undoManager);
        JScrollPane js = new JScrollPane(textA);
        js.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        c.add(js);
        
        JMenuBar mb = new JMenuBar();
        JMenu m = new JMenu("File");
        JMenuItem mi = new JMenuItem("Open...");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    open();
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('O', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        m.add(mi);
        mi = new JMenuItem("Merge...");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    merge();
                                }
                            });
        m.add(mi);
        mi = new JMenuItem("Save");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    save();
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('S', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        m.add(mi);
        mi = new JMenuItem("Save As...");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    saveAs();
                                }
                            });
        m.add(mi);
        if (model != null) {
            mi = new JMenuItem("Apply to Table");
            mi.addActionListener(new ActionListener() {
                                    public void actionPerformed(ActionEvent e) {
                                        apply();
                                    }
                                });
            m.add(mi);
        }
        mi = new JMenuItem("Close");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    nuke();
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('W', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        m.add(mi);
        mb.add(m);

        m = new JMenu("Edit");
        mi = new JMenuItem("Undo");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    undo();
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('Z', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        m.add(mi);
        mi = new JMenuItem("Redo");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    redo();
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('Y', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        m.add(mi);
        m.addSeparator();
        mi = new JMenuItem("Cut");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    cut();
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('X', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        m.add(mi);
        mi = new JMenuItem("Copy");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    copy();
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('C', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        m.add(mi);
        mi = new JMenuItem("Paste");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    paste();
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('V', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        m.add(mi);
        mi = new JMenuItem("Clear");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    clear();
                                }
                            });
        m.add(mi);
        mi = new JMenuItem("Select All");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    selectAll();
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('A', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        m.add(mi);
        mb.add(m);

        m = new JMenu("Options");
        mi = new JCheckBoxMenuItem("Wrap Lines");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    JCheckBoxMenuItem mi =
                                        (JCheckBoxMenuItem) e.getSource();
                                    textA.setLineWrap(mi.getState());
                                }
                            });
        m.add(mi);
        mi = new JMenuItem("Find/Replace...");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    find();
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('F', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        m.add(mi);
        mb.add(m);

        setJMenuBar(mb);

        pack();
    }

    private void nuke() {
        if (isDirty()) {
            Toolkit.getDefaultToolkit().beep();
            if (model != null) {
                int result = JOptionPane.showInternalConfirmDialog(
                            Main.getDesktop(),
                            "Apply changes to table cell?",
                            "Confirm", JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                if (result == JOptionPane.CANCEL_OPTION)
                    return;
                if (result == JOptionPane.YES_OPTION)
                    model.setValueAt(textA.getText(), row, column);
                if (file == null) {
                    dispose();
                    return;
                }
            }
            int result = JOptionPane.showInternalConfirmDialog(
                            Main.getDesktop(),
                            (model == null ? "Save changes before closing?"
                                    : "Save changes to file before closing?"),
                            "Confirm", JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.CANCEL_OPTION)
                return;
            if (result == JOptionPane.YES_OPTION)
                if (!save())
                    return;
        }
        dispose();
    }

    private void open() {
        if (isDirty()) {
            Toolkit.getDefaultToolkit().beep();
            int result = JOptionPane.showInternalConfirmDialog(
                            Main.getDesktop(),
                            "Save changes before discarding?",
                            "Confirm", JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.CANCEL_OPTION)
                return;
            if (result == JOptionPane.YES_OPTION)
                if (!save())
                    return;
        }
        load(false);
    }

    private void merge() {
        load(true);
    }

    private void load(boolean merge) {
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogTitle(merge ? "Merge" : "Open");
        if (file != null)
            jfc.setSelectedFile(file);
        if (jfc.showOpenDialog(Main.getDesktop())
                                    == JFileChooser.APPROVE_OPTION) {
            File file = jfc.getSelectedFile();
            String s = null;
            try {
                s = FileUtils.loadTextFile(file);
            } catch (IOException e) {
                MessageBox.show("Error " + (merge ? "merging" : "loading")
                                + " file", e);
                return;
            }

            if (merge) {
                Document doc = textA.getDocument();
                int start = textA.getSelectionStart();
                int end = textA.getSelectionEnd();
                try {
                    if (end > start)
                        doc.remove(start, end - start);
                    doc.insertString(start, s, null);
                    textA.setSelectionStart(start);
                    textA.setSelectionEnd(start + s.length());
                } catch (BadLocationException e) {}
            } else {
                textA.setText(s);
                textA.setCaretPosition(0);
                unsaved = false;
                undoManager.discardAllEdits();
                this.file = file;
                if (allowRenaming || (isCellEditor && model == null)) {
                    setTitle(file.getName());
                    isCellEditor = false;
                }
                if (isCellEditor)
                    unsaved = true;
            }
        }
    }

    private void apply() {
        model.setValueAt(textA.getText(), row, column);
        unsaved = false;
        undoManager.discardAllEdits();
    }

    private boolean save() {
        if (file == null)
            return saveAs();
        return saveAsFile(file);
    }

    private boolean saveAs() {
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogTitle("Save");
        if (file != null)
            jfc.setSelectedFile(file);
        if (jfc.showSaveDialog(Main.getDesktop())
                                    != JFileChooser.APPROVE_OPTION)
            return false;
        File newFile = jfc.getSelectedFile();
        if (newFile.exists()) {
            Toolkit.getDefaultToolkit().beep();
            if (JOptionPane.showInternalConfirmDialog(
                            Main.getDesktop(),
                            "Overwrite existing " + newFile.getName() + "?",
                            "Confirm", JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE)
                                    == JOptionPane.CANCEL_OPTION)
                return false;
        }

        return saveAsFile(newFile);
    }

    private boolean saveAsFile(File file) {
        try {
            FileUtils.saveTextFile(file, textA.getText());
            unsaved = false;
            undoManager.discardAllEdits();
            this.file = file;
            if (allowRenaming || (isCellEditor && model == null)) {
                setTitle(file.getName());
                isCellEditor = false;
            }
            return true;
        } catch (IOException e) {
            MessageBox.show("Save failed.", e);
            return false;
        }
    }

    public void undo() {
        try {
            undoManager.undo();
        } catch (CannotUndoException e) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void redo() {
        try {
            undoManager.redo();
        } catch (CannotRedoException e) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void cut() {
        textA.cut();
    }

    private void copy() {
        textA.copy();
    }

    private void paste() {
        textA.paste();
    }

    private void clear() {
        Document doc = textA.getDocument();
        int start = textA.getSelectionStart();
        int end = textA.getSelectionEnd();
        try {
            doc.remove(start, end - start);
            textA.setCaretPosition(start);
        } catch (BadLocationException e) {}
    }

    private void selectAll() {
        textA.setSelectionStart(0);
        textA.setSelectionEnd(textA.getDocument().getLength());
    }

    private void find() {
        if (findReplaceDialog != null) {
            findReplaceDialog.deiconifyAndRaise();
        } else {
            findReplaceDialog = new FindReplaceDialog(this);
            findReplaceDialog.setParent(this);
            findReplaceDialog.showCentered();
        }
    }

    protected boolean wantToHandleReturn() {
        // To be overridden to 'return true' by subclasses that want to
        // do something when the user types return
        return false;
    }

    protected void handleReturn() {
        // To be overridden by subclasses that want to do something
        // when the user types return
    }

    public boolean isDirty() {
        return unsaved || undoManager.canUndo() || super.isDirty();
    }

    // Callbacks for FindReplaceDialog

    public void find(String find, boolean down, boolean wholeWord,
                     boolean matchCase) {
        String text = textA.getText();
        int selBegin = textA.getCaret().getMark();
        int selEnd = textA.getCaret().getDot();
        if (selBegin > selEnd) {
            int temp = selBegin;
            selBegin = selEnd;
            selEnd = temp;
        }
        int pos;
        if (down) {
            pos = scanDown(text, find, selEnd, wholeWord, matchCase);
            if (pos == -1) {
                if (!askWrap(down))
                    return;
                pos = scanDown(text, find, 0, wholeWord, matchCase);
                if (pos == -1) {
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
            }
        } else {
            pos = scanUp(text, find, selBegin, wholeWord, matchCase);
            if (pos == -1) {
                if (!askWrap(down))
                    return;
                pos = scanUp(text, find, text.length(), wholeWord, matchCase);
                if (pos == -1) {
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
            }
        }
        textA.getCaret().setDot(pos);
        textA.getCaret().moveDot(pos + find.length());
    }

    public void replace(String replace) {
        textA.replaceSelection(replace);
    }

    public void replaceThenFind(String find, String replace, boolean down,
                                boolean wholeWord, boolean matchCase) {
        replace(replace);
        find(find, down, wholeWord, matchCase);
    }

    public void replaceAll(String find, String replace, boolean wholeWord,
                           boolean matchCase) {
        StringBuffer buf = new StringBuffer(textA.getText());
        int pos = 0;
        int findLength = find.length();
        int replaceLength = replace.length();
        int changes = 0;
        if (matchCase) {
            while (true) {
                pos = buf.indexOf(find, pos);
                if (pos == -1)
                    break;
                if (!wholeWord || isWord(buf, pos, findLength)) {
                    buf.replace(pos, pos + findLength, replace);
                    pos += replaceLength;
                    changes++;
                } else {
                    pos += findLength;
                    if (pos > buf.length())
                        break;
                }
            }
        } else {
            while (pos + findLength <= buf.length()) {
                if (buf.substring(pos, pos +
                            findLength).equalsIgnoreCase(find)
                        && (!wholeWord || isWord(buf, pos, findLength))) {
                    buf.replace(pos, pos + findLength, replace);
                    pos += replaceLength;
                    changes++;
                } else
                    pos++;
            }
        }
        if (changes == 0) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showInternalMessageDialog(Main.getDesktop(),
                    "The search string was not found.");
        } else {
            textA.setText(buf.toString());
            textA.setCaretPosition(0);
            JOptionPane.showInternalMessageDialog(Main.getDesktop(),
                    Integer.toString(changes) + " subsitution"
                    + (changes == 1 ? "" : "s") + " made.");
        }
    }

    private boolean askWrap(boolean down) {
        return JOptionPane.showInternalConfirmDialog(
                    Main.getDesktop(),
                    "Continue searching from " + (down ? "beginning" : "end")
                    + " of text?",
                    "Confirm", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE)
                                    == JOptionPane.YES_OPTION;
    }

    private int scanDown(String text, String find, int pos, boolean wholeWord,
                         boolean matchCase) {
        int newPos;
        if (matchCase)
            newPos = text.indexOf(find, pos);
        else
            newPos = caseInsensitiveSearchDown(text, find, pos);
        if (newPos != -1 && wholeWord && !isWord(text, newPos, find.length()))
            return scanDown(text, find, newPos + find.length(), wholeWord,
                            matchCase);
        else
            return newPos;
    }

    private int scanUp(String text, String find, int pos, boolean wholeWord,
                       boolean matchCase) {
        int newPos;
        if (matchCase)
            newPos = text.lastIndexOf(find, pos - find.length());
        else
            newPos = caseInsensitiveSearchUp(text, find, pos - find.length());
        if (newPos != -1 && wholeWord && !isWord(text, newPos, find.length()))
            return scanUp(text, find, newPos, wholeWord, matchCase);
        else
            return newPos;
    }

    private boolean isWord(String text, int offset, int length) {
        if (offset > 0) {
            char c = text.charAt(offset - 1);
            if (Character.isLetterOrDigit(c) || c == '_')
                return false;
        }
        if (offset + length < text.length()) {
            char c = text.charAt(offset + length);
            if (Character.isLetterOrDigit(c) || c == '_')
                return false;
        }
        return true;
    }

    private boolean isWord(StringBuffer text, int offset, int length) {
        if (offset > 0) {
            char c = text.charAt(offset - 1);
            if (Character.isLetterOrDigit(c) || c == '_')
                return false;
        }
        if (offset + length < text.length()) {
            char c = text.charAt(offset + length);
            if (Character.isLetterOrDigit(c) || c == '_')
                return false;
        }
        return true;
    }

    private int caseInsensitiveSearchDown(String text, String find, int pos) {
        int findLength = find.length();
        int maxPos = text.length() - findLength;
        while (pos < maxPos) {
            if (text.substring(pos, pos + findLength).equalsIgnoreCase(find))
                return pos;
            pos++;
        }
        return -1;
    }

    private int caseInsensitiveSearchUp(String text, String find, int pos) {
        int findLength = find.length();
        while (pos >= 0) {
            if (text.substring(pos, pos + findLength).equalsIgnoreCase(find))
                return pos;
            pos--;
        }
        return -1;
    }

    public void updateTitle() {
        if (isCellEditor)
            setTitle(getParentTitle() + " [" + row + ", " + column + "]");
    }

    public void childDisposed(MyFrame child) {
        if (child == findReplaceDialog)
            findReplaceDialog = null;
    }
}
