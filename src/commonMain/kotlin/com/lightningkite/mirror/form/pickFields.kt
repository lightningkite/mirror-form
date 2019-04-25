package com.lightningkite.mirror.form

import com.lightningkite.mirror.info.MirrorAnnotation
import com.lightningkite.mirror.info.MirrorClass

fun <T : Any> MirrorClass.Field<T, *>.includedInForm(request: FormRequest<T>): Boolean {
    if(request.general.developerMode) return true

    val hasRestrictedAnnotation = annotations.any { anno ->
        val annotationType = (anno as? MirrorAnnotation)?.annotationType
        annotationType == FormViewOnly::class || annotationType == FormHidden::class
    }
    if (hasRestrictedAnnotation) return false

    if (this in request.general.impliedFields.keys) return false

    return true
}

fun <T : Any> MirrorClass<T>.pickDisplayFields(request: DisplayRequest<T>) = fields
        .asSequence()
        .let {
            if (request.general.developerMode)
                it
            else
                it.filter {
                    it.annotations.none { anno ->
                        val annotationType = (anno as? MirrorAnnotation)?.annotationType
                        annotationType == FormEditOnly::class || annotationType == FormHidden::class
                    }
                }.filter {
                    it !in request.general.impliedFields.keys
                }
        }