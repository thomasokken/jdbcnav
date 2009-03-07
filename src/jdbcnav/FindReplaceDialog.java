///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2009	Thomas Okken
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

import jdbcnav.util.MyGridBagConstraints;
import jdbcnav.util.MyGridBagLayout;
import jdbcnav.util.MyTextField;


public class FindReplaceDialog extends MyFrame {
	private TextEditorFrame target;
	private JTextField findField;
	private JTextField replaceField;
	private JCheckBox wholeWordCB;
	private JCheckBox matchCaseCB;

	public FindReplaceDialog(TextEditorFrame target_p) {
		super(target_p.getTitle()+" - Find/Replace", false, true, false, false);
		target = target_p;
		Container c = getContentPane();
		c.setLayout(new MyGridBagLayout());

		JLabel label = new JLabel("Find what: ");
		MyGridBagConstraints gbc = new MyGridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = MyGridBagConstraints.HORIZONTAL;
		c.add(label, gbc);

		label = new JLabel("Replace with: ");
		gbc.gridy++;
		c.add(label, gbc);

		findField = new MyTextField(20);
		gbc.gridx++;
		gbc.gridy = 0;
		gbc.weightx = 1;
		c.add(findField, gbc);

		replaceField = new MyTextField(20);
		gbc.gridy++;
		c.add(replaceField, gbc);

		JPanel p1 = new JPanel();
		p1.setLayout(new MyGridBagLayout());
		gbc.gridx = 0;
		gbc.gridy++;
		gbc.gridwidth = 2;
		c.add(p1, gbc);

		JPanel p2 = new JPanel();
		p2.setLayout(new MyGridBagLayout());
		gbc.gridy++;
		c.add(p2, gbc);

		wholeWordCB = new JCheckBox("Whole Word");
		wholeWordCB.setSelected(true);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		p1.add(wholeWordCB, gbc);

		matchCaseCB = new JCheckBox("Match Case");
		gbc.gridx++;
		gbc.weightx = 1;
		p1.add(matchCaseCB, gbc);

		JButton button = new JButton("Find Next");
		button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String find = findField.getText();
						boolean wholeWord = wholeWordCB.isSelected();
						boolean matchCase = matchCaseCB.isSelected();
						target.find(find, true, wholeWord, matchCase);
					}
				});
		gbc.gridx = 0;
		gbc.gridy = 0;
		p2.add(button, gbc);

		button = new JButton("Replace, then Find Next");
		button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String find = findField.getText();
						String replace = replaceField.getText();
						boolean wholeWord = wholeWordCB.isSelected();
						boolean matchCase = matchCaseCB.isSelected();
						target.replaceThenFind(find, replace, true, wholeWord,
											   matchCase);
					}
				});
		gbc.gridx++;
		p2.add(button, gbc);

		button = new JButton("Replace");
		button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String replace = replaceField.getText();
						target.replace(replace);
					}
				});
		gbc.gridx++;
		p2.add(button, gbc);

		button = new JButton("Undo");
		button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						target.undo();
					}
				});
		gbc.gridx++;
		p2.add(button, gbc);

		button = new JButton("Find Previous");
		button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String find = findField.getText();
						boolean wholeWord = wholeWordCB.isSelected();
						boolean matchCase = matchCaseCB.isSelected();
						target.find(find, false, wholeWord, matchCase);
					}
				});
		gbc.gridx = 0;
		gbc.gridy++;
		p2.add(button, gbc);

		button = new JButton("Replace, then Find Previous");
		button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String find = findField.getText();
						String replace = replaceField.getText();
						boolean wholeWord = wholeWordCB.isSelected();
						boolean matchCase = matchCaseCB.isSelected();
						target.replaceThenFind(find, replace, false, wholeWord,
											   matchCase);
					}
				});
		gbc.gridx++;
		p2.add(button, gbc);

		button = new JButton("Replace All");
		button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String find = findField.getText();
						String replace = replaceField.getText();
						boolean wholeWord = wholeWordCB.isSelected();
						boolean matchCase = matchCaseCB.isSelected();
						target.replaceAll(find, replace, wholeWord, matchCase);
					}
				});
		gbc.gridx++;
		p2.add(button, gbc);

		button = new JButton("Redo");
		button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						target.redo();
					}
				});
		gbc.gridx++;
		p2.add(button, gbc);

		pack();
	}

	public void updateTitle() {
		setTitle(getParentTitle() + " - Find/Replace");
	}
}
