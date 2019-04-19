package com.lightningkite.mirror.form

import com.lightningkite.reacktive.Lifecycle
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.StandardObservableProperty
import com.lightningkite.reacktive.property.lifecycle.bind
import com.lightningkite.reacktive.property.lifecycle.listen

abstract class PartForm<T>(val main: MutableObservableProperty<FormState<T>>) {

    data class Part<T, A>(
            val name: String = "",
            val get: (T) -> A,
            val observable: StandardObservableProperty<FormState<A>>
    ) {
        val successfulValue: A get() = (observable.value as FormState.Success).value
    }

    abstract fun make(): T

    val parts = ArrayList<Part<T, *>>()
    fun <A> part(name: String, get: (T) -> A): Part<T, A> {
        val part = Part(
                name = name,
                get = get,
                observable = StandardObservableProperty(main.value.breakDown(get)
                        ?: FormState.empty())
        )
        parts += part
        return part
    }

    fun bind(lifecycle: Lifecycle) {
        var justALocalModification = false
        lifecycle.bind(main) {
            if (justALocalModification) {
                justALocalModification = false
                return@bind
            }
            for (part in parts) {
                val newValue = it.breakDown(part.get)
                if (newValue != null) {
                    @Suppress("UNCHECKED_CAST")
                    (part.observable as MutableObservableProperty<Any?>).value = newValue
                }
            }
        }
        for (part in parts) {
            lifecycle.listen(part.observable) {
                justALocalModification = true
                main.value = combine()
            }
        }
    }

    private fun combine(): FormState<T> {
        for (part in parts) {
            val fs = part.observable.value
            if (fs.isEmpty) return FormState.empty()
            if (fs is FormState.Invalid) return FormState.invalid("${part.name}: " + fs.cause)
        }
        return FormState.success(make())
    }
}