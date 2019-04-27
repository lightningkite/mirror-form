package com.lightningkite.mirror.form

import com.lightningkite.reacktive.Lifecycle
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.StandardObservableProperty
import com.lightningkite.reacktive.property.lifecycle.listen

class ValidatingForm<Raw, Type>(
        val main: MutableObservableProperty<FormState<Type>>,
        val toRaw: (Type) -> Raw,
        val toTypeWithValidation: (Raw) -> FormState<Type>
) {
    val raw = StandardObservableProperty(main.value.breakDown(toRaw) ?: FormState.empty())

    fun bind(lifecycle: Lifecycle) {
        var isLocalChange = false

        lifecycle.listen(raw) {
            isLocalChange = true
            main.value = if (it is FormState.Success) {
                it.value.let(toTypeWithValidation)
            } else {
                it.asType()
            }
        }

        lifecycle.listen(main) {
            if (isLocalChange) {
                isLocalChange = false
                return@listen
            }
            raw.value = if (it is FormState.Success) {
                FormState.success(it.value.let(toRaw))
            } else {
                it.asType()
            }
        }
    }
}