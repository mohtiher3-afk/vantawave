package com.example.performance

import android.app.Activity
import android.util.Log
import androidx.metrics.performance.JankStats

/**
 * PerformanceReporter helper class to monitor UI frame metrics and jank
 * using the androidx.metrics.performance library and log them to Logcat in real-time.
 */
class PerformanceReporter(private val activity: Activity) {
    companion object {
        private const val TAG = "VantaPerformance"
    }

    private val frameListener = JankStats.OnFrameListener { frameData ->
        if (frameData.isJank) {
            Log.w(
                TAG,
                "Jank detected! Frame Duration UI: ${frameData.frameDurationUiNanos}ns, " +
                "States: ${frameData.states}"
            )
        } else {
            Log.d(
                TAG,
                "Frame normal: Frame Duration UI: ${frameData.frameDurationUiNanos}ns"
            )
        }
    }

    private val jankStats: JankStats = JankStats.create(activity.window, frameListener)

    init {
        jankStats.isTrackingEnabled = true
        Log.i(TAG, "PerformanceReporter initialized for ${activity.javaClass.simpleName} using androidx.metrics.performance")
    }

    fun addState(key: String, value: String) {
        try {
            jankStats.stateHolder.addState(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add performance state: $key=$value", e)
        }
    }

    fun removeState(key: String) {
        try {
            jankStats.stateHolder.removeState(key)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove performance state: $key", e)
        }
    }

    fun setTrackingEnabled(enabled: Boolean) {
        jankStats.isTrackingEnabled = enabled
    }
}
