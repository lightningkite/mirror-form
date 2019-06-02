package com.lightningkite.mirror.form.form

import com.lightningkite.kommon.atomic.AtomicValue
import com.lightningkite.koolui.Location
import com.lightningkite.koolui.async.UI
import com.lightningkite.koolui.builders.horizontal
import com.lightningkite.koolui.builders.imageButton
import com.lightningkite.koolui.builders.space
import com.lightningkite.koolui.builders.vertical
import com.lightningkite.koolui.concepts.Importance
import com.lightningkite.koolui.concepts.NumberInputType
import com.lightningkite.koolui.concepts.TextInputType
import com.lightningkite.koolui.geometry.AlignPair
import com.lightningkite.koolui.image.color
import com.lightningkite.koolui.image.withSizing
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.lokalize.location.Geohash
import com.lightningkite.mirror.form.FormState
import com.lightningkite.reacktive.EnablingMutableCollection
import com.lightningkite.reacktive.invokeAll
import com.lightningkite.reacktive.property.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GeohashFormVG<DEPENDENCY : ViewFactory<VIEW>, VIEW>(val observable: MutableObservableProperty<FormState<Geohash>>) : ViewGenerator<DEPENDENCY, VIEW> {
    val gettingAddressFromGeohash = AtomicValue(false)
    val gettingGeohashFromAddress = AtomicValue(false)

    val geocoding = StandardObservableProperty(false)
    val address = SemiboundObservableProperty(
            startValue = "",
            source = observable,
            consumeUpdate = {
                GlobalScope.launch(kotlinx.coroutines.Dispatchers.UI) {
                    if(!gettingAddressFromGeohash.compareAndSet(false, true)) {
                        return@launch
                    }
                    geocoding.value = true
                    var lastValue: Geohash? = null
                    while (lastValue != observable.value.valueOrNull) {
                        lastValue = observable.value.valueOrNull
                        delay(1000)
                    }
                    gettingAddressFromGeohash.value = false
                    if (lastValue == null) {
                        value = ""
                    } else {
                        value = Location.getAddress(lastValue) ?: ""
                    }
                    geocoding.value = false
                }
            },
            tryWrite = {
                GlobalScope.launch(kotlinx.coroutines.Dispatchers.UI) {
                    if(!gettingGeohashFromAddress.compareAndSet(false, true)) {
                        return@launch
                    }
                    geocoding.value = true
                    var lastValue: String = ""
                    while (lastValue != self.value) {
                        lastValue = self.value
                        delay(1000)
                    }
                    gettingGeohashFromAddress.value = false
                    if (lastValue.isNotBlank()) {
                        value = Location.getGeohash(lastValue)?.let { FormState.success(it) }
                                ?: FormState.empty()
                    }
                    geocoding.value = false
                }
            }
    )
    val latitude: SemiboundObservableProperty<Number?, FormState<Geohash>> = SemiboundObservableProperty(
            startValue = observable.value.valueOrNull?.latitude as? Number,
            source = observable,
            consumeUpdate = {
                value = it.valueOrNull?.latitude
            },
            tryWrite = { lat ->
                if(lat != null) {
                    longitude.value?.let { long ->
                        value = FormState.success(Geohash(lat.toDouble(), long.toDouble()))
                    }
                }
            }
    )
    val longitude: SemiboundObservableProperty<Number?, FormState<Geohash>> = SemiboundObservableProperty(
            startValue = observable.value.valueOrNull?.longitude as? Number,
            source = observable,
            consumeUpdate = {
                value = it.valueOrNull?.longitude
            },
            tryWrite = { long ->
                if(long != null) {
                    latitude.value?.let { lat ->
                        value = FormState.success(Geohash(lat.toDouble(), long.toDouble()))
                    }
                }
            }
    )
    val hash = SemiboundObservableProperty(
            startValue = observable.value.valueOrNull?.toString() ?: "",
            source = observable,
            consumeUpdate = {
                value = it.valueOrNull?.toString() ?: ""
            },
            tryWrite = { text ->
                if(text.isNotBlank()) {
                    value = FormState.success(Geohash(text))
                }
            }
    )

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        vertical {
            -align(
                    AlignPair.FillFill to textArea(address, placeholder = "Address or ZIP", type = TextInputType.Address),
                    AlignPair.TopRight to work(space(1f), geocoding)
            )
            -horizontal {
                +numberField(latitude, placeholder = "Latitude", type = NumberInputType.Float, decimalPlaces = 5)
                +numberField(longitude, placeholder = "Longitude", type = NumberInputType.Float, decimalPlaces = 5)
            }
            -horizontal {
                +textField(hash, type = TextInputType.CapitalizedIdentifier, placeholder = "Geohash")
                -imageButton(com.lightningkite.koolui.image.MaterialIcon.myLocation.color(colorSet.foreground).withSizing(), importance = Importance.Low) {
                    Location.requestOnce("Get your location once to insert it into the form") {
                        observable.value = FormState.success(it.location)
                    }
                }
            }
        }.apply {

            Unit
        }
    }
}