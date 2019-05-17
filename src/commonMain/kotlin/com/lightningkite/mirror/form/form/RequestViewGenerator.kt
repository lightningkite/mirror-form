//package com.lightningkite.mirror.form.form
//
//import com.lightningkite.koolui.builders.*
//import com.lightningkite.koolui.concepts.Importance
//import com.lightningkite.koolui.geometry.AlignPair
//import com.lightningkite.koolui.image.MaterialIcon
//import com.lightningkite.koolui.image.color
//import com.lightningkite.koolui.image.withSizing
//import com.lightningkite.koolui.views.ViewFactory
//import com.lightningkite.koolui.views.ViewGenerator
//import com.lightningkite.mirror.form.FormRequest
//import com.lightningkite.mirror.request.Request
//import com.lightningkite.reacktive.property.StandardObservableProperty
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.launch
//
//fun RequestViewGenerator<T : Request<OUT>, OUT, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
//        val formRequest: FormRequest<T>,
//        val wraps: ViewGenerator<DEPENDENCY, VIEW> = ReflectiveFormViewGenerator(formRequest),
//        val onResult: (OUT) -> Unit
//) : ViewGenerator<DEPENDENCY, VIEW> {
//
//    val isWorking = StandardObservableProperty(false)
//
//    override fun generate(dependency: DEPENDENCY): VIEW = wraps.generate(dependency)
//
//    override fun generateActions(dependency: DEPENDENCY): VIEW? = with(dependency) {
//        work(
//                view = imageButton(
//                        imageWithSizing = MaterialIcon.check.color(dependency.colorSet.foreground).withSizing(),
//                        label = "Run Request",
//                        importance = Importance.Low,
//                        onClick = {
//                            val request: T = formRequest.observable.value.valueOrNull ?: return@imageButton
//                            GlobalScope.launch {
//                                val result: OUT = (formRequest.general.requestHandler ?: return@launch).invoke(request)
//                                onResult(result)
//                            }
//                        }
//                ),
//                isWorking = isWorking
//        )
//
//    }
//}