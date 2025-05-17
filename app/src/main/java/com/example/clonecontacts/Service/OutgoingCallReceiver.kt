package com.example.clonecontacts.Service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class OutgoingCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "OutgoingCallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val outgoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
        Log.d(TAG, "onReceive: Outgoing call number: $outgoingNumber")
    }
}