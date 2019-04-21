package com.lightningkite.mirror.form

import com.lightningkite.koolui.concepts.TextSize
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.form.view.StringViewGenerator
import kotlin.reflect.KClass


//Convenience stuff for setting up interceptors

fun <T : Any> ViewEncoder.Interceptors.string(
        type: KClass<T>,
        priority: Float = 0f,
        matches: (DisplayRequest<*>) -> Boolean = { true },
        toString: (T) -> String? = { this.toString() }
) {
    this += object : ViewEncoder.BaseTypeInterceptor<T>(type, priority) {

        override fun matchesTyped(request: DisplayRequest<T>): Boolean = matches.invoke(request)

        @Suppress("UNCHECKED_CAST")
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: DisplayRequest<T>): ViewGenerator<DEPENDENCY, VIEW> = StringViewGenerator(request, toString)
    }
}