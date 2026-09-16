package com.mazur.tarot.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.mazur.tarot.BuildConfig

private const val TAG = "RewardedAdManager"

/** Oficjalny testowy identyfikator reklamy nagradzanej Google - używany zawsze w buildzie debug. */
private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

/**
 * Debug -> zawsze testowe ID Google (nigdy prawdziwe reklamy podczas developmentu). Release ->
 * [BuildConfig.PROD_REWARDED_UNIT_ID] wczytane z local.properties (ADMOB_REWARDED_UNIT_ID) w
 * app/build.gradle.kts, z fallbackiem na testowe ID, jeśli wpis nie został skonfigurowany. Brak
 * ręcznej podmiany stałych w kodzie na potrzeby publikacji.
 */
private val REWARDED_AD_UNIT_ID: String
    get() = if (BuildConfig.DEBUG) TEST_REWARDED_AD_UNIT_ID else BuildConfig.PROD_REWARDED_UNIT_ID.ifBlank { TEST_REWARDED_AD_UNIT_ID }

/**
 * Cienki wrapper na Google Mobile Ads (AdMob) obsługujący reklamy nagradzane - darmowa
 * alternatywa dla wersji PRO, odblokowująca jedno zapytanie do AI w ekranie "Zapytaj Kart"
 * lub jednorazowe odsłonięcie kart dodatkowych w Karcie Dnia.
 */
class RewardedAdManager(private val context: Context) {

    private var rewardedAd: RewardedAd? = null
    private var initialized = false

    val isAdReady: Boolean get() = rewardedAd != null

    fun initialize() {
        if (initialized) return
        initialized = true
        MobileAds.initialize(context) { loadAd() }
    }

    fun loadAd() {
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                    rewardedAd = null
                }
            },
        )
    }

    /** Wyświetla reklamę, jeśli jest gotowa. [onRewardEarned] jest wywoływane tylko po pełnym obejrzeniu. */
    fun showAd(activity: Activity, onRewardEarned: () -> Unit, onUnavailable: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad == null) {
            loadAd()
            onUnavailable()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded ad failed to show: ${error.message}")
                rewardedAd = null
                loadAd()
            }
        }
        ad.show(activity) { onRewardEarned() }
    }
}
