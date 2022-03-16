package com.example.marketandroidapp.loader

import android.content.Context
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import shared.Currency

suspend fun notifyAboutUpdatedChosenCurrencies(
    httpClient: HttpClient,
    userId: ULong,
    chosenCurrencies: Map<Currency, Unit>,
    appContext: Context,
) = downloadBlock(appContext, Unit) {
    httpClient.request<Unit>("$selfAddress/setCurrenciesByUserId/$userId") {
        method = HttpMethod.Put
        contentType(ContentType.Application.Json)
        body = chosenCurrencies.keys.map { it.name }
    }
}