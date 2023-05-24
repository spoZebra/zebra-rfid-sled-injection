package com.spozebra.zebrarfidsledinjection.barcode

interface IBarcodeScannedListener {
    fun newBarcodeScanned(barcode : String?)
}