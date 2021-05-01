/*
 * Created by JFormDesigner on Sat May 01 01:00:38 EDT 2021
 */

package org.wysko.midis2jam2;

import com.formdev.flatlaf.IntelliJTheme;
import org.wysko.midis2jam2.gui.ExceptionDisplay;
import org.wysko.midis2jam2.gui.JResizedIconButton;
import org.wysko.midis2jam2.midi.MidiFile;
import org.wysko.midis2jam2.util.Utils;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Soundbank;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import static java.awt.Cursor.getPredefinedCursor;
import static java.util.Objects.requireNonNull;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.FILES_ONLY;
import static javax.swing.JOptionPane.*;
import static org.wysko.midis2jam2.Launcher.getVersion;

/**
 * @author Jacob Wysko
 */
@SuppressWarnings("unused")
public class GuiLauncher extends JFrame {
	
	/**
	 * The selected soundfont (sf2) file.
	 */
	private File selectedSf2File;
	
	/**
	 * The selected Standard MIDI file.
	 */
	private File selectedMidiFile;
	
	public GuiLauncher() {
		initComponents();
	}
	
	private JResizedIconButton resetSoundFontButton;
	
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
		aModel.addAll(Arrays.stream(infoArr).filter(i -> !i.getName().equals("Real Time Sequencer")).collect(Collectors.toList()));
		guiLauncher.midiDeviceDropDown.setModel(aModel);
		guiLauncher.midiDeviceDropDown.setSelectedIndex(0);
		
		// Set tooltip values
		ToolTipManager.sharedInstance().setInitialDelay(200);
		ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
		
		// Set version number
		String version = new BufferedReader(new InputStreamReader(requireNonNull(GuiLauncher.class.getResourceAsStream("/version.txt")))).lines().collect(Collectors.joining("\n"));
		guiLauncher.versionText.setText(version);
		
		// Check for updates
		EventQueue.invokeLater(() -> {
			try {
				var html = Utils.getHTML("https://midis2jam2.xyz/api/update?v=" + getVersion());
				var jep = new JEditorPane();
				jep.setContentType("text/html");
				jep.setText("<html>This version is out of date and is no longer supported. <a " +
						"href=\"https://midis2jam2.xyz\">Download the latest version.</a></html>\"");
				jep.setEditable(false);//so its not editable
				jep.setOpaque(false);//so we dont see whit background
				jep.addHyperlinkListener(hle -> {
					if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
						try {
							Desktop.getDesktop().browse(hle.getURL().toURI());
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				});
				if (html.contains("Out of")) {
					JOptionPane.showMessageDialog(guiLauncher, jep,
							"Update available", WARNING_MESSAGE);
					Midis2jam2.logger.warning("Out of date!!");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	
	/**
	 * Prompts the user to load a MIDI file, the result is stored in {@link #selectedMidiFile}.
	 */
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
		if (f.showDialog(this, "Load") == APPROVE_OPTION) {
			selectedMidiFile = f.getSelectedFile();
			midiFilePathTextField.setText(selectedMidiFile.getAbsolutePath());
		}
	}
	
	/**
	 * Prompts the user to load a soundfont file, the result is stored in {@link #selectedSf2File}.
	 */
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
		if (f.showDialog(this, "Load") == APPROVE_OPTION) {
			selectedSf2File = f.getSelectedFile();
			soundFontPathTextField.setText(selectedSf2File.getAbsolutePath());
		}
	}
	
	private void midiDeviceDropDownActionPerformed(ActionEvent e) {
		if (!((MidiDevice.Info) midiDeviceDropDown.getSelectedItem()).getName().equals("Gervill")) {
			soundfontLabel.setEnabled(false);
			soundFontPathTextField.setEnabled(false);
			loadSoundFontButton.setEnabled(false);
			resetSoundFontButton.setEnabled(false);
		} else {
			soundfontLabel.setEnabled(true);
			soundFontPathTextField.setEnabled(true);
			loadSoundFontButton.setEnabled(true);
			resetSoundFontButton.setEnabled(true);
		}
	}
	
	private void startButtonPressed(ActionEvent e) {
		// Collect MIDI file
		this.setCursor(getPredefinedCursor(Cursor.WAIT_CURSOR));
		if (midiFilePathTextField.getText().isBlank()) {
			this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			showMessageDialog(this, "You must specify a MIDI file.", "No MIDI file selected", INFORMATION_MESSAGE);
			return;
		}
		var midiFile = new File(midiFilePathTextField.getText());
		if (!midiFile.exists()) {
			this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			showMessageDialog(this, "The specified MIDI file does not exist.", "MIDI file does not exist", ERROR_MESSAGE);
			return;
		}
		try {
			MidiFile.readMidiFile(midiFile);
		} catch (IOException ioException) {
			this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			showMessageDialog(this, new ExceptionDisplay("There was an error reading the MIDI file.", ioException), "I/O error", JOptionPane.ERROR_MESSAGE);
			return;
		} catch (InvalidMidiDataException invalidMidiDataException) {
			this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			showMessageDialog(this, new ExceptionDisplay("The MIDI file has invalid data, or is not a Standard MIDI " +
					"file.", invalidMidiDataException), "Bad MIDI file", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		// Collect sf2
		Soundbank soundfont = null;
		if (!soundFontPathTextField.getText().equals("Default SoundFont")) {
			var soundfontFile = new File(soundFontPathTextField.getText());
			if (!soundfontFile.exists()) {
				this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				showMessageDialog(this, "The specified SoundFont does not exist.", "SoundFont file does not exist",
						ERROR_MESSAGE);
				return;
			}
			try {
				MidiSystem.getSoundbank(soundfontFile);
			} catch (InvalidMidiDataException invalidMidiDataException) {
				this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				showMessageDialog(this, new ExceptionDisplay("The SoundFont file has invalid data, or is not a " +
						"SoundFont file.", invalidMidiDataException), "Bad SoundFont file", JOptionPane.ERROR_MESSAGE);
				return;
			} catch (IOException ioException) {
				this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				showMessageDialog(this, new ExceptionDisplay("There was an error reading the SoundFont file.",
						ioException), "I/O error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
	private void resetSoundFontButtonActionPerformed(ActionEvent e) {
		soundFontPathTextField.setText("Default SoundFont");
	}
	
	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JLabel logo;
	
	private JPanel panel1;
	
	private JLabel label1;
	
	private JTextField midiFilePathTextField;
	
	private JResizedIconButton loadMidiFileButton;
	
	private JLabel label2;
	
	private JComboBox<MidiDevice.Info> midiDeviceDropDown;
	
	private JLabel soundfontLabel;
	
	private JTextField soundFontPathTextField;
	
	private JResizedIconButton loadSoundFontButton;
	
	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		logo = new JLabel();
		panel1 = new JPanel();
		label1 = new JLabel();
		midiFilePathTextField = new JTextField();
		loadMidiFileButton = new JResizedIconButton();
		label2 = new JLabel();
		midiDeviceDropDown = new JComboBox<>();
		soundfontLabel = new JLabel();
		soundFontPathTextField = new JTextField();
		loadSoundFontButton = new JResizedIconButton();
		resetSoundFontButton = new JResizedIconButton();
		panel2 = new JPanel();
		hSpacer1 = new JPanel(null);
		label4 = new JLabel();
		latencySpinner = new JSpinner();
		hSpacer2 = new JPanel(null);
		startButton = new JResizedIconButton();
		versionText = new JLabel();
		
		//======== this ========
		setTitle("midis2jam2");
		setIconImage(new ImageIcon(getClass().getResource("/ico/icon16.png")).getImage());
		setResizable(false);
		var contentPane = getContentPane();
		contentPane.setLayout(new GridBagLayout());
		((GridBagLayout) contentPane.getLayout()).columnWidths = new int[]{0, 0};
		((GridBagLayout) contentPane.getLayout()).rowHeights = new int[]{132, 145, 77, 0, 0, 0};
		((GridBagLayout) contentPane.getLayout()).columnWeights = new double[]{1.0, 1.0E-4};
		((GridBagLayout) contentPane.getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
		
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
			((GridBagLayout) panel1.getLayout()).columnWidths = new int[]{87, 141, 92, 0, 0};
			((GridBagLayout) panel1.getLayout()).rowHeights = new int[]{0, 0, 9, 0, 0};
			((GridBagLayout) panel1.getLayout()).columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0E-4};
			((GridBagLayout) panel1.getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0E-4};
			
			//---- label1 ----
			label1.setText("MIDI File:");
			label1.setHorizontalAlignment(SwingConstants.RIGHT);
			label1.setLabelFor(midiFilePathTextField);
			panel1.add(label1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- midiFilePathTextField ----
			midiFilePathTextField.setEditable(false);
			midiFilePathTextField.setToolTipText("Specify the MIDI file to play.");
			panel1.add(midiFilePathTextField, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- loadMidiFileButton ----
			loadMidiFileButton.setText("Load...");
			loadMidiFileButton.setIcon(new ImageIcon(getClass().getResource("/open.png")));
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
			
			//---- midiDeviceDropDown ----
			midiDeviceDropDown.setToolTipText("Select the MIDI device to play from. Gervill is\nthe default device and can play soundfonts.");
			midiDeviceDropDown.addActionListener(e -> midiDeviceDropDownActionPerformed(e));
			panel1.add(midiDeviceDropDown, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- soundfontLabel ----
			soundfontLabel.setText("SoundFont:");
			soundfontLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			soundfontLabel.setLabelFor(soundFontPathTextField);
			panel1.add(soundfontLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- soundFontPathTextField ----
			soundFontPathTextField.setEditable(false);
			soundFontPathTextField.setToolTipText("Optionally specify a soundfont.");
			soundFontPathTextField.setText("Default SoundFont");
			panel1.add(soundFontPathTextField, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- loadSoundFontButton ----
			loadSoundFontButton.setText("Load...");
			loadSoundFontButton.setIcon(new ImageIcon(getClass().getResource("/open.png")));
			loadSoundFontButton.addActionListener(e -> loadSoundFontButtonActionPerformed(e));
			panel1.add(loadSoundFontButton, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- resetSoundFontButton ----
			resetSoundFontButton.setIcon(new ImageIcon(getClass().getResource("/reset.png")));
			resetSoundFontButton.setToolTipText("Reset to the default SoundFont.");
			resetSoundFontButton.addActionListener(e -> resetSoundFontButtonActionPerformed(e));
			panel1.add(resetSoundFontButton, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 0), 0, 0));
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
		startButton.setIcon(new ImageIcon(getClass().getResource("/music.png")));
		startButton.addActionListener(e -> startButtonPressed(e));
		contentPane.add(startButton, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
				new Insets(10, 0, 5, 0), 0, 0));
		
		//---- versionText ----
		versionText.setText("text");
		versionText.setHorizontalAlignment(SwingConstants.RIGHT);
		contentPane.add(versionText, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(0, 0, 10, 5), 0, 0));
		pack();
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}
	
	private JPanel panel2;
	
	private JPanel hSpacer1;
	
	private JLabel label4;
	
	private JSpinner latencySpinner;
	
	private JPanel hSpacer2;
	
	private JResizedIconButton startButton;
	
	private JLabel versionText;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
