package com.lightningkite.mirror.form

import com.lightningkite.kommon.atomic.AtomicReference
import com.lightningkite.kommon.collection.addSorted
import com.lightningkite.koolui.concepts.NumberInputType
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.mirror.info.IntMirror
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.MirrorType
import com.lightningkite.mirror.info.UnitMirror
import com.lightningkite.reacktive.property.StandardObservableProperty
import com.lightningkite.reacktive.property.TransformMutableObservableProperty
import kotlin.reflect.KClass

data class ViewRequest<V, T>(
        val factory: ViewFactory<V>,
        val type: MirrorType<T>,
        val showHidden: Boolean = false,
        val size: ViewSize = ViewSize.Full,
        val parentValue: Any? = null,
        val field: MirrorClass.Field<*, T>? = null,
        val importance: Float = field?.importance ?: .5f,
        val value: T? = null
)

object ViewEncoder2 {
    fun <V, T> generate(request: ViewRequest<V, T>): V {
        val direct = byType.value[request.type.base.kClass]
        return if (direct != null) {
            @Suppress("UNCHECKED_CAST")
            (direct as ForType<T>).encode(request)
        } else {
            val generated = backups.value.asSequence().mapNotNull { it.makeEncoderOrNull(request.type) }.firstOrNull()
                    ?: throw IllegalArgumentException("No form could be generated for type ${request.type}")
            generated.encode(request)
        }
    }


    interface ForType<T> {
        fun <V> encode(request: ViewRequest<V, T>): V
    }

    interface Backup : Comparable<Backup> {
        val priority: Double get() = 0.0
        override fun compareTo(other: Backup): Int = -this.priority.compareTo(other.priority)
        fun <T> makeEncoderOrNull(type: MirrorType<T>): ForType<T>?
    }

    val byType = AtomicReference<Map<KClass<*>, ForType<*>>>(mapOf())
    val backups = AtomicReference<List<Backup>>(listOf())
}

object FormEncoder2 {

    data class Form<V, T>(val view: V, val dump: () -> T?)

    fun <V, T> generate(request: ViewRequest<V, T>): Form<V, T> {
        val direct = byType.value[request.type]
        return if (direct != null) {
            @Suppress("UNCHECKED_CAST")
            (direct as ForType<T>).encode(request)
        } else {
            val generated = backups.value.asSequence().mapNotNull { it.makeEncoderOrNull(request.type) }.firstOrNull()
                    ?: throw IllegalArgumentException("No form could be generated for type ${request.type}")
            generated.encode(request)
        }
    }


    interface ForType<T> {
        fun <V> encode(request: ViewRequest<V, T>): Form<V, T>
    }

    interface Backup : Comparable<Backup> {
        val priority: Double get() = 0.0
        override fun compareTo(other: Backup): Int = -this.priority.compareTo(other.priority)
        fun <T> makeEncoderOrNull(type: MirrorType<T>): ForType<T>?
    }

    val byType = AtomicReference<Map<MirrorType<*>, ForType<*>>>(mapOf())
    val backups = AtomicReference<List<Backup>>(listOf())

    inline fun <T> register(type: MirrorType<T>, crossinline action: (request: ViewRequest<Any, T>) -> Form<Any, T>): ForType<T> {
        @Suppress("UNCHECKED_CAST")
        val forType = object : ForType<T> {
            override fun <V> encode(request: ViewRequest<V, T>): Form<V, T> {
                return action(request as ViewRequest<Any, T>) as Form<V, T>
            }
        }
        byType.value = byType.value.plus(type to forType)
        return forType
    }

    inline fun register(priority: Double, crossinline action: (type: MirrorType<*>) -> ForType<*>?): Backup {
        val backup = object : Backup {
            override val priority: Double
                get() = priority

            @Suppress("UNCHECKED_CAST")
            override fun <T> makeEncoderOrNull(type: MirrorType<T>): ForType<T>? = action(type) as ForType<T>?
        }
        backups.value = backups.value.toMutableList().apply { addSorted(backup) }
        return backup
    }

    init {
        register(IntMirror) { request ->
            val observable = StandardObservableProperty(request.value ?: 0)
            val view = request.factory.numberField(
                    value = TransformMutableObservableProperty(
                            observable = observable,
                            transformer = { it },
                            reverseTransformer = { it: Number? ->
                                it?.toInt() ?: request.value ?: 0
                            }
                    ),
                    type = NumberInputType.Integer,
                    placeholder = request.value.toString()
            )
            val dump = { observable.value }
            Form(
                    view = view,
                    dump = dump
            )
        }
    }
}