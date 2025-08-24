package com.example.clonecontacts.activity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.TelecomManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.clonecontacts.R
import com.example.clonecontacts.Service.MyInCallService
import java.util.concurrent.TimeUnit

class OutgoingCallActivity : AppCompatActivity() {

    private lateinit var calleeName: TextView
    private lateinit var calleeNumber: TextView
    private lateinit var callStatus: TextView
    private lateinit var endCallButton: Button
    private lateinit var speakerButton: Button
    private lateinit var muteButton: Button
    private lateinit var background: ImageView
    private var callStartTime: Long = 0
    private var isCallActive = false
    private val handler = Handler(Looper.getMainLooper())

    private val updateTimerRunnable = object : Runnable {
        override fun run() {
            if (isCallActive) {
                val durationMillis = System.currentTimeMillis() - callStartTime
                val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
                callStatus.text = String.format("%02d:%02d", minutes, seconds)
                handler.postDelayed(this, 1000)
            }
        }
    }

    private val callStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val state = it.getIntExtra(MyInCallService.EXTRA_CALL_STATE, -1)
                Log.d(TAG, "Received call state: $state")
                updateUI(state)

                if (state == Call.STATE_DISCONNECTED) {
                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_outgoing_call)
        setupFullScreenMode()
        calleeName = findViewById(R.id.callee_name)
        calleeNumber = findViewById(R.id.callee_number)
        callStatus = findViewById(R.id.call_duration)
        endCallButton = findViewById(R.id.end_call_button)
        speakerButton = findViewById(R.id.speaker_button)
        muteButton = findViewById(R.id.mute_button)
        background = findViewById(R.id.background_outgoing_call)

        val sharedPref = getSharedPreferences("ColorPicker", Context.MODE_PRIVATE)
        val color = sharedPref.getInt("selected_color", Color.rgb(255, 165, 0))
        background.setBackgroundColor(color)

        val number = intent.getStringExtra("callee_number") ?: ""
        val name = intent.getStringExtra("callee_name")
        calleeNumber.text = number
        calleeName.text = name ?: "Số không xác định"

        if (!checkAndRequestPermissions()) {
            finish()
            return
        }

        endCallButton.setOnClickListener { endCall() }
        speakerButton.setOnClickListener {
            if (MyInCallService.instance?.toggleSpeakerphone() == true) {
                speakerButton.text = "Tắt loa"
                speakerButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.volume_off, 0, 0, 0)
            } else {
                speakerButton.text = "Bật loa"
                speakerButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.volume_up, 0, 0, 0)
            }
        }
        muteButton.setOnClickListener {
            if (MyInCallService.instance?.toggleMute() == true) {
                muteButton.text = "Bật mic"
                speakerButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.mic, 0, 0, 0)
            } else {
                muteButton.text = "Tắt mic"
                speakerButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.mic_off, 0, 0, 0)
            }
        }

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                callStateReceiver,
                IntentFilter(MyInCallService.ACTION_CALL_STATE_CHANGED)
            )
    }

    private fun setupFullScreenMode() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }

    private fun updateUI(state: Int) {
        when (state) {
            Call.STATE_DIALING -> {
                callStatus.text = "Đang gọi..."
                speakerButton.isEnabled = false
                muteButton.isEnabled = false
            }

            Call.STATE_ACTIVE -> {
                if (!isCallActive) {
                    callStartTime = System.currentTimeMillis()
                    isCallActive = true
                    handler.post(updateTimerRunnable)
                }
                callStatus.text = "Đã kết nối cuộc gọi"
                speakerButton.isEnabled = true
                muteButton.isEnabled = true
            }

            Call.STATE_DISCONNECTED -> {
                isCallActive = false
                handler.removeCallbacks(updateTimerRunnable)
                callStatus.text = "Cuộc gọi đã kết thúc"
                finish()
            }

            else -> callStatus.text = "Đang kết nối..."
        }
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
                PERMISSION_REQUEST_CODE
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
        if (requestCode == PERMISSION_REQUEST_CODE) {
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

    private fun endCall() {
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            telecomManager.endCall()
            updateUI(Call.STATE_DISCONNECTED)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(callStateReceiver)
        handler.removeCallbacks(updateTimerRunnable)
    }

    companion object {
        private const val TAG = "OutgoingCallActivity"
        private const val PERMISSION_REQUEST_CODE = 1
    }
}
