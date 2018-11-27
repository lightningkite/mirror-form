package com.lightningkite.koolui

import org.w3c.dom.HTMLElement
import kotlin.browser.document

fun HTMLElement.appendLifecycled(other: HTMLElement) {
    other.lifecycle.parent = this.lifecycle
    appendChild(other)
}

fun HTMLElement.removeLifecycled(other: HTMLElement) {
    removeChild(other)
    other.lifecycle.parent = null
}

@Suppress("UNCHECKED_CAST")
fun <T : HTMLElement> HTMLElement.appendLifecycled(name: String, setup: T.() -> Unit): T {
    val newNode = document.createElement(name).let { it as T }.apply(setup)
    newNode.lifecycle.parent = this.lifecycle
    appendChild(newNode)
    return newNode
}

@Suppress("UNCHECKED_CAST")
fun <T : HTMLElement> makeElement(name: String, setup: T.() -> Unit): T =
    document.createElement(name).let { it as T }.apply(setup)