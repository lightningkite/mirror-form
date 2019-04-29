package com.lightningkite.mirror.form.view

import com.lightningkite.kommon.collection.push
import com.lightningkite.koolui.builders.vertical
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.form.*
import com.lightningkite.mirror.form.info.humanify
import com.lightningkite.mirror.form.info.needsNoContext
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.reacktive.property.transform

class ReflectiveSummaryViewGenerator<T : Any, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        fieldCount: Int = 3,
        val request: DisplayRequest<T>
) : ViewGenerator<DEPENDENCY, VIEW> {

    val type = request.type as MirrorClass<T>
    val fields = type.pickDisplayFields<T>(request).take(fieldCount).map { field ->
        @Suppress("UNCHECKED_CAST")
        field to ViewEncoder.getViewGenerator<Any?, DEPENDENCY, VIEW>(
                request.child(field = field, observable = request.observable.transform { field.get(it) })
        )
    }.toList()

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        val v = frame(vertical {
            for ((field, generator) in fields) {
                if (field.needsNoContext) {
                    -generator.generate(dependency)
                } else {
                    -entryContext(label = field.name.humanify(), field = generator.generate(dependency))
                }
            }
        })
        if (request.clickable) {
            v.clickable {
                val displayValue = request.observable.value
                request.general.stack<DEPENDENCY, VIEW>().push(DisplayViewGenerator(
                        data = displayValue,
                        type = request.type,
                        generalRequest = request.general
                ))
            }
        } else v
    }
}