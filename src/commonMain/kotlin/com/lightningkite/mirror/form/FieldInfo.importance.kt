package com.lightningkite.mirror.form

import com.lightningkite.mirror.info.MirrorClass


@Target(AnnotationTarget.FIELD)
annotation class Importance(val amount: Float)

val MirrorClass.Field<*, *>.importance: Float
    get() = annotations
            .find { it.name.endsWith("Importance") }
            ?.arguments?.firstOrNull() as? Float ?: .5f