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