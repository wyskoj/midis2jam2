package org.wysko.midis2jam2.gui

import java.awt.Image
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JButton

/** A button that resizes its icon to fit the button size. */
class JResizedIconButton : JButton() {
    /** Sets the icon of the button to a given [icon]. */
    override fun setIcon(icon: Icon) {
        super.setIcon(ImageIcon((icon as ImageIcon).image.getScaledInstance(20, 20, Image.SCALE_SMOOTH)))
    }
}