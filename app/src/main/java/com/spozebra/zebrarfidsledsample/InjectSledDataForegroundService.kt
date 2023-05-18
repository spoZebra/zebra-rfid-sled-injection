package com.spozebra.zebrarfidsledsample

import android.R
import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.spozebra.zebrarfidsledsample.barcode.IBarcodeScannedListener
import com.spozebra.zebrarfidsledsample.rfid.IRFIDReaderListener


class InjectSledDataForegroundService : Service(), IBarcodeScannedListener, IRFIDReaderListener {
    val CHANNEL_ID = "InjectSledDataForegroundService"

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val input = intent.getStringExtra("inputExtra")
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("InjectSledDataForegroundService")
            .setContentText(input)
            .setSmallIcon(R.drawable.sym_def_app_icon)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)

        // DOWORK
        return START_NOT_STICKY
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager: NotificationManager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun newBarcodeScanned(barcode: String?) {
        TODO("Not yet implemented")
    }

    override fun newTagRead(epc: String?) {
        TODO("Not yet implemented")
    }
}