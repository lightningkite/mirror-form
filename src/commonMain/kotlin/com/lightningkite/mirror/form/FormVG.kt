package com.lightningkite.mirror.form

import com.lightningkite.koolui.color.Color
import com.lightningkite.koolui.concepts.TabItem
import com.lightningkite.koolui.image.asImage
import com.lightningkite.koolui.image.color
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.info.Type
import com.lightningkite.reacktive.list.MutableObservableList
import com.lightningkite.reacktive.list.ObservableList
import com.lightningkite.reacktive.list.observableListOf
import com.lightningkite.reacktive.property.StandardObservableProperty
import com.lightningkite.reacktive.property.lifecycle.openCloseBinding

class FormVG<VIEW, V>(
        val formEncoder: FormEncoder,
        val stack: MutableObservableList<ViewGenerator<ViewFactory<VIEW>, VIEW>>,
        value: V,
        val type: Type<V>,
        val onComplete:FormVG<VIEW, V>.(V)->Unit
) : ViewGenerator<ViewFactory<VIEW>, VIEW> {

    val valueObs = StandardObservableProperty(value)

    val doneAction = TabItem(
            image = com.lightningkite.koolui.image.MaterialIcon.check.color(Color.white).asImage(),
            text = "Done",
            description = "Finish editing."
    )

    var formDump = { value }

    override val actions: ObservableList<Pair<TabItem, suspend () -> Unit>> = observableListOf(
            doneAction to suspend {
                onComplete(formDump())
            }
    )
    override val title: String = formEncoder.registry.kClassToExternalNameRegistry[type.kClass]!!.humanify()

    override fun generate(dependency: ViewFactory<VIEW>): VIEW {
        val result = formEncoder.write(
                factory = dependency,
                stack = stack,
                type = type,
                value = valueObs.value
        )
        val dump = result.dump

        with(dependency){
            result.view.lifecycle.openCloseBinding(
                    onOpen = {},
                    onClose = {
                        valueObs.value = dump()
                    }
            )
        }
        formDump = result.dump
        return result.view
    }
}


//class SuspendMapVG<VIEW, K, V : Any>(
//        val formEncoder: FormEncoder,
//        val stack: MutableObservableList<ViewGenerator<ViewFactory<VIEW>, VIEW>>,
//        val suspendMap: SuspendMap<K, V>,
//        val onSelect: (SuspendMapVG<VIEW, K, V>.(Pair<K, V>) -> Unit)? = null
//) : ViewGenerator<ViewFactory<VIEW>, VIEW> {
//
//    val lastQuery = StandardObservableList<Pair<K, V>>()
//    val condition = StandardObservableProperty(Condition.Always<V>())
//    val sort = StandardObservableProperty<Sort<V>?>(null)
//    val quickFilter = StandardObservableProperty("")
//
//    val filteredResults = lastQuery.filtering()
//    init{
//        quickFilter.add { filter ->
//            filteredResults.filter = if(filter.isBlank()){
//                { true }
//            } else {
//                {
//                    val queryString = (it.first.toString() + " " + it.second.toString())
//                    val queryFor = filter.split(" ")
//                    queryFor.all { queryString.contains(it, true) }
//                }
//            }
//        }
//    }
//
//    fun reloadResults() {
//        GlobalScope.launch(Dispatchers.UI) {
//            lastQuery.replace(suspendMap.query(condition = condition.value, sortedBy = sort.value))
//        }
//    }
//
//    init {
//        reloadResults()
//    }
//
//    override fun generate(dependency: ViewFactory<VIEW>): VIEW = with(dependency) {
//        vertical {
//            - textField(text = quickFilter, placeholder = "Search")
//            - list(
//                    data = filteredResults,
//                    makeView = { obs ->
//                        text(text = obs.transform { it.toString() })
//                                .clickable {
//                                    onSelect?.invoke(this@SuspendMapVG, obs.value) ?: run {
//                                        //open edit form
//                                    }
//                                }
//                                .altClickable {
//                                    launchSelector(options = listOf(
//                                            "Edit" to {
//                                                Unit
//                                            },
//                                            "Delete" to {
//                                                launchConfirmationDialog(message = "Are you sure you want to delete ${obs.value.second}?") {
//                                                    GlobalScope.launch(Dispatchers.UI) {
//                                                        suspendMap.remove(obs.value.first)
//                                                        lastQuery.remove(obs.value)
//                                                    }
//                                                }
//                                                Unit
//                                            }
//                                    ))
//                                }
//                    }
//            )
//        }
//
//    }
//
//    fun openEditForm(dependency: ViewFactory<VIEW>) {
//        stack.pushFrom(this, FormVG())
//    }
//}
//