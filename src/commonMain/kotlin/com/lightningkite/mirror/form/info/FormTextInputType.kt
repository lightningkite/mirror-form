package com.lightningkite.mirror.form.info

import com.lightningkite.koolui.concepts.TextInputType
import com.lightningkite.mirror.info.MirrorAnnotation
import com.lightningkite.mirror.info.MirrorClass


@Target(AnnotationTarget.FIELD)
annotation class FormTextInputType(val inputType: TextInputType)

val MirrorClass.Field<*, *>.textInputType: TextInputType
    get() = annotations
            .asSequence()
            .mapNotNull { it as? MirrorAnnotation }
            .filter { it.annotationType == FormTextInputType::class }
            .firstOrNull()?.asMap()?.values?.firstOrNull() as? TextInputType ?: when(this.name){
        "email" -> TextInputType.Email
        "title" -> TextInputType.Name
        "name" -> TextInputType.Name
        "subject" -> TextInputType.Sentence
        "id", "vin" -> TextInputType.CapitalizedIdentifier
        "body" -> TextInputType.Paragraph
        "password", "confirmPassword", "newPassword", "oldPassword" -> TextInputType.Password
        else -> TextInputType.Sentence
    }