package com.lightningkite.mirror.form.form

import com.lightningkite.koolui.builders.horizontal
import com.lightningkite.koolui.builders.imageButton
import com.lightningkite.koolui.builders.launchInfoDialog
import com.lightningkite.koolui.concepts.Importance
import com.lightningkite.koolui.image.MaterialIcon
import com.lightningkite.koolui.image.color
import com.lightningkite.koolui.image.withSizing
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.form.FormState
import com.lightningkite.mirror.form.GeneralRequest
import com.lightningkite.mirror.form.PartFormViewGenerator
import com.lightningkite.mirror.form.info.humanify
import com.lightningkite.mirror.info.MirrorType
import com.lightningkite.mirror.request.Request
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.ObservableProperty
import com.lightningkite.reacktive.property.StandardObservableProperty
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FormViewGenerator<T, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val wraps: ViewGenerator<DEPENDENCY, VIEW>,
        val obs: ObservableProperty<FormState<T>>,
        override val title: String = "",
        val onComplete: FormViewGenerator<T, DEPENDENCY, VIEW>.(T) -> Unit
) : ViewGenerator<DEPENDENCY, VIEW> by wraps {

    val working = StandardObservableProperty(false)

    override fun generateActions(dependency: DEPENDENCY): VIEW? = with(dependency) {
        horizontal {
            val sub = wraps.generateActions(dependency)
            if (sub != null) {
                -sub
            }
            -work(imageButton(
                    imageWithSizing = MaterialIcon.check.color(dependency.colorSet.foreground).withSizing(),
                    label = "Save",
                    importance = Importance.Low,
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
            ), working)
        }
    }
}

fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> FormViewGenerator(
        type: MirrorType<T>,
        startingWith: FormState<T> = FormState.empty(),
        generalRequest: GeneralRequest,// = GeneralRequest(),
        onComplete: FormViewGenerator<T, DEPENDENCY, VIEW>.(T) -> Unit
): FormViewGenerator<T, DEPENDENCY, VIEW> {
    val obs: MutableObservableProperty<FormState<T>> = StandardObservableProperty(startingWith)
    return FormViewGenerator(
            wraps = PartFormViewGenerator(obs, type, generalRequest = generalRequest),
            obs = obs,
            title = type.base.localName.humanify(),
            onComplete = onComplete
    )
}

fun <T : Request<OUT>, OUT, DEPENDENCY : ViewFactory<VIEW>, VIEW> DEPENDENCY.RequestViewGenerator(
        type: MirrorType<T>,
        startingWith: FormState<T> = FormState.empty(),
        generalRequest: GeneralRequest,// = GeneralRequest(),
        onComplete: FormViewGenerator<T, DEPENDENCY, VIEW>.(OUT) -> Unit
): FormViewGenerator<T, DEPENDENCY, VIEW> {
    return FormViewGenerator(
            type = type,
            startingWith = startingWith,
            generalRequest = generalRequest,
            onComplete = {
                GlobalScope.launch {
                    working.value = true
                    try {
                        val result = generalRequest.requestHandler!!.invoke(it)
                        onComplete(result)
                    } catch (e: Exception) {
                        launchInfoDialog(title = "Error", message = e.message ?: "An unknown error occurred")
                    }
                    working.value = false
                }
            }
    )
}