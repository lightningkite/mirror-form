package com.lightningkite.mirror.form

import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.MirrorType

interface CommonRequest<T> {
    val general: GeneralRequest
    val type: MirrorType<T>
    val scale: ViewSize
    val owningField: MirrorClass.Field<*, *>?
}