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
 * Created by JFormDesigner on Sun May 02 22:59:40 EDT 2021
 */

package org.wysko.midis2jam2.gui;

import org.wysko.midis2jam2.GuiLauncher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.Locale;

import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.FILES_ONLY;

/**
 * @author Jacob Wysko
 */
public class SoundFontList extends JPanel {
	
	private final transient List<File> soundFonts;
	
	private final GuiLauncher guiLauncher;
	
	public SoundFontList(List<File> soundFonts, GuiLauncher guiLauncher) {
		initComponents();
		this.soundFonts = soundFonts;
		updateSf2List();
		this.guiLauncher = guiLauncher;
	}
	
	
	private File lastDir = new JFileChooser().getFileSystemView().getDefaultDirectory();
	
	private void addButtonActionPerformed(ActionEvent e) {
		var f = new JFileChooser();
		f.setPreferredSize(new Dimension(800, 600));
		f.setDialogTitle("Load SoundFont file");
		f.setMultiSelectionEnabled(true);
		f.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith(".sf2");
			}
			
			@Override
			public String getDescription() {
				return "SoundFont files (*.sf2)";
			}
		});
		f.getActionMap().get("viewTypeDetails").actionPerformed(null); // Set default to details view
		f.setFileSelectionMode(FILES_ONLY);
		f.setCurrentDirectory(lastDir);
		if (f.showDialog(this, "Load") == APPROVE_OPTION) {
			for (File selectedFile : f.getSelectedFiles()) {
				if (!soundFonts.contains(selectedFile)) {
					soundFonts.add(selectedFile);
				}
			}
			lastDir = f.getSelectedFiles()[0].getParentFile();
			updateSf2List();
			guiLauncher.updateSf2List();
		}
	}
	
	private void updateSf2List() {
		final DefaultListModel<File> model = new DefaultListModel<>();
		model.addAll(soundFonts);
		soundFontJList.setModel(model);
	}
	
	private void removeButtonActionPerformed(ActionEvent e) {
		var selectedIndices = soundFontJList.getSelectedIndices();
		for (int i = selectedIndices.length - 1; i >= 0; i--) {
			if (soundFonts.get(selectedIndices[i]) != null) // Not the default synth
				soundFonts.remove(selectedIndices[i]);
		}
		updateSf2List();
		guiLauncher.updateSf2List();
	}
	
	private void upButtonActionPerformed(ActionEvent e) {
		var selectedIndices = soundFontJList.getSelectedIndices();
		for (var i = selectedIndices.length - 1; i >= 0; i--) {
			int selectedIndex = selectedIndices[i];
			swapList(selectedIndex - 1, selectedIndex);
		}
		var freshIndices = new int[selectedIndices.length];
		for (var i = 0; i < selectedIndices.length; i++) {
			freshIndices[i] = Math.max(0, selectedIndices[i] - 1);
		}
		updateSf2List();
		soundFontJList.requestFocusInWindow();
		SwingUtilities.invokeLater(() -> soundFontJList.setSelectedIndices(freshIndices));
		guiLauncher.updateSf2List();
	}
	
	private void swapList(int a, int b) {
		if (a < 0 || b >= soundFonts.size()) return;
		var file = soundFonts.get(a);
		soundFonts.set(a, soundFonts.get(b));
		soundFonts.set(b, file);
	}
	
	private void downButtonActionPerformed(ActionEvent e) {
		var selectedIndices = soundFontJList.getSelectedIndices();
		for (var i = selectedIndices.length - 1; i >= 0; i--) {
			int selectedIndex = selectedIndices[i];
			swapList(selectedIndex, selectedIndex + 1);
		}
		var freshIndices = new int[selectedIndices.length];
		for (var i = 0; i < selectedIndices.length; i++) {
			freshIndices[i] = Math.min(soundFonts.size() - 1, selectedIndices[i] + 1);
		}
		updateSf2List();
		soundFontJList.requestFocusInWindow();
		SwingUtilities.invokeLater(() -> soundFontJList.setSelectedIndices(freshIndices));
		guiLauncher.updateSf2List();
	}
	
	private void okButtonActionPerformed(ActionEvent e) {
		((JDialog) SwingUtilities.getRoot((Component) e.getSource())).dispose();
		guiLauncher.updateSf2List();
	}
	
	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		dialogPane = new JPanel();
		buttonBar = new JPanel();
		okButton = new JButton();
		scrollPane1 = new JScrollPane();
		soundFontJList = new JList<>();
		soundFontJList.setCellRenderer(new SoundFontListCellRenderer());
		panel1 = new JPanel();
		addButton = new JResizedIconButton();
		removeButton = new JResizedIconButton();
		upButton = new JResizedIconButton();
		downButton = new JResizedIconButton();
		
		//======== this ========
		setLayout(new BorderLayout());
		
		//======== dialogPane ========
		{
			dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
			dialogPane.setLayout(new BorderLayout());
			
			//======== buttonBar ========
			{
				buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
				buttonBar.setLayout(new GridBagLayout());
				((GridBagLayout) buttonBar.getLayout()).columnWidths = new int[]{0, 85, 80};
				((GridBagLayout) buttonBar.getLayout()).columnWeights = new double[]{1.0, 0.0, 0.0};
				
				//---- okButton ----
				okButton.setText("OK");
				okButton.addActionListener(e -> okButtonActionPerformed(e));
				buttonBar.add(okButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 0, 0), 0, 0));
			}
			dialogPane.add(buttonBar, BorderLayout.SOUTH);
			
			//======== scrollPane1 ========
			{
				scrollPane1.setViewportView(soundFontJList);
			}
			dialogPane.add(scrollPane1, BorderLayout.CENTER);
			
			//======== panel1 ========
			{
				panel1.setLayout(new GridBagLayout());
				((GridBagLayout) panel1.getLayout()).columnWidths = new int[]{0, 0};
				((GridBagLayout) panel1.getLayout()).rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
				((GridBagLayout) panel1.getLayout()).columnWeights = new double[]{1.0, 1.0E-4};
				((GridBagLayout) panel1.getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
				
				//---- addButton ----
				addButton.setIcon(new ImageIcon(getClass().getResource("/add.png")));
				addButton.addActionListener(e -> addButtonActionPerformed(e));
				panel1.add(addButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
						new Insets(0, 0, 0, 0), 0, 0));
				
				//---- removeButton ----
				removeButton.setIcon(new ImageIcon(getClass().getResource("/remove.png")));
				removeButton.addActionListener(e -> removeButtonActionPerformed(e));
				panel1.add(removeButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
						GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
						new Insets(0, 0, 0, 0), 0, 0));
				
				//---- upButton ----
				upButton.setIcon(new ImageIcon(getClass().getResource("/up.png")));
				upButton.addActionListener(e -> upButtonActionPerformed(e));
				panel1.add(upButton, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
						GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
						new Insets(0, 0, 0, 0), 0, 0));
				
				//---- downButton ----
				downButton.setIcon(new ImageIcon(getClass().getResource("/down.png")));
				downButton.addActionListener(e -> downButtonActionPerformed(e));
				panel1.add(downButton, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
						GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
						new Insets(0, 0, 0, 0), 0, 0));
			}
			dialogPane.add(panel1, BorderLayout.EAST);
		}
		add(dialogPane, BorderLayout.CENTER);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}
	
	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JPanel dialogPane;
	
	private JPanel buttonBar;
	
	private JButton okButton;
	
	private JScrollPane scrollPane1;
	
	private JList<File> soundFontJList;
	
	private JPanel panel1;
	
	private JResizedIconButton addButton;
	
	private JResizedIconButton removeButton;
	
	private JResizedIconButton upButton;
	
	private JResizedIconButton downButton;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
