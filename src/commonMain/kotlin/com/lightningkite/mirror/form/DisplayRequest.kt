package com.lightningkite.mirror.form

import com.lightningkite.kommon.native.ensureNeverFrozen
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.MirrorType
import com.lightningkite.reacktive.property.ObservableProperty

data class DisplayRequest<T>(
        override val general: GeneralRequest,
        override val type: MirrorType<T>,
        override val scale: ViewSize = ViewSize.Full,
        override val owningField: MirrorClass.Field<*, *>? = null,
        val clickable: Boolean = true,
        val observable: ObservableProperty<T>
) : CommonRequest<T> {
    init {
        ensureNeverFrozen()
    }

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
            clickable = clickable,
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
            clickable = clickable,
            observable = observable
    )

    fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> getVG() = ViewEncoder.getViewGenerator<T, DEPENDENCY, VIEW>(this)
    fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> getVG(viewGenerator: ViewGenerator<DEPENDENCY, VIEW>) = ViewEncoder.getViewGenerator<T, DEPENDENCY, VIEW>(this)
}