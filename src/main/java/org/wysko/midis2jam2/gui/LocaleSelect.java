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
 * Created by JFormDesigner on Tue Jul 20 23:25:34 EDT 2021
 */

package org.wysko.midis2jam2.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;

import static javax.swing.JOptionPane.INFORMATION_MESSAGE;

/** @author Jacob Wysko */
public class LocaleSelect extends JDialog {
	
	public LocaleSelect(Window owner) {
		super(owner);
		initComponents();
	}
	
	private void cancelButtonActionPerformed(ActionEvent e) {
		dispose();
	}
	
	private void okButtonActionPerformed(ActionEvent e) {
		final ResourceBundle bundle = ResourceBundle.getBundle("i18n.locale");
		
		final String selectedItem = (String) localeSpinner.getSelectedItem();
		String locale = GuiLauncher.getSupportedLocales().get(selectedItem);
		GuiLauncher guiLauncher = getGuiLauncher();
		guiLauncher.getSettings().setLocale(locale);
		guiLauncher.saveSettings();
		
		JOptionPane.showMessageDialog(this,
				bundle.getString("restart_message"),
				bundle.getString("restart_required"),
				INFORMATION_MESSAGE);
		dispose();
	}
	
	private GuiLauncher getGuiLauncher() {
		return (GuiLauncher) getOwner();
	}
	
	@SuppressWarnings("all")
	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		ResourceBundle bundle = ResourceBundle.getBundle("i18n.locale");
		dialogPane = new JPanel();
		contentPanel = new JPanel();
		selectLocaleLabel = new JLabel();
		panel1 = new JPanel();
		localeSpinner = new JComboBox<>();
		buttonBar = new JPanel();
		okButton = new JButton();
		cancelButton = new JButton();
		
		//======== this ========
		setMinimumSize(new Dimension(400, 200));
		setTitle(bundle.getString("LocaleSelect.locale"));
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		
		//======== dialogPane ========
		{
			dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
			dialogPane.setLayout(new BorderLayout());
			
			//======== contentPanel ========
			{
				contentPanel.setLayout(new BorderLayout());
				
				//---- selectLocaleLabel ----
				selectLocaleLabel.setText(bundle.getString("LocaleSelect.selectLocaleLabel.text"));
				contentPanel.add(selectLocaleLabel, BorderLayout.NORTH);
				
				//======== panel1 ========
				{
					panel1.setLayout(new GridBagLayout());
					((GridBagLayout) panel1.getLayout()).columnWidths = new int[]{0, 0};
					((GridBagLayout) panel1.getLayout()).rowHeights = new int[]{0, 0};
					((GridBagLayout) panel1.getLayout()).columnWeights = new double[]{1.0, 1.0E-4};
					((GridBagLayout) panel1.getLayout()).rowWeights = new double[]{1.0, 1.0E-4};
					panel1.add(localeSpinner, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
							GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
							new Insets(0, 0, 0, 0), 0, 0));
				}
				contentPanel.add(panel1, BorderLayout.CENTER);
			}
			dialogPane.add(contentPanel, BorderLayout.CENTER);
			
			//======== buttonBar ========
			{
				buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
				buttonBar.setLayout(new GridBagLayout());
				((GridBagLayout) buttonBar.getLayout()).columnWidths = new int[]{0, 85, 80};
				((GridBagLayout) buttonBar.getLayout()).columnWeights = new double[]{1.0, 0.0, 0.0};
				
				//---- okButton ----
				okButton.setText(bundle.getString("LocaleSelect.okButton.text"));
				okButton.addActionListener(e -> okButtonActionPerformed(e));
				buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 0, 5), 0, 0));
				
				//---- cancelButton ----
				cancelButton.setText(bundle.getString("LocaleSelect.cancelButton.text"));
				cancelButton.addActionListener(e -> cancelButtonActionPerformed(e));
				buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 0, 0), 0, 0));
			}
			dialogPane.add(buttonBar, BorderLayout.SOUTH);
		}
		contentPane.add(dialogPane, BorderLayout.CENTER);
		pack();
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
		
		final DefaultComboBoxModel spinnerListModel =
				new DefaultComboBoxModel<String>(new Vector<>(GuiLauncher.getSupportedLocales().keySet()));
		localeSpinner.setModel(spinnerListModel);
		
		final String locale = getGuiLauncher().getSettings().getLocale();
		for (Map.Entry<String, String> entry : getGuiLauncher().getSupportedLocales().entrySet()) {
			if (entry.getValue().equals(locale)) {
				String language = entry.getKey();
				localeSpinner.setSelectedItem(language);
				break;
			}
		}
	}
	
	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JPanel dialogPane;
	
	private JPanel contentPanel;
	
	private JLabel selectLocaleLabel;
	
	private JPanel panel1;
	
	private JComboBox<String> localeSpinner;
	
	private JPanel buttonBar;
	
	private JButton okButton;
	
	private JButton cancelButton;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
