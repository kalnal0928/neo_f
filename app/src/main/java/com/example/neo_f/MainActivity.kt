package com.example.neo_f

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var hangulEngine: HangulEngine
    private var composingText = ""

    // 모든 뷰 참조를 lazy 초기화 및 Nullable로 선언하여 안정성 확보
    private val koreanShiftKey: Button? by lazy { findViewById(R.id.key_shift) }
    private val englishShiftKey: Button? by lazy { findViewById(R.id.key_shift_eng) }
    private val koreanKeyboardLayout: LinearLayout? by lazy { findViewById(R.id.korean_keyboard) }
    private val englishKeyboardLayout: LinearLayout? by lazy { findViewById(R.id.english_keyboard) }
    private val symbolKeyboardLayout: LinearLayout? by lazy { findViewById(R.id.symbol_keyboard) }
    private val editText: EditText? by lazy { findViewById(R.id.editText) }

    private var isKoreanShifted = false
        set(value) {
            field = value
            // FIX: Null-safe call (?.) 사용
            koreanShiftKey?.isActivated = value
        }
    private var isEnglishShifted = false
        set(value) {
            field = value
            // FIX: Null-safe call (?.) 사용
            englishShiftKey?.isActivated = value
            updateEnglishKeyboardCase()
        }

    private enum class KeyboardMode { KOREAN, ENGLISH, SYMBOL }
    private var currentKeyboardMode = KeyboardMode.KOREAN
    private var lastAlphabetMode = KeyboardMode.KOREAN

    // FIX: Map의 Value 타입을 Nullable Button으로 변경
    private val englishKeyButtons = mutableMapOf<Int, Button?>()

    private val backspaceHandler = Handler(Looper.getMainLooper())
    private var backspaceRunnable: Runnable? = null
    private val LONG_PRESS_THRESHOLD = 500L
    private val REPEAT_DELAY = 50L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        editText?.showSoftInputOnFocus = false

        setupHangulEngine()
        setupKoreanKeyboardListeners()
        setupEnglishKeyboardListeners()
        setupSymbolKeyboardListeners()
        setupCommonFunctionalKeys()

        setKeyboardMode(KeyboardMode.KOREAN)
    }

    private fun setupHangulEngine() {
        hangulEngine = HangulEngine({ result ->
            val editable = editText?.text ?: return@HangulEngine
            val currentEditableLength = editable.length

            val startReplacement = currentEditableLength - composingText.length
            val endReplacement = currentEditableLength

            if (startReplacement >= 0 && endReplacement >= startReplacement) {
                editable.replace(startReplacement, endReplacement, result.committed + result.composing)
            } else {
                editable.append(result.committed + result.composing)
            }
            composingText = result.composing
        }, 300L) // 기본 타이머 값 300ms
    }

    private fun setupKoreanKeyboardListeners() {
        val keyButtons = mapOf(
            R.id.key_g to 'ㄱ', R.id.key_n to 'ㄴ', R.id.key_d to 'ㄷ', R.id.key_a to 'ㅏ', R.id.key_eo to 'ㅓ',
            R.id.key_r to 'ㄹ', R.id.key_m to 'ㅁ', R.id.key_b to 'ㅂ', R.id.key_o to 'ㅗ', R.id.key_u to 'ㅜ',
            R.id.key_s to 'ㅅ', R.id.key_ng to 'ㅇ', R.id.key_j to 'ㅈ', R.id.key_eu to 'ㅡ', R.id.key_i to 'ㅣ'
        )

        keyButtons.forEach { (id, char) ->
            findViewById<Button?>(id)?.setOnClickListener {
                val charToSend = if (isKoreanShifted) {
                    when (char) {
                        'ㄱ' -> 'ㄲ'; 'ㄷ' -> 'ㄸ'; 'ㅂ' -> 'ㅃ'; 'ㅅ' -> 'ㅆ'; 'ㅈ' -> 'ㅉ'
                        else -> char
                    }
                } else {
                    char
                }
                hangulEngine.processKey(charToSend)

                if (isKoreanShifted && charToSend != char) {
                    isKoreanShifted = false
                }
            }
        }
    }

    private fun setupEnglishKeyboardListeners() {
        val keyIds = ('a'..'z').map { "key_${it}_eng" }
        keyIds.forEach { idName ->
            val id = resources.getIdentifier(idName, "id", packageName)
            if (id != 0) {
                val button = findViewById<Button?>(id)
                englishKeyButtons[id] = button
                button?.setOnClickListener {
                    var text = button.text.toString()
                    if (isEnglishShifted) {
                        text = text.uppercase()
                        isEnglishShifted = false
                    }
                    editText?.append(text)
                }
            }
        }

        englishShiftKey?.setOnClickListener {
            isEnglishShifted = !isEnglishShifted
        }
    }

    private fun updateEnglishKeyboardCase() {
        englishKeyButtons.values.forEach { button ->
            button?.let { btn ->
                val currentText = btn.text?.toString() ?: ""
                btn.setText(if (isEnglishShifted) currentText.uppercase() else currentText.lowercase())
            }
        }
    }

    private fun setupSymbolKeyboardListeners() {
        val symbolKeys = mapOf(
            R.id.key_1_sym to "1", R.id.key_2_sym to "2", R.id.key_3_sym to "3",
            R.id.key_4_sym to "4", R.id.key_5_sym to "5", R.id.key_6_sym to "6",
            R.id.key_7_sym to "7", R.id.key_8_sym to "8", R.id.key_9_sym to "9",
            R.id.key_0_sym to "0", R.id.key_at_sym to "@", R.id.key_hash_sym to "#",
            R.id.key_dollar_sym to "$", R.id.key_percent_sym to "%", R.id.key_amp_sym to "&",
            R.id.key_star_sym to "*", R.id.key_minus_sym to "-", R.id.key_plus_sym to "+",
            R.id.key_open_paren_sym to "(", R.id.key_close_paren_sym to ")",
            R.id.key_exclamation_sym to "!", R.id.key_quote_sym to "\"",
            R.id.key_colon_sym to ":", R.id.key_semicolon_sym to ";",
            R.id.key_slash_sym to "/", R.id.key_question_sym to "?",
            R.id.key_comma_sym to ",", R.id.key_period_sym to "."
        )

        symbolKeys.forEach { (id, text) ->
            findViewById<Button?>(id)?.setOnClickListener {
                editText?.append(text)
            }
        }

        findViewById<Button?>(R.id.key_space_sym)?.setOnClickListener {
            editText?.append(" ")
        }

        findViewById<Button?>(R.id.key_enter_sym)?.setOnClickListener {
            editText?.append("\n")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCommonFunctionalKeys() {
        val backspaceTouchListener = View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    performSingleBackspace() // Initial single backspace
                    backspaceRunnable = Runnable {
                        performSingleBackspace() // Repeat single backspace
                        backspaceHandler.postDelayed(backspaceRunnable!!, REPEAT_DELAY)
                    }.also {
                        backspaceHandler.postDelayed(it, LONG_PRESS_THRESHOLD) // Start repeating after long press threshold
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    backspaceRunnable?.let { backspaceHandler.removeCallbacks(it) }
                    backspaceRunnable = null // Clear the runnable
                    true
                }
                else -> false
            }
        }
        findViewById<Button?>(R.id.key_backspace)?.setOnTouchListener(backspaceTouchListener)
        findViewById<Button?>(R.id.key_backspace_eng)?.setOnTouchListener(backspaceTouchListener)
        findViewById<Button?>(R.id.key_backspace_sym)?.setOnTouchListener(backspaceTouchListener)

        val enterListener = View.OnClickListener {
            editText?.append("\n")
            hangulEngine.reset()
            composingText = ""
        }
        findViewById<Button?>(R.id.key_enter)?.setOnClickListener(enterListener)
        findViewById<Button?>(R.id.key_enter_eng)?.setOnClickListener(enterListener)

        val spaceListener = View.OnClickListener {
            if (currentKeyboardMode == KeyboardMode.KOREAN) {
                hangulEngine.processKey(' ')
            } else {
                editText?.append(" ")
            }
        }
        findViewById<Button?>(R.id.key_space)?.setOnClickListener(spaceListener)
        findViewById<Button?>(R.id.key_space_eng)?.setOnClickListener(spaceListener)

        val langListener = View.OnClickListener {
            val nextMode = if (currentKeyboardMode == KeyboardMode.KOREAN) KeyboardMode.ENGLISH else KeyboardMode.KOREAN
            setKeyboardMode(nextMode)
        }
        findViewById<Button?>(R.id.key_lang_eng)?.setOnClickListener(langListener)

        // 자판 전환 버튼 (한글 -> 영어 -> 숫자 -> 한글 순환)
        findViewById<Button?>(R.id.key_mode_kor)?.setOnClickListener {
            val nextMode = when (currentKeyboardMode) {
                KeyboardMode.KOREAN -> KeyboardMode.ENGLISH
                KeyboardMode.ENGLISH -> KeyboardMode.SYMBOL
                KeyboardMode.SYMBOL -> KeyboardMode.KOREAN
            }
            setKeyboardMode(nextMode)
        }
        
        findViewById<Button?>(R.id.key_mode_eng)?.setOnClickListener {
            val nextMode = if (currentKeyboardMode == KeyboardMode.SYMBOL) lastAlphabetMode else KeyboardMode.SYMBOL
            setKeyboardMode(nextMode)
        }
        
        findViewById<Button?>(R.id.key_mode_sym)?.setOnClickListener {
            setKeyboardMode(lastAlphabetMode)
        }

        // 된소리 키 (Shift 키)
        koreanShiftKey?.setOnClickListener {
            isKoreanShifted = !isKoreanShifted
        }
    }

    private fun performSingleBackspace() {
        if (currentKeyboardMode == KeyboardMode.KOREAN) {
            val newComposing = hangulEngine.backspace()
            if (newComposing == "BACKSPACE") {
                deleteLastCharFromEditText()
                composingText = "" // Reset composingText after direct deletion
            } else {
                val editable = editText?.text ?: return
                val currentComposingLength = composingText.length

                val startOfComposingInEditable = editable.length - currentComposingLength
                val endOfComposingInEditable = editable.length

                if (startOfComposingInEditable >= 0) {
                    editable.replace(startOfComposingInEditable, endOfComposingInEditable, newComposing)
                } else {
                    if (newComposing.isEmpty()) {
                        deleteLastCharFromEditText()
                    }
                }
                composingText = newComposing
            }
        } else {
            deleteLastCharFromEditText()
        }
    }

    private fun performWordBackspace() {
        val text = editText?.text?.toString() ?: return
        val selectionEnd = editText?.selectionEnd ?: 0
        if (selectionEnd > 0) {
            val lastSpace = text.substring(0, selectionEnd).trimEnd().lastIndexOf(' ')
            val start = if (lastSpace == -1) 0 else lastSpace + 1
            if (start < selectionEnd) {
                editText?.text?.delete(start, selectionEnd)
            } else {
                deleteLastCharFromEditText()
            }
        }
    }

    private fun deleteLastCharFromEditText() {
        val text: Editable = editText?.text ?: return
        val selectionEnd = editText?.selectionEnd ?: 0
        if (selectionEnd > 0) {
            text.delete(selectionEnd - 1, selectionEnd)
        }
    }

    private fun setKeyboardMode(mode: KeyboardMode) {
        if (mode != KeyboardMode.SYMBOL) {
            lastAlphabetMode = mode
        }
        currentKeyboardMode = mode

        koreanKeyboardLayout?.visibility = if (mode == KeyboardMode.KOREAN) View.VISIBLE else View.GONE
        englishKeyboardLayout?.visibility = if (mode == KeyboardMode.ENGLISH) View.VISIBLE else View.GONE
        symbolKeyboardLayout?.visibility = if (mode == KeyboardMode.SYMBOL) View.VISIBLE else View.GONE

        hangulEngine.reset()
        composingText = ""
        isKoreanShifted = false
        isEnglishShifted = false
    }
}




