package com.lightningkite.mirror.form

import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.mirror.form.form.FormViewGenerator
import com.lightningkite.mirror.info.MirrorType
import com.lightningkite.reacktive.property.ConstantObservableProperty
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.ObservableProperty

fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> DEPENDENCY.display(
        data: T,
        type: MirrorType<T>,
        generalRequest: GeneralRequest,// = GeneralRequest(),
        scale: ViewSize = ViewSize.Full
) = ViewEncoder.getViewGenerator<T, DEPENDENCY, VIEW>(DisplayRequest(
        general = generalRequest,
        type = type,
        scale = scale,
        observable = ConstantObservableProperty(data)
)).generate(this)

fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> DEPENDENCY.display(
        observable: ObservableProperty<T>,
        type: MirrorType<T>,
        generalRequest: GeneralRequest,// = GeneralRequest(),
        scale: ViewSize = ViewSize.Full
) = ViewEncoder.getViewGenerator<T, DEPENDENCY, VIEW>(DisplayRequest(
        general = generalRequest,
        type = type,
        scale = scale,
        observable = observable
)).generate(this)

fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> DEPENDENCY.form(
        observable: MutableObservableProperty<FormState<T>>,
        type: MirrorType<T>,
        generalRequest: GeneralRequest,// = GeneralRequest(),
        scale: ViewSize = ViewSize.Full
) = FormEncoder.getViewGenerator<T, DEPENDENCY, VIEW>(FormRequest(
        general = generalRequest,
        type = type,
        scale = scale,
        observable = observable
)).generate(this)



fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> DisplayViewGenerator(
        data: T,
        type: MirrorType<T>,
        generalRequest: GeneralRequest,// = GeneralRequest(),
        scale: ViewSize = ViewSize.Full
) = ViewEncoder.getViewGenerator<T, DEPENDENCY, VIEW>(DisplayRequest(
        general = generalRequest,
        type = type,
        scale = scale,
        observable = ConstantObservableProperty(data)
))

fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> DisplayViewGenerator(
        observable: ObservableProperty<T>,
        type: MirrorType<T>,
        generalRequest: GeneralRequest,// = GeneralRequest(),
        scale: ViewSize = ViewSize.Full
) = ViewEncoder.getViewGenerator<T, DEPENDENCY, VIEW>(DisplayRequest(
        general = generalRequest,
        type = type,
        scale = scale,
        observable = observable
))

fun <T, DEPENDENCY : ViewFactory<VIEW>, VIEW> PartFormViewGenerator(
        observable: MutableObservableProperty<FormState<T>>,
        type: MirrorType<T>,
        generalRequest: GeneralRequest,// = GeneralRequest(),
        scale: ViewSize = ViewSize.Full
) = FormEncoder.getViewGenerator<T, DEPENDENCY, VIEW>(FormRequest(
        general = generalRequest,
        type = type,
        scale = scale,
        observable = observable
))