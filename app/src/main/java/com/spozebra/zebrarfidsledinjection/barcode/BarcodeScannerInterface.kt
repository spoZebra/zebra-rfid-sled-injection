package com.spozebra.zebrarfidsledinjection.barcode


import android.content.Context
import com.zebra.rfid.api3.InvalidUsageException
import com.zebra.rfid.api3.OperationFailureException
import com.zebra.scannercontrol.*
import java.util.*


class BarcodeScannerInterface(val listener : IBarcodeScannedListener): IDcsSdkApiDelegate {
    private var sdkHandler: SDKHandler? = null

    fun connect(context : Context) : Boolean {
        if(sdkHandler == null)
            sdkHandler = SDKHandler(context)

        sdkHandler!!.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL)
        sdkHandler!!.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_LE)
        sdkHandler!!.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_USB_CDC)

        sdkHandler!!.dcssdkSetDelegate(this)
        var notifications_mask = 0
        notifications_mask = notifications_mask or (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value or
                DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value)
        notifications_mask = notifications_mask or (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value or
                DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value)
        notifications_mask = notifications_mask or DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value

        // subscribe to events set in notification mask
        sdkHandler!!.dcssdkSubsribeForEvents(notifications_mask)
        sdkHandler!!.dcssdkEnableAvailableScannersDetection(true)

        val scannerInfoList : ArrayList<DCSScannerInfo> = ArrayList()
        sdkHandler!!.dcssdkGetAvailableScannersList(scannerInfoList)

        try {
            val scanner = scannerInfoList[0]
            if(scanner.isActive)
                return true

            // Connect
            val result = sdkHandler!!.dcssdkEstablishCommunicationSession(scanner.scannerID)

            return result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS

        } catch (e: Exception) {
            return false
        }

    }

    fun onDestroy() {
        try {
            if (sdkHandler != null) {
                sdkHandler = null
            }
        } catch (e: InvalidUsageException) {
            e.printStackTrace()
        } catch (e: OperationFailureException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    override fun dcssdkEventScannerAppeared(p0: DCSScannerInfo?) {
    }

    override fun dcssdkEventScannerDisappeared(p0: Int) {
    }

    override fun dcssdkEventCommunicationSessionEstablished(p0: DCSScannerInfo?) {
    }

    override fun dcssdkEventCommunicationSessionTerminated(p0: Int) {
    }

    override fun dcssdkEventBarcode(p0: ByteArray?, p1: Int, p2: Int) {
        val barcode = String(p0!!)
        listener.newBarcodeScanned(barcode)
    }

    override fun dcssdkEventImage(p0: ByteArray?, p1: Int) {
    }

    override fun dcssdkEventVideo(p0: ByteArray?, p1: Int) {
    }

    override fun dcssdkEventBinaryData(p0: ByteArray?, p1: Int) {
    }

    override fun dcssdkEventFirmwareUpdate(p0: FirmwareUpdateEvent?) {
    }

    override fun dcssdkEventAuxScannerAppeared(p0: DCSScannerInfo?, p1: DCSScannerInfo?) {
    }
}