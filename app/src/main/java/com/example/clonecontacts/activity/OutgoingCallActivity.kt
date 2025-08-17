package com.example.clonecontacts.activity
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.clonecontacts.R
import com.example.clonecontacts.Service.CallManager
import com.example.clonecontacts.Service.MyInCallService
import java.util.concurrent.TimeUnit
class OutgoingCallActivity : AppCompatActivity() {

    private lateinit var calleeName: TextView
    private lateinit var calleeNumber: TextView
    private lateinit var callStatus: TextView
    private lateinit var endCallButton: Button
    private lateinit var speakerButton: Button
    private lateinit var muteButton: Button
    private lateinit var audioManager: AudioManager

    private var callStartTime: Long = 0
    private var isCallActive = false
    private val handler = Handler(Looper.getMainLooper())

    // Runnable cập nhật thời gian cuộc gọi (nếu cuộc gọi đã được kết nối)
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

    // BroadcastReceiver nhận trạng thái cuộc gọi từ MyInCallService
    private val callStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val state = it.getIntExtra(MyInCallService.EXTRA_CALL_STATE, -1)
                Log.d(TAG, "Received call state from MyInCallService: $state")
                updateUI(state)

                if (state == Call.STATE_DISCONNECTED) {
                    Log.d(TAG, "Remote party ended the call.")
                    finish()  // Đóng màn hình khi cuộc gọi kết thúc
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outgoing_call)

        // Khởi tạo giao diện
        calleeName = findViewById(R.id.callee_name)
        calleeNumber = findViewById(R.id.callee_number)
        callStatus = findViewById(R.id.call_duration)
        endCallButton = findViewById(R.id.end_call_button)
        speakerButton = findViewById(R.id.speaker_button)
        muteButton = findViewById(R.id.mute_button)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Lấy thông tin từ Intent
        val number = intent.getStringExtra("callee_number") ?: ""
        val name = intent.getStringExtra("callee_name")
        calleeNumber.text = number
        calleeName.text = name ?: "Unknown"

        // Kiểm tra quyền
        if (!checkAndRequestPermissions()) {
            finish()
            return
        }

        // Thiết lập sự kiện nút
        endCallButton.setOnClickListener { endCall() }
        speakerButton.setOnClickListener { toggleSpeaker() }
        muteButton.setOnClickListener { toggleMute() }

        // Đăng ký BroadcastReceiver để nhận trạng thái cuộc gọi từ MyInCallService
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(callStateReceiver, IntentFilter(MyInCallService.ACTION_CALL_STATE_CHANGED))
    }
    // Cập nhật giao diện dựa trên trạng thái cuộc gọi
    private fun updateUI(state: Int) {
        Log.d(TAG, "Updating UI with state: $state")
        when (state) {
            Call.STATE_DIALING -> {
                callStatus.text = "Dialing..."
                speakerButton.isEnabled = false
                muteButton.isEnabled = false
            }
            Call.STATE_ACTIVE -> {
                val call = CallManager.currentCall
                if (call != null && call.state == Call.STATE_ACTIVE && !isCallActive) {
                    callStartTime = System.currentTimeMillis()
                    isCallActive = true
                    handler.post(updateTimerRunnable)

                    // Tự bật loa khi vừa kết nối
                    enableSpeaker()
                }
                callStatus.text = "Call Connected"
                speakerButton.isEnabled = true
                muteButton.isEnabled = true
            }

            Call.STATE_DISCONNECTED -> {
                Log.d(TAG, "Call ended by remote party or system.")
                isCallActive = false
                handler.removeCallbacks(updateTimerRunnable)
                callStatus.text = "Call Ended"
                finish()  // Đóng Activity khi cuộc gọi kết thúc
            }
            else -> callStatus.text = "Connecting..."
        }
    }

    // Kiểm tra và yêu cầu các quyền cần thiết
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
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions required for call functionality!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // Phương thức kết thúc cuộc gọi
    private fun endCall() {
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            telecomManager.endCall()
            updateUI(Call.STATE_DISCONNECTED)
            finish()
        }
    }

    // Bật loa ngoài
    private fun enableSpeaker() {
        if (!isCallActive) return
        audioManager.mode = AudioManager.MODE_IN_CALL   // hoặc MODE_IN_COMMUNICATION tùy thiết bị
        audioManager.isSpeakerphoneOn = true
        speakerButton.text = "Tắt loa"
    }

    // Tắt loa ngoài
    private fun disableSpeaker() {
        if (!isCallActive) return
        audioManager.mode = AudioManager.MODE_IN_CALL
        audioManager.isSpeakerphoneOn = false
        speakerButton.text = "Mở loa"
    }

    private fun toggleSpeaker() {
        if (!isCallActive) return
        if (audioManager.isSpeakerphoneOn) {
            disableSpeaker()
        } else {
            enableSpeaker()
        }
    }




    // Bật/tắt micro (tắt tiếng)
    private fun toggleMute() {
        if (!isCallActive) return
        val isMuted = audioManager.isMicrophoneMute
        audioManager.isMicrophoneMute = !isMuted
        muteButton.text = if (audioManager.isMicrophoneMute) "Bật âm" else "Tắt âm"
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
        LocalBroadcastManager.getInstance(this).unregisterReceiver(callStateReceiver)
        handler.removeCallbacks(updateTimerRunnable)
    }

    companion object {
        private const val TAG = "OutgoingCallActivity"
        private const val PERMISSION_REQUEST_CODE = 1
    }

}
