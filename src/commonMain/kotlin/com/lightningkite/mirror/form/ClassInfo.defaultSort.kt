package com.lightningkite.mirror.form

import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.info.FieldInfo


@Target(AnnotationTarget.FIELD)
annotation class DefaultSort(val ascending: Boolean = true)

val ClassInfo<*>.defaultSort: Sort.Field<*, *>?
    get() {
        fields.forEach {
            @Suppress("UNCHECKED_CAST") val field = it as FieldInfo<Any, Comparable<Comparable<*>>>
            val sort = field.annotations
                    .find { it.name.endsWith("DefaultSort") }
                    ?.arguments?.firstOrNull() as? Boolean
            if (sort != null) {
                return Sort.Field(field, sort)
            }
        }
        return null
    }