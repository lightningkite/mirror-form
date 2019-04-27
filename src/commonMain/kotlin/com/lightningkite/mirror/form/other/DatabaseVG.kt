package com.lightningkite.mirror.form.other

import com.lightningkite.kommon.collection.pop
import com.lightningkite.kommon.collection.push
import com.lightningkite.kommon.exception.stackTraceString
import com.lightningkite.koolui.async.UI
import com.lightningkite.koolui.builders.horizontal
import com.lightningkite.koolui.builders.imageButton
import com.lightningkite.koolui.builders.launchInfoDialog
import com.lightningkite.koolui.builders.vertical
import com.lightningkite.koolui.concepts.Importance
import com.lightningkite.koolui.image.ImageScaleType
import com.lightningkite.koolui.image.MaterialIcon
import com.lightningkite.koolui.image.color
import com.lightningkite.koolui.image.withSizing
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.ConditionMirror
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.archive.model.SortMirror
import com.lightningkite.mirror.form.FormState
import com.lightningkite.mirror.form.display
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
        val database: Database<T>
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
            -horizontal {
                //Filter
                //
            }

            +refresh(
                    contains = list(
                            data = items,
                            firstIndex = firstIndexObs,
                            lastIndex = lastIndexObs
                    ) { itemObs ->
                        display(observable = itemObs, type = type)
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
        if(loading.value) return
        GlobalScope.launch(Dispatchers.UI) {
            loading.value = true
            try {
                val newData = database.get(
                        condition = condition.value,
                        sort = sort.value,
                        count = 20,
                        after = items.lastOrNull()
                )
                if(startLoadIndex != loadIndex) return@launch

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
                        stack.push(FormViewGenerator(ConditionMirror(type), FormState.success(condition.value)) {
                            condition.value = it
                            reset()
                        })
                    }
            )
            -imageButton(
                    imageWithSizing = MaterialIcon.sort.color(dependency.colorSet.foreground).withSizing(),
                    label = "Sort",
                    importance = Importance.Low,
                    onClick = {
                        val sortType: MirrorType<List<Sort<T, *>>> = ListMirror(SortMirror(type, SortMirror.VMirrorMinimal) as MirrorType<Sort<T, *>>)
                        stack.push(FormViewGenerator<List<Sort<T, *>>, DEPENDENCY, VIEW>(
                                type = sortType,
                                startingWith = FormState.success(sort.value)
                        ) {
                            sort.value = it
                            reset()
                        })
                        Unit
                    }
            )
            -work(imageButton(
                    imageWithSizing = MaterialIcon.add.color(dependency.colorSet.foreground).withSizing(),
                    label = "Add",
                    importance = Importance.Low,
                    onClick = {
                        stack.push(FormViewGenerator(type) { newItem ->
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
                            }
                        })
                    }
            ), addWorking)
        }
    }
}