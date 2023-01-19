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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.undo.*;

import jdbcnav.util.FileUtils;
import jdbcnav.util.MiscUtils;
import jdbcnav.util.MyGridBagConstraints;
import jdbcnav.util.MyGridBagLayout;
import jdbcnav.util.NonTabJTextArea;


public class BinaryEditorFrame extends MyFrame {
    private BinaryDataManager datamgr;
    private boolean unsaved;
    private boolean allowRenaming;
    private File file;
    private UndoManager undoManager = new MyUndoManager();
    private TableModel model;
    private int row, column;
    private boolean isCellEditor;
    JMenuItem undoMI, redoMI;

    public BinaryEditorFrame(String name, byte[] data) {
        this(null, name, data, false, true, null, 0, 0);
    }

    public BinaryEditorFrame(File file, byte[] data) {
        this(file, file.getName(), data, false, true, null, 0, 0);
    }

    public BinaryEditorFrame(String name, byte[] data, TableModel model,
                           int row, int column) {
        this(null, name, data, false, false, model, row, column);
        isCellEditor = true;
    }

    private BinaryEditorFrame(File file, String name, byte[] data,
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

        if (data == null)
            data = new byte[1];
        datamgr = new BinaryDataManager(data, undoManager);

        JTextArea addrA = datamgr.makeTextArea(
                                        datamgr.getAddrDoc(), null, 24, 9);
        JTextArea hexA = datamgr.makeTextArea(
                                        datamgr.getHexDoc(), null, 24, 49);
        JTextArea asciiA = datamgr.makeTextArea(
                                        datamgr.getAsciiDoc(), null, 24, 17);
        addrA.setFont(new Font("Courier", Font.PLAIN, 12));
        addrA.setCaret(new MyCaret());
        hexA.setFont(new Font("Courier", Font.PLAIN, 12));
        hexA.setCaret(new MyCaret());
        asciiA.setFont(new Font("Courier", Font.PLAIN, 12));
        asciiA.setCaret(new MyCaret());
        datamgr.setTextAreas(addrA, hexA, asciiA);

        // TODO: create a subclass of JPanel that implements Scrollable, so
        // we can get the right kind of behavior w.r.t. window sizing (no need
        // to limit the window size like we currently to at the end of this
        // constructor!) and scroll increments... Using values we get by
        // forwarding the appropriate Scrollable method calls to the JTextAreas
        // we contain.

        JPanel p = new JPanel();
        p.setLayout(new MyGridBagLayout());
        MyGridBagConstraints gbc = new MyGridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.gridwidth = gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 8, 0, 0);
        gbc.fill = MyGridBagConstraints.BOTH;
        p.add(addrA, gbc);
        gbc.gridx++;
        p.add(hexA, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        p.add(asciiA, gbc);
        p.setBackground(addrA.getBackground());

        JScrollPane js = new JScrollPane(p);
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
        mi.setAccelerator(KeyStroke.getKeyStroke('O', MiscUtils.getMenuShortcutKeyMask()));
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
        mi.setAccelerator(KeyStroke.getKeyStroke('S', MiscUtils.getMenuShortcutKeyMask()));
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
        mi.setAccelerator(KeyStroke.getKeyStroke('W', MiscUtils.getMenuShortcutKeyMask()));
        m.add(mi);
        mb.add(m);

        m = new JMenu("Edit");
        undoMI = new JMenuItem("Undo");
        undoMI.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    undo();
                                }
                            });
        undoMI.setAccelerator(KeyStroke.getKeyStroke('Z', MiscUtils.getMenuShortcutKeyMask()));
        m.add(undoMI);
        redoMI = new JMenuItem("Redo");
        redoMI.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    redo();
                                }
                            });
        redoMI.setAccelerator(KeyStroke.getKeyStroke('Y', MiscUtils.getMenuShortcutKeyMask()));
        m.add(redoMI);
        m.addSeparator();
        mi = new JMenuItem("Cut");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    cut();
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('X', MiscUtils.getMenuShortcutKeyMask()));
        m.add(mi);
        mi = new JMenuItem("Copy");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    copy();
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('C', MiscUtils.getMenuShortcutKeyMask()));
        m.add(mi);
        mi = new JMenuItem("Paste");
        mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    paste();
                                }
                            });
        mi.setAccelerator(KeyStroke.getKeyStroke('V', MiscUtils.getMenuShortcutKeyMask()));
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
        mi.setAccelerator(KeyStroke.getKeyStroke('A', MiscUtils.getMenuShortcutKeyMask()));
        m.add(mi);
        mb.add(m);

        setJMenuBar(mb);

        pack();

        Dimension mysize = getSize();
        Dimension dtsize = Main.getDesktop().getSize();
        if (mysize.height > dtsize.height) {
            mysize.height = dtsize.height;
            setSize(mysize);
        }
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
                if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION)
                    return;
                if (result == JOptionPane.YES_OPTION)
                    model.setValueAt(datamgr.getData(), row, column);
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
            if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION)
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
            if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION)
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
            byte[] data = null;
            try {
                data = FileUtils.loadBinaryFile(file);
            } catch (IOException e) {
                MessageBox.show("Error " + (merge ? "merging" : "loading")
                                + " file", e);
                return;
            }

            if (merge) {
                datamgr.merge(data);
            } else {
                datamgr.load(data);
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
        model.setValueAt(datamgr.getData(), row, column);
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
                                    != JOptionPane.OK_OPTION)
                return false;
        }

        return saveAsFile(newFile);
    }

    private boolean saveAsFile(File file) {
        try {
            FileUtils.saveBinaryFile(file, datamgr.getData());
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
        datamgr.cut();
    }

    private void copy() {
        datamgr.copy();
    }

    private void paste() {
        datamgr.paste();
    }

    private void clear() {
        datamgr.clear();
    }

    private void selectAll() {
        datamgr.selectAll();
    }

    public boolean isDirty() {
        return unsaved || undoManager.canUndo() || super.isDirty();
    }

    public void updateTitle() {
        if (isCellEditor)
            setTitle(getParentTitle() + " [" + row + ", " + column + "]");
    }

    private class MyCaret extends DefaultCaret {
        public void install(JTextComponent c) {
            super.install(c);
            setSelectionVisible(true);
        }

        public void setSelectionVisible(boolean vis) {
            if (vis)
                super.setSelectionVisible(vis);
        }
    }

    private static class BinaryDataManager {
        private byte[] data;

        private JTextArea addrTA;
        private JTextArea hexTA;
        private JTextArea asciiTA;
        private AddrDocument addrDoc;
        private HexDocument hexDoc;
        private AsciiDocument asciiDoc;
        private boolean inCaretUpdate = false;
        private UndoManager undoManager;

        public BinaryDataManager(byte[] data, UndoManager undoManager) {
            this.data = data;
            this.undoManager = undoManager;
            addrDoc = new AddrDocument();
            hexDoc = new HexDocument();
            asciiDoc = new AsciiDocument();
        }

        public void setTextAreas(JTextArea addrArea, JTextArea hexArea,
                                                        JTextArea asciiArea) {
            addrArea.setEditable(false);
            addrArea.getCaret().setVisible(false);
            addrArea.addCaretListener(addrDoc);
            hexArea.addCaretListener(hexDoc);
            asciiArea.addCaretListener(asciiDoc);
            addrTA = addrArea;
            hexTA = hexArea;
            asciiTA = asciiArea;
        }

        public Document getAddrDoc() {
            return addrDoc;
        }

        public Document getHexDoc() {
            return hexDoc;
        }

        public Document getAsciiDoc() {
            return asciiDoc;
        }

        public byte[] getData() {
            return data;
        }

        public void load(byte[] data) {
            UndoableEdit edit = new ReplaceBytes(data, 0, this.data.length,
                                                 "Load");
            edit.redo();
            undoManager.addEdit(edit);
        }

        public void merge(byte[] data) {
            int start = hexDoc.getSelectionStart() / 2;
            int end = (hexDoc.getSelectionEnd() + 1) / 2;
            UndoableEdit edit = new ReplaceBytes(data, start, end, "Merge");
            edit.redo();
            undoManager.addEdit(edit);
        }

        public void cut() {
            copy();
            int start = hexDoc.getSelectionStart() / 2;
            int end = (hexDoc.getSelectionEnd() + 1) / 2;
            UndoableEdit edit = new ReplaceBytes(new byte[0], start, end,
                                                 "Cut");
            edit.redo();
            undoManager.addEdit(edit);
        }

        public void copy() {
            int start = hexDoc.getSelectionStart() / 2;
            int end = (hexDoc.getSelectionEnd() + 1) / 2;
            byte[] cp = new byte[end - start];
            System.arraycopy(data, start, cp, 0, end - start);
            Main.getClipboard().put(cp);
        }

        public void paste() {
            Object clipObj = Main.getClipboard().get();
            byte[] clip;
            if (clipObj instanceof byte[])
                clip = (byte[]) clipObj;
            else if (clipObj instanceof String) {
                clip = ((String) clipObj).getBytes();
            } else {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            int start = hexDoc.getSelectionStart() / 2;
            int end = (hexDoc.getSelectionEnd() + 1) / 2;
            UndoableEdit edit = new ReplaceBytes(clip, start, end, "Paste");
            edit.redo();
            undoManager.addEdit(edit);
        }

        public void clear() {
            int start = hexDoc.getSelectionStart() / 2;
            int end = (hexDoc.getSelectionEnd() + 1) / 2;
            UndoableEdit edit = new ReplaceBytes(new byte[0], start, end,
                                                 "Clear");
            edit.redo();
            undoManager.addEdit(edit);
        }

        public void selectAll() {
            inCaretUpdate = true;
            addrDoc.caretUpdate(0, data.length * 2);
            hexDoc.caretUpdate(0, data.length * 2);
            asciiDoc.caretUpdate(0, data.length * 2);
            inCaretUpdate = false;
        }


        private class ReplaceBytes implements UndoableEdit {

            // In this class, 'start' and 'end' represent byte offsets within
            // the byte array -- unlike in most other places within
            // BinaryDataManager, where the unit of choice is nybble offsets.

            private byte[] data;
            private int start;
            private int end;
            private String name;
            public ReplaceBytes(byte[] data, int start, int end, String name) {
                this.data = data;
                this.start = start;
                this.end = end;
                this.name = name;
            }
            public void undo() {
                redo2();
                // Notice anything weird here?
                // undo() and redo() both simply swap this.data and
                // BinaryDataManager.this.data (and update 'end' accordingly).
                // The assumption is that undo() and redo() will always be
                // called alternatingly (anything else would make no sense
                // anyway!).
            }
            public boolean canUndo() {
                return true;
            }
            public void redo() {
                redo2();
                updateCaret();
            }
            private void redo2() {
                byte[] bdmdata = BinaryDataManager.this.data;
                if (data.length == end - start) {
                    // System.arrayswap() would be nice here!
                    // Still, this could be done more efficiently by doing
                    // it in chunks and using System.arraycopy() on those
                    // chunks, but as always, I have too much TODO already.
                    for (int i = 0; i < data.length; i++) {
                        byte b = data[i];
                        data[i] = bdmdata[i + start];
                        bdmdata[i + start] = b;
                    }
                } else if (bdmdata.length == end - start) {
                    end = data.length;
                    byte[] temp = data;
                    data = bdmdata;
                    BinaryDataManager.this.data = temp;
                } else {
                    byte[] newbdmdata = new byte[bdmdata.length + data.length
                                                - (end - start)];
                    byte[] newdata = new byte[end - start];
                    System.arraycopy(bdmdata, start, newdata, 0, end - start);
                    System.arraycopy(bdmdata, 0, newbdmdata, 0, start);
                    System.arraycopy(data, 0, newbdmdata, start, data.length);
                    System.arraycopy(bdmdata, end, newbdmdata,
                                    start + data.length, bdmdata.length - end);
                    end = start + data.length;
                    data = newdata;
                    BinaryDataManager.this.data = newbdmdata;
                }
                inCaretUpdate = true;
                int ss = start * 2;
                int ee = (start + data.length) * 2;
                int ll = (end - start) * 2;
                addrDoc.dataChanged();
                hexDoc.dataChanged(ss, ee, ll);
                asciiDoc.dataChanged(ss, ee, ll);
                inCaretUpdate = false;
            }
            protected void updateCaret() {
                inCaretUpdate = true;
                addrDoc.caretUpdate(end * 2, start * 2);
                hexDoc.caretUpdate(end * 2, start * 2);
                asciiDoc.caretUpdate(end * 2, start * 2);
                inCaretUpdate = false;
            }
            public boolean canRedo() {
                return true;
            }
            public void die() {
                // Nothing to do
            }
            public boolean addEdit(UndoableEdit anEdit) {
                return false;
            }
            public boolean replaceEdit(UndoableEdit anEdit) {
                return false;
            }
            public boolean isSignificant() {
                return true;
            }
            public String getPresentationName() {
                return name;
            }
            public String getUndoPresentationName() {
                return "Undo " + name;
            }
            public String getRedoPresentationName() {
                return "Redo " + name;
            }
        }


        private class Typing extends ReplaceBytes {
            // Note: 'start' and 'end' are byte offsets;
            // 'dotAfter' and 'markAfter' are nybble offsets!
            private int dot, mark;
            public Typing(byte[] data, int start, int end, int dotAfter,
                          int markAfter) {
                super(data, start, end, "Typing");
                dot = dotAfter;
                mark = markAfter;
            }
            protected void updateCaret() {
                inCaretUpdate = true;
                addrDoc.caretUpdate(dot, mark);
                hexDoc.caretUpdate(dot, mark);
                asciiDoc.caretUpdate(dot, mark);
                inCaretUpdate = false;
            }
        }


        private static final String nybble = "0123456789abcdef";

        private static String byteToHex(int b) {
            int n1 = (b >> 4) & 15;
            int n2 = b & 15;
            return "" + nybble.charAt(n1) + nybble.charAt(n2);
        }

        private static String intToHex(int n) {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < 8; i++) {
                buf.append(nybble.charAt((n >> 28) & 15));
                n <<= 4;
            }
            return buf.toString();
        }

        private static String lineToHex(byte[] data, int addr) {
            int max = 16;
            if (addr + max > data.length)
                max = data.length - addr;
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < max; i++) {
                buf.append(byteToHex(data[addr + i]));
                if (i != max - 1) {
                    buf.append(' ');
                    if (i == 7)
                        buf.append(' ');
                }
            }
            buf.append('\n');
            return buf.toString();
        }
        
        private static String lineToAscii(byte[] data, int addr) {
            StringBuffer buf = new StringBuffer();
            int max = 16;
            if (addr + max > data.length)
                max = data.length - addr;
            for (int i = 0; i < max; i++) {
                int c = data[addr + i] & 255;
                buf.append(c < 32 ? '.' : (char) c);
            }
            buf.append('\n');
            return buf.toString();
        }



        private class AddrDocument implements Document, CaretListener {
            private ArrayList<Element> elements;
            private ArrayList<DocumentListener> listeners;
            private Element root;
            private int dot = 0;
            private int mark = 0;
            public AddrDocument() {
                root = new AddrRootElement();
                elements = new ArrayList<Element>();
                listeners = new ArrayList<DocumentListener>();
                int lines = (data.length + 15) / 16;
                if (lines == 0)
                    lines = 1;
                for (int i = 0; i < lines; i++)
                    elements.add(new AddrElement(i));
            }
            public void dataChanged() {
                int oldlines = elements.size();
                int newlines = (data.length + 15) / 16;
                if (newlines == 0)
                    newlines = 1;
                if (newlines == oldlines)
                    return;
                if (newlines < oldlines) {
                    int start = newlines * 9;
                    int length = getLength() - start;
                    RemovedEvent removed = new RemovedEvent(this, root,
                                                start, length, newlines);
                    for (int i = newlines; i < oldlines; i++)
                        removed.add(elements.remove(newlines));
                    for (DocumentListener listener : listeners)
                        listener.removeUpdate(removed);
                } else {
                    int start = getLength();
                    int length = newlines * 9 - start;
                    InsertedEvent inserted = new InsertedEvent(this, root,
                                                start, length, oldlines);
                    for (int i = oldlines; i < newlines; i++) {
                        Element line = new AddrElement(i);
                        inserted.add(line);
                        elements.add(line);
                    }
                    for (DocumentListener listener : listeners)
                        listener.insertUpdate(inserted);
                }
            }
            public void caretUpdate(CaretEvent e) {
                if (inCaretUpdate)
                    return;
                inCaretUpdate = true;
                int mark = e.getMark();
                int dot = e.getDot();
                int newdot, newmark;
                if (mark <= dot) {
                    newmark = mark / 9 * 32;
                    newdot = dot / 9 * 32 + 32;
                    if (this.dot > data.length * 2)
                        this.dot = data.length * 2;
                } else {
                    newdot = dot / 9 * 32;
                    newmark = mark / 9 * 32 + 32;
                    if (this.mark > data.length * 2)
                        this.mark = data.length * 2;
                }
                if (newmark != this.mark || newdot != this.dot) {
                    hexDoc.caretUpdate(newdot, newmark);
                    asciiDoc.caretUpdate(newdot, newmark);
                    this.dot = newdot;
                    this.mark = newmark;
                }
                inCaretUpdate = false;
            }
            public void caretUpdate(int dot, int mark) {
                Caret c = addrTA.getCaret();
                boolean markMoved = mark != this.mark;
                if (markMoved) {
                    if (mark < dot)
                        c.setDot((mark / 32) * 9);
                    else
                        c.setDot(((mark - 1) / 32) * 9 + 8);
                    this.mark = mark;
                }
                if (markMoved || dot != this.dot) {
                    if (mark < dot)
                        c.moveDot(((dot - 1) / 32) * 9 + 8);
                    else
                        c.moveDot((dot / 32) * 9);
                    this.dot = dot;
                }
            }
            public int getLength() {
                return elements.size() * 9;
            }
            public void addDocumentListener(DocumentListener listener) {
                listeners.add(listener);
            }
            public void removeDocumentListener(DocumentListener listener) {
                listeners.remove(listener);
            }
            public void addUndoableEditListener(UndoableEditListener lsnr) {
                // Not supported
            }
            public void removeUndoableEditListener(UndoableEditListener lsnr) {
                // Not supported
            }
            public Object getProperty(Object key) {
                // Not supported
                return null;
            }
            public void putProperty(Object key, Object value) {
                // Not supported
            }
            public void remove(int offs, int len) throws BadLocationException {
                throw new UnsupportedOperationException(
                            "AddrDocument may not be modified.");
            }
            public void insertString(int offset, String str, AttributeSet a)
                                                throws BadLocationException {
                throw new UnsupportedOperationException(
                            "AddrDocument may not be modified.");
            }
            public String getText(int offset, int length)
                                                throws BadLocationException {
                int endpos = offset + length;
                StringBuffer buf = new StringBuffer();
                int lineno = offset / 9;
                int addr = lineno * 16;
                int pos = offset - 9 * lineno;
                while (offset + 9 - pos <= endpos) {
                    buf.append(intToHex(addr).substring(pos));
                    buf.append('\n');
                    offset += 9 - pos;
                    addr += 16;
                    pos = 0;
                }
                if (offset < endpos)
                    buf.append(intToHex(addr).substring(pos,
                                                pos + endpos - offset));
                return buf.toString();
            }
            public void getText(int offset, int length, Segment txt)
                                                throws BadLocationException {
                txt.array = getText(offset, length).toCharArray();
                txt.offset = 0;
                txt.count = length;
            }
            public Position getStartPosition() {
                return new MyPosition(0);
            }
            public Position getEndPosition() {
                return new MyPosition(getLength());
            }
            public Position createPosition(int offs)
                                                throws BadLocationException {
                return new MyPosition(offs);
            }
            public Element[] getRootElements() {
                return new Element[] { root };
            }
            public Element getDefaultRootElement() {
                return root;
            }
            public void render(Runnable r) {
                // We don't do the thread-safety stuff; whatever happens in
                // BinaryDataManager is assumed to happen on the Swing event
                // thread.
                r.run();
            }

            private class AddrElement implements Element {
                private int lineno;
                public AddrElement(int lineno) {
                    this.lineno = lineno;
                }
                public AttributeSet getAttributes() {
                    return null;
                }
                public Document getDocument() {
                    return AddrDocument.this;
                }
                public Element getElement(int index) {
                    return null;
                }
                public int getElementCount() {
                    return 0;
                }
                public int getElementIndex(int offset) {
                    return 0;
                }
                public int getEndOffset() {
                    return 9 * (lineno + 1);
                }
                public String getName() {
                    return "addr[" + lineno + "]";
                }
                public Element getParentElement() {
                    return root;
                }
                public int getStartOffset() {
                    return 9 * lineno;
                }
                public boolean isLeaf() {
                    return true;
                }
            }

            private class AddrRootElement implements Element {
                public AddrRootElement() {
                    // Nothing to do
                }
                public AttributeSet getAttributes() {
                    return null;
                }
                public Document getDocument() {
                    return AddrDocument.this;
                }
                public Element getElement(int index) {
                    return elements.get(index);
                }
                public int getElementCount() {
                    return elements.size();
                }
                public int getElementIndex(int offset) {
                    int index = offset / 9;
                    if (index < 0)
                        return 0;
                    else if (index >= elements.size())
                        return elements.size() - 1;
                    else
                        return index;
                }
                public int getEndOffset() {
                    return getLength();
                }
                public String getName() {
                    return "addr";
                }
                public Element getParentElement() {
                    return null;
                }
                public int getStartOffset() {
                    return 0;
                }
                public boolean isLeaf() {
                    return false;
                }
            }
        }
        
        
        private class HexDocument implements Document, CaretListener {
            private ArrayList<Element> elements;
            private ArrayList<DocumentListener> listeners;
            private Element root;
            private int dot = 0;
            private int mark = 0;
            public HexDocument() {
                root = new HexRootElement();
                elements = new ArrayList<Element>();
                listeners = new ArrayList<DocumentListener>();
                int lines = (data.length + 15) / 16;
                if (lines == 0)
                    lines = 1;
                for (int i = 0; i < lines; i++)
                    elements.add(new HexElement(i));
            }
            public void dataChanged(int s, int e, int l) {
                int start = addr2doc(s);
                int end = addr2doc(e);
                int length = addr2doc(s + l) - start;

                int oldlines = elements.size();
                int newlines = (data.length + 15) / 16;
                if (newlines == 0)
                    newlines = 1;
                
                int clen = end - start;
                if (clen > length)
                    clen = length;

                if (clen < length) {
                    InsertedEvent inserted = new InsertedEvent(this, root,
                                        start + clen, length - clen,
                                        oldlines);
                    for (int i = oldlines; i < newlines; i++) {
                        Element line = new HexElement(i);
                        inserted.add(line);
                        elements.add(line);
                    }
                    fireInserted(listeners, inserted);
                } else if (clen < end - start) {
                    RemovedEvent removed = new RemovedEvent(this, root,
                                        start + clen, end - start - clen,
                                        newlines);
                    for (int i = newlines; i < oldlines; i++)
                        removed.add(elements.remove(newlines));
                    fireRemoved(listeners, removed);
                }

                if (end - start != length) {
                    // Text moved; this means the change extends to the end
                    // of the text.
                    clen = getLength() - start;
                }
                if (clen != 0) {
                    // Aargh!!! PlainView (and God knows who else) does not
                    // even look at the 'length' property of a 'change' event;
                    // it simply finds the elements that matches the 'offset'
                    // property and repaints it, and screw everything else.
                    // So, we have to fire a ChangedEvent for each and every
                    // line individually.

                    int firstline = s / 32;
                    int lastline = elements.size();
                    int totallength = getLength();
                    for (int i = firstline; i < lastline; i++) {
                        int off = i * 49;
                        int len = 49;
                        if (off + len > totallength)
                            len = totallength - off;
                        ChangedEvent changed = new ChangedEvent(this, off, len);
                        fireChanged(listeners, changed);
                    }
                }
            }
            public int getSelectionStart() {
                return dot < mark ? dot : mark;
            }
            public int getSelectionEnd() {
                return dot < mark ? mark : dot;
            }
            public void caretUpdate(CaretEvent e) {
                if (inCaretUpdate)
                    return;
                inCaretUpdate = true;
                int mark = e.getMark();
                int dot = e.getDot();
                int newmark = doc2addr(mark);
                int newdot = doc2addr(dot);
                if (newmark != this.mark || newdot != this.dot) {
                    addrDoc.caretUpdate(newdot, newmark);
                    asciiDoc.caretUpdate(newdot, newmark);
                    this.dot = newdot;
                    this.mark = newmark;
                }
                inCaretUpdate = false;
            }
            public void caretUpdate(int dot, int mark) {
                this.dot = dot;
                this.mark = mark;
                Caret c = hexTA.getCaret();
                int newdot, newmark;
                if (mark == dot) {
                    newmark = newdot = addr2doc(mark - 1) + 1;
                } else if (mark < dot) {
                    newmark = addr2doc(mark);
                    newdot = addr2doc(dot - 1) + 2;
                } else {
                    newmark = addr2doc(mark - 1) + 2;
                    newdot = addr2doc(dot);
                }
                boolean markMoved = newmark != c.getMark();
                if (markMoved)
                    c.setDot(newmark);
                if (markMoved || newdot != c.getDot())
                    c.moveDot(newdot);
            }
            private int addr2doc(int addr) {
                int lineno = addr / 32;
                int remainder = addr % 32;
                remainder += remainder / 2;
                if (remainder > 23)
                    remainder++;
                return (lineno * 49) + remainder;
            }
            private int doc2addr(int doc) {
                int lineno = doc / 49;
                int addr = lineno * 32;
                int remainder = doc - 49 * lineno;
                if (remainder > 23)
                    remainder--;
                addr += ((remainder + 1) * 2) / 3;
                return addr;
            }
            public int getLength() {
                int lines = data.length / 16;
                int remainder = data.length - 16 * lines;
                int len = lines * 49;
                if (remainder != 0) {
                    len += 3 * remainder;
                    if (remainder > 8)
                        len++;
                }
                return len;
            }
            public void addDocumentListener(DocumentListener listener) {
                listeners.add(listener);
            }
            public void removeDocumentListener(DocumentListener listener) {
                listeners.remove(listener);
            }
            public void addUndoableEditListener(UndoableEditListener lsnr) {
                // Not supported
            }
            public void removeUndoableEditListener(UndoableEditListener lsnr) {
                // Not supported
            }
            public Object getProperty(Object key) {
                // Not supported
                return null;
            }
            public void putProperty(Object key, Object value) {
                // Not supported
            }
            public void remove(int offs, int len) throws BadLocationException {
                int startNybble = doc2addr(offs);
                int endNybble = doc2addr(offs + len);
                if (startNybble == endNybble) {
                    // User hit backspace while in the whitespace separating
                    // two bytes
                    startNybble--;
                }
                int start = startNybble / 2;
                int end = (endNybble + 1) / 2;
                byte[] newdata;
                int newdot = startNybble;
                if ((startNybble & 1) != 0) {
                    byte b = (byte) (data[start] & 0xF0);
                    newdata = new byte[] { b };
                } else {
                    newdata = new byte[0];
                }
                UndoableEdit edit = new Typing(newdata, start, end, newdot,
                                               newdot);
                edit.redo();
                undoManager.addEdit(edit);
            }
            public void insertString(int offset, String str, AttributeSet a)
                                                throws BadLocationException {
                if (str.length() != 1)
                    return;
                int nybble;
                char c = str.charAt(0);
                if (c >= '0' && c <= '9')
                    nybble = c - '0';
                else if (c >= 'a' && c <= 'f')
                    nybble = c - 'a' + 10;
                else if (c >= 'A' && c <= 'F')
                    nybble = c - 'A' + 10;
                else
                    return;

                int addr = doc2addr(offset);
                int start = addr / 2;
                int newdot = addr + 1;
                UndoableEdit edit;

                if ((addr & 1) != 0) {
                    byte b = (byte) ((data[start] & 0xF0) + nybble);
                    edit = new Typing(new byte[] { b }, start, start + 1,
                                      newdot, newdot);
                } else {
                    byte b = (byte) (nybble << 4);
                    edit = new Typing(new byte[] { b }, start, start,
                                      newdot, newdot);
                }
                edit.redo();
                undoManager.addEdit(edit);
            }
            public String getText(int offset, int length)
                                                throws BadLocationException {
                int endpos = offset + length;
                StringBuffer buf = new StringBuffer();
                int lineno = offset / 49;
                int addr = lineno * 16;
                int pos = offset - 49 * lineno;
                while (offset + 49 - pos <= endpos) {
                    buf.append(lineToHex(data, addr).substring(pos));
                    offset += 49 - pos;
                    addr += 16;
                    pos = 0;
                }
                if (offset < endpos)
                    buf.append(lineToHex(data, addr).substring(pos,
                                                pos + endpos - offset));
                return buf.toString();
            }
            public void getText(int offset, int length, Segment txt)
                                                throws BadLocationException {
                txt.array = getText(offset, length).toCharArray();
                txt.offset = 0;
                txt.count = length;
            }
            public Position getStartPosition() {
                return new MyPosition(0);
            }
            public Position getEndPosition() {
                return new MyPosition(getLength());
            }
            public Position createPosition(int offs)
                                                throws BadLocationException {
                return new MyPosition(offs);
            }
            public Element[] getRootElements() {
                return new Element[] { root };
            }
            public Element getDefaultRootElement() {
                return root;
            }
            public void render(Runnable r) {
                // We don't do the thread-safety stuff; whatever happens in
                // BinaryDataManager is assumed to happen on the Swing event
                // thread.
                r.run();
            }

            private class HexElement implements Element {
                private int lineno;
                public HexElement(int lineno) {
                    this.lineno = lineno;
                }
                public AttributeSet getAttributes() {
                    return null;
                }
                public Document getDocument() {
                    return HexDocument.this;
                }
                public Element getElement(int index) {
                    return null;
                }
                public int getElementCount() {
                    return 0;
                }
                public int getElementIndex(int offset) {
                    return 0;
                }
                public int getEndOffset() {
                    if (lineno == elements.size() - 1)
                        return getLength();
                    else
                        return 49 * (lineno + 1);
                }
                public String getName() {
                    return "hex[" + lineno + "]";
                }
                public Element getParentElement() {
                    return root;
                }
                public int getStartOffset() {
                    return 49 * lineno;
                }
                public boolean isLeaf() {
                    return true;
                }
            }

            private class HexRootElement implements Element {
                public HexRootElement() {
                    // Nothing to do
                }
                public AttributeSet getAttributes() {
                    return null;
                }
                public Document getDocument() {
                    return HexDocument.this;
                }
                public Element getElement(int index) {
                    return elements.get(index);
                }
                public int getElementCount() {
                    return elements.size();
                }
                public int getElementIndex(int offset) {
                    int index = offset / 49;
                    if (index < 0)
                        return 0;
                    else if (index >= elements.size())
                        return elements.size() - 1;
                    else
                        return index;
                }
                public int getEndOffset() {
                    return getLength();
                }
                public String getName() {
                    return "hex";
                }
                public Element getParentElement() {
                    return null;
                }
                public int getStartOffset() {
                    return 0;
                }
                public boolean isLeaf() {
                    return false;
                }
            }
        }
        
        
        private class AsciiDocument implements Document, CaretListener {
            private ArrayList<Element> elements;
            private ArrayList<DocumentListener> listeners;
            private Element root;
            private int dot = 0;
            private int mark = 0;
            public AsciiDocument() {
                root = new AsciiRootElement();
                elements = new ArrayList<Element>();
                listeners = new ArrayList<DocumentListener>();
                int lines = (data.length + 15) / 16;
                if (lines == 0)
                    lines = 1;
                for (int i = 0; i < lines; i++)
                    elements.add(new AsciiElement(i));
            }
            public void dataChanged(int s, int e, int l) {
                int start = addr2doc(s);
                int end = addr2doc(e);
                int length = addr2doc(s + l) - start;

                int oldlines = elements.size();
                int newlines = (data.length + 15) / 16;
                if (newlines == 0)
                    newlines = 1;
                
                int clen = end - start;
                if (clen > length)
                    clen = length;

                if (clen < length) {
                    InsertedEvent inserted = new InsertedEvent(this, root,
                                        start + clen, length - clen,
                                        oldlines);
                    for (int i = oldlines; i < newlines; i++) {
                        Element line = new AsciiElement(i);
                        inserted.add(line);
                        elements.add(line);
                    }
                    fireInserted(listeners, inserted);
                } else if (clen < end - start) {
                    RemovedEvent removed = new RemovedEvent(this, root,
                                        start + clen, end - start - clen,
                                        newlines);
                    for (int i = newlines; i < oldlines; i++)
                        removed.add(elements.remove(newlines));
                    fireRemoved(listeners, removed);
                }

                if (end - start != length) {
                    // Text moved; this means the change extends to the end
                    // of the text.
                    clen = getLength() - start;
                }
                if (clen != 0) {
                    // Aargh!!! PlainView (and God knows who else) does not
                    // even look at the 'length' property of a 'change' event;
                    // it simply finds the elements that matches the 'offset'
                    // property and repaints it, and screw everything else.
                    // So, we have to fire a ChangedEvent for each and every
                    // line individually.

                    int firstline = s / 32;
                    int lastline = elements.size();
                    int totallength = getLength();
                    for (int i = firstline; i < lastline; i++) {
                        int off = i * 17;
                        int len = 17;
                        if (off + len > totallength)
                            len = totallength - off;
                        ChangedEvent changed = new ChangedEvent(this, off, len);
                        fireChanged(listeners, changed);
                    }
                }}
            public void caretUpdate(CaretEvent e) {
                if (inCaretUpdate)
                    return;
                inCaretUpdate = true;
                int mark = e.getMark();
                int dot = e.getDot();
                int newmark = doc2addr(mark);
                int newdot = doc2addr(dot);
                if (newmark != this.mark || newdot != this.dot) {
                    addrDoc.caretUpdate(newdot, newmark);
                    hexDoc.caretUpdate(newdot, newmark);
                    this.dot = newdot;
                    this.mark = newmark;
                }
                inCaretUpdate = false;
            }
            public void caretUpdate(int dot, int mark) {
                this.dot = dot;
                this.mark = mark;
                Caret c = asciiTA.getCaret();
                int newdot, newmark;
                if (mark == dot) {
                    newmark = newdot = addr2doc(mark);
                } else if (mark < dot) {
                    newmark = addr2doc(mark);
                    newdot = addr2doc(dot + 1);
                } else {
                    newmark = addr2doc(mark + 1);
                    newdot = addr2doc(dot);
                }
                boolean markMoved = newmark != c.getMark();
                if (markMoved)
                    c.setDot(newmark);
                if (markMoved || newdot != c.getDot())
                    c.moveDot(newdot);
            }
            private int addr2doc(int addr) {
                int lineno = addr / 32;
                int remainder = (addr % 32) / 2;
                return (lineno * 17) + remainder;
            }
            private int doc2addr(int doc) {
                int lineno = doc / 17;
                return 2 * (doc - lineno);
            }
            public int getLength() {
                int linefeeds = (data.length + 15) / 16;
                return data.length + linefeeds;
            }
            public void addDocumentListener(DocumentListener listener) {
                listeners.add(listener);
            }
            public void removeDocumentListener(DocumentListener listener) {
                listeners.remove(listener);
            }
            public void addUndoableEditListener(UndoableEditListener lsnr) {
                // Not supported
            }
            public void removeUndoableEditListener(UndoableEditListener lsnr) {
                // Not supported
            }
            public Object getProperty(Object key) {
                // Not supported
                return null;
            }
            public void putProperty(Object key, Object value) {
                // Not supported
            }
            public void remove(int offs, int len) throws BadLocationException {
                int start = doc2addr(offs) / 2;
                int end = (doc2addr(offs + len) + 1) / 2;
                if (start == end)
                    // User hit backspace on a newline
                    start--;
                UndoableEdit edit = new Typing(new byte[0], start, end,
                                               start * 2, start * 2);
                edit.redo();
                undoManager.addEdit(edit);
            }
            public void insertString(int offset, String str, AttributeSet a)
                                                throws BadLocationException {
                if (str.length() != 1)
                    return;
                byte b = (byte) str.charAt(0);
                int addr = doc2addr(offset);
                int start = addr / 2;
                int newdot = addr + 2;
                UndoableEdit edit = new Typing(new byte[] { b }, start, start,
                                               newdot, newdot);
                edit.redo();
                undoManager.addEdit(edit);
            }
            public String getText(int offset, int length)
                                                throws BadLocationException {
                int endpos = offset + length;
                StringBuffer buf = new StringBuffer();
                int lineno = offset / 17;
                int addr = lineno * 16;
                int pos = offset - 17 * lineno;
                while (offset + 17 - pos <= endpos) {
                    buf.append(lineToAscii(data, addr).substring(pos));
                    offset += 17 - pos;
                    addr += 16;
                    pos = 0;
                }
                if (offset < endpos)
                    buf.append(lineToAscii(data, addr).substring(pos,
                                                pos + endpos - offset));
                return buf.toString();
            }
            public void getText(int offset, int length, Segment txt)
                                                throws BadLocationException {
                txt.array = getText(offset, length).toCharArray();
                txt.offset = 0;
                txt.count = length;
            }
            public Position getStartPosition() {
                return new MyPosition(0);
            }
            public Position getEndPosition() {
                return new MyPosition(getLength());
            }
            public Position createPosition(int offs)
                                                throws BadLocationException {
                return new MyPosition(offs);
            }
            public Element[] getRootElements() {
                return new Element[] { root };
            }
            public Element getDefaultRootElement() {
                return root;
            }
            public void render(Runnable r) {
                // We don't do the thread-safety stuff; whatever happens in
                // BinaryDataManager is assumed to happen on the Swing event
                // thread.
                r.run();
            }

            private class AsciiElement implements Element {
                private int lineno;
                public AsciiElement(int lineno) {
                    this.lineno = lineno;
                }
                public AttributeSet getAttributes() {
                    return null;
                }
                public Document getDocument() {
                    return AsciiDocument.this;
                }
                public Element getElement(int index) {
                    return null;
                }
                public int getElementCount() {
                    return 0;
                }
                public int getElementIndex(int offset) {
                    return 0;
                }
                public int getEndOffset() {
                    if (lineno == elements.size() - 1)
                        return getLength();
                    else
                        return 17 * (lineno + 1);
                }
                public String getName() {
                    return "hex[" + lineno + "]";
                }
                public Element getParentElement() {
                    return root;
                }
                public int getStartOffset() {
                    return 17 * lineno;
                }
                public boolean isLeaf() {
                    return true;
                }
            }

            private class AsciiRootElement implements Element {
                public AsciiRootElement() {
                    // Nothing to do
                }
                public AttributeSet getAttributes() {
                    return null;
                }
                public Document getDocument() {
                    return AsciiDocument.this;
                }
                public Element getElement(int index) {
                    return elements.get(index);
                }
                public int getElementCount() {
                    return elements.size();
                }
                public int getElementIndex(int offset) {
                    int index = offset / 17;
                    if (index < 0)
                        return 0;
                    else if (index >= elements.size())
                        return elements.size() - 1;
                    else
                        return index;
                }
                public int getEndOffset() {
                    return getLength();
                }
                public String getName() {
                    return "hex";
                }
                public Element getParentElement() {
                    return null;
                }
                public int getStartOffset() {
                    return 0;
                }
                public boolean isLeaf() {
                    return false;
                }
            }
        }

        public JTextArea makeTextArea(Document doc, String text,
                                                        int rows, int cols) {
            return new TextArea(doc, text, rows, cols);
        }

        private class TextArea extends NonTabJTextArea {
            public TextArea(Document doc, String text, int rows, int cols) {
                super(doc, text, rows, cols);
            }
            public void cut() {
                BinaryDataManager.this.cut();
            }
            public void copy() {
                BinaryDataManager.this.copy();
            }
            public void paste() {
                BinaryDataManager.this.paste();
            }
        }

        private class MyPosition implements Position {
            private int offset;
            public MyPosition(int offset) {
                this.offset = offset;
            }
            public int getOffset() {
                return offset;
            }
        }

        private class ChangedEvent implements DocumentEvent {
            private Document doc;
            private int start;
            private int length;
            public ChangedEvent(Document doc, int start, int length) {
                this.doc = doc;
                this.start = start;
                this.length = length;
            }
            public ElementChange getChange(Element element) {
                return null;
            }
            public Document getDocument() {
                return doc;
            }
            public int getLength() {
                return length;
            }
            public int getOffset() {
                return start;
            }
            public EventType getType() {
                return EventType.CHANGE;
            }
            public String toString() {
                return "ChangedEvent[start=" + start + " length=" + length +
                        "]";
            }
        }

        private class RemovedEvent implements DocumentEvent,
                                              DocumentEvent.ElementChange {
            private Document doc;
            private Element root;
            private int start;
            private int length;
            private int index;
            private ArrayList<Element> children = new ArrayList<Element>();
            public RemovedEvent(Document doc, Element root,
                                int start, int length, int index) {
                this.doc = doc;
                this.root = root;
                this.start = start;
                this.length = length;
                this.index = index;
            }
            public void add(Element child) {
                children.add(child);
            }
            public ElementChange getChange(Element element) {
                return element == root && children.size() != 0 ? this : null;
            }
            public Document getDocument() {
                return doc;
            }
            public int getLength() {
                return length;
            }
            public int getOffset() {
                return start;
            }
            public EventType getType() {
                return EventType.REMOVE;
            }

            // DocumentEvent.ElementChange methods

            public Element[] getChildrenAdded() {
                return null;
            }
            public Element[] getChildrenRemoved() {
                return children.toArray(new Element[0]);
            }
            public Element getElement() {
                return root;
            }
            public int getIndex() {
                return index;
            }
            public String toString() {
                return "RemovedEvent[start=" + start + " length=" + length
                            + " index=" + index + " nchildren="
                            + children.size() + "]";
            }
        }

        private class InsertedEvent implements DocumentEvent,
                                               DocumentEvent.ElementChange {
            private Document doc;
            private Element root;
            private int start;
            private int length;
            private int index;
            private ArrayList<Element> children = new ArrayList<Element>();
            public InsertedEvent(Document doc, Element root,
                                 int start, int length, int index) {
                this.doc = doc;
                this.root = root;
                this.start = start;
                this.length = length;
                this.index = index;
            }
            public void add(Element child) {
                children.add(child);
            }
            public ElementChange getChange(Element element) {
                return element == root && children.size() != 0 ? this : null;
            }
            public Document getDocument() {
                return doc;
            }
            public int getLength() {
                return length;
            }
            public int getOffset() {
                return start;
            }
            public EventType getType() {
                return EventType.INSERT;
            }

            // DocumentEvent.ElementChange methods

            public Element[] getChildrenAdded() {
                return children.toArray(new Element[0]);
            }
            public Element[] getChildrenRemoved() {
                return null;
            }
            public Element getElement() {
                return root;
            }
            public int getIndex() {
                return index;
            }
            public String toString() {
                return "InsertedEvent[start=" + start + " length=" + length
                            + " index=" + index + " nchildren="
                            + children.size() + "]";
            }
        }


        private static void fireChanged(ArrayList<DocumentListener> listeners, DocumentEvent event) {
            for (DocumentListener listener : listeners)
                listener.changedUpdate(event);
        }

        private static void fireInserted(ArrayList<DocumentListener> listeners, DocumentEvent event) {
            for (DocumentListener listener : listeners)
                listener.insertUpdate(event);
        }

        private static void fireRemoved(ArrayList<DocumentListener> listeners, DocumentEvent event) {
            for (DocumentListener listener : listeners)
                listener.removeUpdate(event);
        }
    }

    private class MyUndoManager extends UndoManager {
        public boolean addEdit(UndoableEdit edit) {
            boolean ret = super.addEdit(edit);
            updateMenuItems();
            return ret;
        }
        public void discardAllEdits() {
            super.discardAllEdits();
            updateMenuItems();
        }
        public void undo() {
            super.undo();
            updateMenuItems();
        }
        public void redo() {
            super.redo();
            updateMenuItems();
        }
        private void updateMenuItems() {
            undoMI.setText(getUndoPresentationName());
            redoMI.setText(getRedoPresentationName());
        }
    }
}
