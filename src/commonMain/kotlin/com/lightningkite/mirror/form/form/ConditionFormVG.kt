package com.lightningkite.mirror.form.form

import com.lightningkite.koolui.builders.button
import com.lightningkite.koolui.builders.horizontal
import com.lightningkite.koolui.builders.space
import com.lightningkite.koolui.builders.vertical
import com.lightningkite.koolui.concepts.Animation
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.form.FormRequest
import com.lightningkite.mirror.form.FormState
import com.lightningkite.mirror.form.ensureSubtype
import com.lightningkite.mirror.form.info.humanify
import com.lightningkite.mirror.info.*
import com.lightningkite.reacktive.list.asObservableList
import com.lightningkite.reacktive.property.*

class ConditionFormVG<T, DEPENDENCY : ViewFactory<VIEW>, VIEW>(val type: ConditionMirror<T>, val request: FormRequest<Condition<T>>) : ViewGenerator<DEPENDENCY, VIEW> {

    sealed class TypeOrField {
        abstract fun matches(value: Condition<*>): Boolean
        data class Type(val type: MirrorType<*>) : TypeOrField() {
            override fun matches(value: Condition<*>): Boolean = type.base.kClass.isInstance(value)
        }

        data class Field<T>(val field: MirrorClass.Field<T, *>) : TypeOrField() {
            override fun matches(value: Condition<*>): Boolean = (value as? Condition.Field<*, *, *>)?.field == field
        }
    }

    val typeOrFieldOptions: List<TypeOrField> = run {
        val fullList = ArrayList<TypeOrField>()
        fullList.add(TypeOrField.Type(ConditionAlwaysMirror))
        fullList.add(TypeOrField.Type(ConditionNeverMirror))
        fullList.add(TypeOrField.Type(ConditionAndMirror(type.TMirror)))
        fullList.add(TypeOrField.Type(ConditionOrMirror(type.TMirror)))
        fullList.add(TypeOrField.Type(ConditionEqualMirror(type.TMirror)))
        fullList.add(TypeOrField.Type(ConditionNotEqualMirror(type.TMirror)))
        fullList.add(TypeOrField.Type(ConditionEqualToOneMirror(type.TMirror)))
        if(type.TMirror.isA(ComparableMirror(type.TMirror))){
            fullList.add(TypeOrField.Type(ConditionLessThanMirror(type.TMirror as MirrorType<Comparable<Comparable<*>>>)))
            fullList.add(TypeOrField.Type(ConditionLessThanOrEqualMirror(type.TMirror as MirrorType<Comparable<Comparable<*>>>)))
            fullList.add(TypeOrField.Type(ConditionGreaterThanMirror(type.TMirror as MirrorType<Comparable<Comparable<*>>>)))
            fullList.add(TypeOrField.Type(ConditionGreaterThanOrEqualMirror(type.TMirror as MirrorType<Comparable<Comparable<*>>>)))
        }
        if(type.TMirror == StringMirror){
            fullList.add(TypeOrField.Type(ConditionStartsWithMirror))
            fullList.add(TypeOrField.Type(ConditionTextSearchMirror))
            fullList.add(TypeOrField.Type(ConditionEndsWithMirror))
            fullList.add(TypeOrField.Type(ConditionRegexTextSearchMirror))
        }
        for(field in type.TMirror.base.fields){
            fullList.add(TypeOrField.Field(field))
        }
        fullList
    }
    //    val selectedTypeOrField = StandardObservableProperty<TypeOrField>(typeOrFieldOptions.find { it.matches(request.observable.value) })
    val selectedTypeOrField = StandardObservableProperty(request.observable.value.valueOrNull?.let { cond -> typeOrFieldOptions.find { it.matches(cond) } } ?: typeOrFieldOptions.first())
    val deferTo = selectedTypeOrField.transformOnChange { typeOrField ->
        when(typeOrField){
            is TypeOrField.Type -> {
                val type = typeOrField.type
                request.sub(
                        type = type as MirrorType<Any?>,
                        observable = request.observable.ensureSubtype(type) as MutableObservableProperty<FormState<Any?>>,
                        scale = request.scale
                ).getVG<DEPENDENCY, VIEW>()
            }
            is TypeOrField.Field<*> -> {
                val field = typeOrField.field as MirrorClass.Field<Any?, Any?>
                val conditionType = ConditionMirror(field.type)
                ConditionFormVG(
                        conditionType,
                        request.sub(
                                type = conditionType,
                                observable = request.observable.ensureSubtype(ConditionFieldMirror(type.TMirror, type.TMirror, field.type)).transform(
                                        mapper = { it.map { (it as Condition.Field<*, *, *>).condition as Condition<Any?> } },
                                        reverseMapper = { it.map { Condition.Field(field, it) } }
                                ),
                                scale = request.scale
                        )
                )
            }
        }

    }

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        vertical {
            -picker(typeOrFieldOptions.asObservableList(), selectedTypeOrField) {
                when(it){
                    is TypeOrField.Field<*> -> it.field.name.humanify()
                    is TypeOrField.Type -> when(it.type){
                        ConditionAlwaysMirror -> "Everything"
                        ConditionNeverMirror -> "Nothing"
                        is ConditionAndMirror<*> -> "Matches all of the following:"
                        is ConditionOrMirror<*> -> "Matches any of the following:"
                        is ConditionEqualMirror<*> -> "="
                        is ConditionNotEqualMirror<*> -> "≠"
                        is ConditionEqualToOneMirror<*> -> "= to any of these:"
                        is ConditionLessThanMirror<*> -> "<"
                        is ConditionLessThanOrEqualMirror<*> -> "≤"
                        is ConditionGreaterThanMirror<*> -> ">"
                        is ConditionGreaterThanOrEqualMirror<*> -> "≥"
                        is ConditionStartsWithMirror -> "Starts with"
                        is ConditionTextSearchMirror -> "Contains"
                        is ConditionEndsWithMirror -> "Ends with"
                        is ConditionRegexTextSearchMirror -> "Matches regular expression"
                        else -> it.type.base.localName.humanify()
                    }
                }
            }
            -swap(deferTo.transform { it.generate(dependency) to Animation.Flip })
        }
    }
}