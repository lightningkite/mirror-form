package com.lightningkite.mirror.form

@Target(AnnotationTarget.FIELD)
annotation class FormBooleanStrings(val on: String, val off: String)

@Target(AnnotationTarget.FIELD)
annotation class FormEditOnly()

@Target(AnnotationTarget.FIELD)
annotation class FormViewOnly()

@Target(AnnotationTarget.FIELD)
annotation class FormHidden()