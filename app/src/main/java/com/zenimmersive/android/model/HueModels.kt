package com.zenimmersive.android.model


import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


data class HueRoomListResult(

    @SerializedName("errors") var errors: ArrayList<String> = arrayListOf(),
    @SerializedName("data") var data: ArrayList<HueRoom> = arrayListOf()

)

data class HueLightListResult(

    @SerializedName("errors") var errors: ArrayList<String> = arrayListOf(),
    @SerializedName("data") var data: ArrayList<Light> = arrayListOf()

)

data class Light(

    @SerializedName("id") var id: String? = null,
    @SerializedName("id_v1") var idV1: String? = null,
    @SerializedName("owner") var owner: Owner? = Owner(),
    @SerializedName("metadata") var metadata: Metadata? = Metadata(),
    @SerializedName("product_data") var productData: ProductData? = ProductData(),
    @SerializedName("identify") var identify: Identify? = Identify(),
    @SerializedName("service_id") var serviceId: Int? = null,
    @SerializedName("on") var on: On? = On(),
    @SerializedName("dimming") var dimming: Dimming? = Dimming(),
    @SerializedName("dimming_delta") var dimmingDelta: DimmingDelta? = DimmingDelta(),
    @SerializedName("color_temperature") var colorTemperature: ColorTemperature? = ColorTemperature(),
    @SerializedName("color_temperature_delta") var colorTemperatureDelta: ColorTemperatureDelta? = ColorTemperatureDelta(),
    @SerializedName("color") var color: Color? = Color(),
    @SerializedName("dynamics") var dynamics: Dynamics? = Dynamics(),
    @SerializedName("alert") var alert: Alert? = Alert(),
    @SerializedName("signaling") var signaling: Signaling? = Signaling(),
    @SerializedName("mode") var mode: String? = null,
    @SerializedName("type") var type: String? = null,

    ) {
    @Expose
    var systemUseCase: Boolean? = false
    @Expose
    var preState: Color? = null
    @Expose
    var preBrightness: Float? = 80f
}

data class Owner(

    @SerializedName("rid") var rid: String? = null,
    @SerializedName("rtype") var rtype: String? = null

)

data class HueDevice(
    @SerializedName("id") var id: String? = null,
    @SerializedName("id_v1") var idV1: String? = null,
    @SerializedName("product_data") var productData: ProductData? = ProductData(),
    @SerializedName("metadata") var metadata: Metadata? = Metadata(),
    @SerializedName("identify") var identify: Identify? = Identify(),
    @SerializedName("services") var services: ArrayList<Services> = arrayListOf(),
    @SerializedName("type") var type: String? = null
)

data class HueRoom(
    @SerializedName("id") var id: String? = null,
    @SerializedName("id_v1") var idV1: String? = null,
    @SerializedName("children") var children: ArrayList<Children> = arrayListOf(),
    @SerializedName("services") var services: ArrayList<Services> = arrayListOf(),
    @SerializedName("metadata") var metadata: Metadata? = Metadata(),
    @SerializedName("type") var type: String? = null
) {
    @SerializedName("localRoomLightList")
    var roomLightList: List<Light>? = null
}

data class Children(

    @SerializedName("rid") var rid: String? = null,
    @SerializedName("rtype") var rtype: String? = null

)

data class Services(

    @SerializedName("rid") var rid: String? = null,
    @SerializedName("rtype") var rtype: String? = null


)

data class Metadata(

    @SerializedName("name") var name: String? = null,
    @SerializedName("archetype") var archetype: String? = null,
    @SerializedName("function") var function: String? = null

)


data class ProductData(

    @SerializedName("model_id") var modelId: String? = null,
    @SerializedName("manufacturer_name") var manufacturerName: String? = null,
    @SerializedName("product_name") var productName: String? = null,
    @SerializedName("product_archetype") var productArchetype: String? = null,
    @SerializedName("certified") var certified: Boolean? = null,
    @SerializedName("software_version") var softwareVersion: String? = null,
    @SerializedName("hardware_platform_type") var hardwarePlatformType: String? = null,
    @SerializedName("function") var function: String? = null

)

class Identify
class DimmingDelta
class ColorTemperatureDelta

data class On(

    @SerializedName("on") var on: Boolean? = null

)

data class Dimming(

    @SerializedName("brightness") var brightness: Float? = null,
    @SerializedName("min_dim_level") var minDimLevel: Float? = null

)


data class MirekSchema(

    @SerializedName("mirek_minimum") var mirekMinimum: Int? = null,
    @SerializedName("mirek_maximum") var mirekMaximum: Int? = null

)

data class ColorTemperature(

    @SerializedName("mirek") var mirek: Int? = null,
    @SerializedName("mirek_valid") var mirekValid: Boolean? = null,
    @SerializedName("mirek_schema") var mirekSchema: MirekSchema? = MirekSchema()

)

data class Xy(

    @SerializedName("x") var x: Double? = null, @SerializedName("y") var y: Double? = null

)

data class Red(

    @SerializedName("x") var x: Double? = null, @SerializedName("y") var y: Double? = null

)

data class Green(

    @SerializedName("x") var x: Double? = null, @SerializedName("y") var y: Double? = null

)

data class Blue(

    @SerializedName("x") var x: Double? = null, @SerializedName("y") var y: Double? = null

)

data class Gamut(

    @SerializedName("red") var red: Red? = Red(),
    @SerializedName("green") var green: Green? = Green(),
    @SerializedName("blue") var blue: Blue? = Blue()

)

data class Color(

    @SerializedName("xy") var xy: Xy? = Xy(),
    @SerializedName("gamut") var gamut: Gamut? = Gamut(),
    @SerializedName("gamut_type") var gamutType: String? = null

)

data class Dynamics(

    @SerializedName("status") var status: String? = null,
    @SerializedName("status_values") var statusValues: ArrayList<String> = arrayListOf(),
    @SerializedName("speed") var speed: Int? = null,
    @SerializedName("speed_valid") var speedValid: Boolean? = null

)

data class Alert(

    @SerializedName("action_values") var actionValues: ArrayList<String> = arrayListOf()

)

data class Signaling(

    @SerializedName("signal_values") var signalValues: ArrayList<String> = arrayListOf()

)


class LightListResult {
    fun hasLights(): Boolean {
        if (lights != null && lights!!.isNotEmpty()) return true
        if (unConfigLights != null && unConfigLights!!.isNotEmpty()) return true
        return false
    }

    @SerializedName("error")
    var error: String? = null

    @SerializedName("rooms")
    var rooms: List<HueRoom>? = null

    @SerializedName("lights")
    var lights: List<Light>? = null

    @SerializedName("unConfigLights")
    var unConfigLights: List<Light>? = null
}

data class HueZoneListResult(
    @SerializedName("errors") var errors: ArrayList<String> = arrayListOf(),
    @SerializedName("data") var data: ArrayList<HueZone> = arrayListOf()
)

data class HueZone(
    @SerializedName("id") var id: String? = null,
    @SerializedName("id_v1") var idV1: String? = null,
    @SerializedName("children") var children: ArrayList<Children> = arrayListOf(),
    @SerializedName("services") var services: ArrayList<Services> = arrayListOf(),
    @SerializedName("metadata") var metadata: Metadata? = Metadata(),
    @SerializedName("type") var type: String? = null
)

data class EffectsV2(
    @SerializedName("action") var action: EffectAction? = null,
    @SerializedName("status") var status: String? = null,
    @SerializedName("type") var type: String? = null
)

data class EffectAction(
    @SerializedName("effect") var effect: String? = null
)