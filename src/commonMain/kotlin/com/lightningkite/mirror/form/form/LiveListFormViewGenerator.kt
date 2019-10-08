package com.lightningkite.mirror.form.form

import com.lightningkite.kommon.collection.push
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
import com.lightningkite.mirror.form.FormState
import com.lightningkite.reacktive.list.*
import com.lightningkite.reacktive.property.*

class LiveListFormViewGenerator<T, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val stack: MutableObservableList<ViewGenerator<DEPENDENCY, VIEW>>,
        val value: WrapperObservableList<FormState<T>>,
        val viewGenerator: (MutableObservableProperty<FormState<T>>) -> ViewGenerator<DEPENDENCY, VIEW>,
        val editViewGenerator: (MutableObservableProperty<FormState<T>>) -> ViewGenerator<DEPENDENCY, VIEW>
) : ViewGenerator<DEPENDENCY, VIEW> {

    var previousState: List<FormState<T>> = value.toList()
    val clipboard = StandardObservableProperty<FormState<T>>(FormState.empty())

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        vertical {
            -horizontal {
                +space(1f)
                -imageButton(
                        imageWithOptions = MaterialIcon.undo.color(dependency.colorSet.foreground).withOptions(scaleType = ImageScaleType.Crop),
                        label = "Undo",
                        onClick = {
                            previousState = value.toList()
                            value.replace(previousState)
                        }
                )
                -imageButton(
                        imageWithOptions = MaterialIcon.add.color(dependency.colorSet.foreground).withOptions(scaleType = ImageScaleType.Crop),
                        label = "Add",
                        onClick = {
                            val duplicate = StandardObservableProperty(FormState.empty<T>())
                            stack.push(FormViewGenerator(
                                    wraps = editViewGenerator(duplicate),
                                    obs = duplicate,
                                    onComplete = {
                                        previousState = value.toList()
                                        value.add(duplicate.value)
                                    }
                            ))
                        }
                )
            }
            +list(
                    data = value
            ) { itemObs, indexObs ->
                vertical {
                    val virtualEdit = VirtualMutableObservableProperty(
                            getterFun = { itemObs.value},
                            setterFun = { value[indexObs.value] = it },
                            event = itemObs
                    )
                    +viewGenerator(virtualEdit).generate(dependency)
                    -imageButton(
                            imageWithOptions = MaterialIcon.moreVert.color(dependency.colorSet.foreground).withOptions(scaleType = ImageScaleType.Crop),
                            label = "More",
                            onClick = {
                                launchSelector(options = listOf(
                                        "Cut" to { },
                                        "Copy" to { },
                                        "Insert Above" to { },
                                        "Insert Below" to { },
                                        "Paste Above" to { },
                                        "Paste Below" to { },
                                        "Delete" to { }
                                ))
                            }
                    )
                }
            }
        }

    }

    override fun generateActions(dependency: DEPENDENCY): VIEW? {
        return super.generateActions(dependency)
    }
}