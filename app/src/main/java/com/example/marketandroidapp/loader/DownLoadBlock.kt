package com.example.marketandroidapp.loader

import android.content.Context
import android.widget.Toast

suspend fun <T> downloadBlock(context: Context, otherwise: T, action: suspend () -> T): T = try {
    action()
} catch (e: Exception) {
    e.printStackTrace()
    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
    otherwise
}

const val selfAddress = "http://10.0.2.2:8080"