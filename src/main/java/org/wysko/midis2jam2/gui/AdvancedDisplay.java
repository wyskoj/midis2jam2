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
 * Created by JFormDesigner on Fri Jun 18 18:23:25 EDT 2021
 */

package org.wysko.midis2jam2.gui;

import org.wysko.midis2jam2.Liaison;
import org.wysko.midis2jam2.Midis2jam2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Jacob Wysko
 */
public class AdvancedDisplay extends Displays {
	
	private final Canvas canvas;
	private Midis2jam2 context;
	
	public AdvancedDisplay(Liaison liaison, Canvas canvas, Midis2jam2 context) {
		initComponents();
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				liaison.stop();
			}
		});
		
		this.canvas = canvas;
		this.add(this.canvas, BorderLayout.CENTER);
		this.context = context;
		
	}
	
	public void setProgressText(String text, double progress) {
		this.progressLabel.setText(text);
		var n = (int) (progress * 1000);
		this.playbackProgress.setValue(n);
	}
	
	private void mouseReleased(MouseEvent e) {
		// TODO add your code here
	}
	
	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		panel2 = new JPanel();
		progressLabel = new JLabel();
		playbackProgress = new JProgressBar();
		
		//======== this ========
		var contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		
		//======== panel2 ========
		{
			panel2.setLayout(new GridBagLayout());
			((GridBagLayout) panel2.getLayout()).columnWidths = new int[]{0, 0};
			((GridBagLayout) panel2.getLayout()).rowHeights = new int[]{0, 0, 0};
			((GridBagLayout) panel2.getLayout()).columnWeights = new double[]{1.0, 1.0E-4};
			((GridBagLayout) panel2.getLayout()).rowWeights = new double[]{0.0, 0.0, 1.0E-4};
			
			//---- progressLabel ----
			progressLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
			progressLabel.setText("TEXT");
			panel2.add(progressLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 5, 0), 0, 0));
			
			//---- playbackProgress ----
			playbackProgress.setMaximum(1000);
			panel2.add(playbackProgress, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 0), 0, 0));
		}
		contentPane.add(panel2, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}
	
	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JPanel panel2;
	private JLabel progressLabel;
	private JProgressBar playbackProgress;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
