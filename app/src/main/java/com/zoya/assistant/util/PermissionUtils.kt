package com.zoya.assistant.util

import android.Manifest
import android.os.Build

/** The permissions Zoya needs, grouped once so onboarding and settings deep-links agree. */
object ZoyaPermissions {
    val REQUIRED: List<String> = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.CALL_PHONE)
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
