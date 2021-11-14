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

package org.wysko.midis2jam2.gui;

import com.formdev.flatlaf.IntelliJTheme;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.install4j.api.launcher.SplashScreen;
import org.apache.commons.cli.*;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiFile;
import org.wysko.midis2jam2.starter.LegacyLiaison;
import org.wysko.midis2jam2.starter.Liaison;
import org.wysko.midis2jam2.util.InstrumentTransition;
import org.wysko.midis2jam2.util.M2J2Settings;
import org.wysko.midis2jam2.util.Utils;

import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.awt.Cursor.getPredefinedCursor;
import static java.util.Objects.requireNonNull;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.FILES_ONLY;
import static javax.swing.JOptionPane.*;

/**
 * @author Jacob Wysko
 */
@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class GuiLauncher extends JFrame {
	
	private static final Map<String, String> supportedLocales = new HashMap<>();
	
	static {
		supportedLocales.put("English", "en");
		supportedLocales.put("Fran\u00E7ais", "fr");
	}
	
	private static final File SETTINGS_FILE = new File(System.getProperty("user.home"), "midis2jam2.settings");
	
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	
	public static Map<String, String> getSupportedLocales() {
		return supportedLocales;
	}
	
	public LauncherSettings getSettings() {
		return settings;
	}
	
	private transient LauncherSettings settings;
	
	public GuiLauncher() {
		settings = new LauncherSettings();
	}
	
	@SuppressWarnings("java:S106")
	public static void main(String[] args) throws SplashScreen.ConnectionException, ParseException {
		
		// Register CLI arguments
		Options options = new Options();
		options.addOption("a", "headless", false, "Run in headless mode.");
		options.addOption("d", "device", true, "The device to use for MIDI playback.");
		options.addOption("e", "legacy-engine", false, "Use the legacy window engine.");
		options.addOption("f", "fullscreen", false, "Starts the application in fullscreen mode.");
		options.addOption("h", "help", false, "Prints a help message and exits.");
		options.addOption("l", "list-devices", false, "Lists the available MIDI devices and exits.");
		options.addOption("s", "soundfont", true, "Specifies the SoundFont to use for MIDI playback by a path to a " +
				"SoundFont file.");
		options.addOption("t", "transition-speed", true, "Specifies the transition speed.");
		options.addOption("v", "version", false, "Prints the version of this program and exits.");
		options.addOption("y", "latency", true, "Adjusts the audio for A/V sync.");
		
		// Initialize GUI
		SplashScreen.writeMessage("Loading...");
		IntelliJTheme.setup(GuiLauncher.class.getResourceAsStream("/Material Darker Contrast.theme.json"));
		
		
		// Parse CLI arguments
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		
		/* Print help message and exit if the user requested it. */
		if (cmd.hasOption("help")) {
			System.out.println(Utils.resourceToString("/man.txt"));
			return;
		}
		
		/* Print version and exit if the user requested it. */
		if (cmd.hasOption("version")) {
			System.out.println(Utils.resourceToString("/version.txt"));
			return;
		}
		
		/* List MIDI devices and exit if the user requested it. */
		if (cmd.hasOption("list-devices")) {
			final MidiDevice.Info[] midiDeviceInfo = MidiSystem.getMidiDeviceInfo();
			for (MidiDevice.Info info : midiDeviceInfo) {
				if (!"Real Time Sequencer".equals(info.getName())) {
					System.out.println(info.getName());
				}
			}
			return;
		}
		
		if (cmd.hasOption("headless")) {
			/* Ensure that the user has specified a MIDI file. */
			if (cmd.getArgs().length == 0) {
				System.err.println("No MIDI file specified.");
				return;
			}
			
			/* Ensure that the file exists. */
			File midiFile = new File(cmd.getArgs()[0]);
			if (!midiFile.exists()) {
				System.err.println("MIDI file does not exist.");
				return;
			}
			
			/* Ensure that the file is a valid MIDI file. */
			Sequence sequence;
			try {
				sequence = MidiSystem.getSequence(midiFile);
			} catch (InvalidMidiDataException e) {
				System.err.println("Invalid MIDI file.");
				System.err.println(Utils.exceptionToLines(e));
				return;
			} catch (IOException e) {
				System.err.println("Could not read MIDI file.");
				System.err.println(Utils.exceptionToLines(e));
				return;
			}
			
			/* Load the SoundFont, if specified */
			Soundbank soundFont = null;
			if (cmd.hasOption("soundfont")) {
				File soundFontFile = new File(cmd.getOptionValue("soundfont"));
				if (!soundFontFile.exists()) {
					System.err.println("SoundFont file does not exist.");
					return;
				}
				try {
					soundFont = MidiSystem.getSoundbank(soundFontFile);
				} catch (InvalidMidiDataException e) {
					System.err.println("Invalid SoundFont file.");
					System.err.println(Utils.exceptionToLines(e));
					return;
				} catch (IOException e) {
					System.err.println("Could not read SoundFont file.");
					System.err.println(Utils.exceptionToLines(e));
					return;
				}
			}
			
			/* Load the MIDI device, if specified */
			String deviceName = "Gervill";
			if (cmd.hasOption("device")) {
				deviceName = cmd.getOptionValue("device");
			}
			
			String finalDeviceName = deviceName;
			final Optional<MidiDevice.Info> first = Arrays.stream(MidiSystem.getMidiDeviceInfo()).filter(info -> info.getName().equals(finalDeviceName)).findFirst();
			if (!first.isPresent()) {
				System.err.println("Device not found.");
				return;
			}
			
			Sequencer sequencer;
			try {
				MidiDevice device = MidiSystem.getMidiDevice(first.get());
				if (deviceName.equals("Gervill")) { // Default device
					Synthesizer synthesizer = MidiSystem.getSynthesizer();
					synthesizer.open();
					if (soundFont != null) {
						sequencer = MidiSystem.getSequencer(false);
						sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
						synthesizer.loadAllInstruments(soundFont);
					} else {
						sequencer = MidiSystem.getSequencer(true);
					}
				} else { // External device
					device.open();
					sequencer = MidiSystem.getSequencer(false);
					sequencer.getTransmitter().setReceiver(device.getReceiver());
				}
				sequencer.open();
				sequencer.setSequence(sequence);
			} catch (MidiUnavailableException | InvalidMidiDataException e) {
				System.err.println("Could not open MIDI device.");
				System.err.println(Utils.exceptionToLines(e));
				return;
			}
			
			/* Set the latency, if specified */
			int latency = 0;
			if (cmd.hasOption("latency")) {
				try {
					latency = Integer.parseInt(cmd.getOptionValue("latency"));
				} catch (NumberFormatException e) {
					System.err.println("Invalid latency value.");
					System.err.println(Utils.exceptionToLines(e));
					return;
				}
			}
			
			/* Set other options */
			boolean fullscreen = cmd.hasOption("fullscreen");
			boolean legacyEngine = cmd.hasOption("legacy-engine");
			String transitionSpeed = "NORMAL";
			if (cmd.hasOption("transition-speed")) {
				transitionSpeed = cmd.getOptionValue("transition-speed");
				try {
					InstrumentTransition.valueOf(transitionSpeed.toUpperCase());
				} catch (IllegalArgumentException e) {
					System.err.println("Invalid transition speed.");
					System.err.println(Utils.exceptionToLines(e));
					return;
				}
			}
			
			
			try {
				if (legacyEngine) {
					LegacyLiaison liaison = new LegacyLiaison(null, sequencer, MidiFile.readMidiFile(midiFile), new M2J2Settings(latency,
							InstrumentTransition.valueOf(transitionSpeed.toUpperCase())),
							fullscreen);
					liaison.start();
				} else {
					Liaison liaison = new Liaison(null, sequencer, MidiFile.readMidiFile(midiFile), new M2J2Settings(latency,
							InstrumentTransition.valueOf(transitionSpeed.toUpperCase())),
							fullscreen);
					liaison.start(Midis2jam2Display.class);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InvalidMidiDataException e) {
				e.printStackTrace();
			}
			
			
		} else {
			GuiLauncher guiLauncher = new GuiLauncher();
			SplashScreen.writeMessage("Loading settings...");
			try {
				String json = Utils.fileToString(SETTINGS_FILE);
				if ("".equals(json.trim())) {
					throw new IllegalStateException();
				}
				guiLauncher.settings = new Gson().fromJson(json, LauncherSettings.class);
			} catch (Exception e) {
				Midis2jam2.getLOGGER().info("Could not load settings. Creating new settings.");
				guiLauncher.settings = new LauncherSettings();
				guiLauncher.saveSettings();
			}
			Locale.setDefault(new Locale(guiLauncher.settings.getLocale()));
			
			SplashScreen.writeMessage("Initializing launcher...");
			guiLauncher.initComponents();
			
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			guiLauncher.pack();
			guiLauncher.setLocation(dim.width / 2 - guiLauncher.getSize().width / 2, dim.height / 2 - guiLauncher.getSize().height / 2);
			guiLauncher.setVisible(true);
			guiLauncher.setDefaultCloseOperation(EXIT_ON_CLOSE);
			
			// Register drag and drop
			guiLauncher.midiFilePathTextField.setDropTarget(new DropTarget() {
				@SuppressWarnings("unchecked")
				@Override
				public synchronized void drop(DropTargetDropEvent evt) {
					try {
						evt.acceptDrop(DnDConstants.ACTION_COPY);
						List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
						guiLauncher.midiFilePathTextField.setText(droppedFiles.get(0).getAbsolutePath());
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			});
			
			SplashScreen.writeMessage("Collecting MIDI devices...");
			
			// Load MIDI devices
			MidiDevice.Info[] infoArr = MidiSystem.getMidiDeviceInfo();
			DefaultComboBoxModel<MidiDevice.Info> aModel = new DefaultComboBoxModel<>();
			
			// Populate MIDI devices (but don't add Real Time Sequencer)
			for (MidiDevice.Info info : infoArr) {
				if (!"Real Time Sequencer".equals(info.getName())) {
					aModel.addElement(info);
				}
			}
			guiLauncher.midiDeviceDropDown.setModel(aModel);
			
			// Set tooltip values
			ToolTipManager.sharedInstance().setInitialDelay(200);
			ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
			
			// Check for updates
			UpdateChecker.checkForUpdates(guiLauncher);
			
			// Bring GUI to front
			guiLauncher.bringToFront();
			
			// Load settings
			guiLauncher.reloadSettings();
			
			ResourceBundle.clearCache();
		}
		
	}
	
	/**
	 * Returns the current version of the program.
	 *
	 * @return the current version of the program
	 */
	public static String getVersion() {
		return Utils.resourceToString("/version.txt");
	}
	
	private void reloadSettings() {
		
		updateSf2List();
		
		// Set instrument transition
		for (Component component : transitionSpeedPanel.getComponents()) {
			((JRadioButton) component).setSelected(component.getName().equals(settings.getTransition().name()));
		}
		
		// Set MIDI device
		for (int i = 0; i < midiDeviceDropDown.getItemCount(); i++) {
			if (midiDeviceDropDown.getItemAt(i).getName().equals(settings.getMidiDevice())) {
				midiDeviceDropDown.setSelectedIndex(i);
				break;
			}
		}
		
		fullscreenCheckbox.setSelected(settings.isFullscreen());
		setLatencySpinnerFromDeviceDropdown();
		legacyEngineCheckbox.setSelected(settings.isLegacyDisplay());
		
		Locale.setDefault(new Locale(settings.getLocale()));
	}
	
	/**
	 * Saves settings, serializing with GSON then writing to the {@link #SETTINGS_FILE}.
	 */
	public void saveSettings() {
		try (FileWriter writer = new FileWriter(SETTINGS_FILE)) {
			writer.write(GSON.toJson(settings));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void updateSf2List() {
		soundFontPathDropDown.removeAllItems();
		List<String> soundFontPaths = settings.getSoundFontPaths();
		if (!soundFontPaths.isEmpty()) {
			soundFontPaths.forEach(e -> soundFontPathDropDown.addItem(e));
		} else {
			soundFontPathDropDown.addItem("Default SoundFont");
		}
		soundFontPathDropDown.revalidate();
		settings.setSoundFontPaths(settings.getSoundFontPaths());
		settings.setLastMidiDir(settings.getLastMidiDir());
		saveSettings();
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
	
	/**
	 * Prompts the user to load a MIDI file.
	 */
	private void loadMidiFileButtonActionPerformed(ActionEvent e) {
		JFileChooser f = new JFileChooser();
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
		f.getActionMap().get("viewTypeDetails").actionPerformed(null);
		f.setFileSelectionMode(FILES_ONLY);
		f.setCurrentDirectory(new File(settings.getLastMidiDir()));
		if (f.showDialog(this, "Load") == APPROVE_OPTION) {
			File selectedMidiFile = f.getSelectedFile();
			midiFilePathTextField.setText(selectedMidiFile.getAbsolutePath());
			settings.setLastMidiDir(selectedMidiFile.getParentFile().getAbsolutePath());
			saveSettings();
		}
	}
	
	/**
	 * Prompts the user to load a soundfont file.
	 */
	private void loadSoundFontButtonActionPerformed(ActionEvent e) {
		
		JDialog dialog = new JDialog(this, "SoundFont list editor", true);
		dialog.getContentPane().add(new SoundFontList(this.settings.getSoundFontPaths(), this));
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		dialog.setLocation(dim.width / 2 - getSize().width / 2,
				dim.height / 2 - getSize().height / 2);
		dialog.setPreferredSize(new Dimension(600, 400));
		dialog.pack();
		dialog.setVisible(true);
	}
	
	private void midiDeviceDropDownActionPerformed(ActionEvent e) {
		MidiDevice.Info info = (MidiDevice.Info) requireNonNull(midiDeviceDropDown.getSelectedItem());
		if ("Gervill".equals(info.getName())) {
			soundFontLabel.setEnabled(true);
			soundFontPathDropDown.setEnabled(true);
			editSoundFontsButton.setEnabled(true);
		} else {
			soundFontLabel.setEnabled(false);
			soundFontPathDropDown.setEnabled(false);
			editSoundFontsButton.setEnabled(false);
		}
		latencySpinner.setValue(settings.getLatencyForDevice(info.getName()));
		settings.setMidiDevice(info.getName());
		saveSettings();
	}
	
	private void startButtonPressed(ActionEvent e) {
		// Collect MIDI file
		this.setCursor(getPredefinedCursor(Cursor.WAIT_CURSOR));
		if ("".equals(midiFilePathTextField.getText().trim())) {
			this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			showMessageDialog(this, "You must specify a MIDI file.", "No MIDI file selected", INFORMATION_MESSAGE);
			return;
		}
		File midiFile = new File(midiFilePathTextField.getText());
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
		
		
		// Initialize MIDI
		try {
			final MidiDevice.Info selectedDevice = requireNonNull((MidiDevice.Info) midiDeviceDropDown.getSelectedItem());
			Sequencer sequencer;
			
			MidiDevice midiDevice = MidiSystem.getMidiDevice(selectedDevice);
			
			if ("Gervill".equals(selectedDevice.getName())) {
				// Internal synth
				Synthesizer synthesizer = MidiSystem.getSynthesizer();
				synthesizer.open();
				
				// Collect sf2
				Soundbank soundfont = null;
				String selectedSf2Path = (String) soundFontPathDropDown.getSelectedItem();
				if ("Default SoundFont".equals(selectedSf2Path)) {
					selectedSf2Path = null;
				}
				if (selectedSf2Path != null) {
					File soundFontFile = new File(selectedSf2Path);
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
			
			int value = (int) latencySpinner.getValue();
			if (midiDevice.getDeviceInfo().getName().startsWith("VirtualMIDISynth")) {
				File vmsConfigFile = new File("C:\\Program Files\\VirtualMIDISynth\\VirtualMIDISynth.conf");
				if (vmsConfigFile.exists()) {
					String vmsConfigData = Utils.fileToString(vmsConfigFile);
					Matcher matcher = Pattern.compile("AdditionalBuffer=(\\d+)").matcher(vmsConfigData);
					if (matcher.find()) {
						String additionalBuffer = matcher.group(1);
						value += Integer.parseInt(additionalBuffer);
					} else {
						value += 250;
					}
				}
				
			}
			if (legacyEngineCheckbox.isSelected()) {
				LegacyLiaison liaison = new LegacyLiaison(this, sequencer, MidiFile.readMidiFile(midiFile),
						new M2J2Settings(value,
								InstrumentTransition.valueOf(getSelectedTransitionRadioButton().getName())),
						fullscreenCheckbox.isSelected());
				this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				SwingUtilities.invokeLater(() -> new Thread(liaison::start).start());
			} else {
				Liaison liaison = new Liaison(this, sequencer, MidiFile.readMidiFile(midiFile), new M2J2Settings(value,
						InstrumentTransition.valueOf(getSelectedTransitionRadioButton().getName())),
						fullscreenCheckbox.isSelected());
				this.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				SwingUtilities.invokeLater(() -> new Thread(() -> liaison.start(Midis2jam2Display.class)).start());
			}
			
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
	
	private void transitionSpeedNoneButtonActionPerformed(ActionEvent e) {
		settings.setTransition(InstrumentTransition.valueOf(getSelectedTransitionRadioButton().getName()));
		saveSettings();
	}
	
	private void fullscreenCheckboxActionPerformed(ActionEvent e) {
		settings.setFullscreen(fullscreenCheckbox.isSelected());
		saveSettings();
	}
	
	private void latencySpinnerStateChanged(ChangeEvent e) {
		setLatencySpinnerFromDeviceDropdown();
		saveSettings();
	}
	
	private void setLatencySpinnerFromDeviceDropdown() {
		settings.setLatencyForDevice(
				((MidiDevice.Info) requireNonNull(midiDeviceDropDown.getSelectedItem())).getName(),
				(int) latencySpinner.getValue()
		);
	}
	
	private void exitMenuItemActionPerformed(ActionEvent e) {
		System.exit(0);
	}
	
	private void aboutMenuItemActionPerformed(ActionEvent e) {
		About about = new About(this, true);
		about.setVisible(true);
	}
	
	private void legacyEngineCheckboxActionPerformed(ActionEvent e) {
		settings.setLegacyDisplay(legacyEngineCheckbox.isSelected());
		saveSettings();
	}
	
	private void localeMenuItemActionPerformed(ActionEvent e) {
		LocaleSelect localeSelect = new LocaleSelect(this);
		localeSelect.setVisible(true);
	}
	
	@SuppressWarnings("all")
	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		ResourceBundle bundle = ResourceBundle.getBundle("i18n.guilauncher");
		menuBar1 = new JMenuBar();
		fileMenu = new JMenu();
		openMidiFileMenuItem = new MenuItemResizedIcon();
		editSoundFontsMenuItem = new MenuItemResizedIcon();
		exitMenuItem = new MenuItemResizedIcon();
		menu1 = new JMenu();
		localeMenuItem = new MenuItemResizedIcon();
		aboutMenuItem = new JMenuItem();
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
		legacyEngineCheckbox = new JCheckBox();
		legacyEngineHelp = new JLabel();
		transitionSpeedLabel = new JLabel();
		transitionSpeedPanel = new JPanel();
		transitionSpeedNoneButton = new JRadioButton();
		transitionSpeedSlowButton = new JRadioButton();
		transitionSpeedNormalButton = new JRadioButton();
		transitionSpeedFastButton = new JRadioButton();
		transitionSpeedHelp = new JLabel();
		startButton = new JResizedIconButton();
		
		//======== this ========
		setTitle(bundle.getString("GuiLauncher.this.title"));
		setIconImage(new ImageIcon(getClass().getResource("/ico/icon48.png")).getImage());
		setResizable(false);
		setMinimumSize(new Dimension(605, 575));
		Container contentPane = getContentPane();
		contentPane.setLayout(new GridBagLayout());
		((GridBagLayout) contentPane.getLayout()).columnWidths = new int[]{0, 0};
		((GridBagLayout) contentPane.getLayout()).rowHeights = new int[]{132, 145, 77, 0, 0};
		((GridBagLayout) contentPane.getLayout()).columnWeights = new double[]{1.0, 1.0E-4};
		((GridBagLayout) contentPane.getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 1.0, 1.0E-4};
		
		//======== menuBar1 ========
		{
			
			//======== fileMenu ========
			{
				fileMenu.setText(bundle.getString("GuiLauncher.fileMenu.text"));
				
				//---- openMidiFileMenuItem ----
				openMidiFileMenuItem.setText(bundle.getString("GuiLauncher.openMidiFileMenuItem.text"));
				openMidiFileMenuItem.setIcon(new ImageIcon(getClass().getResource("/open.png")));
				openMidiFileMenuItem.addActionListener(e -> loadMidiFileButtonActionPerformed(e));
				fileMenu.add(openMidiFileMenuItem);
				
				//---- editSoundFontsMenuItem ----
				editSoundFontsMenuItem.setText(bundle.getString("GuiLauncher.editSoundFontsMenuItem.text"));
				editSoundFontsMenuItem.setIcon(new ImageIcon(getClass().getResource("/soundfont.png")));
				editSoundFontsMenuItem.addActionListener(e -> loadSoundFontButtonActionPerformed(e));
				fileMenu.add(editSoundFontsMenuItem);
				fileMenu.addSeparator();
				
				//---- exitMenuItem ----
				exitMenuItem.setText(bundle.getString("GuiLauncher.exitMenuItem.text"));
				exitMenuItem.setIcon(new ImageIcon(getClass().getResource("/exit.png")));
				exitMenuItem.addActionListener(e -> exitMenuItemActionPerformed(e));
				fileMenu.add(exitMenuItem);
			}
			menuBar1.add(fileMenu);
			
			//======== menu1 ========
			{
				menu1.setText(bundle.getString("GuiLauncher.menu1.text"));
				
				//---- localeMenuItem ----
				localeMenuItem.setText(bundle.getString("GuiLauncher.localeMenuItem.text"));
				localeMenuItem.setIcon(new ImageIcon(getClass().getResource("/locale.png")));
				localeMenuItem.addActionListener(e -> localeMenuItemActionPerformed(e));
				menu1.add(localeMenuItem);
				
				//---- aboutMenuItem ----
				aboutMenuItem.setText(bundle.getString("GuiLauncher.aboutMenuItem.text"));
				aboutMenuItem.setIcon(new ImageIcon(getClass().getResource("/help.png")));
				aboutMenuItem.addActionListener(e -> aboutMenuItemActionPerformed(e));
				menu1.add(aboutMenuItem);
			}
			menuBar1.add(menu1);
		}
		setJMenuBar(menuBar1);
		
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
			((GridBagLayout) configurationPanel.getLayout()).rowHeights = new int[]{35, 35, 9, 0, 0};
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
			((GridBagLayout) settingsPanel.getLayout()).rowHeights = new int[]{0, 0, 0, 0, 6, 0};
			((GridBagLayout) settingsPanel.getLayout()).columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
			((GridBagLayout) settingsPanel.getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
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
			latencySpinner.addChangeListener(e -> latencySpinnerStateChanged(e));
			settingsPanel.add(latencySpinner, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
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
			fullscreenCheckbox.addActionListener(e -> fullscreenCheckboxActionPerformed(e));
			settingsPanel.add(fullscreenCheckbox, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- fullscreenHelp ----
			fullscreenHelp.setIcon(new ImageIcon(getClass().getResource("/help.png")));
			fullscreenHelp.setToolTipText(bundle.getString("GuiLauncher.fullscreenHelp.toolTipText"));
			settingsPanel.add(fullscreenHelp, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- legacyEngineCheckbox ----
			legacyEngineCheckbox.setText(bundle.getString("GuiLauncher.legacyEngineCheckbox.text"));
			legacyEngineCheckbox.addActionListener(e -> legacyEngineCheckboxActionPerformed(e));
			settingsPanel.add(legacyEngineCheckbox, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- legacyEngineHelp ----
			legacyEngineHelp.setIcon(new ImageIcon(getClass().getResource("/help.png")));
			legacyEngineHelp.setToolTipText(bundle.getString("GuiLauncher.legacyEngineHelp"));
			settingsPanel.add(legacyEngineHelp, new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- transitionSpeedLabel ----
			transitionSpeedLabel.setText(bundle.getString("GuiLauncher.transitionSpeedLabel.text"));
			settingsPanel.add(transitionSpeedLabel, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
					GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
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
				transitionSpeedNoneButton.addActionListener(e -> transitionSpeedNoneButtonActionPerformed(e));
				transitionSpeedPanel.add(transitionSpeedNoneButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 5, 5), 0, 0));
				
				//---- transitionSpeedSlowButton ----
				transitionSpeedSlowButton.setText(bundle.getString("GuiLauncher.transitionSpeedSlowButton.text"));
				transitionSpeedSlowButton.setName("SLOW");
				transitionSpeedSlowButton.addActionListener(e -> transitionSpeedNoneButtonActionPerformed(e));
				transitionSpeedPanel.add(transitionSpeedSlowButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 5, 0), 0, 0));
				
				//---- transitionSpeedNormalButton ----
				transitionSpeedNormalButton.setText(bundle.getString("GuiLauncher.transitionSpeedNormalButton.text"));
				transitionSpeedNormalButton.setSelected(true);
				transitionSpeedNormalButton.setName("NORMAL");
				transitionSpeedNormalButton.addActionListener(e -> transitionSpeedNoneButtonActionPerformed(e));
				transitionSpeedPanel.add(transitionSpeedNormalButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 0, 5), 0, 0));
				
				//---- transitionSpeedFastButton ----
				transitionSpeedFastButton.setText(bundle.getString("GuiLauncher.transitionSpeedFastButton.text"));
				transitionSpeedFastButton.setName("FAST");
				transitionSpeedFastButton.addActionListener(e -> transitionSpeedNoneButtonActionPerformed(e));
				transitionSpeedPanel.add(transitionSpeedFastButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 0, 0), 0, 0));
			}
			settingsPanel.add(transitionSpeedPanel, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
					new Insets(0, 0, 5, 5), 0, 0));
			
			//---- transitionSpeedHelp ----
			transitionSpeedHelp.setIcon(new ImageIcon(getClass().getResource("/help.png")));
			transitionSpeedHelp.setToolTipText(bundle.getString("GuiLauncher.transitionSpeedHelp.toolTipText"));
			settingsPanel.add(transitionSpeedHelp, new GridBagConstraints(4, 3, 1, 1, 0.0, 0.0,
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
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(10, 0, 0, 0), 0, 0));
		pack();
		setLocationRelativeTo(getOwner());
		
		//---- transitionSpeedButtonGroup ----
		ButtonGroup transitionSpeedButtonGroup = new ButtonGroup();
		transitionSpeedButtonGroup.add(transitionSpeedNoneButton);
		transitionSpeedButtonGroup.add(transitionSpeedSlowButton);
		transitionSpeedButtonGroup.add(transitionSpeedNormalButton);
		transitionSpeedButtonGroup.add(transitionSpeedFastButton);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}
	
	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JMenuBar menuBar1;
	
	private JMenu fileMenu;
	
	private MenuItemResizedIcon openMidiFileMenuItem;
	
	private MenuItemResizedIcon editSoundFontsMenuItem;
	
	private MenuItemResizedIcon exitMenuItem;
	
	private JMenu menu1;
	
	private MenuItemResizedIcon localeMenuItem;
	
	private JMenuItem aboutMenuItem;
	
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
	
	private JCheckBox legacyEngineCheckbox;
	
	private JLabel legacyEngineHelp;
	
	private JLabel transitionSpeedLabel;
	
	private JPanel transitionSpeedPanel;
	
	private JRadioButton transitionSpeedNoneButton;
	
	private JRadioButton transitionSpeedSlowButton;
	
	private JRadioButton transitionSpeedNormalButton;
	
	private JRadioButton transitionSpeedFastButton;
	
	private JLabel transitionSpeedHelp;
	
	private JResizedIconButton startButton;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
