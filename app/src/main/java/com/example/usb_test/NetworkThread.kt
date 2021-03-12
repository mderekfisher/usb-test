package com.example.usb_test

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.serialization.cbor.*
import kotlinx.serialization.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.withSign

// Network code cannot be run from a main UI thread. Must be separate.

class NetworkThread: Runnable {

    public override fun run() {
//        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
//        val deviceList: HashMap<String, UsbDevice> = manager.getDeviceList()
//        Log.d("tag", deviceList.keys.toString())
//        if (deviceList.keys.size == 1) {
//            val device: UsbDevice = deviceList.values.first()
//            val connection: UsbDeviceConnection = manager.openDevice(device)
//            val outEndpoint: UsbEndpoint = device.getInterface(0).getEndpoint(0)
//            val inEndpoint: UsbEndpoint = device.getInterface(0).getEndpoint(1)
//
//        } else {
//            Log.d("onCreate", "Unable to find USB device. Fail")
//        }
    }
}
