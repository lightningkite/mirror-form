package com.lightningkite.mirror.form.form

import com.lightningkite.kommon.atomic.AtomicValue
import com.lightningkite.koolui.Location
import com.lightningkite.koolui.async.UI
import com.lightningkite.koolui.builders.*
import com.lightningkite.koolui.concepts.Importance
import com.lightningkite.koolui.concepts.TextInputType
import com.lightningkite.koolui.geometry.AlignPair
import com.lightningkite.koolui.image.MaterialIcon
import com.lightningkite.koolui.image.color
import com.lightningkite.koolui.image.withOptions
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.lokalize.location.Geohash
import com.lightningkite.mirror.form.FormState
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
    val latitude: SemiboundObservableProperty<Double?, FormState<Geohash>> = SemiboundObservableProperty(
            startValue = observable.value.valueOrNull?.latitude,
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
    val longitude: SemiboundObservableProperty<Double?, FormState<Geohash>> = SemiboundObservableProperty(
            startValue = observable.value.valueOrNull?.longitude,
            source = observable,
            consumeUpdate = {
                value = it.valueOrNull?.longitude
            },
            tryWrite = { long ->
                if(long != null) {
                    latitude.value?.let { lat ->
                        value = FormState.success(Geohash(lat, long))
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
    val isGettingLocation = StandardObservableProperty(false)

    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        vertical {
            -align(
                    AlignPair.FillFill to textArea(address, placeholder = "Address or ZIP", type = TextInputType.Address),
                    AlignPair.TopRight to work(space(1f), geocoding)
            )
            -horizontal {
                +numberField(latitude, placeholder = "Latitude", decimalPlaces = 5)
                +numberField(longitude, placeholder = "Longitude", decimalPlaces = 5)
            }
            -horizontal {
                +textField(hash, type = TextInputType.CapitalizedIdentifier, placeholder = "Geohash")
                -work(imageButton(MaterialIcon.myLocation.color(colorSet.foreground).withOptions(), importance = Importance.Low) {
                    GlobalScope.launch {
                        isGettingLocation.value = true
                        try {
                            observable.value = FormState.success(Location.requestOnce("Get your location once to insert it into the form", 100.0).location)
                        } catch(e:Exception){

                        }
                        isGettingLocation.value = false
                    }
                    Unit
                }, isGettingLocation)
            }
        }.apply {

            Unit
        }
    }
}