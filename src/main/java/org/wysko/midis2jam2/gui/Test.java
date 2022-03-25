/*
 * Copyright (C) 2022 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.gui;/*
 * Created by JFormDesigner on Sat Feb 26 15:52:49 EST 2022
 */

import java.awt.event.*;

import com.formdev.flatlaf.FlatDarculaLaf;
import kotlin.Pair;

import java.awt.*;
import javax.swing.*;

/**
 * @author unknown
 */
public class Test extends JPanel {
	public static void main(String[] args) {
		FlatDarculaLaf.setup();
		Test test = new Test();
		JFrame frame = new JFrame();
		frame.getContentPane().add(test);
		frame.setVisible(true);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
	
	public Test() {
		initComponents();
	}
	
	private void changeType(ActionEvent e) {
		((CardLayout) panel2.getLayout()).show(panel2, ((Pair<String, String>) comboBox1.getSelectedItem()).component2());
	}
	
	private void cancelPressed(ActionEvent e) {
		// TODO add your code here
	}
	
	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		label1 = new JLabel();
		editorPane1 = new JEditorPane();
		separator1 = new JSeparator();
		panel1 = new JPanel();
		label2 = new JLabel();
		comboBox1 = new JComboBox<>();
		comboBox1.setModel(new DefaultComboBoxModel<>(new Pair[] {
			new Pair("Default", "NO_SETTINGS"),
			new Pair("Fixed", "ONE_FILE"),
			new Pair("Repeated cubemap", "ONE_FILE"),
			new Pair("Unique cubemap", "SIX_FILES"),
			new Pair("Color", "COLOR")
		}));
		panel2 = new JPanel();
		panel4 = new JPanel();
		label4 = new JLabel();
		panel3 = new JPanel();
		label3 = new JLabel();
		comboBox2 = new JComboBox();
		panel6 = new JPanel();
		colorChooser1 = new JColorChooser();
		six_items = new JPanel();
		label6 = new JLabel();
		comboBox5 = new JComboBox();
		label5 = new JLabel();
		label7 = new JLabel();
		label8 = new JLabel();
		label9 = new JLabel();
		comboBox3 = new JComboBox();
		comboBox4 = new JComboBox();
		comboBox7 = new JComboBox();
		comboBox8 = new JComboBox();
		label10 = new JLabel();
		comboBox6 = new JComboBox();
		separator2 = new JSeparator();
		panel5 = new JPanel();
		button1 = new JButton();
		button2 = new JButton();

		//======== this ========
		setLayout(new GridBagLayout());
		((GridBagLayout)getLayout()).columnWidths = new int[] {154, 0};
		((GridBagLayout)getLayout()).rowHeights = new int[] {0, 0, 0, 0, 6, 0, 0, 0};
		((GridBagLayout)getLayout()).columnWeights = new double[] {1.0, 1.0E-4};
		((GridBagLayout)getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

		//---- label1 ----
		label1.setText("Configure Background");
		label1.setFont(label1.getFont().deriveFont(label1.getFont().getStyle() | Font.BOLD, label1.getFont().getSize() + 2f));
		add(label1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(0, 0, 5, 0), 0, 0));

		//---- editorPane1 ----
		editorPane1.setContentType("text/html");
		editorPane1.setText("<html>\nYou can configure the appearance of the background using a custom picture or color. There are several different background types:\n<ul>\n    <li>Default &mdash; The default background (grey checkerboard) is used.</li>\n    <li>Fixed &mdash; The background picture is static and does not move (similar to MIDIJam).</li>\n    <li>Repeated cubemap &mdash; A single background picture is displayed on each of the six surrounding walls.</li>\n    <li>Unique cubemap &mdash; Six pictures are respectively displayed on each of the six surrounding walls.</li>\n    <li>Color &mdash; The background is a solid color.</li>\n</ul>\nTo use a custom picture, you must place the image file into the <a href><tt>.midis2jam2/backgrounds</tt></a> folder in your user folder.\n</html>");
		editorPane1.setEditable(false);
		editorPane1.setMargin(new Insets(0, 0, 0, 0));
		add(editorPane1, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(0, 0, 5, 0), 0, 0));
		add(separator1, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(0, 0, 5, 0), 0, 0));

		//======== panel1 ========
		{
			panel1.setLayout(new FlowLayout());

			//---- label2 ----
			label2.setText("Select a background type:");
			panel1.add(label2);

			//---- comboBox1 ----
			comboBox1.addActionListener(e -> changeType(e));
			panel1.add(comboBox1);
		}
		add(panel1, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(0, 0, 5, 0), 0, 0));

		//======== panel2 ========
		{
			panel2.setLayout(new CardLayout());

			//======== panel4 ========
			{
				panel4.setLayout(new BorderLayout());

				//---- label4 ----
				label4.setText("There are no configurable settings.");
				label4.setHorizontalAlignment(SwingConstants.CENTER);
				label4.setEnabled(false);
				panel4.add(label4, BorderLayout.CENTER);
			}
			panel2.add(panel4, "NO_SETTINGS");

			//======== panel3 ========
			{
				panel3.setLayout(new GridBagLayout());
				((GridBagLayout)panel3.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0};
				((GridBagLayout)panel3.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
				((GridBagLayout)panel3.getLayout()).columnWeights = new double[] {1.0, 0.0, 1.0, 1.0, 1.0E-4};
				((GridBagLayout)panel3.getLayout()).rowWeights = new double[] {1.0, 0.0, 1.0, 1.0E-4};

				//---- label3 ----
				label3.setText("Background image:");
				panel3.add(label3, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 5, 5), 0, 0));
				panel3.add(comboBox2, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			}
			panel2.add(panel3, "ONE_FILE");

			//======== panel6 ========
			{
				panel6.setLayout(new BorderLayout());
				panel6.add(colorChooser1, BorderLayout.CENTER);
			}
			panel2.add(panel6, "COLOR");

			//======== six_items ========
			{
				six_items.setLayout(new GridBagLayout());
				((GridBagLayout)six_items.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0, 0, 0};
				((GridBagLayout)six_items.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
				((GridBagLayout)six_items.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0E-4};
				((GridBagLayout)six_items.getLayout()).rowWeights = new double[] {1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0E-4};

				//---- label6 ----
				label6.setText("Up:");
				label6.setHorizontalAlignment(SwingConstants.CENTER);
				label6.setVerticalAlignment(SwingConstants.BOTTOM);
				six_items.add(label6, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
				six_items.add(comboBox5, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));

				//---- label5 ----
				label5.setText("West:");
				label5.setHorizontalAlignment(SwingConstants.CENTER);
				label5.setVerticalAlignment(SwingConstants.BOTTOM);
				six_items.add(label5, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));

				//---- label7 ----
				label7.setText("North:");
				label7.setHorizontalAlignment(SwingConstants.CENTER);
				label7.setVerticalAlignment(SwingConstants.BOTTOM);
				six_items.add(label7, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));

				//---- label8 ----
				label8.setText("East:");
				label8.setHorizontalAlignment(SwingConstants.CENTER);
				label8.setVerticalAlignment(SwingConstants.BOTTOM);
				six_items.add(label8, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));

				//---- label9 ----
				label9.setText("South:");
				label9.setHorizontalAlignment(SwingConstants.CENTER);
				label9.setVerticalAlignment(SwingConstants.BOTTOM);
				six_items.add(label9, new GridBagConstraints(4, 3, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
				six_items.add(comboBox3, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
				six_items.add(comboBox4, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
				six_items.add(comboBox7, new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
				six_items.add(comboBox8, new GridBagConstraints(4, 4, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));

				//---- label10 ----
				label10.setText("Down:");
				label10.setHorizontalAlignment(SwingConstants.CENTER);
				label10.setVerticalAlignment(SwingConstants.BOTTOM);
				six_items.add(label10, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
				six_items.add(comboBox6, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			}
			panel2.add(six_items, "SIX_FILES");
		}
		add(panel2, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(0, 0, 5, 0), 0, 0));
		add(separator2, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(0, 0, 5, 0), 0, 0));

		//======== panel5 ========
		{
			panel5.setLayout(new FlowLayout());

			//---- button1 ----
			button1.setText("OK");
			panel5.add(button1);

			//---- button2 ----
			button2.setText("Cancel");
			button2.addActionListener(e -> cancelPressed(e));
			panel5.add(button2);
		}
		add(panel5, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(0, 0, 0, 0), 0, 0));
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}
	
	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JLabel label1;
	private JEditorPane editorPane1;
	private JSeparator separator1;
	private JPanel panel1;
	private JLabel label2;
	private JComboBox<Pair<String, String>> comboBox1;
	private JPanel panel2;
	private JPanel panel4;
	private JLabel label4;
	private JPanel panel3;
	private JLabel label3;
	private JComboBox comboBox2;
	private JPanel panel6;
	private JColorChooser colorChooser1;
	private JPanel six_items;
	private JLabel label6;
	private JComboBox comboBox5;
	private JLabel label5;
	private JLabel label7;
	private JLabel label8;
	private JLabel label9;
	private JComboBox comboBox3;
	private JComboBox comboBox4;
	private JComboBox comboBox7;
	private JComboBox comboBox8;
	private JLabel label10;
	private JComboBox comboBox6;
	private JSeparator separator2;
	private JPanel panel5;
	private JButton button1;
	private JButton button2;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
