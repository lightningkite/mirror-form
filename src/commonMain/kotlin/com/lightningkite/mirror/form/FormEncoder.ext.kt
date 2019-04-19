package com.lightningkite.mirror.form

import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.reacktive.property.MutableObservableProperty
import kotlin.reflect.KClass


//Convenience stuff for setting up interceptors

fun <T : Any> FormEncoder.Interceptors.observable(type: KClass<T>, default: T, priority: Float = 0f, generate: ViewFactory<Any?>.(MutableObservableProperty<T>) -> Any?) {
    this += FormEncoder.TypeInterceptor(
            requiresType = type,
            matchPriority = 0f
    ) {
        ObservableFormViewGenerator(
                request = it,
                default = default,
                gen = generate
        )
    }
}

fun FormEncoder.Interceptors.default(
        requiresType: KClass<*>?,
        matchPriority: Float = 0f,
        matches: (Request<*>) -> Boolean = { true },
        generate: (Request<*>) -> FormEncoder.FormViewGenerator<Any?, ViewFactory<Any?>, Any?>
) {
    this += FormEncoder.DefaultInterceptor(
            requiresType = requiresType,
            matchPriority = matchPriority,
            matches = matches,
            generate = generate
    )
}

fun <T : Any> FormEncoder.Interceptors.type(
        requiresType: KClass<T>,
        matchPriority: Float = 0f,
        matches: (Request<*>) -> Boolean = { true },
        generate: (Request<*>) -> FormEncoder.FormViewGenerator<T, ViewFactory<Any?>, Any?>
) {
    this += FormEncoder.TypeInterceptor<T>(
            requiresType = requiresType,
            matchPriority = matchPriority,
            matches = matches,
            generate = generate
    )
}