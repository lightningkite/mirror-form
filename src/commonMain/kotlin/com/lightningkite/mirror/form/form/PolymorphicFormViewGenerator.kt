package com.lightningkite.mirror.form.form

import com.lightningkite.koolui.builders.space
import com.lightningkite.koolui.builders.text
import com.lightningkite.koolui.builders.vertical
import com.lightningkite.koolui.concepts.Animation
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.form.*
import com.lightningkite.mirror.form.info.humanify
import com.lightningkite.mirror.info.MirrorRegistry
import com.lightningkite.mirror.info.MirrorType
import com.lightningkite.reacktive.list.asObservableList
import com.lightningkite.reacktive.property.transform
import kotlinx.serialization.UnionKind


class PolymorphicFormViewGenerator<T, DEPENDENCY : ViewFactory<VIEW>, VIEW>(private val request: FormRequest<T>) : ViewGenerator<DEPENDENCY, VIEW> {

    val nnOptions = MirrorRegistry.allSatisfying(request.type).filter { it.kind != UnionKind.POLYMORPHIC }
    val options = if (request.type.isNullable) listOf(null) + nnOptions.toList() else nnOptions.toList()

    val form = PolymorphicForm<T>(request.observable, request.type)

    var previousType: MirrorType<*>? = null
    var previousVg: ViewGenerator<DEPENDENCY, VIEW> = ViewGenerator.empty()
    val subVg = form.type.perfectNullable().transform { _ ->
        val type = form.type.value.valueOrNull
        if(previousType == type){
            return@transform previousVg
        }
        previousType = type
        previousVg = if (type == null) ViewGenerator.empty() else {
            @Suppress("UNCHECKED_CAST")
            request.sub(
                    type = type as MirrorType<T>,
                    observable = form.actualValue.transform(
                            mapper = {
                                if (it is FormState.Success) {
                                    val nn: Any = it.value!!
                                    if (type.kClass.isInstance(nn)) {
                                        it
                                    } else {
                                        FormState.empty()
                                    }
                                } else it
                            },
                            reverseMapper = { it }
                    ),
                    scale = request.scale
            ).getVG<DEPENDENCY, VIEW>()
        }
        previousVg
    }

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        if (options.isEmpty()) {
            text(text = "No available options")
        } else {
            vertical {
                -picker(
                        options = options.asObservableList(),
                        selected = form.type.perfect(options.first()),
                        toString = { it?.localName?.humanify() ?: request.general.nullString }
                )
                val view = swap(subVg.transform { it.generate(dependency) to Animation.Flip })
                if (request.scale >= ViewSize.Full) +view else -view
            }
        }
    }

    override fun generateActions(dependency: DEPENDENCY): VIEW? = if(request.scale == ViewSize.Full) with(dependency) {
        swap(subVg.transform { (it.generateActions(dependency) ?: dependency.space(0f)) to Animation.Flip })
    } else null
}