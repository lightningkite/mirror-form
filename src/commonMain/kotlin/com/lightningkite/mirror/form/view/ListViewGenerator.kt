package com.lightningkite.mirror.form.view

import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.reacktive.list.ObservableList
import com.lightningkite.reacktive.property.ObservableProperty

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