package com.v2ray.vpn.service

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object V2RayManager {

    init {
        // Load native lib if using libv2ray.so
        try {
            System.loadLibrary("v2ray")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }

    external fun runV2Ray(configPath: String, tunFd: Int): Long
    external fun stopV2Ray(handle: Long): Int

    private var nativeHandle: Long = 0

    fun startV2Ray(context: Context) {
        try {
            val configDir = context.filesDir.absolutePath
            val configFile = File(configDir, "config.json")
            
            // Ensure config exists before running
            if (!configFile.exists()) return

            // Native execution call can be integrated here based on your .so library setup
            // nativeHandle = runV2Ray(configFile.absolutePath, -1)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopV2Ray() {
        try {
            if (nativeHandle != 0L) {
                stopV2Ray(nativeHandle)
                nativeHandle = 0L
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
