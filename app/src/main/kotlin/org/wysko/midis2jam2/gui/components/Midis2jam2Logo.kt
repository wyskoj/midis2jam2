package org.wysko.midis2jam2.gui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.midis2jam2_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun Midis2jam2Logo() {
    Image(painter = painterResource(Res.drawable.midis2jam2_logo), contentDescription = "")
}