package com.lightningkite.mirror.form

inline class ViewSize2(val layer: Int = 0) {
    companion object {
        val Full = ViewSize2(2)
        val Summary = ViewSize2(1)
        val SingleLine = ViewSize2(0)
    }

    val isFull get() = layer >= Full.layer
    val isSummary get() = layer == Summary.layer
    val isSingleLine get() = layer == SingleLine.layer
}