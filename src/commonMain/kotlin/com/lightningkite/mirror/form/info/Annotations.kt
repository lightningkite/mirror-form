package com.lightningkite.mirror.form.info

import com.lightningkite.koolui.concepts.NumberInputType
import com.lightningkite.koolui.concepts.TextInputType

@Target(AnnotationTarget.FIELD)
annotation class FormBooleanStrings(val on: String, val off: String)

@Target(AnnotationTarget.FIELD)
annotation class FormEditOnly()

@Target(AnnotationTarget.FIELD)
annotation class FormViewOnly()

@Target(AnnotationTarget.FIELD)
annotation class FormHidden()