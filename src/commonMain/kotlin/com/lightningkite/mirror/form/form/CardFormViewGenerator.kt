package com.lightningkite.mirror.form.form

import com.lightningkite.kommon.collection.push
import com.lightningkite.kommon.collection.pushFrom
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.form.FormEncoder
import com.lightningkite.mirror.form.FormState
import com.lightningkite.reacktive.list.MutableObservableList
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.StandardObservableProperty

class CardFormViewGenerator<T, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val stack: MutableObservableList<ViewGenerator<DEPENDENCY, VIEW>>,
        val summaryVG: ViewGenerator<DEPENDENCY, VIEW>,
        val fullVG: ()->ViewGenerator<DEPENDENCY, VIEW>
) : ViewGenerator<DEPENDENCY, VIEW> {
    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        card(summaryVG.generate(dependency)).clickable {
            stack.pushFrom(this@CardFormViewGenerator, fullVG())
        }
    }
}