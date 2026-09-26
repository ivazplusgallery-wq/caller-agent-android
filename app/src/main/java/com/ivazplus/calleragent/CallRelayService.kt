package com.ivazplus.calleragent

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Always-on foreground service - the Android equivalent of RelayClient in
 * desktop_client.py. It listens for the phone's call state and, as soon as
 * the phone starts RINGING, POSTs the incoming number to the server's
 * /call endpoint (same endpoint the Android app was always meant to call,
 * see the "کارهای باقی‌مونده" notes).
 *
 * Getting the actual number in onCallStateChanged requires both
 * READ_PHONE_STATE and READ_CALL_LOG permissions on Android 9+ - both are
 * requested by MainActivity before this service is started.
 */
class CallRelayService : Service() {

    private val executor = Executors.newSingleThreadExecutor()
    private var telephonyManager: TelephonyManager? = null
    private var listener: PhoneStateListener? = null
    private var lastNotifiedNumber: String? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY: if Android kills the process under memory pressure,
        // it will try to recreate the service (onCreate runs again) once
        // resources free up.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopListening()
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun startForegroundWithNotification() {
        val channelId = "caller_agent_channel"
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            channelId, "ایجنت تماس‌گیرنده", NotificationManager.IMPORTANCE_LOW
        )
        nm.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ایجنت تماس‌گیرنده")
            .setContentText("در حال گوش دادن به تماس‌های ورودی")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }
    }

    @Suppress("DEPRECATION")
    private fun startListening() {
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        listener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        if (!phoneNumber.isNullOrBlank() && phoneNumber != lastNotifiedNumber) {
                            lastNotifiedNumber = phoneNumber
                            sendCallEvent(phoneNumber)
                        }
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        // Call ended (or was never answered) - let the same
                        // number trigger a fresh lookup next time it calls.
                        lastNotifiedNumber = null
                    }
                }
            }
        }
        telephonyManager?.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    @Suppress("DEPRECATION")
    private fun stopListening() {
        telephonyManager?.listen(listener, PhoneStateListener.LISTEN_NONE)
    }

    private fun sendCallEvent(number: String) {
        executor.submit {
            try {
                val prefs = Prefs.get(this)
                val host = prefs.getString("host", "") ?: ""
                val port = prefs.getInt("port", 8000)
                val secure = prefs.getBoolean("secure", false)
                val apiKey = prefs.getString("api_key", "") ?: ""
                if (host.isEmpty() || apiKey.isEmpty()) return@submit

                val scheme = if (secure) "https" else "http"
                val url = URL("$scheme://$host:$port/call")
                val conn = url.openConnection() as HttpURLConnection
                try {
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("x-api-key", apiKey)
                    conn.doOutput = true
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000

                    val body = JSONObject().put("number", number).toString()
                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    conn.responseCode // triggers the request
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                // Best-effort only: if this single POST fails (bad network,
                // server briefly down, ...) we don't retry here - the next
                // incoming call will just try again.
                e.printStackTrace()
            }
        }
    }
}
