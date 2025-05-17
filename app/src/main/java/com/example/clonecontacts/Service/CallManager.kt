package com.example.clonecontacts.Service

import android.telecom.Call
import android.util.Log

object CallManager {
    var currentCall: Call? = null

    fun updateCall(call: Call?) {
        currentCall?.unregisterCallback(callCallback)
        currentCall = call
        currentCall?.registerCallback(callCallback)
    }
    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            Log.d("CallManager", "Call state changed: $state")
            when (state) {
                Call.STATE_RINGING -> Log.d("CallManager", "Incoming call ringing")
                Call.STATE_ACTIVE -> Log.d("CallManager", "Call active")
                Call.STATE_DISCONNECTED -> Log.d("CallManager", "Call disconnected")
            }
        }
    }
}