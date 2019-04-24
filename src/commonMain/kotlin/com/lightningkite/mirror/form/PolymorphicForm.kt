package com.lightningkite.mirror.form

import com.lightningkite.mirror.info.MirrorType
import com.lightningkite.mirror.info.satisfies
import com.lightningkite.mirror.info.type
import com.lightningkite.reacktive.Lifecycle
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.StandardObservableProperty
import com.lightningkite.reacktive.property.lifecycle.bind
import com.lightningkite.reacktive.property.lifecycle.listen

class PolymorphicForm<T>(main: MutableObservableProperty<FormState<T>>, val satisfies: MirrorType<out T>) : PartForm<T>(main) {

    val type = part("type") {
        if (it == null) null else {
            val notNull: Any = it
            notNull::class.type.satisfies(satisfies)
        }
    }

    val actualValue = part("value") { it }

    override fun make(): T = actualValue.successfulValue

}