package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream

@Composable
fun ImageCropperDialog(
    imageUri: Uri,
    onCropSuccess: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Load bitmap safely on launch
    LaunchedEffect(imageUri) {
        isLoading = true
        bitmap = loadSafeBitmap(context, imageUri, maxDim = 1200)
        isLoading = false
    }

    if (isLoading) {
        Dialog(onDismissRequest = onDismiss) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color(0xFF0F172A), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    } else if (bitmap == null) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "이미지를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    } else {
        val originalBitmap = bitmap!!
        val wImg = originalBitmap.width.toFloat()
        val hImg = originalBitmap.height.toFloat()

        // Size in Dp for the viewport
        val viewPortSizeDp = 280.dp
        val density = LocalDensity.current
        val viewPortSizePx = remember(density) {
            density.run { viewPortSizeDp.toPx() }
        }

        // States for pan, zoom, rotate
        var userScale by remember { mutableStateOf(1f) }
        var userOffset by remember { mutableStateOf(Offset.Zero) }
        var rotationDegrees by remember { mutableStateOf(0f) }

        // Initial base scale to FILL the viewport
        val baseScale = remember(wImg, hImg, viewPortSizePx) {
            viewPortSizePx / minOf(wImg, hImg)
        }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Deep Slate Theme
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .padding(vertical = 16.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "포스터 이미지 크롭",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White.copy(alpha = 0.7f))
                        }
                    }

                    Text(
                        text = "두 손가락 줌/이동 또는 하단 바를 조작하여\n정사각형 프레임 내 잘라낼 영역을 선택하세요.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 1:1 Viewing Port Frame with exact dimensions
                    Box(
                        modifier = Modifier
                            .size(viewPortSizeDp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(BorderStroke(2.dp, Color(0xFF6366F1)), RoundedCornerShape(12.dp)) // Indigo accent border
                            .clipToBounds()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    userScale = (userScale * zoom).coerceIn(1f, 5f)
                                    userOffset = userOffset + pan
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // The transformed image
                        androidx.compose.foundation.Image(
                            bitmap = originalBitmap.asImageBitmap(),
                            contentDescription = "자르기 대상 이미지",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = baseScale * userScale,
                                    scaleY = baseScale * userScale,
                                    translationX = userOffset.x,
                                    translationY = userOffset.y,
                                    rotationZ = rotationDegrees
                                )
                        )

                        // 3x3 Grid Overlay line decorators for high visual quality
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)))
                        ) {
                            // Grid columns split
                            Row(modifier = Modifier.fillMaxSize()) {
                                Spacer(modifier = Modifier.weight(1f).fillMaxHeight().border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))))
                                Spacer(modifier = Modifier.weight(1f).fillMaxHeight().border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))))
                                Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                            }
                            // Grid rows split
                            Column(modifier = Modifier.fillMaxSize()) {
                                Spacer(modifier = Modifier.weight(1f).fillMaxWidth().border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))))
                                Spacer(modifier = Modifier.weight(1f).fillMaxWidth().border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))))
                                Spacer(modifier = Modifier.weight(1f).fillMaxWidth())
                            }
                        }
                    }

                    // Zoom Bar Slider for precise adjustment
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("크기 배율", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Text("${(userScale * 100).toInt()}%", color = Color(0xFF6366F1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = userScale,
                            onValueChange = { userScale = it },
                            valueRange = 1f..5f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color(0xFF6366F1),
                                activeTickColor = Color(0xFF6366F1),
                                thumbColor = Color(0xFF818CF8)
                            )
                        )
                    }

                    // Adjustment tools
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "90도 회전", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("90° 회전", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Reset button
                        OutlinedButton(
                            onClick = {
                                userScale = 1f
                                userOffset = Offset.Zero
                                rotationDegrees = 0f
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
                        ) {
                            Text("초기화", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                    // Finalize Crop Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("취소", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                try {
                                    val cropped = cropBitmap(
                                        originalBitmap,
                                        wImg,
                                        hImg,
                                        baseScale,
                                        userScale,
                                        userOffset,
                                        rotationDegrees,
                                        viewPortSizePx
                                    )
                                    val croppedFile = saveCroppedBitmap(context, cropped)
                                    if (croppedFile != null) {
                                        onCropSuccess(croppedFile.absolutePath)
                                    } else {
                                        Toast.makeText(context, "이미지 자르기 실패", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "자르기 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.5f).height(48.dp)
                        ) {
                            Text("자르기 완료", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Loads a bitmap safely from a Uri by downsampling it based on a maximum size.
 */
private fun loadSafeBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
    try {
        val inputStream1 = context.contentResolver.openInputStream(uri) ?: return null
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream1, null, options)
        inputStream1.close()

        var sampleSize = 1
        if (options.outWidth > maxDim || options.outHeight > maxDim) {
            val halfWidth = options.outWidth / 2
            val halfHeight = options.outHeight / 2
            while ((halfWidth / sampleSize) >= maxDim && (halfHeight / sampleSize) >= maxDim) {
                sampleSize *= 2
            }
        }

        val inputStream2 = context.contentResolver.openInputStream(uri) ?: return null
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
        inputStream2.close()
        return bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

/**
 * Calculates and draws the transform-scaled square crop of original image.
 * Uses high-resolution dimensions for clean, un-pixelated results (800x800).
 */
private fun cropBitmap(
    bitmap: Bitmap,
    wImg: Float,
    hImg: Float,
    baseScale: Float,
    userScale: Float,
    userOffset: Offset,
    rotationDegrees: Float,
    viewPortSizePx: Float
): Bitmap {
    val targetSize = 800f
    val croppedBitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(croppedBitmap)
    canvas.drawColor(android.graphics.Color.WHITE) // Background matches white poster card

    val k = targetSize / viewPortSizePx
    val matrix = Matrix()

    // 1. Move original pivot to coordinates origin (0,0) so scaling and rotation work relative to image center
    matrix.postTranslate(-wImg / 2f, -hImg / 2f)

    // 2. Apply clockwise rotations
    matrix.postRotate(rotationDegrees)

    // 3. Scale up to match viewport dimension mappings
    val finalScale = baseScale * userScale * k
    matrix.postScale(finalScale, finalScale)

    // 4. Translate back to target canvas center plus user visual offsets
    val finalX = 400f + (userOffset.x * k)
    val finalY = 400f + (userOffset.y * k)
    matrix.postTranslate(finalX, finalY)

    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    canvas.drawBitmap(bitmap, matrix, paint)
    return croppedBitmap
}

/**
 * Saves cropped high-res bitmap onto persistent secure files directory.
 */
private fun saveCroppedBitmap(context: Context, cropped: Bitmap): File? {
    return try {
        val imgDir = File(context.filesDir, "images")
        if (!imgDir.exists()) imgDir.mkdirs()
        
        val imgFile = File(imgDir, "crop_${System.currentTimeMillis()}.png")
        FileOutputStream(imgFile).use { fos ->
            cropped.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        imgFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
