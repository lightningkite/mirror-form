package com.lightningkite.mirror.form.view

import com.lightningkite.kommon.collection.swap
import com.lightningkite.koolui.builders.space
import com.lightningkite.koolui.builders.vertical
import com.lightningkite.koolui.concepts.Animation
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.reacktive.list.ObservableList
import com.lightningkite.reacktive.property.ConstantObservableProperty
import com.lightningkite.reacktive.property.ObservableProperty
import com.lightningkite.reacktive.property.transform

class ListSummaryViewGenerator<T, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val value: ObservableList<T>,
        val lines: Int = 3,
        val viewGenerator: (ObservableProperty<T>) -> ViewGenerator<DEPENDENCY, VIEW>
) : ViewGenerator<DEPENDENCY, VIEW> {

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        vertical {
            for (line in 0 until lines) {
                -swap(value.onListUpdate.transform { it.getOrNull(0) }.transform {
                    (if (it == null) {
                        space(1f).margin(0f)
                    } else {
                        viewGenerator(ConstantObservableProperty(it)).generate(dependency)
                    }) to Animation.Flip
                })
            }
            -text(
                    text = value.onListUpdate.transform {
                        if (it.size > lines) "..." else ""
                    }
            )
        }
    }
}