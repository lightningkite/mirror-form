package com.lightningkite.mirror.form.form

import com.lightningkite.kommon.string.BackedByString
import com.lightningkite.koolui.concepts.TextInputType
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.form.FormState
import com.lightningkite.mirror.form.ValidatingForm
import com.lightningkite.mirror.form.perfectNonNull
import com.lightningkite.reacktive.property.MutableObservableProperty

class BackedByStringFormViewGenerator<T : BackedByString, DEPENDENCY : ViewFactory<VIEW>, VIEW>(
        val observable: MutableObservableProperty<FormState<T?>>,
        val toT: String.() -> T,
        val inputType: TextInputType
) : ViewGenerator<DEPENDENCY, VIEW> {
    val form = ValidatingForm(
            main = observable,
            toRaw = { it?.string ?: "" },
            toTypeWithValidation = {
                val transformed = it.toT()
                when {
                    transformed.string.isEmpty() -> FormState.success(null)
                    transformed.isValid -> FormState.success(transformed)
                    else -> FormState.invalid("Invalid")
                }
            }
    )

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        textField(
                text = form.raw.perfectNonNull(""),
                type = inputType
        ).apply {
            form.bind(lifecycle)
        }
    }
}