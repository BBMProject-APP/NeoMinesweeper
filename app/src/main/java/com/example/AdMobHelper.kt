package com.example

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

// ==========================================
// 1. BANNER AD COMPOSABLE
// ==========================================
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    // ID Banner Asli milik bosku
    adUnitId: String = "ca-app-pub-8960108261064180/4741036804"
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

// ==========================================
// 2. REWARDED AD MANAGER
// ==========================================
class RewardedAdManager(private val context: Context) {
    private var rewardedAd: RewardedAd? = null

    // ID Rewarded Ad Asli milik bosku
    private val adUnitId: String = "ca-app-pub-8960108261064180/3368403515"

    // Memuat iklan di background
    fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }
            }
        )
    }

    // Menampilkan iklan jika sudah siap
    fun showAd(activity: Activity, onRewardEarned: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.show(activity, OnUserEarnedRewardListener {
                // Panggil callback hadiah (misal: tambah +1 nyawa)
                onRewardEarned()
            })
            rewardedAd = null
            loadAd() // otomatis muat ulang iklan berikutnya
        } else {
            Toast.makeText(context, "wait a moment...", Toast.LENGTH_SHORT).show()
            loadAd()
        }
    }
}