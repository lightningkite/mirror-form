package com.lightningkite.mirror.form

import com.lightningkite.mirror.info.FieldInfo


@Target(AnnotationTarget.FIELD)
annotation class NeedsNoContext

val FieldInfo<*, *>.needsNoContext: Boolean
    get() = annotations
            .any { it.name.endsWith("NeedsNoContext") } || when(this.name) {
        "email" -> true
        "title" -> true
        "name" -> true
        "body" -> true
        else -> false
    }