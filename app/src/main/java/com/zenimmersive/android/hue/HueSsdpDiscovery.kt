package com.zenimmersive.android.hue

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.zenimmersive.android.helper.LogSystem
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

class HueSsdpDiscovery(private val context: Context) {

    private val SEARCH_PORT = 1900
    private val SEARCH_ADDRESS = "239.255.255.250"
    private val SSDP_MX = 1
    private val SSDP_ST = "upnp:rootdevice"
    private val NEWLINE = "\r\n"
    private val SEARCH_MESSAGE = (
            "M-SEARCH * HTTP/1.1" + NEWLINE +
            "HOST: $SEARCH_ADDRESS:$SEARCH_PORT" + NEWLINE +
            "MAN: \"ssdp:discover\"" + NEWLINE +
            "MX: $SSDP_MX" + NEWLINE +
            "ST: $SSDP_ST" + NEWLINE +
            NEWLINE
    ).toByteArray()

    private var socket: DatagramSocket? = null
    private var isSearching = false

    interface DiscoveryCallback {
        fun onBridgeFound(bridge: Bridge)
        fun onDiscoveryFinished(bridges: List<Bridge>)
        fun onError(error: String)
    }

    fun startDiscovery(callback: DiscoveryCallback, timeoutMs: Long = 10000) {
        if (isSearching) return
        isSearching = true

        val discoveredBridges = ArrayList<Bridge>()

        Thread {
            try {
                socket = DatagramSocket(null)
                socket?.reuseAddress = true
                socket?.bind(InetSocketAddress(0)) // Bind to any available port
                
                // Send discovery packet
                val address = InetAddress.getByName(SEARCH_ADDRESS)
                val packet = DatagramPacket(SEARCH_MESSAGE, SEARCH_MESSAGE.size, address, SEARCH_PORT)
                
                // Send a few times to be sure
                for (i in 0..2) {
                    socket?.send(packet)
                    Thread.sleep(100)
                }

                val buffer = ByteArray(2048)
                val receivePacket = DatagramPacket(buffer, buffer.size)

                // Receive loop
                val endTime = System.currentTimeMillis() + timeoutMs
                socket?.soTimeout = 1000 // 1s socket timeout to allow checking loop condition

                while (isSearching && System.currentTimeMillis() < endTime) {
                    try {
                        socket?.receive(receivePacket)
                        val message = String(receivePacket.data, 0, receivePacket.length)
                        
                        // Parse simple response (Hue bridges identify as such)
                        // Look for "IpBridge" or "hue-bridgeid" in the response
                        if (message.contains("IpBridge", ignoreCase = true) || 
                            message.contains("hue-bridge", ignoreCase = true) ||
                            message.contains("signify", ignoreCase = true)) {
                            
                            LogSystem.e("HueSSDP", "Potential bridge found: $message")
                            
                            // Extract Location header for IP or just take sender IP
                            // LOCATION: http://192.168.1.100:80/description.xml
                            var ip: String? = receivePacket.address.hostAddress
                            var id = "Unknown"
                            
                            // Try to parse Location header for cleaner IP if available
                            val lines = message.split(NEWLINE)
                            for(line in lines) {
                                if (line.startsWith("LOCATION:", ignoreCase = true)) {
                                     try {
                                         // http://192.168.1.50:80/description.xml
                                         val parts = line.substring(9).trim().split(":")
                                          // Simple extraction - relying on sender IP is often safer for UDP
                                          // but let's stick to Sender IP which is 100% the device
                                     } catch (e: Exception) {}
                                }
                                if (line.startsWith("USN:", ignoreCase = true)) {
                                    id = line.substring(4).trim()
                                }
                            }

                            if (!ip.isNullOrEmpty()) {
                                // Check if already added
                                var exists = false
                                for (b in discoveredBridges) {
                                    if (b.ipAddress == ip) exists = true
                                }
                                
                                if (!exists) {
                                     val bridge = Bridge(
                                        id = id,
                                        ipAddress = ip
                                    )
                                    discoveredBridges.add(bridge)
                                    Handler(Looper.getMainLooper()).post {
                                        callback.onBridgeFound(bridge)
                                    }
                                }
                            }
                        }

                    } catch (e: Exception) {
                        // Socket timeout, loop again
                    }
                }
            } catch (e: Exception) {
                LogSystem.e("HueSSDP", "Error in discovery: ${e.message}")
            } finally {
                socket?.close()
                isSearching = false
                Handler(Looper.getMainLooper()).post {
                    callback.onDiscoveryFinished(discoveredBridges)
                }
            }
        }.start()
    }

    fun stopDiscovery() {
        isSearching = false
        socket?.close()
    }
}
