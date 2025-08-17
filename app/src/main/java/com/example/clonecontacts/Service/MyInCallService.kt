package com.example.clonecontacts.Service

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.clonecontacts.activity.IncomingCallActivity

class MyInCallService : InCallService() {
    companion object {
        private const val TAG = "MyInCallService"
        const val ACTION_CALL_STATE_CHANGED = "com.example.clonecontacts.CALL_STATE_CHANGED"
        const val EXTRA_PHONE_NUMBER = "PHONE_NUMBER"
        const val EXTRA_CALL_STATE = "CALL_STATE"
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "Call added: ${call.details.handle.schemeSpecificPart}, state: ${call.state}")
        CallManager.updateCall(call)
        call.registerCallback(callCallback)
        sendCallStateBroadcast(call, call.state)
        if (call.state == Call.STATE_RINGING) {
            showCallUI(call)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed: ${call.details.handle.schemeSpecificPart}")
        call.unregisterCallback(callCallback)
        CallManager.updateCall(null)
        sendCallStateBroadcast(call, Call.STATE_DISCONNECTED)
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            Log.d(TAG, "Call state changed: $state")
            sendCallStateBroadcast(call, state)

            when (state) {
                Call.STATE_RINGING -> showCallUI(call)
                Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                    Log.d(TAG, "Call ended by remote party or system.")
                    CallManager.updateCall(null)
                    sendCallStateBroadcast(call, Call.STATE_DISCONNECTED)
                }
            }
        }
    }




    private fun sendCallStateBroadcast(call: Call, state: Int) {
        val intent = Intent(ACTION_CALL_STATE_CHANGED).apply {
            putExtra(EXTRA_PHONE_NUMBER, call.details.handle.schemeSpecificPart)
            putExtra(EXTRA_CALL_STATE, state)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun showCallUI(call: Call) {
        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            putExtra(EXTRA_PHONE_NUMBER, call.details.handle.schemeSpecificPart)
            putExtra(EXTRA_CALL_STATE, call.state)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }


}