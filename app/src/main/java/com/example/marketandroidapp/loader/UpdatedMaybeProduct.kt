package com.example.marketandroidapp.loader

import android.content.Context
import io.ktor.client.*
import io.ktor.client.request.*
import shared.Currency
import shared.MaybeProduct

suspend fun updatedMaybeProduct(
    httpClient: HttpClient,
    index: Int,
    chosenCurrencies: Map<Currency, Unit>,
    appContext: Context,
) = downloadBlock(appContext, otherwise = MaybeProduct.None) {
    val initial = httpClient.request<MaybeProduct>("$selfAddress/getProductById/$index")
    if (chosenCurrencies.isEmpty()) return@downloadBlock initial
    initial.map { product ->
        val newPrices = product.prices.filterKeys { it in chosenCurrencies }
        product.copy(prices = newPrices)
    }
}