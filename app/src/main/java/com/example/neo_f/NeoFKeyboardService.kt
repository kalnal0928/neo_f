package com.example.neo_f

import android.annotation.SuppressLint
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
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
    private var numberKeyboardLayout: LinearLayout? = null
    private var englishKeyboardLayout: LinearLayout? = null
    private var symbolKeyboardLayout: LinearLayout? = null
    private var numberRowKorean: LinearLayout? = null
    private var numberRowEnglish: LinearLayout? = null
    
    private var audioManager: AudioManager? = null
    private var isSoundEnabled = true
    private var isVibrationEnabled = true
    private var isColorEffectEnabled = true
    private var isScaleEffectEnabled = true
    private var touchColor = 0xFF4CAF50.toInt()
    private var keyboardView: View? = null
    private var needsRecreate = false

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

    private enum class KeyboardMode { KOREAN, NUMBER, ENGLISH, SYMBOL }
    private var currentKeyboardMode = KeyboardMode.KOREAN
    private var lastAlphabetMode = KeyboardMode.KOREAN

    private val englishKeyButtons = mutableMapOf<Int, Button?>()

    private val backspaceHandler = Handler(Looper.getMainLooper())
    private var backspaceRunnable: Runnable? = null
    private val LONG_PRESS_THRESHOLD = 500L
    private val REPEAT_DELAY = 50L
    
    private val enterHandler = Handler(Looper.getMainLooper())
    private var enterLongPressRunnable: Runnable? = null
    private var enterPressStartTime = 0L
    private var enterLongPressThreshold = 500L

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate called")
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        isSoundEnabled = SettingsActivity.isSoundEnabled(this)
    }
    
    private fun applyTextSize(view: View) {
        val textSize = SettingsActivity.getTextSize(this)
        Log.d(TAG, "Applying text size: $textSize sp")
        
        // 모든 버튼에 글자 크기 적용
        applyTextSizeToButtons(view, textSize)
    }
    
    private fun applyTextSizeToButtons(view: View, textSize: Float) {
        if (view is Button) {
            // setTextSize는 기본적으로 SP 단위를 사용
            view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSize)
            // 텍스트 색상도 적용
            val textColor = SettingsActivity.getTextColor(this)
            view.setTextColor(textColor)
            Log.d(TAG, "Applied text size $textSize sp and color to button: ${view.text}")
        } else if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                applyTextSizeToButtons(view.getChildAt(i), textSize)
            }
        }
    }
    
    private fun applyKeySpacing(view: View) {
        val spacing = SettingsActivity.getKeySpacing(this)
        Log.d(TAG, "Applying key spacing: $spacing dp")
        
        // 모든 버튼에 간격 적용
        applyKeySpacingToButtons(view, spacing)
    }
    
    private fun applyKeySpacingToButtons(view: View, spacing: Int) {
        if (view is Button) {
            val params = view.layoutParams as? android.view.ViewGroup.MarginLayoutParams
            params?.setMargins(spacing, spacing, spacing, spacing)
            view.layoutParams = params
        } else if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                applyKeySpacingToButtons(view.getChildAt(i), spacing)
            }
        }
    }
    
    private fun applyKeyCornerRadius(view: View) {
        val cornerRadius = SettingsActivity.getKeyCornerRadius(this)
        Log.d(TAG, "Applying key corner radius: $cornerRadius dp")
        
        // 모든 버튼에 라운드 적용
        applyKeyCornerRadiusToButtons(view, cornerRadius)
    }
    
    private fun applyKeyHeight(view: View) {
        val keyHeight = SettingsActivity.getKeyHeight(this)
        Log.d(TAG, "Applying key height: $keyHeight dp")
        
        // 모든 버튼에 높이 적용
        applyKeyHeightToButtons(view, keyHeight)
    }
    
    private fun applyKeyHeightToButtons(view: View, heightDp: Int) {
        if (view is Button) {
            val heightPx = (heightDp * resources.displayMetrics.density).toInt()
            val params = view.layoutParams
            params.height = heightPx
            view.layoutParams = params
            Log.d(TAG, "Applied height $heightDp dp to button: ${view.text}")
        } else if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                applyKeyHeightToButtons(view.getChildAt(i), heightDp)
            }
        }
    }
    
    private fun applyKeyCornerRadiusToButtons(view: View, radiusDp: Int) {
        if (view is Button) {
            val radiusPx = (radiusDp * resources.displayMetrics.density).toInt()
            
            // 버튼의 현재 상태 저장
            val wasClickable = view.isClickable
            val wasEnabled = view.isEnabled
            val wasFocusable = view.isFocusable
            
            // 사용자 설정 색상 가져오기
            val keyBackgroundColor = SettingsActivity.getKeyBackgroundColor(this)
            val functionalKeyColor = SettingsActivity.getFunctionalKeyColor(this)
            
            // 버튼 ID로 배경색 결정
            val backgroundColor = when (view.id) {
                R.id.key_mode_kor, R.id.key_mode_num, R.id.key_mode_eng, R.id.key_mode_sym,
                R.id.key_shift, R.id.key_shift_eng, R.id.key_hangul_num,
                R.id.key_space, R.id.key_space_eng, R.id.key_space_num, R.id.key_space_sym,
                R.id.key_enter_eng, R.id.key_search_kor, R.id.key_search_num, R.id.key_enter_sym,
                R.id.key_backspace, R.id.key_backspace_eng, R.id.key_backspace_num, R.id.key_backspace_sym,
                R.id.key_del_eng, R.id.key_at_eng, R.id.key_period_eng, R.id.key_comma_eng,
                R.id.key_0_kor, R.id.key_1_kor, R.id.key_2_kor, R.id.key_3_kor, R.id.key_4_kor,
                R.id.key_5_kor, R.id.key_6_kor, R.id.key_7_kor, R.id.key_8_kor, R.id.key_9_kor,
                R.id.key_0_eng_num, R.id.key_1_eng_num, R.id.key_2_eng_num, R.id.key_3_eng_num, R.id.key_4_eng_num,
                R.id.key_5_eng_num, R.id.key_6_eng_num, R.id.key_7_eng_num, R.id.key_8_eng_num, R.id.key_9_eng_num -> {
                    functionalKeyColor
                }
                else -> keyBackgroundColor
            }
            
            // 새로운 GradientDrawable 생성
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.setColor(backgroundColor)
            drawable.cornerRadius = radiusPx.toFloat()
            view.background = drawable
            
            // 버튼 상태 복원
            view.isClickable = wasClickable
            view.isEnabled = wasEnabled
            view.isFocusable = wasFocusable
            
            // 배경이 변경되었으므로 저장된 원래 배경 업데이트
            buttonOriginalBackgrounds[view] = drawable.constantState?.newDrawable()?.mutate()
            
            Log.d(TAG, "Applied corner radius $radiusDp dp to button: ${view.text}, clickable: $wasClickable")
        } else if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                applyKeyCornerRadiusToButtons(view.getChildAt(i), radiusDp)
            }
        }
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
            val view = layoutInflater.inflate(R.layout.keyboard_layout, null)
            Log.d(TAG, "Layout inflated successfully")
            
            // 뷰 저장
            keyboardView = view

            setupHangulEngine()
            Log.d(TAG, "HangulEngine setup complete")
            
            setupViewReferences(view)
            Log.d(TAG, "View references setup complete")
            
            // UI 스타일 먼저 적용 (리스너 설정 전에)
            applyTextSize(view)
            Log.d(TAG, "Text size applied")
            
            applyKeySpacing(view)
            Log.d(TAG, "Key spacing applied")
            
            applyKeyCornerRadius(view)
            Log.d(TAG, "Key corner radius applied")
            
            applyKeyHeight(view)
            Log.d(TAG, "Key height applied")
            
            // 리스너 설정 (UI 스타일 적용 후에)
            setupKoreanKeyboardListeners(view)
            Log.d(TAG, "Korean keyboard listeners setup complete")
            
            setupNumberKeyboardListeners(view)
            Log.d(TAG, "Number keyboard listeners setup complete")
            
            setupEnglishKeyboardListeners(view)
            Log.d(TAG, "English keyboard listeners setup complete")
            
            setupSymbolKeyboardListeners(view)
            Log.d(TAG, "Symbol keyboard listeners setup complete")
            
            setupCommonFunctionalKeys(view)
            Log.d(TAG, "Common functional keys setup complete")

            // 숫자 행 표시/숨김 및 리스너 설정
            updateNumberRowVisibility()
            Log.d(TAG, "Number row visibility updated")

            setKeyboardMode(KeyboardMode.KOREAN)
            Log.d(TAG, "Keyboard mode set to KOREAN")

            view
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
        
        // 피드백 관련 설정만 다시 로드 (UI 변경 없음)
        isSoundEnabled = SettingsActivity.isSoundEnabled(this)
        isVibrationEnabled = SettingsActivity.isVibrationEnabled(this)
        isColorEffectEnabled = SettingsActivity.isColorEffectEnabled(this)
        isScaleEffectEnabled = SettingsActivity.isScaleEffectEnabled(this)
        touchColor = SettingsActivity.getTouchColor(this)
        enterLongPressThreshold = SettingsActivity.getEnterLongPressThreshold(this)
        
        // HangulEngine 설정 업데이트
        hangulEngine?.syllableTimeoutMs = SettingsActivity.getSyllableTimeout(this)
        hangulEngine?.isCharacterCycleEnabled = SettingsActivity.isCharacterCycleEnabled(this)
        
        Log.d(TAG, "Settings reloaded in onStartInput")
    }
    
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.d(TAG, "onStartInputView called, restarting: $restarting")
        
        // 설정이 변경되어 재생성이 필요한지 확인
        val prefs = getSharedPreferences("NeoFKeyboardPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("needs_recreate", false)) {
            Log.d(TAG, "Settings changed, recreating keyboard view")
            prefs.edit().putBoolean("needs_recreate", false).apply()
            
            // 상태 초기화
            cleanupResources()
            
            // 새로운 뷰 생성 및 설정
            val newView = onCreateInputView()
            setInputView(newView)
            
            // 키보드 뷰 참조 업데이트
            keyboardView = newView
            
            Log.d(TAG, "Keyboard view recreated successfully")
            return
        }
        
        // 숫자 행 표시/숨김만 업데이트 (다른 설정은 onCreateInputView에서만 적용)
        keyboardView?.let { view ->
            updateNumberRowVisibility()
        } ?: Log.w(TAG, "keyboardView is null, cannot apply settings")
    }
    
    override fun onBindInput() {
        super.onBindInput()
        Log.d(TAG, "onBindInput called")
    }
    
    override fun onUnbindInput() {
        super.onUnbindInput()
        Log.d(TAG, "onUnbindInput called")
        // 핸들러만 정리하고 뷰는 유지
        backspaceRunnable?.let { 
            backspaceHandler.removeCallbacks(it)
            backspaceRunnable = null
        }
        enterLongPressRunnable?.let {
            enterHandler.removeCallbacks(it)
            enterLongPressRunnable = null
        }
        hangulEngine?.reset()
        composingText = ""
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        cleanupResources()
        keyboardView = null
    }
    
    private fun cleanupResources() {
        Log.d(TAG, "Cleaning up resources")
        
        // 백스페이스 핸들러 정리
        backspaceRunnable?.let { 
            backspaceHandler.removeCallbacks(it)
            backspaceRunnable = null
        }
        
        // 엔터 핸들러 정리
        enterLongPressRunnable?.let {
            enterHandler.removeCallbacks(it)
            enterLongPressRunnable = null
        }
        
        // HangulEngine 완전히 제거
        hangulEngine?.reset()
        hangulEngine = null
        composingText = ""
        
        // 버튼 참조 정리
        koreanShiftKey = null
        englishShiftKey = null
        koreanKeyboardLayout = null
        numberKeyboardLayout = null
        englishKeyboardLayout = null
        symbolKeyboardLayout = null
        numberRowKorean = null
        numberRowEnglish = null
        englishKeyButtons.clear()
        buttonOriginalBackgrounds.clear()
        
        // 상태 초기화
        isKoreanShifted = false
        isEnglishShifted = false
        currentKeyboardMode = KeyboardMode.KOREAN
        numberRowListenersSetup = false
    }

    private fun setupViewReferences(view: View) {
        koreanShiftKey = view.findViewById(R.id.key_shift)
        englishShiftKey = view.findViewById(R.id.key_shift_eng)
        koreanKeyboardLayout = view.findViewById(R.id.korean_keyboard)
        numberKeyboardLayout = view.findViewById(R.id.number_keyboard)
        englishKeyboardLayout = view.findViewById(R.id.english_keyboard)
        symbolKeyboardLayout = view.findViewById(R.id.symbol_keyboard)
        numberRowKorean = view.findViewById(R.id.number_row_korean)
        numberRowEnglish = view.findViewById(R.id.number_row_english)
    }

    private fun setupHangulEngine() {
        // 설정에서 타이머 값과 문자 순환 설정 읽기
        val syllableTimeout = SettingsActivity.getSyllableTimeout(this)
        val isCharacterCycleEnabled = SettingsActivity.isCharacterCycleEnabled(this)
        Log.d(TAG, "Syllable timeout set to: $syllableTimeout ms")
        Log.d(TAG, "Character cycle enabled: $isCharacterCycleEnabled")
        
        hangulEngine = HangulEngine({ result ->
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
        }, syllableTimeout, isCharacterCycleEnabled)
    }

    private fun setupKoreanKeyboardListeners(view: View) {
        val keyButtons = mapOf(
            R.id.key_g to 'ㄱ', R.id.key_n to 'ㄴ', R.id.key_d to 'ㄷ', R.id.key_a to 'ㅏ', R.id.key_eo to 'ㅓ',
            R.id.key_r to 'ㄹ', R.id.key_m to 'ㅁ', R.id.key_b to 'ㅂ', R.id.key_o to 'ㅡ', R.id.key_u to 'ㅣ',
            R.id.key_s to 'ㅅ', R.id.key_ng to 'ㅇ', R.id.key_j to 'ㅈ', R.id.key_eu to 'ㅗ', R.id.key_i to 'ㅜ'
        )

        keyButtons.forEach { (id, char) ->
            val button = view.findViewById<Button?>(id)
            setupButtonWithFeedback(button) {
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

    private fun setupNumberKeyboardListeners(view: View) {
        // 숫자 및 특수문자 키
        val numberKeys = mapOf(
            R.id.key_1_num to "1", R.id.key_2_num to "2", R.id.key_3_num to "3",
            R.id.key_star_num to "*", R.id.key_plus_num to "+", R.id.key_slash_num to "/",
            R.id.key_4_num to "4", R.id.key_5_num to "5", R.id.key_6_num to "6",
            R.id.key_hash_num to "#", R.id.key_minus_num to "-", R.id.key_equal_num to "=",
            R.id.key_7_num to "7", R.id.key_8_num to "8", R.id.key_9_num to "9",
            R.id.key_0_num to "0", R.id.key_at_num to "@", R.id.key_period_num to "."
        )

        numberKeys.forEach { (id, text) ->
            setupButtonWithFeedback(view.findViewById(id)) {
                currentInputConnection?.commitText(text, 1)
            }
        }

        // 스페이스 바
        setupButtonWithFeedback(view.findViewById(R.id.key_space_num)) {
            currentInputConnection?.commitText(" ", 1)
        }

        // 백스페이스 버튼
        view.findViewById<Button?>(R.id.key_backspace_num)?.setOnTouchListener { _, event ->
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

        // 검색 버튼 (돋보기)
        setupButtonWithFeedback(view.findViewById(R.id.key_search_num)) {
            sendEnterAction()
        }
    }

    private fun setupEnglishKeyboardListeners(view: View) {
        val keyIds = ('a'..'z').map { "key_${it}_eng" }
        keyIds.forEach { idName ->
            val id = resources.getIdentifier(idName, "id", packageName)
            if (id != 0) {
                val button = view.findViewById<Button?>(id)
                englishKeyButtons[id] = button
                setupButtonWithFeedback(button) {
                    var text = button?.text.toString()
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

        setupButtonWithFeedback(view.findViewById(R.id.key_shift_eng)) {
            isEnglishShifted = !isEnglishShifted
        }
        
        // @ 버튼
        setupButtonWithFeedback(view.findViewById(R.id.key_at_eng)) {
            currentInputConnection?.commitText("@", 1)
        }
        
        // .? 버튼
        setupButtonWithFeedback(view.findViewById(R.id.key_period_eng)) {
            if (isEnglishShifted) {
                currentInputConnection?.commitText("?", 1)
                isEnglishShifted = false
            } else {
                currentInputConnection?.commitText(".", 1)
            }
        }
        
        // ,! 버튼
        setupButtonWithFeedback(view.findViewById(R.id.key_comma_eng)) {
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
            setupButtonWithFeedback(view.findViewById(id)) {
                currentInputConnection?.commitText(text, 1)
            }
        }

        setupButtonWithFeedback(view.findViewById(R.id.key_space_sym)) {
            currentInputConnection?.commitText(" ", 1)
        }

        // 심볼 키보드의 엔터키도 동일하게 처리
        val enterTouchListenerSym = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    enterPressStartTime = System.currentTimeMillis()
                    enterLongPressRunnable = Runnable {
                        playKeySound()
                        if (v is Button) applyTouchFeedback(v)
                        sendEnterAction()
                    }.also {
                        enterHandler.postDelayed(it, enterLongPressThreshold)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val pressDuration = System.currentTimeMillis() - enterPressStartTime
                    enterLongPressRunnable?.let { 
                        enterHandler.removeCallbacks(it)
                        enterLongPressRunnable = null
                    }
                    
                    if (event.action == MotionEvent.ACTION_UP && pressDuration < enterLongPressThreshold) {
                        playKeySound()
                        if (v is Button) applyTouchFeedback(v)
                        sendNewLine()
                    }
                    true
                }
                else -> false
            }
        }
        view.findViewById<Button?>(R.id.key_enter_sym)?.setOnTouchListener(enterTouchListenerSym)
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

        // 엔터키 - 짧게 누르면 줄바꿈, 길게 누르면 액션 실행
        val enterTouchListener = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    enterPressStartTime = System.currentTimeMillis()
                    enterLongPressRunnable = Runnable {
                        // 길게 누름 - 액션 실행 (보내기, 검색 등)
                        playKeySound()
                        if (v is Button) applyTouchFeedback(v)
                        sendEnterAction()
                    }.also {
                        enterHandler.postDelayed(it, enterLongPressThreshold)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val pressDuration = System.currentTimeMillis() - enterPressStartTime
                    enterLongPressRunnable?.let { 
                        enterHandler.removeCallbacks(it)
                        enterLongPressRunnable = null
                    }
                    
                    // 짧게 누름 - 줄바꿈
                    if (event.action == MotionEvent.ACTION_UP && pressDuration < enterLongPressThreshold) {
                        playKeySound()
                        if (v is Button) applyTouchFeedback(v)
                        sendNewLine()
                    }
                    true
                }
                else -> false
            }
        }
        view.findViewById<Button?>(R.id.key_enter_eng)?.setOnTouchListener(enterTouchListener)

        // 한글 키보드의 검색 버튼
        setupButtonWithFeedback(view.findViewById(R.id.key_search_kor)) {
            sendEnterAction()
        }

        setupButtonWithFeedback(view.findViewById(R.id.key_space)) {
            if (currentKeyboardMode == KeyboardMode.KOREAN) {
                hangulEngine?.processKey(' ')
            } else {
                currentInputConnection?.commitText(" ", 1)
            }
        }
        setupButtonWithFeedback(view.findViewById(R.id.key_space_eng)) {
            if (currentKeyboardMode == KeyboardMode.KOREAN) {
                hangulEngine?.processKey(' ')
            } else {
                currentInputConnection?.commitText(" ", 1)
            }
        }

        // 한글 키보드의 자판 전환 버튼
        val modeKorButton = view.findViewById<Button>(R.id.key_mode_kor)
        Log.d(TAG, "key_mode_kor button: $modeKorButton")
        setupButtonWithFeedback(modeKorButton) {
            Log.d(TAG, "Mode button clicked, current: $currentKeyboardMode")
            val nextMode = when (currentKeyboardMode) {
                KeyboardMode.KOREAN -> KeyboardMode.NUMBER
                KeyboardMode.NUMBER -> KeyboardMode.ENGLISH
                KeyboardMode.ENGLISH -> KeyboardMode.SYMBOL
                KeyboardMode.SYMBOL -> KeyboardMode.KOREAN
            }
            Log.d(TAG, "Switching to: $nextMode")
            setKeyboardMode(nextMode)
        }

        // 숫자 키보드의 자판 전환 버튼
        val modeNumButton = view.findViewById<Button>(R.id.key_mode_num)
        Log.d(TAG, "key_mode_num button: $modeNumButton")
        setupButtonWithFeedback(modeNumButton) {
            Log.d(TAG, "Mode button clicked, current: $currentKeyboardMode")
            val nextMode = when (currentKeyboardMode) {
                KeyboardMode.KOREAN -> KeyboardMode.NUMBER
                KeyboardMode.NUMBER -> KeyboardMode.ENGLISH
                KeyboardMode.ENGLISH -> KeyboardMode.SYMBOL
                KeyboardMode.SYMBOL -> KeyboardMode.KOREAN
            }
            Log.d(TAG, "Switching to: $nextMode")
            setKeyboardMode(nextMode)
        }

        // 영어 키보드의 자판 전환 버튼
        val modeEngButton = view.findViewById<Button>(R.id.key_mode_eng)
        Log.d(TAG, "key_mode_eng button: $modeEngButton")
        setupButtonWithFeedback(modeEngButton) {
            Log.d(TAG, "Mode button clicked, current: $currentKeyboardMode")
            val nextMode = when (currentKeyboardMode) {
                KeyboardMode.KOREAN -> KeyboardMode.NUMBER
                KeyboardMode.NUMBER -> KeyboardMode.ENGLISH
                KeyboardMode.ENGLISH -> KeyboardMode.SYMBOL
                KeyboardMode.SYMBOL -> KeyboardMode.KOREAN
            }
            Log.d(TAG, "Switching to: $nextMode")
            setKeyboardMode(nextMode)
        }

        // 심볼 키보드의 자판 전환 버튼
        val modeSymButton = view.findViewById<Button>(R.id.key_mode_sym)
        Log.d(TAG, "key_mode_sym button: $modeSymButton")
        setupButtonWithFeedback(modeSymButton) {
            Log.d(TAG, "Mode button clicked, current: $currentKeyboardMode")
            val nextMode = when (currentKeyboardMode) {
                KeyboardMode.KOREAN -> KeyboardMode.NUMBER
                KeyboardMode.NUMBER -> KeyboardMode.ENGLISH
                KeyboardMode.ENGLISH -> KeyboardMode.SYMBOL
                KeyboardMode.SYMBOL -> KeyboardMode.KOREAN
            }
            Log.d(TAG, "Switching to: $nextMode")
            setKeyboardMode(nextMode)
        }

        // 숫자 키보드의 한글 전환 버튼
        val hangulNumButton = view.findViewById<Button>(R.id.key_hangul_num)
        Log.d(TAG, "key_hangul_num button: $hangulNumButton")
        setupButtonWithFeedback(hangulNumButton) {
            Log.d(TAG, "Hangul button clicked, switching to KOREAN")
            setKeyboardMode(KeyboardMode.KOREAN)
        }

        // 한글 키보드의 ㄲ 버튼 (된소리 변환)
        setupButtonWithFeedback(koreanShiftKey) {
            hangulEngine?.applyDoubleConsonant()
        }
    }

    private fun performSingleBackspace() {
        val ic = currentInputConnection ?: return
        
        if (currentKeyboardMode == KeyboardMode.KOREAN) {
            val newComposing = hangulEngine?.backspace() ?: "BACKSPACE"
            if (newComposing == "BACKSPACE") {
                // 조합 중인 문자가 없으면 이전 문자 삭제
                ic.deleteSurroundingText(1, 0)
                composingText = ""
            } else {
                // 조합 중인 문자 처리
                if (composingText.isNotEmpty()) {
                    // 기존 조합 문자 제거
                    ic.finishComposingText()
                    ic.deleteSurroundingText(composingText.length, 0)
                }
                
                if (newComposing.isNotEmpty()) {
                    // 새로운 조합 문자 설정
                    ic.setComposingText(newComposing, 1)
                    composingText = newComposing
                } else {
                    // 조합이 완전히 끝난 경우 (초성만 있다가 지워진 경우)
                    composingText = ""
                }
            }
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }

    private fun sendNewLine() {
        Log.d(TAG, "sendNewLine called")
        val ic = currentInputConnection ?: return
        hangulEngine?.reset()
        composingText = ""
        Log.d(TAG, "Committing newline character")
        ic.commitText("\n", 1)
    }
    
    private fun sendEnterAction() {
        Log.d(TAG, "sendEnterAction called")
        val ic = currentInputConnection ?: return
        hangulEngine?.reset()
        composingText = ""
        
        val editorInfo = currentInputEditorInfo
        if (editorInfo != null && (editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
            when (editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    Log.d(TAG, "Performing SEARCH action")
                    ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
                }
                EditorInfo.IME_ACTION_SEND -> {
                    Log.d(TAG, "Performing SEND action")
                    ic.performEditorAction(EditorInfo.IME_ACTION_SEND)
                }
                EditorInfo.IME_ACTION_GO -> {
                    Log.d(TAG, "Performing GO action")
                    ic.performEditorAction(EditorInfo.IME_ACTION_GO)
                }
                EditorInfo.IME_ACTION_NEXT -> {
                    Log.d(TAG, "Performing NEXT action")
                    ic.performEditorAction(EditorInfo.IME_ACTION_NEXT)
                }
                EditorInfo.IME_ACTION_DONE -> {
                    Log.d(TAG, "Performing DONE action")
                    ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
                }
                else -> {
                    Log.d(TAG, "No specific action, committing newline")
                    ic.commitText("\n", 1)
                }
            }
        } else {
            Log.d(TAG, "No editor action, committing newline")
            ic.commitText("\n", 1)
        }
    }

    private fun setKeyboardMode(mode: KeyboardMode) {
        Log.d(TAG, "setKeyboardMode called: $mode")
        if (mode != KeyboardMode.SYMBOL && mode != KeyboardMode.NUMBER) {
            lastAlphabetMode = mode
        }
        currentKeyboardMode = mode

        Log.d(TAG, "Korean layout: $koreanKeyboardLayout, Number: $numberKeyboardLayout, English: $englishKeyboardLayout, Symbol: $symbolKeyboardLayout")
        
        koreanKeyboardLayout?.visibility = if (mode == KeyboardMode.KOREAN) View.VISIBLE else View.GONE
        numberKeyboardLayout?.visibility = if (mode == KeyboardMode.NUMBER) View.VISIBLE else View.GONE
        englishKeyboardLayout?.visibility = if (mode == KeyboardMode.ENGLISH) View.VISIBLE else View.GONE
        symbolKeyboardLayout?.visibility = if (mode == KeyboardMode.SYMBOL) View.VISIBLE else View.GONE

        Log.d(TAG, "Visibility set - Korean: ${koreanKeyboardLayout?.visibility}, Number: ${numberKeyboardLayout?.visibility}, English: ${englishKeyboardLayout?.visibility}, Symbol: ${symbolKeyboardLayout?.visibility}")

        hangulEngine?.reset()
        composingText = ""
        isKoreanShifted = false
        isEnglishShifted = false
    }

    override fun onFinishInput() {
        super.onFinishInput()
        Log.d(TAG, "onFinishInput called")
        
        // 백스페이스 핸들러 정리
        backspaceRunnable?.let { 
            backspaceHandler.removeCallbacks(it)
            backspaceRunnable = null
        }
        
        hangulEngine?.reset()
        composingText = ""
    }
    
    private fun playKeySound() {
        if (isSoundEnabled) {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.5f)
        }
    }
    
    private val buttonOriginalBackgrounds = mutableMapOf<Button, android.graphics.drawable.Drawable?>()
    
    private fun applyTouchFeedback(button: Button) {
        // 진동 피드백
        if (isVibrationEnabled) {
            button.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        
        // 색상 효과
        if (isColorEffectEnabled) {
            // 원래 배경이 저장되어 있지 않으면 저장
            if (!buttonOriginalBackgrounds.containsKey(button)) {
                buttonOriginalBackgrounds[button] = button.background?.constantState?.newDrawable()?.mutate()
            }
            
            val originalBg = buttonOriginalBackgrounds[button]
            
            // 터치 색상 적용 - GradientDrawable을 복사하여 색상만 변경
            if (originalBg is android.graphics.drawable.GradientDrawable) {
                val touchDrawable = originalBg.constantState?.newDrawable()?.mutate() as? android.graphics.drawable.GradientDrawable
                touchDrawable?.setColor(touchColor)
                button.background = touchDrawable
            } else {
                button.setBackgroundColor(touchColor)
            }
            
            // 100ms 후 원래 배경으로 복원
            button.postDelayed({
                if (originalBg != null) {
                    button.background = originalBg.constantState?.newDrawable()?.mutate()
                }
            }, 100)
        }
        
        // 크기 효과
        if (isScaleEffectEnabled) {
            button.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(50)
                .withEndAction {
                    button.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(50)
                        .start()
                }
                .start()
        }
    }
    
    private fun setupButtonWithFeedback(button: Button?, action: () -> Unit) {
        if (button == null) {
            Log.w(TAG, "setupButtonWithFeedback called with null button")
            return
        }
        
        button.setOnClickListener {
            Log.d(TAG, "Button clicked: ${button.text}")
            try {
                applyTouchFeedback(it as Button)
                playKeySound()
                action()
            } catch (e: Exception) {
                Log.e(TAG, "Error in button click handler", e)
            }
        }
    }
    
    private var numberRowListenersSetup = false
    
    private fun updateNumberRowVisibility() {
        val showNumberRow = SettingsActivity.isShowNumberRow(this)
        Log.d(TAG, "=== updateNumberRowVisibility START ===")
        Log.d(TAG, "showNumberRow: $showNumberRow")
        Log.d(TAG, "numberRowKorean: $numberRowKorean")
        Log.d(TAG, "numberRowEnglish: $numberRowEnglish")
        Log.d(TAG, "numberRowListenersSetup: $numberRowListenersSetup")
        
        if (numberRowKorean == null || numberRowEnglish == null) {
            Log.e(TAG, "Number row views are null! Trying to find them again...")
            keyboardView?.let { view ->
                numberRowKorean = view.findViewById(R.id.number_row_korean)
                numberRowEnglish = view.findViewById(R.id.number_row_english)
                Log.d(TAG, "After re-finding - Korean: $numberRowKorean, English: $numberRowEnglish")
            }
        }
        
        numberRowKorean?.visibility = if (showNumberRow) View.VISIBLE else View.GONE
        numberRowEnglish?.visibility = if (showNumberRow) View.VISIBLE else View.GONE
        
        Log.d(TAG, "Visibility set - Korean: ${numberRowKorean?.visibility}, English: ${numberRowEnglish?.visibility}")
        
        // 숫자 행 버튼 리스너 설정
        if (showNumberRow) {
            if (!numberRowListenersSetup) {
                Log.d(TAG, "Setting up number row listeners for the first time")
                setupNumberRowListeners()
                numberRowListenersSetup = true
            } else {
                Log.d(TAG, "Number row listeners already set up")
            }
        }
        
        Log.d(TAG, "=== updateNumberRowVisibility END ===")
    }
    
    private fun setupNumberRowListeners() {
        Log.d(TAG, "Setting up number row listeners")
        
        // 한글 키보드 숫자 행
        for (i in 0..9) {
            val korId = resources.getIdentifier("key_${i}_kor", "id", packageName)
            if (korId != 0) {
                val button = keyboardView?.findViewById<Button>(korId)
                Log.d(TAG, "Korean number button $i: $button")
                setupButtonWithFeedback(button) {
                    Log.d(TAG, "Number $i clicked (Korean)")
                    currentInputConnection?.commitText(i.toString(), 1)
                }
            } else {
                Log.w(TAG, "Korean number button $i not found")
            }
        }
        
        // 영어 키보드 숫자 행
        for (i in 0..9) {
            val engId = resources.getIdentifier("key_${i}_eng_num", "id", packageName)
            if (engId != 0) {
                val button = keyboardView?.findViewById<Button>(engId)
                Log.d(TAG, "English number button $i: $button")
                setupButtonWithFeedback(button) {
                    Log.d(TAG, "Number $i clicked (English)")
                    currentInputConnection?.commitText(i.toString(), 1)
                }
            } else {
                Log.w(TAG, "English number button $i not found")
            }
        }
        
        Log.d(TAG, "Number row listeners setup complete")
    }
}
