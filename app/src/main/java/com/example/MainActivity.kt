package com.example

import android.os.Bundle
import android.net.Uri
import com.example.ui.ImageCropperDialog
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import java.util.Locale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModelProvider
import com.example.data.PosterDatabase
import com.example.data.PosterRepository
import com.example.data.SavedPoster
import com.example.ui.PosterViewModel
import com.example.ui.PosterViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.EmojiDatabase
import com.example.utils.PosterGenerator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room Database & Repository
        val database = PosterDatabase.getDatabase(this)
        val repository = PosterRepository(database.savedPosterDao())
        val viewModel: PosterViewModel by lazy {
            ViewModelProvider(this, PosterViewModelFactory(repository, applicationContext))[PosterViewModel::class.java]
        }

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFFF3F4F9)
                ) { innerPadding ->
                    PosterMakerScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun CenterPlaced(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    ratio: Float,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .offset(
                x = ((cx - w / 2f) * ratio).dp,
                y = ((cy - h / 2f) * ratio).dp
            )
            .size((w * ratio).dp, (h * ratio).dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PosterMakerScreen(
    viewModel: PosterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Observe State from ViewModel
    val textA by viewModel.textA.collectAsState()
    val emojiA by viewModel.emojiA.collectAsState()
    val textB by viewModel.textB.collectAsState()
    val emojiB by viewModel.emojiB.collectAsState()
    val fontSizeSp by viewModel.fontSizeSp.collectAsState()
    val textColorHex by viewModel.textColorHex.collectAsState()
    val isVerticalLayout by viewModel.isVerticalLayout.collectAsState()
    val footerText by viewModel.footerText.collectAsState()
    val showFooter by viewModel.showFooter.collectAsState()
    val borderStyle by viewModel.borderStyle.collectAsState()
    val activeEmojiSelector by viewModel.activeEmojiSelector.collectAsState()
    val savedPosters by viewModel.savedPosters.collectAsState()

    val operatorSizeSp by viewModel.operatorSize.collectAsState()
    val operatorWeight by viewModel.operatorWeight.collectAsState()
    val mysterySizeSp by viewModel.mysterySize.collectAsState()
    val mysteryWeight by viewModel.mysteryWeight.collectAsState()
    val customFontPath by viewModel.customFontPath.collectAsState()
    val customFontName by viewModel.customFontName.collectAsState()

    val imagePathA by viewModel.imagePathA.collectAsState()
    val imagePathB by viewModel.imagePathB.collectAsState()
    val imageSizeA by viewModel.imageSizeA.collectAsState()
    val imageSizeB by viewModel.imageSizeB.collectAsState()
    val featherA by viewModel.featherA.collectAsState()
    val featherB by viewModel.featherB.collectAsState()

    var imageUriToCrop by remember { mutableStateOf<Uri?>(null) }
    var cropTarget by remember { mutableStateOf<String?>(null) } // "A" or "B"

    val imagePickerALauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            imageUriToCrop = it
            cropTarget = "A"
        }
    }

    val imagePickerBLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            imageUriToCrop = it
            cropTarget = "B"
        }
    }

    val activeCropUri = imageUriToCrop
    val activeCropTarget = cropTarget
    if (activeCropUri != null && activeCropTarget != null) {
        ImageCropperDialog(
            imageUri = activeCropUri,
            onCropSuccess = { croppedPath ->
                if (activeCropTarget == "A") {
                    val imgDir = File(context.filesDir, "images")
                    if (imgDir.exists()) {
                        imgDir.listFiles()?.filter { it.name.startsWith("img_a_") || (it.name.startsWith("crop_") && it.absolutePath != croppedPath) }?.forEach { it.delete() }
                    }
                    viewModel.setImagePathA(croppedPath)
                    Toast.makeText(context, "첫 번째 이미지 업로드 완료!", Toast.LENGTH_SHORT).show()
                } else {
                    val imgDir = File(context.filesDir, "images")
                    if (imgDir.exists()) {
                        imgDir.listFiles()?.filter { it.name.startsWith("img_b_") || (it.name.startsWith("crop_") && it.absolutePath != croppedPath) }?.forEach { it.delete() }
                    }
                    viewModel.setImagePathB(croppedPath)
                    Toast.makeText(context, "두 번째 이미지 업로드 완료!", Toast.LENGTH_SHORT).show()
                }
                imageUriToCrop = null
                cropTarget = null
            },
            onDismiss = {
                imageUriToCrop = null
                cropTarget = null
            }
        )
    }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val fontDir = File(context.filesDir, "fonts")
                    if (!fontDir.exists()) fontDir.mkdirs()
                    fontDir.listFiles()?.forEach { it.delete() } // Clean old fonts

                    var displayName = "custom_font.ttf"
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex >= 0) {
                            displayName = cursor.getString(nameIndex)
                        }
                    }

                    val fontFile = File(fontDir, "custom_font_${System.currentTimeMillis()}.ttf")
                    fontFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }

                    viewModel.setCustomFont(fontFile.absolutePath, displayName)
                    Toast.makeText(context, "폰트 적용 완료: $displayName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "폰트 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val customFontFamily = remember(customFontPath) {
        if (customFontPath != null && java.io.File(customFontPath).exists()) {
            try {
                androidx.compose.ui.text.font.FontFamily(android.graphics.Typeface.createFromFile(customFontPath))
            } catch (e: Throwable) {
                androidx.compose.ui.text.font.FontFamily.SansSerif
            }
        } else {
            androidx.compose.ui.text.font.FontFamily.SansSerif
        }
    }

    // Local presets for styling
    val colorPresets = remember {
        listOf(
            "#000000" to "기본 블랙",
            "#1B365D" to "시크 네이비",
            "#C2185B" to "크림슨 로즈",
            "#1B5E20" to "에메랄드 그린",
            "#4A148C" to "로얄 퍼플",
            "#D35400" to "선셋 오렌지"
        )
    }

    val activeColor = remember(textColorHex) {
        try {
            Color(android.graphics.Color.parseColor(textColorHex))
        } catch (e: Exception) {
            Color.Black
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎨 이모티콘 세로 이미지 메이커",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "글자를 치면 이모티콘이 쏙! 나만의 멋진 포스터 조합 레시피",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Live Poster Design Area (9:16 Preview Canvas Card)
        Card(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .aspectRatio(9f / 16f)
                .shadow(20.dp, RoundedCornerShape(32.dp))
                .border(6.dp, Color.White, RoundedCornerShape(32.dp))
                .testTag("poster_preview_canvas"),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(32.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                val cardWidthDp = maxWidth
                val ratio = cardWidthDp.value / 1080f
                val scale = fontSizeSp / 40f

                // Border Styling matching PosterGenerator exactly in proportional inset and stroke
                val borderInsetDp = (40f * ratio).dp
                val doubleBorderInnerInsetDp = (52f * ratio).dp
                val thinStrokeDp = (6f * ratio).dp
                val boldStrokeDp = (16f * ratio).dp
                val borderCornerRadiusDp = (16f * ratio).dp

                when (borderStyle) {
                    "thin" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(borderInsetDp)
                                .border(thinStrokeDp, activeColor, RoundedCornerShape(borderCornerRadiusDp))
                        )
                    }
                    "bold" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(borderInsetDp)
                                .border(boldStrokeDp, activeColor, RoundedCornerShape(borderCornerRadiusDp))
                        )
                    }
                    "double" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(borderInsetDp)
                                .border(thinStrokeDp, activeColor, RoundedCornerShape(borderCornerRadiusDp))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(doubleBorderInnerInsetDp)
                                .border(
                                    thinStrokeDp,
                                    activeColor,
                                    RoundedCornerShape(((16f - 12f).coerceAtLeast(4f) * ratio).dp)
                                )
                        )
                    }
                }

                // Load and cache original bitmaps to avoid heavy decodes in recompositions
                var decodedBitmapA by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                DisposableEffect(imagePathA) {
                    if (imagePathA != null) {
                        try {
                            decodedBitmapA = android.graphics.BitmapFactory.decodeFile(imagePathA)
                        } catch (e: Throwable) {
                            decodedBitmapA = null
                        }
                    } else {
                        decodedBitmapA = null
                    }
                    onDispose {
                        decodedBitmapA?.recycle()
                        decodedBitmapA = null
                    }
                }

                var decodedBitmapB by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                DisposableEffect(imagePathB) {
                    if (imagePathB != null) {
                        try {
                            decodedBitmapB = android.graphics.BitmapFactory.decodeFile(imagePathB)
                        } catch (e: Throwable) {
                            decodedBitmapB = null
                        }
                    } else {
                        decodedBitmapB = null
                    }
                    onDispose {
                        decodedBitmapB?.recycle()
                        decodedBitmapB = null
                    }
                }

                // Load feathered bitmaps for rendering
                val previewBitmapA = remember(decodedBitmapA, imageSizeA, featherA, scale) {
                    val original = decodedBitmapA
                    if (original != null) {
                        val targetPx = (imageSizeA * scale * 2f).toInt().coerceAtLeast(1)
                        PosterGenerator.createFeatheredBitmap(original, targetPx, featherA)?.asImageBitmap()
                    } else null
                }

                val previewBitmapB = remember(decodedBitmapB, imageSizeB, featherB, scale) {
                    val original = decodedBitmapB
                    if (original != null) {
                        val targetPx = (imageSizeB * scale * 2f).toInt().coerceAtLeast(1)
                        PosterGenerator.createFeatheredBitmap(original, targetPx, featherB)?.asImageBitmap()
                    } else null
                }

                val opWeight = when (operatorWeight) {
                    "light" -> FontWeight.Light
                    "bold" -> FontWeight.Bold
                    else -> FontWeight.Normal
                }

                val qWeight = when (mysteryWeight) {
                    "bold" -> FontWeight.Bold
                    "black" -> FontWeight.Black
                    else -> FontWeight.Normal
                }

                if (isVerticalLayout) {
                    // Vertical exact coordinates
                    val heightA = if (imagePathA != null) imageSizeA * scale else 160f * scale
                    val heightB = if (imagePathB != null) imageSizeB * scale else 160f * scale
                    val labelHeight = 72f * scale
                    val spacing = 12f * scale
                    val opSpacing = 36f * scale
                    val opHeight = operatorSizeSp * 3.5f
                    val boxHeight = mysterySizeSp * 3.5f * 2.1f // vertical layout multiplier 2.1
                    
                    val totalStackHeight = (
                        heightA + spacing + labelHeight +
                        opSpacing + opHeight + opSpacing +
                        heightB + spacing + labelHeight +
                        opSpacing + opHeight + opSpacing +
                        boxHeight
                    )
                    
                    val currentY = (1920f - totalStackHeight) / 2f
                    val centerX = 540f

                    // 1. Element A
                    val elementACY = currentY + (heightA / 2f)
                    CenterPlaced(cx = centerX, cy = elementACY, w = if (imagePathA != null) imageSizeA * scale else 160f * scale, h = heightA, ratio = ratio) {
                        if (previewBitmapA != null) {
                            Image(
                                bitmap = previewBitmapA,
                                contentDescription = "Custom Image A",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = emojiA,
                                fontSize = (160f * scale * ratio).sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    val textA_CY = currentY + heightA + spacing + (labelHeight / 2f)
                    CenterPlaced(cx = centerX, cy = textA_CY, w = 1000f, h = labelHeight, ratio = ratio) {
                        Text(
                            text = textA.uppercase(Locale.getDefault()),
                            fontSize = (72f * scale * ratio).sp,
                            fontWeight = FontWeight.Bold,
                            color = activeColor,
                            letterSpacing = (-0.5).sp,
                            textAlign = TextAlign.Center,
                            fontFamily = customFontFamily
                        )
                    }

                    // 2. Plus Operator
                    val plus_CY = currentY + heightA + spacing + labelHeight + opSpacing + (opHeight / 2f)
                    CenterPlaced(cx = centerX, cy = plus_CY, w = 300f, h = opHeight, ratio = ratio) {
                        Text(
                            text = "+",
                            fontSize = (operatorSizeSp * 3.5f * ratio).sp,
                            fontWeight = opWeight,
                            color = Color(0xFF94A3B8), // slate-400
                            fontFamily = customFontFamily,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 3. Element B
                    val elementBCY = currentY + heightA + spacing + labelHeight + opSpacing + opHeight + opSpacing + (heightB / 2f)
                    CenterPlaced(cx = centerX, cy = elementBCY, w = if (imagePathB != null) imageSizeB * scale else 160f * scale, h = heightB, ratio = ratio) {
                        if (previewBitmapB != null) {
                            Image(
                                bitmap = previewBitmapB,
                                contentDescription = "Custom Image B",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = emojiB,
                                fontSize = (160f * scale * ratio).sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    val textB_CY = currentY + heightA + spacing + labelHeight + opSpacing + opHeight + opSpacing + heightB + spacing + (labelHeight / 2f)
                    CenterPlaced(cx = centerX, cy = textB_CY, w = 1000f, h = labelHeight, ratio = ratio) {
                        Text(
                            text = textB.uppercase(Locale.getDefault()),
                            fontSize = (72f * scale * ratio).sp,
                            fontWeight = FontWeight.Bold,
                            color = activeColor,
                            letterSpacing = (-0.5).sp,
                            textAlign = TextAlign.Center,
                            fontFamily = customFontFamily
                        )
                    }

                    // 4. Equal Operator
                    val equal_CY = currentY + heightA + spacing + labelHeight + opSpacing + opHeight + opSpacing + heightB + spacing + labelHeight + opSpacing + (opHeight / 2f)
                    CenterPlaced(cx = centerX, cy = equal_CY, w = 300f, h = opHeight, ratio = ratio) {
                        Text(
                            text = "=",
                            fontSize = (operatorSizeSp * 3.5f * ratio).sp,
                            fontWeight = opWeight,
                            color = Color(0xFF94A3B8),
                            fontFamily = customFontFamily,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 5. Mystery Box
                    val box_CY = currentY + heightA + spacing + labelHeight + opSpacing + opHeight + opSpacing + heightB + spacing + labelHeight + opSpacing + opHeight + opSpacing + (boxHeight / 2f)
                    val boxCornerRadius = boxHeight * 0.18f
                    CenterPlaced(cx = centerX, cy = box_CY, w = boxHeight, h = boxHeight, ratio = ratio) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape((boxCornerRadius * ratio).dp))
                                .background(Color(0xFFf8fafc))
                                .drawBehind {
                                    val strokeWidth = (6f * scale * ratio).dp.toPx()
                                    val dashWidth = (24f * scale * ratio).dp.toPx()
                                    val dashGap = (16f * scale * ratio).dp.toPx()
                                    val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                        floatArrayOf(dashWidth, dashGap),
                                        0f
                                    )
                                    drawRoundRect(
                                        color = Color(0xFFcbd5e1),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = strokeWidth,
                                            pathEffect = pathEffect
                                        ),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius((boxCornerRadius * ratio).dp.toPx())
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "?",
                                fontSize = (mysterySizeSp * 3.5f * ratio).sp,
                                fontWeight = qWeight,
                                color = Color(0xFF94A3B8),
                                fontFamily = customFontFamily,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Horizontal Layout exact coordinates matching PosterGenerator
                    val verticalCenterY = 960f
                    val xA = 180f
                    val xPlus = 370f
                    val xB = 540f
                    val xEqual = 710f
                    val xMystery = 900f

                    val textYDelta = 45f * scale
                    val textYDown = 65f * scale

                    // 1. Element A
                    val imgSizeA = imageSizeA * scale
                    val hEmojiSize = 140f * scale
                    val hTextSize = 54f * scale

                    CenterPlaced(cx = xA, cy = verticalCenterY - textYDelta, w = if (imagePathA != null) imgSizeA else hEmojiSize, h = if (imagePathA != null) imgSizeA else hEmojiSize, ratio = ratio) {
                        if (previewBitmapA != null) {
                            Image(
                                bitmap = previewBitmapA,
                                contentDescription = "Custom Image A",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = emojiA,
                                fontSize = (hEmojiSize * ratio).sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    CenterPlaced(cx = xA, cy = verticalCenterY + textYDown, w = 320f, h = hTextSize, ratio = ratio) {
                        Text(
                            text = textA.uppercase(Locale.getDefault()),
                            fontSize = (hTextSize * ratio).sp,
                            fontWeight = FontWeight.Bold,
                            color = activeColor,
                            letterSpacing = (-0.5).sp,
                            textAlign = TextAlign.Center,
                            fontFamily = customFontFamily
                        )
                    }

                    // 2. Plus Operator
                    CenterPlaced(cx = xPlus, cy = verticalCenterY, w = 100f, h = operatorSizeSp * 3.5f, ratio = ratio) {
                        Text(
                            text = "+",
                            fontSize = (operatorSizeSp * 3.5f * ratio).sp,
                            fontWeight = opWeight,
                            color = Color(0xFF94A3B8),
                            fontFamily = customFontFamily,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 3. Element B
                    val imgSizeB = imageSizeB * scale
                    CenterPlaced(cx = xB, cy = verticalCenterY - textYDelta, w = if (imagePathB != null) imgSizeB else hEmojiSize, h = if (imagePathB != null) imgSizeB else hEmojiSize, ratio = ratio) {
                        if (previewBitmapB != null) {
                            Image(
                                bitmap = previewBitmapB,
                                contentDescription = "Custom Image B",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = emojiB,
                                fontSize = (hEmojiSize * ratio).sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    CenterPlaced(cx = xB, cy = verticalCenterY + textYDown, w = 320f, h = hTextSize, ratio = ratio) {
                        Text(
                            text = textB.uppercase(Locale.getDefault()),
                            fontSize = (hTextSize * ratio).sp,
                            fontWeight = FontWeight.Bold,
                            color = activeColor,
                            letterSpacing = (-0.5).sp,
                            textAlign = TextAlign.Center,
                            fontFamily = customFontFamily
                        )
                    }

                    // 4. Equal Operator
                    CenterPlaced(cx = xEqual, cy = verticalCenterY, w = 100f, h = operatorSizeSp * 3.5f, ratio = ratio) {
                        Text(
                            text = "=",
                            fontSize = (operatorSizeSp * 3.5f * ratio).sp,
                            fontWeight = opWeight,
                            color = Color(0xFF94A3B8),
                            fontFamily = customFontFamily,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 5. Mystery Box
                    val boxWidthHeight = mysterySizeSp * 3.5f * 2.0f
                    val boxCornerRadius = boxWidthHeight * 0.18f
                    CenterPlaced(cx = xMystery, cy = verticalCenterY, w = boxWidthHeight, h = boxWidthHeight, ratio = ratio) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape((boxCornerRadius * ratio).dp))
                                .background(Color(0xFFf8fafc))
                                .drawBehind {
                                    val strokeWidth = (6f * scale * ratio).dp.toPx()
                                    val dashWidth = (24f * scale * ratio).dp.toPx()
                                    val dashGap = (16f * scale * ratio).dp.toPx()
                                    val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                        floatArrayOf(dashWidth, dashGap),
                                        0f
                                    )
                                    drawRoundRect(
                                        color = Color(0xFFcbd5e1),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = strokeWidth,
                                            pathEffect = pathEffect
                                        ),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius((boxCornerRadius * ratio).dp.toPx())
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "?",
                                fontSize = (mysterySizeSp * 3.5f * ratio).sp,
                                fontWeight = qWeight,
                                color = Color(0xFF94A3B8),
                                fontFamily = customFontFamily,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Optional Footer text matching PosterGenerator exactly
                if (showFooter && footerText.isNotEmpty()) {
                    Text(
                        text = footerText.uppercase(Locale.getDefault()),
                        color = activeColor.copy(alpha = 0.58f),
                        fontSize = (32f * ratio).sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = (100f * ratio).dp),
                        textAlign = TextAlign.Center,
                        fontFamily = customFontFamily
                    )
                }
            }
        }

        // Action Buttons: Save & Share (Touch targets strictly >= 48dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    viewModel.saveToGallery(context) { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("save_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = "저장")
                Spacer(Modifier.width(8.dp))
                Text("갤러리에 저장", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    viewModel.sharePoster(context) { message ->
                        if (!message.contains("성공")) {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("share_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Share, contentDescription = "공유")
                Spacer(Modifier.width(8.dp))
                Text("포스터 공유", fontWeight = FontWeight.Bold)
            }
        }

        // Customization Panel Accordion/Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "⚙️ 포스터 레시피 커스텀 설정",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Inputs Row
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Element A Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Emoji visual with manual selector trigger
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .clickable { viewModel.openEmojiSelector("A") }
                                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emojiA, fontSize = 24.sp)
                        }

                        OutlinedTextField(
                            value = textA,
                            onValueChange = { viewModel.onTextAChanged(it) },
                            label = { Text("첫 번째 단어") },
                            placeholder = { Text("예: 사과, 하트, 개구리") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_a"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                    }

                    // Element B Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Emoji visual with manual selector trigger
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .clickable { viewModel.openEmojiSelector("B") }
                                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emojiB, fontSize = 24.sp)
                        }

                        OutlinedTextField(
                            value = textB,
                            onValueChange = { viewModel.onTextBChanged(it) },
                            label = { Text("두 번째 단어") },
                            placeholder = { Text("예: 바나나, 불, 자동차") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_b"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )
                    }

                    Text(
                        text = "💡 이모티콘 아이콘을 터치해 직접 이모티콘을 바꿀 수도 있습니다!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                // 1.5. Custom Image Upload and Feathering Settings
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "🖼️ 이미지 업로드 및 스타일링 설정",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Card/Box for Component A Custom Image
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "첫 번째 요소 (A) 이미지",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (imagePathA != null) {
                                    IconButton(
                                        onClick = { viewModel.setImagePathA(null) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "이모티콘으로 복원",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { imagePickerALauncher.launch(arrayOf("image/*")) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Upload", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (imagePathA == null) "이미지 업로드" else "이미지 변경", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                if (imagePathA != null) {
                                    Text(
                                        text = "설정됨 ✔️",
                                        fontSize = 11.sp,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "이모티콘 사용 중",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            if (imagePathA != null) {
                                // Image Size A Slider
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("크기 설정", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${imageSizeA.toInt()} px", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(
                                        value = imageSizeA,
                                        onValueChange = { viewModel.setImageSizeA(it) },
                                        valueRange = 60f..250f,
                                        modifier = Modifier.height(24.dp)
                                    )
                                }

                                // Feather A Slider
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("페더 (가장자리 흰색 페이드)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${(featherA * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(
                                        value = featherA,
                                        onValueChange = { viewModel.setFeatherA(it) },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Card/Box for Component B Custom Image
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "두 번째 요소 (B) 이미지",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (imagePathB != null) {
                                    IconButton(
                                        onClick = { viewModel.setImagePathB(null) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "이모티콘으로 복원",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { imagePickerBLauncher.launch(arrayOf("image/*")) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Upload", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (imagePathB == null) "이미지 업로드" else "이미지 변경", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                if (imagePathB != null) {
                                    Text(
                                        text = "설정됨 ✔️",
                                        fontSize = 11.sp,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "이모티콘 사용 중",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            if (imagePathB != null) {
                                // Image Size B Slider
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("크기 설정", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${imageSizeB.toInt()} px", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(
                                        value = imageSizeB,
                                        onValueChange = { viewModel.setImageSizeB(it) },
                                        valueRange = 60f..250f,
                                        modifier = Modifier.height(24.dp)
                                    )
                                }

                                // Feather B Slider
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("페더 (가장자리 흰색 페이드)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${(featherB * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(
                                        value = featherB,
                                        onValueChange = { viewModel.setFeatherB(it) },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                // 2. Layout, Font Sizes, & Customizing Preset Ink Colors
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Layout Direction Select
                    Text(
                        text = "📐 포스터 정렬 방식",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = !isVerticalLayout,
                            onClick = { viewModel.setVerticalLayout(false) },
                            label = { Text("가로 한 줄 (A + B = ?)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isVerticalLayout,
                            onClick = { viewModel.setVerticalLayout(true) },
                            label = { Text("세로 차곡차곡") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔍 글자 크기 조절",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${fontSizeSp.toInt()} SP",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = fontSizeSp,
                        onValueChange = { viewModel.setFontSize(it) },
                        valueRange = 24f..72f,
                        steps = 8
                    )
                }

                // Color presets row
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🎨 포스터 잉크 색상",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        colorPresets.forEach { (hex, name) ->
                            val isSelected = textColorHex.lowercase() == hex.lowercase()
                            val colorValue = Color(android.graphics.Color.parseColor(hex))

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { viewModel.setTextColorHex(hex) }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(colorValue)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = name,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Border preset options
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🖼️ 포스터 테두리 데코",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "none" to "없음",
                            "thin" to "얇게",
                            "bold" to "굵게",
                            "double" to "두 줄"
                        ).forEach { (style, label) ->
                            val isSelected = borderStyle == style
                            InputChip(
                                selected = isSelected,
                                onClick = { viewModel.setBorderStyle(style) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                // 사용자 지정 폰트 설정 섹션
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🔤 사용자 지정 폰트 적용",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                fontPickerLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "폰트 불러오기")
                            Spacer(Modifier.width(8.dp))
                            Text("폰트 파일 불러오기 (.ttf / .otf)", fontSize = 12.sp)
                        }
                        
                        if (customFontName != null) {
                            IconButton(
                                onClick = { viewModel.setCustomFont(null, null) },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = "폰트 초기화",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    if (customFontName != null) {
                        Text(
                            text = "적용 완료: $customFontName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "기본 Sans-Serif 폰트 적용 중",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                // 연산자 및 물음표 크기/진하기 조절 섹션
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "➕ 연산자(+) & 물음표(?) 커스텀 설정",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "연산자 (+) 크기 조절",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${operatorSizeSp.toInt()} SP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = operatorSizeSp,
                            onValueChange = { viewModel.setOperatorSize(it) },
                            valueRange = 10f..60f,
                            steps = 10
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "연산자 (+) 두께",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(90.dp)
                            )
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("light" to "가늘게", "normal" to "보통", "bold" to "굵게").forEach { (weight, label) ->
                                    val isSelected = operatorWeight == weight
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setOperatorWeight(weight) },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "물음표 (?) 크기 조절",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${mysterySizeSp.toInt()} SP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = mysterySizeSp,
                            onValueChange = { viewModel.setMysterySize(it) },
                            valueRange = 15f..80f,
                            steps = 13
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "물음표 (?) 두께",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(90.dp)
                            )
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("normal" to "보통", "bold" to "굵게", "black" to "아주 굵게").forEach { (weight, label) ->
                                    val isSelected = mysteryWeight == weight
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setMysteryWeight(weight) },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                // 3. Optional watermark / footer title
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🏷️ 하단 설명 슬로건/날짜 추가",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = showFooter,
                            onCheckedChange = { viewModel.setShowFooter(it) }
                        )
                    }

                    if (showFooter) {
                        OutlinedTextField(
                            value = footerText,
                            onValueChange = { viewModel.setFooterText(it) },
                            label = { Text("하단 고정 문구") },
                            placeholder = { Text("예: 2026.05.29 • 조합 레시피") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Recent Combinations Log (Room database history)
        if (savedPosters.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "📂 저장된 포스터 레시피 기록",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    savedPosters.forEach { poster ->
                        Card(
                            modifier = Modifier
                                .width(160.dp)
                                .shadow(2.dp, RoundedCornerShape(10.dp))
                                .clickable { viewModel.loadSavedPoster(poster) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                // Miniature visual presentation of equation
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${poster.emojiA} + ${poster.emojiB}",
                                        fontSize = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteSavedPoster(poster) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "삭제",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = "${poster.textA} + ${poster.textB}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = if (poster.isVerticalLayout) "세로배열" else "가로배열",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Centered Manual Emoji Selection Dialog Overlay
    if (activeEmojiSelector != null) {
        var dialogSearchQuery by remember { mutableStateOf("") }
        var selectedCategoryIndex by remember { mutableStateOf(0) }
        val activeCategory = remember(selectedCategoryIndex) {
            EmojiDatabase.categories.getOrNull(selectedCategoryIndex) ?: EmojiDatabase.categories.first()
        }

        val displayedEmojis = remember(selectedCategoryIndex, dialogSearchQuery) {
            if (dialogSearchQuery.isBlank()) {
                activeCategory.emojis
            } else {
                val query = dialogSearchQuery.trim().lowercase()
                val matchesText = EmojiDatabase.findEmojiForText(query)
                EmojiDatabase.categories.flatMap { it.emojis }.distinct().filter { emoji ->
                    emoji == matchesText || 
                    EmojiDatabase.emojiMap.entries.any { (key, value) -> value == emoji && key.contains(query) } ||
                    EmojiDatabase.englishEmojiMap.entries.any { (key, value) -> value == emoji && key.contains(query) }
                }
            }
        }

        Dialog(onDismissRequest = { viewModel.closeEmojiSelector() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (activeEmojiSelector == "A") "첫 번째" else "두 번째"} 이모티콘 직접 선택",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.closeEmojiSelector() }) {
                            Icon(Icons.Filled.Close, contentDescription = "닫기")
                        }
                    }

                    // Search input
                    OutlinedTextField(
                        value = dialogSearchQuery,
                        onValueChange = { dialogSearchQuery = it },
                        placeholder = { Text("이모지와 단어 검색 (예: 사과, 하트)", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "검색") },
                        trailingIcon = {
                            if (dialogSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { dialogSearchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "검색어 지우기")
                                }
                            }
                        }
                    )

                    // Categories Horizontal Scroll selection chip
                    if (dialogSearchQuery.isBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EmojiDatabase.categories.forEachIndexed { index, category ->
                                val isSelected = index == selectedCategoryIndex
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategoryIndex = index },
                                    label = { Text("${category.icon} ${category.name}", fontSize = 12.sp) }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "검색 결과 (${displayedEmojis.size}개)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (displayedEmojis.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("검색 결과가 없습니다. 다른 단어를 검색해보세요!", fontSize = 13.sp, color = Color.Gray)
                        }
                    } else {
                        // Large easy grid picker (touch targets strictly >= 48dp)
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 48.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(displayedEmojis) { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            if (activeEmojiSelector == "A") {
                                                viewModel.setEmojiA(emoji)
                                            } else {
                                                viewModel.setEmojiB(emoji)
                                            }
                                            viewModel.closeEmojiSelector()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 24.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
