package com.example.neo_f

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var timerSeekBar: SeekBar
    private lateinit var timerValueText: TextView
    private lateinit var presetFast: Button
    private lateinit var presetNormal: Button
    private lateinit var presetSlow: Button
    private lateinit var soundSwitch: SwitchCompat
    private lateinit var textSizeSeekBar: SeekBar
    private lateinit var textSizeValueText: TextView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnReset: Button
    
    // 임시 설정값 (저장 전)
    private var tempSyllableTimeout: Long = 0
    private var tempSoundEnabled: Boolean = true
    private var tempTextSize: Float = 0f
    
    // 원본 설정값 (취소 시 복원용)
    private var originalSyllableTimeout: Long = 0
    private var originalSoundEnabled: Boolean = true
    private var originalTextSize: Float = 0f

    companion object {
        private const val PREFS_NAME = "NeoFKeyboardPrefs"
        private const val KEY_SYLLABLE_TIMEOUT = "syllable_timeout_ms"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_TEXT_SIZE = "text_size_sp"
        private const val DEFAULT_TIMEOUT = 300L
        private const val DEFAULT_SOUND_ENABLED = true
        private const val DEFAULT_TEXT_SIZE = 18f

        fun getSyllableTimeout(context: Context): Long {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getLong(KEY_SYLLABLE_TIMEOUT, DEFAULT_TIMEOUT)
        }

        fun setSyllableTimeout(context: Context, timeoutMs: Long) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_SYLLABLE_TIMEOUT, timeoutMs).apply()
        }

        fun isSoundEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED)
        }

        fun setSoundEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        }

        fun getTextSize(context: Context): Float {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getFloat(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE)
        }

        fun setTextSize(context: Context, textSize: Float) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putFloat(KEY_TEXT_SIZE, textSize).apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 뷰 초기화
        timerSeekBar = findViewById(R.id.timer_seekbar)
        timerValueText = findViewById(R.id.timer_value_text)
        presetFast = findViewById(R.id.preset_fast)
        presetNormal = findViewById(R.id.preset_normal)
        presetSlow = findViewById(R.id.preset_slow)
        soundSwitch = findViewById(R.id.sound_switch)
        textSizeSeekBar = findViewById(R.id.text_size_seekbar)
        textSizeValueText = findViewById(R.id.text_size_value)
        btnSave = findViewById(R.id.btn_save)
        btnCancel = findViewById(R.id.btn_cancel)
        btnReset = findViewById(R.id.btn_reset)

        // 현재 설정 불러오기 및 백업
        originalSyllableTimeout = getSyllableTimeout(this)
        originalSoundEnabled = isSoundEnabled(this)
        originalTextSize = getTextSize(this)
        
        tempSyllableTimeout = originalSyllableTimeout
        tempSoundEnabled = originalSoundEnabled
        tempTextSize = originalTextSize
        
        // 리스너 설정 전에 초기값 설정
        updateSeekBarFromTimeout(tempSyllableTimeout)
        updateTextSizeSeekBar(tempTextSize)
        soundSwitch.setOnCheckedChangeListener(null) // 리스너 제거
        soundSwitch.isChecked = tempSoundEnabled

        // SeekBar 리스너 설정
        timerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 100ms ~ 1000ms 범위로 변환
                val timeoutMs = (progress + 100).toLong()
                timerValueText.text = "$timeoutMs ms"
                if (fromUser) {
                    tempSyllableTimeout = timeoutMs
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 프리셋 버튼 리스너
        presetFast.setOnClickListener {
            tempSyllableTimeout = 150L
            updateSeekBarFromTimeout(tempSyllableTimeout)
        }

        presetNormal.setOnClickListener {
            tempSyllableTimeout = 300L
            updateSeekBarFromTimeout(tempSyllableTimeout)
        }

        presetSlow.setOnClickListener {
            tempSyllableTimeout = 500L
            updateSeekBarFromTimeout(tempSyllableTimeout)
        }

        // 터치음 스위치 리스너
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            tempSoundEnabled = isChecked
        }

        // 글자 크기 SeekBar 리스너
        textSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 12sp ~ 32sp 범위로 변환
                val textSize = (progress + 12).toFloat()
                textSizeValueText.text = "${textSize.toInt()} sp"
                if (fromUser) {
                    tempTextSize = textSize
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 저장 버튼
        btnSave.setOnClickListener {
            saveSettings()
            finish()
        }
        
        // 취소 버튼
        btnCancel.setOnClickListener {
            finish()
        }
        
        // 기본값 버튼
        btnReset.setOnClickListener {
            resetToDefaults()
        }
    }
    
    private fun saveSettings() {
        setSyllableTimeout(this, tempSyllableTimeout)
        setSoundEnabled(this, tempSoundEnabled)
        setTextSize(this, tempTextSize)
        
        android.widget.Toast.makeText(this, "설정이 저장되었습니다", android.widget.Toast.LENGTH_SHORT).show()
    }
    
    private fun resetToDefaults() {
        tempSyllableTimeout = DEFAULT_TIMEOUT
        tempSoundEnabled = DEFAULT_SOUND_ENABLED
        tempTextSize = DEFAULT_TEXT_SIZE
        
        updateSeekBarFromTimeout(tempSyllableTimeout)
        soundSwitch.isChecked = tempSoundEnabled
        updateTextSizeSeekBar(tempTextSize)
        
        android.widget.Toast.makeText(this, "기본값으로 복원되었습니다", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun updateTextSizeSeekBar(textSize: Float) {
        // 12sp ~ 32sp 범위를 0 ~ 20 progress로 변환
        val progress = (textSize - 12).toInt().coerceIn(0, 20)
        textSizeSeekBar.progress = progress
        textSizeValueText.text = "${textSize.toInt()} sp"
    }

    private fun updateSeekBarFromTimeout(timeoutMs: Long) {
        // 100ms ~ 1000ms 범위를 0 ~ 900 progress로 변환
        val progress = (timeoutMs - 100).toInt().coerceIn(0, 900)
        timerSeekBar.progress = progress
        timerValueText.text = "$timeoutMs ms"
    }


}
