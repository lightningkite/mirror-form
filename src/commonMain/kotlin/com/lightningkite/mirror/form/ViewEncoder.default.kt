package com.lightningkite.mirror.form

import com.lightningkite.kommon.exception.stackTraceString
import com.lightningkite.kommon.string.Email
import com.lightningkite.kommon.string.Uri
import com.lightningkite.koolui.*
import com.lightningkite.koolui.async.UI

import com.lightningkite.koolui.builders.*
import com.lightningkite.koolui.concepts.Animation
import com.lightningkite.koolui.concepts.Importance
import com.lightningkite.koolui.image.*
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.lokalize.DefaultLocale
import com.lightningkite.lokalize.location.Geohash
import com.lightningkite.lokalize.time.*
import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.form.info.humanify
import com.lightningkite.mirror.form.view.*
import com.lightningkite.mirror.info.*
import com.lightningkite.reacktive.list.asObservableList
import com.lightningkite.reacktive.property.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    fun <T : Any> stringWithClick(
            type: KClass<T>,
            priority: Float = 0f,
            matches: (DisplayRequest<*>) -> Boolean = { true },
            toString: (T) -> String? = { it.toString() },
            onClick: (T) -> Unit
    ) {
        this += object : ViewEncoder.BaseInterceptor(type, priority) {
            override fun <T> matches(request: DisplayRequest<T>): Boolean = matches(request)

            @Suppress("UNCHECKED_CAST")
            override fun <T2, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: DisplayRequest<T2>): ViewGenerator<DEPENDENCY, VIEW> {
                return object : ViewGenerator<DEPENDENCY, VIEW> {
                    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                        StringViewGenerator<T2, DEPENDENCY, VIEW>(
                                request = request,
                                toString = toString as (T2) -> String?
                        ).generate(dependency).clickable {
                            onClick(request.observable.value as T)
                        }
                    }
                }
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

    stringWithClick(Email::class) {
        ExternalAccess.openUri(Uri("mailto:${it.string}"))
    }
    stringWithClick(Uri::class) {
        ExternalAccess.openUri(it)
    }
    this += object : ViewEncoder.BaseTypeInterceptor<Uri>(Uri::class, matchPriority = 1f) {
        override fun matchesTyped(request: DisplayRequest<Uri>): Boolean = request.owningField?.annotations?.any {
            (it as? MirrorAnnotation)?.annotationType == ImageUri::class || it is ImageUri
        } == true
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: DisplayRequest<Uri>): ViewGenerator<DEPENDENCY, VIEW> = ViewGenerator.make("Image") {
            loadingImage(request.observable) {
                (Image.fromUrl(it) ?: Image.blank).let {
                    when (request.scale) {
                        ViewSize.OneLine -> it.withOptions(scaleType = ImageScaleType.Crop)
                        ViewSize.Summary -> it.withOptions(scaleType = ImageScaleType.Crop)
                        ViewSize.Full -> it.withOptions(scaleType = ImageScaleType.Fill)
                    }
                }
            }.let {
                when (request.scale) {
                    ViewSize.OneLine -> it.setHeight(40f)
                    ViewSize.Summary -> it.setHeight(100f)
                    ViewSize.Full -> it
                }
            }
        }
    }

    string(Date::class) { DefaultLocale.renderDate(it) }
    string(DateTime::class) { DefaultLocale.renderDateTime(it) }
    string(Time::class) { DefaultLocale.renderTime(it) }
    string(TimeStamp::class) { DefaultLocale.renderTimeStamp(it) }
    string(DaysOfWeek::class)

    string(KClass::class) { it.type.localName.humanify() }
    string(MirrorType::class) { it.base.localName.humanify() + if (it.isNullable) " (Optional)" else "" }
    string(MirrorClass::class) { it.localName.humanify() }
    string(MirrorClass.Field::class) { it.name }

    this += object : ViewEncoder.BaseTypeInterceptor<Geohash>(Geohash::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: DisplayRequest<Geohash>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                val geocoded = StandardObservableProperty("")
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    horizontal {
                        +text(
                                text = CombineObservableProperty2(request.observable, geocoded) { hash, coded ->
                                    if (coded.isNotEmpty()) {
                                        coded
                                    } else {
                                        "${hash.latitude}, ${hash.longitude}"
                                    }
                                }
                        ).apply {
                            lifecycle.bind(request.observable) {
                                scope.launch(Dispatchers.UI) {
                                    var lastValue: Geohash? = it
                                    while (lastValue != request.observable.value) {
                                        lastValue = request.observable.value
                                        delay(1000)
                                    }
                                    geocoded.value = Location.getAddress(request.observable.value) ?: ""
                                }
                            }
                        }
                        -imageButton(MaterialIcon.map.color(colorSet.foreground).withOptions(), "Open Map", importance = Importance.Low) {
                            ExternalAccess.openGeohash(request.observable.value)
                        }
                    }

                }
            }
        }
    }


    this += object : ViewEncoder.BaseTypeInterceptor<ClosedRange<*>>(ClosedRange::class) {
        override fun matchesTyped(request: DisplayRequest<ClosedRange<*>>): Boolean = request.scale >= ViewSize.Summary
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: DisplayRequest<ClosedRange<*>>): ViewGenerator<DEPENDENCY, VIEW> {
            val type = request.type as ClosedRangeMirror<*>
            @Suppress("UNCHECKED_CAST")
            return RangeViewGenerator(
                    subFirst = request.sub<Any?>(type.TMirror as MirrorType<Any?>, request.observable.transform { it.start }).getVG(),
                    subSecond = request.sub<Any?>(type.TMirror as MirrorType<Any?>, request.observable.transform { it.endInclusive }).getVG()
            )
        }
    }
    this += object : ViewEncoder.BaseTypeInterceptor<ClosedRange<*>>(ClosedRange::class) {
        override fun matchesTyped(request: DisplayRequest<ClosedRange<*>>): Boolean = request.scale < ViewSize.Summary
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: DisplayRequest<ClosedRange<*>>): ViewGenerator<DEPENDENCY, VIEW> {
            val type = request.type as ClosedRangeMirror<*>
            @Suppress("UNCHECKED_CAST")
            return RangeSingleLineViewGenerator(
                    subFirst = request.sub<Any?>(type.TMirror as MirrorType<Any?>, request.observable.transform { it.start }).getVG(),
                    subSecond = request.sub<Any?>(type.TMirror as MirrorType<Any?>, request.observable.transform { it.endInclusive }).getVG()
            )
        }
    }
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

    //Reference
    this += object : ViewEncoder.BaseNullableTypeInterceptor<Reference<*>>(Reference::class) {

        override fun matchesTyped(request: DisplayRequest<Reference<*>?>): Boolean {
            @Suppress("UNCHECKED_CAST") val t = ((request.type.base as ReferenceMirror<*>).MODELMirror as MirrorType<Any>).base as MirrorClass<Any>
            return request.general.databases.getOrNull(t.base) != null && request.scale > ViewSize.OneLine
        }

        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: DisplayRequest<Reference<*>?>): ViewGenerator<DEPENDENCY, VIEW> {
            @Suppress("UNCHECKED_CAST") val t = ((request.type.base as ReferenceMirror<*>).MODELMirror as MirrorType<Any>).base as MirrorClass<HasUuid>
            @Suppress("UNCHECKED_CAST") val idField = t.base.fields.find { it.name == "id" } as MirrorClass.Field<HasUuid, Uuid>
            @Suppress("UNCHECKED_CAST") val database = request.general.databases.getOrNull(t) as? Database<HasUuid>
                    ?: throw IllegalArgumentException()

            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    val loading = StandardObservableProperty(false)
                    val item = StandardObservableProperty<HasUuid?>(null)
                    work(
                            view = card(request.sub(
                                    type = t.nullable,
                                    observable = item,
                                    scale = request.scale
                            ).getVG<DEPENDENCY, VIEW>().generate(dependency)),
                            isWorking = loading
                    ).apply {
                        lifecycle.bind(request.observable) { ref ->
                            if (ref == null) {
                                item.value = null
                            } else {
                                scope.launch(Dispatchers.UI) {
                                    loading.value = true
                                    item.value = try {
                                        @Suppress("UNCHECKED_CAST")
                                        (ref as Reference<HasUuid>).resolve(t, request.general.databases, request.general.subgraph)
                                    } catch (t: Throwable) {
                                        //TODO: Maybe a failed-to-load message?
                                        println(t.stackTraceString())
                                        null
                                    }
                                    loading.value = false
                                }
                            }
                            Unit
                        }
                        Unit
                    }
                }
            }
        }
    }

    //Reference (one line)
    this += object : ViewEncoder.BaseNullableTypeInterceptor<Reference<*>>(Reference::class) {

        override fun matchesTyped(request: DisplayRequest<Reference<*>?>): Boolean {
            @Suppress("UNCHECKED_CAST") val t = ((request.type.base as ReferenceMirror<*>).MODELMirror as MirrorType<Any>).base as MirrorClass<Any>
            return request.general.databases.getOrNull(t.base) != null && request.scale == ViewSize.OneLine
        }

        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: DisplayRequest<Reference<*>?>): ViewGenerator<DEPENDENCY, VIEW> {
            @Suppress("UNCHECKED_CAST") val t = ((request.type.base as ReferenceMirror<*>).MODELMirror as MirrorType<Any>).base as MirrorClass<HasUuid>
            @Suppress("UNCHECKED_CAST") val idField = t.base.fields.find { it.name == "id" } as MirrorClass.Field<HasUuid, Uuid>
            @Suppress("UNCHECKED_CAST") val database = request.general.databases[t] as Database<HasUuid>

            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    val loading = StandardObservableProperty(false)
                    val item = StandardObservableProperty<HasUuid?>(null)
                    work(
                            view = request.sub(
                                    type = t.nullable,
                                    observable = item,
                                    scale = request.scale
                            ).getVG<DEPENDENCY, VIEW>().generate(dependency),
                            isWorking = loading
                    ).apply {
                        lifecycle.bind(request.observable) { ref ->
                            if (ref == null) {
                                item.value = null
                            } else {
                                scope.launch(Dispatchers.UI) {
                                    loading.value = true
                                    item.value = try {
                                        @Suppress("UNCHECKED_CAST")
                                        (ref as Reference<HasUuid>).resolve(t, request.general.databases, request.general.subgraph)
                                    } catch (t: Throwable) {
                                        //TODO: Maybe a failed-to-load message?
                                        println(t.stackTraceString())
                                        null
                                    }
                                    loading.value = false
                                }
                            }
                            Unit
                        }
                        Unit
                    }
                }
            }
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

    //Reflective (Single field)
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

    //Reflective (one-line)
    this += object : ViewEncoder.BaseNotNullInterceptor(matchPriority = 0f) {
        override fun <T : Any> matchesNotNull(request: DisplayRequest<T>): Boolean = request.type.base.fields.size >= 2 && request.scale == ViewSize.OneLine

        override fun <T : Any, DEPENDENCY : ViewFactory<VIEW>, VIEW> generateNotNull(request: DisplayRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
            @Suppress("UNCHECKED_CAST")
            val singleField = (request.type as MirrorClass<T>).pickDisplayFields(request).first()
            @Suppress("UNCHECKED_CAST")
            val defer = DisplayRequest(
                    general = request.general,
                    type = singleField.type.nullable as MirrorType<Any?>,
                    scale = request.scale,
                    owningField = singleField,
                    observable = request.observable.transform { singleField.get(it) }
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
        override fun <T> matches(request: DisplayRequest<T>): Boolean = !request.type.isNullable && request.type.base.fields.size >= 2 && request.scale == ViewSize.Summary
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
                                        MirrorRegistry.retrieve(it).localName.humanify()
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
                                        type = MirrorRegistry.retrieve(it) as MirrorType<Any>,
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
                                        MirrorRegistry.retrieve(it).localName.humanify()
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
                                        type = MirrorRegistry.retrieve(it) as MirrorType<Any>,
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

    //Give up
    this += object : ViewEncoder.BaseInterceptor(matchPriority = Float.NEGATIVE_INFINITY) {
        override fun <T> matches(request: DisplayRequest<T>): Boolean = true

        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(
                request: DisplayRequest<T>
        ) = object : ViewGenerator<DEPENDENCY, VIEW> {
            override fun generate(dependency: DEPENDENCY): VIEW = dependency.text(text = "No form generator found")
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