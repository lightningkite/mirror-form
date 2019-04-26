//package com.lightningkite.mirror.form
//
//import com.lightningkite.reacktive.EnablingMutableCollection
//import com.lightningkite.reacktive.property.MutableObservableProperty
//
//
///**
// * Transforms an observable property from one type to another.
// * Created by jivie on 2/22/16.
// */
//class TransformFormStateMutableObservableProperty<S, T>(
//        val observable: MutableObservableProperty<FormState<S>>,
//        val transformer: (S) -> T,
//        val onSet: (FormState<T>) -> Unit
//) : EnablingMutableCollection<(FormState<T>) -> Unit>(), MutableObservableProperty<FormState<T>> {
//    override var value: FormState<T> = observable.value.breakDown(transformer) ?: FormState.empty()
//        get(){
//            val potentialUpdated = observable.value.breakDown(transformer)
//            if(potentialUpdated != null){
//                field = potentialUpdated
//            }
//            return field
//        }
//        set(value) {
//            field = value
//            forEach { it.invoke(value) }
//            onSet(value)
//        }
//
//    val callback = { a: FormState<S> ->
//        val broken = a.breakDown(transformer)
//        if(broken != null && broken != value) {
//            value = broken
//            forEach { it.invoke(broken) }
//        }
//    }
//
//    override fun enable() {
//        observable.add(callback)
//    }
//
//    override fun disable() {
//        observable.remove(callback)
//    }
//}
//
//fun <S, T> MutableObservableProperty<FormState<S>>.transformFormState(mapper: (S) -> T, onSet: (FormState<T>)->Unit): TransformFormStateMutableObservableProperty<S, T> {
//    return TransformFormStateMutableObservableProperty(this, mapper, onSet)
//}