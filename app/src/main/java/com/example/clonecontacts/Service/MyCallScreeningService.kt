package com.example.clonecontacts.Service

import android.Manifest
import com.example.clonecontacts.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.clonecontacts.activity.IncomingCallActivity

class MyCallScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "MyCallScreeningService"
        private const val CHANNEL_ID = "incoming_call_channel"
        private const val NOTIFICATION_ID = 1
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onScreenCall(callDetails: Call.Details) {
        // Lấy số điện thoại của cuộc gọi đến
        val incomingNumber = callDetails.handle.schemeSpecificPart
        Log.d(TAG, "onScreenCall: Cuộc gọi đến từ số: $incomingNumber")

        // Hiển thị thông báo trên thanh thông báo
        showIncomingCallNotification(incomingNumber)

        // Xây dựng phản hồi để cho phép cuộc gọi đi tiếp
        val response = CallResponse.Builder()
            .setDisallowCall(false)    // Cho phép cuộc gọi
            .setSkipCallLog(false)     // Ghi nhận log cuộc gọi
            .setSkipNotification(false) // Không ẩn thông báo hệ thống của cuộc gọi đến
            .build()

        // Gửi phản hồi về cho hệ thống
        respondToCall(callDetails, response)
        Log.d(TAG, "onScreenCall: Đã xử lý cuộc gọi đến và gửi phản hồi")
    }

    /**
     * Tạo và hiển thị notification cho cuộc gọi đến.
     */
    private fun showIncomingCallNotification(incomingNumber: String) {
        // Tạo notification channel nếu thiết bị chạy Android O trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Incoming Call Notifications"
            val channelDescription = "Thông báo hiển thị khi có cuộc gọi đến"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
                description = channelDescription
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Tạo Intent để mở IncomingCallActivity
        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            putExtra("PHONE_NUMBER", incomingNumber)
            putExtra("CALL_STATE", Call.STATE_RINGING) // Trạng thái cuộc gọi đang đổ chuông
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        // Tạo PendingIntent để gắn vào thông báo
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        // Xây dựng notification với PendingIntent
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.star)
            .setContentTitle("Cuộc gọi đến")
            .setContentText("Số: $incomingNumber")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // Gắn PendingIntent để mở Activity khi nhấp

        // Hiển thị notification
        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@MyCallScreeningService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Không có quyền hiển thị thông báo!")
                return
            }
            notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }
}