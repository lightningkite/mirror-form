package com.lightningkite.mirror.form

import com.lightningkite.mirror.info.MirrorType
import com.lightningkite.mirror.info.satisfies
import com.lightningkite.mirror.info.type
import com.lightningkite.reacktive.Lifecycle
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.StandardObservableProperty
import com.lightningkite.reacktive.property.lifecycle.bind
import com.lightningkite.reacktive.property.lifecycle.listen

class NullableForm<T: Any>(val main: MutableObservableProperty<FormState<T?>>) {

    val isNotNull = StandardObservableProperty<Boolean>(false)
    val value = StandardObservableProperty<FormState<T>>(FormState.empty())

    fun bind(lifecycle: Lifecycle) {
        var justALocalModification = false
        lifecycle.bind(main) { it ->
            if (justALocalModification) {
                justALocalModification = false
                return@bind
            }
            isNotNull.value = it !is FormState.Success || it.value == null
            value.value = if(it is FormState.Success && it.value == null) FormState.empty() else {
                @Suppress("UNCHECKED_CAST")
                it as FormState<T>
            }
        }
        run {
            var previous = isNotNull.value
            lifecycle.bind(isNotNull) {
                if(it == previous) return@bind
                previous = it
                justALocalModification = true
                main.value = if(it){
                    value.value
                } else {
                    FormState.success<T?>(null)
                }
            }
        }
        run {
            var previous = value.value
            lifecycle.bind(value) {
                if(it == previous) return@bind
                previous = it
                justALocalModification = true
                if(isNotNull.value) {
                    main.value = it
                }
            }
        }
//        run {
//            var previous = main.value.let { it !is FormState.Success || it.value == null }
//            lifecycle.listen(isNotNull) {
//                if (it == previous) return@listen
//                previous = it
//                justALocalModification = true
//                main.value = if (it) {
//                    value.value
//                } else {
//                    FormState.success<T?>(null)
//                }
//            }
//        }
//        run {
//            var previous = main.value.let {
//                if (it is FormState.Success && it.value == null) FormState.empty() else {
//                    @Suppress("UNCHECKED_CAST")
//                    it as FormState<T>
//                }
//            }
//            lifecycle.listen(value) {
//                if (it == previous) return@listen
//                previous = it
//                justALocalModification = true
//                if (isNotNull.value) {
//                    main.value = it
//                }
//            }
//        }
    }
}