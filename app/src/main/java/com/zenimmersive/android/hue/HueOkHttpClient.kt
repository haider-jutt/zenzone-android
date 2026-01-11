package com.zenimmersive.android.hue

import android.content.Context
import com.zenimmersive.android.helper.LogSystem
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Locale
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object HueOkHttpClient {
    private const val protocall = "https"
    private const val ConfigUrl = protocall + "://%s/api/config"
    private const val ApiTokenUrl = protocall + "://%s/api"
    private const val LightListUrl = protocall + "://%s/clip/v2/resource/light"
    private const val LightDetailUrl = protocall + "://%s/clip/v2/resource/light/%s"
    private const val RoomListUrl = protocall + "://%s/clip/v2/resource/room"
    private const val ZoneUrl = protocall + "://%s/clip/v2/resource/zone"
    private const val ZoneDetailUrl = protocall + "://%s/clip/v2/resource/zone/%s"
    private const val GroupedLightUrl = protocall + "://%s/clip/v2/resource/grouped_light"
    private const val GroupedLightDetailUrl = protocall + "://%s/clip/v2/resource/grouped_light/%s"

    fun getConfigurationURL(bridgeIp: String?): String {
        return String.format(Locale.ENGLISH, ConfigUrl, bridgeIp)
    }

    fun getApiTokenURL(bridgeIp: String?): String {
        return String.format(Locale.ENGLISH, ApiTokenUrl, bridgeIp)
    }

    fun getLightURL(bridgeIp: String?): String {
        return String.format(Locale.ENGLISH, LightListUrl, bridgeIp)
    }

    fun getLightURL(bridgeIp: String?, lightId: String?): String {
        return String.format(Locale.ENGLISH, LightDetailUrl, bridgeIp, lightId)
    }

    fun getRoomURL(bridgeIp: String?): String {
        return String.format(Locale.ENGLISH, RoomListUrl, bridgeIp)
    }

    fun getZoneURL(bridgeIp: String?): String {
        return String.format(Locale.ENGLISH, ZoneUrl, bridgeIp)
    }

    fun getZoneURL(bridgeIp: String?, zoneId: String?): String {
        return String.format(Locale.ENGLISH, ZoneDetailUrl, bridgeIp, zoneId)
    }

    fun getGroupedLightURL(bridgeIp: String?): String {
        return String.format(Locale.ENGLISH, GroupedLightUrl, bridgeIp)
    }

    fun getGroupedLightURL(bridgeIp: String?, groupedLightId: String?): String {
        return String.format(Locale.ENGLISH, GroupedLightDetailUrl, bridgeIp, groupedLightId)
    }

    val discoveryURL: String
        get() = "https://discovery.meethue.com/"

    private var okHttpClient: OkHttpClient? = null
    private const val ipAddress = ""

    // Method to clear the OkHttpClient instance
    fun clearClient() {
        okHttpClient = null
    }


    fun generateCertificateFromString(): X509Certificate {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val inputStream = ByteArrayInputStream(certificateString.toByteArray(Charsets.UTF_8))
        return certificateFactory.generateCertificate(inputStream) as X509Certificate
    }

    // Example usage
    val certificateString = """
-----BEGIN CERTIFICATE-----
MIICMjCCAdigAwIBAgIUO7FSLbaxikuXAljzVaurLXWmFw4wCgYIKoZIzj0EAwIw
OTELMAkGA1UEBhMCTkwxFDASBgNVBAoMC1BoaWxpcHMgSHVlMRQwEgYDVQQDDAty
b290LWJyaWRnZTAiGA8yMDE3MDEwMTAwMDAwMFoYDzIwMzgwMTE5MDMxNDA3WjA5
MQswCQYDVQQGEwJOTDEUMBIGA1UECgwLUGhpbGlwcyBIdWUxFDASBgNVBAMMC3Jv
b3QtYnJpZGdlMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEjNw2tx2AplOf9x86
aTdvEcL1FU65QDxziKvBpW9XXSIcibAeQiKxegpq8Exbr9v6LBnYbna2VcaK0G22
jOKkTqOBuTCBtjAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBhjAdBgNV
HQ4EFgQUZ2ONTFrDT6o8ItRnKfqWKnHFGmQwdAYDVR0jBG0wa4AUZ2ONTFrDT6o8
ItRnKfqWKnHFGmShPaQ7MDkxCzAJBgNVBAYTAk5MMRQwEgYDVQQKDAtQaGlsaXBz
IEh1ZTEUMBIGA1UEAwwLcm9vdC1icmlkZ2WCFDuxUi22sYpLlwJY81Wrqy11phcO
MAoGCCqGSM49BAMCA0gAMEUCIEBYYEOsa07TH7E5MJnGw557lVkORgit2Rm1h3B2
sFgDAiEA1Fj/C3AN5psFMjo0//mrQebo0eKd3aWRx+pQY08mk48=
-----END CERTIFICATE-----
""".trimIndent()

    @JvmStatic
    fun getInstance(): OkHttpClient {
        return buildOkHttpClient()!!
    }

    @JvmStatic
    fun getInstance(context: Context, bridgeIp: String): OkHttpClient {
        if (okHttpClient != null) return okHttpClient!!
        okHttpClient = buildOkHttpClient()
        return okHttpClient!!
    }

    private fun buildOkHttpClient(): OkHttpClient? {
        var okHttpClient: OkHttpClient? = null
        try {
            //Hue Bridge Root CA
            val certificate = generateCertificateFromString()

            // Create a KeyStore containing the trusted certificate
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null)
                setCertificateEntry("ca", certificate)
            }

            // Create a TrustManager that trusts the certificate in the KeyStore
            val trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    .apply {
                        init(keyStore)
                    }
            val trustManagers =
                trustManagerFactory.trustManagers.filterIsInstance<X509TrustManager>()

            // Custom TrustManager to debug certificate chain
            val customTrustManager = object : X509TrustManager {
                override fun checkClientTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?
                ) {
                    trustManagers.forEach {
                        it.checkClientTrusted(chain, authType)
                    }
                }

                override fun checkServerTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?
                ) {

                    trustManagers.forEach {
                        try {
                            it.checkServerTrusted(chain, authType)
                        } catch (e: Exception) {
//                            LogSystem.e(
//                                "TrustManager",
//                                "Certificate chain validation failed",
//                                e
//                            )
                        }
                    }

                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    var acceptedIssuers = arrayOf<X509Certificate>()
                    trustManagers.forEach {
                        acceptedIssuers =
                            acceptedIssuers.plus(it.acceptedIssuers)
                    }
                    return acceptedIssuers
                }
            }

            // Create an SSLContext that uses the custom TrustManager
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf<TrustManager>(customTrustManager), SecureRandom())
            }

            // Build the OkHttpClient with the SSLContext and a HostnameVerifier
            val builder = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor())
                .sslSocketFactory(sslContext.socketFactory, customTrustManager)
                .hostnameVerifier { _, _ -> true }

            // Handle different Android versions
            //builder.connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))

            okHttpClient = builder.build()
        } catch (e: Exception) {
            e.printStackTrace()
            LogSystem.e("Error", "Http Client Initialization", e)
            okHttpClient = OkHttpClient.Builder().build()
        }
        return okHttpClient
    }
}
