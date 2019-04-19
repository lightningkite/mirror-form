package com.lightningkite.mirror.form

sealed class FormState<out T> {
    abstract val valueOrNull: T?
    open val isEmpty: Boolean get() = false
    abstract fun <A> breakDown(get: (T) -> A): FormState<A>?
    abstract fun <A> asType(): FormState<A>

    data class Success<T>(val value: T) : FormState<T>() {
        override val valueOrNull: T? get() = value
        override fun <A> breakDown(get: (T) -> A): FormState<A>? = success(value.let(get))
        override fun <A> asType(): FormState<A> = empty()
    }

    data class Invalid<T>(val cause: Any? = null) : FormState<T>() {
        override val valueOrNull: T? get() = null
        override fun <A> breakDown(get: (T) -> A): FormState<A>? = null
        override fun <A> asType(): FormState<A> = Invalid(cause)
    }

    object Empty : FormState<Any?>() {
        override val valueOrNull: Any? get() = null
        override val isEmpty: Boolean
            get() = true

        override fun <A> breakDown(get: (Any?) -> A): FormState<A>? = empty()
        override fun <A> asType(): FormState<A> = empty()
    }

    companion object {
        @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
        inline fun <T> empty() = Empty as FormState<T>

        fun <T> success(value: T) = Success(value)
        fun <T> invalid(cause: Any?) = Invalid<T>(cause)

        fun <T> combineFailures(parts: Sequence<Pair<String, FormState<*>>>): FormState<T>? {
            for ((key, part) in parts) {
                if (part.isEmpty) return empty()
                if (part is Invalid) return invalid("$key: " + part.cause)
            }
            return null
        }
    }
}
//
//fun <T: Any> ObservableProperty<FormState<T>>.snapForm(
//        fields: Iterable<MirrorClass.Field<T, *>>
//): Map<MirrorClass.Field<T, *>, StandardObservableProperty<FormState<*>>>{
//    return fields.associate {
//        it to StandardObservableProperty(successfulValue.breakDown(it.get) ?: FormState.empty())
//    }
//}
//
//fun <T: Any> Lifecycle.bindForm(
//        type: MirrorType<T>,
//        form: MutableObservableProperty<FormState<T>>,
//        fields: Map<MirrorClass.Field<T, *>, StandardObservableProperty<FormState<*>>>,
//        providingValues: Map<MirrorClass.Field<T, *>, Any?>
//) = bindFormManual(form = form, fields = fields){
//    val output = HashMap<Int, Any?>()
//    fields.entries.associateTo(output) { it.key.index to it.successfulValue.successfulValue.let{ it as FormState.Success}.successfulValue }
//    providingValues.entries.associateTo(output) { it.key.index to it.successfulValue }
//    PartialBreaker.fold(type, output)
//}
//
//inline fun <T: Any> Lifecycle.bindFormManual(
//        form: MutableObservableProperty<FormState<T>>,
//        fields: Map<MirrorClass.Field<T, *>, StandardObservableProperty<FormState<*>>>,
//        crossinline make: (Map<MirrorClass.Field<T, *>, Any?>)->T
//) {
//    var justALocalModification = false
//    listen(form){
//        if(justALocalModification){
//            justALocalModification = false
//            return@listen
//        }
//        for((field, obs) in fields) {
//            val newValue = it.breakDown(field.get)
//            if(newValue != null){
//                @Suppress("UNCHECKED_CAST")
//                (obs as MutableObservableProperty<Any?>).successfulValue = newValue
//            }
//        }
//    }
//    for((_, obs) in fields) {
//        listen(obs){
//            justALocalModification = true
//            form.successfulValue = FormState.combineFailures<T>(
//                    parts = fields.entries.associate { it.key.name to it.successfulValue.successfulValue }
//            ) ?: FormState.success(make(fields.entries.associate { it.key to (it.successfulValue.successfulValue as FormState.Success).successfulValue }))
//        }
//    }
//}

