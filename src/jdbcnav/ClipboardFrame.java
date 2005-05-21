package jdbcnav;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import jdbcnav.util.ArrayTableModel;


public class ClipboardFrame extends MyFrame implements Clipboard.Listener {
    private Clipboard clipboard;
    private JCheckBoxMenuItem wrapLinesMI;
    private JMenuItem clearMI;
    private JScrollPane scrollPane;
    private JTextArea jta;

    public ClipboardFrame(Clipboard clipboard) {
	super("Clipboard", true, true, true, true);
	this.clipboard = clipboard;
	getContentPane().setLayout(new GridLayout(1, 1));
	scrollPane = new JScrollPane();
	getContentPane().add(scrollPane);

	JMenuBar mb = new JMenuBar();
	JMenu m = new JMenu("Clipboard");
	wrapLinesMI = new JCheckBoxMenuItem("Wrap Lines");
	wrapLinesMI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    if (jta != null)
				jta.setLineWrap(wrapLinesMI.getState());
			}
		    });
	m.add(wrapLinesMI);
	clearMI = new JMenuItem("Clear");
	clearMI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    Main.getClipboard().put(Clipboard.EMPTY);
			}
		    });
	m.add(clearMI);
	JMenuItem mi = new JMenuItem("Close");
	mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    dispose();
			}
		    });
	mi.setAccelerator(KeyStroke.getKeyStroke('W', Event.CTRL_MASK));
	m.add(mi);
	mb.add(m);
	setJMenuBar(mb);
	clipboardUpdated(clipboard.get());
	setSize(500, 300);
    }

    public void clipboardUpdated(Object data) {
	jta = null;
	if (data == Clipboard.EMPTY) {
	    wrapLinesMI.setEnabled(false);
	    clearMI.setEnabled(false);
	    scrollPane.setViewportView(new JLabel(""));
	} else if (data instanceof Object[][]) {
	    wrapLinesMI.setEnabled(false);
	    clearMI.setEnabled(true);
	    MyTable table = new MyTable(new ArrayTableModel((Object[][]) data));
	    table.setNiceSize();
	    scrollPane.setViewportView(table);
	} else if (data instanceof byte[]) {
	    jta = new JTextArea();
	    jta.setEditable(false);
	    jta.setLineWrap(wrapLinesMI.getState());
	    StringBuffer buf = new StringBuffer();
	    byte[] b = (byte[]) data;
	    String nybble = "0123456789abcdef";
	    for (int i = 0; i < b.length; i++) {
		byte B = b[i];
		buf.append(nybble.charAt((B >> 4) & 15));
		buf.append(nybble.charAt(B & 15));
		if (i != b.length - 1) {
		    if (i % 16 == 15)
			buf.append('\n');
		    else if (i % 8 == 7)
			buf.append("  ");
		    else
			buf.append(' ');
		}
	    }
	    jta.setText(buf.toString());
	    clearMI.setEnabled(true);
	    scrollPane.setViewportView(jta);
	} else {
	    jta = new JTextArea();
	    jta.setEditable(false);
	    jta.setLineWrap(wrapLinesMI.getState());
	    jta.setText(String.valueOf(data));
	    wrapLinesMI.setEnabled(true);
	    clearMI.setEnabled(true);
	    scrollPane.setViewportView(jta);
	}
    }
}