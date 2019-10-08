package com.lightningkite.mirror.form.view

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
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.form.DisplayRequest
import com.lightningkite.mirror.form.ViewEncoder
import com.lightningkite.mirror.form.info.humanify
import com.lightningkite.mirror.form.info.needsNoContext
import com.lightningkite.mirror.form.pickDisplayFields
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.reacktive.property.transform

class ReflectiveViewGenerator<T : Any, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val request: DisplayRequest<T>
) : ViewGenerator<DEPENDENCY, VIEW> {

    val type = request.type as MirrorClass<T>
    val fields = type.pickDisplayFields(request).map { field ->
        @Suppress("UNCHECKED_CAST")
        field to ViewEncoder.getViewGenerator<Any?, DEPENDENCY, VIEW>(
                request.child(field = field, observable = request.observable.transform { field.get(it) })
        )
    }

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        scrollVertical(vertical {
            for ((field, generator) in fields) {
                if (field.needsNoContext) {
                    -generator.generate(dependency)
                } else {
                    -entryContext(label = field.name.humanify(), field = generator.generate(dependency))
                }

            }
        })
    }
}