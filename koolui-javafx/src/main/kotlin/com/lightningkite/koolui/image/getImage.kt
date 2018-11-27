package com.lightningkite.koolui.image

import com.lightningkite.koolui.resources.Resources
import javafx.scene.image.Image
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

actual suspend fun Resources.getImage(filename: String): Displayable = GlobalScope.async {
    val result = Image(Resources.getByteArray(filename).inputStream())
    Displayable { _, _ -> result }
}.await()
