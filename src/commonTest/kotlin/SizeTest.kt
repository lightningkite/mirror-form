package com.lightningkite.mirror.form

import kotlin.test.Test
import kotlin.test.assertEquals

class SizeTest {
    @Test fun test(){
        assertEquals(ViewSize.Summary, ViewSize.Full.shrink())
        assertEquals(ViewSize.OneLine, ViewSize.Summary.shrink())
        assertEquals(ViewSize.OneLine, ViewSize.OneLine.shrink())

        assertEquals(ViewSize.Full, ViewSize.Full.grow())
        assertEquals(ViewSize.Full, ViewSize.Summary.grow())
        assertEquals(ViewSize.Summary, ViewSize.OneLine.grow())
    }
}