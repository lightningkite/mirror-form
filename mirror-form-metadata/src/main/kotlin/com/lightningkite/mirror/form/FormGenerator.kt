package com.lightningkite.mirror.form

import com.lightningkite.koolui.builders.text
import com.lightningkite.koolui.concepts.TextInputType
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.lokalize.*
import com.lightningkite.mirror.info.FieldInfo
import com.lightningkite.mirror.info.Type
import com.lightningkite.mirror.info.type
import com.lightningkite.mirror.serialization.Encoder
import com.lightningkite.mirror.serialization.SerializationRegistry
import com.lightningkite.mirror.serialization.TypeEncoder
import com.lightningkite.reacktive.list.WrapperObservableList
import com.lightningkite.reacktive.property.StandardObservableProperty
import com.lightningkite.reacktive.property.TransformMutableObservableProperty
import com.lightningkite.reacktive.property.transform
import kotlin.reflect.KClass

class FormGenerator(override val registry: SerializationRegistry) : Encoder<FormGenerator.Output<Any?>> {
    override val arbitraryEncoders: MutableList<Encoder.Generator<Output<Any?>>> = ArrayList()
    override val encoders: MutableMap<Type<*>, TypeEncoder<Output<Any?>, Any?>> = HashMap()
    override val kClassEncoders: MutableMap<KClass<*>, (Type<*>) -> TypeEncoder<Output<Any?>, Any?>?> = HashMap()

    class Output<V>(
            val factory: ViewFactory<V>
    ) {
        var context: FieldInfo<*, *>? = null
        var output: V? = null
        var dump: ()->Any? = {Unit}
    }

    init {
        addEncoder(Unit::class.type){ value ->
            output = factory.text(text = "<nothing>")
            dump = {}
        }
        addEncoder(Boolean::class.type){ value ->
            val observable = StandardObservableProperty(value)
            output = factory.toggle(observable)
            dump = { observable.value }
        }
        addEncoder(Char::class.type){ value ->
            val observable = StandardObservableProperty(value.toString())
            output = factory.textField(
                    text = observable,
                    placeholder = value.toString(),
                    type = TextInputType.CapitalizedIdentifier
            )
            dump = {
                if(observable.value.isEmpty()){
                    value
                } else {
                    observable.value.first()
                }
            }
        }
        addEncoder(String::class.type){ value ->
            val observable = StandardObservableProperty(value)
            val context = context
            val subtype = context?.annotations?.find { it.name.endsWith("Subtype") }?.arguments?.firstOrNull()?.let{it as? String}?.toLowerCase() ?: when {
                context == null -> null
                context.name.contains("email", true) -> "email"
                context.name.contains("password", true) -> "password"
                context.name.contains("address", true) -> "address"
                context.name.contains("phone", true) -> "phone"
                context.name.contains("url", true) -> "url"
                context.name.contains("name", true) -> "name"
                context.name.contains("id", true) -> "id"
                else -> null
            }
            output = factory.textField(
                    text = observable,
                    placeholder = value,
                    type = when(subtype){
                        "password" -> TextInputType.Password
                        "email" -> TextInputType.Email
                        "address" -> TextInputType.Address
                        "phone" -> TextInputType.Phone
                        "url" -> TextInputType.URL
                        "name" -> TextInputType.Name
                        "id" -> TextInputType.CapitalizedIdentifier
                        else -> TextInputType.Sentence
                    }
            )
            dump = { observable.value }
        }
        addEncoder(Byte::class.type){ value ->
            val observable = StandardObservableProperty(value)
            output = factory.numberField(
                    value = TransformMutableObservableProperty(
                            observable = observable,
                            transformer = { it: Byte -> it },
                            reverseTransformer = { it: Number? ->
                                it?.toByte() ?: value
                            }
                    ),
                    placeholder = value.toString()
            )
            dump = { observable.value }
        }
        addEncoder(Short::class.type){ value ->
            val observable = StandardObservableProperty(value)
            output = factory.numberField(
                    value = TransformMutableObservableProperty(
                            observable = observable,
                            transformer = { it: Short -> it },
                            reverseTransformer = { it: Number? ->
                                it?.toShort() ?: value
                            }
                    ),
                    placeholder = value.toString()
            )
            dump = { observable.value }
        }
        addEncoder(Int::class.type){ value ->
            val observable = StandardObservableProperty(value)
            output = factory.numberField(
                    value = TransformMutableObservableProperty(
                            observable = observable,
                            transformer = { it: Int -> it },
                            reverseTransformer = { it: Number? ->
                                it?.toInt() ?: value
                            }
                    ),
                    placeholder = value.toString()
            )
            dump = { observable.value }
        }
        addEncoder(Long::class.type){ value ->
            val observable = StandardObservableProperty(value)
            output = factory.numberField(
                    value = TransformMutableObservableProperty(
                            observable = observable,
                            transformer = { it: Long -> it },
                            reverseTransformer = { it: Number? ->
                                it?.toLong() ?: value
                            }
                    ),
                    placeholder = value.toString()
            )
            dump = { observable.value }
        }
        addEncoder(Float::class.type){ value ->
            val observable = StandardObservableProperty(value)
            output = factory.numberField(
                    value = TransformMutableObservableProperty(
                            observable = observable,
                            transformer = { it: Float -> it },
                            reverseTransformer = { it: Number? ->
                                it?.toFloat() ?: value
                            }
                    ),
                    placeholder = value.toString()
            )
            dump = { observable.value }
        }
        addEncoder(Double::class.type){ value ->
            val observable = StandardObservableProperty(value)
            output = factory.numberField(
                    value = TransformMutableObservableProperty(
                            observable = observable,
                            transformer = { it: Double -> it },
                            reverseTransformer = { it: Number? ->
                                it?.toDouble() ?: value
                            }
                    ),
                    placeholder = value.toString()
            )
            dump = { observable.value }
        }
        addEncoder(Date::class.type){ value ->
            val observable = StandardObservableProperty(value)
            output = factory.datePicker(observable)
            dump = { observable.value }
        }
        addEncoder(Time::class.type){ value ->
            val observable = StandardObservableProperty(value)
            output = factory.timePicker(observable)
            dump = { observable.value }
        }
        addEncoder(DateTime::class.type){ value ->
            val observable = StandardObservableProperty(value)
            output = factory.dateTimePicker(observable)
            dump = { observable.value }
        }
        addEncoder(TimeStamp::class.type){ value ->
            val observable = StandardObservableProperty(value)
            output = factory.dateTimePicker(TransformMutableObservableProperty(
                    observable = observable,
                    transformer = { it: TimeStamp -> it.dateTime() },
                    reverseTransformer = { it: DateTime -> it.toTimeStamp() }
            ))
            dump = { observable.value }
        }
        addEncoder(object : Encoder.Generator<FormGenerator.Output<Any?>>{
            override val description: String = "enum"
            override val priority: Float get() = .9f

            override fun generateEncoder(type: Type<*>): TypeEncoder<Output<Any?>, Any?>? {
                val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
                val enumValues = classInfo.enumValues ?: return null

                return { value ->
                    val observable = StandardObservableProperty(value as Any)
                    factory.picker(
                            options = WrapperObservableList(enumValues.toMutableList()),
                            selected = observable,
                            makeView = {
                                factory.text(text = observable.transform { (it as Enum<*>).name.humanify() })
                            }
                    )
                }
            }
        })
    }
}