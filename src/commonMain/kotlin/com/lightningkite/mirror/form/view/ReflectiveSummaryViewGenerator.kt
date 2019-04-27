package com.lightningkite.mirror.form.view

import com.lightningkite.kommon.collection.push
import com.lightningkite.koolui.builders.vertical
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.form.DisplayRequest
import com.lightningkite.mirror.form.DisplayViewGenerator
import com.lightningkite.mirror.form.ViewEncoder
import com.lightningkite.mirror.form.humanify
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.reacktive.property.transform

class ReflectiveSummaryViewGenerator<T : Any, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        fieldCount: Int = 3,
        val request: DisplayRequest<T>
) : ViewGenerator<DEPENDENCY, VIEW> {

    val fields = request.type.base.fields.asSequence().take(fieldCount).map { field ->
        val castedField = field as MirrorClass.Field<T, *>
        @Suppress("UNCHECKED_CAST")
        castedField to ViewEncoder.getViewGenerator<Any?, DEPENDENCY, VIEW>(
                request.child(field = field, observable = request.observable.transform { castedField.get(it) })
        )
    }.toList()

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        val v = frame(vertical {
            for ((field, generator) in fields) {
                -entryContext(label = field.name.humanify(), field = generator.generate(dependency))
            }
        })
        if(request.clickable){
            v.clickable {
                val displayValue = request.observable.value
                request.general.stack<DEPENDENCY, VIEW>().push(DisplayViewGenerator(
                        data = displayValue,
                        type = request.type
                ))
            }
        } else v
    }
}