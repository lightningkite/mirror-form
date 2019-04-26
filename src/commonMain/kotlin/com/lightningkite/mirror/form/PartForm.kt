package com.lightningkite.mirror.form

import com.lightningkite.reacktive.EnablingMutableCollection
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.addAndInvoke

abstract class PartForm<T>(val main: MutableObservableProperty<FormState<T>>) {

    inner class PartProperty<A>(
            val name: String = "",
            val required: Boolean = true,
            val get: (T) -> A
    ): EnablingMutableCollection<(FormState<A>) -> Unit>(), MutableObservableProperty<FormState<A>> {

        var listening = false
        var fromMain = false

        override var value: FormState<A> = main.value.breakDown(get) ?: FormState.empty()
            set(value) {
                field = value
                if(fromMain){
                    fromMain = false
                } else {
                    forEach { it.invoke(value) }
                    main.value = combine()
                }
            }

        val callback = { a: FormState<T> ->
            val broken = a.breakDown(get)
            if(broken != null && broken != value) {
                fromMain = true
                value = broken
            }
        }

        override fun enable() {
            listening = true
            main.add(callback)
//            main.addAndInvoke(callback)
        }

        override fun disable() {
            main.remove(callback)
            listening = false
        }

        @Suppress("UNCHECKED_CAST")
        val successfulValue: A get() = value.valueOrNull as A
        //Perhaps something could be added here?
    }

    abstract fun make(): T

    val parts = ArrayList<PartProperty<*>>()
    fun <A> part(name: String, required: Boolean = true, get: (T) -> A): PartProperty<A> {
        val part = PartProperty(
                name = name,
                required = required,
                get = get
        )
        parts += part
        return part
    }

    private fun combine(): FormState<T> {
        for (part in parts) {
            val fs = part.value
            if (fs.isEmpty && part.required) {
                return FormState.empty()
            }
            if (fs is FormState.Invalid) return FormState.invalid("${part.name}: " + fs.cause)
        }
        return FormState.success(make())
    }
}