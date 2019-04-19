package com.lightningkite.mirror.form

import com.lightningkite.mirror.info.FieldInfo

interface ViewContextStack {
    val contexts: MutableList<ViewContext>
    val context get() = contexts.last()
    fun useContext(context: ViewContext, action: () -> Unit) {
        contexts.add(context)
        action()
        contexts.removeAt(contexts.lastIndex)
    }
    fun useContext(
            fieldInfo: FieldInfo<*, *>? = null,
            owner: Any? = null,
            size: ViewSize = ViewSize.Full,
            importance: Float = fieldInfo?.importance ?: .5f,
            action: () -> Unit
    ) {
        contexts.add(ViewContext(fieldInfo, owner, size, importance))
        action()
        contexts.removeAt(contexts.lastIndex)
    }
    fun forField(
            owner: Any?,
            fieldInfo: FieldInfo<*, *>,
            size: ViewSize = context.size.shrink(),
            importance: Float = fieldInfo.importance,
            action:()->Unit
    ) = useContext(ViewContext(
            fieldInfo = fieldInfo,
            owner = owner,
            size = size,
            importance = importance
    ), action)
    fun shrink(action:()->Unit) = useContext(context.copy(size = context.size.shrink()), action)
}