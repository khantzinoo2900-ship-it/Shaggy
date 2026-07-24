package com.v2ray.vpn.parser

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

object V2RayConfigParser {

    fun parseV2RayUrl(v2rayUrl: String): String {
        try {
            if (v2rayUrl.startsWith("vmess://")) {
                val base64Data = v2rayUrl.substring(8)
                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT or Base64.URL_SAFE)
                val jsonString = String(decodedBytes, Charsets.UTF_8)
                val json = JSONObject(jsonString)
                
                return generateV2RayConfig(
                    address = json.optString("add"),
                    port = json.optInt("port"),
                    uuid = json.optString("id"),
                    alterId = json.optInt("aid", 0),
                    net = json.optString("net", "tcp"),
                    type = json.optString("type", "none"),
                    path = json.optString("path", ""),
                    host = json.optString("host", "")
                )
            }
        } catch (e: Exception {
            e.printStackTrace()
        }
        return ""
    }

    private fun generateV2RayConfig(
        address: String,
        port: Int,
        uuid: String,
        alterId: Int,
        net: String,
        type: String,
        path: String,
        host: String
    ): String {
        val config = JSONObject()
        
        // Log setup
        val logObj = JSONObject()
        logObj.put("loglevel", "warning")
        config.put("log", logObj)

        // Outbound setup (V2Ray client connection)
        val outboundsArray = JSONArray()
        val outbound = JSONObject()
        outbound.put("protocol", "vmess")
        
        val settings = JSONObject()
        val vnextArray = JSONArray()
        val server = JSONObject()
        server.put("address", address)
        server.put("port", port)
        
        val usersArray = JSONArray()
        val user = JSONObject()
        user.put("id", uuid)
        user.put("alterId", alterId)
        user.put("security", "auto")
        usersArray.put(user)
        
        server.put("users", usersArray)
        vnextArray.put(server)
        settings.put("vnext", vnextArray)
        outbound.put("settings", settings)

        // Stream Settings (Transport layer configuration)
        val streamSettings = JSONObject()
        streamSettings.put("network", net)
        
        if (net == "ws") {
            val wsSettings = JSONObject()
            wsSettings.put("path", path)
            if (host.isNotEmpty()) {
                val headers = JSONObject()
                headers.put("Host", host)
                wsSettings.put("headers", headers)
            }
            streamSettings.put("wsSettings", wsSettings)
        }
        
        outbound.put("streamSettings", streamSettings)
        outboundsArray.put(outbound)
        
        // Freedom outbound for direct traffic
        val freedomOutbound = JSONObject()
        freedomOutbound.put("protocol", "freedom")
        freedomOutbound.put("tag", "direct")
        outboundsArray.put(freedomOutbound)

        config.put("outbounds", outboundsArray)

        // Inbound setup (Local SOCKS/HTTP proxy for VPN Service)
        val inboundsArray = JSONArray()
        val inbound = JSONObject()
        inbound.put("port", 10808)
        inbound.put("listen", "127.0.0.1")
        inbound.put("protocol", "socks")
        
        val inboundSettings = JSONObject()
        inboundSettings.put("udp", true)
        inbound.put("settings", inboundSettings)
        inboundsArray.put(inbound)
        
        config.put("inbounds", inboundsArray)

        return config.toString()
    }
                 }
                 
