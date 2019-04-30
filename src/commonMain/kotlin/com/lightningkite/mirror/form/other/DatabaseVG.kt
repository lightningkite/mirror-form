package com.lightningkite.mirror.form.other

import com.lightningkite.kommon.collection.pop
import com.lightningkite.kommon.collection.popFrom
import com.lightningkite.kommon.collection.push
import com.lightningkite.kommon.collection.pushFrom
import com.lightningkite.kommon.exception.stackTraceString
import com.lightningkite.koolui.async.UI
import com.lightningkite.koolui.builders.*
import com.lightningkite.koolui.concepts.Importance
import com.lightningkite.koolui.image.ImageScaleType
import com.lightningkite.koolui.image.MaterialIcon
import com.lightningkite.koolui.image.color
import com.lightningkite.koolui.image.withSizing
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.form.*
import com.lightningkite.mirror.form.form.FormViewGenerator
import com.lightningkite.mirror.info.AnyMirror
import com.lightningkite.mirror.info.ComparableMirror
import com.lightningkite.mirror.info.ListMirror
import com.lightningkite.mirror.info.MirrorType
import com.lightningkite.reacktive.list.MutableObservableList
import com.lightningkite.reacktive.list.WrapperObservableList
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.ObservableProperty
import com.lightningkite.reacktive.property.StandardObservableProperty
import com.lightningkite.reacktive.property.lifecycle.bind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DatabaseVG<T : Any, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val stack: MutableObservableList<ViewGenerator<DEPENDENCY, VIEW>>,
        val type: MirrorType<T>,
        val database: Database<T>,
        val generalRequest: GeneralRequest,// = GeneralRequest(),
        val onSelect: (T) -> Unit = {
            stack.push(DisplayViewGenerator(it, type, generalRequest = generalRequest))
        }
) : ViewGenerator<DEPENDENCY, VIEW> {
    override val title: String
        get() = super.title

    val items = WrapperObservableList<T>()
    val loading = StandardObservableProperty(false)
    var anyLeft = true
    val firstIndexObs = StandardObservableProperty(0)
    val lastIndexObs = StandardObservableProperty(0)

    val condition = StandardObservableProperty<Condition<T>>(Condition.Always)
    val sort = StandardObservableProperty<List<Sort<T, *>>>(listOf())

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        vertical {
            +refresh(
                    contains = list(
                            data = items,
                            firstIndex = firstIndexObs,
                            lastIndex = lastIndexObs
                    ) { itemObs ->
                        card(horizontal {
                            +display(observable = itemObs, type = type, generalRequest = generalRequest, scale = ViewSize.Summary).clickable {
                                onSelect(itemObs.value)
                            }
                            -imageButton(
                                    imageWithSizing = MaterialIcon.moreVert.color(dependency.colorSet.foreground).withSizing(scaleType = ImageScaleType.Crop),
                                    label = "More",
                                    importance = Importance.Low,
                                    onClick = {
                                        launchSelector(options = listOf(
                                                "Edit" to {
                                                    val oldItem = itemObs.value
                                                    stack.pushFrom(this@DatabaseVG, FormViewGenerator(
                                                            type = type,
                                                            startingWith = FormState.success(oldItem),
                                                            generalRequest = generalRequest
                                                    ) { newItem ->
                                                        GlobalScope.launch(Dispatchers.UI) {
                                                            try {
                                                                val result = database.update(
                                                                        condition = Condition.Equal(oldItem),
                                                                        operation = Operation.Set(newItem)
                                                                )
                                                                val index = items.indexOfFirst { it == oldItem }
                                                                if(index != -1){
                                                                    items[index] = newItem
                                                                }
                                                            } catch (t: Throwable) {
                                                                dependency.launchInfoDialog(
                                                                        title = "Error Updating Item",
                                                                        message = t.message ?: "Unknown error"
                                                                )
                                                            }
                                                            stack.popFrom(this@FormViewGenerator)
                                                        }
                                                    })
                                                },
                                                "Delete" to {
                                                    val oldItem = itemObs.value
                                                    launchConfirmationDialog {
                                                        GlobalScope.launch(Dispatchers.UI) {
                                                            try {
                                                                val result = database.delete(
                                                                        condition = Condition.Equal(oldItem)
                                                                )
                                                                items.remove(oldItem)
                                                            } catch (t: Throwable) {
                                                                dependency.launchInfoDialog(
                                                                        title = "Error Inserting Item",
                                                                        message = t.message ?: "Unknown error"
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                        ))
                                    }
                            )
                        })

                    }.apply {
                        lifecycle.bind(lastIndexObs) {
                            if (it >= items.lastIndex) {
                                loadMore()
                            }
                        }
                    },
                    working = loading,
                    onRefresh = {
                        reset()
                    }
            )
        }
    }

    var loadIndex = 0

    init {
        reset()
    }

    fun reset() {
        loadIndex++
        items.clear()
        anyLeft = true
        loading.value = false
        loadMore()
    }

    fun loadMore() {
        val startLoadIndex = loadIndex
        if (loading.value) return
        GlobalScope.launch(Dispatchers.UI) {
            loading.value = true
            try {
                val newData = database.get(
                        condition = condition.value,
                        sort = sort.value,
                        count = 20,
                        after = items.lastOrNull()
                )
                if (startLoadIndex != loadIndex) return@launch

                items.addAll(newData)
                if (newData.isEmpty()) {
                    anyLeft = false
                }
            } catch (t: Throwable) {
                println(t.stackTraceString())
            }
            loading.value = false
        }
    }

    val addWorking = StandardObservableProperty(false)
    override fun generateActions(dependency: DEPENDENCY): VIEW? = with(dependency) {
        horizontal {
            -imageButton(
                    imageWithSizing = MaterialIcon.filter.color(dependency.colorSet.foreground).withSizing(),
                    label = "Filter",
                    importance = Importance.Low,
                    onClick = {
                        stack.pushFrom(this@DatabaseVG, FormViewGenerator(ConditionMirror(type), FormState.success(condition.value), generalRequest = generalRequest) {
                            condition.value = it
                            reset()
                            stack.popFrom(this@FormViewGenerator)
                        })
                    }
            )
            -imageButton(
                    imageWithSizing = MaterialIcon.sort.color(dependency.colorSet.foreground).withSizing(),
                    label = "Sort",
                    importance = Importance.Low,
                    onClick = {
                        val sortType: MirrorType<List<Sort<T, *>>> = ListMirror(SortMirror(type, SortMirror.VMirrorMinimal) as MirrorType<Sort<T, *>>)
                        stack.pushFrom(this@DatabaseVG, FormViewGenerator<List<Sort<T, *>>, DEPENDENCY, VIEW>(
                                type = sortType,
                                startingWith = FormState.success(sort.value),
                                generalRequest = generalRequest
                        ) {
                            sort.value = it
                            reset()
                            stack.popFrom(this@FormViewGenerator)
                        })
                        Unit
                    }
            )
            -work(imageButton(
                    imageWithSizing = MaterialIcon.add.color(dependency.colorSet.foreground).withSizing(),
                    label = "Add",
                    importance = Importance.Low,
                    onClick = {
                        stack.pushFrom(this@DatabaseVG, FormViewGenerator(type, generalRequest = generalRequest) { newItem ->
                            GlobalScope.launch(Dispatchers.UI) {
                                addWorking.value = true
                                try {
                                    val result = database.insert(listOf(newItem))
                                    items.addAll(0, result)
                                } catch (t: Throwable) {
                                    dependency.launchInfoDialog(
                                            title = "Error Inserting Item",
                                            message = t.message ?: "Unknown error"
                                    )
                                }
                                addWorking.value = false
                                stack.popFrom(this@FormViewGenerator)
                            }
                        })
                    }
            ), addWorking)
        }
    }
}