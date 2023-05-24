# Zebra RFID Sled Injection
Sample app that starts a foreground service which is capable of injecting externally both tags and barcode read from a Zebra RFID Sled.

This injection is possible by using our Zebra Key Injection API (com.zebra.eventinjectionservice) which allows a non-privileged application to mimic key presses and screen movements (touch). 
Please reach out to Zebra if you want to know more about it - https://developer.zebra.com/

***Right now, DataWedge does not support embedded scan engine of RFD90 and RFD40 sleds. It will be included by the end of 2023***
In the meantime, to achieve this we have two option:
- Use Zebra SDK in your app (see this sample project as reference: https://github.com/spoZebra/zebra-rfid-sled-sdk-sample)
- Use this sample injection app that acts like DataWedge

## Hardware Requirements
- Zebra RFD90 or RFD40 sled
- A device running A8 or above

## Demo
https://github.com/spoZebra/zebra-rfid-sled-injection/assets/101400857/1dfb07c7-db48-4ee4-a9ac-674c3dab2ae9

