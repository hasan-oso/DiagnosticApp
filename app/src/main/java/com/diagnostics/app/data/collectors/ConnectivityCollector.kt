/*
 * Copyright (c) 2025 Hasan Oso. All Rights Reserved.
 * 
 * PROPRIETARY SOFTWARE - DO NOT DISTRIBUTE
 * 
 * This file is part of DiagnosticApp.
 * Unauthorized use is strictly prohibited.
 * 
 * Owner: Hasan Oso
 * Student ID: 230212925
 * Institution: OST?M Technical University
 */
package com.diagnostics.app.data.collectors

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager

class ConnectivityCollector(private val context: Context) {
    
    fun collectConnectivityData(): ConnectivityData {
        return ConnectivityData(
            wifiAvailable = isWifiAvailable(),
            bluetoothVersion = getBluetoothVersion(),
            nfcAvailable = isNfcAvailable(),
            mobileNetworkType = getMobileNetworkType(),
            dualSimSupport = isDualSimSupported(),
            esimSupport = isEsimSupported(),
            usbVersion = getUsbVersion()
        )
    }
    
    private fun isWifiAvailable(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)
    }
    
    private fun getBluetoothVersion(): String {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter
            
            if (bluetoothAdapter == null) {
                "Not Available"
            } else {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> "5.2+" // Android 12+
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> "5.0+" // Android 10+
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> "5.0"  // Android 8+
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> "4.2+" // Android 6+
                    else -> "4.0+"
                }
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun isNfcAvailable(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
    }
    
    private fun getMobileNetworkType(): String {
        return try {
            if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    when (telephonyManager.dataNetworkType) {
                        TelephonyManager.NETWORK_TYPE_NR -> "5G"
                        TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                        TelephonyManager.NETWORK_TYPE_HSPAP,
                        TelephonyManager.NETWORK_TYPE_HSPA,
                        TelephonyManager.NETWORK_TYPE_HSUPA,
                        TelephonyManager.NETWORK_TYPE_HSDPA,
                        TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                        TelephonyManager.NETWORK_TYPE_EDGE,
                        TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
                        else -> "Unknown"
                    }
                } else {
                    "Unknown"
                }
            } else {
                "Permission Denied"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun isDualSimSupported(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                telephonyManager.phoneCount > 1
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isEsimSupported(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_EUICC)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    
    private fun getUsbVersion(): String {
        return try {
            val pm = context.packageManager
            when {
                // USB 3.1+ (Android 12+ with host support)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST) -> "3.1"
                
                // USB 2.0 (host support available)
                pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST) -> "2.0"
                
                // USB accessory only
                pm.hasSystemFeature(PackageManager.FEATURE_USB_ACCESSORY) -> "2.0 (Accessory Only)"
                
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    data class ConnectivityData(
        val wifiAvailable: Boolean,
        val bluetoothVersion: String,
        val nfcAvailable: Boolean,
        val mobileNetworkType: String,
        val dualSimSupport: Boolean,
        val esimSupport: Boolean,
        val usbVersion: String
    )
}
