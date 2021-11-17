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
 * Created by JFormDesigner on Sat May 01 11:32:25 EDT 2021
 */

package org.wysko.midis2jam2.gui;

import org.wysko.midis2jam2.util.Utils;

import javax.swing.*;
import java.awt.*;

/**
 * Displays an exception and its stack trace to help debugging.
 *
 * @author Jacob Wysko
 */
@SuppressWarnings({"java:S1213", "FieldCanBeLocal", "java:S1450"})
public class ExceptionDisplay extends JPanel {
	
	public ExceptionDisplay(String message, Exception e) {
		initComponents();
		this.errorMessage.setText(message);
		
		this.errorStacktrace.setText(Utils.exceptionToLines(e));
	}
	
	@SuppressWarnings("all")
	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		errorMessage = new JLabel();
		scrollPane1 = new JScrollPane();
		errorStacktrace = new JTextArea();
		
		//======== this ========
		setMaximumSize(new Dimension(100, 100));
		setLayout(new BorderLayout());
		
		//---- errorMessage ----
		errorMessage.setText("text");
		add(errorMessage, BorderLayout.NORTH);
		
		//======== scrollPane1 ========
		{
			scrollPane1.setMaximumSize(new Dimension(100, 100));
			scrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			scrollPane1.revalidate();
			
			//---- errorStacktrace ----
			errorStacktrace.setMaximumSize(new Dimension(100, 100));
			errorStacktrace.setEditable(false);
			scrollPane1.setViewportView(errorStacktrace);
		}
		add(scrollPane1, BorderLayout.CENTER);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}
	
	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JLabel errorMessage;
	
	private JScrollPane scrollPane1;
	
	private JTextArea errorStacktrace;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
