package com.lightningkite.mirror.form

import com.lightningkite.kommon.string.Email
import com.lightningkite.kommon.string.Uri
import com.lightningkite.koolui.builders.horizontal
import com.lightningkite.koolui.builders.text
import com.lightningkite.koolui.concepts.Animation
import com.lightningkite.koolui.concepts.NumberInputType
import com.lightningkite.koolui.concepts.TextInputType
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.lokalize.time.*
import com.lightningkite.mirror.form.form.*
import com.lightningkite.mirror.info.*
import com.lightningkite.reacktive.list.MutableObservableList
import com.lightningkite.reacktive.list.MutableObservableListFromProperty
import com.lightningkite.reacktive.list.asObservableList
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.StandardObservableProperty
import com.lightningkite.reacktive.property.transform
import kotlinx.serialization.UnionKind
import mirror.kotlin.PairMirror


inline fun <reified T : Number> FormEncoder.Interceptors.number(noinline toT: Number.() -> T, inputType: NumberInputType, decimalPlaces: Int = 2) {
    this += object : FormEncoder.BaseNullableTypeInterceptor<T>(T::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<T?>): ViewGenerator<DEPENDENCY, VIEW> {
            return NumberFormViewGenerator(
                    observable = request.observable,
                    toT = toT,
                    allowNull = request.type.isNullable,
                    numberInputType = inputType,
                    decimalPlaces = decimalPlaces
            )
        }
    }
}

val FormEncoderDefaultModule = FormEncoder.Interceptors().apply {

    number(Number::toByte, NumberInputType.Integer, 0)
    number(Number::toShort, NumberInputType.Integer, 0)
    number(Number::toInt, NumberInputType.Integer, 0)
    number(Number::toLong, NumberInputType.Integer, 0)
    number(Number::toFloat, NumberInputType.Float, 4)
    number(Number::toDouble, NumberInputType.Float, 4)

    this += object : FormEncoder.BaseNullableTypeInterceptor<Unit>(Unit::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<Unit?>): ViewGenerator<DEPENDENCY, VIEW> {
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
                            type = TextInputType.Sentence
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

    this += object : FormEncoder.BaseNullableTypeInterceptor<Uri>(Uri::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<Uri?>): ViewGenerator<DEPENDENCY, VIEW> {
            return BackedByStringFormViewGenerator(
                    observable = request.observable,
                    toT = ::Uri,
                    inputType = TextInputType.URL
            )
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
                    subFirst = request.sub(type.AMirror, form.first.observable).getVG(),
                    subSecond = request.sub(type.BMirror, form.second.observable).getVG()
            )
        }
    }


    //List
    this += object : FormEncoder.BaseTypeInterceptor<List<Any?>>(List::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<List<Any?>>): ViewGenerator<DEPENDENCY, VIEW> {
            val type = request.type as ListMirror<Any?>
            return ListFormViewGenerator(
                    stack = request.general.stack as MutableObservableList<ViewGenerator<DEPENDENCY, VIEW>>,
                    value = MutableObservableListFromProperty(request.observable.perfectNonNull(listOf())),
                    makeView = { request.display(scale = ViewSize.Full, observable = it, type = type.EMirror).getVG<DEPENDENCY, VIEW>().generate(this) },
                    editViewGenerator = { start, onResult ->
                        val obs = StandardObservableProperty<FormState<Any?>>(if (start == null) FormState.empty() else FormState.success(start))
                        val underlyingVg = request.sub(type = type.EMirror, observable = obs, scale = ViewSize.Full).getVG<DEPENDENCY, VIEW>()
                        FormViewGenerator(underlyingVg, obs, onResult)
                    }
            )
        }
    }

    //Map


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
                            makeView = { itemObs ->
                                text(itemObs.transform {
                                    (it as? Enum<*>)?.name?.humanify() ?: request.general.nullString
                                })
                            }
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
                            makeView = { itemObs ->
                                text(itemObs.transform { it?.localName?.humanify() ?: request.general.nullString })
                            }
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
                            } else ViewGenerator.empty()
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
    //Reflective (1 field)
    //Reflective (Many fields)


    //Default to using a carded summary on smaller view sizes
    this += object : FormEncoder.BaseInterceptor() {
        override fun <T> matches(request: FormRequest<T>): Boolean = request.scale <= ViewSize.Summary

        override fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> generate(
                request: FormRequest<T>
        ) = CardFormViewGenerator<T, DEPENDENCY, VIEW>(
//                stack =
//                startValue = request.successfulValue,
                stack = request.general.stack as MutableObservableList<ViewGenerator<DEPENDENCY, VIEW>>,
                summaryVG = ViewEncoder.getViewGenerator(request.display(null as T)), //TODO: Handle null
                fullVG = { FormEncoder.getViewGenerator(request.copy(scale = ViewSize.Full)) }
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

fun <T> MutableObservableProperty<FormState<T>>.perfectNonNull(default: T) = this.transform(
        mapper = { it.valueOrNull ?: default },
        reverseMapper = { FormState.success(it) }
)

@Suppress("UNCHECKED_CAST")
fun <T> MutableObservableProperty<FormState<T?>>.nullIsEmpty() = this.transform(
        mapper = { if (it is FormState.Success && it.value == null) FormState.empty() else it as FormState<T> },
        reverseMapper = { it }
)

fun <T> MutableObservableProperty<FormState<T?>>.perfectNullable() = this.transform(
        mapper = { it.valueOrNull },
        reverseMapper = { FormState.success(it) }
)