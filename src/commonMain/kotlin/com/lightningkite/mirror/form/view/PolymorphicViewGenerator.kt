//package com.lightningkite.mirror.form.view
//
//import com.lightningkite.koolui.builders.vertical
//import com.lightningkite.koolui.concepts.TextSize
//import com.lightningkite.koolui.views.ViewFactory
//import com.lightningkite.koolui.views.ViewGenerator
//import com.lightningkite.mirror.form.DisplayRequest
//import com.lightningkite.mirror.form.ViewEncoder
//import com.lightningkite.mirror.info.MirrorType
//
//class PolymorphicViewGenerator<T, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
//        val request: DisplayRequest<*>
//) : ViewGenerator<DEPENDENCY, VIEW> {
//    val kclass = request.value?.let { it::class }
//    val underlying = if (kclass != null) {
//        val deferRequest = request.copy(newType = kclass.type as MirrorType<Any?>, value = request.value)
//        ViewEncoder.getViewGenerator<Any?, DEPENDENCY, VIEW>(deferRequest)
//    } else {
//        null
//    }
//
//    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
//        vertical {
//            -text(size = TextSize.Tiny, text = kclass?.type?.base?.name?.humanify()
//                    ?: request.type.base.name.humanify())
//            if (underlying != null) {
//                -underlying.generate(dependency)
//            } else {
//                -text(
//                        text = request.general.nullString
//                )
//            }
//        }
//    }
//}