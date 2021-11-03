/*
 * Copyright (C) 2021 Jacob Wysko
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

/*
 * Created by JFormDesigner on Sat Jun 05 20:30:56 EDT 2021
 */

package org.wysko.midis2jam2.gui;

import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.starter.Liaison;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** @author Jacob Wysko */
public class Midis2jam2Display extends Displays {
	
	public Midis2jam2Display(Liaison liaison, Canvas canvas, Midis2jam2 context) {
		initComponents();
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				liaison.stop();
			}
		});
		
		panel1.add(canvas);
	}
	
	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		panel1 = new JPanel();
		
		//======== this ========
		setIconImage(new ImageIcon(getClass().getResource("/ico/icon16.png")).getImage());
		setTitle("midis2jam2");
		Container contentPane = getContentPane();
		contentPane.setLayout(new GridBagLayout());
		((GridBagLayout) contentPane.getLayout()).columnWidths = new int[]{0, 0};
		((GridBagLayout) contentPane.getLayout()).rowHeights = new int[]{0, 0};
		((GridBagLayout) contentPane.getLayout()).columnWeights = new double[]{1.0, 1.0E-4};
		((GridBagLayout) contentPane.getLayout()).rowWeights = new double[]{1.0, 1.0E-4};
		
		//======== panel1 ========
		{
			panel1.setLayout(new BorderLayout());
		}
		contentPane.add(panel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(0, 0, 0, 0), 0, 0));
		pack();
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}
	
	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JPanel panel1;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
