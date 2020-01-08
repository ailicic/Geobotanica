package com.geobotanica.geobotanica.util

import android.os.Build

fun isEmulator(): Boolean = Build.FINGERPRINT.startsWith("generic")
    || Build.FINGERPRINT.startsWith("unknown")
    || Build.MODEL.contains("google_sdk")
    || Build.MODEL.contains("Emulator")
    || Build.MODEL.contains("Android SDK built for x86")
    || Build.MANUFACTURER.contains("Genymotion")
    || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
    || "google_sdk" == Build.PRODUCT