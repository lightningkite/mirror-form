package com.lightningkite.mirror.form.view

import com.lightningkite.kommon.collection.swap

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
import com.lightningkite.reacktive.list.ObservableList
import com.lightningkite.reacktive.property.ConstantObservableProperty
import com.lightningkite.reacktive.property.ObservableProperty
import com.lightningkite.reacktive.property.transform

class ListSummaryViewGenerator<T, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val value: ObservableList<T>,
        val lines: Int = 3,
        val viewGenerator: (ObservableProperty<T>) -> ViewGenerator<DEPENDENCY, VIEW>
) : ViewGenerator<DEPENDENCY, VIEW> {

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        vertical {
            for (line in 0 until lines) {
                -swap(value.onListUpdate.transform { it.getOrNull(0) }.transform {
                    (if (it == null) {
                        space(1f).margin(0f)
                    } else {
                        viewGenerator(ConstantObservableProperty(it)).generate(dependency)
                    }) to Animation.Flip
                })
            }
            -text(
                    text = value.onListUpdate.transform {
                        if (it.size > lines) "..." else ""
                    }
            )
        }
    }
}