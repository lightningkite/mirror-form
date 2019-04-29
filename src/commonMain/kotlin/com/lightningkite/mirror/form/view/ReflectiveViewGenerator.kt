package com.lightningkite.mirror.form.view

import com.lightningkite.koolui.builders.vertical
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
        frame(vertical {
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