package com.lightningkite.mirror.form

import com.lightningkite.kommon.string.Email
import com.lightningkite.kommon.string.Uri
import com.lightningkite.koolui.concepts.NumberInputType
import com.lightningkite.koolui.concepts.TextInputType
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.lokalize.time.*
import com.lightningkite.mirror.form.form.*
import com.lightningkite.mirror.form.view.PairViewGenerator
import com.lightningkite.reacktive.list.MutableObservableList
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.StandardObservableProperty
import com.lightningkite.reacktive.property.lifecycle.listen
import com.lightningkite.reacktive.property.transform
import mirror.kotlin.PairMirror


val FormEncoderDefaultModule = FormEncoder.Interceptors().apply {

    inline fun <reified T : Number> number(noinline toT: Number.() -> T, inputType: NumberInputType, decimalPlaces: Int = 2) {
        this += object : FormEncoder.BaseNullableTypeInterceptor<T>(T::class) {
            override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<T?>): ViewGenerator<DEPENDENCY, VIEW> {
                return NumberFormViewGenerator(
                        observable = request.observable,
                        toT = toT,
                        numberInputType = inputType,
                        decimalPlaces = decimalPlaces
                )
            }
        }
    }

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
                    toggle(request.observable.perfectNonNull())
                }
            }
        }
    }

    this += object : FormEncoder.BaseTypeInterceptor<String>(String::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<String>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    textField(
                            text = request.observable.perfectNonNull(),
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
                    datePicker(request.observable.perfectNonNull())
                }
            }
        }
    }

    this += object : FormEncoder.BaseTypeInterceptor<Time>(Time::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<Time>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    timePicker(request.observable.perfectNonNull())
                }
            }
        }
    }

    this += object : FormEncoder.BaseTypeInterceptor<DateTime>(DateTime::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<DateTime>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    dateTimePicker(request.observable.perfectNonNull())
                }
            }
        }
    }

    this += object : FormEncoder.BaseTypeInterceptor<TimeStamp>(TimeStamp::class) {
        override fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> generateTyped(request: FormRequest<TimeStamp>): ViewGenerator<DEPENDENCY, VIEW> {
            return object : ViewGenerator<DEPENDENCY, VIEW> {
                override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
                    dateTimePicker(request.observable.perfectNonNull().transform(
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
    //Map
    //Enum
    //Polymorphic
    //Nullable
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
                fullVG = FormEncoder.getViewGenerator(request.copy(scale = ViewSize.Full))
        )
    }
}

fun <T> MutableObservableProperty<FormState<T>>.perfectNonNull() = this.transform(
        mapper = { it.valueOrNull!! },
        reverseMapper = { FormState.success(it) }
)

fun <T> MutableObservableProperty<FormState<T?>>.perfectNullable() = this.transform(
        mapper = { it.valueOrNull },
        reverseMapper = { FormState.success(it) }
)