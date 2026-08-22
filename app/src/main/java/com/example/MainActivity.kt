package com.example

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ThemeColors {
    // Overall Theme Backgrounds (deep midnight / obsidian navy)
    val BackgroundDark1 = Color(0xFF0B1426)
    val BackgroundDark2 = Color(0xFF0F1A30)
    val BackgroundGradient = Brush.verticalGradient(
        colors = listOf(BackgroundDark1, BackgroundDark2),
    )

    // Card and Section Containers (slightly lighter, desaturated dark blue-grey)
    val CardBg = Color(0xFF152238)
    val CardBgSecondary = Color(0xFF1B2A47)

    // Primary Action Accent (sky blue / electric blue)
    val PrimaryAccent = Color(0xFF38B6FF)
    val PrimaryAccentDark = Color(0xFF29A3EF)

    // Secondary / Utility Buttons / Unselected difficulty tabs
    val SecondarySteel = Color(0xFF24344D)
    val SecondarySteelLight = Color(0xFF2C3E59)

    // Text Elements
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFE2E8F0) // slate-200
    val TextMuted = Color(0xFF94A3B8) // slate-400

    // Borders
    val BorderSubtle = Color(0xFF2C3E59)

    // Numbers for Clue Tiles (Minesweeper)
    val Number1 = Color(0xFF38B6FF) // Sky Blue
    val Number2 = Color(0xFF34D399) // Emerald Green
    val Number3 = Color(0xFFF87171) // Light Red
    val Number4 = Color(0xFFC084FC) // Purple
    val Number5OrMore = Color(0xFFFBBF24) // Amber/Orange
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Sound feedback helper
object SoundHelper {
    private var soundPool: SoundPool? = null
    private var soundIdClick: Int = -1
    private var soundIdFlag: Int = -1
    private var soundIdExplosion: Int = -1
    private var soundIdWin: Int = -1

    fun initialize(context: Context) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundIdClick = soundPool?.load(context, R.raw.click, 1) ?: -1
        soundIdFlag = soundPool?.load(context, R.raw.flag, 1) ?: -1
        soundIdExplosion = soundPool?.load(context, R.raw.explosion, 1) ?: -1
        soundIdWin = soundPool?.load(context, R.raw.win, 1) ?: -1
    }

    fun playClick() {
        if (soundIdClick != -1) soundPool?.play(soundIdClick, 1f, 1f, 0, 0, 1f)
    }

    fun playReveal() {
        // Using same click sound for reveal
        playClick()
    }

    fun playFlag() {
        if (soundIdFlag != -1) soundPool?.play(soundIdFlag, 1f, 1f, 0, 0, 1f)
    }

    fun playExplosion() {
        if (soundIdExplosion != -1) soundPool?.play(soundIdExplosion, 1f, 1f, 0, 0, 1f)
    }

    fun playWin() {
        if (soundIdWin != -1) soundPool?.play(soundIdWin, 1f, 1f, 0, 0, 1f)
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            super.attachBaseContext(newBase.createAttributionContext("default"))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 🚀 Inisialisasi Splash Screen (Memanggil logo Splash.png)
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // 🔊 Inisialisasi SoundHelper
        SoundHelper.initialize(this)

        // 📢 Inisialisasi SDK AdMob
        MobileAds.initialize(this) {}

        enableEdgeToEdge()

        // 🎨 Bungkus UI dengan Theme (dynamicColor = false agar warna tetap neon)
        setContent {
            MyApplicationTheme(dynamicColor = false) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        MinesweeperApp()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundHelper.release()
    }
}

@Composable
fun AdMobBannerPlaceholder(modifier: Modifier = Modifier) {
    // Memuat AdMob Banner Ad yang sebenarnya menggunakan AndroidView
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        factory = { context ->
            AdView(context).apply {
                // Tentukan ukuran banner (320x50 standar / banner adaptif)
                setAdSize(AdSize.BANNER)

                // Gunakan Ad Unit ID Asli milikmu atau Test Unit ID
                // Test Banner ID resmi Google: "ca-app-pub-3940256099942544/6300978111"
                adUnitId = "ca-app-pub-8960108261064180/7896131458"

                // Mulai panggil/load iklan
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
@Composable
fun MinesweeperApp() {
    val viewModel: MinesweeperViewModel = viewModel()
    var isPlaying by remember { mutableStateOf(value = false) }
    val context = LocalContext.current

    // Observe toasts
    LaunchedEffect(viewModel.toastMessage) {
        viewModel.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColors.BackgroundGradient)
        ) {
            // Persistent Google AdMob Banner Ad at the absolute top of the screen
            AdMobBannerPlaceholder()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (!isPlaying) {
                    MainMenuScreen(
                        viewModel = viewModel
                    ) {
                        isPlaying = true
                        viewModel.startNewGame(viewModel.currentDifficulty)
                    }
                } else {
                    GameplayScreen(
                        viewModel = viewModel,
                        onBack = {
                            isPlaying = false
                        }
                    )
                }
            }
        }

        // Fireworks overlay when user wins!
        if (viewModel.showWinModal) {
            FireworksEffect(modifier = Modifier.fillMaxSize())
        }

        // Modal Manager
        ModalManager(
            viewModel = viewModel,
            onPlayAgain = {
                viewModel.startNewGame(viewModel.currentDifficulty)
            }
        ) {
            isPlaying = false
        }
    }
}

@Composable
fun TopTimesCard(
    difficultyName: String,
    times: List<CompletionTime>,
    onClear: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = ThemeColors.CardBg
        ),
        border = BorderStroke(1.dp, ThemeColors.BorderSubtle),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 TOP 5 SPEEDRUNS",
                    color = ThemeColors.PrimaryAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                if (times.isNotEmpty()) {
                    Text(
                        text = "Clear",
                        color = Color(0xFFEF4444).copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onClear() }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (times.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ThemeColors.SecondarySteel.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No speedruns yet for $difficultyName.\nClear the board to set a record!",
                        color = ThemeColors.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    times.forEachIndexed { index, record ->
                        val medal = when (index) {
                            0 -> "🥇"
                            1 -> "🥈"
                            2 -> "🥉"
                            else -> "⏱️"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ThemeColors.CardBgSecondary, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = medal,
                                    fontSize = 14.sp,
                                    modifier = Modifier.width(24.dp)
                                )
                                Text(
                                    text = "Rank #${index + 1}",
                                    color = ThemeColors.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val min = record.timeSeconds / 60
                                val sec = record.timeSeconds % 60
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d:%02d", min, sec),
                                    color = ThemeColors.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = formatTimestamp(record.timestamp),
                                    color = ThemeColors.TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainMenuScreen(
    viewModel: MinesweeperViewModel,
    onPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeColors.BackgroundGradient)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header / Logo Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(
                    text = "NEO MINESWEEPER",
                    color = ThemeColors.TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "PORTRAIT LEVEL EDITION",
                    color = ThemeColors.PrimaryAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Score stats card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = ThemeColors.CardBg
                    ),
                    border = BorderStroke(1.dp, ThemeColors.BorderSubtle),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("WINS", color = ThemeColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${viewModel.wins}", color = ThemeColors.PrimaryAccent, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(ThemeColors.BorderSubtle)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("LOSSES", color = ThemeColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${viewModel.loses}", color = Color(0xFFEF4444), fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(ThemeColors.BorderSubtle)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("LIVES", color = ThemeColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${viewModel.lives}", color = Color(0xFFEF4444), fontSize = 24.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            // Controls & Selection Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SELECT DIFFICULTY",
                    color = ThemeColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Level Button Segment selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .background(ThemeColors.CardBgSecondary, RoundedCornerShape(16.dp))
                        .border(1.dp, ThemeColors.BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Difficulty.entries.forEach { diff ->
                        val selected = viewModel.currentDifficulty == diff
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) ThemeColors.PrimaryAccent else Color.Transparent)
                                .clickable {
                                    SoundHelper.playClick()
                                    viewModel.changeDifficulty(diff)
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = diff.name,
                                color = if (selected) Color.White else ThemeColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // High scores card displays the top 5 speedrun times for this level
            TopTimesCard(
                difficultyName = viewModel.currentDifficulty.name,
                times = viewModel.topTimesForCurrentDifficulty,
                onClear = {
                    SoundHelper.playClick()
                    viewModel.clearScores()
                }
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Resume Button if saved game exists
                if (viewModel.hasSavedGame()) {
                    Button(
                        onClick = {
                            SoundHelper.playClick()
                            val loaded = viewModel.loadSavedGame()
                            if (loaded) {
                                onPlay()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemeColors.PrimaryAccent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RESUME SAVED GAME",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Play Button
                Button(
                    onClick = {
                        SoundHelper.playClick()
                        onPlay()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemeColors.PrimaryAccentDark
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (viewModel.hasSavedGame()) "START NEW GAME" else "PLAY GAME",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // How To Play Button
                OutlinedButton(
                    onClick = {
                        SoundHelper.playClick()
                        viewModel.showHowToModal = true
                    },
                    border = BorderStroke(1.dp, ThemeColors.PrimaryAccent),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ThemeColors.PrimaryAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = ThemeColors.PrimaryAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HOW TO PLAY",
                        color = ThemeColors.PrimaryAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Footer
            Text(
                text = "NEO MINESWEEPER © 2026",
                color = ThemeColors.TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun GameplayScreen(
    viewModel: MinesweeperViewModel,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    // 🎬 Inisialisasi RewardedAdManager khusus di layar gameplay
    val rewardedAdManager = remember { RewardedAdManager(context) }

    // Auto-load iklan begitu pemain masuk ke GameplayScreen
    LaunchedEffect(Unit) {
        rewardedAdManager.loadAd()
    }

    // System back button interceptor
    BackHandler(enabled = true) {
        if (viewModel.gameLogic.status == GameStatus.PLAYING) {
            viewModel.showSaveConfirmationModal = true
        } else {
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeColors.BackgroundGradient)
    ) {
        // ==========================================
        // 🎯 BANNER ADMOB DI SINI
        // ==========================================
        AdMobBanner()

        // Status bar header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    SoundHelper.playClick()
                    if (viewModel.gameLogic.status == GameStatus.PLAYING) {
                        viewModel.showSaveConfirmationModal = true
                    } else {
                        onBack()
                    }
                },
                modifier = Modifier
                    .background(ThemeColors.CardBg, CircleShape)
                    .border(1.dp, ThemeColors.BorderSubtle, CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ThemeColors.TextPrimary
                )
            }

            // Stats row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timer Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(ThemeColors.CardBg, RoundedCornerShape(20.dp))
                        .border(1.dp, ThemeColors.BorderSubtle, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("⏱️", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    val min = viewModel.timeElapsed / 60
                    val sec = viewModel.timeElapsed % 60
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d", min, sec),
                        color = ThemeColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Flags Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(ThemeColors.CardBg, RoundedCornerShape(20.dp))
                        .border(1.dp, ThemeColors.BorderSubtle, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("🚩", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${viewModel.gameLogic.remainingFlags}",
                        color = ThemeColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Lives Indicator + Tombol Bantuan Nonton Iklan
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(ThemeColors.CardBg, RoundedCornerShape(20.dp))
                        .border(1.dp, ThemeColors.BorderSubtle, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Lives",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${viewModel.lives}",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // 🎁 TOMBOL + NYAWA DARURAT
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(ThemeColors.PrimaryAccent, CircleShape)
                            .clip(CircleShape)
                            .clickable {
                                SoundHelper.playClick()
                                activity?.let {
                                    rewardedAdManager.showAd(it) {
                                        viewModel.addLife()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Nyawa",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Gameplay Area Grid
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val cols = viewModel.currentDifficulty.cols
            val rows = viewModel.currentDifficulty.rows

            // Hitung ukuran ubin optimal agar muat sempurna di layar
            val tileSize = minOf(this.maxWidth / cols, this.maxHeight / rows)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                for (r in 0 until rows) {
                    Row(
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (c in 0 until cols) {
                            val ubin = viewModel.gameGrid.board[r][c]
                            Box(
                                modifier = Modifier
                                    .size(tileSize)
                                    .padding(1.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (ubin.isRevealed) {
                                            if (ubin.isDog) Color(0xFFEF4444).copy(alpha = 0.4f)
                                            else ThemeColors.BackgroundDark2
                                        } else {
                                            ThemeColors.SecondarySteelLight
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (ubin.isRevealed) ThemeColors.BorderSubtle else ThemeColors.CardBg.copy(
                                            alpha = 0.5f
                                        )
                                    )
                                    .pointerInput(r, c, ubin.isRevealed, ubin.isFlagged, viewModel.gameLogic.status) {
                                        detectTapGestures(
                                            onTap = {
                                                if (viewModel.gameLogic.status == GameStatus.PLAYING) {
                                                    // Hanya bisa buka jika ubin BELUM terbuka & BELUM di-flag
                                                    if (!ubin.isRevealed && !ubin.isFlagged) {

                                                        // 1. Eksekusi pembukaan ubin di ViewModel
                                                        viewModel.revealTile(r, c)

                                                        // 2. Cek status game terbaru dari ViewModel
                                                        when (viewModel.gameLogic.status) {
                                                            GameStatus.GAMEOVER -> {
                                                                SoundHelper.playExplosion()
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            }
                                                            GameStatus.WON -> {
                                                                SoundHelper.playWin()
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            }
                                                            else -> {
                                                                SoundHelper.playReveal()
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            onLongPress = {
                                                if (viewModel.gameLogic.status == GameStatus.PLAYING) {
                                                    // Flag hanya bisa dipasang/lepas pada ubin yang BELUM terbuka
                                                    if (!ubin.isRevealed) {
                                                        val changed = viewModel.toggleFlag(r, c)
                                                        if (changed) {
                                                            SoundHelper.playFlag()
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (ubin.isRevealed) {
                                    if (ubin.isDog) {
                                        Text("💥", fontSize = (tileSize.value * 0.5f).sp)
                                    } else if (ubin.barkVolume > 0) {
                                        val numberColor = when (ubin.barkVolume) {
                                            1 -> ThemeColors.Number1
                                            2 -> ThemeColors.Number2
                                            3 -> ThemeColors.Number3
                                            4 -> ThemeColors.Number4
                                            else -> ThemeColors.Number5OrMore
                                        }
                                        Text(
                                            text = "${ubin.barkVolume}",
                                            color = numberColor,
                                            fontWeight = FontWeight.Black,
                                            fontSize = (tileSize.value * 0.45f).sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                } else if (ubin.isFlagged) {
                                    Text("🚩", fontSize = (tileSize.value * 0.5f).sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Instructions Tip Footer in Game Screen
        Text(
            text = "💡 Tap to Reveal • Long-press to Flag 🚩",
            color = ThemeColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )
    }
}
@Composable
fun ModalManager(
    viewModel: MinesweeperViewModel,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    // 🎬 Inisialisasi RewardedAdManager khusus Modal Manager
    val rewardedAdManager = remember { RewardedAdManager(context) }

    // Auto-load iklan saat dialog/modal butuh iklan
    LaunchedEffect(viewModel.showOutOfLivesModal) {
        if (viewModel.showOutOfLivesModal) {
            rewardedAdManager.loadAd()
        }
    }

    // 1. Lose Modal with remaining lives
    if (viewModel.showLoseWithLivesModal) {
        Dialog(onDismissRequest = { viewModel.showLoseWithLivesModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ThemeColors.CardBg),
                border = BorderStroke(2.dp, Color(0xFFEF4444)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💔", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Ouch! Exploded!",
                        color = ThemeColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "You hit a mine, but you still have lives left! Continue on this challenge.",
                        color = ThemeColors.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .background(ThemeColors.SecondarySteel, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Remaining Lives: ", color = ThemeColors.TextSecondary, fontSize = 12.sp)
                        Text(
                            text = viewModel.lives.toString(),
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            SoundHelper.playClick()
                            viewModel.showLoseWithLivesModal = false
                            onPlayAgain()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.PrimaryAccent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("TRY AGAIN", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            SoundHelper.playClick()
                            viewModel.showLoseWithLivesModal = false
                            onBackToMenu()
                        }
                    ) {
                        Text("CHANGE LEVEL", color = ThemeColors.TextMuted)
                    }
                }
            }
        }
    }

    // 2. Out of lives modal (Integrasi Iklan AdMob)
    if (viewModel.showOutOfLivesModal) {
        Dialog(onDismissRequest = {}) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ThemeColors.CardBg),
                border = BorderStroke(2.dp, ThemeColors.BorderSubtle),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💔", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "OUT OF LIVES!",
                        color = ThemeColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Don't give up yet! Refill your stamina now to maintain your streak progress.",
                        color = ThemeColors.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // 🎬 TOMBOL NONTON IKLAN VIDEO (+5 LIVES)
                    Button(
                        onClick = {
                            SoundHelper.playClick()
                            activity?.let {
                                rewardedAdManager.showAd(it) {
                                    // Pemain dapat hadiah +5 Lives
                                    viewModel.refillLivesWatchVideo()
                                    viewModel.showOutOfLivesModal = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.PrimaryAccent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🎬 WATCH VIDEO (+5 LIVES)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 🎁 CLAIM FREE LIVE
                    TextButton(onClick = {
                        SoundHelper.playClick()
                        viewModel.claimFreeLive()
                        viewModel.showOutOfLivesModal = false
                    }) {
                        Text("🎁 CLAIM +1 FREE LIVE", color = ThemeColors.PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // 3. Win Modal
    if (viewModel.showWinModal) {
        Dialog(onDismissRequest = { viewModel.showWinModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ThemeColors.CardBg),
                border = BorderStroke(2.dp, Color(0xFF22C55E)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎉", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "YOU WIN!",
                        color = ThemeColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Outstanding sweeps! You cleared the board safely without hitting a single mine.",
                        color = ThemeColors.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .background(ThemeColors.SecondarySteel, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏱️ Clear Time: ", color = ThemeColors.TextSecondary, fontSize = 12.sp)
                        val min = viewModel.timeElapsed / 60
                        val sec = viewModel.timeElapsed % 60
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", min, sec),
                            color = ThemeColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            SoundHelper.playClick()
                            viewModel.showWinModal = false
                            onPlayAgain()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.PrimaryAccent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("PLAY AGAIN", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        SoundHelper.playClick()
                        viewModel.showWinModal = false
                        onBackToMenu()
                    }) {
                        Text("BACK TO MAIN MENU", color = ThemeColors.TextMuted)
                    }
                }
            }
        }
    }

    // 4. How To Play Modal
    if (viewModel.showHowToModal) {
        Dialog(onDismissRequest = { viewModel.showHowToModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ThemeColors.CardBg),
                border = BorderStroke(1.dp, ThemeColors.BorderSubtle),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ℹ️", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "How To Play",
                        color = ThemeColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "1. Tap any tile on the grid to reveal it.\n\n" +
                                "2. The numbers indicate how many mines are hidden in the adjacent 8 tiles.\n\n" +
                                "3. Long-press any unrevealed tile to place a flag 🚩 to mark suspected mines.\n\n" +
                                "4. Clear the board of all safe tiles to win! Keep an eye on your remaining Lives 💔.",
                        color = ThemeColors.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Left,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            SoundHelper.playClick()
                            viewModel.showHowToModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.PrimaryAccent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("UNDERSTOOD", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 5. Save Confirmation Modal
    if (viewModel.showSaveConfirmationModal) {
        Dialog(onDismissRequest = { viewModel.showSaveConfirmationModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ThemeColors.CardBg),
                border = BorderStroke(1.dp, ThemeColors.BorderSubtle),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💾", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Save Game Progress?",
                        color = ThemeColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "You have an ongoing game. Would you like to save your current board state and timer so you can resume later?",
                        color = ThemeColors.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Option 1: Save & Exit
                    Button(
                        onClick = {
                            SoundHelper.playClick()
                            viewModel.showSaveConfirmationModal = false
                            viewModel.saveGameState()
                            onBackToMenu()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.PrimaryAccent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SAVE & EXIT", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 2: Exit without Saving
                    Button(
                        onClick = {
                            SoundHelper.playClick()
                            viewModel.showSaveConfirmationModal = false
                            viewModel.clearSavedGame()
                            onBackToMenu()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("DISCARD & EXIT", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 3: Cancel
                    TextButton(onClick = {
                        SoundHelper.playClick()
                        viewModel.showSaveConfirmationModal = false
                    }) {
                        Text("CANCEL", color = ThemeColors.TextMuted, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
class FireworkParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    var alpha: Float = 1.0f,
    val decay: Float
)

class FireworkRocket(
    var x: Float,
    var y: Float,
    val targetY: Float,
    val vx: Float,
    val vy: Float,
    val color: Color
)

@Composable
fun FireworksEffect(
    modifier: Modifier = Modifier
) {
    val particles = remember { mutableStateListOf<FireworkParticle>() }
    val rockets = remember { mutableStateListOf<FireworkRocket>() }

    LaunchedEffect(Unit) {
        val colors = listOf(
            Color(0xFFFF3366), // Pink/Red
            Color(0xFFFFCC00), // Gold
            Color(0xFF33CC66), // Green
            Color(0xFF3399FF), // Blue
            Color(0xFF9933FF), // Purple
            Color(0xFFFF9933), // Orange
            Color(0xFFFF00CC)  // Neon pink
        )

        val startTime = System.currentTimeMillis()
        var lastTime = withFrameNanos { it }
        var nextLaunchTime = 0L
        var accumulatedTime = 0L

        while (true) {
            val now = withFrameNanos { it }
            val elapsedNanos = now - lastTime
            lastTime = now
            val elapsedMs = elapsedNanos / 1_000_000L

            val timeSinceStart = System.currentTimeMillis() - startTime
            
            // Periodically launch rockets (only for the first 5 seconds)
            if (timeSinceStart < 5000L) {
                accumulatedTime += elapsedMs
                if (accumulatedTime >= nextLaunchTime) {
                    accumulatedTime = 0
                    nextLaunchTime = 250 + (Math.random() * 400).toLong() // every 250-650ms

                    // Spawn a rocket from bottom
                    val startX = 0.15f + (Math.random() * 0.7f).toFloat()
                    val targetY = 0.1f + (Math.random() * 0.4f).toFloat() // top half of screen
                    val speedY = -12f - (Math.random() * 8f).toFloat() // launch velocity
                    val color = colors.random()

                    rockets.add(
                        FireworkRocket(
                            x = startX,
                            y = 1.0f, // relative bottom
                            targetY = targetY,
                            vx = (-1.5f + (Math.random() * 3.0f)).toFloat(), // slight wind
                            vy = speedY,
                            color = color
                        )
                    )
                }
            } else if (rockets.isEmpty() && particles.isEmpty()) {
                // Done animating
                break
            }

            // Update rockets
            val rIterator = rockets.iterator()
            while (rIterator.hasNext()) {
                val r = rIterator.next()
                val step = (elapsedMs / 16.67f).coerceIn(0.1f, 3.0f)
                
                // rocket move up
                r.y += (r.vy * 0.0015f) * step
                r.x += (r.vx * 0.0015f) * step

                // explode if reached top
                if (r.y <= r.targetY) {
                    // Spawn explosion particles
                    val count = 20 + (Math.random() * 20).toInt()
                    val burstColors = if (Math.random() < 0.3) {
                        colors
                    } else {
                        listOf(r.color)
                    }
                    repeat(count) {
                        val angle = Math.random() * kotlin.math.PI * 2
                        val speed = 1.5f + (Math.random() * 5.5f).toFloat()
                        val vx = (kotlin.math.cos(angle) * speed).toFloat()
                        val vy = (kotlin.math.sin(angle) * speed).toFloat()
                        particles.add(
                            FireworkParticle(
                                x = r.x,
                                y = r.y,
                                vx = vx * 0.0015f,
                                vy = vy * 0.0015f,
                                color = burstColors.random(),
                                size = 4f + (Math.random() * 5f).toFloat(),
                                alpha = 1.0f,
                                decay = 0.015f + (Math.random() * 0.015f).toFloat()
                            )
                        )
                    }
                    rIterator.remove()
                }
            }

            // Update particles
            val pIterator = particles.iterator()
            while (pIterator.hasNext()) {
                val p = pIterator.next()
                val step = (elapsedMs / 16.67f).coerceIn(0.1f, 3.0f)
                p.x += p.vx * step
                p.y += p.vy * step
                p.vy += 0.0001f * step // gravity pull
                p.alpha -= p.decay * step

                if (p.alpha <= 0f) {
                    pIterator.remove()
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        val w = size.width
        val h = size.height

        // Draw active rockets
        rockets.forEach { r ->
            drawCircle(
                color = r.color,
                radius = 6f,
                center = androidx.compose.ui.geometry.Offset(r.x * w, r.y * h)
            )
        }

        // Draw active particles
        particles.forEach { p ->
            drawCircle(
                color = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f)),
                radius = p.size,
                center = androidx.compose.ui.geometry.Offset(p.x * w, p.y * h)
            )
        }
    }
}

