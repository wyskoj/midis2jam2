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
 * Created by JFormDesigner on Wed Jun 30 12:30:26 EDT 2021
 */

package org.wysko.midis2jam2.gui;

import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.util.Utils;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.wysko.midis2jam2.util.Utils.exceptionToLines;

/**
 * Displays information about midis2jam2.
 *
 * @author Jacob Wysko
 */
@SuppressWarnings({"java:S1213", "FieldCanBeLocal", "java:S1450"})
public class About extends JDialog {
	
	public About() {
		initComponents();
	}
	
	public About(Frame owner, boolean modal) {
		super(owner, modal);
		initComponents();
	}
	
	/**
	 * Opens a hyperlink in the default browser.
	 *
	 * @param hle the hyperlink event
	 */
	private static void hyperlinkUpdate(HyperlinkEvent hle) {
		if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
			try {
				Desktop.getDesktop().browse(hle.getURL().toURI());
			} catch (IOException | URISyntaxException ex) {
				Midis2jam2.getLOGGER().warning(() -> "Could not open page.\n" + exceptionToLines(ex));
			}
		}
	}
	
	@SuppressWarnings("all")
	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		ResourceBundle bundle = ResourceBundle.getBundle("i18n.about");
		panel1 = new JPanel();
		label1 = new JLabel();
		label2 = new JLabel();
		label3 = new JLabel();
		label4 = new JLabel();
		websiteLink = new JEditorPane();
		copyrightInfo = new JEditorPane();
		otherContribs = new JLabel();
		licenseInfo = new JEditorPane();
		label5 = new JLabel();
		scrollPane1 = new JScrollPane();
		oslText = new JEditorPane();
		
		//======== this ========
		setTitle(bundle.getString("About.this.title"));
		setResizable(false);
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		
		//======== panel1 ========
		{
			panel1.setLayout(new GridBagLayout());
			((GridBagLayout) panel1.getLayout()).columnWidths = new int[]{700, 0};
			((GridBagLayout) panel1.getLayout()).rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 208, 0};
			((GridBagLayout) panel1.getLayout()).columnWeights = new double[]{1.0, 1.0E-4};
			((GridBagLayout) panel1.getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
			
			//---- label1 ----
			label1.setIcon(new ImageIcon(getClass().getResource("/rounded.png")));
			panel1.add(label1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
					new Insets(15, 0, 5, 0), 0, 0));
			
			//---- label2 ----
			label2.setText("midis2jam2");
			label2.setFont(label2.getFont().deriveFont(label2.getFont().getStyle() | Font.BOLD));
			panel1.add(label2, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 5, 0), 0, 0));
			
			//---- label3 ----
			label3.setText("vX.X.X");
			panel1.add(label3, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 5, 0), 0, 0));
			
			//---- label4 ----
			label4.setText(bundle.getString("About.what_is_midis2jam2"));
			panel1.add(label4, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 5, 0), 0, 0));
			
			//---- websiteLink ----
			websiteLink.setBackground(UIManager.getColor("darcula.background"));
			websiteLink.setContentType("text/html");
			websiteLink.setText(bundle.getString("About.websiteLink.text"));
			websiteLink.setEditable(false);
			websiteLink.setOpaque(false);
			panel1.add(websiteLink, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 5, 0), 0, 0));
			
			//---- copyrightInfo ----
			copyrightInfo.setContentType("text/html");
			copyrightInfo.setText("<html>\n  <head>\n\n  </head>\n  <body>\n    <p style=\"margin-top: 0; text-align: center;\">\n      Copyright &copy; MMXXI Jacob Wysko<br/>\n      <small>Some assets Copyright &copy; Scott Haag 2007</small>\n    </p>\n  </body>\n</html>\n");
			copyrightInfo.setEditable(false);
			copyrightInfo.setBackground(UIManager.getColor("Button.background"));
			panel1.add(copyrightInfo, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 0), 0, 0));
			
			//---- otherContribs ----
			otherContribs.setText("<html><small>Other contributors: jlachniet \u2022 nikitalita \u2022 Mr. Tremolo Measure</small></html>");
			otherContribs.setHorizontalAlignment(SwingConstants.CENTER);
			panel1.add(otherContribs, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 0), 0, 0));
			
			//---- licenseInfo ----
			licenseInfo.setBackground(UIManager.getColor("darcula.background"));
			licenseInfo.setContentType("text/html");
			licenseInfo.setText("<html>\n<div style=\"text-align:center\">\nThis program comes with absolutely no warranty.<br/>\nSee the <a href=\"https://www.gnu.org/licenses/gpl-3.0.en.html\">GNU General Public License, version 3</a> for details.\n</div>\n</html>\n\n");
			licenseInfo.setEditable(false);
			licenseInfo.setOpaque(false);
			panel1.add(licenseInfo, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 20, 0), 0, 0));
			
			//---- label5 ----
			label5.setText("Open source libraries");
			label5.setHorizontalAlignment(SwingConstants.CENTER);
			label5.setFont(label5.getFont().deriveFont(label5.getFont().getStyle() | Font.BOLD));
			panel1.add(label5, new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 0), 0, 0));
			
			//======== scrollPane1 ========
			{
				
				//---- oslText ----
				oslText.setBackground(UIManager.getColor("Button.background"));
				oslText.setContentType("text/html");
				oslText.setFont(oslText.getFont().deriveFont(oslText.getFont().getSize() - 2f));
				oslText.setEditable(false);
				scrollPane1.setViewportView(oslText);
			}
			panel1.add(scrollPane1, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 0), 0, 0));
		}
		contentPane.add(panel1, BorderLayout.CENTER);
		pack();
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
		
		
		// Set version number
		
		String version = Utils.resourceToString("/version.txt");
		label3.setText("v" + version);
		
		licenseInfo.addHyperlinkListener(About::hyperlinkUpdate);
		websiteLink.addHyperlinkListener(About::hyperlinkUpdate);
		
		// Load OSL HTML (remove style tag)
		String oslHTML = Utils.resourceToString("/dependency-license.html");
		final Matcher matcher = Pattern.compile("([\\s\\S]+)<style>[\\s\\S]+<\\/style>([\\s\\S]+)").matcher(oslHTML);
		matcher.find();
		oslHTML = matcher.group(1) + matcher.group(2);
		oslText.setText(oslHTML);
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				scrollPane1.getVerticalScrollBar().setValue(0);
			}
		});
		
		oslText.addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					try {
						Desktop.getDesktop().browse(e.getURL().toURI());
					} catch (URISyntaxException | IOException ex) {
						Midis2jam2.getLOGGER().warning("Failed to load URI.");
						Midis2jam2.getLOGGER().warning(Utils.exceptionToLines(ex));
					}
				}
			}
		});
		
	}
	
	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JPanel panel1;
	
	private JLabel label1;
	
	private JLabel label2;
	
	private JLabel label3;
	
	private JLabel label4;
	
	private JEditorPane websiteLink;
	
	private JEditorPane copyrightInfo;
	
	private JLabel otherContribs;
	
	private JEditorPane licenseInfo;
	
	private JLabel label5;
	
	private JScrollPane scrollPane1;
	
	private JEditorPane oslText;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
