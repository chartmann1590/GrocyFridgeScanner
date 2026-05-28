package com.charleshartmann.grocyfridge.ads

import android.app.Activity
import android.util.Log
import com.charleshartmann.grocyfridge.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAdManager {

    private const val TAG = "InterstitialAdManager"

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun load(activity: Activity) {
        if (areAdsHidden() || interstitialAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            activity,
            BuildConfig.AD_MOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    Log.d(TAG, "Interstitial ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.w(TAG, "Interstitial ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun show(activity: Activity, onDismissed: () -> Unit) {
        if (areAdsHidden()) {
            onDismissed()
            return
        }
        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "No interstitial ad ready, proceeding")
            onDismissed()
            load(activity)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                Log.d(TAG, "Interstitial ad dismissed")
                onDismissed()
                load(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                Log.w(TAG, "Interstitial ad failed to show: ${error.message}")
                onDismissed()
                load(activity)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad showed")
            }
        }

        ad.show(activity)
    }

    private fun areAdsHidden(): Boolean = try {
        val cls = Class.forName("android.os.SystemProperties")
        val get = cls.getMethod("get", String::class.java, String::class.java)
        val value = get.invoke(null, "debug.grocy.ads.disabled", "0") as? String
        value == "1"
    } catch (_: Throwable) {
        false
    }
}
