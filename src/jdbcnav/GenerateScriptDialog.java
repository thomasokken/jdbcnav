///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2006  Thomas Okken
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
import javax.swing.*;
import javax.swing.event.*;

import jdbcnav.util.MyGridBagConstraints;
import jdbcnav.util.MyGridBagLayout;


public class GenerateScriptDialog extends MyFrame {
    public static final int DROP = 0;
    public static final int CREATE = 1;
    public static final int REBUILD = 2;
    public static final int UPDATE_FROM = 3;
    public static final int UPDATE_TO = 4;

    private Callback cb;
    private JRadioButton dropRB;
    private JRadioButton createRB;
    private JRadioButton rebuildRB;
    private JRadioButton updateRB;
    private JRadioButton fromRB;
    private JRadioButton toRB;
    private JComboBox otherCB;
    private JCheckBox fqtnCB;
    private JComboBox metadriverCB;

    public GenerateScriptDialog(BrowserFrame bf, Callback cb, String sgname) {
	super("Generate Script");
	this.cb = cb;
	Container c = getContentPane();

	c.setLayout(new GridLayout(1, 1));
	JPanel jp = new JPanel();
	c.add(jp);
	c = jp;

	c.setLayout(new MyGridBagLayout());
	MyGridBagConstraints gbc = new MyGridBagConstraints();
	gbc.gridx = 0;
	gbc.gridy = 0;
	gbc.anchor = MyGridBagConstraints.WEST;
	gbc.gridwidth = 3;
	c.add(new JLabel("Type of script: "), gbc);

	ButtonGroup bg = new ButtonGroup();

	gbc.gridy++;
	dropRB = new JRadioButton("Drop");
	bg.add(dropRB);
	c.add(dropRB, gbc);

	gbc.gridy++;
	createRB = new JRadioButton("Drop, Create");
	bg.add(createRB);
	c.add(createRB, gbc);

	gbc.gridy++;
	gbc.gridwidth = 5;
	rebuildRB = new JRadioButton("Drop, Create, Populate");
	bg.add(rebuildRB);
	c.add(rebuildRB, gbc);

	gbc.gridy++;
	gbc.gridwidth = 1;
	updateRB = new JRadioButton("Update");
	bg.add(updateRB);
	c.add(updateRB, gbc);
	updateRB.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
				    boolean b = ((JRadioButton)
					    e.getSource()).isSelected();
				    fromRB.setEnabled(b);
				    toRB.setEnabled(b);
				    otherCB.setEnabled(b);
				}
			    });

	bg = new ButtonGroup();

	gbc.gridx++;
	fromRB = new JRadioButton("From: ");
	bg.add(fromRB);
	c.add(fromRB, gbc);
	fromRB.setSelected(true);

	gbc.gridy++;
	toRB = new JRadioButton("To: ");
	bg.add(toRB);
	c.add(toRB, gbc);

	gbc.gridx++;
	gbc.gridy--;
	gbc.gridheight = 2;
	gbc.gridwidth = 3;
	otherCB = new JComboBox(bf.getOtherInstances());
	c.add(otherCB, gbc);

	if (otherCB.getSelectedItem() == null) {
	    rebuildRB.setSelected(true);
	    fromRB.setEnabled(false);
	    toRB.setEnabled(false);
	    otherCB.setEnabled(false);
	} else
	    updateRB.setSelected(true);

	gbc.gridx = 3;
	gbc.gridy = 0;
	gbc.gridheight = 1;
	gbc.gridwidth = 2;
	fqtnCB = new JCheckBox("Use fully qualified table names");
	c.add(fqtnCB, gbc);

	gbc.gridy++;
	gbc.gridwidth = 1;
	c.add(new JLabel("Generate SQL for: "), gbc);

	gbc.gridx++;
	metadriverCB = new JComboBox(InternalDriverMap.getScriptGeneratorNames());
	metadriverCB.setSelectedItem(sgname);
	c.add(metadriverCB, gbc);

	gbc.gridx = 0;
	gbc.gridy = 6;
	gbc.gridwidth = 5;
	gbc.weightx = 1;
	gbc.fill = MyGridBagConstraints.HORIZONTAL;
	JPanel p = new JPanel();
	c.add(p, gbc);

	p.setLayout(new MyGridBagLayout());
	gbc = new MyGridBagConstraints();
	gbc.gridx = 0;
	gbc.gridy = 0;
	JButton okB = new JButton("OK");
	okB.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    ok();
				}
			    });
	p.add(okB, gbc);
	JButton cancelB = new JButton("Cancel");
	cancelB.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    dispose();
				}
			    });
	gbc.gridx++;
	p.add(cancelB, gbc);
	pack();
    }
    
    private void ok() {
	int what;
	BrowserFrame other = null;
	if (dropRB.isSelected())
	    what = DROP;
	else if (createRB.isSelected())
	    what = CREATE;
	else if (rebuildRB.isSelected())
	    what = REBUILD;
	else {
	    other = (BrowserFrame) otherCB.getSelectedItem();
	    if (other == null) {
		Toolkit.getDefaultToolkit().beep();
		return;
	    }
	    if (fromRB.isSelected())
		what = UPDATE_FROM;
	    else
		what = UPDATE_TO;
	}
	String sgname = (String) metadriverCB.getSelectedItem();
	dispose();
	cb.invoke(what, fqtnCB.isSelected(), other, sgname);
    }

    public interface Callback {
	public void invoke(int what, boolean fqtn, BrowserFrame other,
			   String sgname);
    }
}
