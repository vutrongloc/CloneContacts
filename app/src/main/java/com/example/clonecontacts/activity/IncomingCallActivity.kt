package com.example.clonecontacts.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.clonecontacts.R
import com.example.clonecontacts.Service.CallManager
import java.util.concurrent.TimeUnit

class IncomingCallActivity : AppCompatActivity() {
    private lateinit var callerNumber: TextView
    private lateinit var callDuration: TextView
    private lateinit var acceptButton: Button
    private lateinit var rejectButton: Button
    private lateinit var hangupButton: Button

    private var callStartTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isCallActive = false

    private val updateTimerRunnable = object : Runnable {
        override fun run() {
            if (isCallActive) {
                val durationMillis = System.currentTimeMillis() - callStartTime
                val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
                callDuration.text = String.format("%02d:%02d", minutes, seconds)
                handler.postDelayed(this, 1000) // Cập nhật mỗi giây
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        // Liên kết các thành phần giao diện
        callerNumber = findViewById(R.id.caller_number)
        callDuration = findViewById(R.id.call_duration)
        acceptButton = findViewById(R.id.btn_accept)
        rejectButton = findViewById(R.id.btn_reject)
        hangupButton = findViewById(R.id.btn_hangup)

        val phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: "Unknown"
        val callState = intent.getIntExtra("CALL_STATE", Call.STATE_DISCONNECTED)

        callerNumber.text = phoneNumber
        updateUI(callState)

        val call = CallManager.currentCall
        call?.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                updateUI(state)
                when (state) {
                    Call.STATE_RINGING -> {
                        callDuration.visibility = View.GONE
                    }
                    Call.STATE_ACTIVE -> {
                        callStartTime = System.currentTimeMillis()
                        isCallActive = true
                        callDuration.visibility = View.VISIBLE
                        handler.removeCallbacks(updateTimerRunnable) // Xóa timer cũ nếu có
                        handler.post(updateTimerRunnable) // Bắt đầu đếm thời gian
                    }
                    Call.STATE_DISCONNECTED -> {
                        isCallActive = false
                        handler.removeCallbacks(updateTimerRunnable)
                        finish()
                    }
                }
            }
        }) ?: run {
            // Nếu không có cuộc gọi, thoát Activity
            finish()
        }

        // Thiết lập sự kiện cho các nút
        acceptButton.setOnClickListener {
            call?.answer(0)
        }
        rejectButton.setOnClickListener {
            call?.reject(false, "")
            finish()
        }
        hangupButton.setOnClickListener {
            call?.disconnect()
            finish()
        }
    }

    private fun updateUI(state: Int) {
        when (state) {
            Call.STATE_RINGING -> {
                acceptButton.visibility = View.VISIBLE
                rejectButton.visibility = View.VISIBLE
                hangupButton.visibility = View.GONE
                callDuration.visibility = View.GONE
            }
            Call.STATE_ACTIVE -> {
                acceptButton.visibility = View.GONE
                rejectButton.visibility = View.GONE
                hangupButton.visibility = View.VISIBLE
                callDuration.visibility = View.VISIBLE
            }
            Call.STATE_DISCONNECTED -> {
                acceptButton.visibility = View.GONE
                rejectButton.visibility = View.GONE
                hangupButton.visibility = View.GONE
                callDuration.visibility = View.GONE
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hủy callback và timer khi Activity bị hủy
        CallManager.currentCall?.unregisterCallback(object : Call.Callback() {})
        handler.removeCallbacks(updateTimerRunnable)
    }
}