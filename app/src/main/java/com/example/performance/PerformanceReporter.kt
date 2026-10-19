package com.example.performance

import android.util.Log
import android.view.Window
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.PerformanceMetricsState

/**
 * PerformanceReporter helper class to monitor UI frame metrics and jank.
 */
class PerformanceReporter(window: Window) {
    companion object {
        private const val TAG = "VantaPerformance"
    }

    private val frameListener = JankStats.OnFrameListener { frameData ->
        if (frameData.isJank) {
            Log.w(
                TAG,
                "Jank detected! $frameData"
            )
        }
    }

    private val jankStats = JankStats.createAndTrack(window, frameListener)

    fun setTrackingEnabled(enabled: Boolean) {
        jankStats.isTrackingEnabled = enabled
    }
}
