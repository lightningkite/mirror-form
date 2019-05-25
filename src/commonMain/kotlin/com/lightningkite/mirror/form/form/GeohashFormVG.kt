package com.lightningkite.mirror.form.form

import com.lightningkite.kommon.exception.stackTraceString
import com.lightningkite.koolui.Location
import com.lightningkite.koolui.async.UI
import com.lightningkite.koolui.builders.horizontal
import com.lightningkite.koolui.builders.imageButton
import com.lightningkite.koolui.builders.space
import com.lightningkite.koolui.builders.vertical
import com.lightningkite.koolui.image.color
import com.lightningkite.koolui.image.withSizing
import com.lightningkite.koolui.views.ViewFactory
import com.lightningkite.koolui.views.ViewGenerator
import com.lightningkite.lokalize.location.Geohash
import com.lightningkite.mirror.form.FormState
import com.lightningkite.reacktive.property.CombineObservableProperty2
import com.lightningkite.reacktive.property.MutableObservableProperty
import com.lightningkite.reacktive.property.StandardObservableProperty
import com.lightningkite.reacktive.property.lifecycle.bind
import com.lightningkite.reacktive.property.lifecycle.listen
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GeohashFormVG<DEPENDENCY: ViewFactory<VIEW>, VIEW>(val observable: MutableObservableProperty<FormState<Geohash>>) : ViewGenerator<DEPENDENCY, VIEW> {
    val geocoding = StandardObservableProperty(false)
    val address = StandardObservableProperty("")
    val latitude = StandardObservableProperty<Number?>(null)
    val longitude = StandardObservableProperty<Number?>(null)
    val hash = StandardObservableProperty("")
    override fun generate(dependency: DEPENDENCY): VIEW = with(dependency) {
        vertical {
            -align(
                    com.lightningkite.koolui.geometry.AlignPair.FillFill to textArea(address, placeholder = "Address or ZIP", type = com.lightningkite.koolui.concepts.TextInputType.Address),
                    com.lightningkite.koolui.geometry.AlignPair.TopRight to work(space(1f), geocoding)
            )
            -horizontal {
                -numberField(latitude, placeholder = "Latitude", type = com.lightningkite.koolui.concepts.NumberInputType.Float, decimalPlaces = 5)
                -numberField(longitude, placeholder = "Longitude", type = com.lightningkite.koolui.concepts.NumberInputType.Float, decimalPlaces = 5)
            }
            -horizontal {
                -textField(hash, type = com.lightningkite.koolui.concepts.TextInputType.CapitalizedIdentifier, placeholder = "Geohash")
                -imageButton(com.lightningkite.koolui.image.MaterialIcon.myLocation.color(colorSet.foreground).withSizing(), importance = com.lightningkite.koolui.concepts.Importance.Low) {
                    com.lightningkite.koolui.Location.requestOnce("Get your location once to insert it into the form") {
                        observable.value = FormState.success(it.location)
                    }
                }
            }
        }.apply {
            var setFromAddress = false
            var setFromLatLng = false
            var setFromHash = false
            lifecycle.bind(observable) {
                if (setFromHash) {
                    setFromHash = false
                } else {
                    hash.value = it.valueOrNull?.toString() ?: ""
                }

                if (setFromLatLng) {
                    setFromLatLng = false
                } else {
                    latitude.value = it.valueOrNull?.latitude
                    longitude.value = it.valueOrNull?.longitude
                }

                if (setFromAddress) {
                    setFromAddress = false
                } else {
                    it.valueOrNull?.let { geohash ->
                        GlobalScope.launch(kotlinx.coroutines.Dispatchers.UI) {
                            geocoding.value = true
                            var lastValue: Geohash? = null
                            while(lastValue != observable.value.valueOrNull){
                                lastValue = observable.value.valueOrNull
                                delay(1000)
                            }
                            address.value = Location.getAddress(geohash) ?: ""
                            geocoding.value = false
                        }
                    } ?: run {
                        address.value = ""
                    }
                }
            }

            lifecycle.listen(address) {
                if(it.isNotBlank()) {
                    GlobalScope.launch(kotlinx.coroutines.Dispatchers.UI) {
                        geocoding.value = true
                        var lastValue: String? = null
                        while(lastValue != address.value){
                            lastValue = address.value
                            delay(1000)
                        }
                        Location.getGeohash(it)?.let { FormState.success(it) }?.let {
                            setFromAddress = true
                            observable.value = it
                        }
                        geocoding.value = false
                    }
                }
                Unit
            }

            lifecycle.listen(CombineObservableProperty2(latitude, longitude) { a, b -> a to b }) { (lat, lng) ->
                lat?.let { lat ->
                    lng?.let { lng ->
                        setFromLatLng = true
                        observable.value = FormState.success(Geohash(lat.toDouble(), lng.toDouble()))
                    }
                }

                Unit
            }

            lifecycle.listen(hash) {
                if (it.isNotEmpty()) {
                    try {
                        observable.value = FormState.success(Geohash(it))
                    } catch(e:Exception){
                        println(e.stackTraceString())
                    }
                }
            }
            Unit
        }
    }
}