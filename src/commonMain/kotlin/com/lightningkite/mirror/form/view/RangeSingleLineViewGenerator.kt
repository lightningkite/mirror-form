package com.lightningkite.mirror.form.view

import com.lightningkite.koolui.builders.horizontal
import com.lightningkite.koolui.builders.linear
import com.lightningkite.koolui.builders.text
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator

class RangeSingleLineViewGenerator<DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val subFirst: ViewGenerator<DEPENDENCY, VIEW>,
        val subSecond: ViewGenerator<DEPENDENCY, VIEW>
) : ViewGenerator<DEPENDENCY, VIEW> {
    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        horizontal {
            -subFirst.generate(dependency)
            -text("-")
            -subSecond.generate(dependency)
        }
    }
}