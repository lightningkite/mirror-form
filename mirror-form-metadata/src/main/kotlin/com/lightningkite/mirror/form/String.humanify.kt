package com.lightningkite.mirror.form

fun String.humanify(): String {
    if(this.isEmpty()) return ""
    return this[0].toUpperCase() + this.replace(Regex("[A-Z]")){ result ->
        " " + result.value
    }.replace('_', ' ').trim().drop(1)
}