package com.lightningkite.mirror.form

import com.lightningkite.mirror.info.MirrorAnnotation
import com.lightningkite.mirror.info.MirrorClass

@Target(AnnotationTarget.FIELD)
annotation class Hidden

val MirrorClass.Field<*, *>.isHidden: Boolean
    get() = annotations.any { it is Hidden || (it as? MirrorAnnotation)?.annotationType == Hidden::class }

//fun <T : Any> MirrorClass<T>.fieldsToRender(showHidden: Boolean = false): Sequence<MirrorClass.Field<T, *>> = fields.asSequence()
//        .let {
//            if (showHidden) it else it.filter { !it.isHidden }
//        }
//        .sortedByDescending { it.importance }