package com.example.clonecontacts.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.clonecontacts.ChucNang
import com.example.clonecontacts.R
import com.example.clonecontacts.Service.CallManager
import com.example.clonecontacts.Service.MyInCallService
import java.util.concurrent.TimeUnit

class IncomingCallActivity : AppCompatActivity() {
    private lateinit var callerNumber: TextView
    private lateinit var callDuration: TextView
    private lateinit var acceptButton: Button
    private lateinit var rejectButton: Button
    private lateinit var hangupButton: Button
    private lateinit var speakerButton: Button
    private lateinit var micButton: Button
    private lateinit var background: ImageView
    private lateinit var callerName: TextView
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

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_incoming_call)
        setupFullScreenMode()

        // Liên kết các thành phần giao diện
        callerNumber = findViewById(R.id.caller_number)
        callerName = findViewById(R.id.caller_name)
        callDuration = findViewById(R.id.call_duration)
        acceptButton = findViewById(R.id.btn_accept)
        rejectButton = findViewById(R.id.btn_reject)
        hangupButton = findViewById(R.id.btn_hangup)
        speakerButton = findViewById(R.id.speaker_incoming_call)
        micButton = findViewById(R.id.mic_incoming_call)
        background = findViewById(R.id.background_incoming_call)

        val sharedPref = getSharedPreferences("ColorPicker", Context.MODE_PRIVATE)
        val color = sharedPref.getInt("selected_color", Color.rgb(255, 165, 0))
        background.setBackgroundColor(color)

        val phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: "Unknown"
        val callState = intent.getIntExtra("CALL_STATE", Call.STATE_DISCONNECTED)
        callerName.setText(ChucNang().getContactNameFromNumber(this, phoneNumber) ?: "Unknown")
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
        if (!checkAndRequestPermissions()) {
            finish()
            return
        }
        speakerButton.setOnClickListener {
            if (MyInCallService.instance?.toggleSpeakerphone() == true) {
                speakerButton.text = "Tắt loa"
                speakerButton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.volume_off,
                    0,
                    0,
                    0
                )
            } else {
                speakerButton.text = "Bật loa"
                speakerButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.volume_up, 0, 0, 0)
            }
        }
        micButton.setOnClickListener {
            if (MyInCallService.instance?.toggleMute() == true) {
                micButton.text = "Bật mic"
                micButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.mic, 0, 0, 0)
            } else {
                micButton.text = "Tắt mic"
                micButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.mic_off, 0, 0, 0)
            }
        }
    }

    private fun updateUI(state: Int) {
        when (state) {
            Call.STATE_RINGING -> {
                acceptButton.visibility = View.VISIBLE
                rejectButton.visibility = View.VISIBLE
                hangupButton.visibility = View.GONE
                callDuration.visibility = View.GONE
                speakerButton.visibility = View.GONE
                micButton.visibility = View.GONE
            }

            Call.STATE_ACTIVE -> {
                acceptButton.visibility = View.GONE
                rejectButton.visibility = View.GONE
                hangupButton.visibility = View.VISIBLE
                callDuration.visibility = View.VISIBLE
                speakerButton.visibility = View.VISIBLE
                micButton.visibility = View.VISIBLE
            }

            Call.STATE_DISCONNECTED -> {
                acceptButton.visibility = View.GONE
                rejectButton.visibility = View.GONE
                hangupButton.visibility = View.GONE
                callDuration.visibility = View.GONE
                speakerButton.visibility = View.GONE
                micButton.visibility = View.GONE
                finish()
            }
        }
    }

    private fun setupFullScreenMode() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.READ_PHONE_STATE
        )
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        return if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                1
            )
            false
        } else true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Permissions required for call functionality!",
                    Toast.LENGTH_SHORT
                ).show()
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