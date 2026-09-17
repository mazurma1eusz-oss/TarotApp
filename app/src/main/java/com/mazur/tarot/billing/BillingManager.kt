package com.mazur.tarot.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.mazur.tarot.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "BillingManager"

/**
 * Identyfikator dawnego produktu jednorazowego zakupu w Google Play Console. Wycofany z oferty
 * (aplikacja korzysta z płatnego API modelu językowego, więc model "zapłać raz, używaj zawsze"
 * nie jest biznesowo zrównoważony) - NIE jest już nigdzie oferowany do kupienia, ale stała
 * zostaje, żeby [restorePurchases] wciąż honorowało wcześniej wydane licencje (np. u testerów).
 */
const val PRODUCT_ID_PRO_LIFETIME = "tarot_pro_lifetime"

/** Identyfikator subskrypcji miesięcznej PRO w Google Play Console (paywall). */
const val PRODUCT_ID_PRO_MONTHLY = "tarot_pro_monthly"

/** Identyfikator subskrypcji tygodniowej PRO - tańszy próg wejścia niż miesięczna. */
const val PRODUCT_ID_PRO_WEEKLY = "tarot_pro_weekly"

/** Identyfikator subskrypcji rocznej PRO - najlepsza wartość dla stałych użytkowników. */
const val PRODUCT_ID_PRO_YEARLY = "tarot_pro_yearly"

/** Konsumowalny Pakiet Start (10 pytań/dopytań), bez subskrypcji. */
const val PRODUCT_ID_PACK_START = "tarot_pack_start"

/** Konsumowalny Pakiet Standard (50 pytań/dopytań), bez subskrypcji. */
const val PRODUCT_ID_PACK_STANDARD = "tarot_pack_standard"

private val PACK_CREDIT_AMOUNTS = mapOf(
    PRODUCT_ID_PACK_START to 10,
    PRODUCT_ID_PACK_STANDARD to 50,
)

// Docelowy cennik (do czasu skonfigurowania produktów w Google Play Console) - pokazywany
// jako fallback, żeby testerzy od razu widzieli finalne ceny zamiast pustego miejsca.
private const val DEFAULT_MONTHLY_PRICE_LABEL = "24,99 zł/mies."
private const val DEFAULT_WEEKLY_PRICE_LABEL = "9,99 zł/tydz."
private const val DEFAULT_YEARLY_PRICE_LABEL = "149,99 zł/rok"
private const val DEFAULT_PACK_START_PRICE_LABEL = "4,99 zł"
private const val DEFAULT_PACK_STANDARD_PRICE_LABEL = "19,99 zł"

/**
 * Cienki wrapper na Google Play Billing Library odpowiedzialny za hybrydową monetyzację:
 * odblokowanie Dostępu Premium subskrypcją ([PRODUCT_ID_PRO_WEEKLY] lub [PRODUCT_ID_PRO_MONTHLY])
 * ORAZ konsumowalne pakiety pytań ([PRODUCT_ID_PACK_START], [PRODUCT_ID_PACK_STANDARD]) dla
 * użytkowników, którzy nie chcą subskrypcji. Jednorazowy zakup [PRODUCT_ID_PRO_LIFETIME] nie
 * jest już oferowany - patrz komentarz przy tej stałej.
 */
class BillingManager(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore,
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val _monthlyProductDetails = MutableStateFlow<ProductDetails?>(null)
    val monthlyProductDetails: StateFlow<ProductDetails?> = _monthlyProductDetails.asStateFlow()

    private val _weeklyProductDetails = MutableStateFlow<ProductDetails?>(null)
    val weeklyProductDetails: StateFlow<ProductDetails?> = _weeklyProductDetails.asStateFlow()

    private val _yearlyProductDetails = MutableStateFlow<ProductDetails?>(null)
    val yearlyProductDetails: StateFlow<ProductDetails?> = _yearlyProductDetails.asStateFlow()

    private val _packStartProductDetails = MutableStateFlow<ProductDetails?>(null)
    val packStartProductDetails: StateFlow<ProductDetails?> = _packStartProductDetails.asStateFlow()

    private val _packStandardProductDetails = MutableStateFlow<ProductDetails?>(null)
    val packStandardProductDetails: StateFlow<ProductDetails?> = _packStandardProductDetails.asStateFlow()

    /** Sformatowana cena (np. "24,99 zł/mies.") do wyświetlenia na Paywallu - realna cena z
     * Google Play, jeśli produkt jest już skonfigurowany w Konsoli, w przeciwnym razie
     * docelowy cennik jako fallback, żeby UI nigdy nie pokazywał pustego miejsca. */
    val monthlyPriceLabel: String get() = subscriptionPriceLabel(_monthlyProductDetails.value) ?: DEFAULT_MONTHLY_PRICE_LABEL
    val weeklyPriceLabel: String get() = subscriptionPriceLabel(_weeklyProductDetails.value) ?: DEFAULT_WEEKLY_PRICE_LABEL
    val yearlyPriceLabel: String get() = subscriptionPriceLabel(_yearlyProductDetails.value) ?: DEFAULT_YEARLY_PRICE_LABEL
    val packStartPriceLabel: String get() = oneTimePriceLabel(_packStartProductDetails.value) ?: DEFAULT_PACK_START_PRICE_LABEL
    val packStandardPriceLabel: String get() = oneTimePriceLabel(_packStandardProductDetails.value) ?: DEFAULT_PACK_STANDARD_PRICE_LABEL

    private fun subscriptionPriceLabel(details: ProductDetails?): String? = details
        ?.subscriptionOfferDetails
        ?.firstOrNull()
        ?.pricingPhases
        ?.pricingPhaseList
        ?.firstOrNull()
        ?.formattedPrice

    private fun oneTimePriceLabel(details: ProductDetails?): String? =
        details?.oneTimePurchaseOfferDetails?.formattedPrice

    private val _billingConnected = MutableStateFlow(false)
    val billingConnected: StateFlow<Boolean> = _billingConnected.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        } else {
            Log.w(TAG, "Purchases update: ${result.responseCode} ${result.debugMessage}")
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun startConnection() {
        if (billingClient.isReady) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                _billingConnected.value = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    restorePurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingConnected.value = false
            }
        })
    }

    private fun queryProductDetails() {
        val monthlyProduct = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID_PRO_MONTHLY)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val weeklyProduct = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID_PRO_WEEKLY)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val yearlyProduct = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID_PRO_YEARLY)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val packStartProduct = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID_PACK_START)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val packStandardProduct = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID_PACK_STANDARD)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(monthlyProduct, weeklyProduct, yearlyProduct, packStartProduct, packStandardProduct))
            .build()

        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetailsList = queryResult.productDetailsList
                _monthlyProductDetails.value = productDetailsList.firstOrNull { it.productId == PRODUCT_ID_PRO_MONTHLY }
                _weeklyProductDetails.value = productDetailsList.firstOrNull { it.productId == PRODUCT_ID_PRO_WEEKLY }
                _yearlyProductDetails.value = productDetailsList.firstOrNull { it.productId == PRODUCT_ID_PRO_YEARLY }
                _packStartProductDetails.value = productDetailsList.firstOrNull { it.productId == PRODUCT_ID_PACK_START }
                _packStandardProductDetails.value = productDetailsList.firstOrNull { it.productId == PRODUCT_ID_PACK_STANDARD }
            } else {
                Log.w(TAG, "queryProductDetails failed: ${result.debugMessage}")
            }
        }
    }

    /**
     * Odpytuje Google Play o wcześniej dokonane zakupy (jednorazowe ORAZ subskrypcje) -
     * przywraca stan PRO po reinstalacji lub po kliknięciu "Przywróć Zakupy". Wywoływane
     * automatycznie po starcie połączenia z Google Play. Obie kategorie (INAPP/SUBS) są
     * odpytywane osobno (wymóg Billing API), a wynik łączony dopiero po obu odpowiedziach,
     * żeby nie zgasić flagi PRO tylko dlatego, że jedna z dwóch kategorii jest pusta.
     * Przy okazji dokańcza (konsumuje) niedokończone zakupy pakietów pytań - patrz [handlePurchase].
     */
    fun restorePurchases() {
        var inappChecked = false
        var subsChecked = false
        var anyActiveProPurchase = false

        fun finishIfDone() {
            if (inappChecked && subsChecked && !anyActiveProPurchase) {
                scope.launch { settingsDataStore.setProUnlocked(false) }
            }
        }

        queryPurchasesOfType(BillingClient.ProductType.INAPP) { foundPro ->
            inappChecked = true
            anyActiveProPurchase = anyActiveProPurchase || foundPro
            finishIfDone()
        }
        queryPurchasesOfType(BillingClient.ProductType.SUBS) { foundPro ->
            subsChecked = true
            anyActiveProPurchase = anyActiveProPurchase || foundPro
            finishIfDone()
        }
    }

    private fun queryPurchasesOfType(productType: String, onChecked: (foundActivePro: Boolean) -> Unit) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(productType)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { handlePurchase(it) }
                val foundPro = purchases.any {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        (
                            it.products.contains(PRODUCT_ID_PRO_LIFETIME) ||
                                it.products.contains(PRODUCT_ID_PRO_MONTHLY) ||
                                it.products.contains(PRODUCT_ID_PRO_WEEKLY) ||
                                it.products.contains(PRODUCT_ID_PRO_YEARLY)
                            )
                }
                onChecked(foundPro)
            } else {
                onChecked(false)
            }
        }
    }

    /** Uruchamia zakup subskrypcji miesięcznej z ekranu Paywall. */
    fun launchSubscriptionPurchaseFlow(activity: Activity) {
        launchSubsFlow(activity, _monthlyProductDetails.value)
    }

    /** Uruchamia zakup subskrypcji tygodniowej z ekranu Paywall. */
    fun launchWeeklySubscriptionPurchaseFlow(activity: Activity) {
        launchSubsFlow(activity, _weeklyProductDetails.value)
    }

    /** Uruchamia zakup subskrypcji rocznej z ekranu Paywall. */
    fun launchYearlySubscriptionPurchaseFlow(activity: Activity) {
        launchSubsFlow(activity, _yearlyProductDetails.value)
    }

    /** Uruchamia zakup jednorazowego, konsumowalnego Pakietu Start (5 pytań). */
    fun launchPackStartPurchaseFlow(activity: Activity) {
        _packStartProductDetails.value?.let { launchInappFlow(activity, it) }
            ?: Log.w(TAG, "Pack Start details not loaded yet")
    }

    /** Uruchamia zakup jednorazowego, konsumowalnego Pakietu Standard (30 pytań). */
    fun launchPackStandardPurchaseFlow(activity: Activity) {
        _packStandardProductDetails.value?.let { launchInappFlow(activity, it) }
            ?: Log.w(TAG, "Pack Standard details not loaded yet")
    }

    private fun launchSubsFlow(activity: Activity, details: ProductDetails?) {
        val offerToken = details?.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (details == null || offerToken == null) {
            Log.w(TAG, "Subscription details not loaded yet")
            return
        }
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build()
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    private fun launchInappFlow(activity: Activity, details: ProductDetails) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        val isProPurchase = purchase.products.any {
            it == PRODUCT_ID_PRO_LIFETIME || it == PRODUCT_ID_PRO_MONTHLY || it == PRODUCT_ID_PRO_WEEKLY || it == PRODUCT_ID_PRO_YEARLY
        }
        val packProductId = purchase.products.firstOrNull { PACK_CREDIT_AMOUNTS.containsKey(it) }

        if (isProPurchase) {
            handleProPurchase(purchase)
        } else if (packProductId != null) {
            handlePackPurchase(purchase, packProductId)
        }
    }

    private fun handleProPurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                scope.launch {
                    settingsDataStore.setProUnlocked(true)
                    settingsDataStore.saveLastPurchaseToken(purchase.purchaseToken)
                }
                if (!purchase.isAcknowledged) {
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(ackParams) { result ->
                        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                            Log.w(TAG, "Acknowledge failed: ${result.debugMessage}")
                        }
                    }
                }
            }
            Purchase.PurchaseState.PENDING -> {
                Log.i(TAG, "Purchase pending, awaiting completion")
            }
            else -> Unit
        }
    }

    /**
     * Pakiety pytań to produkty konsumowalne: kredyty są przyznawane od razu po zakupie,
     * a zakup jest natychmiast "skonsumowany" ([ConsumeParams]), żeby użytkownik mógł kupić
     * kolejny pakiet w przyszłości (Google Play nie pozwala ponownie kupić nieskonsumowanego
     * produktu jednorazowego).
     */
    private fun handlePackPurchase(purchase: Purchase, productId: String) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val amount = PACK_CREDIT_AMOUNTS[productId] ?: return
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.consumeAsync(consumeParams) { result, _ ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                scope.launch { settingsDataStore.grantQuestionCredits(amount) }
            } else {
                Log.w(TAG, "Consume failed for $productId: ${result.debugMessage}")
            }
        }
    }

    fun endConnection() {
        if (billingClient.isReady) billingClient.endConnection()
    }
}
