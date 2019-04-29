package com.lightningkite.mirror.form.info

import com.lightningkite.mirror.info.MirrorAnnotation
import com.lightningkite.mirror.info.MirrorClass


@Target(AnnotationTarget.FIELD)
annotation class FormNeedsNoContext

val MirrorClass.Field<*, *>.needsNoContext: Boolean
    get() = annotations
            .any { it is FormNeedsNoContext || (it as? MirrorAnnotation)?.annotationType == FormNeedsNoContext::class } || when (this.name) {
        "email" -> true
        "title" -> true
        "name" -> true
        "body" -> true
        else -> false
    }