package com.lightningkite.mirror.form

import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.info.Type
import com.lightningkite.reacktive.list.MutableObservableList

class DisplayVG<VIEW, V>(
        val formEncoder: ViewEncoder,
        val stack: MutableObservableList<ViewGenerator<ViewFactory<VIEW>, VIEW>>,
        val value: V,
        val type: Type<V>
) : ViewGenerator<ViewFactory<VIEW>, VIEW> {

    override val title: String = formEncoder.registry.kClassToExternalNameRegistry[type.kClass]!!.humanify()

    override fun generate(dependency: ViewFactory<VIEW>): VIEW = formEncoder.build(
            factory = dependency,
            stack = stack,
            type = type,
            value = value
    )
}