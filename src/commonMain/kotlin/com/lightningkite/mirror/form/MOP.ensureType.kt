package com.lightningkite.mirror.form

import com.lightningkite.mirror.info.MirrorType
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.transform

fun <T> MutableObservableProperty<FormState<T>>.ensureSubtype(subtype: MirrorType<*>) = transform(
        mapper = {
            if (it is FormState.Success) {
                val nn: Any = it.value!!
                if (subtype.base.kClass.isInstance(nn)) {
                    it
                } else {
                    FormState.empty()
                }
            } else it
        },
        reverseMapper = { it }
)