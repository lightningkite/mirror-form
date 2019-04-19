package com.lightningkite.mirror.form

import com.lightningkite.kommon.collection.popFrom
import com.lightningkite.kommon.collection.push
import com.lightningkite.koolui.builders.*
import com.lightningkite.koolui.color.Color
import com.lightningkite.koolui.concepts.Animation
import com.lightningkite.koolui.concepts.TextInputType
import com.lightningkite.koolui.concepts.TextSize
import com.lightningkite.koolui.image.MaterialIcon
import com.lightningkite.koolui.image.asImage
import com.lightningkite.koolui.image.color
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.lokalize.*
import com.lightningkite.lokalize.time.*
import com.lightningkite.mirror.archive.database.SuspendMapRegistry
import com.lightningkite.mirror.archive.model.Reference
import com.lightningkite.mirror.info.*
import com.lightningkite.mirror.request.RequestHandler
import com.lightningkite.mirror.serialization.Encoder
import com.lightningkite.mirror.serialization.SerializationRegistry
import com.lightningkite.mirror.serialization.TypeEncoder
import com.lightningkite.reacktive.list.MutableObservableList
import com.lightningkite.reacktive.list.StandardObservableList
import com.lightningkite.reacktive.list.WrapperObservableList
import com.lightningkite.reacktive.list.asObservableList
import com.lightningkite.reacktive.property.StandardObservableProperty
import com.lightningkite.reacktive.property.TransformMutableObservableProperty
import com.lightningkite.reacktive.property.transform
import kotlin.reflect.KClass

class FormEncoder(
        override val registry: SerializationRegistry,
        val suspendMaps: SuspendMapRegistry,
        val requestHandler: RequestHandler
) : Encoder<FormEncoder.Builder<Any?>> {
    override val arbitraryEncoders: MutableList<Encoder.Generator<Builder<Any?>>> = ArrayList()
    override val encoders: MutableMap<Type<*>, TypeEncoder<Builder<Any?>, Any?>> = HashMap()
    override val kClassEncoders: MutableMap<KClass<*>, (Type<*>) -> TypeEncoder<Builder<Any?>, Any?>?> = HashMap()

    val viewEncoder = ViewEncoder(registry, suspendMaps, requestHandler)

    class Builder<VIEW>(
            factory: ViewFactory<VIEW>,
            stack: MutableObservableList<ViewGenerator<ViewFactory<VIEW>, VIEW>> = StandardObservableList(),
            showHidden: Boolean = false,
            startContext: ViewContext = ViewContext(),
            val dataRepository: HashMap<String, Any?> = HashMap()
    ) : ViewEncoder.Builder<VIEW>(
            factory = factory,
            stack = stack,
            showHidden = showHidden,
            startContext = startContext
    ) {
        override val detailOnClick: Boolean get() = false

        @Suppress("UNCHECKED_CAST")
        fun <T> data(
                keyAddition: String = "",
                make: () -> T
        ): T = dataRepository.getOrPut(contexts.joinToString("") {
            it.fieldInfo?.name ?: "_"
        } + keyAddition, make) as T

        var dump: () -> Any? = { Unit }
        fun copy() = Builder(
                factory = factory,
                stack = stack,
                showHidden = showHidden,
                startContext = context,
                dataRepository = dataRepository
        )
    }

    fun buttonToFullEdit(builder: Builder<Any?>, value: Any?, type: Type<*>) = with(builder) {
        val obs = data { StandardObservableProperty(value) }
        view = with(factory) {
            @Suppress("UNCHECKED_CAST")
            card(swap(obs.transform {
                build(viewEncoder, it, type as Type<Any?>).clickable {
                    stack.push(FormVG(
                            formEncoder = this@FormEncoder,
                            stack = stack,
                            value = obs.value,
                            type = type,
                            onComplete = {
                                obs.value = it
                                stack.popFrom(this@FormVG)
                            }
                    ))
                } to Animation.None
            }))
        }
        dump = {
            if (type.nullable || obs.value != null) {
                obs.value
            } else {
                throw IncompleteException("${builder.context.fieldInfo?.name?.humanify() ?: ""} must be filled out.")
            }
        }
    }

    class Output<VIEW, VALUE>(
            val view: VIEW,
            val dump: () -> VALUE
    )

    fun <VIEW, VALUE> write(
            dataRepository: HashMap<String, Any?>,
            factory: ViewFactory<VIEW>,
            stack: MutableObservableList<ViewGenerator<ViewFactory<VIEW>, VIEW>>,
            type: Type<VALUE>,
            value: VALUE
    ): Output<VIEW, VALUE> {
        val builder: Builder<VIEW> = Builder(factory, stack, dataRepository = dataRepository)
        val builderRef = builder
        @Suppress("UNCHECKED_CAST")
        encode(builderRef as Builder<Any?>, value, type)
        @Suppress("UNCHECKED_CAST")
        return Output(view = builder.view!!, dump = builder.dump as () -> VALUE)
    }


    class IncompleteException(message: String) : Exception(message)

    fun <T> addCanBeNullEncoder(type: Type<T>, action: TypeEncoder<Builder<Any?>, T?>) {
        @Suppress("UNCHECKED_CAST")
        encoders[type] = action as Builder<Any?>.(value: Any?) -> Unit
    }

    init {
        addCanBeNullEncoder(Unit::class.type) { value ->
            view = factory.text(text = "<nothing>")
            dump = {}
        }
        addCanBeNullEncoder(Boolean::class.type) { value ->
            val observable = data { StandardObservableProperty(value ?: false) }
            view = factory.toggle(observable)
            dump = { observable.value }
        }
        addCanBeNullEncoder(Char::class.type) { value ->
            val observable = data { StandardObservableProperty(value.toString()) }
            view = factory.textField(
                    text = observable,
                    placeholder = value.toString(),
                    type = TextInputType.CapitalizedIdentifier
            )
            dump = {
                if (observable.value.isEmpty()) {
                    value
                } else {
                    observable.value.first()
                }
            }
        }
        addCanBeNullEncoder(String::class.type) { value ->
            val observable = data { StandardObservableProperty(value ?: "") }
            val context = context
            val field = context.fieldInfo
            val subtype = field?.annotations?.find { it.name.endsWith("Subtype") }?.arguments?.firstOrNull()?.let { it as? String }?.toLowerCase()
                    ?: when {
                        field == null -> null
                        field.name.contains("email", true) -> "email"
                        field.name.contains("password", true) -> "password"
                        field.name.contains("address", true) -> "address"
                        field.name.contains("phone", true) -> "phone"
                        field.name.contains("url", true) -> "url"
                        field.name.contains("name", true) -> "name"
                        field.name.contains("id", true) -> "id"
                        else -> null
                    }
            view = factory.textField(
                    text = observable,
                    placeholder = value ?: "",
                    type = when (subtype) {
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
        addCanBeNullEncoder(Byte::class.type) { valueNullable ->
            val value = valueNullable ?: 0
            val observable = data { StandardObservableProperty(value) }
            view = factory.numberField(
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
        addCanBeNullEncoder(Short::class.type) { valueNullable ->
            val value = valueNullable ?: 0
            val observable = data { StandardObservableProperty(value) }
            view = factory.numberField(
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
        addCanBeNullEncoder(Int::class.type) { valueNullable ->
            val value = valueNullable ?: 0
            val observable = data { StandardObservableProperty(value) }
            view = factory.numberField(
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
        addCanBeNullEncoder(Long::class.type) { valueNullable ->
            val value = valueNullable ?: 0
            val observable = data { StandardObservableProperty(value) }
            view = factory.numberField(
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
        addCanBeNullEncoder(Float::class.type) { valueNullable ->
            val value = valueNullable ?: 0f
            val observable = data { StandardObservableProperty(value) }
            view = factory.numberField(
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
        addCanBeNullEncoder(Double::class.type) { valueNullable ->
            val value = valueNullable ?: 0.0
            val observable = data { StandardObservableProperty(value) }
            view = factory.numberField(
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

        initializeEncoders()

        addCanBeNullEncoder(Date::class.type) { value ->
            val observable = data { StandardObservableProperty(value ?: TimeStamp.now().date()) }
            view = factory.datePicker(observable)
            dump = { observable.value }
        }
        addCanBeNullEncoder(Time::class.type) { value ->
            val observable = data { StandardObservableProperty(value ?: TimeStamp.now().time()) }
            view = factory.timePicker(observable)
            dump = { observable.value }
        }
        addCanBeNullEncoder(DateTime::class.type) { value ->
            val observable = data { StandardObservableProperty(value ?: TimeStamp.now().dateTime()) }
            view = factory.dateTimePicker(observable)
            dump = { observable.value }
        }
        addCanBeNullEncoder(TimeStamp::class.type) { value ->
            val observable = data { StandardObservableProperty(value ?: TimeStamp.now()) }
            view = factory.dateTimePicker(TransformMutableObservableProperty(
                    observable = observable,
                    transformer = { it: TimeStamp -> it.dateTime() },
                    reverseTransformer = { it: DateTime -> it.toTimeStamp() }
            ))
            dump = { observable.value }
        }


        addEncoder(KClass::class) { type ->
            val options = ArrayList<ClassInfo<*>?>()
            options.addAll(registry.classInfoRegistry.values)
            if (type.nullable) {
                options.add(null)
            }
            return@addEncoder { nullableValue ->
                val value = if (type.nullable) nullableValue else nullableValue ?: options.first()!!.kClass
                val observable = data { StandardObservableProperty(value?.let { registry.classInfoRegistry[it] }) }
                view = factory.picker(
                        options = WrapperObservableList(options),
                        selected = observable,
                        makeView = { obs -> factory.text(obs.transform { it?.localName?.humanify() ?: "None" }) }
                )
                dump = { observable.value?.kClass }
            }
        }
        addEncoder(ClassInfo::class) { type ->
            val options = ArrayList<ClassInfo<*>?>()
            options.addAll(registry.classInfoRegistry.values)
            if (type.nullable) {
                options.add(null)
            }
            return@addEncoder { nullableValue ->
                val value = if (type.nullable) nullableValue else nullableValue ?: options.first()
                val observable = data { StandardObservableProperty(value) }
                view = factory.picker(
                        options = WrapperObservableList(options),
                        selected = observable,
                        makeView = { obs -> factory.text(obs.transform { it?.localName?.humanify() ?: "None" }) }
                )
                dump = { observable.value }
            }
        }
        addEncoder(FieldInfo::class) { type ->
            val owner = type.param(0)
            val classInfo = registry.classInfoRegistry[owner.type.kClass]!!
            val directToClass = classInfo.canBeInstantiated
            val options = ((if (directToClass) classInfo.fields.toMutableList() else registry.classInfoRegistry.values.flatMap {
                if (it.allImplements(registry.classInfoRegistry).contains(owner.type)) it.fields else listOf()
            }) as List<FieldInfo<*, *>?>).toMutableList()
            if (type.nullable) {
                options.add(null)
            }
            return@addEncoder fun FormEncoder.Builder<Any?>.(nullableValue: FieldInfo<*, *>?) {
                val value = if (type.nullable) nullableValue else nullableValue ?: options.first()
                val observable = data { StandardObservableProperty(value) }
                view = factory.picker(
                        options = WrapperObservableList(options),
                        selected = observable,
                        makeView = { obs ->
                            factory.text(obs.transform {
                                if (it == null)
                                    "None"
                                else if (directToClass)
                                    it.name.humanify()
                                else
                                    it.owner.localName.humanify() + ": " + it.name.humanify()
                            })
                        }
                )
                dump = { observable.value }
                Unit
            }
        }


        addEncoder(List::class) { type ->
            if (type.nullable) return@addEncoder null
            val elementType = type.param(0).type
            val elementCoder = viewEncoder.rawEncoder(elementType)

            return@addEncoder label@ fun Builder<Any?>.(value: List<*>?) {
                if (context.size != ViewSize.Full) {
                    buttonToFullEdit(this, value, type)
                    return
                }

                val elements = data { (value ?: listOf<Any?>()).toMutableList().asObservableList() }

                val builderCopy = this.copy()
                builderCopy.contexts.add(context.copy(size = context.size.shrink()))

                view = with(factory) {
                    vertical {
                        +list(
                                data = elements,
                                makeView = { obs ->
                                    horizontal {
                                        +swap(obs.transform {
                                            elementCoder.invoke(builderCopy, it)
                                            builderCopy.view to Animation.None
                                        }).clickable {
                                            val index = elements.indexOf(obs.value)
                                            @Suppress("UNCHECKED_CAST")
                                            stack.push(FormVG(
                                                    formEncoder = this@FormEncoder,
                                                    stack = stack,
                                                    value = obs.value,
                                                    type = elementType as Type<Any?>,
                                                    onComplete = {
                                                        elements[index] = it
                                                        stack.popFrom(this@FormVG)
                                                    }
                                            ))
                                        }
                                        -imageButton(
                                                image = MaterialIcon.moreVert.color(colorSet.foreground).asImage(),
                                                label = "Edit",
                                                onClick = {
                                                    launchSelector(
                                                            title = obs.value?.toString() ?: "None",
                                                            options = listOf(
                                                                    "Insert Above" to {
                                                                        val index = elements.indexOf(obs.value)
                                                                        @Suppress("UNCHECKED_CAST")
                                                                        stack.push(FormVG(
                                                                                formEncoder = this@FormEncoder,
                                                                                stack = stack,
                                                                                value = null,
                                                                                type = elementType as Type<Any?>,
                                                                                onComplete = {
                                                                                    elements.add(index, it)
                                                                                    stack.popFrom(this@FormVG)
                                                                                }
                                                                        ))
                                                                        Unit
                                                                    },
                                                                    "Edit" to {
                                                                        val index = elements.indexOf(obs.value)
                                                                        @Suppress("UNCHECKED_CAST")
                                                                        stack.push(FormVG(
                                                                                formEncoder = this@FormEncoder,
                                                                                stack = stack,
                                                                                value = obs.value,
                                                                                type = elementType as Type<Any?>,
                                                                                onComplete = {
                                                                                    elements[index] = it
                                                                                    stack.popFrom(this@FormVG)
                                                                                }
                                                                        ))
                                                                        Unit
                                                                    },
                                                                    //TODO: Copy/Paste functionality
                                                                    "Delete" to {
                                                                        launchConfirmationDialog(
                                                                                message = "Are you sure you want to delete ${obs.value}?"
                                                                        ) {
                                                                            elements.remove(obs.value)
                                                                        }
                                                                    }
                                                            )
                                                    )
                                                }
                                        )
                                    }
                                }
                        )
                        -button(
                                label = "Add",
                                onClick = {
                                    @Suppress("UNCHECKED_CAST")
                                    stack.push(FormVG(
                                            formEncoder = this@FormEncoder,
                                            stack = stack,
                                            value = null,
                                            type = elementType as Type<Any?>,
                                            onComplete = {
                                                elements.add(it)
                                                stack.popFrom(this@FormVG)
                                            }
                                    ))
                                    Unit
                                }
                        )
                    }
                }
                dump = { elements }
            }
        }

        addEncoder(Map::class) { type ->
            if (type.nullable) return@addEncoder null
            val keyType = type.param(0).type
            val keyCoder = viewEncoder.rawEncoder(keyType)
            val valueType = type.param(1).type
            val valueCoder = viewEncoder.rawEncoder(valueType)

            return@addEncoder label@ fun Builder<Any?>.(value: Map<*, *>?) {
                if (context.size != ViewSize.Full) {
                    buttonToFullEdit(this, value, type)
                    return
                }

                val elements = data {
                    (value?.entries?.map { it.key to it.value } ?: listOf()).toMutableList().asObservableList()
                }

                val builderCopy = this.copy()
                builderCopy.contexts.add(context.copy(size = context.size.shrink()))

                view = with(factory) {
                    vertical {
                        +list(
                                data = elements,
                                makeView = { obs ->
                                    horizontal {
                                        +vertical {
                                            -swap(obs.transform {
                                                keyCoder.invoke(builderCopy, it.first)
                                                builderCopy.view to Animation.None
                                            }).clickable {
                                                val index = elements.indexOf(obs.value)
                                                @Suppress("UNCHECKED_CAST")
                                                stack.push(FormVG(
                                                        formEncoder = this@FormEncoder,
                                                        stack = stack,
                                                        value = obs.value.first,
                                                        type = keyType as Type<Any?>,
                                                        onComplete = {
                                                            elements[index] = it to obs.value.second
                                                            stack.popFrom(this@FormVG)
                                                        }
                                                ))
                                            }
                                            -swap(obs.transform {
                                                valueCoder.invoke(builderCopy, it.second)
                                                builderCopy.view to Animation.None
                                            }).clickable {
                                                val index = elements.indexOf(obs.value)
                                                @Suppress("UNCHECKED_CAST")
                                                stack.push(FormVG(
                                                        formEncoder = this@FormEncoder,
                                                        stack = stack,
                                                        value = obs.value.second,
                                                        type = valueType as Type<Any?>,
                                                        onComplete = {
                                                            elements[index] = obs.value.first to it
                                                            stack.popFrom(this@FormVG)
                                                        }
                                                ))
                                            }
                                        }
                                        -imageButton(
                                                image = MaterialIcon.delete.color(colorSet.foreground).asImage(),
                                                label = "Delete",
                                                onClick = {
                                                    launchConfirmationDialog(
                                                            message = "Are you sure you want to delete ${obs.value}?"
                                                    ) {
                                                        elements.remove(obs.value)
                                                    }
                                                }
                                        )
                                    }
                                }
                        )
                        -button(
                                label = "Add",
                                onClick = {
                                    @Suppress("UNCHECKED_CAST")
                                    stack.push(FormVG(
                                            formEncoder = this@FormEncoder,
                                            stack = stack,
                                            value = null,
                                            type = Pair::class.type.copy(typeParameters = listOf(
                                                    TypeProjection(keyType),
                                                    TypeProjection(valueType)
                                            )) as Type<Any?>,
                                            onComplete = {
                                                elements.add(it as Pair<Any?, Any?>)
                                                stack.popFrom(this@FormVG)
                                            }
                                    ))
                                    Unit
                                }
                        )
                    }
                }
                dump = { elements }
            }
        }


        addEncoder(Reference::class) { type ->
            if (type.nullable) return@addEncoder null

            val keyType = type.param(0).type
            val valueType = type.param(1).type
            val suspendMap = suspendMaps.maps[valueType.kClass]

            if (suspendMap != null) {
                return@addEncoder { value ->
                    val isWorking = StandardObservableProperty(false)
                    val valuePair = data { StandardObservableProperty<Pair<Any?, Any?>>(value?.key to null) }
                    view = factory.work(factory.button(
                            label = valuePair.transform { it.second?.toString() ?: it.first?.toString() ?: "None" },
                            onClick = {
                                //Open up the selector VC
                            }
                    ), isWorking)
                    dump = {
                        val key = valuePair.value.first
                        if (keyType.nullable || key != null) {
                            Reference<Any?, Any>(valuePair.value.first)
                        } else {
                            throw IncompleteException("${context.fieldInfo?.name?.humanify()} cannot be empty.  Please select a reference.")
                        }
                    }
                }
            } else {
                val base = rawEncoder(valueType.copy(nullable = valueType.nullable || type.nullable))
                return@addEncoder { value ->
                    base.invoke(this, value?.key)
                    val direct = dump
                    dump = { direct.invoke()?.let { Reference<Any?, Any?>(it) } }
                }
            }
        }


        addEncoder(object : Encoder.Generator<FormEncoder.Builder<Any?>> {
            override val description: String = "enum"
            override val priority: Float get() = .9f

            override fun generateEncoder(type: Type<*>): TypeEncoder<Builder<Any?>, Any?>? {
                val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
                val enumValues = classInfo.enumValues ?: return null

                return { value ->
                    val observable = data { StandardObservableProperty(value ?: enumValues.first()) }
                    view = factory.picker(
                            options = WrapperObservableList(enumValues.toMutableList()),
                            selected = observable,
                            makeView = {
                                factory.text(text = observable.transform { (it as Enum<*>).name.humanify() })
                            }
                    )
                    dump = { observable.value }
                }
            }
        })
        addEncoder(object : Encoder.Generator<FormEncoder.Builder<Any?>> {
            override val description: String = "null"
            override val priority: Float get() = 1f

            override fun generateEncoder(type: Type<*>): TypeEncoder<Builder<Any?>, Any?>? {
                if (!type.nullable) return null
                val underlyingCoder = rawEncoder(type.copy(nullable = false))

                return { value ->
                    val isNull = data(".null") { StandardObservableProperty(value == null) }
                    var underlyingDump: () -> Any? = { null }
                    view = factory.swap(
                            view = isNull.transform {
                                if (it) {
                                    factory.button(label = "Put something here") {
                                        isNull.value = false
                                    }
                                } else {
                                    val gen = FormEncoder.Builder(this.factory, this.stack)
                                    underlyingCoder.invoke(gen, value)
                                    underlyingDump = gen.dump
                                    factory.horizontal {
                                        +gen.view
                                        -factory.imageButton(image = MaterialIcon.close.color(Color.red).asImage()) {
                                            isNull.value = true
                                        }
                                    }
                                } to Animation.Flip
                            }
                    )
                    dump = { if (isNull.value) null else underlyingDump() }
                }
            }
        })
        addEncoder(object : Encoder.Generator<FormEncoder.Builder<Any?>> {
            override val description: String = "reflection"
            override val priority: Float get() = 0f

            override fun generateEncoder(type: Type<*>): TypeEncoder<Builder<Any?>, Any?>? {
                val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
                val default = try {
                    classInfo.constructDefault()
                } catch (e: Exception) {
                    null
                }
                val lazySubCoders by lazy {
                    (registry.classInfoRegistry[type.kClass]
                            ?: throw IllegalArgumentException("KClass ${type.kClass} not registered.")).fieldsToRender(default == null).associateWith { rawEncoder(it.type as Type<*>) }
                }
                return label@{ value ->

                    if (context.size != ViewSize.Full) {
                        buttonToFullEdit(this, value, type)
                        return@label
                    }

                    val out = this
                    val dumps = HashMap<String, () -> Any?>()

                    //Add default dumps because of hidden fields
                    (registry.classInfoRegistry[type.kClass]
                            ?: throw IllegalArgumentException("KClass ${type.kClass} not registered.")).fields.forEach {
                        dumps[it.name] = { it.get.untyped(value ?: default!!) }
                    }

                    view = with(factory) {
                        vertical {
                            -text(text = classInfo.localName.humanify(), size = TextSize.Subheader)
                            for ((field, coder) in lazySubCoders) {
                                out.forField(owner = value, fieldInfo = field) {
                                    coder.invoke(out, value?.let { field.get.untyped(it) })
                                    dumps[field.name] = out.dump
                                    -entryContext(
                                            label = field.name.humanify(),
                                            field = out.view,
                                            help = field.annotations
                                                    .find { it.name.endsWith("Description") }
                                                    ?.arguments
                                                    ?.firstOrNull() as? String
                                                    ?: field.name.humanify()
                                    )
                                }
                            }
                        }
                    }
                    dump = {
                        val m = dumps.mapValues { it.value.invoke() }
                        classInfo.construct(m)
                    }
                }
            }
        })
        addEncoder(object : Encoder.Generator<FormEncoder.Builder<Any?>> {
            override val description: String = "polymorphic"
            override val priority: Float get() = 1.1f

            override fun generateEncoder(type: Type<*>): TypeEncoder<Builder<Any?>, Any?>? {
                val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
                if (classInfo.canBeInstantiated) return null

                //Set up what options the user has
                val options = ArrayList<ClassInfo<*>?>()
                options.addAll(registry.classInfoRegistry.values.filter {
                    it.allImplements(registry.classInfoRegistry).contains(type)
                })
                if (type.nullable) options.add(null)

                return { value ->
                    var underlyingDump: () -> Any? = { null }
                    val typeObs = data(".polymorphic") { StandardObservableProperty(if (value == null) null else registry.classInfoRegistry[value::class]) }
                    view = factory.swap(
                            view = typeObs.transform { classInfo ->
                                factory.vertical {
                                    -factory.picker(
                                            options = WrapperObservableList(options),
                                            selected = typeObs,
                                            makeView = {
                                                factory.text(text = it.transform {
                                                    it?.localName?.humanify() ?: "None"
                                                })
                                            }
                                    )

                                    if (classInfo == null) {
                                        underlyingDump = { null }
                                    } else {
                                        val gen = FormEncoder.Builder(factory, stack)
                                        rawEncoder(classInfo.type).invoke(
                                                gen,
                                                if (classInfo.kClass.isInstance(value))
                                                    value
                                                else
                                                    classInfo.construct(mapOf())
                                        )
                                        underlyingDump = gen.dump
                                        -gen.view
                                    }
                                } to Animation.Flip
                            }
                    )
                    dump = { underlyingDump.invoke() }
                }
            }
        })
    }
}
