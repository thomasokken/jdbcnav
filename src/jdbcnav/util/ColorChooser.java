package jdbcnav.util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import jdbcnav.MyFrame;

public class ColorChooser extends MyFrame {
    private JColorChooser jcc;
    private Listener listener;

    public interface Listener {
	void apply(Color c);
	void close();
    }

    public ColorChooser(String title, Color color) {
	super(title, false, true, false, false);
	Container c = getContentPane();
	c.setLayout(new MyGridBagLayout());
	MyGridBagConstraints gbc = new MyGridBagConstraints();

	gbc.gridx = 0;
	gbc.gridy = 0;
	gbc.fill = MyGridBagConstraints.BOTH;
	jcc = new JColorChooser(color);
	c.add(jcc, gbc);

	gbc.gridx = 0;
	gbc.gridy = 1;
	gbc.fill = MyGridBagConstraints.NONE;
	gbc.anchor = MyGridBagConstraints.CENTER;
	JPanel p = new JPanel();
	p.setLayout(new GridLayout(1, 3));
	JButton b = new JButton("OK");
	b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    listener.apply(jcc.getColor());
		    dispose();
		}
	    });
	p.add(b);
	b = new JButton("Apply");
	b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    listener.apply(jcc.getColor());
		}
	    });
	p.add(b);
	b = new JButton("Close");
	b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    dispose();
		}
	    });
	p.add(b);
	c.add(p, gbc);

	pack();
    }

    public void setListener(Listener listener) {
	this.listener = listener;
    }

    public void dispose() {
	listener.close();
	super.dispose();
    }
}
