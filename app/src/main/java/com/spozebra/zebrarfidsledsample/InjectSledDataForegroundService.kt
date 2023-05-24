package com.spozebra.zebrarfidsledsample

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.*
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.spozebra.zebrarfidsledsample.barcode.BarcodeScannerInterface
import com.spozebra.zebrarfidsledsample.barcode.IBarcodeScannedListener
import com.spozebra.zebrarfidsledsample.rfid.IRFIDReaderListener
import com.spozebra.zebrarfidsledsample.rfid.RFIDReaderInterface
import com.zebra.eventinjectionservice.IEventInjectionService

class InjectSledDataForegroundService : Service(), IBarcodeScannedListener, IRFIDReaderListener {

    private val TAG: String = InjectSledDataForegroundService::class.java.simpleName
    private val CHANNEL_ID = "InjectSledDataForegroundService"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        // Perform any additional setup here if needed
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create the notification channel
        createNotificationChannel()

        // Create the notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setContentText("Service is running...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        // Start the service in the foreground with the notification
        startForeground(NOTIFICATION_ID, notification)
        val t: Thread = object : Thread() {
            override fun run() {
                try {
                    log("InjectSledDataForegroundService started")
                    bindEventInjectionService()
                    configureDevice()
                } catch (re: Exception) {
                    Log.d(TAG, re.toString())
                }
            }
        }
        t.start()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Return null because this service doesn't support binding
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun bindEventInjectionService() {
        val intent = Intent("com.zebra.eventinjectionservice.IEventInjectionService")
        intent.setPackage("com.zebra.eventinjectionservice")
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE)
    }

    private fun configureDevice() {

        log("Looking for RFID Sled...")

        // CONFIGURE SCANNER
        // Configure BT Scanner
        if (scannerInterface == null) {
            scannerInterface = BarcodeScannerInterface(this)
        }

        var res = scannerInterface!!.connect(applicationContext)
        if(res)
            log("Scanner initialized")
        else
            log("ERROR: Unable to initialize sled scanner")

        // Configure RFID
        if (rfidInterface == null) {
            rfidInterface = RFIDReaderInterface(this)
        }

        res = rfidInterface!!.connect(applicationContext)
        if(res)
            log("RFID initialized")
        else
            log("ERROR: Unable to initialize sled RFID antenna")
    }

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            iEventInjectionService = IEventInjectionService.Stub.asInterface(service)
            log("EventInjectionService Connected")
            try {
                val result : Boolean = iEventInjectionService!!.authenticate()
                if (result) {
                    log("EventInjectionService: Caller authentication successful")
                } else {
                    log("ERROR: EventInjectionService caller authentication failed")
                }
            } catch (re: RemoteException) {
                log("ERROR: " + re.message)
                Log.d(TAG, "EXCEPTION")
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            log("EventInjectionService Disonnected")
        }
    }

    override fun newBarcodeScanned(barcode: String?) {
        log("New barcode scanned: $barcode")
        injectData(barcode)
    }

    override fun newTagRead(epc: String?) {
        log("New tag collected: $epc")
        injectData(epc)
    }

    private fun injectData(characters: String?) {
        // Copy scanned data into device clipboard to be pasted using our event injection service
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("text", characters)
        clipboard.setPrimaryClip(clip)
        // Paste data
        sendKeyEvent()
    }

    private fun sendKeyEvent() {
        val t: Thread = object : Thread() {
            override fun run() {
                try {
                    // Inject a PASTE action so that the tag/barcode data will be injected in any field under focus
                    val now = SystemClock.uptimeMillis()
                    // Press paste
                    iEventInjectionService!!.injectInputEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PASTE, 0), 2)
                    // Release paste
                    iEventInjectionService!!.injectInputEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PASTE, 0), 2)
                } catch (re: RemoteException) {
                    Log.d(TAG, re.toString())
                }
            }
        }
        t.start()
    }

    private fun log(message: String) {
         broadcastLogUpdate(message)
    }

    private fun broadcastLogUpdate(log: String) {
        val intent = Intent(ACTION_LOG_UPDATED)
        intent.putExtra(EXTRA_LOG_MESSAGE, log)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {
        val ACTION_LOG_UPDATED = "com.spozebra.zebrarfidsledsample.ACTION_LOG_UPDATED"
        val EXTRA_LOG_MESSAGE = "com.spozebra.zebrarfidsledsample.EXTRA_LOG_MESSAGE"
        private var iEventInjectionService: IEventInjectionService? = null
        private var rfidInterface: RFIDReaderInterface? = null
        private var scannerInterface: BarcodeScannerInterface? = null
    }
}