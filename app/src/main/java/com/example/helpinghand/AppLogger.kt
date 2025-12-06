package com.example.helpinghand

import android.util.Log

object AppLogger {
    const val TAG_API = "API"
    const val TAG_DB = "DB"
    const val TAG_NAV = "NAV"
    const val TAG_VM = "VM"
    const val TAG_SENSOR = "SENSOR"
    const val TAG_ASYNC = "ASYNC"

    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    fun e(tag: String, msg: String, t: Throwable? = null) {
        Log.e(tag, msg, t)
    }
}
