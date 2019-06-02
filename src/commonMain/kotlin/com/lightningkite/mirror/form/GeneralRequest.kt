package com.lightningkite.mirror.form

import com.lightningkite.kommon.native.ensureNeverFrozen
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.archive.database.DatabaseMap
import com.lightningkite.mirror.archive.model.Subgraph
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.request.Request
import com.lightningkite.reacktive.list.MutableObservableList
import com.lightningkite.reacktive.list.WrapperObservableList

data class GeneralRequest(
        val developerMode: Boolean = false,
        val nullString: String = "N/A",
        val impliedFields: Map<MirrorClass.Field<*, *>, Any?> = mapOf(),
        val databases: Database.Provider = DatabaseMap(mapOf()),
        val subgraph: Subgraph = Subgraph(),
        val requestHandler: Request.Handler? = null,
        val stack: MutableObservableList<*> = WrapperObservableList<Any?>()
) {
    init {
        ensureNeverFrozen()
    }

    @Suppress("UNCHECKED_CAST")
    fun <DEPENDENCY : ViewFactory<VIEW>, VIEW> stack() = stack as MutableObservableList<ViewGenerator<DEPENDENCY, VIEW>>
}

