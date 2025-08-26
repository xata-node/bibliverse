package com.gemini.bibliverse.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Класс для управления всеми операциями, связанными с платежами
class BillingManager(context: Context) {

    // Состояния для UI: список товаров и сообщения для пользователя
    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products = _products.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    // Слушатель обновлений покупок
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else {
            Log.e("BillingManager", "Purchase error: ${billingResult.debugMessage}")
            _message.value = "Purchase error: ${billingResult.debugMessage}"
        }
    }

    // Инициализация BillingClient
    private var billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    // Установка соединения с Google Play
    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingManager", "Billing client connected")
                    queryProducts() // Запрашиваем товары после подключения
                } else {
                    Log.e("BillingManager", "Billing client connection failed: ${billingResult.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.w("BillingManager", "Billing client disconnected")
                // Можно попробовать переподключиться
            }
        })
    }

    // Запрос информации о товарах (донатах)
    private fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("donation_tier_1") // ID, который вы указали в Play Console
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("donation_tier_2") // Пример второго доната
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("donation_tier_3")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList)

        billingClient.queryProductDetailsAsync(params.build()) { billingResult, productDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _products.value = productDetailsResult.productDetailsList
                Log.d("BillingManager", "Products queried: ${productDetailsResult.productDetailsList.size}")
            } else {
                Log.e("BillingManager", "Failed to query products: ${billingResult.debugMessage}")
            }
        }
    }

    // Запуск процесса покупки
    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    // Обработка успешной покупки
    private fun handlePurchase(purchase: Purchase) {
        // "Потребляем" товар, чтобы его можно было купить снова (важно для донатов)
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.consumeAsync(consumeParams) { billingResult, _ ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d("BillingManager", "Purchase consumed")
                _message.value = "Thank you for your support!"
            } else {
                Log.e("BillingManager", "Failed to consume purchase: ${billingResult.debugMessage}")
            }
        }
    }

    // Сброс сообщения для UI
    fun clearMessage() {
        _message.value = null
    }
}
