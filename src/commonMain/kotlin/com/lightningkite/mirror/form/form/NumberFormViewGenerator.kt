package com.lightningkite.mirror.form.form

import com.lightningkite.koolui.concepts.NumberInputType
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.form.FormState
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.transform

class NumberFormViewGenerator<T : Number, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val observable: MutableObservableProperty<FormState<T?>>,
        val toT: Number.() -> T,
        val allowNull: Boolean = false,
        val numberInputType: NumberInputType,
        val decimalPlaces: Int = 2
) : ViewGenerator<DEPENDENCY, VIEW> {
    override fun generate(dependency: DEPENDENCY): VIEW {
        if(allowNull){
            if(observable.value.isEmpty){
                observable.value = FormState.success(null)
            }
        }
        return dependency.numberField(
                value = observable.transform<FormState<T?>, Number?>(
                        mapper = {
                            it.valueOrNull
                        },
                        reverseMapper = {
                            if(it == null && !allowNull){
                                FormState.empty()
                            } else {
                                FormState.success(it?.toT())
                            }
                        }
                ),
                placeholder = if(allowNull) "N/A" else "",
                type = numberInputType,
                decimalPlaces = decimalPlaces
        )
    }
}