package com.example.marketandroidapp.loader

import android.content.Context
import io.ktor.client.*
import io.ktor.client.request.*
import java.lang.IllegalArgumentException

suspend fun updatedProductListSize(httpClient: HttpClient, appContext: Context) =
    downloadBlock(appContext, otherwise = 0UL) {
        val found = httpClient.request<String>("$selfAddress/getProductsCount")
        found.toULongOrNull() ?: throw IllegalArgumentException("$found is not a ULong")
    }