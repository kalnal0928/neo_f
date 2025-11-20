package com.example.neo_f

import android.annotation.SuppressLint
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout

class NeoFKeyboardService : InputMethodService() {

    private val TAG = "NeoFKeyboardService"
    
    private var hangulEngine: HangulEngine? = null
    private var composingText = ""

    private var koreanShiftKey: Button? = null
    private var englishShiftKey: Button? = null
    private var koreanKeyboardLayout: LinearLayout? = null
    private var englishKeyboardLayout: LinearLayout? = null
    private var symbolKeyboardLayout: LinearLayout? = null

    private var isKoreanShifted = false
        set(value) {
            field = value
            koreanShiftKey?.isActivated = value
        }
    private var isEnglishShifted = false
        set(value) {
            field = value
            englishShiftKey?.isActivated = value
            updateEnglishKeyboardCase()
        }

    private enum class KeyboardMode { KOREAN, ENGLISH, SYMBOL }
    private var currentKeyboardMode = KeyboardMode.KOREAN
    private var lastAlphabetMode = KeyboardMode.KOREAN

    private val englishKeyButtons = mutableMapOf<Int, Button?>()

    private val backspaceHandler = Handler(Looper.getMainLooper())
    private var backspaceRunnable: Runnable? = null
    private val LONG_PRESS_THRESHOLD = 500L
    private val REPEAT_DELAY = 50L

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate called")
    }
    
    override fun onEvaluateFullscreenMode(): Boolean {
        Log.d(TAG, "onEvaluateFullscreenMode called")
        return false // 전체화면 모드 비활성화
    }
    
    override fun onEvaluateInputViewShown(): Boolean {
        Log.d(TAG, "onEvaluateInputViewShown called")
        return true // 항상 키보드 표시
    }

    override fun onCreateInputView(): View? {
        Log.d(TAG, "onCreateInputView called")
        
        // 테스트 모드: 간단한 레이아웃으로 먼저 테스트
        val TEST_MODE = false
        
        if (TEST_MODE) {
            Log.d(TAG, "Using test layout")
            val testView = layoutInflater.inflate(R.layout.keyboard_simple_test, null)
            testView.findViewById<Button>(R.id.test_button)?.setOnClickListener {
                Log.d(TAG, "Test button clicked")
                currentInputConnection?.commitText("테스트", 1)
            }
            return testView
        }
        
        return try {
            val keyboardView = layoutInflater.inflate(R.layout.keyboard_layout, null)
            Log.d(TAG, "Layout inflated successfully")

            setupHangulEngine()
            Log.d(TAG, "HangulEngine setup complete")
            
            setupViewReferences(keyboardView)
            Log.d(TAG, "View references setup complete")
            
            setupKoreanKeyboardListeners(keyboardView)
            Log.d(TAG, "Korean keyboard listeners setup complete")
            
            setupEnglishKeyboardListeners(keyboardView)
            Log.d(TAG, "English keyboard listeners setup complete")
            
            setupSymbolKeyboardListeners(keyboardView)
            Log.d(TAG, "Symbol keyboard listeners setup complete")
            
            setupCommonFunctionalKeys(keyboardView)
            Log.d(TAG, "Common functional keys setup complete")

            setKeyboardMode(KeyboardMode.KOREAN)
            Log.d(TAG, "Keyboard mode set to KOREAN")

            keyboardView
        } catch (e: Exception) {
            Log.e(TAG, "Error creating input view", e)
            e.printStackTrace()
            null
        }
    }
    
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Log.d(TAG, "onStartInput called, restarting: $restarting")
        Log.d(TAG, "InputType: ${attribute?.inputType}")
    }
    
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.d(TAG, "onStartInputView called, restarting: $restarting")
    }
    
    override fun onBindInput() {
        super.onBindInput()
        Log.d(TAG, "onBindInput called")
    }
    
    override fun onUnbindInput() {
        super.onUnbindInput()
        Log.d(TAG, "onUnbindInput called")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
    }

    private fun setupViewReferences(view: View) {
        koreanShiftKey = view.findViewById(R.id.key_shift)
        englishShiftKey = view.findViewById(R.id.key_shift_eng)
        koreanKeyboardLayout = view.findViewById(R.id.korean_keyboard)
        englishKeyboardLayout = view.findViewById(R.id.english_keyboard)
        symbolKeyboardLayout = view.findViewById(R.id.symbol_keyboard)
    }

    private fun setupHangulEngine() {
        hangulEngine = HangulEngine { result ->
            val ic = currentInputConnection ?: return@HangulEngine
            
            // 조합 중인 텍스트가 있으면 먼저 제거
            if (composingText.isNotEmpty()) {
                ic.finishComposingText()
                ic.deleteSurroundingText(composingText.length, 0)
            }
            
            // 완성된 문자가 있으면 커밋
            if (result.committed.isNotEmpty()) {
                ic.commitText(result.committed, 1)
            }
            
            // 조합 중인 문자가 있으면 설정
            if (result.composing.isNotEmpty()) {
                ic.setComposingText(result.composing, 1)
                composingText = result.composing
            } else {
                composingText = ""
            }
        }
    }

    private fun setupKoreanKeyboardListeners(view: View) {
        val keyButtons = mapOf(
            R.id.key_g to 'ㄱ', R.id.key_n to 'ㄴ', R.id.key_d to 'ㄷ', R.id.key_a to 'ㅏ', R.id.key_eo to 'ㅓ',
            R.id.key_r to 'ㄹ', R.id.key_m to 'ㅁ', R.id.key_b to 'ㅂ', R.id.key_o to 'ㅗ', R.id.key_u to 'ㅜ',
            R.id.key_s to 'ㅅ', R.id.key_ng to 'ㅇ', R.id.key_j to 'ㅈ', R.id.key_eu to 'ㅡ', R.id.key_i to 'ㅣ'
        )

        keyButtons.forEach { (id, char) ->
            view.findViewById<Button?>(id)?.setOnClickListener {
                val charToSend = if (isKoreanShifted) {
                    when (char) {
                        'ㄱ' -> 'ㄲ'; 'ㄷ' -> 'ㄸ'; 'ㅂ' -> 'ㅃ'; 'ㅅ' -> 'ㅆ'; 'ㅈ' -> 'ㅉ'
                        else -> char
                    }
                } else {
                    char
                }
                hangulEngine?.processKey(charToSend)

                if (isKoreanShifted && charToSend != char) {
                    isKoreanShifted = false
                }
            }
        }
    }

    private fun setupEnglishKeyboardListeners(view: View) {
        val keyIds = ('a'..'z').map { "key_${it}_eng" }
        keyIds.forEach { idName ->
            val id = resources.getIdentifier(idName, "id", packageName)
            if (id != 0) {
                val button = view.findViewById<Button?>(id)
                englishKeyButtons[id] = button
                button?.setOnClickListener {
                    var text = button.text.toString()
                    if (isEnglishShifted) {
                        text = text.uppercase()
                        isEnglishShifted = false
                    } else {
                        text = text.lowercase()
                    }
                    currentInputConnection?.commitText(text, 1)
                }
            }
        }

        view.findViewById<Button?>(R.id.key_shift_eng)?.setOnClickListener {
            isEnglishShifted = !isEnglishShifted
        }
        
        // @ 버튼
        view.findViewById<Button?>(R.id.key_at_eng)?.setOnClickListener {
            currentInputConnection?.commitText("@", 1)
        }
        
        // .? 버튼
        view.findViewById<Button?>(R.id.key_period_eng)?.setOnClickListener {
            if (isEnglishShifted) {
                currentInputConnection?.commitText("?", 1)
                isEnglishShifted = false
            } else {
                currentInputConnection?.commitText(".", 1)
            }
        }
        
        // ,! 버튼
        view.findViewById<Button?>(R.id.key_comma_eng)?.setOnClickListener {
            if (isEnglishShifted) {
                currentInputConnection?.commitText("!", 1)
                isEnglishShifted = false
            } else {
                currentInputConnection?.commitText(",", 1)
            }
        }
        
        // DEL 버튼
        view.findViewById<Button?>(R.id.key_del_eng)?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    performSingleBackspace()
                    backspaceRunnable = Runnable {
                        performSingleBackspace()
                        backspaceHandler.postDelayed(backspaceRunnable!!, REPEAT_DELAY)
                    }.also {
                        backspaceHandler.postDelayed(it, LONG_PRESS_THRESHOLD)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    backspaceRunnable?.let { backspaceHandler.removeCallbacks(it) }
                    backspaceRunnable = null
                    true
                }
                else -> false
            }
        }
    }

    private fun updateEnglishKeyboardCase() {
        englishKeyButtons.values.forEach { button ->
            button?.let { btn ->
                val currentText = btn.text?.toString() ?: ""
                btn.text = if (isEnglishShifted) currentText.uppercase() else currentText.lowercase()
            }
        }
    }

    private fun setupSymbolKeyboardListeners(view: View) {
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
            view.findViewById<Button?>(id)?.setOnClickListener {
                currentInputConnection?.commitText(text, 1)
            }
        }

        view.findViewById<Button?>(R.id.key_space_sym)?.setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
        }

        view.findViewById<Button?>(R.id.key_enter_sym)?.setOnClickListener {
            sendEnterKey()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCommonFunctionalKeys(view: View) {
        val backspaceTouchListener = View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    performSingleBackspace()
                    backspaceRunnable = Runnable {
                        performSingleBackspace()
                        backspaceHandler.postDelayed(backspaceRunnable!!, REPEAT_DELAY)
                    }.also {
                        backspaceHandler.postDelayed(it, LONG_PRESS_THRESHOLD)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    backspaceRunnable?.let { backspaceHandler.removeCallbacks(it) }
                    backspaceRunnable = null
                    true
                }
                else -> false
            }
        }
        view.findViewById<Button?>(R.id.key_backspace)?.setOnTouchListener(backspaceTouchListener)
        view.findViewById<Button?>(R.id.key_backspace_eng)?.setOnTouchListener(backspaceTouchListener)
        view.findViewById<Button?>(R.id.key_backspace_sym)?.setOnTouchListener(backspaceTouchListener)

        val enterListener = View.OnClickListener {
            sendEnterKey()
        }
        view.findViewById<Button?>(R.id.key_enter)?.setOnClickListener(enterListener)
        view.findViewById<Button?>(R.id.key_enter_eng)?.setOnClickListener(enterListener)

        val spaceListener = View.OnClickListener {
            if (currentKeyboardMode == KeyboardMode.KOREAN) {
                hangulEngine?.processKey(' ')
            } else {
                currentInputConnection?.commitText(" ", 1)
            }
        }
        view.findViewById<Button?>(R.id.key_space)?.setOnClickListener(spaceListener)
        view.findViewById<Button?>(R.id.key_space_eng)?.setOnClickListener(spaceListener)

        // 한글 키보드의 자판 전환 버튼
        view.findViewById<Button?>(R.id.key_mode_kor)?.setOnClickListener {
            val nextMode = when (currentKeyboardMode) {
                KeyboardMode.KOREAN -> KeyboardMode.ENGLISH
                KeyboardMode.ENGLISH -> KeyboardMode.SYMBOL
                KeyboardMode.SYMBOL -> KeyboardMode.KOREAN
            }
            setKeyboardMode(nextMode)
        }

        // 영어 키보드의 자판 전환 버튼
        view.findViewById<Button?>(R.id.key_mode_eng)?.setOnClickListener {
            val nextMode = when (currentKeyboardMode) {
                KeyboardMode.KOREAN -> KeyboardMode.ENGLISH
                KeyboardMode.ENGLISH -> KeyboardMode.SYMBOL
                KeyboardMode.SYMBOL -> KeyboardMode.KOREAN
            }
            setKeyboardMode(nextMode)
        }

        // 심볼 키보드의 자판 전환 버튼
        view.findViewById<Button?>(R.id.key_mode_sym)?.setOnClickListener {
            val nextMode = when (currentKeyboardMode) {
                KeyboardMode.KOREAN -> KeyboardMode.ENGLISH
                KeyboardMode.ENGLISH -> KeyboardMode.SYMBOL
                KeyboardMode.SYMBOL -> KeyboardMode.KOREAN
            }
            setKeyboardMode(nextMode)
        }

        koreanShiftKey?.setOnClickListener {
            isKoreanShifted = !isKoreanShifted
        }
    }

    private fun performSingleBackspace() {
        val ic = currentInputConnection ?: return
        
        if (currentKeyboardMode == KeyboardMode.KOREAN) {
            val newComposing = hangulEngine?.backspace() ?: "BACKSPACE"
            if (newComposing == "BACKSPACE") {
                ic.deleteSurroundingText(1, 0)
                composingText = ""
            } else {
                if (composingText.isNotEmpty()) {
                    ic.deleteSurroundingText(composingText.length, 0)
                }
                if (newComposing.isNotEmpty()) {
                    ic.setComposingText(newComposing, 1)
                }
                composingText = newComposing
            }
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }

    private fun sendEnterKey() {
        val ic = currentInputConnection ?: return
        hangulEngine?.reset()
        composingText = ""
        
        val editorInfo = currentInputEditorInfo
        if (editorInfo != null && (editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
            when (editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION) {
                EditorInfo.IME_ACTION_SEARCH -> ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
                EditorInfo.IME_ACTION_SEND -> ic.performEditorAction(EditorInfo.IME_ACTION_SEND)
                EditorInfo.IME_ACTION_GO -> ic.performEditorAction(EditorInfo.IME_ACTION_GO)
                EditorInfo.IME_ACTION_NEXT -> ic.performEditorAction(EditorInfo.IME_ACTION_NEXT)
                EditorInfo.IME_ACTION_DONE -> ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
                else -> ic.commitText("\n", 1)
            }
        } else {
            ic.commitText("\n", 1)
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

        hangulEngine?.reset()
        composingText = ""
        isKoreanShifted = false
        isEnglishShifted = false
    }

    override fun onFinishInput() {
        super.onFinishInput()
        hangulEngine?.reset()
        composingText = ""
    }
}
