package com.lightningkite.mirror.form

import com.lightningkite.kommon.atomic.AtomicReference
import com.lightningkite.kommon.collection.addSorted
import com.lightningkite.koolui.builders.horizontal
import com.lightningkite.koolui.builders.linear
import com.lightningkite.koolui.builders.text
import com.lightningkite.koolui.builders.vertical
import com.lightningkite.koolui.concepts.Animation
import com.lightningkite.koolui.concepts.Importance
import com.lightningkite.koolui.concepts.TextSize
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.lokalize.DefaultLocale
import com.lightningkite.lokalize.time.Date
import com.lightningkite.lokalize.time.DateTime
import com.lightningkite.lokalize.time.Time
import com.lightningkite.lokalize.time.TimeStamp
import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.archive.model.Reference
import com.lightningkite.mirror.archive.model.ReferenceMirror
import com.lightningkite.mirror.archive.model.UuidMirror
import com.lightningkite.mirror.archive.property.SuspendProperty
import com.lightningkite.mirror.info.*
import com.lightningkite.mirror.request.Request
import com.lightningkite.mirror.request.RequestMirror
import com.lightningkite.reacktive.list.asObservableList
import com.lightningkite.reacktive.property.transform
import kotlinx.serialization.UnionKind
import mirror.kotlin.PairMirror
import kotlin.reflect.KClass

class ViewContext3<V>(
        val factory: ViewFactory<V>,
        val developerMode: Boolean = false
) {
    val nullString get() = if (developerMode) "<null>" else "N/A"

    data class Field<T>(
            val type: MirrorType<T>,
            val impliedSubfields: Collection<MirrorClass.Field<T, *>> = listOf(),
            val fromField: MirrorClass.Field<*, T>? = null,
            val size: ViewSize = ViewSize.Full,
            val importance: Importance = Importance.High,
            val value: T? = null
    ) {

        val textSize: TextSize
            get() = when (size) {
                ViewSize.Full -> TextSize.Header
                ViewSize.Summary -> when (importance) {
                    Importance.Low -> TextSize.Tiny
                    Importance.Normal -> TextSize.Body
                    Importance.High -> TextSize.Subheader
                    else -> TextSize.Subheader
                }
                ViewSize.OneLine -> when (importance) {
                    Importance.Low -> TextSize.Tiny
                    Importance.Normal -> TextSize.Tiny
                    Importance.High -> TextSize.Body
                    else -> TextSize.Body
                }
            }

        val maxLines: Int
            get() = when (size) {
                ViewSize.Full -> Int.MAX_VALUE
                ViewSize.Summary -> 3
                ViewSize.OneLine -> 1
            }

        fun <S> inherit(
                fromField: MirrorClass.Field<T, S>,
                value: S? = this.value?.let { fromField.get(it) },
                size: ViewSize = this.size.smaller,
                importance: Importance = this.importance.less
        ): Field<S> = Field<S>(
                fromField = fromField,
                value = value,
                type = fromField.type,
                size = size,
                importance = importance
        )

        fun <S> inherit(
                type: MirrorType<S>,
                value: S? = null,
                size: ViewSize = this.size.smaller,
                importance: Importance = this.importance.less
        ): Field<S> = Field<S>(
                fromField = null,
                value = value,
                type = type,
                size = size,
                importance = importance
        )
    }

    val stack = ArrayList<Field<*>>()
    val field get() = stack.last()
    inline fun <OUT> with(field: Field<*>, action: () -> OUT): OUT {
        stack.add(field)
        val result = action()
        stack.removeAt(stack.lastIndex)
        return result
    }
}

object ViewEncoder3 {

    interface ForType<T> {
        fun <V> encode(context: ViewContext3<V>, field: ViewContext3.Field<T>): V
    }

    interface Backup : Comparable<Backup> {
        val priority: Double get() = 0.0
        override fun compareTo(other: Backup): Int = -this.priority.compareTo(other.priority)
        fun <T> makeEncoderOrNull(type: MirrorType<T>): ForType<T>?
    }

    val byType = AtomicReference<Map<MirrorType<*>, ForType<*>>>(mapOf())
    val byKClass = AtomicReference<Map<KClass<*>, Backup>>(mapOf())
    val backups = AtomicReference<List<Backup>>(listOf())

    fun <T> register(type: MirrorType<T>, forType: ForType<T>) {
        byType.value = byType.value + (type to forType)
    }

    fun <T : Any> register(type: KClass<T>, backup: Backup) {
        byKClass.value = byKClass.value + (type to backup)
    }

    fun register(backup: Backup) {
        backups.value = backups.value.toMutableList().apply { addSorted(backup) }
    }

    @Suppress("UNCHECKED_CAST")
    fun <V> encode(context: ViewContext3<V>): V = encoder<Any?>(context.field.type as MirrorType<Any?>).encode(context, context.field as ViewContext3.Field<Any?>)

    fun <T> encoder(type: MirrorType<T>): ForType<T> {
        val direct = byType.value[type]
        return if (direct != null) {
            @Suppress("UNCHECKED_CAST")
            (direct as ForType<T>)
        } else {
            val generated = backups.value.asSequence().mapNotNull { it.makeEncoderOrNull(type) }.firstOrNull()
            if (generated != null) {
                byType.value = byType.value + (type to generated)
                generated
            } else {
                val generated2 = backups.value.asSequence().mapNotNull { it.makeEncoderOrNull(type) }.firstOrNull()
                        ?: throw IllegalArgumentException("No form could be generated for type ${type}")
                byType.value = byType.value + (type to generated2)
                generated2
            }
        }
    }

    fun <V, T> ViewFactory<V>.textForField(field: ViewContext3.Field<T>, text: String = ""): V {
        return text(
                text = text,
                size = field.textSize,
                importance = field.importance,
                maxLines = field.maxLines
        )
    }

    fun <T> string(convert: (T) -> String? = { it.toString() }): ForType<T> {
        return object : ForType<T> {
            override fun <V> encode(context: ViewContext3<V>, field: ViewContext3.Field<T>): V {
                return context.factory.text(
                        text = field.value?.let(convert) ?: context.nullString,
                        size = field.textSize,
                        importance = field.importance,
                        maxLines = field.maxLines
                )
            }
        }
    }

    fun <T> stringWithContext(convert: ViewContext3<*>.(T) -> String = { it.toString() }): ForType<T> {
        return object : ForType<T> {
            override fun <V> encode(context: ViewContext3<V>, field: ViewContext3.Field<T>): V {
                return context.factory.textForField(field, field.value?.let { convert(context, it) }
                        ?: context.nullString)
            }
        }
    }

    fun <TT> stringBackup(convert: (TT) -> String = { it.toString() }): Backup {
        return object : Backup {
            override fun <T> makeEncoderOrNull(type: MirrorType<T>): ForType<T>? = string {
                @Suppress("UNCHECKED_CAST")
                (it as TT).let(convert)
            }
        }
    }


    init {
        register(UnitMirror, string())
        register(BooleanMirror, string())
        register(ByteMirror, string())
        register(ShortMirror, string())
        register(IntMirror, string())
        register(LongMirror, string())
        register(FloatMirror, string())
        register(DoubleMirror, string())
        register(CharMirror, string())
        register(StringMirror, string())

        register(Date::class.type, string { DefaultLocale.renderDate(it) })
        register(DateTime::class.type, string { DefaultLocale.renderDateTime(it) })
        register(Time::class.type, string { DefaultLocale.renderTime(it) })
        register(TimeStamp::class.type, string { DefaultLocale.renderTimeStamp(it) })

        register(KClass::class, stringBackup<KClass<*>> { it.type.localName.humanify() })
        register(MirrorClass::class, stringBackup<MirrorClass<*>> { it.localName.humanify() })
        register(MirrorClass.Field::class, stringBackup<MirrorClass.Field<*, *>> { it.name })

        register(Pair::class, object : Backup {
            override fun <T> makeEncoderOrNull(type: MirrorType<T>): ForType<T>? = object : ForType<T> {
                val pairType = type as PairMirror<*, *>
                override fun <V> encode(context: ViewContext3<V>, field: ViewContext3.Field<T>): V {
                    return with(context.factory) {
                        frame(linear {
                            context.with(field.inherit(type = pairType.AMirror)) {
                                -encode(context)
                            }
                            context.with(field.inherit(type = pairType.BMirror)) {
                                -encode(context)
                            }
                        })
                    }
                }
            }
        })

        register(List::class, object : Backup {
            override fun <T> makeEncoderOrNull(type: MirrorType<T>): ForType<T>? = object : ForType<T> {
                val listType = type as ListMirror<*>
                override fun <V> encode(context: ViewContext3<V>, field: ViewContext3.Field<T>): V {
                    val value = field.value as List<Any?>
                    return with(context.factory) {
                        when (field.size) {
                            ViewSize.Full -> list(
                                    data = value.asObservableList(),
                                    makeView = { itemObs ->
                                        swap(itemObs.transform { value ->
                                            @Suppress("UNCHECKED_CAST")
                                            context.with(field.inherit(listType.EMirror as MirrorType<Any?>, value)) {
                                                encode(context) to Animation.Flip
                                            }
                                        })
                                    }
                            )
                            ViewSize.Summary -> frame(vertical {
                                value.asSequence().take(2).forEach { subvalue ->
                                    @Suppress("UNCHECKED_CAST")
                                    context.with(field.inherit(listType.EMirror as MirrorType<Any?>, subvalue)) {
                                        -encode(context)
                                    }
                                }
                                if (value.size > 2) {
                                    -text(text = "...")
                                }
                            })
                            ViewSize.OneLine -> frame(horizontal {
                                value.asSequence().take(2).forEach { subvalue ->
                                    @Suppress("UNCHECKED_CAST")
                                    context.with(field.inherit(listType.EMirror as MirrorType<Any?>, subvalue)) {
                                        -encode(context)
                                    }
                                }
                                if (value.size > 2) {
                                    -text(text = "...")
                                }
                            })
                        }
                    }
                }
            }
        })

        register(Map::class, object : Backup {
            override fun <T> makeEncoderOrNull(type: MirrorType<T>): ForType<T>? = object : ForType<T> {
                val mapType = type as MapMirror<*, *>
                override fun <V> encode(context: ViewContext3<V>, field: ViewContext3.Field<T>): V {
                    @Suppress("UNCHECKED_CAST") val value = (field.value as Map<Any?, Any?>).entries.map { it.toPair() }
                    @Suppress("UNCHECKED_CAST") val pairType = PairMirror(mapType.KMirror, mapType.VMirror) as PairMirror<Any?, Any?>
                    return with(context.factory) {
                        when (field.size) {
                            ViewSize.Full -> list(
                                    data = value.asObservableList(),
                                    makeView = { itemObs ->
                                        swap(itemObs.transform { value ->
                                            @Suppress("UNCHECKED_CAST")
                                            context.with(field.inherit(pairType, value)) {
                                                encode(context) to Animation.Flip
                                            }
                                        })
                                    }
                            )
                            ViewSize.Summary -> frame(vertical {
                                value.asSequence().take(2).forEach { subvalue ->
                                    @Suppress("UNCHECKED_CAST")
                                    context.with(field.inherit(pairType, subvalue)) {
                                        -encode(context)
                                    }
                                }
                                if (value.size > 2) {
                                    -text(text = "...")
                                }
                            })
                            ViewSize.OneLine -> frame(horizontal {
                                value.asSequence().take(2).forEach { subvalue ->
                                    @Suppress("UNCHECKED_CAST")
                                    context.with(field.inherit(pairType, subvalue)) {
                                        -encode(context)
                                    }
                                }
                                if (value.size > 2) {
                                    -text(text = "...")
                                }
                            })
                        }
                    }
                }
            }
        })

        register(object : Backup {
            override val priority: Double get() = 1.1
            override fun toString(): String = "enum"
            override fun <T> makeEncoderOrNull(type: MirrorType<T>): ForType<T>? {
                if (type.base.enumValues == null) return null
                return string {
                    (it as? Enum<*>)?.name?.humanify()
                }
            }
        })

        register(object : Backup {
            override val priority: Double get() = 1.0
            override fun toString(): String = "null"
            override fun <T> makeEncoderOrNull(type: MirrorType<T>): ForType<T>? {
                if (!type.isNullable) return null
                @Suppress("UNCHECKED_CAST")
                return encoder(type.base) as ForType<T>
            }
        })

        register(object : Backup {
            override val priority: Double get() = 1.1
            override fun toString(): String = "polymorphic"
            override fun <T> makeEncoderOrNull(type: MirrorType<T>): ForType<T>? {
                if (type.base.kind != UnionKind.POLYMORPHIC) return null
                return object : ForType<T> {
                    override fun <V> encode(context: ViewContext3<V>, field: ViewContext3.Field<T>): V {
                        val value = field.value
                        @Suppress("UNCHECKED_CAST")
                        return if (value == null) string<Any?> { null }.encode(context, field as ViewContext3.Field<Any?>)
                        else encoder((value as Any)::class.type as MirrorType<Any>).encode(context, field as ViewContext3.Field<Any>)
                    }
                }
            }
        })

        register(object : Backup {
            override val priority: Double get() = 0.0
            override fun toString(): String = "mirror"
            override fun <T> makeEncoderOrNull(type: MirrorType<T>): ForType<T>? {
                val rawType = type.base

                if (rawType.fields.isEmpty()) {
                    return string { type.base.localName.humanify() }
                }

                return object : ForType<T> {

                    val defaultSort = rawType.defaultSort

                    override fun <V> encode(context: ViewContext3<V>, field: ViewContext3.Field<T>): V = with(context.factory) {

                        val fieldsToRender = rawType.fields
                                .filter { context.developerMode || it.annotations.any { (it as? MirrorAnnotation)?.annotationType == Hidden::class } }
                                .filterIndexed { index, it -> index == 0 || !field.impliedSubfields.let { it as List<MirrorClass.Field<*, *>> }.contains(it) }

                        when (field.size) {
                            ViewSize.Full -> vertical {
                                for (subfield in fieldsToRender) {
                                    if (subfield.needsNoContext) {
                                        @Suppress("UNCHECKED_CAST")
                                        -context.with(field.inherit(subfield as MirrorClass.Field<T, Any?>)) {
                                            encode(context)
                                        }
                                    } else {
                                        @Suppress("UNCHECKED_CAST")
                                        entryContext(
                                                label = subfield.name.humanify(),
                                                field = context.with(field.inherit(subfield as MirrorClass.Field<T, Any?>)) {
                                                    encode(context)
                                                }
                                        )
                                    }
                                }
                            }
                            ViewSize.Summary -> vertical {
                                for (subfield in fieldsToRender.asSequence().take(3)) {
                                    if (subfield.needsNoContext) {
                                        @Suppress("UNCHECKED_CAST")
                                        -context.with(field.inherit(subfield as MirrorClass.Field<T, Any?>)) {
                                            encode(context)
                                        }
                                    } else {
                                        @Suppress("UNCHECKED_CAST")
                                        entryContext(
                                                label = subfield.name.humanify(),
                                                field = context.with(field.inherit(subfield as MirrorClass.Field<T, Any?>)) {
                                                    encode(context)
                                                }
                                        )
                                    }
                                }
                            }
                            ViewSize.OneLine -> {
                                @Suppress("UNCHECKED_CAST")
                                context.with(field.inherit(fieldsToRender.first() as MirrorClass.Field<T, Any?>)) {
                                    encode(context)
                                }
                            }
                        }
                    }
                }
            }
        })


        register(Request::class, object : Backup {
            override fun <T> makeEncoderOrNull(type: MirrorType<T>): ForType<T>? = object : ForType<T> {
                val mapType = type as RequestMirror<*>
                override fun <V> encode(context: ViewContext3<V>, field: ViewContext3.Field<T>): V {
                    return with(context.factory) {
                        when (field.size) {
                            ViewSize.Full -> TODO()
                            ViewSize.Summary -> TODO()
                            ViewSize.OneLine -> TODO()
                        }
                    }
                }
            }
        })
        register(SuspendProperty::class, object : Backup {
            override fun <T> makeEncoderOrNull(type: MirrorType<T>): ForType<T>? = object : ForType<T> {
                override fun <V> encode(context: ViewContext3<V>, field: ViewContext3.Field<T>): V {
                    return with(context.factory) {
                        when (field.size) {
                            ViewSize.Full -> TODO()
                            ViewSize.Summary -> TODO()
                            ViewSize.OneLine -> TODO()
                        }
                    }
                }
            }
        })
        register(Database::class, object : Backup {
            override fun <T> makeEncoderOrNull(type: MirrorType<T>): ForType<T>? = object : ForType<T> {
                override fun <V> encode(context: ViewContext3<V>, field: ViewContext3.Field<T>): V {
                    return with(context.factory) {
                        when (field.size) {
                            ViewSize.Full -> TODO()
                            ViewSize.Summary -> TODO()
                            ViewSize.OneLine -> TODO()
                        }
                    }
                }
            }
        })
        register(Reference::class, object : Backup {
            override fun <T> makeEncoderOrNull(type: MirrorType<T>): ForType<T>? = object : ForType<T> {
                val mapType = type as ReferenceMirror<*>
                override fun <V> encode(context: ViewContext3<V>, field: ViewContext3.Field<T>): V {
                    return with(context.factory) {
                        when (field.size) {
                            ViewSize.Full -> TODO()
                            ViewSize.Summary -> TODO()
                            ViewSize.OneLine -> TODO()
                        }
                    }
                }
            }
        })
        register(UuidMirror, string())
    }
}
//TODO: How to handle "related"?  Suppose I want all the deals a company owns.