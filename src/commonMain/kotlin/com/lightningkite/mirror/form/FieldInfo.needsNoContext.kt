package com.lightningkite.mirror.form

import com.lightningkite.mirror.info.MirrorClass


@Target(AnnotationTarget.FIELD)
annotation class NeedsNoContext

val MirrorClass.Field<*, *>.needsNoContext: Boolean
    get() = annotations
            .any { it.name.endsWith("NeedsNoContext") } || when(this.name) {
        "email" -> true
        "title" -> true
        "name" -> true
        "body" -> true
        else -> false
    }