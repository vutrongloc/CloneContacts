package com.example.clonecontacts.Service

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log

class OutgoingCallListener(private val context: Context) : PhoneStateListener() {

    companion object {
        private const val TAG = "OutgoingCallListener"
    }

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    /**
     * Bắt đầu lắng nghe trạng thái cuộc gọi.
     */
    fun startListening() {
        // Lắng nghe các thay đổi trạng thái cuộc gọi
        telephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE)
        Log.d(TAG, "startListening: Đang bắt đầu lắng nghe trạng thái cuộc gọi")
    }

    /**
     * Dừng lắng nghe trạng thái cuộc gọi.
     */
    fun stopListening() {
        telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE)
        Log.d(TAG, "stopListening: Đã dừng lắng nghe trạng thái cuộc gọi")
    }

    /**
     * Hàm callback khi trạng thái cuộc gọi thay đổi.
     * state: Trạng thái cuộc gọi hiện tại (IDLE, OFFHOOK, RINGING)
     * incomingNumber: Số điện thoại đến (có thể null)
     */
    override fun onCallStateChanged(state: Int, incomingNumber: String?) {
        super.onCallStateChanged(state, incomingNumber)
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Khi cuộc gọi được bắt đầu (có thể là cuộc gọi đi hoặc cuộc gọi đến đã được trả lời)
                Log.d(TAG, "CALL_STATE_OFFHOOK: Cuộc gọi đang hoạt động. Số: $incomingNumber")
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                // Khi không có cuộc gọi
                Log.d(TAG, "CALL_STATE_IDLE: Không có cuộc gọi hoạt động")
            }
            TelephonyManager.CALL_STATE_RINGING -> {
                // Khi có cuộc gọi đến
                Log.d(TAG, "CALL_STATE_RINGING: Cuộc gọi đến từ số: $incomingNumber")
            }
        }
    }
}
