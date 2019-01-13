package com.lightningkite.mirror.form

import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.info.FieldInfo

@Target(AnnotationTarget.FIELD)
annotation class Hidden

val FieldInfo<*, *>.isHidden: Boolean
    get() = annotations.any { it.name.endsWith("Hidden") }

fun <T : Any> ClassInfo<T>.fieldsToRender(showHidden: Boolean = false): Sequence<FieldInfo<T, *>> = fields.asSequence()
        .let {
            if (showHidden) it else it.filter { !it.isHidden }
        }
        .sortedByDescending { it.importance }