package com.lightningkite.mirror.form

enum class ViewSize {
    Full,
    Summary,
    OneLine;

    fun shrink(): ViewSize = this.ordinal.plus(1).coerceIn(values().indices).let { values()[it] }
    fun grow(): ViewSize = this.ordinal.minus(1).coerceIn(values().indices).let { values()[it] }

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
}
}