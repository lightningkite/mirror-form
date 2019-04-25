package com.lightningkite.mirror.form.view

import com.lightningkite.koolui.builders.horizontal
import com.lightningkite.koolui.builders.linear
import com.lightningkite.koolui.builders.space
import com.lightningkite.koolui.builders.vertical
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator

class PairViewGenerator<DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val subFirst: ViewGenerator<DEPENDENCY, VIEW>,
        val subSecond: ViewGenerator<DEPENDENCY, VIEW>
) : ViewGenerator<DEPENDENCY, VIEW> {
    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        frame(vertical {
            -subFirst.generate(dependency)
            -horizontal {
                -space(16f)
                +subSecond.generate(dependency)
            }
        })
    }
}