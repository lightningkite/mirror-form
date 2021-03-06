package com.lightningkite.mirror.form.form

import com.lightningkite.kommon.collection.pop
import com.lightningkite.kommon.collection.push
import com.lightningkite.kommon.exception.stackTraceString
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
import com.lightningkite.reacktive.list.*
import com.lightningkite.reacktive.property.*
import com.lightningkite.reacktive.property.lifecycle.bind

class ListFormViewGenerator<T, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val stack: MutableObservableList<ViewGenerator<DEPENDENCY, VIEW>>,
        val value: MutableObservableList<T>,
        val makeView: DEPENDENCY.(ObservableProperty<T>) -> VIEW,
        val editViewGenerator: (start: T?, onResult: (T) -> Unit) -> ViewGenerator<DEPENDENCY, VIEW>
) : ViewGenerator<DEPENDENCY, VIEW> {

    init {
        println("Created at:")
        println(Exception().stackTraceString())
    }

    var previousState: List<T> = value.toList()
    val clipboard = StandardObservableProperty<T?>(null)

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        list(
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
                        imageWithOptions = MaterialIcon.moreVert.color(dependency.colorSet.foreground).withOptions(scaleType = ImageScaleType.Crop),
                        label = "More",
                        importance = Importance.Low,
                        onClick = {
                            launchSelector(options = listOf(
                                    "Edit" to {
                                        val index = indexObs.value
                                        stack.push(editViewGenerator(itemObs.value) {
                                            previousState = value.toList()
                                            value[index] = it
                                            stack.pop()
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
        }.apply {
            if(value is MutableObservableListFromProperty<*>){
                lifecycle.bind(value.property){
                    println("--x--")
                    value.property.debugPrint()
                }
            } else {
                println("value is ${value}")
            }
        }


    }

    override fun generateActions(dependency: DEPENDENCY): VIEW? = with(dependency) {
        horizontal {
            +space(1f)
            -imageButton(
                    imageWithOptions = MaterialIcon.undo.color(dependency.colorSet.foreground).withOptions(),
                    label = "Undo",
                    importance = Importance.Low,
                    onClick = {
                        val setTo = previousState
                        previousState = value.toList()
                        value.replace(setTo)
                    }
            )
            -imageButton(
                    imageWithOptions = MaterialIcon.add.color(dependency.colorSet.foreground).withOptions(),
                    label = "Add",
                    importance = Importance.Low,
                    onClick = {
                        stack.push(editViewGenerator(null) {
                            stack.pop()
                            previousState = value.toList()
                            value.add(it)
                            println("Adding ${it}")
//                            if(value is MutableObservableListFromProperty<*>) {
//                                println("--x--")
//                                value.property.debugPrint()
//                            }
                        })
                    }
            )
        }
    }
}

fun ObservableProperty<*>.debugPrint() {
    println("Value: $value")
    if(this is TransformObservableProperty<*, *>){
        this.observable.debugPrint()
    }
}