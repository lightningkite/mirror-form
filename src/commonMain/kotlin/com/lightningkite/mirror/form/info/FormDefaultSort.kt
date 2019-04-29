package com.lightningkite.mirror.form.info

import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.info.MirrorAnnotation
import com.lightningkite.mirror.info.MirrorClass


@Target(AnnotationTarget.FIELD)
annotation class FormDefaultSort(val ascending: Boolean = true)

val MirrorClass<*>.defaultSort: Sort<*, *>?
    get() {
        fields.forEach {
            @Suppress("UNCHECKED_CAST") val field = it as MirrorClass.Field<Any, Comparable<Comparable<*>>>
            val sortAnnotation = field.annotations
                    .asSequence()
                    .mapNotNull { it as? MirrorAnnotation }
                    .find { it.annotationType == FormDefaultSort::class }
            if (sortAnnotation != null) {
                val asc = sortAnnotation.asMap()["ascending"] as? Boolean ?: true
                return Sort(field, asc)
            }
        }
        return null
    }