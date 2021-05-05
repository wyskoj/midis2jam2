/*
 * Created by JFormDesigner on Sat May 01 01:00:38 EDT 2021
 */

package org.wysko.midis2jam2;

import com.formdev.flatlaf.IntelliJTheme;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.wysko.midis2jam2.Midis2jam2.M2J2Settings;
import org.wysko.midis2jam2.gui.*;
import org.wysko.midis2jam2.midi.MidiFile;
import org.wysko.midis2jam2.util.Utils;

import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;

import static java.awt.Cursor.getPredefinedCursor;
import static java.util.Objects.requireNonNull;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.FILES_ONLY;
import static javax.swing.JOptionPane.*;

/**
 * @author Jacob Wysko
 */
@SuppressWarnings("unused")
public class GuiLauncher extends JFrame {
	
	public GuiLauncher() {
		initComponents();
	}
	
	private static final File SETTINGS_FILE = new File("midis2jam2.conf");
	
	private transient LauncherSettings settings;
	
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
				jep.setEditable(false);
				jep.setOpaque(false);
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
					showMessageDialog(guiLauncher, jep,
							"Update available", WARNING_MESSAGE);
					Midis2jam2.logger.warning("Out of date!!");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		
		// Launch directly into midis2jam2 if a MIDI file is specified
		if (args.length == 1) {
			guiLauncher.midiFilePathTextField.setText(args[0]);
			guiLauncher.startButtonPressed(null);
		}
		
		// Bring GUI to front
		guiLauncher.bringToFront();
		
		
		// Load YAML
		
		try {
			final LauncherSettings settings = new Gson().fromJson(new FileReader(SETTINGS_FILE), LauncherSettings.class);
			if (settings == null) throw new Exception();
			guiLauncher.settings = settings;
		} catch (Exception e) {
			guiLauncher.settings = new LauncherSettings();
			guiLauncher.settings.getSoundFontPaths().add(null); // Default SoundFont
		}
		
		guiLauncher.updateSf2List();
	}
	
	public void updateSf2List() {
		soundFontPathDropDown.removeAllItems();
		settings.getSoundFontPaths().forEach(e -> soundFontPathDropDown.addItem(e));
		soundFontPathDropDown.revalidate();
		settings.setSoundFontPaths(settings.getSoundFontPaths());
		settings.setLastMidiDir(settings.getLastMidiDir());
		saveSettings();
	}
	
	private void saveSettings() {
		try {
			var writer = new FileWriter(SETTINGS_FILE);
			var gsonBuilder = new GsonBuilder();
			gsonBuilder.excludeFieldsWithoutExposeAnnotation().create().toJson(settings, writer);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static String getVersion() {
		return new Scanner(requireNonNull(GuiLauncher.class.getResourceAsStream("/version.txt"))).next();
	}
	
	/**
	 * Prompts the user to load a MIDI file.
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
		f.setCurrentDirectory(new File(settings.getLastMidiDir()));
		if (f.showDialog(this, "Load") == APPROVE_OPTION) {
			var selectedMidiFile = f.getSelectedFile();
			midiFilePathTextField.setText(selectedMidiFile.getAbsolutePath());
			settings.setLastMidiDir(selectedMidiFile.getParentFile().getAbsolutePath());
			saveSettings();
		}
	}
	
	
	/**
	 * Prompts the user to load a soundfont file.
	 */
	private void loadSoundFontButtonActionPerformed(ActionEvent e) {
		
		var dialog = new JDialog(this, "SoundFont list editor", true);
		dialog.getContentPane().add(new SoundFontList(this.settings.getSoundFontPaths(), this));
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		dialog.setLocation(dim.width / 2 - getSize().width / 2,
				dim.height / 2 - getSize().height / 2);
		dialog.setPreferredSize(new Dimension(600, 400));
		dialog.pack();
		dialog.setVisible(true);
		
	}
	
	private void midiDeviceDropDownActionPerformed(ActionEvent e) {
		if (((MidiDevice.Info) requireNonNull(midiDeviceDropDown.getSelectedItem())).getName().equals("Gervill")) {
			soundfontLabel.setEnabled(true);
			soundFontPathDropDown.setEnabled(true);
			loadSoundFontButton.setEnabled(true);
			latencySpinner.setValue(100);
		} else {
			soundfontLabel.setEnabled(false);
			soundFontPathDropDown.setEnabled(false);
			loadSoundFontButton.setEnabled(false);
			latencySpinner.setValue(0);
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
		Sequence sequence;
		try {
			sequence = MidiSystem.getSequence(midiFile);
		} catch (IOException ioException) {
			this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			showMessageDialog(this, new ExceptionDisplay("There was an error reading the MIDI file.", ioException), "I/O error", ERROR_MESSAGE);
			return;
		} catch (InvalidMidiDataException invalidMidiDataException) {
			this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			showMessageDialog(this, new ExceptionDisplay("The MIDI file has invalid data, or is not a Standard MIDI " +
					"file.", invalidMidiDataException), "Bad MIDI file", ERROR_MESSAGE);
			return;
		}
		
		// Collect sf2
		Soundbank soundfont = null;
		final String selectedSf2Path = (String) soundFontPathDropDown.getSelectedItem();
		if (selectedSf2Path != null) {
			var soundFontFile = new File(selectedSf2Path);
			if (!soundFontFile.exists()) {
				this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				showMessageDialog(this, "The specified SoundFont does not exist.", "SoundFont file does not exist",
						ERROR_MESSAGE);
				return;
			}
			try {
				soundfont = MidiSystem.getSoundbank(soundFontFile);
			} catch (InvalidMidiDataException invalidMidiDataException) {
				this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				showMessageDialog(this, new ExceptionDisplay("The SoundFont file has invalid data, or is not a " +
						"SoundFont file.", invalidMidiDataException), "Bad SoundFont file", ERROR_MESSAGE);
				return;
			} catch (IOException ioException) {
				this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				showMessageDialog(this, new ExceptionDisplay("There was an error reading the SoundFont file.",
						ioException), "I/O error", ERROR_MESSAGE);
				return;
			}
		}
		
		
		// Initialize MIDI
		try {
			var selectedDevice = requireNonNull((MidiDevice.Info) midiDeviceDropDown.getSelectedItem());
			Sequencer sequencer;
			
			var midiDevice = MidiSystem.getMidiDevice(selectedDevice);
			
			if (selectedDevice.getName().equals("Gervill")) {
				// Internal synth
				var synthesizer = MidiSystem.getSynthesizer();
				synthesizer.open();
				// Load soundfont
				if (soundfont != null) {
					sequencer = MidiSystem.getSequencer(false);
					sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
					synthesizer.loadAllInstruments(soundfont);
				} else {
					sequencer = MidiSystem.getSequencer(true);
				}
			} else {
				// External synth
				midiDevice.open();
				sequencer = MidiSystem.getSequencer(false);
				sequencer.getTransmitter().setReceiver(midiDevice.getReceiver());
			}
			sequencer.open();
			sequencer.setSequence(sequence);
			
			var liaison = new Liaison(this, sequencer, MidiFile.readMidiFile(midiFile),
					M2J2Settings.create(((int) latencySpinner.getValue())));
			this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			SwingUtilities.invokeLater(() -> new Thread(liaison::start).start());
			
		} catch (MidiUnavailableException midiUnavailableException) {
			showMessageDialog(this, new ExceptionDisplay("The requested MIDI component cannot be opened or created " +
					"because it is unavailable.", midiUnavailableException), "MIDI Unavailable Error", ERROR_MESSAGE);
			this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		} catch (InvalidMidiDataException invalidMidiDataException) {
			showMessageDialog(this, new ExceptionDisplay("Inappropriate MIDI data was encountered.",
					invalidMidiDataException), "Invalid MIDI data Error", ERROR_MESSAGE);
			this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		} catch (IOException ioException) {
			showMessageDialog(this, new ExceptionDisplay("An I/O error occurred.",
					ioException), "I/O Error", ERROR_MESSAGE);
			this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}
	
	public void disableAll() {
		this.setEnabled(false);
	}
	
	public void enableAll() {
		this.setEnabled(true);
		bringToFront();
	}
	
	private void bringToFront() {
		SwingUtilities.invokeLater(() -> {
			this.toFront();
			this.repaint();
		});
	}
	
	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		logo = new JLabel();
		panel1 = new JPanel();
		label1 = new JLabel();
		midiFilePathTextField = new JTextField();
		loadMidiFileButton = new JResizedIconButton();
		label3 = new JLabel();
		label2 = new JLabel();
		midiDeviceDropDown = new JComboBox<>();
		label5 = new JLabel();
		soundfontLabel = new JLabel();
		soundFontPathDropDown = new JComboBox<>();
		soundFontPathDropDown.setRenderer(new SoundFontListCellRenderer());
		loadSoundFontButton = new JResizedIconButton();
		label6 = new JLabel();
		panel2 = new JPanel();
		hSpacer1 = new JPanel(null);
		label4 = new JLabel();
		latencySpinner = new JSpinner();
		hSpacer2 = new JPanel(null);
		label7 = new JLabel();
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
			((GridBagLayout) panel1.getLayout()).columnWidths = new int[]{109, 141, 92, 0, 0};
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
			
			//---- label3 ----
			label3.setIcon(new ImageIcon(getClass().getResource("/help.png")));
			label3.setToolTipText("Press \"Load...\" to select a MIDI file to play.");
			panel1.add(label3, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
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
			midiDeviceDropDown.addActionListener(e -> midiDeviceDropDownActionPerformed(e));
			panel1.add(midiDeviceDropDown, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- label5 ----
			label5.setIcon(new ImageIcon(getClass().getResource("/help.png")));
			label5.setToolTipText("This dropdown shows a list of active MIDI devices available on your\ncomputer. Gervill is the default Java MIDI synthesizer and can play\nwith SoundFonts.");
			panel1.add(label5, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- soundfontLabel ----
			soundfontLabel.setText("SoundFont:");
			soundfontLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			soundfontLabel.setLabelFor(soundFontPathDropDown);
			panel1.add(soundfontLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- soundFontPathDropDown ----
			soundFontPathDropDown.setEditable(false);
			panel1.add(soundFontPathDropDown, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- loadSoundFontButton ----
			loadSoundFontButton.setText("Edit...");
			loadSoundFontButton.setIcon(new ImageIcon(getClass().getResource("/soundfont.png")));
			loadSoundFontButton.addActionListener(e -> loadSoundFontButtonActionPerformed(e));
			panel1.add(loadSoundFontButton, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- label6 ----
			label6.setIcon(new ImageIcon(getClass().getResource("/help.png")));
			label6.setToolTipText("Press \"Edit...\" to edit the list of SoundFonts available to use. To \nplay a MIDI file with a SoundFont, ensure Gervill is the currently\nenabled MIDI device.");
			panel1.add(label6, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
		}
		contentPane.add(panel1, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(5, 5, 10, 5), 0, 0));
		
		//======== panel2 ========
		{
			panel2.setBorder(new TitledBorder(null, "Settings", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
			panel2.setLayout(new GridBagLayout());
			((GridBagLayout) panel2.getLayout()).columnWidths = new int[]{0, 27, 0, 0, 0, 0};
			((GridBagLayout) panel2.getLayout()).rowHeights = new int[]{0, 0};
			((GridBagLayout) panel2.getLayout()).columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
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
			latencySpinner.setModel(new SpinnerNumberModel(100, null, null, 1));
			panel2.add(latencySpinner, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 0, 5), 0, 0));
			panel2.add(hSpacer2, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 5), 0, 0));
			
			//---- label7 ----
			label7.setIcon(new ImageIcon(getClass().getResource("/help.png")));
			label7.setToolTipText("The audio and video may be out of sync. Adjust this number to align\nthem. Gervill tends to need a value of 100.");
			panel2.add(label7, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 5), 0, 0));
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
	
	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JLabel logo;
	
	private JPanel panel1;
	
	private JLabel label1;
	
	private JTextField midiFilePathTextField;
	
	private JResizedIconButton loadMidiFileButton;
	
	private JLabel label3;
	
	private JLabel label2;
	
	private JComboBox<MidiDevice.Info> midiDeviceDropDown;
	
	private JLabel label5;
	
	private JLabel soundfontLabel;
	
	private JComboBox<String> soundFontPathDropDown;
	
	private JResizedIconButton loadSoundFontButton;
	
	private JLabel label6;
	
	private JPanel panel2;
	
	private JPanel hSpacer1;
	
	private JLabel label4;
	
	private JSpinner latencySpinner;
	
	private JPanel hSpacer2;
	
	private JLabel label7;
	
	private JResizedIconButton startButton;
	
	private JLabel versionText;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
