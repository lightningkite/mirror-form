package com.lightningkite.mirror.form.form

import com.lightningkite.kommon.collection.pop
import com.lightningkite.kommon.collection.push
import com.lightningkite.koolui.builders.horizontal
import com.lightningkite.koolui.builders.imageButton
import com.lightningkite.koolui.builders.space
import com.lightningkite.koolui.builders.vertical
import com.lightningkite.koolui.concepts.Animation
import com.lightningkite.koolui.concepts.Importance
import com.lightningkite.koolui.image.ImageScaleType
import com.lightningkite.koolui.image.MaterialIcon
import com.lightningkite.koolui.image.color
import com.lightningkite.koolui.image.withSizing
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.form.DisplayRequest
import com.lightningkite.mirror.form.FormState
import com.lightningkite.mirror.form.ViewEncoder
import com.lightningkite.mirror.info.ListMirror
import com.lightningkite.reacktive.list.*
import com.lightningkite.reacktive.property.*

class ListFormViewGenerator<T, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val stack: MutableObservableList<ViewGenerator<DEPENDENCY, VIEW>>,
        val value: MutableObservableList<T>,
        val makeView: DEPENDENCY.(ObservableProperty<T>) -> VIEW,
        val editViewGenerator: (start: T?, onResult: (T) -> Unit) -> ViewGenerator<DEPENDENCY, VIEW>
) : ViewGenerator<DEPENDENCY, VIEW> {

    var previousState: List<T> = value.toList()
    val clipboard = StandardObservableProperty<T?>(null)

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        vertical {
            +list(
                    data = value
            ) { itemObs, indexObs ->
                horizontal {
                    val virtualEdit = VirtualMutableObservableProperty(
                            getterFun = { itemObs.value },
                            setterFun = { value[indexObs.value] = it },
                            event = itemObs
                    )
                    +makeView(dependency, virtualEdit)
                    -imageButton(
                            imageWithSizing = MaterialIcon.moreVert.color(dependency.colorSet.foreground).withSizing(scaleType = ImageScaleType.Crop),
                            label = "More",
                            importance = Importance.Low,
                            onClick = {
                                launchSelector(options = listOf(
                                        "Edit" to {
                                            val index = indexObs.value
                                            stack.push(editViewGenerator(itemObs.value) {
                                                stack.pop()
                                                previousState = value.toList()
                                                value[index] = it
                                            })
                                            Unit
                                        },
                                        "Cut" to {
                                            previousState = value.toList()
                                            clipboard.value = value.removeAt(indexObs.value)
                                            Unit
                                        },
                                        "Copy" to {
                                            clipboard.value = value[indexObs.value]
                                            Unit
                                        },
                                        "Paste Above" to label@{
                                            val v = clipboard.value ?: return@label
                                            previousState = value.toList()
                                            value.add(indexObs.value, v)
                                            Unit
                                        },
                                        "Paste Below" to label@{
                                            val v = clipboard.value ?: return@label
                                            previousState = value.toList()
                                            value.add(indexObs.value + 1, v)
                                            Unit
                                        },
                                        "Insert Above" to {
                                            stack.push(editViewGenerator(null) {
                                                previousState = value.toList()
                                                value.add(indexObs.value, it)
                                            })
                                            Unit
                                        },
                                        "Insert Below" to {
                                            stack.push(editViewGenerator(null) {
                                                previousState = value.toList()
                                                value.add(indexObs.value + 1, it)
                                            })
                                            Unit
                                        },
                                        "Delete" to {
                                            previousState = value.toList()
                                            value.removeAt(indexObs.value)
                                            Unit
                                        }
                                ))
                            }
                    )
                }
            }
        }

    }

    override fun generateActions(dependency: DEPENDENCY): VIEW? = with(dependency){
        horizontal {
            +space(1f)
            -imageButton(
                    imageWithSizing = MaterialIcon.undo.color(dependency.colorSet.foreground).withSizing(scaleType = ImageScaleType.Crop),
                    label = "Undo",
                    importance = Importance.Low,
                    onClick = {
                        val setTo = previousState
                        previousState = value.toList()
                        value.replace(setTo)
                    }
            )
            -imageButton(
                    imageWithSizing = MaterialIcon.add.color(dependency.colorSet.foreground).withSizing(scaleType = ImageScaleType.Crop),
                    label = "Add",
                    importance = Importance.Low,
                    onClick = {
                        stack.push(editViewGenerator(null) {
                            stack.pop()
                            previousState = value.toList()
                            value.add(it)
                        })
                    }
            )
        }
    }
}