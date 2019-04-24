package com.lightningkite.mirror.form

import com.lightningkite.koolui.builders.horizontal
import com.lightningkite.koolui.builders.space
import com.lightningkite.koolui.builders.text
import com.lightningkite.koolui.builders.vertical
import com.lightningkite.koolui.concepts.Animation
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.lokalize.DefaultLocale
import com.lightningkite.lokalize.Locale.Companion.default
import com.lightningkite.lokalize.time.Date
import com.lightningkite.lokalize.time.DateTime
import com.lightningkite.lokalize.time.Time
import com.lightningkite.lokalize.time.TimeStamp
import com.lightningkite.mirror.archive.model.Uuid
import com.lightningkite.mirror.form.view.*
import com.lightningkite.mirror.info.*
import com.lightningkite.reacktive.list.asObservableList
import com.lightningkite.reacktive.property.ConstantObservableProperty
import com.lightningkite.reacktive.property.ObservableProperty
import com.lightningkite.reacktive.property.transform
import kotlinx.serialization.UnionKind
import mirror.kotlin.PairMirror
import kotlin.reflect.KClass


val ViewEncoderDefaultModule = ViewEncoder.Interceptors().apply {

    fun <T : Any> string(
            type: KClass<T>,
            priority: Float = 0f,
            matches: (DisplayRequest<*>) -> Boolean = { true },
            toString: (T) -> String? = { it.toString() }
    ) {
        this += object : ViewEncoder.BaseInterceptor(type, priority) {
            override fun <T> matches(request: DisplayRequest<T>): Boolean = matches(request)

            @Suppress("UNCHECKED_CAST")
            override fun <T2, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: DisplayRequest<T2>): ViewGenerator<DEPENDENCY, VIEW> {
                return StringViewGenerator(
                        request = request,
                        toString = toString as (T2) -> String?
                )
            }
        }
    }

    string(Unit::class)
    string(Boolean::class)
    string(Byte::class)
    string(Short::class)
    string(Int::class)
    string(Long::class)
    string(Float::class)
    string(Double::class)
    string(Char::class)
    string(String::class)
    string(Uuid::class)

    string(Date::class) { DefaultLocale.renderDate(it) }
    string(DateTime::class) { DefaultLocale.renderDateTime(it) }
    string(Time::class) { DefaultLocale.renderTime(it) }
    string(TimeStamp::class) { DefaultLocale.renderTimeStamp(it) }

    string(KClass::class) { it.type.localName.humanify() }
    string(MirrorClass::class) { it.localName.humanify() }
    string(MirrorClass.Field::class) { it.name }

    this += object : ViewEncoder.BaseTypeInterceptor<Pair<Any?, Any?>>(Pair::class) {
        override fun matchesTyped(request: DisplayRequest<Pair<Any?, Any?>>): Boolean = request.scale >= ViewSize.Summary
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: DisplayRequest<Pair<Any?, Any?>>): ViewGenerator<DEPENDENCY, VIEW> {
            val type = request.type as PairMirror<Any?, Any?>
            return PairViewGenerator(
                    subFirst = request.sub(type.AMirror, request.observable.transform { it.first }).getVG(),
                    subSecond = request.sub(type.BMirror, request.observable.transform { it.second }).getVG()
            )
        }
    }
    this += object : ViewEncoder.BaseTypeInterceptor<Pair<Any?, Any?>>(Pair::class) {
        override fun matchesTyped(request: DisplayRequest<Pair<Any?, Any?>>): Boolean = request.scale < ViewSize.Summary
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: DisplayRequest<Pair<Any?, Any?>>): ViewGenerator<DEPENDENCY, VIEW> {
            val type = request.type as PairMirror<Any?, Any?>
            return PairSingleLineViewGenerator(
                    subFirst = request.sub(type.AMirror, request.observable.transform { it.first }).getVG(),
                    subSecond = request.sub(type.BMirror, request.observable.transform { it.second }).getVG()
            )
        }
    }

    this += object : ViewEncoder.BaseTypeInterceptor<List<Any?>>(List::class) {
        override fun matchesTyped(request: DisplayRequest<List<Any?>>): Boolean = request.scale == ViewSize.Full

        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: DisplayRequest<List<Any?>>): ViewGenerator<DEPENDENCY, VIEW> {
            val type = request.type as ListMirror<Any?>
            return ListViewGenerator<Any?, DEPENDENCY, VIEW>(
                    value = request.observable.asObservableList(),
                    viewGenerator = { request.sub(type.EMirror, it).getVG() }
            )
        }
    }
    this += object : ViewEncoder.BaseTypeInterceptor<List<Any?>>(List::class) {
        override fun matchesTyped(request: DisplayRequest<List<Any?>>): Boolean = request.scale < ViewSize.Full

        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: DisplayRequest<List<Any?>>): ViewGenerator<DEPENDENCY, VIEW> {
            val type = request.type as ListMirror<Any?>
            return ListSummaryViewGenerator<Any?, DEPENDENCY, VIEW>(
                    value = request.observable.asObservableList(),
                    lines = request.scale.maxLines(),
                    viewGenerator = { request.sub(type.EMirror, it).getVG() }
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    this += object : ViewEncoder.BaseTypeInterceptor<Map<Any?, Any?>>(Map::class as KClass<Map<Any?, Any?>>) {
        override fun matchesTyped(request: DisplayRequest<Map<Any?, Any?>>): Boolean = request.scale == ViewSize.Full

        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: DisplayRequest<Map<Any?, Any?>>): ViewGenerator<DEPENDENCY, VIEW> {
            //Defer maps to list of pairs
            val type = request.type as MapMirror<Any?, Any?>
            val transformed: ObservableProperty<List<Pair<Any?, Any?>>> = request.observable.transform { it.entries.map { it.toPair() } }
            return request.sub(ListMirror(PairMirror(type.KMirror, type.VMirror)), transformed, scale = request.scale).getVG()
        }
    }

    //Enums
    this += object : ViewEncoder.BaseInterceptor(matchPriority = .1f) {
        override fun <T> matches(request: DisplayRequest<T>): Boolean = request.type.base.enumValues != null

        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: DisplayRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
            return StringViewGenerator(
                    request = request,
                    toString = { (it as? Enum<*>)?.name?.humanify() }
            )
        }
    }

    //Null
    this += object : ViewEncoder.BaseInterceptor(matchPriority = -1f) {
        override fun <T> matches(request: DisplayRequest<T>): Boolean = request.type.isNullable
        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: DisplayRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    swap(request.observable.transform {
                        if (it == null) {
                            text(text = request.general.nullString, size = request.scale.textSize())
                        } else {
                            @Suppress("UNCHECKED_CAST") val underlying = request.sub<Any>(
                                    request.type.base as MirrorClass<Any>,
                                    ConstantObservableProperty<Any>(it),
                                    scale = request.scale
                            ).getVG<DEPENDENCY, VIEW>()
                            underlying.generate(dependency)
                        } to Animation.None
                    })
                }
            }
        }
    }

    //Reflective (No fields)
    this += object : ViewEncoder.BaseInterceptor(matchPriority = 0f) {
        override fun <T> matches(request: DisplayRequest<T>): Boolean = request.type.base.fields.isEmpty()
        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: DisplayRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = dependency.space(1f)
            }
        }
    }

    //Reflective (Single fields)
    this += object : ViewEncoder.BaseInterceptor(matchPriority = 0f) {
        override fun <T> matches(request: DisplayRequest<T>): Boolean = request.type.base.fields.size == 1
        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: DisplayRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
            @Suppress("UNCHECKED_CAST")
            val singleField = request.type.base.fields.first() as MirrorClass.Field<Any, Any?>
            val defer = DisplayRequest(
                    general = request.general,
                    type = singleField.type.nullable,
                    scale = request.scale,
                    owningField = singleField,
                    observable = request.observable.transform { (it as? Any)?.let(singleField.get) }
            )
            return defer.getVG()
        }
    }

    //Reflective (2+ fields, full)
    this += object : ViewEncoder.BaseInterceptor(matchPriority = 0f) {
        override fun <T> matches(request: DisplayRequest<T>): Boolean = !request.type.isNullable && request.type.base.fields.size >= 2 && request.scale == ViewSize.Full
        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: DisplayRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
            @Suppress("UNCHECKED_CAST")
            return ReflectiveViewGenerator(
                    request = request as DisplayRequest<Any>
            )
        }
    }

    //Reflective (2+ fields, small)
    this += object : ViewEncoder.BaseInterceptor(matchPriority = 0f) {
        override fun <T> matches(request: DisplayRequest<T>): Boolean = !request.type.isNullable && request.type.base.fields.size >= 2 && request.scale < ViewSize.Full
        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: DisplayRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
            @Suppress("UNCHECKED_CAST")
            return ReflectiveSummaryViewGenerator(
                    request = request as DisplayRequest<Any>,
                    fieldCount = request.scale.maxLines()
            )
        }
    }

    //Polymorphic
    this += object : ViewEncoder.BaseInterceptor(matchPriority = 0f) {
        override fun <T> matches(request: DisplayRequest<T>): Boolean = request.type.base.kind == UnionKind.POLYMORPHIC && request.scale >= ViewSize.Summary
        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: DisplayRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    vertical {
                        -text(
                                text = request.observable.transform {
                                    if (it == null) {
                                        request.type.base.localName.humanify()
                                    } else {
                                        (it as Any)::class.type.localName.humanify()
                                    }
                                },
                                size = request.scale.textSize()
                        )
                        -swap(request.observable.transform {
                            val vg: ViewGenerator<DEPENDENCY, VIEW> = if (it == null) {
                                ViewGenerator.empty()
                            } else {
                                @Suppress("UNCHECKED_CAST")
                                val requestWithType = request.sub<Any>(
                                        type = (it as Any)::class.type as MirrorType<Any>,
                                        observable = ConstantObservableProperty(it) as ObservableProperty<Any>,
                                        scale = request.scale
                                )
                                requestWithType.getVG()
                            }
                            vg.generate(dependency) to Animation.None
                        })
                    }
                }
            }
        }
    }

    //Polymorphic (single line)
    this += object : ViewEncoder.BaseInterceptor(matchPriority = 0f) {
        override fun <T> matches(request: DisplayRequest<T>): Boolean = request.type.base.kind == UnionKind.POLYMORPHIC && request.scale < ViewSize.Summary
        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: DisplayRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    horizontal {
                        -text(
                                text = request.observable.transform {
                                    if (it == null) {
                                        request.type.base.localName.humanify()
                                    } else {
                                        (it as Any)::class.type.localName.humanify()
                                    }
                                },
                                size = request.scale.textSize()
                        )
                        -swap(request.observable.transform {
                            val vg: ViewGenerator<DEPENDENCY, VIEW> = if (it == null) {
                                ViewGenerator.empty()
                            } else {
                                @Suppress("UNCHECKED_CAST")
                                val requestWithType = request.sub<Any>(
                                        type = (it as Any)::class.type as MirrorType<Any>,
                                        observable = ConstantObservableProperty(it) as ObservableProperty<Any>,
                                        scale = request.scale
                                )
                                requestWithType.getVG()
                            }
                            vg.generate(dependency) to Animation.None
                        })
                    }
                }
            }
        }
    }

//    default(null, matchPriority = 0f, matches = { it. }) { request ->
//        val successfulValue = request.successfulValue
//        if (successfulValue == null) {
//            @Suppress("UNCHECKED_CAST")
//            StringViewGenerator(request as Request<Any?>) { null }
//        } else {
//            val kclass = successfulValue::class
//            @Suppress("UNCHECKED_CAST")
//            val deferRequest = request.copy<Any?>(newType = kclass.type as MirrorType<Any?>, successfulValue = successfulValue)
//            ViewEncoder.getAnyViewGenerator(deferRequest)
//        }
//    }
}