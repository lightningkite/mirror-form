package com.lightningkite.mirror.form

import com.lightningkite.kommon.atomic.AtomicReference
import com.lightningkite.kommon.collection.SortedBag
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import kotlin.reflect.KClass

object ViewEncoder {

    interface Interceptor : Comparable<Interceptor> {
        val requiresType: KClass<*>?
        val matchPriority: Float get() = Float.NEGATIVE_INFINITY
        override fun compareTo(other: Interceptor): Int = -this.matchPriority.compareTo(other.matchPriority)
        fun <T> matches(request: DisplayRequest<T>): Boolean = true

        fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: DisplayRequest<T>): ViewGenerator<DEPENDENCY, VIEW>

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
        open fun matchesTyped(request: DisplayRequest<T>): Boolean = true
        abstract fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: DisplayRequest<T>): ViewGenerator<DEPENDENCY, VIEW>

        @Suppress("UNCHECKED_CAST")
        final override fun <T2> matches(request: DisplayRequest<T2>): Boolean = !request.type.isNullable && matchesTyped(request as DisplayRequest<T>)

        @Suppress("UNCHECKED_CAST")
        final override fun <T2, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: DisplayRequest<T2>): ViewGenerator<DEPENDENCY, VIEW> = generateTyped(request as DisplayRequest<T>)
    }

    abstract class BaseNullableTypeInterceptor<T : Any>(
            override val requiresType: KClass<T>,
            override val matchPriority: Float = 0f
    ) : Interceptor {
        open fun matchesTyped(request: DisplayRequest<T?>): Boolean = true
        abstract fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: DisplayRequest<T?>): ViewGenerator<DEPENDENCY, VIEW>

        @Suppress("UNCHECKED_CAST")
        final override fun <T2> matches(request: DisplayRequest<T2>): Boolean = matchesTyped(request as DisplayRequest<T?>)

        @Suppress("UNCHECKED_CAST")
        final override fun <T2, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: DisplayRequest<T2>): ViewGenerator<DEPENDENCY, VIEW> = generateTyped(request as DisplayRequest<T?>)
    }

    @Deprecated("x")
    data class DefaultInterceptor(
            override val requiresType: KClass<*>?,
            override val matchPriority: Float = 0f,
            val matches: (DisplayRequest<*>) -> Boolean = { true },
            val generate: (DisplayRequest<*>) -> ViewGenerator<ViewFactory<Any?>, Any?>
    ) : Interceptor {
        override fun <T> matches(request: DisplayRequest<T>): Boolean = this.matches.invoke(request)

        @Suppress("UNCHECKED_CAST")
        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: DisplayRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
            return this.generate.invoke(request) as ViewGenerator<DEPENDENCY, VIEW>
        }
    }

    @Deprecated("x")
    data class DirectInterceptor(
            override val requiresType: KClass<*>?,
            override val matchPriority: Float = 0f,
            val matches: (DisplayRequest<*>) -> Boolean = { true },
            val generate: ViewFactory<Any?>.(DisplayRequest<*>) -> Any?
    ) : Interceptor {
        override fun <T> matches(request: DisplayRequest<T>): Boolean = this.matches.invoke(request)

        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: DisplayRequest<T>): ViewGenerator<DEPENDENCY, VIEW> = object : ViewGenerator<DEPENDENCY, VIEW> {

            @Suppress("UNCHECKED_CAST")
            override fun generate(dependency: DEPENDENCY): VIEW = generate.invoke(dependency as ViewFactory<Any?>, request) as VIEW
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

        fun resolve(request: DisplayRequest<*>): Interceptor? {
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

    private val interceptors = AtomicReference(ViewEncoderDefaultModule)
    fun register(interceptors: Interceptors) {
        var start = this.interceptors.value
        var new = start + interceptors
        while (start != this.interceptors.value) {
            start = this.interceptors.value
            new = start + interceptors
        }
        this.interceptors.value = new
    }

    fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> getViewGenerator(request: DisplayRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
        return interceptors.value.resolve(request)?.generate(request)
                ?: throw IllegalArgumentException("No matching ViewGenerator was found.")
    }

    fun <T> getAnyViewGenerator(request: DisplayRequest<T>): ViewGenerator<ViewFactory<Any?>, Any?> {
        return interceptors.value.resolve(request)?.generate(request)
                ?: throw IllegalArgumentException("No matching ViewGenerator was found.")
    }


}
