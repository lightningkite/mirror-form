package com.lightningkite.mirror.form

import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.MirrorType
import com.lightningkite.reacktive.list.MutableObservableList
import com.lightningkite.reacktive.list.WrapperObservableList
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.ObservableProperty
import com.lightningkite.reacktive.property.StandardObservableProperty
import com.lightningkite.reacktive.property.transform

data class GeneralRequest(
        val developerMode: Boolean = false,
        val nullString: String = "N/A",
        val skipFields: Collection<MirrorClass.Field<*, *>> = listOf(),
        val stack: MutableObservableList<ViewGenerator<ViewFactory<Any?>, Any?>> = WrapperObservableList()
)

interface CommonRequest<T> {
    val general: GeneralRequest
    val type: MirrorType<T>
    val scale: ViewSize
    val owningField: MirrorClass.Field<*, *>?
}

data class FormRequest<T>(
        override val general: GeneralRequest,
        override val type: MirrorType<T>,
        override val scale: ViewSize = ViewSize.Full,
        override val owningField: MirrorClass.Field<*, *>? = null,
        val observable: MutableObservableProperty<FormState<T>> = StandardObservableProperty(FormState.empty())
) : CommonRequest<T> {

    @Suppress("UNCHECKED_CAST")
    fun <S> child(
            field: MirrorClass.Field<*, *>,
            observable: MutableObservableProperty<FormState<S>>,
            scale: ViewSize = this.scale.shrink()
    ): FormRequest<S> = FormRequest<S>(
            general = general,
            type = field.type as MirrorType<S>,
            scale = scale,
            owningField = field,
            observable = observable
    )

    @Suppress("UNCHECKED_CAST")
    fun <S> sub(
            type: MirrorType<S>,
            observable: MutableObservableProperty<FormState<S>>,
            scale: ViewSize = this.scale.shrink()
    ): FormRequest<S> = FormRequest<S>(
            general = general,
            type = type,
            scale = scale,
            observable = observable
    )

    @Suppress("UNCHECKED_CAST")
    fun <S> copy(
            type: MirrorType<S>,
            observable: MutableObservableProperty<FormState<S>>,
            scale: ViewSize = this.scale
    ): FormRequest<S> = FormRequest<S>(
            general = general,
            type = type,
            scale = scale,
            observable = observable
    )

    fun display(default: T) = DisplayRequest(
            general = general,
            type = type,
            scale = scale,
            owningField = owningField,
            observable = observable.transform { it.valueOrNull ?: default }
    )

    fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> getVG() = FormEncoder.getViewGenerator<T, DEPENDENCY, VIEW>(this)
    fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> getVG(viewGenerator: ViewGenerator<DEPENDENCY, VIEW>) = FormEncoder.getViewGenerator<T, DEPENDENCY, VIEW>(this)
}

data class DisplayRequest<T>(
        override val general: GeneralRequest,
        override val type: MirrorType<T>,
        override val scale: ViewSize = ViewSize.Full,
        override val owningField: MirrorClass.Field<*, *>? = null,
        val observable: ObservableProperty<T>
) : CommonRequest<T> {

    @Suppress("UNCHECKED_CAST")
    fun <S> child(
            field: MirrorClass.Field<*, *>,
            observable: ObservableProperty<S>,
            scale: ViewSize = this.scale.shrink()
    ): DisplayRequest<S> = DisplayRequest<S>(
            general = general,
            type = field.type as MirrorType<S>,
            scale = scale,
            owningField = field,
            observable = observable
    )

    @Suppress("UNCHECKED_CAST")
    fun <S> sub(
            type: MirrorType<S>,
            observable: ObservableProperty<S>,
            scale: ViewSize = this.scale.shrink()
    ): DisplayRequest<S> = DisplayRequest<S>(
            general = general,
            type = type,
            scale = scale,
            observable = observable
    )

    fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> getVG() = ViewEncoder.getViewGenerator<T, DEPENDENCY, VIEW>(this)
    fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> getVG(viewGenerator: ViewGenerator<DEPENDENCY, VIEW>) = ViewEncoder.getViewGenerator<T, DEPENDENCY, VIEW>(this)
}