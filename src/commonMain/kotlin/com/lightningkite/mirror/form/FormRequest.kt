package com.lightningkite.mirror.form

import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.MirrorType
import com.lightningkite.mirror.info.nullable
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.ObservableProperty
import com.lightningkite.reacktive.property.StandardObservableProperty
import com.lightningkite.reacktive.property.transform

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

    fun <S> display(
            type: MirrorType<S>,
            observable: ObservableProperty<S>,
            general: GeneralRequest = this.general,
            scale: ViewSize = this.scale,
            owningField: MirrorClass.Field<*, *>? = this.owningField
    ) = DisplayRequest(
            general = general,
            type = type,
            scale = scale,
            owningField = owningField,
            clickable = false,
            observable = observable
    )

    fun display(
            observable: ObservableProperty<T>,
            type: MirrorType<T> = this.type,
            general: GeneralRequest = this.general,
            scale: ViewSize = this.scale,
            owningField: MirrorClass.Field<*, *>? = this.owningField
    ) = DisplayRequest(
            general = general,
            type = type,
            scale = scale,
            owningField = owningField,
            clickable = false,
            observable = observable
    )

    fun display(default: T) = DisplayRequest(
            general = general,
            type = type,
            scale = scale,
            owningField = owningField,
            clickable = false,
            observable = observable.transform { it.valueOrNull ?: default }
    )

    fun displayNullable() = DisplayRequest(
            general = general,
            type = type.nullable,
            scale = scale,
            owningField = owningField,
            clickable = false,
            observable = observable.transform { it.valueOrNull }
    )

    fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> getVG() = FormEncoder.getViewGenerator<T, DEPENDENCY, VIEW>(this)
    fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> getVG(viewGenerator: ViewGenerator<DEPENDENCY, VIEW>) = FormEncoder.getViewGenerator<T, DEPENDENCY, VIEW>(this)
}