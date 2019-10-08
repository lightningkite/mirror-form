package com.lightningkite.mirror.form.form


import com.lightningkite.koolui.async.*
import com.lightningkite.koolui.canvas.*
import com.lightningkite.koolui.color.*
import com.lightningkite.koolui.concepts.*
import com.lightningkite.koolui.geometry.*
import com.lightningkite.koolui.image.*
import com.lightningkite.koolui.implementationhelpers.*
import com.lightningkite.koolui.layout.*
import com.lightningkite.koolui.notification.*
import com.lightningkite.koolui.preferences.*
import com.lightningkite.koolui.resources.*
import com.lightningkite.koolui.views.*
import com.lightningkite.koolui.views.basic.*
import com.lightningkite.koolui.views.dialogs.*
import com.lightningkite.koolui.views.graphics.*
import com.lightningkite.koolui.views.interactive.*
import com.lightningkite.koolui.views.layout.*
import com.lightningkite.koolui.views.navigation.*
import com.lightningkite.koolui.views.root.*
import com.lightningkite.koolui.views.web.*
import com.lightningkite.koolui.*
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
        frame(vertical {
            -subFirst.generate(dependency)
            -horizontal {
                -space(16f)
                +subSecond.generate(dependency)
            }
        })
    }
}