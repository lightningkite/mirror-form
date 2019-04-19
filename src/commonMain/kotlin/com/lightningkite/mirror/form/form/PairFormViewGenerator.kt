package com.lightningkite.mirror.form.form

import com.lightningkite.koolui.builders.linear
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.form.PartForm
import com.lightningkite.mirror.form.FormState
import com.lightningkite.reacktive.property.MutableObservableProperty

class PairFormViewGenerator<DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val form: Form<*, *>,
        val subFirst: ViewGenerator<DEPENDENCY, VIEW>,
        val subSecond: ViewGenerator<DEPENDENCY, VIEW>
) : ViewGenerator<DEPENDENCY, VIEW> {
    class Form<A, B>(main: MutableObservableProperty<FormState<Pair<A, B>>>) : PartForm<Pair<A, B>>(main) {
        val first = part("First") { it.first }
        val second = part("Second") { it.second }
        override fun make(): Pair<A, B> = Pair(first.successfulValue, second.successfulValue)
    }

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        frame(linear {
            -subFirst.generate(dependency)
            -subSecond.generate(dependency)
        }).apply { form.bind(lifecycle) }
    }
}