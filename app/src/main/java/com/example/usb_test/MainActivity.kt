package com.example.usb_test

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View

private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

private const val BODY_VENDOR_ID = 1155
private const val BODY_PRODUCT_ID = 0
private const val DFU_PRODUCT_ID = 57105

class MainActivity : AppCompatActivity() {


    private var manager: UsbManager? = null
    private var deviceList: HashMap<String, UsbDevice>? = null
    private var device: UsbDevice? = null
    private var deviceInterface: UsbInterface? = null
    private var sendEndpoint: UsbEndpoint? = null // USB_DIR_OUT
    private var readEndpoint: UsbEndpoint? = null // USB_DIR_IN
    private var deviceConnection: UsbDeviceConnection? = null
    private var usbReceiver: BroadcastReceiver? = null


    object CommandType {
        var PING = 0
        var START = 1
        var STOP = 2
        var DFU = 3
        var LED_SET_BLOCK = 4
        var GAS_GAUGE_READ_DATA = 6 // No longer used in favor of get all sensors
        var SERVO_SET_GAINS_PID = 8
        var BODY_BOARD_REV_NUMBER = 15
        var BODY_BOARD_UNIQUE_ID = 16
        var GET_LOG = 18
        var SERVO_SET_GOAL_ANGLE_3 = 19
        var SERVO_GET_PRESENT_ANGLE_3 = 20 // Obsolete. Use get all sensors.
        var SERVO_FOLLOW_POSITION = 21
        var RESET_BODY_POSITION =
            22 // This was abandoned after making the animation interpreter smarter
        var GET_ALL_SENSORS = 24
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        manager = this.getSystemService(Context.USB_SERVICE) as UsbManager

        usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    ACTION_USB_PERMISSION -> {
                        Log.d("OnReceive", "PERMISSION")
                        synchronized(this) {
                            (intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?)?.let {
                                if (intent.getBooleanExtra(
                                        UsbManager.EXTRA_PERMISSION_GRANTED,
                                        false
                                    )
                                ) {
                                    setDevice(it)
                                } else {
                                    Log.d("USB", "permission denied for device $it")
                                }
                            }
                        }
                    }
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        Log.d("USB", "ATTACHED")
                        synchronized(this) {
                            requestPermission()
                        }

                    }
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        Log.d("USB", "DETACHED")
                        synchronized(this) {
                            (intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?).let {
                                if (device == it) {
                                    release()
                                }
                            }
                        }
                    }
                }
            }
        }

        this.registerReceiver(usbReceiver, IntentFilter(ACTION_USB_PERMISSION))
        this.registerReceiver(usbReceiver, IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED))
        this.registerReceiver(usbReceiver, IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED))
        requestPermission()
    }

    fun setDevice(usbDevice: UsbDevice) {
        device = usbDevice
        deviceInterface = device?.getInterface(0)
        sendEndpoint = deviceInterface?.getEndpoint(0)
        readEndpoint = deviceInterface?.getEndpoint(1)
        val connection = manager?.openDevice(device)
        deviceConnection =
            if (connection != null && connection.claimInterface(deviceInterface, true)) {
                Log.d("USB", "open SUCCESS")
                connection
            } else {
                Log.e("USB", "open FAIL")
                null
            }
    }

    fun release() {
        Log.d("USB", "Releasing USB device")
        deviceConnection?.releaseInterface(device?.getInterface(0))
        deviceConnection?.close()
        deviceConnection = null
    }

    fun requestPermission() {
        Log.d("USB", "Requesting permission...")
        val permissionIntent =
            PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        manager?.accessoryList?.forEach {
            Log.d("ACCESSORY", "manufacturer: ${it.manufacturer}")
        }
        manager?.deviceList?.values?.forEach { device ->
            if (BODY_VENDOR_ID == device.vendorId && BODY_PRODUCT_ID == device.productId) {
                manager?.requestPermission(device, permissionIntent)
            }
//            else if (BODY_VENDOR_ID == device.vendorId && DFU_PRODUCT_ID == device.productId) {
//                manager?.requestPermission(device, permissionIntent)
//            }
        }
    }

    fun readUSBDevList(view: View) {
        manager?.deviceList?.values?.forEach { device ->
            Log.d("USB", "VID: ${device.vendorId}, PID: ${device.productId}")
        }
    }

    fun sendEnterDFU(view: View): Boolean {
        return sendToUsb(byteArrayOf(CommandType.DFU.toByte()), false)
    }

    fun sendPing(view: View): Boolean {
        if (deviceConnection == null) {
            requestPermission()
            return false
        }
        return sendToUsb(byteArrayOf(CommandType.PING.toByte()), true)
    }

    private fun sendToUsb(content: ByteArray, requireResponse: Boolean): Boolean {
        Log.d("USB", "sendToUsb message: sendBytes ${content.contentToString()} ")
        val responseArray = ByteArray(64) // 0 is OK, 1 is that there was some error.
        responseArray[0] = 1
        if (usbRequest(content, responseArray, requireResponse) > 0) {
            if (responseArray[0] == 0.toByte()) {
                return true
            }
        }
        return false
    }

    private fun usbRequest(
        requestArray: ByteArray,
        responseArray: ByteArray,
        requireResponse: Boolean
    ): Int {
        deviceConnection?.let {
            // Send data in requestArray
            var transferSize =
                it.bulkTransfer(
                    sendEndpoint,
                    requestArray,
                    requestArray.size,
                    0
                )

            if (transferSize < 0) {
                Log.w(
                    "USB",
                    "Failed to send USB request. Request: ${requestArray.contentToString()}"
                )
                return transferSize
            }

            // Receive data into responseArray
            if (requireResponse) {
                transferSize =
                    it.bulkTransfer(
                        readEndpoint,
                        responseArray,
                        responseArray.size,
                        0
                    )
                if (transferSize < 0) {
                    Log.w(
                        "USB",
                        "Failed to receive USB request. Request: ${requestArray.contentToString()}"
                    )
                    return transferSize
                }
            }
            return transferSize
        }
        Log.e("USB", "Device connection was null when attempting usbRequest")
        return -1
    }

    override fun onStop() {
        super.onStop()

        try {
            unregisterReceiver(usbReceiver);
        } catch (e: IllegalArgumentException) { /* Already unregistered */
        }
    }
}
