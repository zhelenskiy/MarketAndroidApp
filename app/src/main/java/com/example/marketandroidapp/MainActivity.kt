package com.example.marketandroidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.marketandroidapp.loader.*
import com.example.marketandroidapp.ui.theme.MarketAndroidAppTheme
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import shared.Currency
import shared.MaybeProduct
import shared.Product

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val httpClient = HttpClient {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }
        setContent {
            MarketAndroidAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background,
                ) {
                    SurfaceContent(httpClient)
                }
            }
        }
    }
}

@Composable
private fun SurfaceContent(httpClient: HttpClient) {
    var userIdString: String by remember { mutableStateOf("0") }
    val userId: ULong? = userIdString.toULongOrNull()
    var resetToken by remember { mutableStateOf(Any()) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = userIdString,
                onValueChange = { userIdString = it },
                singleLine = true,
                label = { Text(text = "User Id") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "User Id")
                },
                modifier = Modifier.weight(1.0F),
            )
            IconButton(onClick = { resetToken = Any() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Update")
            }

        }
        if (userId != null) {
            SurfaceUserContent(userId, httpClient, resetToken)
        }
    }
}

@Composable
private fun SurfaceUserContent(userId: ULong, httpClient: HttpClient, resetToken: Any) {
    val chosenCurrencies = remember { mutableStateMapOf<Currency, Unit>() }
    val appContext = LocalContext.current
    LaunchedEffect(key1 = userId, key2 = resetToken) {
        val chosenSet = updatedChosenCurrenciesSet(httpClient, userId, appContext)
        chosenCurrencies.apply {
            clear()
            putAll(chosenSet.associateWith { })
        }
    }
    val scope = rememberCoroutineScope()
    var productsListExpectedSize by remember(resetToken) { mutableStateOf(0UL) }
    LaunchedEffect(key1 = resetToken) {
        productsListExpectedSize = updatedProductListSize(httpClient, appContext)
    }
    val productsList = remember(resetToken) { mutableStateListOf<MaybeProduct?>() }
    CurrencyRow(chosenCurrencies, userId, httpClient) {
        for (index in productsList.indices)
        scope.launch {
            productsList[index] = updatedMaybeProduct(httpClient, index, chosenCurrencies, appContext)
        }
    }
    NewProduct(httpClient) { productsListExpectedSize++ }
    ProductsList(httpClient, chosenCurrencies, productsList, productsListExpectedSize)
}

@Composable
fun NewProduct(
    httpClient: HttpClient,
    afterUpdate: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    if (expanded) {
        var name: String by remember { mutableStateOf("") }
        val pricesStr = remember { mutableStateMapOf<Currency, String>() }
        var validPrices = true
        val prices = pricesStr
            .filterValues { it.isNotEmpty() }
            .mapNotNull { (currency, priceStr) ->
                val toULongOrNull = priceStr.toULongOrNull() ?: return@mapNotNull run {
                    validPrices = false
                    null
                }
                currency to toULongOrNull
            }
            .toMap()
        val scope = rememberCoroutineScope()
        val appContext = LocalContext.current
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            downloadBlock(appContext, Unit) {
                                val product = Product(name, prices)
                                httpClient.request<Unit>("$selfAddress/addProduct") {
                                    method = HttpMethod.Post
                                    contentType(ContentType.Application.Json)
                                    body = product
                                }
                                expanded = false
                                afterUpdate()
                            }
                        }
                    },
                    enabled = name.isNotBlank() && validPrices,
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Accept the new product",
                    )
                }
                IconButton(
                    onClick = { expanded = false },
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel creating a product"
                    )
                }
            }
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "New product name") },
            )
            Text("Prices")
            for (currency in Currency.values()) {
                TextField(
                    value = pricesStr[currency] ?: "",
                    onValueChange = { pricesStr[currency] = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = currency.symbol) }
                )
            }
        }
    } else {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Create a product")
        }
    }
}

@Composable
private fun CurrencyRow(
    chosenCurrencies: SnapshotStateMap<Currency, Unit>,
    userId: ULong,
    httpClient: HttpClient,
    updateToken: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth(),
    ) {
        for (currency in Currency.values()) {
            val isChosen = currency in chosenCurrencies
            val appContext = LocalContext.current
            Button(
                onClick = {
                    if (isChosen) {
                        chosenCurrencies.remove(currency)
                    } else {
                        chosenCurrencies[currency] = Unit
                    }
                    scope.launch {
                        notifyAboutUpdatedChosenCurrencies(
                            httpClient, userId, chosenCurrencies, appContext
                        )
                    }
                    updateToken()
                },
                modifier = if (isChosen) Modifier else Modifier.alpha(0.5F),
            ) {
                Text(text = currency.symbol)
            }
        }
    }
}

@Composable
private fun ProductsList(
    httpClient: HttpClient,
    chosenCurrencies: Map<Currency, Unit>,
    productList: SnapshotStateList<MaybeProduct?>,
    productsListExpectedSize: ULong
) {
    val scope = rememberCoroutineScope()
    val appContext = LocalContext.current
    val state = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(1.0F),
        state = state,
    ) {
        items(minOf(productsListExpectedSize, Int.MAX_VALUE.toULong()).toInt()) { index ->
            while (index !in productList.indices) {
                productList.add(null)
            }
            scope.launch {
                productList[index] = updatedMaybeProduct(httpClient, index, chosenCurrencies, appContext)
            }
            MaybeProduct(productList[index])
        }
    }
}

@Composable
private fun MaybeProduct(product: MaybeProduct?) {
    when (product) {
        is MaybeProduct.Just -> {
            val (name, prices) = product.value
            val currencyText = prices.entries.joinToString("\n") { (currency, price) ->
                currency.format(price)
            }
            Card(
                modifier = Modifier.padding(5.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = name, fontSize = 5.em, fontWeight = FontWeight.Bold)
                    Text(text = currencyText)
                }
            }
        }
        MaybeProduct.None -> Unit
        null -> Text(
            text = "Loading...",
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
