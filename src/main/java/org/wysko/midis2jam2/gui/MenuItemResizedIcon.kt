package org.wysko.midis2jam2.gui

import java.awt.Image
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JMenuItem

class MenuItemResizedIcon : JMenuItem() {
	override fun setIcon(defaultIcon: Icon) {
		super.setIcon(ImageIcon((defaultIcon as ImageIcon).image.getScaledInstance(20, 20, Image.SCALE_SMOOTH)))
	}
}