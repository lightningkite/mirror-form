package com.lightningkite.mirror.form.form

import com.lightningkite.koolui.async.*
import com.lightningkite.koolui.canvas.*
import com.lightningkite.koolui.color.*
import com.lightningkite.koolui.concepts.*
import com.lightningkite.koolui.geometry.*
import com.lightningkite.koolui.image.*
import com.lightningkite.koolui.implementationhelpers.*
import com.lightningkite.koolui.layout.*
import com.lightningkite.koolui.notification.*
import com.lightningkite.koolui.preferences.*
import com.lightningkite.koolui.resources.*
import com.lightningkite.koolui.views.*
import com.lightningkite.koolui.views.basic.*
import com.lightningkite.koolui.views.dialogs.*
import com.lightningkite.koolui.views.graphics.*
import com.lightningkite.koolui.views.interactive.*
import com.lightningkite.koolui.views.layout.*
import com.lightningkite.koolui.views.navigation.*
import com.lightningkite.koolui.views.root.*
import com.lightningkite.koolui.views.web.*
import com.lightningkite.koolui.concepts.Importance
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.breaker.PartialBreaker
import com.lightningkite.mirror.form.*
import com.lightningkite.mirror.form.info.humanify
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.ObservableProperty
import com.lightningkite.reacktive.property.transform

class ReflectiveFormViewGenerator<T : Any, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val request: FormRequest<T>
) : ViewGenerator<DEPENDENCY, VIEW> {
    class ReflectiveForm<T : Any>(
            val request: FormRequest<T>,
            val type: MirrorClass<T> = (request.type as MirrorClass<T>),
            main: MutableObservableProperty<FormState<T>>
    ) : PartForm<T>(main) {
        val allParts = type.fields
                .associate {
                    it to part(it.name.humanify(), required = !it.optional) { owner -> it.get(owner) }
                }
        val displayedParts = allParts.filterKeys { it.includedInForm(request) }

        val implied = request.general.impliedFields.asSequence()
                .filter { it.key in type.fields }
                .map { it.key.index to it.value }
                .toList()

        override fun make(): T {
            return PartialBreaker.fold<T>(
                    type = request.type,
                    elements = allParts.asSequence()
                            .filter { !it.value.value.isEmpty }
                            .associate { it.key.index to it.value.successfulValue }
                            .plus(implied)
            )
        }
    }

    inner class Part(
            val field: MirrorClass.Field<T, *>,
            val part: PartForm<T>.PartProperty<*>,
            val vg: ViewGenerator<DEPENDENCY, VIEW>
    )

    val form = ReflectiveForm(request, request.type as MirrorClass<T>, request.observable)
    val formParts = form.displayedParts.entries.map {
        Part(
                field = it.key,
                part = it.value,
                vg = request.child(it.key, it.value).getVG<DEPENDENCY, VIEW>()
        )
    }

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        scrollVertical(vertical {
            for (p in formParts) {
                -entryContext(
                        label = p.field.name.humanify(),
                        feedback = (p.part).feedback(p.part.required),
                        field = p.vg.generate(dependency)
                )
            }
        })
    }
}

private fun ObservableProperty<out FormState<*>>.feedback(required: Boolean): ObservableProperty<Pair<Importance, String>?> = transform {
    if(it.isEmpty && required) Importance.Low to "This field is required"
    else if(it is FormState.Invalid) Importance.Danger to it.cause.toString()
    else null
}