package com.lightningkite.mirror.form.info

import com.lightningkite.koolui.concepts.NumberInputType
import com.lightningkite.koolui.concepts.TextInputType
import com.lightningkite.mirror.info.MirrorAnnotation
import com.lightningkite.mirror.info.MirrorClass


@Target(AnnotationTarget.FIELD)
annotation class FormNumberInputType(val inputType: NumberInputType)

val MirrorClass.Field<*, *>.numberInputType: NumberInputType?
    get() = annotations
            .asSequence()
            .mapNotNull { it as? MirrorAnnotation }
            .filter { it.annotationType == NumberInputType::class }
            .firstOrNull()?.asMap()?.values?.firstOrNull() as? NumberInputType