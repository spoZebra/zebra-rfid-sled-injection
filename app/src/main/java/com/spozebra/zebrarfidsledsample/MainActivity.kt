package com.spozebra.zebrarfidsledsample

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.spozebra.zebrarfidsledsample.emdk.EmdkEngine
import com.spozebra.zebrarfidsledsample.emdk.IEmdkEngineListener
import com.spozebra.zebrarfidsledsample.ssm.ConfigurationManager
import com.symbol.emdk.EMDKResults
import java.io.File


class MainActivity : AppCompatActivity(), IEmdkEngineListener {

    private val TAG: String = MainActivity::class.java.simpleName
    private val BLUETOOTH_PERMISSION_REQUEST_CODE = 100

    private var emdkEngine: EmdkEngine? = null
    lateinit var configurationManager: ConfigurationManager

    private lateinit var restartButton: Button
    private lateinit var listViewLog: ListView

    private lateinit var serviceIntent: Intent
    private var logList: MutableList<String> = mutableListOf()

    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == InjectSledDataForegroundService.ACTION_LOG_UPDATED) {
                val logMessage = intent.getStringExtra(InjectSledDataForegroundService.EXTRA_LOG_MESSAGE)
                newDataFromForeground(logMessage)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listViewLog = findViewById(R.id.listViewLog)

        logList = parseLogFile() as MutableList<String>
        val tagsLIstAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, logList)
        listViewLog.adapter = tagsLIstAdapter

        configurationManager = ConfigurationManager(applicationContext)

        restartButton = findViewById(R.id.restartButton)

        restartButton.setOnClickListener {
            serviceIntent = Intent(this, InjectSledDataForegroundService::class.java)
            stopService(serviceIntent)
            startForegroundService(serviceIntent)
        }

        // Register the broadcast receiver
        registerReceivers()

        //Scanner Initializations
        //Handling Runtime BT permissions for Android 12 and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ),
                    BLUETOOTH_PERMISSION_REQUEST_CODE
                )
            } else {
                configureDevice()
            }
        } else {
            configureDevice()
        }
    }

    private fun parseLogFile(): List<String> {
        val logFile = File(applicationContext.filesDir, "logs/service_logs.txt")
        if(!logFile.exists())
            return mutableListOf()

        return try {
            logFile.readLines()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                configureDevice()
            } else {
                Toast.makeText(this, "Bluetooth Permissions not granted", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // Create filter for the broadcast intent
    private fun registerReceivers() {
        // Register the log receiver
        val intentFilter = IntentFilter(InjectSledDataForegroundService.ACTION_LOG_UPDATED)
        LocalBroadcastManager.getInstance(this).registerReceiver(logReceiver, intentFilter)
    }

    private fun configureDevice(){
        // Init Emdk (static, initialized once).
        this.emdkEngine = EmdkEngine.getInstance(applicationContext, this);
    }

    override fun emdkInitialized() {
        // Check if the access was granted already for this app
        // Configuration Manager uses SSM (Zebra Secure Storage Manager) which is in charge of saving/retrieving app params
        var isServiceAccessGranted = "false" // TODO, fix SSM implementation...for now let's grant the access everytime
            //configurationManager.getValue(Constants.IS_SERVICE_ACCESS_GRANTED, "false");

        if (isServiceAccessGranted == "false") {
            // If not, download the profile which activates the access thru MX
            val result = emdkEngine!!.setProfile(Constants.SERVICE_ACCESS_PROFILE, null)
            if (result!!.extendedStatusCode == EMDKResults.EXTENDED_STATUS_CODE.NONE) {
                // Access granted, update the permissionsGranted flag
//                configurationManager.updateValue(Constants.IS_SERVICE_ACCESS_GRANTED, "true")
                isServiceAccessGranted = "true"
            } else {
                Log.e(TAG, "Error Granting permissions thru MX")
                Toast.makeText(this, "Error Granting permissions thru MX", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        if (isServiceAccessGranted == "true") {

            // START FOREGROUND SERVICE
            serviceIntent = Intent(this, InjectSledDataForegroundService::class.java)
            startForegroundService(serviceIntent)
        }
    }

    fun newDataFromForeground(data: String?) {
        runOnUiThread {
            logList.add(0, data!!)
            listViewLog.invalidateViews()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(logReceiver)
    }
}