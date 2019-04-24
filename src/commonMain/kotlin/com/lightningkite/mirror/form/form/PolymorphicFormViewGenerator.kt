package com.lightningkite.mirror.form.form

import com.lightningkite.koolui.builders.space
import com.lightningkite.koolui.builders.text
import com.lightningkite.koolui.builders.vertical
import com.lightningkite.koolui.concepts.Animation
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.form.*
import com.lightningkite.mirror.info.MirrorRegistry
import com.lightningkite.mirror.info.MirrorType
import com.lightningkite.reacktive.list.asObservableList
import com.lightningkite.reacktive.property.transform
import kotlinx.serialization.UnionKind


class PolymorphicFormViewGenerator<T, DEPENDENCY: ViewFactory<VIEW>, VIEW>(private val request: FormRequest<T>) : ViewGenerator<DEPENDENCY, VIEW> {

    val nnOptions = MirrorRegistry.allSatisfying(request.type).filter { it.kind != UnionKind.POLYMORPHIC }
    val options = if (request.type.isNullable) listOf(null) + nnOptions.toList() else nnOptions.toList()

    val form = PolymorphicForm<T>(request.observable, request.type)

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        if(options.isEmpty()){
            text(text = "No available options")
        } else {
            vertical {
                -picker(
                        options = options.asObservableList(),
                        selected = form.type.observable.perfect(options.first()),
                        makeView = { itemObs ->
                            text(itemObs.transform { it?.localName?.humanify() ?: request.general.nullString })
                        }
                )
                -swap(form.type.observable.perfectNullable().transform { type ->
                    val vg = if (type == null) space(0f) else {
                        @Suppress("UNCHECKED_CAST")
                        request.sub(
                                type = type as MirrorType<T>,
                                observable = form.actualValue.observable.transform(
                                        mapper = {
                                            if (it is FormState.Success) {
                                                val nn: Any = it.value!!
                                                if (nn::class == type.kClass) {
                                                    it
                                                } else {
                                                    FormState.empty()
                                                }
                                            } else it
                                        },
                                        reverseMapper = { it }
                                ),
                                scale = request.scale
                        ).getVG<DEPENDENCY, VIEW>().generate(dependency)
                    }
                    vg to Animation.Flip
                })
            }.apply {
                form.bind(lifecycle)
            }
        }
    }
}