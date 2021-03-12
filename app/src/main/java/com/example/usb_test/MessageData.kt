package com.example.usb_test

import kotlinx.serialization.*

@Serializable
data class FrameData(val frame_id: Int, val data: Map<Int, Int>)
