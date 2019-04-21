package com.lightningkite.mirror.form

import com.lightningkite.koolui.concepts.TextSize

enum class ViewSize {
    OneLine,
    Summary,
    Full;

    fun shrink(): ViewSize = this.ordinal.minus(1).coerceIn(values().indices).let { values()[it] }
    fun grow(): ViewSize = this.ordinal.plus(1).coerceIn(values().indices).let { values()[it] }

    companion object {
        val values = ViewSize.values().toList()
    }

    val bigger: ViewSize
        get() {
            return values.getOrNull(values.indexOf(this) + 1) ?: ViewSize.Full
        }
    val smaller: ViewSize
        get() {
            return values.getOrNull(values.indexOf(this) - 1) ?: ViewSize.OneLine
        }

    fun textSize(): TextSize = when (this) {
        ViewSize.Full -> TextSize.Subheader
        ViewSize.Summary -> TextSize.Body
        ViewSize.OneLine -> TextSize.Body
    }

    fun maxLines(): Int = when (this) {
        ViewSize.Full -> Int.MAX_VALUE
        ViewSize.Summary -> 3
        ViewSize.OneLine -> 1
    }
}
