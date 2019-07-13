package com.lightningkite.mirror.form

import com.lightningkite.kommon.collection.pop
import com.lightningkite.kommon.collection.push
import com.lightningkite.kommon.exception.stackTraceString
import com.lightningkite.kommon.string.Email
import com.lightningkite.kommon.string.Uri
import com.lightningkite.koolui.ExternalAccess
import com.lightningkite.koolui.Location
import com.lightningkite.koolui.async.UI

import com.lightningkite.koolui.builders.*
import com.lightningkite.koolui.concepts.Animation
import com.lightningkite.koolui.concepts.Importance
import com.lightningkite.koolui.concepts.NumberInputType
import com.lightningkite.koolui.concepts.TextInputType
import com.lightningkite.koolui.geometry.AlignPair
import com.lightningkite.koolui.image.MaterialIcon
import com.lightningkite.koolui.image.color
import com.lightningkite.koolui.image.withSizing
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.lokalize.location.Geohash
import com.lightningkite.lokalize.time.*
import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.archive.database.get
import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.breaker.Breaker
import com.lightningkite.mirror.form.form.*
import com.lightningkite.mirror.form.info.humanify
import com.lightningkite.mirror.form.info.numberInputType
import com.lightningkite.mirror.form.info.textInputType
import com.lightningkite.mirror.form.other.DatabaseVG
import com.lightningkite.mirror.info.*
import com.lightningkite.reacktive.list.MutableObservableList
import com.lightningkite.reacktive.list.MutableObservableListFromProperty
import com.lightningkite.reacktive.list.asObservableList
import com.lightningkite.reacktive.property.*
import com.lightningkite.reacktive.property.lifecycle.bind
import com.lightningkite.reacktive.property.lifecycle.listen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.StructureKind
import kotlinx.serialization.UnionKind
import mirror.kotlin.PairMirror
import kotlin.reflect.KClass


inline fun <reified T : Number> FormEncoder.Interceptors.integer(noinline toT: Long.() -> T, allowNegatives: Boolean = true) {
    this += object : FormEncoder.BaseNullableTypeInterceptor<T>(T::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<T?>): ViewGenerator<DEPENDENCY, VIEW> {
            return IntegerFormViewGenerator(
                    observable = request.observable,
                    toT = toT,
                    allowNull = request.type.isNullable,
                    allowNegatives = allowNegatives
            )
        }
    }
}
inline fun <reified T : Number> FormEncoder.Interceptors.number(noinline toT: Double.() -> T, allowNegatives: Boolean = true, decimalPlaces: Int = 2) {
    this += object : FormEncoder.BaseNullableTypeInterceptor<T>(T::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<T?>): ViewGenerator<DEPENDENCY, VIEW> {
            return NumberFormViewGenerator(
                    observable = request.observable,
                    toT = toT,
                    allowNull = request.type.isNullable,
                    decimalPlaces = decimalPlaces,
                    allowNegatives = allowNegatives
            )
        }
    }
}

val FormEncoderDefaultModule = FormEncoder.Interceptors().apply {

    integer(Long::toByte)
    integer(Long::toShort)
    integer(Long::toInt)
    integer(Long::toLong)

    number(Double::toFloat, decimalPlaces = 4)
    number(Double::toDouble, decimalPlaces = 4)

    this += object : FormEncoder.BaseNullableTypeInterceptor<Unit>(Unit::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<Unit?>): ViewGenerator<DEPENDENCY, VIEW> {
            request.observable.value = FormState.success(Unit)
            return ViewGenerator.empty()
        }
    }

    this += object : FormEncoder.BaseTypeInterceptor<Boolean>(Boolean::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<Boolean>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    toggle(request.observable.perfectNonNull(false))
                }
            }
        }
    }

    this += object : FormEncoder.BaseTypeInterceptor<String>(String::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<String>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    textField(
                            text = request.observable.perfectNonNull(""),
                            type = request.owningField?.textInputType ?: TextInputType.Sentence
                    )
                }
            }
        }
    }

    this += object : FormEncoder.BaseNullableTypeInterceptor<Char>(Char::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<Char?>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    textField(
                            text = request.observable.transform(
                                    mapper = { it.valueOrNull?.toString() ?: "" },
                                    reverseMapper = { FormState.success(it.firstOrNull()) }
                            ),
                            type = TextInputType.Name
                    )
                }
            }
        }
    }

    this += object : FormEncoder.BaseNullableTypeInterceptor<Email>(Email::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<Email?>): ViewGenerator<DEPENDENCY, VIEW> {
            return BackedByStringFormViewGenerator(
                    observable = request.observable,
                    toT = ::Email,
                    inputType = TextInputType.Email
            )
        }
    }

    this += object : FormEncoder.BaseNullableTypeInterceptor<Uri>(Uri::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<Uri?>): ViewGenerator<DEPENDENCY, VIEW> {
            return object: ViewGenerator<DEPENDENCY, VIEW> {
                val editor = BackedByStringFormViewGenerator<Uri, DEPENDENCY, VIEW>(
                        observable = request.observable,
                        toT = ::Uri,
                        inputType = TextInputType.URL
                )

                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency){
                    horizontal {
                        +editor.generate(dependency)
                        -imageButton(MaterialIcon.link.color(dependency.colorSet.foreground).withSizing(), "Test Link", Importance.Low){
                            request.observable.value.valueOrNull?.let {
                                ExternalAccess.openUri(it)
                            }
                        }
                    }
                }

            }
        }
    }

    this += object : FormEncoder.BaseTypeInterceptor<Date>(Date::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<Date>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    datePicker(request.observable.perfectNonNull(TimeStamp.now().date()))
                }
            }
        }
    }

    this += object : FormEncoder.BaseTypeInterceptor<Time>(Time::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<Time>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    timePicker(request.observable.perfectNonNull(TimeStamp.now().time()))
                }
            }
        }
    }

    this += object : FormEncoder.BaseTypeInterceptor<DateTime>(DateTime::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<DateTime>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    dateTimePicker(request.observable.perfectNonNull(TimeStamp.now().dateTime()))
                }
            }
        }
    }

    this += object : FormEncoder.BaseTypeInterceptor<TimeStamp>(TimeStamp::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<TimeStamp>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    dateTimePicker(request.observable.perfectNonNull(TimeStamp.now()).transform(
                            mapper = { it.dateTime() },
                            reverseMapper = { it.toTimeStamp() }
                    ))
                }
            }
        }
    }

    this += object : FormEncoder.BaseTypeInterceptor<Uuid>(Uuid::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<Uuid>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    button(
                            label = request.observable.perfectNonNull(Uuid.randomUUID4()).transform { it.toString() },
                            onClick = {
                                if (request.observable.value.valueOrNull == null) {
                                    request.observable.value = FormState.success(Uuid.randomUUID4())
                                } else {
                                    launchConfirmationDialog(message = "Do you want to regenerate this ID?") {
                                        request.observable.value = FormState.success(Uuid.randomUUID4())
                                    }
                                }
                            }
                    )
                }
            }
        }
    }

    this += object : FormEncoder.BaseTypeInterceptor<Geohash>(Geohash::class) {
        override fun matchesTyped(request: FormRequest<Geohash>): Boolean = request.scale >= ViewSize.Summary
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<Geohash>): ViewGenerator<DEPENDENCY, VIEW> {
            return GeohashFormVG(request.observable)
        }
    }


//    string(KClass::class) { it.type.localName.humanify() }
//    string(MirrorClass::class) { it.localName.humanify() }
//    string(MirrorClass.Field::class) { it.name }


    //Pair
    this += object : FormEncoder.BaseTypeInterceptor<Pair<Any?, Any?>>(Pair::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<Pair<Any?, Any?>>): ViewGenerator<DEPENDENCY, VIEW> {
            val form = PairFormViewGenerator.Form(request.observable)
            val type = request.type as PairMirror<Any?, Any?>
            return PairFormViewGenerator(
                    form = form,
                    subFirst = request.sub(type.AMirror, form.first).getVG(),
                    subSecond = request.sub(type.BMirror, form.second).getVG()
            )
        }
    }


    //List
    this += object : FormEncoder.BaseTypeInterceptor<List<Any?>>(List::class) {
        override fun matchesTyped(request: FormRequest<List<Any?>>): Boolean = request.scale >= ViewSize.Full
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<List<Any?>>): ViewGenerator<DEPENDENCY, VIEW> {
            val type = request.type as ListMirror<Any?>
            return ListFormViewGenerator(
                    stack = request.general.stack as MutableObservableList<ViewGenerator<DEPENDENCY, VIEW>>,
                    value = MutableObservableListFromProperty(request.observable.perfectNonNull(listOf())),
                    makeView = { request.display(scale = ViewSize.Full, observable = it, type = type.EMirror).getVG<DEPENDENCY, VIEW>().generate(this) },
                    editViewGenerator = { start, onResult ->
                        val obs = StandardObservableProperty<FormState<Any?>>(if (start == null) FormState.empty() else FormState.success(start))
                        val underlyingVg = request.sub(type = type.EMirror, observable = obs, scale = ViewSize.Full).getVG<DEPENDENCY, VIEW>()
                        FormViewGenerator(underlyingVg, obs) { onResult(it) }
                    }
            )
        }
    }

    //Map

    //Condition
    this += object : FormEncoder.BaseTypeInterceptor<Condition<Any?>>(Condition::class as KClass<Condition<Any?>>){
        override fun matchesTyped(request: FormRequest<Condition<Any?>>): Boolean = request.scale >= ViewSize.Full
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<Condition<Any?>>): ViewGenerator<DEPENDENCY, VIEW> {
            return ConditionFormVG(request.type as ConditionMirror<Any?>, request)
        }

    }

    //Reference
    this += object : FormEncoder.BaseNullableTypeInterceptor<Reference<*>>(Reference::class) {

        override fun matchesTyped(request: FormRequest<Reference<*>?>): Boolean {
            @Suppress("UNCHECKED_CAST") val t = ((request.type.base as ReferenceMirror<*>).MODELMirror as MirrorType<Any>).base as MirrorClass<Any>
            return request.general.databases.getOrNull(t.base) != null && request.scale > ViewSize.OneLine
        }

        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<Reference<*>?>): ViewGenerator<DEPENDENCY, VIEW> {
            @Suppress("UNCHECKED_CAST") val t = ((request.type.base as ReferenceMirror<*>).MODELMirror as MirrorType<Any>).base as MirrorClass<HasUuid>
            @Suppress("UNCHECKED_CAST") val idField = t.base.fields.find { it.name == "id" } as MirrorClass.Field<HasUuid, Uuid>
            @Suppress("UNCHECKED_CAST") val database = request.general.databases[t] as? Database<HasUuid>
                    ?: throw IllegalArgumentException()

            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    val loading = StandardObservableProperty(false)
                    val item = StandardObservableProperty<HasUuid?>(null)
                    work(
                            view = card(DisplayRequest(
                                    general = request.general,
                                    type = t.nullable,
                                    observable = item,
                                    clickable = false,
                                    scale = request.scale
                            ).getVG<DEPENDENCY, VIEW>().generate(dependency)).clickable {
                                val stack = request.general.stack<DEPENDENCY, VIEW>()
                                stack.push(
                                        DatabaseVG(
                                                stack = stack,
                                                type = t,
                                                database = database,
                                                generalRequest = request.general,
                                                onSelect = {
                                                    stack.pop()
                                                    request.observable.value = FormState.success(Reference<HasUuid>(it.id))
                                                }
                                        )
                                )
                            }.altClickable {
                                if (request.type.isNullable) {
                                    request.observable.value = FormState.success(null)
                                }
                            },
                            isWorking = loading
                    ).apply {
                        lifecycle.bind(request.observable) { formState ->
                            val ref = formState.valueOrNull
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


    //Enum
    this += object : FormEncoder.BaseInterceptor(matchPriority = 1f) {
        override fun <T> matches(request: FormRequest<T>): Boolean = request.type.base.enumValues != null

        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: FormRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
            val nnOptions = request.type.base.enumValues!!
            val options = if (request.type.isNullable) listOf(null) + nnOptions.toList() else nnOptions.toList()
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    @Suppress("UNCHECKED_CAST")
                    picker(
                            options = options.toList().asObservableList(),
                            selected = (request.observable as MutableObservableProperty<FormState<Any?>>).perfect(options.first()),
                            toString = {
                                ((it as? Enum<*>)?.name ?: it?.toString())?.humanify() ?: request.general.nullString
                            }
                    )
                }
            }
        }

    }

    //MirrorClass.Field
    this += object : FormEncoder.BaseNullableTypeInterceptor<MirrorClass.Field<*, *>>(MirrorClass.Field::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<MirrorClass.Field<*, *>?>): ViewGenerator<DEPENDENCY, VIEW> {
            val castType = request.type.base as MirrorClassFieldMirror<*, *>
            val allFields = castType.OwnerMirror.base.fields.toList() as List<MirrorClass.Field<*, *>>
            val nnOptions = allFields.filter { it.type isA castType.ValueMirror }
            val options = if (request.type.isNullable) listOf(null) + nnOptions.toList() else nnOptions.toList()
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    @Suppress("UNCHECKED_CAST")
                    picker(
                            options = options.asObservableList(),
                            selected = request.observable.perfect(options.first()),
                            toString = { it?.name?.humanify() ?: request.general.nullString }
                    )
                }
            }
        }
    }

    //MirrorType
    this += object : FormEncoder.BaseNullableTypeInterceptor<MirrorClass<*>>(MirrorClass::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<MirrorClass<*>?>): ViewGenerator<DEPENDENCY, VIEW> {
            val castType = request.type.base as MirrorClassMirror<*>
            val nnOptions = MirrorRegistry.allSatisfying(castType.TypeMirror)
            val options = if (request.type.isNullable) listOf(null) + nnOptions.toList() else nnOptions.toList()
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    @Suppress("UNCHECKED_CAST")
                    picker(
                            options = options.asObservableList(),
                            selected = request.observable.perfect(options.first()),
                            toString = { it?.localName?.humanify() ?: request.general.nullString }
                    )
                }
            }
        }
    }

    //Polymorphic
    this += object : FormEncoder.BaseInterceptor(matchPriority = .9f) {
        override fun <T> matches(request: FormRequest<T>): Boolean = request.type.kind == UnionKind.POLYMORPHIC
        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: FormRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
            return PolymorphicFormViewGenerator(request)
        }
    }

    //Nullable
    this += object : FormEncoder.BaseInterceptor(matchPriority = 0.1f) {
        override fun <T> matches(request: FormRequest<T>): Boolean = request.type.isNullable
        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: FormRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {

                @Suppress("UNCHECKED_CAST")
                val form = NullableForm(request.observable as MutableObservableProperty<FormState<Any?>>)

                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    horizontal {
                        -toggle(form.isNotNull)
                        +swap(form.isNotNull.transform {
                            @Suppress("UNCHECKED_CAST") val vg = if (it) {
                                request.sub(
                                        type = request.type.base as MirrorType<Any>,
                                        observable = form.value,
                                        scale = request.scale
                                ).getVG<DEPENDENCY, VIEW>()
                            } else object : ViewGenerator<DEPENDENCY, VIEW> {
                                override fun generate(dependency: DEPENDENCY): VIEW = dependency.text(
                                        text = request.general.nullString,
                                        importance = Importance.Low
                                )
                            }
                            vg.generate(dependency) to Animation.Fade
                        })
                    }.apply {
                        form.bind(lifecycle)
                    }
                }
            }
        }
    }

    //Reflective (no fields)
    this += object : FormEncoder.BaseInterceptor(matchPriority = 0f) {
        override fun <T> matches(request: FormRequest<T>): Boolean = (request.type.kind == StructureKind.CLASS || request.type.kind == UnionKind.OBJECT) && !request.type.isNullable && request.type.base.fields.isEmpty()
        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: FormRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
            request.observable.value = FormState.success(Breaker.fold(request.type, arrayOf()))
            return ViewGenerator.empty()
        }
    }

    //Reflective (1 field)
    this += object : FormEncoder.BaseInterceptor(matchPriority = 0f) {
        override fun <T> matches(request: FormRequest<T>): Boolean = request.type.kind == StructureKind.CLASS && !request.type.isNullable && request.type.base.fields.size == 1
        @Suppress("UNCHECKED_CAST")
        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: FormRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
            val mirrorClass = request.type.base as MirrorClass<Any>
            val field = mirrorClass.fields[0] as MirrorClass.Field<Any, Any?>
            return request.sub(
                    field.type,
                    request.observable.transform(
                            mapper = { it.map { field.get(it as Any) } },
                            reverseMapper = {
                                it.map { Breaker.fold(mirrorClass, arrayOf(it)) as T }
                            }
                    ),
                    scale = request.scale
            ).getVG()
        }
    }

    //Reflective (Many fields)
    this += object : FormEncoder.BaseInterceptor(matchPriority = 0f) {
        override fun <T> matches(request: FormRequest<T>): Boolean = request.type.kind == StructureKind.CLASS && !request.type.isNullable && request.type.base.fields.size > 1 && request.scale >= ViewSize.Full
        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(request: FormRequest<T>): ViewGenerator<DEPENDENCY, VIEW> {
            @Suppress("UNCHECKED_CAST")
            return ReflectiveFormViewGenerator(request as FormRequest<Any>)
        }
    }


    //Default to using a carded summary on smaller view sizes
    this += object : FormEncoder.BaseInterceptor() {
        override fun <T> matches(request: FormRequest<T>): Boolean = request.scale <= ViewSize.Summary

        @Suppress("UNCHECKED_CAST")
        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(
                request: FormRequest<T>
        ) = CardFormViewGenerator<T, DEPENDENCY, VIEW>(
                stack = request.general.stack as MutableObservableList<ViewGenerator<DEPENDENCY, VIEW>>,
                summaryVG = ViewEncoder.getViewGenerator(request.displayNullable()),
                request = request
        )
    }

    //Give up
    this += object : FormEncoder.BaseInterceptor(matchPriority = Float.NEGATIVE_INFINITY) {
        override fun <T> matches(request: FormRequest<T>): Boolean = true

        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(
                request: FormRequest<T>
        ) = object : ViewGenerator<DEPENDENCY, VIEW> {
            override fun generate(dependency: DEPENDENCY): VIEW = dependency.text(text = "No form generator found")
        }
    }
}


fun <T> MutableObservableProperty<FormState<T>>.perfect(default: T) = this.transform(
        mapper = {
            if (it is FormState.Success) it.value
            else default
        },
        reverseMapper = { FormState.success(it) }
)

fun <T> MutableObservableProperty<FormState<T>>.perfectNonNull(default: T): TransformMutableObservableProperty<FormState<T>, T> {
    if (this.value !is FormState.Success) {
        this.value = FormState.success(default)
    }
    return this.transform(
            mapper = { it.valueOrNull ?: default },
            reverseMapper = { FormState.success(it) }
    )
}

@Suppress("UNCHECKED_CAST")
fun <T> MutableObservableProperty<FormState<T?>>.nullIsEmpty() = this.transform(
        mapper = { if (it is FormState.Success && it.value == null) FormState.empty() else it as FormState<T> },
        reverseMapper = { it }
)

fun <T> MutableObservableProperty<FormState<T?>>.perfectNullable() = this.transform(
        mapper = { it.valueOrNull },
        reverseMapper = { FormState.success(it) }
)