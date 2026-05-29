package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.PosterRepository
import com.example.data.SavedPoster
import com.example.utils.EmojiDatabase
import com.example.utils.PosterGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PosterViewModel(
    private val repository: PosterRepository,
    private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("poster_settings", Context.MODE_PRIVATE)

    // Input States
    private val _textA = MutableStateFlow("사과")
    val textA: StateFlow<String> = _textA.asStateFlow()

    private val _emojiA = MutableStateFlow("🍎")
    val emojiA: StateFlow<String> = _emojiA.asStateFlow()

    private val _textB = MutableStateFlow("바나나")
    val textB: StateFlow<String> = _textB.asStateFlow()

    private val _emojiB = MutableStateFlow("🍌")
    val emojiB: StateFlow<String> = _emojiB.asStateFlow()

    // Style States
    private val _fontSizeSp = MutableStateFlow(40f) // Sp units
    val fontSizeSp: StateFlow<Float> = _fontSizeSp.asStateFlow()

    private val _textColorHex = MutableStateFlow("#000000")
    val textColorHex: StateFlow<String> = _textColorHex.asStateFlow()

    private val _isVerticalLayout = MutableStateFlow(false)
    val isVerticalLayout: StateFlow<Boolean> = _isVerticalLayout.asStateFlow()

    private val _footerText = MutableStateFlow("EMOJI FORMULA POSTER")
    val footerText: StateFlow<String> = _footerText.asStateFlow()

    private val _showFooter = MutableStateFlow(true)
    val showFooter: StateFlow<Boolean> = _showFooter.asStateFlow()

    private val _borderStyle = MutableStateFlow("thin") // "none", "thin", "bold", "double"
    val borderStyle: StateFlow<String> = _borderStyle.asStateFlow()

    // Operator sizing & weights
    private val _operatorSize = MutableStateFlow(18f)
    val operatorSize: StateFlow<Float> = _operatorSize.asStateFlow()

    private val _operatorWeight = MutableStateFlow("light") // "light", "normal", "bold"
    val operatorWeight: StateFlow<String> = _operatorWeight.asStateFlow()

    // Mystery (?) sizing & weights
    private val _mysterySize = MutableStateFlow(24f)
    val mysterySize: StateFlow<Float> = _mysterySize.asStateFlow()

    private val _mysteryWeight = MutableStateFlow("bold") // "normal", "bold", "black"
    val mysteryWeight: StateFlow<String> = _mysteryWeight.asStateFlow()

    // Custom Font Paths
    private val _customFontPath = MutableStateFlow<String?>(null)
    val customFontPath: StateFlow<String?> = _customFontPath.asStateFlow()

    private val _customFontName = MutableStateFlow<String?>(null)
    val customFontName: StateFlow<String?> = _customFontName.asStateFlow()

    // Custom Image Settings
    private val _imagePathA = MutableStateFlow<String?>(null)
    val imagePathA: StateFlow<String?> = _imagePathA.asStateFlow()

    private val _imagePathB = MutableStateFlow<String?>(null)
    val imagePathB: StateFlow<String?> = _imagePathB.asStateFlow()

    private val _imageSizeA = MutableStateFlow(120f)
    val imageSizeA: StateFlow<Float> = _imageSizeA.asStateFlow()

    private val _imageSizeB = MutableStateFlow(120f)
    val imageSizeB: StateFlow<Float> = _imageSizeB.asStateFlow()

    private val _featherA = MutableStateFlow(0f)
    val featherA: StateFlow<Float> = _featherA.asStateFlow()

    private val _featherB = MutableStateFlow(0f)
    val featherB: StateFlow<Float> = _featherB.asStateFlow()

    // Dialog state for manually selecting emoji
    private val _activeEmojiSelector = MutableStateFlow<String?>(null) // "A", "B", or null
    val activeEmojiSelector: StateFlow<String?> = _activeEmojiSelector.asStateFlow()

    // Room Database Saved Creations State
    val savedPosters: StateFlow<List<SavedPoster>> = repository.allPosters
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        try {
            // Load initial settings from SharedPreferences for auto-save restore
            _textA.value = prefs.getString("textA", "사과") ?: "사과"
            _emojiA.value = prefs.getString("emojiA", "🍎") ?: "🍎"
            _textB.value = prefs.getString("textB", "바나나") ?: "바나나"
            _emojiB.value = prefs.getString("emojiB", "🍌") ?: "🍌"
            _fontSizeSp.value = prefs.getFloat("fontSizeSp", 40f)
            _textColorHex.value = prefs.getString("textColorHex", "#000000") ?: "#000000"
            _isVerticalLayout.value = prefs.getBoolean("isVerticalLayout", false)
            _footerText.value = prefs.getString("footerText", "EMOJI FORMULA POSTER") ?: "EMOJI FORMULA POSTER"
            _showFooter.value = prefs.getBoolean("showFooter", true)
            _borderStyle.value = prefs.getString("borderStyle", "thin") ?: "thin"
            _operatorSize.value = prefs.getFloat("operatorSizeSp", 18f)
            _operatorWeight.value = prefs.getString("operatorWeight", "light") ?: "light"
            _mysterySize.value = prefs.getFloat("mysterySizeSp", 24f)
            _mysteryWeight.value = prefs.getString("mysteryWeight", "bold") ?: "bold"
            _customFontPath.value = prefs.getString("customFontPath", null)
            _customFontName.value = prefs.getString("customFontName", null)
            _imagePathA.value = prefs.getString("imagePathA", null)
            _imagePathB.value = prefs.getString("imagePathB", null)
            _imageSizeA.value = prefs.getFloat("imageSizeA", 120f)
            _imageSizeB.value = prefs.getFloat("imageSizeB", 120f)
            _featherA.value = prefs.getFloat("featherA", 0f)
            _featherB.value = prefs.getFloat("featherB", 0f)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                prefs.edit().clear().apply()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun autoSave() {
        prefs.edit().apply {
            putString("textA", _textA.value)
            putString("emojiA", _emojiA.value)
            putString("textB", _textB.value)
            putString("emojiB", _emojiB.value)
            putFloat("fontSizeSp", _fontSizeSp.value)
            putString("textColorHex", _textColorHex.value)
            putBoolean("isVerticalLayout", _isVerticalLayout.value)
            putString("footerText", _footerText.value)
            putBoolean("showFooter", _showFooter.value)
            putString("borderStyle", _borderStyle.value)
            putFloat("operatorSizeSp", _operatorSize.value)
            putString("operatorWeight", _operatorWeight.value)
            putFloat("mysterySizeSp", _mysterySize.value)
            putString("mysteryWeight", _mysteryWeight.value)
            putString("customFontPath", _customFontPath.value)
            putString("customFontName", _customFontName.value)
            putString("imagePathA", _imagePathA.value)
            putString("imagePathB", _imagePathB.value)
            putFloat("imageSizeA", _imageSizeA.value)
            putFloat("imageSizeB", _imageSizeB.value)
            putFloat("featherA", _featherA.value)
            putFloat("featherB", _featherB.value)
            apply()
        }
    }

    fun onTextAChanged(newText: String) {
        _textA.value = newText
        // Automatically find matching emoji
        val match = EmojiDatabase.findEmojiForText(newText)
        if (match != "✨" && match != "❓") {
            _emojiA.value = match
        }
        autoSave()
    }

    fun onTextBChanged(newText: String) {
        _textB.value = newText
        // Automatically find matching emoji
        val match = EmojiDatabase.findEmojiForText(newText)
        if (match != "✨" && match != "❓") {
            _emojiB.value = match
        }
        autoSave()
    }

    fun setEmojiA(emoji: String) {
        _emojiA.value = emoji
        // When preset emoji selected, clean up custom image so it goes back to emoji
        _imagePathA.value = null
        autoSave()
    }

    fun setEmojiB(emoji: String) {
        _emojiB.value = emoji
        _imagePathB.value = null
        autoSave()
    }

    fun setFontSize(size: Float) {
        _fontSizeSp.value = size
        autoSave()
    }

    fun setTextColorHex(hex: String) {
        _textColorHex.value = hex
        autoSave()
    }

    fun setVerticalLayout(vertical: Boolean) {
        _isVerticalLayout.value = vertical
        autoSave()
    }

    fun setFooterText(text: String) {
        _footerText.value = text
        autoSave()
    }

    fun setShowFooter(show: Boolean) {
        _showFooter.value = show
        autoSave()
    }

    fun setBorderStyle(style: String) {
        _borderStyle.value = style
        autoSave()
    }

    fun setOperatorSize(size: Float) {
        _operatorSize.value = size
        autoSave()
    }

    fun setOperatorWeight(weight: String) {
        _operatorWeight.value = weight
        autoSave()
    }

    fun setMysterySize(size: Float) {
        _mysterySize.value = size
        autoSave()
    }

    fun setMysteryWeight(weight: String) {
        _mysteryWeight.value = weight
        autoSave()
    }

    fun setCustomFont(path: String?, name: String?) {
        _customFontPath.value = path
        _customFontName.value = name
        autoSave()
    }

    fun setImagePathA(path: String?) {
        _imagePathA.value = path
        autoSave()
    }

    fun setImagePathB(path: String?) {
        _imagePathB.value = path
        autoSave()
    }

    fun setImageSizeA(size: Float) {
        _imageSizeA.value = size
        autoSave()
    }

    fun setImageSizeB(size: Float) {
        _imageSizeB.value = size
        autoSave()
    }

    fun setFeatherA(feather: Float) {
        _featherA.value = feather
        autoSave()
    }

    fun setFeatherB(feather: Float) {
        _featherB.value = feather
        autoSave()
    }

    fun openEmojiSelector(target: String) {
        _activeEmojiSelector.value = target
    }

    fun closeEmojiSelector() {
        _activeEmojiSelector.value = null
    }

    /**
     * Re-loads a saved combo from the database.
     */
    fun loadSavedPoster(poster: SavedPoster) {
        _textA.value = poster.textA
        _emojiA.value = poster.emojiA
        _textB.value = poster.textB
        _emojiB.value = poster.emojiB
        _fontSizeSp.value = poster.fontSize.toFloat()
        _textColorHex.value = poster.textColorHex
        _isVerticalLayout.value = poster.isVerticalLayout
        _imagePathA.value = poster.imagePathA
        _imagePathB.value = poster.imagePathB
        _imageSizeA.value = poster.imageSizeA
        _imageSizeB.value = poster.imageSizeB
        _featherA.value = poster.featherA
        _featherB.value = poster.featherB
        autoSave()
    }

    /**
     * Generates active bitmap based on state.
     */
    fun createBitmap(): Bitmap {
        return PosterGenerator.generatePosterBitmap(
            textA = _textA.value,
            emojiA = _emojiA.value,
            textB = _textB.value,
            emojiB = _emojiB.value,
            fontSizeSp = _fontSizeSp.value,
            textColorHex = _textColorHex.value,
            isVerticalLayout = _isVerticalLayout.value,
            footerText = _footerText.value,
            showFooter = _showFooter.value,
            borderStyle = _borderStyle.value,
            operatorSizeSp = _operatorSize.value,
            operatorWeight = _operatorWeight.value,
            mysterySizeSp = _mysterySize.value,
            mysteryWeight = _mysteryWeight.value,
            customFontPath = _customFontPath.value,
            imagePathA = _imagePathA.value,
            imagePathB = _imagePathB.value,
            imageSizeA = _imageSizeA.value,
            imageSizeB = _imageSizeB.value,
            featherA = _featherA.value,
            featherB = _featherB.value
        )
    }

    /**
     * Save to gallery and add to Room DB.
     */
    fun saveToGallery(context: Context, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val bitmap = createBitmap()
            val saveResult = PosterGenerator.savePosterToGallery(context, bitmap)
            saveResult.onSuccess { msg ->
                // Add to Room history log
                repository.insert(
                    SavedPoster(
                        textA = _textA.value,
                        emojiA = _emojiA.value,
                        textB = _textB.value,
                        emojiB = _emojiB.value,
                        fontSize = _fontSizeSp.value.toInt(),
                        textColorHex = _textColorHex.value,
                        isVerticalLayout = _isVerticalLayout.value,
                        imagePathA = _imagePathA.value,
                        imagePathB = _imagePathB.value,
                        imageSizeA = _imageSizeA.value,
                        imageSizeB = _imageSizeB.value,
                        featherA = _featherA.value,
                        featherB = _featherB.value
                    )
                )
                onResult(msg)
            }.onFailure { err ->
                onResult("저장 실패: ${err.message}")
            }
        }
    }

    /**
     * Share poster directly with intent.
     */
    fun sharePoster(context: Context, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val bitmap = createBitmap()
            val shareResult = PosterGenerator.sharePoster(context, bitmap)
            shareResult.onSuccess {
                onResult("공유 창이 열렸습니다.")
            }.onFailure { err ->
                onResult("공유 실패: ${err.message}")
            }
        }
    }

    /**
     * Delete poster item from database.
     */
    fun deleteSavedPoster(poster: SavedPoster) {
        viewModelScope.launch {
            repository.deleteById(poster.id)
        }
    }
}

class PosterViewModelFactory(
    private val repository: PosterRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PosterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PosterViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
