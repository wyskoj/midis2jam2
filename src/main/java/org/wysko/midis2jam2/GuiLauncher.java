/*
 * Created by JFormDesigner on Fri Apr 30 22:22:15 EDT 2021
 */

package org.wysko.midis2jam2;

import com.formdev.flatlaf.IntelliJTheme;
import org.wysko.midis2jam2.gui.JResizedIconButton;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;

import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.FILES_ONLY;

/**
 * @author Jacob Wysko
 */
public class GuiLauncher extends JFrame {
	
	private File selectedMidiFile;
	
	private File selectedSf2File;
	
	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JLabel logo;
	
	private JPanel panel1;
	
	private JLabel label1;
	
	private JTextField midiFilePathTextField;
	
	private JResizedIconButton loadMidiFileButton;
	
	private JLabel label2;
	
	private JComboBox midiDeviceDropDown;
	
	private JLabel label3;
	
	private JTextField soundFontPathTextField;
	
	private JButton loadSoundFontButton;
	
	private JPanel panel2;
	
	private JPanel hSpacer1;
	
	private JLabel label4;
	
	private JSpinner latencySpinner;
	
	private JPanel hSpacer2;
	
	private JButton startButton;
	
	public GuiLauncher() {
		initComponents();
	}
	
	public static void main(String[] args) {
		// Initialize GUI
		IntelliJTheme.install(GuiLauncher.class.getResourceAsStream("/Material Darker Contrast.theme.json"));
		var guiLauncher = new GuiLauncher();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		guiLauncher.setSize(new Dimension(535, 450));
		guiLauncher.setLocation(dim.width / 2 - guiLauncher.getSize().width / 2, dim.height / 2 - guiLauncher.getSize().height / 2);
		guiLauncher.setVisible(true);
		guiLauncher.setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		// Load MIDI devices
		var infoArr = MidiSystem.getMidiDeviceInfo();
		var aModel = new DefaultComboBoxModel<MidiDevice.Info>();
		aModel.addAll(Arrays.asList(infoArr));
		guiLauncher.midiDeviceDropDown.setModel(aModel);
		guiLauncher.midiDeviceDropDown.setSelectedIndex(0);
	}
	
	private void loadMidiFileButtonActionPerformed(ActionEvent e) {
		var f = new JFileChooser();
		f.setPreferredSize(new Dimension(800, 600));
		f.setDialogTitle("Load MIDI file");
		f.setMultiSelectionEnabled(false);
		f.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith(".mid") || f.getName().toLowerCase(Locale.ROOT).endsWith(".midi");
			}
			
			@Override
			public String getDescription() {
				return "Standard MIDI files (*.mid; *.midi)";
			}
		});
		f.getActionMap().get("viewTypeDetails").actionPerformed(null); // Set default to details view
		f.setFileSelectionMode(FILES_ONLY);
		if (f.showDialog(this, "Load...") == APPROVE_OPTION) {
			selectedMidiFile = f.getSelectedFile();
			midiFilePathTextField.setText(selectedMidiFile.getAbsolutePath());
		}
	}
	
	private void loadSoundFontButtonActionPerformed(ActionEvent e) {
		var f = new JFileChooser();
		f.setPreferredSize(new Dimension(800, 600));
		f.setDialogTitle("Load Soundfont file");
		f.setMultiSelectionEnabled(false);
		f.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith(".sf2");
			}
			
			@Override
			public String getDescription() {
				return "Soundfont files (*.sf2)";
			}
		});
		f.getActionMap().get("viewTypeDetails").actionPerformed(null); // Set default to details view
		f.setFileSelectionMode(FILES_ONLY);
		if (f.showDialog(this, "Load...") == APPROVE_OPTION) {
			selectedSf2File = f.getSelectedFile();
			soundFontPathTextField.setText(selectedSf2File.getAbsolutePath());
		}
	}
	
	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		logo = new JLabel();
		panel1 = new JPanel();
		label1 = new JLabel();
		midiFilePathTextField = new JTextField();
		loadMidiFileButton = new JResizedIconButton();
		label2 = new JLabel();
		midiDeviceDropDown = new JComboBox();
		label3 = new JLabel();
		soundFontPathTextField = new JTextField();
		loadSoundFontButton = new JButton();
		panel2 = new JPanel();
		hSpacer1 = new JPanel(null);
		label4 = new JLabel();
		latencySpinner = new JSpinner();
		hSpacer2 = new JPanel(null);
		startButton = new JButton();
		
		//======== this ========
		setTitle("midis2jam2");
		setIconImage(new ImageIcon(getClass().getResource("/ico/icon16.png")).getImage());
		var contentPane = getContentPane();
		contentPane.setLayout(new GridBagLayout());
		((GridBagLayout) contentPane.getLayout()).columnWidths = new int[]{0, 0};
		((GridBagLayout) contentPane.getLayout()).rowHeights = new int[]{132, 34, 77, 0, 0};
		((GridBagLayout) contentPane.getLayout()).columnWeights = new double[]{1.0, 1.0E-4};
		((GridBagLayout) contentPane.getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0E-4};
		
		//---- logo ----
		logo.setIcon(new ImageIcon(getClass().getResource("/logo.png")));
		logo.setHorizontalAlignment(SwingConstants.CENTER);
		contentPane.add(logo, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(10, 0, 5, 0), 0, 0));
		
		//======== panel1 ========
		{
			panel1.setBorder(new TitledBorder(null, "Configuration", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
			panel1.setLayout(new GridBagLayout());
			((GridBagLayout) panel1.getLayout()).columnWidths = new int[]{103, 141, 92, 0, 0};
			((GridBagLayout) panel1.getLayout()).rowHeights = new int[]{0, 0, 4, 0};
			((GridBagLayout) panel1.getLayout()).columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0E-4};
			((GridBagLayout) panel1.getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 1.0E-4};
			
			//---- label1 ----
			label1.setText("MIDI File:");
			label1.setHorizontalAlignment(SwingConstants.RIGHT);
			label1.setLabelFor(midiFilePathTextField);
			panel1.add(label1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			panel1.add(midiFilePathTextField, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- loadMidiFileButton ----
			loadMidiFileButton.setText("Load...");
			loadMidiFileButton.addActionListener(e -> loadMidiFileButtonActionPerformed(e));
			panel1.add(loadMidiFileButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- label2 ----
			label2.setText("MIDI Device:");
			label2.setHorizontalAlignment(SwingConstants.RIGHT);
			label2.setLabelFor(midiDeviceDropDown);
			panel1.add(label2, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			panel1.add(midiDeviceDropDown, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- label3 ----
			label3.setText("SoundFont:");
			label3.setHorizontalAlignment(SwingConstants.RIGHT);
			label3.setLabelFor(soundFontPathTextField);
			panel1.add(label3, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 5), 0, 0));
			panel1.add(soundFontPathTextField, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 5), 0, 0));
			
			//---- loadSoundFontButton ----
			loadSoundFontButton.setText("Load...");
			loadSoundFontButton.addActionListener(e -> loadSoundFontButtonActionPerformed(e));
			panel1.add(loadSoundFontButton, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 5), 0, 0));
		}
		contentPane.add(panel1, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(5, 5, 10, 5), 0, 0));
		
		//======== panel2 ========
		{
			panel2.setBorder(new TitledBorder(null, "Settings", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
			panel2.setLayout(new GridBagLayout());
			((GridBagLayout) panel2.getLayout()).columnWidths = new int[]{0, 27, 0, 0, 0};
			((GridBagLayout) panel2.getLayout()).rowHeights = new int[]{0, 0};
			((GridBagLayout) panel2.getLayout()).columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0E-4};
			((GridBagLayout) panel2.getLayout()).rowWeights = new double[]{0.0, 1.0E-4};
			panel2.add(hSpacer1, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 5), 0, 0));
			
			//---- label4 ----
			label4.setText("Latency fix (in milliseconds):");
			label4.setLabelFor(latencySpinner);
			panel2.add(label4, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 0, 5), 0, 0));
			
			//---- latencySpinner ----
			latencySpinner.setModel(new SpinnerNumberModel(0, null, null, 1));
			panel2.add(latencySpinner, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 0, 5), 0, 0));
			panel2.add(hSpacer2, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 0), 0, 0));
		}
		contentPane.add(panel2, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(5, 5, 10, 5), 0, 0));
		
		//---- startButton ----
		startButton.setText("Start!");
		startButton.setFont(new Font("Segoe UI", Font.ITALIC, 16));
		contentPane.add(startButton, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
				new Insets(10, 0, 0, 0), 0, 0));
		pack();
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
