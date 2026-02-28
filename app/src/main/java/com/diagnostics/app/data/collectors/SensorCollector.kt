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

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log

/**
 * SensorCollector - Collects device sensor information
 * 
 * Enumerates all available sensors on the device and categorizes them.
 * No special permissions required - SensorManager is freely accessible.
 */
class SensorCollector(private val context: Context) {
    
    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    
    fun collectSensorData(): SensorData {
        return try {
            val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
            
            val sensorTypes = allSensors.map { sensor ->
                getSensorTypeName(sensor.type)
            }.distinct() // Remove duplicates (some devices have multiple sensors of same type)
            
            Log.d("SensorCollector", "Found ${allSensors.size} sensors (${sensorTypes.size} unique types)")
            
            SensorData(
                sensorCountTotal = allSensors.size,
                sensorTypesList = sensorTypes
            )
            
        } catch (e: Exception) {
            Log.e("SensorCollector", "Failed to enumerate sensors", e)
            SensorData(
                sensorCountTotal = 0,
                sensorTypesList = emptyList()
            )
        }
    }
    
    /**
     * Convert sensor type constant to human-readable name
     */
    private fun getSensorTypeName(type: Int): String {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER -> "ACCELEROMETER"
            Sensor.TYPE_MAGNETIC_FIELD -> "MAGNETIC_FIELD"
            Sensor.TYPE_ORIENTATION -> "ORIENTATION"
            Sensor.TYPE_GYROSCOPE -> "GYROSCOPE"
            Sensor.TYPE_LIGHT -> "LIGHT"
            Sensor.TYPE_PRESSURE -> "PRESSURE"
            Sensor.TYPE_TEMPERATURE -> "TEMPERATURE"
            Sensor.TYPE_PROXIMITY -> "PROXIMITY"
            Sensor.TYPE_GRAVITY -> "GRAVITY"
            Sensor.TYPE_LINEAR_ACCELERATION -> "LINEAR_ACCELERATION"
            Sensor.TYPE_ROTATION_VECTOR -> "ROTATION_VECTOR"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "RELATIVE_HUMIDITY"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "AMBIENT_TEMPERATURE"
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "MAGNETIC_FIELD_UNCALIBRATED"
            Sensor.TYPE_GAME_ROTATION_VECTOR -> "GAME_ROTATION_VECTOR"
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> "GYROSCOPE_UNCALIBRATED"
            Sensor.TYPE_SIGNIFICANT_MOTION -> "SIGNIFICANT_MOTION"
            Sensor.TYPE_STEP_DETECTOR -> "STEP_DETECTOR"
            Sensor.TYPE_STEP_COUNTER -> "STEP_COUNTER"
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "GEOMAGNETIC_ROTATION_VECTOR"
            Sensor.TYPE_HEART_RATE -> "HEART_RATE"
            Sensor.TYPE_POSE_6DOF -> "POSE_6DOF"
            Sensor.TYPE_STATIONARY_DETECT -> "STATIONARY_DETECT"
            Sensor.TYPE_MOTION_DETECT -> "MOTION_DETECT"
            Sensor.TYPE_HEART_BEAT -> "HEART_BEAT"
            Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT -> "LOW_LATENCY_OFFBODY_DETECT"
            Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> "ACCELEROMETER_UNCALIBRATED"
            else -> "UNKNOWN_TYPE_$type"
        }
    }
}

data class SensorData(
    val sensorCountTotal: Int,
    val sensorTypesList: List<String>
)
