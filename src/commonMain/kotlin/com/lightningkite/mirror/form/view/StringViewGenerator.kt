package com.lightningkite.mirror.form.view

import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.form.DisplayRequest
import com.lightningkite.reacktive.property.transform

class StringViewGenerator<T, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        private val request: DisplayRequest<T>,
        private val toString: (T) -> String?
) : ViewGenerator<DEPENDENCY, VIEW> {
    override fun generate(dependency: DEPENDENCY): VIEW {
        return dependency.text(
                text = request.observable.transform { it?.let(toString) ?: request.general.nullString },
                size = request.scale.textSize(),
                maxLines = request.scale.maxLines()
        )
    }
}