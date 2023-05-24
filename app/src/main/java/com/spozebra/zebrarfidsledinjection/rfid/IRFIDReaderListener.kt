package com.spozebra.zebrarfidsledinjection.rfid

interface IRFIDReaderListener {
    fun newTagRead(epc : String?)
}