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
import android.opengl.GLES20
import android.util.Log
import android.view.WindowManager
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay

/**
 * Collects GPU renderer string and screen refresh rate.
 * Uses an off-screen EGL surface to query GL_RENDERER without needing a View.
 */
object DisplayInfoCollector {

    private const val TAG = "DisplayInfoCollector"

    /**
     * Get the GPU renderer string (e.g. "Mali-G78 MP14", "Adreno (TM) 660").
     * Creates a temporary off-screen EGL context to query OpenGL.
     */
    fun getGPURenderer(context: Context): String {
        return try {
            val egl = EGLContext.getEGL() as EGL10
            val display: EGLDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

            val version = IntArray(2)
            egl.eglInitialize(display, version)

            val configAttribs = intArrayOf(
                EGL10.EGL_RENDERABLE_TYPE, 4,  // EGL_OPENGL_ES2_BIT
                EGL10.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            egl.eglChooseConfig(display, configAttribs, configs, 1, numConfigs)

            if (numConfigs[0] == 0 || configs[0] == null) {
                Log.w(TAG, "No EGL config found")
                return "unknown"
            }

            val contextAttribs = intArrayOf(
                0x3098, 2,  // EGL_CONTEXT_CLIENT_VERSION = 2
                EGL10.EGL_NONE
            )
            val eglContext = egl.eglCreateContext(
                display, configs[0], EGL10.EGL_NO_CONTEXT, contextAttribs
            )

            val surfaceAttribs = intArrayOf(
                EGL10.EGL_WIDTH, 1,
                EGL10.EGL_HEIGHT, 1,
                EGL10.EGL_NONE
            )
            val eglSurface = egl.eglCreatePbufferSurface(display, configs[0], surfaceAttribs)

            egl.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)

            val renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "unknown"

            // Cleanup
            egl.eglMakeCurrent(
                display,
                EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT
            )
            egl.eglDestroySurface(display, eglSurface)
            egl.eglDestroyContext(display, eglContext)
            egl.eglTerminate(display)

            Log.d(TAG, "GPU renderer: $renderer")
            renderer
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get GPU renderer: ${e.message}")
            "unknown"
        }
    }

    /**
     * Get the current screen refresh rate in Hz.
     */
    @Suppress("DEPRECATION")
    fun getRefreshRate(context: Context): Float {
        return try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val rate = wm.defaultDisplay.refreshRate
            Log.d(TAG, "Screen refresh rate: ${rate}Hz")
            rate
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get refresh rate: ${e.message}")
            60f
        }
    }
}
