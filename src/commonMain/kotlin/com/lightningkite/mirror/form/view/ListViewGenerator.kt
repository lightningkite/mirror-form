package com.lightningkite.mirror.form.view

import com.lightningkite.koolui.concepts.Animation
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.form.DisplayRequest
import com.lightningkite.mirror.form.ViewEncoder
import com.lightningkite.mirror.info.ListMirror
import com.lightningkite.reacktive.list.ObservableList
import com.lightningkite.reacktive.list.asObservableList
import com.lightningkite.reacktive.property.ObservableProperty
import com.lightningkite.reacktive.property.transform

class ListViewGenerator<T, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val value: ObservableList<T>,
        val viewGenerator: (ObservableProperty<T>) -> ViewGenerator<DEPENDENCY, VIEW>
) : ViewGenerator<DEPENDENCY, VIEW> {

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        list(
                data = value
        ) { itemObs ->
            viewGenerator(itemObs).generate(dependency)
        }
    }
}