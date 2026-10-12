package com.example.aqisensor.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.aqisensor.R
import kotlinx.coroutines.delay

/**
 * Full-screen splash screen that plays the Lottie animation once,
 * then signals [onFinished] so the caller can navigate away.
 *
 * The animation plays exactly one iteration. After completion,
 * a brief fade-out transition occurs before invoking [onFinished].
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.splash_animation)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,          // play once, then stop
        isPlaying = true,
    )

    // Track when the animation has fully completed one cycle
    var animationEnded by remember { mutableStateOf(false) }
    LaunchedEffect(progress) {
        if (progress == 1f) {
            animationEnded = true
        }
    }

    // After animation ends, hold for a beat then invoke the callback
    LaunchedEffect(animationEnded) {
        if (animationEnded) {
            delay(350)           // small pause so the last frame is visible
            onFinished()
        }
    }

    // ── Splash UI ──────────────────────────────────────────────────────────
    AnimatedVisibility(
        visible = !animationEnded,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(400)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(280.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "AQI Sensor",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Breathe Better",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
        }
    }
}
