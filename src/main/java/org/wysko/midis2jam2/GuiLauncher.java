/*
 * Created by JFormDesigner on Sat May 01 01:00:38 EDT 2021
 */

package org.wysko.midis2jam2;

import com.formdev.flatlaf.IntelliJTheme;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ini4j.Ini;
import org.jetbrains.annotations.NotNull;
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
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.regex.Pattern;
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
	
	private static final File INI_FILE = new File("midis2jam2.ini");
	
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
		guiLauncher.setSize(new Dimension(590, 530));
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
		try {
			String version = new BufferedReader(new InputStreamReader(requireNonNull(GuiLauncher.class.getResourceAsStream("/version.txt")))).lines().collect(Collectors.joining("\n"));
			guiLauncher.versionText.setText(version);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Check for updates
		EventQueue.invokeLater(() -> new Thread(() -> {
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
		}));
		
		// Launch directly into midis2jam2 if a MIDI file is specified
		if (args.length == 1) {
			guiLauncher.midiFilePathTextField.setText(args[0]);
			guiLauncher.startButtonPressed(null);
		}
		
		// Bring GUI to front
		guiLauncher.bringToFront();
		
		
		// Load GSON
		
		try {
			final LauncherSettings settings = new Gson().fromJson(new FileReader(SETTINGS_FILE), LauncherSettings.class);
			if (settings == null) throw new IOException();
			guiLauncher.settings = settings;
		} catch (Exception e) {
			guiLauncher.settings = new LauncherSettings();
			guiLauncher.settings.getSoundFontPaths().add(null); // Default SoundFont
		}
		
		guiLauncher.updateSf2List();
		
		// Load INI
		
		var ini = guiLauncher.ini();
		if (ini.get("visuals", "transition") != null) {
			for (Component component : guiLauncher.transitionSpeedPanel.getComponents()) {
				((JRadioButton) component).setSelected(component.getName().equals(ini.get("visuals", "transition")));
			}
		}
	}
	
	public void updateSf2List() {
		soundFontPathDropDown.removeAllItems();
		settings.getSoundFontPaths().forEach(e -> soundFontPathDropDown.addItem(e));
		soundFontPathDropDown.revalidate();
		settings.setSoundFontPaths(settings.getSoundFontPaths());
		settings.setLastMidiDir(settings.getLastMidiDir());
		saveSettings();
	}
	
	private Ini ini() {
		try {
			return new Ini(INI_FILE);
		} catch (IOException e) {
			return new Ini();
		}
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
	
	private void saveIni(ActionEvent e) {
		
		// Get transition speed
		JRadioButton button = getSelectedTransitionRadioButton();
		
		var ini = ini();
		
		ini.put("visuals", "transition", button.getName());
		try {
			ini.store(INI_FILE);
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}
	
	@NotNull
	private JRadioButton getSelectedTransitionRadioButton() {
		JRadioButton button = null;
		for (Component component : transitionSpeedPanel.getComponents()) {
			if (((JRadioButton) component).isSelected()) {
				button = (JRadioButton) component;
				break;
			}
		}
		assert button != null;
		return button;
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
			soundFontLabel.setEnabled(true);
			soundFontPathDropDown.setEnabled(true);
			editSoundFontsButton.setEnabled(true);
			latencySpinner.setValue(100);
		} else {
			soundFontLabel.setEnabled(false);
			soundFontPathDropDown.setEnabled(false);
			editSoundFontsButton.setEnabled(false);
			latencySpinner.setValue(0);
		}
		
	}
	
	private void startButtonPressed(ActionEvent e) {
		saveIni(e);
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
			
			var value = (int) latencySpinner.getValue();
			if (midiDevice.getDeviceInfo().getName().startsWith("VirtualMIDISynth")) {
				var vmsConfigFile = new File("C:\\Program Files\\VirtualMIDISynth\\VirtualMIDISynth.conf");
				if (vmsConfigFile.exists()) {
					var vmsConfigData = Files.readString(vmsConfigFile.toPath());
					var matcher = Pattern.compile("AdditionalBuffer=(\\d+)").matcher(vmsConfigData);
					if (matcher.find()) {
						var additionalBuffer = matcher.group(1);
						value += Integer.parseInt(additionalBuffer);
					} else {
						value += 250;
					}
				}
				
			}
			var liaison = new Liaison(this, sequencer, MidiFile.readMidiFile(midiFile), new M2J2Settings(value,
					M2J2Settings.InstrumentTransition.valueOf(getSelectedTransitionRadioButton().getName())),
					fullscreenCheckbox.isSelected());
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
		ResourceBundle bundle = ResourceBundle.getBundle("i18n.guilauncher");
		logo = new JLabel();
		configurationPanel = new JPanel();
		midiFileLabel = new JLabel();
		midiFilePathTextField = new JTextField();
		loadMidiFileButton = new JResizedIconButton();
		midiFileHelp = new JLabel();
		midiDeviceLabel = new JLabel();
		midiDeviceDropDown = new JComboBox<>();
		midiDeviceHelp = new JLabel();
		soundFontLabel = new JLabel();
		soundFontPathDropDown = new JComboBox<>();
		soundFontPathDropDown.setRenderer(new SoundFontListCellRenderer());
		editSoundFontsButton = new JResizedIconButton();
		soundFontHelp = new JLabel();
		settingsPanel = new JPanel();
		hSpacer1 = new JPanel(null);
		latencyFixLabel = new JLabel();
		latencySpinner = new JSpinner();
		hSpacer2 = new JPanel(null);
		latencyHelp = new JLabel();
		displayLabel = new JLabel();
		fullscreenCheckbox = new JCheckBox();
		fullscreenHelp = new JLabel();
		transitionSpeedLabel = new JLabel();
		transitionSpeedPanel = new JPanel();
		transitionSpeedNoneButton = new JRadioButton();
		transitionSpeedSlowButton = new JRadioButton();
		transitionSpeedNormalButton = new JRadioButton();
		transitionSpeedFastButton = new JRadioButton();
		transitionSpeedHelp = new JLabel();
		startButton = new JResizedIconButton();
		versionText = new JLabel();
		
		//======== this ========
		setTitle(bundle.getString("GuiLauncher.this.title"));
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
		
		//======== configurationPanel ========
		{
			configurationPanel.setBorder(new TitledBorder(null, bundle.getString("GuiLauncher.configurationPanel.border"), TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
			configurationPanel.setLayout(new GridBagLayout());
			((GridBagLayout) configurationPanel.getLayout()).columnWidths = new int[]{109, 141, 92, 0, 0};
			((GridBagLayout) configurationPanel.getLayout()).rowHeights = new int[]{0, 0, 9, 0, 0};
			((GridBagLayout) configurationPanel.getLayout()).columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0E-4};
			((GridBagLayout) configurationPanel.getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0E-4};
			
			//---- midiFileLabel ----
			midiFileLabel.setText(bundle.getString("GuiLauncher.midiFileLabel.text"));
			midiFileLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			midiFileLabel.setLabelFor(midiFilePathTextField);
			configurationPanel.add(midiFileLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- midiFilePathTextField ----
			midiFilePathTextField.setEditable(false);
			configurationPanel.add(midiFilePathTextField, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- loadMidiFileButton ----
			loadMidiFileButton.setText(bundle.getString("GuiLauncher.loadMidiFileButton.text"));
			loadMidiFileButton.setIcon(new ImageIcon(getClass().getResource("/open.png")));
			loadMidiFileButton.addActionListener(e -> loadMidiFileButtonActionPerformed(e));
			configurationPanel.add(loadMidiFileButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- midiFileHelp ----
			midiFileHelp.setIcon(new ImageIcon(getClass().getResource("/help.png")));
			midiFileHelp.setToolTipText(bundle.getString("GuiLauncher.midiFileHelp.toolTipText"));
			configurationPanel.add(midiFileHelp, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- midiDeviceLabel ----
			midiDeviceLabel.setText(bundle.getString("GuiLauncher.midiDeviceLabel.text"));
			midiDeviceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			midiDeviceLabel.setLabelFor(midiDeviceDropDown);
			configurationPanel.add(midiDeviceLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- midiDeviceDropDown ----
			midiDeviceDropDown.addActionListener(e -> midiDeviceDropDownActionPerformed(e));
			configurationPanel.add(midiDeviceDropDown, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- midiDeviceHelp ----
			midiDeviceHelp.setIcon(new ImageIcon(getClass().getResource("/help.png")));
			midiDeviceHelp.setToolTipText(bundle.getString("GuiLauncher.midiDeviceHelp.toolTipText"));
			configurationPanel.add(midiDeviceHelp, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- soundFontLabel ----
			soundFontLabel.setText(bundle.getString("GuiLauncher.soundFontLabel.text"));
			soundFontLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			soundFontLabel.setLabelFor(soundFontPathDropDown);
			configurationPanel.add(soundFontLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- soundFontPathDropDown ----
			soundFontPathDropDown.setEditable(false);
			configurationPanel.add(soundFontPathDropDown, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- editSoundFontsButton ----
			editSoundFontsButton.setText(bundle.getString("GuiLauncher.editSoundFontsButton.text"));
			editSoundFontsButton.setIcon(new ImageIcon(getClass().getResource("/soundfont.png")));
			editSoundFontsButton.addActionListener(e -> loadSoundFontButtonActionPerformed(e));
			configurationPanel.add(editSoundFontsButton, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- soundFontHelp ----
			soundFontHelp.setIcon(new ImageIcon(getClass().getResource("/help.png")));
			soundFontHelp.setToolTipText(bundle.getString("GuiLauncher.soundFontHelp.toolTipText"));
			configurationPanel.add(soundFontHelp, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
		}
		contentPane.add(configurationPanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(5, 5, 10, 5), 0, 0));
		
		//======== settingsPanel ========
		{
			settingsPanel.setBorder(new TitledBorder(null, bundle.getString("GuiLauncher.settingsPanel.border"), TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
			settingsPanel.setLayout(new GridBagLayout());
			((GridBagLayout) settingsPanel.getLayout()).columnWidths = new int[]{0, 27, 0, 0, 0, 0};
			((GridBagLayout) settingsPanel.getLayout()).rowHeights = new int[]{0, 0, 0, 6, 0};
			((GridBagLayout) settingsPanel.getLayout()).columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
			((GridBagLayout) settingsPanel.getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0E-4};
			settingsPanel.add(hSpacer1, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- latencyFixLabel ----
			latencyFixLabel.setText(bundle.getString("GuiLauncher.latencyFixLabel.text"));
			latencyFixLabel.setLabelFor(latencySpinner);
			settingsPanel.add(latencyFixLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- latencySpinner ----
			latencySpinner.setModel(new SpinnerNumberModel(100, null, null, 1));
			settingsPanel.add(latencySpinner, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 5, 5), 0, 0));
			settingsPanel.add(hSpacer2, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- latencyHelp ----
			latencyHelp.setIcon(new ImageIcon(getClass().getResource("/help.png")));
			latencyHelp.setToolTipText(bundle.getString("GuiLauncher.latencyHelp.toolTipText"));
			settingsPanel.add(latencyHelp, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- displayLabel ----
			displayLabel.setText(bundle.getString("GuiLauncher.displayLabel.text"));
			settingsPanel.add(displayLabel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- fullscreenCheckbox ----
			fullscreenCheckbox.setText(bundle.getString("GuiLauncher.fullscreenCheckbox.text"));
			fullscreenCheckbox.setHorizontalAlignment(SwingConstants.CENTER);
			settingsPanel.add(fullscreenCheckbox, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- fullscreenHelp ----
			fullscreenHelp.setIcon(new ImageIcon(getClass().getResource("/help.png")));
			fullscreenHelp.setToolTipText(bundle.getString("GuiLauncher.fullscreenHelp.toolTipText"));
			settingsPanel.add(fullscreenHelp, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- transitionSpeedLabel ----
			transitionSpeedLabel.setText(bundle.getString("GuiLauncher.transitionSpeedLabel.text"));
			settingsPanel.add(transitionSpeedLabel, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//======== transitionSpeedPanel ========
			{
				transitionSpeedPanel.setLayout(new GridBagLayout());
				((GridBagLayout) transitionSpeedPanel.getLayout()).columnWidths = new int[]{0, 0, 0};
				((GridBagLayout) transitionSpeedPanel.getLayout()).rowHeights = new int[]{0, 0, 0};
				((GridBagLayout) transitionSpeedPanel.getLayout()).columnWeights = new double[]{0.0, 0.0, 1.0E-4};
				((GridBagLayout) transitionSpeedPanel.getLayout()).rowWeights = new double[]{0.0, 0.0, 1.0E-4};
				
				//---- transitionSpeedNoneButton ----
				transitionSpeedNoneButton.setText(bundle.getString("GuiLauncher.transitionSpeedNoneButton.text"));
				transitionSpeedNoneButton.setName("NONE");
				transitionSpeedNoneButton.addActionListener(e -> saveIni(e));
				transitionSpeedPanel.add(transitionSpeedNoneButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 5, 5), 0, 0));
				
				//---- transitionSpeedSlowButton ----
				transitionSpeedSlowButton.setText(bundle.getString("GuiLauncher.transitionSpeedSlowButton.text"));
				transitionSpeedSlowButton.setName("SLOW");
				transitionSpeedSlowButton.addActionListener(e -> saveIni(e));
				transitionSpeedPanel.add(transitionSpeedSlowButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 5, 0), 0, 0));
				
				//---- transitionSpeedNormalButton ----
				transitionSpeedNormalButton.setText(bundle.getString("GuiLauncher.transitionSpeedNormalButton.text"));
				transitionSpeedNormalButton.setSelected(true);
				transitionSpeedNormalButton.setName("NORMAL");
				transitionSpeedNormalButton.addActionListener(e -> saveIni(e));
				transitionSpeedPanel.add(transitionSpeedNormalButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 0, 5), 0, 0));
				
				//---- transitionSpeedFastButton ----
				transitionSpeedFastButton.setText(bundle.getString("GuiLauncher.transitionSpeedFastButton.text"));
				transitionSpeedFastButton.setName("FAST");
				transitionSpeedFastButton.addActionListener(e -> saveIni(e));
				transitionSpeedPanel.add(transitionSpeedFastButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 0, 0), 0, 0));
			}
			settingsPanel.add(transitionSpeedPanel, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- transitionSpeedHelp ----
			transitionSpeedHelp.setIcon(new ImageIcon(getClass().getResource("/help.png")));
			transitionSpeedHelp.setToolTipText(bundle.getString("GuiLauncher.transitionSpeedHelp.toolTipText"));
			settingsPanel.add(transitionSpeedHelp, new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
		}
		contentPane.add(settingsPanel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(5, 5, 10, 5), 0, 0));
		
		//---- startButton ----
		startButton.setText(bundle.getString("GuiLauncher.startButton.text"));
		startButton.setFont(new Font("Segoe UI", Font.ITALIC, 16));
		startButton.setIcon(new ImageIcon(getClass().getResource("/music.png")));
		startButton.addActionListener(e -> startButtonPressed(e));
		contentPane.add(startButton, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
				new Insets(10, 0, 5, 0), 0, 0));
		
		//---- versionText ----
		versionText.setText(bundle.getString("GuiLauncher.versionText.text"));
		versionText.setHorizontalAlignment(SwingConstants.RIGHT);
		contentPane.add(versionText, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(0, 0, 10, 5), 0, 0));
		pack();
		setLocationRelativeTo(getOwner());
		
		//---- transitionSpeedButtonGroup ----
		var transitionSpeedButtonGroup = new ButtonGroup();
		transitionSpeedButtonGroup.add(transitionSpeedNoneButton);
		transitionSpeedButtonGroup.add(transitionSpeedSlowButton);
		transitionSpeedButtonGroup.add(transitionSpeedNormalButton);
		transitionSpeedButtonGroup.add(transitionSpeedFastButton);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}
	
	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JLabel logo;
	private JPanel configurationPanel;
	private JLabel midiFileLabel;
	private JTextField midiFilePathTextField;
	private JResizedIconButton loadMidiFileButton;
	private JLabel midiFileHelp;
	private JLabel midiDeviceLabel;
	private JComboBox<MidiDevice.Info> midiDeviceDropDown;
	private JLabel midiDeviceHelp;
	private JLabel soundFontLabel;
	private JComboBox<String> soundFontPathDropDown;
	private JResizedIconButton editSoundFontsButton;
	private JLabel soundFontHelp;
	private JPanel settingsPanel;
	private JPanel hSpacer1;
	private JLabel latencyFixLabel;
	private JSpinner latencySpinner;
	private JPanel hSpacer2;
	private JLabel latencyHelp;
	private JLabel displayLabel;
	private JCheckBox fullscreenCheckbox;
	private JLabel fullscreenHelp;
	private JLabel transitionSpeedLabel;
	private JPanel transitionSpeedPanel;
	private JRadioButton transitionSpeedNoneButton;
	private JRadioButton transitionSpeedSlowButton;
	private JRadioButton transitionSpeedNormalButton;
	private JRadioButton transitionSpeedFastButton;
	private JLabel transitionSpeedHelp;
	private JResizedIconButton startButton;
	private JLabel versionText;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
