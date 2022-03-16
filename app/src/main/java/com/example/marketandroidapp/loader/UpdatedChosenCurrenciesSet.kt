package com.example.marketandroidapp.loader

import android.content.Context
import io.ktor.client.*
import io.ktor.client.request.*
import shared.Currency


suspend fun updatedChosenCurrenciesSet(httpClient: HttpClient, userId: ULong?, appContext: Context) =
    downloadBlock(appContext, otherwise = emptySet()) {
        httpClient.request<Set<Currency>>("$selfAddress/getCurrenciesByUserId/$userId")
    }