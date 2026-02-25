package com.paici.app.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.paici.app.data.SubjectSegmentationProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── State machine ─────────────────────────────────────────────────────────────

private sealed class CamState {
    object Viewfinder : CamState()
    object Processing : CamState()
    data class Result(
        val capturedBitmap: Bitmap,
        val displayBitmap: Bitmap?,   // null → fallback（显示原图）
        val english: String,
        val chinese: String,
        val phonetic: String,         // e.g. "/ˈæpəl/"，查不到时为 "/ — /"
    ) : CamState()
}

/**
 * 拍照识词页面。
 *
 * @param onBack      点击返回
 * @param onWordAdded 用户点击"加入词本"时回调，传入英文和中文
 */
@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onWordAdded: (english: String, chinese: String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = context as LifecycleOwner
    val scope = rememberCoroutineScope()

    // ── 权限 ────────────────────────────────────────────────────────────────

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ── TTS ──────────────────────────────────────────────────────────────────

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        TextToSpeech(context) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
        }
    }
    DisposableEffect(Unit) {
        onDispose { tts.shutdown() }
    }

    // ── CameraX 对象 ─────────────────────────────────────────────────────────

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val labeler = remember {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.65f)
                .build()
        )
    }
    val segmentProcessor = remember { SubjectSegmentationProcessor(context) }

    DisposableEffect(Unit) {
        onDispose {
            labeler.close()
            segmentProcessor.close()
        }
    }

    // ── 页面状态 ──────────────────────────────────────────────────────────────

    var camState by remember { mutableStateOf<CamState>(CamState.Viewfinder) }

    // ── 绑定相机 ──────────────────────────────────────────────────────────────

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect
        val cameraProvider = withContext(Dispatchers.IO) {
            ProcessCameraProvider.getInstance(context).get()
        }
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
            )
        } catch (_: Exception) { /* 相机不可用时静默处理 */ }
    }

    // ── 拍照 + 识别 ───────────────────────────────────────────────────────────

    fun capture() {
        if (camState !is CamState.Viewfinder) return
        camState = CamState.Processing

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(proxy: ImageProxy) {
                    val bitmap = proxy.toBitmap()
                    val rotation = proxy.imageInfo.rotationDegrees
                    proxy.close()

                    val inputImage = InputImage.fromBitmap(bitmap, rotation)
                    labeler.process(inputImage)
                        .addOnSuccessListener { labels ->
                            val top = labels.firstOrNull()
                            if (top == null) {
                                camState = CamState.Viewfinder
                                return@addOnSuccessListener
                            }
                            val english = top.text.lowercase()
                            val chinese  = labelToChinese[english]  ?: "—"
                            val phonetic = labelToPhonetic[english] ?: "/ — /"
                            scope.launch {
                                val displayBitmap = segmentProcessor.process(bitmap)
                                camState = CamState.Result(
                                    capturedBitmap = bitmap,
                                    displayBitmap  = displayBitmap,
                                    english        = english,
                                    chinese        = chinese,
                                    phonetic       = phonetic,
                                )
                            }
                        }
                        .addOnFailureListener {
                            camState = CamState.Viewfinder
                        }
                }

                override fun onError(exception: ImageCaptureException) {
                    camState = CamState.Viewfinder
                }
            }
        )
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    when (val state = camState) {

        // ── Viewfinder ───────────────────────────────────────────────────────
        is CamState.Viewfinder -> {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "需要相机权限才能使用此功能",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .navigationBarsPadding()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = { capture() },
                        enabled = hasCameraPermission,
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "拍照",
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
        }

        // ── Processing ───────────────────────────────────────────────────────
        is CamState.Processing -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        color = Color.White,
                        strokeWidth = 4.dp,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "识别中…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                    )
                }
            }
        }

        // ── Result ───────────────────────────────────────────────────────────
        is CamState.Result -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0D0D1A), Color(0xFF1A1A2E))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    // 顶部：弱返回按钮
                    IconButton(
                        onClick = { camState = CamState.Viewfinder },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "重新预览",
                            tint = Color.White.copy(alpha = 0.5f),
                        )
                    }

                    // 中部：图像展示
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.displayBitmap != null) {
                            Image(
                                bitmap = state.displayBitmap.asImageBitmap(),
                                contentDescription = state.english,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                            )
                        } else {
                            Image(
                                bitmap = state.capturedBitmap.asImageBitmap(),
                                contentDescription = state.english,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(24.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }

                    // 文字信息区
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = state.english,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                        )
                        IconButton(
                            onClick = {
                                tts.speak(state.english, TextToSpeech.QUEUE_FLUSH, null, "word_tts")
                            },
                            enabled = ttsReady,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "发音",
                                tint = if (ttsReady) Color.White.copy(alpha = 0.7f)
                                       else Color.White.copy(alpha = 0.3f),
                            )
                        }
                        Text(
                            text = state.phonetic,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.chinese,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    // 底部三按钮决策区
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { camState = CamState.Viewfinder },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("重拍", color = Color.White.copy(alpha = 0.8f))
                        }

                        Button(
                            onClick = {
                                onWordAdded(state.english, state.chinese)
                                onBack()
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(56.dp)
                                .padding(horizontal = 8.dp),
                        ) {
                            Text("✓ 加入词本")
                        }

                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("放弃", color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}
