package com.lightningkite.mirror.form

import com.lightningkite.mirror.info.FieldInfo


@Target(AnnotationTarget.FIELD)
annotation class Importance(val amount: Float)

val FieldInfo<*, *>.importance: Float
    get() = annotations
            .find { it.name.endsWith("Importance") }
            ?.arguments?.firstOrNull() as? Float ?: .5f