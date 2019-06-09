package com.lightningkite.mirror.form.view

import com.lightningkite.koolui.builders.*
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator

class RangeViewGenerator<DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val subFirst: ViewGenerator<DEPENDENCY, VIEW>,
        val subSecond: ViewGenerator<DEPENDENCY, VIEW>
) : ViewGenerator<DEPENDENCY, VIEW> {
    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        frame(vertical {
            -subFirst.generate(dependency)
            -horizontal {
                -text("to")
                +subSecond.generate(dependency)
            }
        })
    }
}