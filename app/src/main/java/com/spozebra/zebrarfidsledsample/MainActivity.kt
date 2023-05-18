package com.spozebra.zebrarfidsledsample

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.KeyEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.spozebra.zebrarfidsledsample.barcode.BarcodeScannerInterface
import com.spozebra.zebrarfidsledsample.barcode.IBarcodeScannedListener
import com.spozebra.zebrarfidsledsample.barcode.TerminalScanDWInterface
import com.spozebra.zebrarfidsledsample.emdk.EmdkEngine
import com.spozebra.zebrarfidsledsample.emdk.IEmdkEngineListener
import com.spozebra.zebrarfidsledsample.rfid.IRFIDReaderListener
import com.spozebra.zebrarfidsledsample.rfid.RFIDReaderInterface
import com.spozebra.zebrarfidsledsample.ssm.ConfigurationManager
import com.symbol.emdk.EMDKResults
import com.zebra.eventinjectionservice.IEventInjectionService


class MainActivity : AppCompatActivity(), IBarcodeScannedListener, IRFIDReaderListener, IEmdkEngineListener {

    private val TAG: String = MainActivity::class.java.simpleName
    private val BLUETOOTH_PERMISSION_REQUEST_CODE = 100

    private var emdkEngine: EmdkEngine? = null
    lateinit var configurationManager: ConfigurationManager
    private var iEventInjectionService : IEventInjectionService? = null

    private lateinit var progressBar: ProgressBar
    private lateinit var listViewRFID: ListView
    private lateinit var listViewBarcodes: ListView
    private lateinit var radioBtnGroup: RadioGroup

    private var scanConnectionMode : ScanConnectionEnum = ScanConnectionEnum.SledScan
    private var isDWRegistered : Boolean = false
    private var barcodeList : MutableList<String> = mutableListOf()
    private var tagsList : MutableList<String> = mutableListOf()


    private val dataWedgeReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (action == "com.spozebra.zebrarfidsledsample.ACTION") {
                val decodedData: String? = intent.getStringExtra("com.symbol.datawedge.data_string")
                this@MainActivity.newBarcodeScanned(decodedData)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        progressBar = findViewById(R.id.progressBar)
        listViewRFID = findViewById(R.id.listViewRFID)
        listViewBarcodes = findViewById(R.id.listViewBarcodes)
        radioBtnGroup = findViewById(R.id.radioGroup)

        val tagsLIstAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, tagsList)
        listViewRFID.adapter = tagsLIstAdapter

        val barcodeListAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, barcodeList)
        listViewBarcodes.adapter = barcodeListAdapter

        radioBtnGroup.setOnCheckedChangeListener { _, checkedId ->

            when (checkedId) {
                R.id.radiobtn_sled -> scanConnectionMode = ScanConnectionEnum.SledScan
                R.id.radiobtn_terminal -> scanConnectionMode = ScanConnectionEnum.TerminalScan
            }
            dispose()
            configureDevice()
        }


        configurationManager = ConfigurationManager(applicationContext)
        // Init Emdk (static, initialized once).
        this.emdkEngine = EmdkEngine.getInstance(applicationContext, this);

        //Scanner Initializations
        //Handling Runtime BT permissions for Android 12 and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
                    BLUETOOTH_PERMISSION_REQUEST_CODE)
            } else {
                configureDevice()
            }
        } else {
            configureDevice()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                configureDevice()
            } else {
                Toast.makeText(this, "Bluetooth Permissions not granted", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    override fun emdkInitialized() {
        // Check if the access was granted already for this app
        // Configuration Manager uses SSM (Zebra Secure Storage Manager) which is in charge of saving/retrieving app params
        var isServiceAccessGranted = configurationManager.getValue(Constants.IS_SERVICE_ACCESS_GRANTED, "false");

        if(isServiceAccessGranted == "false"){
            // If not, download the profile which activates the access thru MX
            val result = emdkEngine!!.setProfile(Constants.SERVICE_ACCESS_PROFILE, null)
            if(result!!.extendedStatusCode == EMDKResults.EXTENDED_STATUS_CODE.NONE) {
                // Access granted, update the permissionsGranted flag
//                configurationManager.updateValue(Constants.IS_SERVICE_ACCESS_GRANTED, "true")
                isServiceAccessGranted = "true"
            }
            else{
                Log.e(TAG, "Error Granting permissions thru MX")
                Toast.makeText(this, "Error Granting permissions thru MX", Toast.LENGTH_SHORT).show()
            }
        }
        if(isServiceAccessGranted == "true") {
            // Get token to be used for dimensioning API
            bindEventInjectionService();
        }
    }

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            iEventInjectionService = IEventInjectionService.Stub.asInterface(service)
            Toast.makeText(
                applicationContext,
                "EventInjectionService Connected",
                Toast.LENGTH_SHORT
            ).show()
            try {
                val result : Boolean = iEventInjectionService!!.authenticate()
                if (result) {
                    Toast.makeText(
                        applicationContext,
                        "caller authentication successful",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        applicationContext,
                        "caller authentication failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (re: RemoteException) {
                Log.d(TAG, "EXCEPTION")
            }

            val handler = Handler()
            handler.postDelayed(object : Runnable {
                override fun run() {
                    //Call your function here
                    type("it works")
                    handler.postDelayed(this, 5000)//1 sec delay
                }
            }, 0)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            iEventInjectionService = null
            Toast.makeText(
                applicationContext,
                "EventInjectionService Disonnected",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun bindEventInjectionService() {
        val intent = Intent("com.zebra.eventinjectionservice.IEventInjectionService")
        intent.setPackage("com.zebra.eventinjectionservice")
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE)
    }

    private fun configureDevice() {
            progressBar.visibility = ProgressBar.VISIBLE
            Thread {
                var connectScannerResult = false

                // CONFIGURE SCANNER
                // If terminal scan was selected, we must use DataWedge instead of the SDK
                if (scanConnectionMode == ScanConnectionEnum.TerminalScan)
                {
                    var dwConf = TerminalScanDWInterface(applicationContext);
                    dwConf.configure()
                    connectScannerResult = true

                    // Register DW receiver
                    registerReceivers()
                }
                else {
                    // Configure BT Scanner
                    if (scannerInterface == null)
                        scannerInterface = BarcodeScannerInterface(this)

                    connectScannerResult = scannerInterface!!.connect(applicationContext)
                }

                // Configure RFID
                if (rfidInterface == null)
                    rfidInterface = RFIDReaderInterface(this)

                var connectRFIDResult = rfidInterface!!.connect(applicationContext, scanConnectionMode)

                runOnUiThread {
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(
                        applicationContext,
                        if (connectRFIDResult && connectScannerResult) "Reader & Scanner are connected!" else "Connection ERROR!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }.start()
    }

    // Create filter for the broadcast intent
    private fun registerReceivers() {
        val filter = IntentFilter()
        filter.addAction("com.symbol.datawedge.api.NOTIFICATION_ACTION") // for notification result
        filter.addAction("com.symbol.datawedge.api.RESULT_ACTION") // for error code result
        filter.addCategory(Intent.CATEGORY_DEFAULT) // needed to get version info

        // register to received broadcasts via DataWedge scanning
        filter.addAction("$packageName.ACTION")
        filter.addAction("$packageName.service.ACTION")
        registerReceiver(dataWedgeReceiver, filter)
        isDWRegistered = true
    }

    override fun newBarcodeScanned(barcode: String?) {
        runOnUiThread {
            type(barcode)
            barcodeList.add(0, barcode!!)
            listViewBarcodes.invalidateViews()
        }
    }

    override fun newTagRead(epc: String?) {
        runOnUiThread {
            type(epc)
            tagsList.add(0, epc!!)
            listViewRFID.invalidateViews()
        }
    }

    private fun type(characters: String?) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("text", characters)
        clipboard.setPrimaryClip(clip)
        sendKeyEvent()
    }

    private fun sendKeyEvent() {
        val t: Thread = object : Thread() {
            override fun run() {
                try {
                    val now = SystemClock.uptimeMillis()

                    iEventInjectionService!!.injectInputEvent(
                        KeyEvent(
                            now,
                            now,
                            KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_PASTE,
                            0
                        ), 2
                    )
                    iEventInjectionService!!.injectInputEvent(
                        KeyEvent(
                            now,
                            now,
                            KeyEvent.ACTION_UP,
                            KeyEvent.KEYCODE_PASTE,
                            0
                        ), 2
                    )
                } catch (re: RemoteException) {
                    Log.d(TAG, re.toString())
                }
            }
        }
        t.start()
    }


    override fun onDestroy() {
        super.onDestroy()
        //dispose()
    }

    private fun dispose(){
        try {

            if (iEventInjectionService != null)
                unbindService(mServiceConnection)
            val intent = Intent("com.zebra.eventinjectionservice.IEventInjectionService")
            intent.setPackage("com.zebra.eventinjectionservice")
            stopService(intent)
            Log.d(TAG, "onDestroy()cald")

            if (isDWRegistered)
                unregisterReceiver(dataWedgeReceiver)

            isDWRegistered = false

            if (rfidInterface != null) {
                rfidInterface!!.onDestroy()
            }
            if (scannerInterface != null) {
                scannerInterface!!.onDestroy()
            }
        }
        catch (ex : Exception){}
    }

    companion object {
        private var rfidInterface: RFIDReaderInterface? = null
        private var scannerInterface: BarcodeScannerInterface? = null
    }

}
