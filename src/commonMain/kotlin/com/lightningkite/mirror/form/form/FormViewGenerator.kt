package com.lightningkite.mirror.form.form

import com.lightningkite.koolui.builders.horizontal
import com.lightningkite.koolui.builders.imageButton
import com.lightningkite.koolui.builders.launchInfoDialog
import com.lightningkite.koolui.image.MaterialIcon
import com.lightningkite.koolui.image.color
import com.lightningkite.koolui.image.withSizing
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.form.FormState
import com.lightningkite.reacktive.property.ObservableProperty

class FormViewGenerator<T, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val wraps: ViewGenerator<DEPENDENCY, VIEW>,
        val obs: ObservableProperty<FormState<T>>,
        val onComplete: (T) -> Unit
) : ViewGenerator<DEPENDENCY, VIEW> by wraps {

    override fun generateActions(dependency: DEPENDENCY): VIEW? = with(dependency) {
        horizontal {
            val sub = wraps.generateActions(dependency)
            if (sub != null) {
                -sub
            }
            -imageButton(
                    imageWithSizing = MaterialIcon.check.color(dependency.colorSet.foreground).withSizing(),
                    label = "Save",
                    onClick = {
                        val result = obs.value
                        if (result is FormState.Success) {
                            onComplete(result.value)
                        } else if (result is FormState.Invalid) {
                            launchInfoDialog("Invalid", result.cause.toString())
                        } else {
                            //?
                        }
                    }
            )
        }
    }
}