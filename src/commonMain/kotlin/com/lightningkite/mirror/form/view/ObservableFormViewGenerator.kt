package com.lightningkite.mirror.form

import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.StandardObservableProperty

class ObservableFormViewGenerator<T, in DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        request: Request<T>,
        default: T,
        val gen: DEPENDENCY.(MutableObservableProperty<T>) -> VIEW
) : FormEncoder.FormViewGenerator<T, DEPENDENCY, VIEW> {
    val observable = StandardObservableProperty(request.value ?: default)
    override val value: T by observable
    override fun generate(dependency: DEPENDENCY): VIEW = gen(dependency, observable)
}