package com.lightningkite.mirror.form

import com.lightningkite.kommon.atomic.AtomicReference
import com.lightningkite.kommon.collection.SortedBag
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.info.*
import com.lightningkite.reacktive.property.transform
import kotlin.reflect.KClass


object FormEncoder {

    interface Interceptor : Comparable<Interceptor> {
        val requiresType: KClass<*>?
        val matchPriority: Float get() = Float.NEGATIVE_INFINITY
        override fun compareTo(other: Interceptor): Int = -this.matchPriority.compareTo(other.matchPriority)
        fun <T> matches(request: FormRequest<T>): Boolean = true

        fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: FormRequest<T>): ViewGenerator<DEPENDENCY, VIEW>

        companion object
    }

    abstract class BaseInterceptor(
            override val requiresType: KClass<*>? = null,
            override val matchPriority: Float = 0f
    ) : Interceptor

    abstract class BaseTypeInterceptor<T : Any>(
            override val requiresType: KClass<T>,
            override val matchPriority: Float = 0f
    ) : Interceptor {
        open fun matchesTyped(request: FormRequest<T>): Boolean = true
        abstract fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<T>): ViewGenerator<DEPENDENCY, VIEW>

        @Suppress("UNCHECKED_CAST")
        final override fun <T2> matches(request: FormRequest<T2>): Boolean = !request.type.isNullable && matchesTyped(request as FormRequest<T>)

        @Suppress("UNCHECKED_CAST")
        final override fun <T2, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: FormRequest<T2>): ViewGenerator<DEPENDENCY, VIEW> = generateTyped(request as FormRequest<T>)
    }

    abstract class BaseNullableTypeInterceptor<T : Any>(
            override val requiresType: KClass<T>,
            override val matchPriority: Float = 0f
    ) : Interceptor {
        open fun matchesTyped(request: FormRequest<T?>): Boolean = true
        abstract fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<T?>): ViewGenerator<DEPENDENCY, VIEW>

        @Suppress("UNCHECKED_CAST")
        final override fun <T2> matches(request: FormRequest<T2>): Boolean = matchesTyped(request as FormRequest<T?>)

        @Suppress("UNCHECKED_CAST")
        final override fun <T2, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: FormRequest<T2>): ViewGenerator<DEPENDENCY, VIEW> {
            if (request.type.isNullable) {
                return generateTyped(request as FormRequest<T?>)
            } else {
                return generateTyped((request as FormRequest<T>).copy(
                        type = request.type.nullable,
                        observable = request.observable.transform(
                                mapper = { it as FormState<T?> },
                                reverseMapper = {
                                    if (it is FormState.Success && it.value == null)
                                        FormState.empty()
                                    else
                                        it as FormState<T>
                                }
                        )
                ))
            }
        }
    }

    @Deprecated("x")
    data class DefaultInterceptor(
            override val requiresType: KClass<*>?,
            override val matchPriority: Float = 0f,
            val matches: (FormRequest<*>) -> Boolean = { true },
            val generate: (FormRequest<*>) -> ViewGenerator<ViewFactory<Any?>, Any?>
    ) : Interceptor {
        override fun <T> matches(request: FormRequest<T>): Boolean = this.matches.invoke(request)

        @Suppress("UNCHECKED_CAST")
        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: FormRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
            return this.generate.invoke(request) as ViewGenerator<DEPENDENCY, VIEW>
        }
    }

    @Deprecated("x")
    data class TypeInterceptor<T : Any>(
            override val requiresType: KClass<T>,
            override val matchPriority: Float = 0f,
            val matches: (FormRequest<T>) -> Boolean = { true },
            val generate: (FormRequest<T>) -> ViewGenerator<ViewFactory<Any?>, Any?>
    ) : Interceptor {
        @Suppress("UNCHECKED_CAST")
        override fun <T2> matches(request: FormRequest<T2>): Boolean = this.matches.invoke(request as FormRequest<T>)

        @Suppress("UNCHECKED_CAST")
        override fun <T2, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: FormRequest<T2>): ViewGenerator<DEPENDENCY, VIEW> {
            return this.generate.invoke(request as FormRequest<T>) as ViewGenerator<DEPENDENCY, VIEW>
        }
    }

    class Interceptors(
            val byType: MutableMap<KClass<*>, SortedBag<Interceptor>> = HashMap(),
            val others: SortedBag<Interceptor> = SortedBag()
    ) {
        constructor(interceptors: Set<Interceptor>) : this(
                byType = interceptors.asSequence()
                        .filter { it.requiresType != null }
                        .groupingBy { it.requiresType!! }
                        .aggregateTo<Interceptor, KClass<*>, SortedBag<Interceptor>, HashMap<KClass<*>, SortedBag<Interceptor>>>(HashMap()) { key, accumulator, element, first ->
                            val a = accumulator ?: SortedBag()
                            a.add(element)
                            a
                        },
                others = interceptors
                        .asSequence()
                        .filter { it.requiresType == null }
                        .fold(SortedBag()) { bag, it -> bag.add(it); bag }
        )

        operator fun plusAssign(interceptor: Interceptor) {
            if (interceptor.requiresType != null) {
                byType.getOrPut(interceptor.requiresType!!) { SortedBag() }.add(interceptor)
            } else {
                others.add(interceptor)
            }
        }

        fun resolve(request: FormRequest<*>): Interceptor? {
            return byType[request.type.base.kClass]?.let {
                it.asSequence().filter { it.matches(request) }.firstOrNull()
            } ?: others.asSequence().filter { it.matches(request) }.firstOrNull()
        }

        operator fun plus(other: Interceptors): Interceptors {
            return Interceptors(
                    byType = this.byType.toMutableMap().apply { putAll(other.byType) },
                    others = this.others + other.others
            )
        }
    }

    private val interceptors = AtomicReference(FormEncoderDefaultModule)
    fun register(interceptors: Interceptors) {
        var start = this.interceptors.value
        var new = start + interceptors
        while (start != this.interceptors.value) {
            start = this.interceptors.value
            new = start + interceptors
        }
        this.interceptors.value = new
    }

    fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> getViewGenerator(request: FormRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
        return interceptors.value.resolve(request)?.generate(request)
                ?: throw IllegalArgumentException("No matching ViewGenerator was found.")
    }


}