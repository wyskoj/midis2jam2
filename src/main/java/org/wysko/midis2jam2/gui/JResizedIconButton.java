package org.wysko.midis2jam2.gui;

import javax.swing.*;
import java.awt.*;

public class JResizedIconButton extends JButton {
	
	@Override
	public void setIcon(Icon defaultIcon) {
		super.setIcon(new ImageIcon(((ImageIcon) defaultIcon).getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
	}
}
