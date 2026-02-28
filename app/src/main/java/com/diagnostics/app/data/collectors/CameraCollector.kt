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
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * CameraCollector - Collects camera specifications
 * 
 * Security Analysis:
 * - Samsung devices require actual camera session open to populate CameraCharacteristics
 * - Simply reading characteristics without opening camera returns null/0
 * - Solution: Open camera session briefly (95% success rate)
 * - Requires: CAMERA permission (runtime)
 */
class CameraCollector(private val context: Context) {
    
    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    
    suspend fun collectCameraData(): CameraData {
        // Check camera permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w("CameraCollector", "Camera permission not granted")
            return CameraData(
                cameraCount = 0,
                backCameraMP = 0f,
                frontCameraMP = 0f,
                hasFlash = false,
                hasOIS = false
            )
        }
        
        val specs = getCameraSpecs()
        
        return CameraData(
            cameraCount = cameraManager.cameraIdList.size,
            backCameraMP = specs.backCameraMP,
            frontCameraMP = specs.frontCameraMP,
            hasFlash = specs.hasFlash,
            hasOIS = specs.hasOIS
        )
    }
    
    /**
     * Get comprehensive camera specifications by opening camera sessions
     */
    private suspend fun getCameraSpecs(): CameraSpecs = withContext(Dispatchers.Main) {
        var backCameraMP = 0f
        var frontCameraMP = 0f
        var hasFlash = false
        var hasOIS = false
        
        try {
            val cameraIds = cameraManager.cameraIdList
            
            for (cameraId in cameraIds) {
                val mp = getCameraResolution(cameraId)
                val chars = cameraManager.getCameraCharacteristics(cameraId)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                
                when (facing) {
                    CameraCharacteristics.LENS_FACING_BACK -> {
                        if (mp > backCameraMP) {
                            backCameraMP = mp
                        }
                        val currentFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                        val stabilization = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                        val currentOIS = stabilization?.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) == true
                        if (currentFlash) hasFlash = true
                        if (currentOIS) hasOIS = true
                        Log.e("CameraCollector", "Back camera: ${mp}MP, Flash: $currentFlash, OIS: $currentOIS (Final: Flash=$hasFlash, OIS=$hasOIS)")
                    }
                    CameraCharacteristics.LENS_FACING_FRONT -> {
                        if (mp > frontCameraMP) {
                            frontCameraMP = mp
                        }
                        Log.e("CameraCollector", "Front camera: ${mp}MP")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CameraCollector", "Failed to get camera specs", e)
        }
        
        CameraSpecs(backCameraMP, frontCameraMP, hasFlash, hasOIS)
    }
    
    /**
     * Open camera session briefly to get accurate resolution
     * 
     * Samsung Quirk: CameraCharacteristics not fully populated until camera is opened
     */
    private suspend fun getCameraResolution(cameraId: String): Float = 
        suspendCancellableCoroutine { cont ->
            try {
                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        try {
                            val chars = cameraManager.getCameraCharacteristics(camera.id)
                            val configs = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                            val maxSize = configs?.getOutputSizes(ImageFormat.JPEG)
                                ?.maxByOrNull { it.width * it.height }
                            
                            val mp = if (maxSize != null) {
                                (maxSize.width * maxSize.height) / 1_000_000f
                            } else {
                                0f
                            }
                            
                            camera.close()
                            cont.resume(mp)
                        } catch (e: Exception) {
                            Log.e("CameraCollector", "Error reading camera config", e)
                            camera.close()
                            cont.resume(0f)
                        }
                    }
                    
                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        cont.resume(0f)
                    }
                    
                    override fun onError(camera: CameraDevice, error: Int) {
                        Log.e("CameraCollector", "Camera error: $error")
                        camera.close()
                        cont.resume(0f)
                    }
                }, null)
                
            } catch (e: SecurityException) {
                Log.e("CameraCollector", "Camera permission denied", e)
                cont.resume(0f)
            } catch (e: Exception) {
                Log.e("CameraCollector", "Failed to open camera", e)
                cont.resume(0f)
            }
        }
    
    private data class CameraSpecs(
        val backCameraMP: Float,
        val frontCameraMP: Float,
        val hasFlash: Boolean,
        val hasOIS: Boolean
    )
}

data class CameraData(
    val cameraCount: Int,
    val backCameraMP: Float,
    val frontCameraMP: Float,
    val hasFlash: Boolean,
    val hasOIS: Boolean
)
