package com.lightningkite.mirror.form

import com.lightningkite.koolui.builders.text
import com.lightningkite.koolui.concepts.Animation
import com.lightningkite.koolui.concepts.TextSize
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.lokalize.*
import com.lightningkite.lokalize.time.*
import com.lightningkite.mirror.archive.database.SuspendMap
import com.lightningkite.mirror.archive.database.SuspendMapRegistry
import com.lightningkite.mirror.archive.model.Reference
import com.lightningkite.mirror.request.RequestHandler
import com.lightningkite.mirror.serialization.Encoder
import com.lightningkite.mirror.serialization.SerializationRegistry
import com.lightningkite.mirror.serialization.TypeEncoder
import com.lightningkite.reacktive.list.MutableObservableList
import com.lightningkite.reacktive.list.StandardObservableList
import com.lightningkite.reacktive.property.StandardObservableProperty
import com.lightningkite.reacktive.property.transform
import kotlin.reflect.KClass

class ViewEncoder(
        override val registry: SerializationRegistry,
        val suspendMaps: SuspendMapRegistry,
        val requestHandler: RequestHandler
) : Encoder<ViewEncoder.Builder<Any?>> {

    override val arbitraryEncoders: MutableList<Encoder.Generator<Builder<Any?>>> = ArrayList()
    override val encoders: MutableMap<Type<*>, TypeEncoder<Builder<Any?>, Any?>> = HashMap()
    override val kClassEncoders: MutableMap<KClass<*>, (Type<*>) -> TypeEncoder<Builder<Any?>, Any?>?> = HashMap()

    open class Builder<VIEW>(
            val factory: ViewFactory<VIEW>,
            val stack: MutableObservableList<ViewGenerator<ViewFactory<VIEW>, VIEW>> = StandardObservableList(),
            val showHidden: Boolean = false,
            startContext: ViewContext = ViewContext()
    ) : ViewContextStack {
        override val contexts = ArrayList<ViewContext>().also { it.add(startContext) }
        override val context get() = contexts.last()
        open val detailOnClick: Boolean get() = true

        var view: VIEW? = null
        fun outputText(text: String) {
            view = factory.text(
                    text = text,
                    size = context.textSize,
                    importance = context.viewImportance,
                    maxLines = context.maxLines
            )
        }

        fun ViewFactory<VIEW>.label(text: String): VIEW {
            return text(
                    text = "$text:",
                    size = context.textSize,
                    importance = context.viewImportance,
                    maxLines = 1
            )
        }

        fun <T> build(encoder: ViewEncoder, value: T, type: Type<T>): VIEW {
            @Suppress("UNCHECKED_CAST")
            encoder.encoder(type).invoke(this.let { it as Builder<Any?> }, value)
            return view!!
        }
    }

    fun <VIEW, T> build(
            factory: ViewFactory<VIEW>,
            stack: MutableObservableList<ViewGenerator<ViewFactory<VIEW>, VIEW>> = StandardObservableList(),
            showHidden: Boolean = false,
            startContext: ViewContext = ViewContext(),
            value: T,
            type: Type<T>
    ):VIEW = Builder(factory = factory, stack = stack, showHidden = showHidden, startContext = startContext).build(this, value, type)

    inline fun <T> addEncoderString(type: Type<T>, crossinline toString: Builder<*>.(T) -> String) {
        addEncoder(type) {
            outputText(toString(it))
        }
    }

    inline fun <T : Any> addEncoderString(kclass: KClass<T>, crossinline toString: Builder<*>.(Type<*>, T?) -> String) {
        addEncoder(kclass) { type ->
            { it ->
                outputText(toString(type, it))
            }
        }
    }

    inline fun <T> addEncoderString(type: Type<T>) {
        addEncoder(type) {
            outputText(it.toString())
        }
    }

    init {

        addEncoder(Unit::class.type) {
            view = factory.space(1f)
        }
        addEncoderString(Boolean::class.type) {
            @Suppress("UNCHECKED_CAST") val onOffTexts = context.fieldInfo?.annotations
                    ?.find { it.name.endsWith("BooleanStrings") }
                    ?.arguments as? List<String> ?: listOf("Off", "On")
            if (it) onOffTexts[1] else onOffTexts[0]
        }
        addEncoderString(Byte::class.type)
        addEncoderString(Short::class.type)
        addEncoderString(Int::class.type)
        addEncoderString(Long::class.type)
        addEncoderString(Char::class.type)
        addEncoderString(String::class.type)

        initializeEncoders()

        addEncoderString(Date::class.type) { DefaultLocale.renderDate(it) }
        addEncoderString(DateTime::class.type) { DefaultLocale.renderDateTime(it) }
        addEncoderString(Time::class.type) { DefaultLocale.renderTime(it) }
        addEncoderString(TimeStamp::class.type) { DefaultLocale.renderTimeStamp(it) }

        addEncoderString(KClass::class) { type, it ->
            if (it == null) {
                "None"
            } else {
                registry.kClassToExternalNameRegistry[it]?.humanify() ?: "Unknown"
            }
        }
        addEncoderString(ClassInfo::class) { type, it ->
            if (it == null) {
                "None"
            } else {
                registry.kClassToExternalNameRegistry[it.kClass]?.humanify() ?: "Unknown"
            }
        }
        addEncoderString(FieldInfo::class) { type, it ->
            if (it == null) {
                "None"
            } else {
                val owner = type.param(0)
                val classInfo = registry.classInfoRegistry[owner.type.kClass]!!
                val directToClass = classInfo.canBeInstantiated
                if (directToClass) it.name.humanify()
                else (registry.kClassToExternalNameRegistry[classInfo.kClass]?.humanify()
                        ?: "Unknown") + " " + it.name.humanify()
            }
        }

        addEncoder(Reference::class) { type ->
            if (type.nullable) return@addEncoder null

            val keyType = type.param(0).type
            val valueType = type.param(1).type
            val suspendMap = suspendMaps.maps[valueType.kClass]
            val keyEncoder = rawEncoder(keyType)
            val entryEncoder = rawEncoder(SuspendMap.Entry::class.type.copy(typeParameters = listOf(TypeProjection(keyType), TypeProjection(valueType))))

            if (suspendMap == null) {
                return@addEncoder { value ->
                    keyEncoder.invoke(this, value!!.key)
                }
            }

            return@addEncoder label@{ value ->

                val full = StandardObservableProperty<SuspendMap.Entry<Any?, Any>?>(null)
                val loadingObs = StandardObservableProperty(false)

                view = factory.work(factory.swap(
                        full.transform {
                            if (it == null) keyEncoder.invoke(this, value!!.key)
                            else entryEncoder.invoke(this, it)
                            view to Animation.None
                        }
                ), loadingObs)
            }
        }
        addEncoder(SuspendMap.Entry::class) { type ->
            if (type.nullable) return@addEncoder null

            val keyType = type.param(0).type
            val valueType = type.param(1).type
            val keyEncoder = rawEncoder(keyType)
            val valueEncoder = rawEncoder(valueType)

            return@addEncoder label@{ value ->
                view = factory.vertical {
                    useContext(
                            owner = value,
                            size = ViewSize.OneLine,
                            importance = .3f
                    ) {
                        keyEncoder.invoke(this@label, value!!.key)
                        -view
                    }
                    useContext(
                            owner = value,
                            importance = .7f
                    ) {
                        valueEncoder.invoke(this@label, value!!.value)
                        -view
                    }
                }
            }
        }
        addEncoder(Map.Entry::class) { type ->
            if (type.nullable) return@addEncoder null

            val keyType = type.param(0).type
            val valueType = type.param(1).type
            val keyEncoder = rawEncoder(keyType)
            val valueEncoder = rawEncoder(valueType)

            return@addEncoder label@{ value ->
                view = factory.vertical {
                    useContext(
                            owner = value,
                            size = ViewSize.OneLine,
                            importance = .3f
                    ) {
                        keyEncoder.invoke(this@label, value!!.key)
                        -view
                    }
                    useContext(
                            owner = value,
                            importance = .7f
                    ) {
                        valueEncoder.invoke(this@label, value!!.value)
                        -view
                    }
                }
            }
        }
        addEncoder(Map::class) { type ->
            if (type.nullable) return@addEncoder null

            val keyType = type.param(0).type
            val valueType = type.param(1).type
            val entryEncoder = rawEncoder(Map.Entry::class.type.copy(typeParameters = listOf(
                    TypeProjection(keyType),
                    TypeProjection(valueType)
            )))

            return@addEncoder label@{ value ->
                when (context.size) {
                    ViewSize.Full -> {
                        view = factory.vertical {
                            value!!.forEach { entry ->
                                shrink {
                                    entryEncoder.invoke(this@label, entry)
                                    -view
                                }
                            }
                        }
                    }
                    ViewSize.Summary -> {
                        view = factory.vertical {
                            value!!.asSequence().take(2).forEach { entry ->
                                shrink {
                                    entryEncoder.invoke(this@label, entry)
                                    -view
                                }
                            }
                            if (value.size > 2) {
                                shrink {
                                    outputText("...")
                                    -view
                                }
                            }
                        }
                    }
                    ViewSize.OneLine -> {
                        outputText(value!!.entries.joinToString(", ") { it.key.toString() + ": " + it.value.toString() })
                    }
                }
            }
        }
        addEncoder(List::class) { type ->
            if (type.nullable) return@addEncoder null

            val elementType = type.param(0).type
            val elementCoder = rawEncoder(elementType)

            return@addEncoder label@{ value ->
                when (context.size) {
                    ViewSize.Full -> {
                        view = factory.vertical {
                            value!!.forEach { entry ->
                                shrink {
                                    elementCoder.invoke(this@label, entry)
                                    -view
                                }
                            }
                        }
                    }
                    ViewSize.Summary -> {
                        view = factory.vertical {
                            value!!.asSequence().take(2).forEach { entry ->
                                shrink {
                                    elementCoder.invoke(this@label, entry)
                                    -view
                                }
                            }
                            if (value.size > 2) {
                                shrink {
                                    outputText("...")
                                    -view
                                }
                            }
                        }
                    }
                    ViewSize.OneLine -> {
                        outputText(value!!.joinToString(", ") { it.toString() })
                    }
                }
            }
        }


        addEncoder(object : Encoder.Generator<ViewEncoder.Builder<Any?>> {
            override val description: String = "enum"
            override val priority: Float get() = .9f

            override fun generateEncoder(type: Type<*>): TypeEncoder<ViewEncoder.Builder<Any?>, Any?>? {
                val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
                if (classInfo.enumValues == null) return null

                return { value ->
                    outputText((value as? Enum<*>)?.name?.humanify() ?: "None")
                }
            }
        })
        addEncoder(object : Encoder.Generator<ViewEncoder.Builder<Any?>> {
            override val description: String = "null"
            override val priority: Float get() = 1f

            override fun generateEncoder(type: Type<*>): TypeEncoder<ViewEncoder.Builder<Any?>, Any?>? {
                if (!type.nullable) return null
                val underlyingCoder = rawEncoder(type.copy(nullable = false))

                return { value ->
                    if (value == null) {
                        outputText("None")
                    } else {
                        underlyingCoder.invoke(this, value)
                    }
                }
            }
        })
        addEncoder(object : Encoder.Generator<ViewEncoder.Builder<Any?>> {
            override val description: String = "reflection"
            override val priority: Float get() = 0f

            override fun generateEncoder(type: Type<*>): TypeEncoder<ViewEncoder.Builder<Any?>, Any?>? {
                val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
                val lazySubCoders by lazy {
                    (registry.classInfoRegistry[type.kClass]
                            ?: throw IllegalArgumentException("KClass ${type.kClass} not registered.")).fieldsToRender(false).associateWith { rawEncoder(it.type as Type<*>) }
                }
                return label@{ value ->
                    val out = this

                    if(value == null){
                        outputText("Fill in a " + registry.kClassToExternalNameRegistry[classInfo.kClass]!!.humanify())
                        return@label
                    }

                    if (classInfo.fields.isEmpty()) {
                        outputText(registry.kClassToExternalNameRegistry[classInfo.kClass]!!.humanify())
                        return@label
                    }

                    with(factory) {
                        when (context.size) {
                            ViewSize.Full -> view = vertical {
                                for ((field, coder) in lazySubCoders) {
                                    out.forField(
                                            fieldInfo = field,
                                            owner = value
                                    ) {
                                        if (context.fieldInfo?.needsNoContext == true) {
                                            coder.invoke(out, field.get.untyped(value!!))
                                            -out.view
                                        } else {
                                            -horizontal {
                                                -label(text = field.annotations
                                                        .find { it.name.endsWith("NiceName") }
                                                        ?.arguments
                                                        ?.firstOrNull() as? String
                                                        ?: field.name.humanify())
                                                coder.invoke(out, field.get.untyped(value!!))
                                                -out.view
                                            }
                                        }
                                    }
                                }
                            }
                            ViewSize.Summary -> {

                                //Find three most important elements and sort information
                                val defaultSort = classInfo.defaultSort
                                val importantFields = classInfo.fieldsToRender(showHidden)
                                        .filter { it != defaultSort?.field }
                                        .takeWhile { it.importance >= .5f }
                                        .take(3)
                                        .toList()
                                val oldImportanceRange = importantFields.last().importance..importantFields.first().importance
                                view = horizontal {
                                    +vertical {
                                        importantFields.forEach { field ->
                                            out.forField(
                                                    fieldInfo = field,
                                                    owner = value,
                                                    importance = (field.importance - oldImportanceRange.start) / (oldImportanceRange.endInclusive - oldImportanceRange.start)
                                            ) {
                                                val coder = lazySubCoders[field]!!
                                                if (context.fieldInfo?.needsNoContext == true) {
                                                    coder.invoke(out, field.get.untyped(value))
                                                    -out.view
                                                } else {
                                                    -horizontal {
                                                        -label(text = field.annotations
                                                                .find { it.name.endsWith("NiceName") }
                                                                ?.arguments
                                                                ?.firstOrNull() as? String
                                                                ?: field.name.humanify())
                                                        coder.invoke(out, field.get.untyped(value))
                                                        -out.view
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (defaultSort != null) {
                                        out.useContext(ViewContext(
                                                fieldInfo = defaultSort.field,
                                                owner = value,
                                                size = context.size.shrink()
                                        )) {
                                            lazySubCoders[defaultSort.field]!!.invoke(out, defaultSort.field.get.untyped(value))
                                            -out.view
                                        }
                                    }
                                }.let {
                                    if (detailOnClick) {
                                        it.clickable {
                                            @Suppress("UNCHECKED_CAST")
                                            stack.push(DisplayVG(
                                                    formEncoder = this@ViewEncoder,
                                                    stack = stack,
                                                    value = value,
                                                    type = type as Type<Any?>
                                            ))
                                        }
                                    } else it
                                }
                            }
                            ViewSize.Footnote -> outputText(value.toString()).let {
                                if (detailOnClick) {
                                    it.clickable {
                                        @Suppress("UNCHECKED_CAST")
                                        stack.push(DisplayVG(
                                                formEncoder = this@ViewEncoder,
                                                stack = stack,
                                                value = value,
                                                type = type as Type<Any?>
                                        ))
                                    }
                                } else it
                            }
                        }
                    }
                    //End view output
                }
            }
        })
        addEncoder(object : Encoder.Generator<ViewEncoder.Builder<Any?>> {
            override val description: String = "polymorphic"
            override val priority: Float get() = 1.1f

            override fun generateEncoder(type: Type<*>): TypeEncoder<ViewEncoder.Builder<Any?>, Any?>? {
                if (type.nullable) return null
                if (registry.classInfoRegistry[type.kClass]?.canBeInstantiated == true) return null
                return label@{ value ->
                    if (value == null) {
                        outputText("None")
                    } else {
                        view = factory.vertical {
                            -factory.text(
                                    text = registry.kClassToExternalNameRegistry[value::class]?.humanify() ?: "",
                                    size = TextSize.Subheader
                            )
                            rawEncoder(value::class.type).invoke(this@label, value)
                            -view
                        }
                    }
                }
            }
        })
    }
}