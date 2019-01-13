package com.lightningkite.mirror.form

import com.lightningkite.koolui.concepts.Importance
import com.lightningkite.koolui.concepts.TextSize
import com.lightningkite.mirror.info.FieldInfo


data class ViewContext(
        val fieldInfo: FieldInfo<*, *>? = null,
        val owner: Any? = null,
        val size: ViewSize = ViewSize.Full,
        val importance: Float = fieldInfo?.importance ?: .5f
) {


    val textSize: TextSize
        get() = when (size) {
            ViewSize.Full -> TextSize.Header
            ViewSize.Summary -> when (importance) {
                in Float.NEGATIVE_INFINITY..0.3f -> TextSize.Tiny
                in 0.3f..0.7f -> TextSize.Body
                in 0.7f..0.9f -> TextSize.Subheader
                else -> TextSize.Header
            }
            ViewSize.Footnote -> when (importance) {
                in Float.NEGATIVE_INFINITY..0.3f -> TextSize.Tiny
                in 0.3f..0.7f -> TextSize.Tiny
                in 0.7f..0.9f -> TextSize.Body
                else -> TextSize.Body
            }
        }

    val viewImportance: Importance
        get() = when (size) {
            ViewSize.Full -> Importance.High
            ViewSize.Summary -> when (importance) {
                in Float.NEGATIVE_INFINITY..0.3f -> Importance.Low
                in 0.3f..0.7f -> Importance.Normal
                in 0.7f..0.9f -> Importance.High
                else -> Importance.High
            }
            ViewSize.Footnote -> when (importance) {
                in Float.NEGATIVE_INFINITY..0.3f -> Importance.Low
                in 0.3f..0.7f -> Importance.Low
                in 0.7f..0.9f -> Importance.Normal
                else -> Importance.Normal
            }
        }

    val maxLines: Int
        get() = when(size){
            ViewSize.Full -> Int.MAX_VALUE
            ViewSize.Summary -> 3
            ViewSize.Footnote -> 1
        }
}