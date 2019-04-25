package com.lightningkite.mirror.form.form

import com.lightningkite.kommon.collection.push
import com.lightningkite.kommon.collection.pushFrom
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.form.FormEncoder
import com.lightningkite.mirror.form.FormRequest
import com.lightningkite.mirror.form.FormState
import com.lightningkite.mirror.form.ViewSize
import com.lightningkite.reacktive.list.MutableObservableList
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.StandardObservableProperty

class CardFormViewGenerator<T, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val stack: MutableObservableList<ViewGenerator<DEPENDENCY, VIEW>>,
        val summaryVG: ViewGenerator<DEPENDENCY, VIEW>,
        val request: FormRequest<T>
) : ViewGenerator<DEPENDENCY, VIEW> {
    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        val embeddedOn = stack.last()
        card(summaryVG.generate(dependency)).clickable {
            val obs = StandardObservableProperty(request.observable.value)
            val vg = FormViewGenerator(
                    wraps = request.copy(scale = ViewSize.Full).getVG<DEPENDENCY, VIEW>(),
                    obs = obs,
                    onComplete = {
                        request.observable.value = obs.value
                    }
            )
            stack.pushFrom(embeddedOn, vg)
        }
    }
}