package com.zenimmersive.android.hue

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import com.zenimmersive.android.helper.LogSystem
import java.net.InetAddress
import java.util.concurrent.CopyOnWriteArrayList

class HueMdnsDiscovery(private val context: Context) {

    private val nsdManager: NsdManager? = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val discoveredBridges = CopyOnWriteArrayList<Bridge>()
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private val SERVICE_TYPE = "_hue._tcp"
    private val lock = Any()
    private var isDiscovering = false

    interface DiscoveryCallback {
        fun onBridgeFound(bridge: Bridge)
        fun onDiscoveryFinished(bridges: List<Bridge>)
        fun onError(error: String)
    }

    fun startDiscovery(callback: DiscoveryCallback, timeoutMs: Long = 10000) {
        synchronized(lock) {
            if (isDiscovering) {
                stopDiscovery()
            }
            isDiscovering = true
            discoveredBridges.clear()
        }

        try {
            // Acquire MulticastLock to receive mDNS packets
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            multicastLock = wifi.createMulticastLock("HueMdnsLock")
            multicastLock?.setReferenceCounted(true)
            multicastLock?.acquire()
            LogSystem.e("HueMdns", "MulticastLock acquired")

            discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {
                    LogSystem.e("HueMdns", "Service discovery started")
                }

                override fun onServiceFound(service: NsdServiceInfo) {
                    LogSystem.e("HueMdns", "Service found: $service")
                    if (service.serviceType.contains("_hue")) {
                        nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                                LogSystem.e("HueMdns", "Resolve failed: $errorCode")
                            }

                            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                                LogSystem.e("HueMdns", "Resolve Succeeded. $serviceInfo")
                                val host: InetAddress = serviceInfo.host
                                val ip = host.hostAddress
                                val id = serviceInfo.serviceName // Often the bridge ID is part of the name

                                if (!ip.isNullOrEmpty()) {
                                    val bridge = Bridge(
                                        id = id ?: "Unknown",
                                        ipAddress = ip,
                                        port = serviceInfo.port
                                    )
                                    // Avoid duplicates
                                    var exists = false
                                    for(b in discoveredBridges) {
                                        if (b.ipAddress == ip) exists = true
                                    }
                                    
                                    if (!exists) {
                                        discoveredBridges.add(bridge)
                                        Handler(Looper.getMainLooper()).post {
                                            callback.onBridgeFound(bridge)
                                        }
                                    }
                                }
                            }
                        })
                    }
                }

                override fun onServiceLost(service: NsdServiceInfo) {
                    // LogSystem.e("HueMdns", "service lost: $service")
                }

                override fun onDiscoveryStopped(serviceType: String) {
                    LogSystem.e("HueMdns", "Discovery stopped: $serviceType")
                }

                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    LogSystem.e("HueMdns", "Discovery failed: Error code:$errorCode")
                    stopDiscovery()
                    Handler(Looper.getMainLooper()).post {
                        callback.onError("Discovery Start Failed: $errorCode")
                    }
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    LogSystem.e("HueMdns", "Discovery failed: Error code:$errorCode")
                    stopDiscovery()
                }
            }

            nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)

            // Stop discovery after timeout
            Handler(Looper.getMainLooper()).postDelayed({
                stopDiscovery()
                callback.onDiscoveryFinished(discoveredBridges)
            }, timeoutMs)

        } catch (e: Exception) {
            LogSystem.e("HueMdns", "Critical error starting discovery: ${e.message}")
            stopDiscovery()
            callback.onError(e.message ?: "Unknown error")
        }
    }

    fun stopDiscovery() {
        synchronized(lock) {
            if (isDiscovering && discoveryListener != null) {
                try {
                    nsdManager?.stopServiceDiscovery(discoveryListener)
                } catch (e: Exception) {
                    // Ignore if already stopped or failed
                }
                isDiscovering = false
                discoveryListener = null
            }
            
            // Release Multicast Lock
            if (multicastLock != null && multicastLock!!.isHeld) {
                multicastLock?.release()
                LogSystem.e("HueMdns", "MulticastLock released")
            }
            multicastLock = null
        }
    }
}
