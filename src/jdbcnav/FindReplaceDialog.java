package jdbcnav;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import jdbcnav.util.MyGridBagConstraints;
import jdbcnav.util.MyGridBagLayout;


public class FindReplaceDialog extends MyFrame {
    private TextEditorFrame target;
    private JTextField findField;
    private JTextField replaceField;
    private JRadioButton downRB;
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

	findField = new JTextField(20);
	gbc.gridx++;
	gbc.gridy = 0;
	gbc.weightx = 1;
	c.add(findField, gbc);

	replaceField = new JTextField(20);
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
