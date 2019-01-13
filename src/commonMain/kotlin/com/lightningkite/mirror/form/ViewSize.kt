package com.lightningkite.mirror.form

enum class ViewSize {
    Full,
    Summary,
    Footnote;

    fun shrink(): ViewSize = this.ordinal.plus(1).coerceIn(values().indices).let { values()[it] }
    fun grow(): ViewSize = this.ordinal.minus(1).coerceIn(values().indices).let { values()[it] }
}