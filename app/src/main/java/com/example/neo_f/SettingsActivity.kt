package com.example.neo_f

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class SettingsActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnReset: Button

    companion object {
        private const val PREFS_NAME = "NeoFKeyboardPrefs"
        private const val KEY_SYLLABLE_TIMEOUT = "syllable_timeout_ms"
        private const val KEY_CHARACTER_CYCLE_ENABLED = "character_cycle_enabled"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_TEXT_SIZE = "text_size_sp"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_COLOR_EFFECT_ENABLED = "color_effect_enabled"
        private const val KEY_SCALE_EFFECT_ENABLED = "scale_effect_enabled"
        private const val KEY_TOUCH_COLOR = "touch_color"
        private const val KEY_ENTER_LONG_PRESS_THRESHOLD = "enter_long_press_threshold"
        private const val KEY_SHOW_NUMBER_ROW = "show_number_row"
        private const val KEY_KEY_SPACING = "key_spacing"
        private const val KEY_KEY_CORNER_RADIUS = "key_corner_radius"
        private const val KEY_KEY_HEIGHT = "key_height"
        private const val KEY_TEXT_COLOR = "text_color"
        private const val KEY_KEY_BACKGROUND_COLOR = "key_background_color"
        private const val KEY_FUNCTIONAL_KEY_COLOR = "functional_key_color"
        
        private const val DEFAULT_TIMEOUT = 300L
        private const val DEFAULT_CHARACTER_CYCLE_ENABLED = true
        private const val DEFAULT_SOUND_ENABLED = true
        private const val DEFAULT_TEXT_SIZE = 18f
        private const val DEFAULT_VIBRATION_ENABLED = true
        private const val DEFAULT_COLOR_EFFECT_ENABLED = true
        private const val DEFAULT_SCALE_EFFECT_ENABLED = true
        private const val DEFAULT_TOUCH_COLOR = 0xFF4CAF50.toInt()
        private const val DEFAULT_ENTER_LONG_PRESS_THRESHOLD = 500L
        private const val DEFAULT_SHOW_NUMBER_ROW = false
        private const val DEFAULT_KEY_SPACING = 2
        private const val DEFAULT_KEY_CORNER_RADIUS = 4
        private const val DEFAULT_KEY_HEIGHT = 48  // 기본 키 높이 (dp)
        private const val DEFAULT_TEXT_COLOR = 0xFFFFFFFF.toInt()  // 흰색
        private const val DEFAULT_KEY_BACKGROUND_COLOR = 0xFF424242.toInt()  // 회색
        private const val DEFAULT_FUNCTIONAL_KEY_COLOR = 0xFF616161.toInt()  // 진한 회색

        fun getSyllableTimeout(context: Context): Long {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getLong(KEY_SYLLABLE_TIMEOUT, DEFAULT_TIMEOUT)
        }

        fun setSyllableTimeout(context: Context, timeoutMs: Long) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_SYLLABLE_TIMEOUT, timeoutMs).apply()
        }

        fun isCharacterCycleEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_CHARACTER_CYCLE_ENABLED, DEFAULT_CHARACTER_CYCLE_ENABLED)
        }

        fun setCharacterCycleEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_CHARACTER_CYCLE_ENABLED, enabled).apply()
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
            // UI 변경이 필요한 설정이므로 재생성 플래그 설정
            prefs.edit().putBoolean("needs_recreate", true).apply()
        }

        fun isVibrationEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_VIBRATION_ENABLED, DEFAULT_VIBRATION_ENABLED)
        }

        fun setVibrationEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
        }

        fun isColorEffectEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_COLOR_EFFECT_ENABLED, DEFAULT_COLOR_EFFECT_ENABLED)
        }

        fun setColorEffectEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_COLOR_EFFECT_ENABLED, enabled).apply()
        }

        fun isScaleEffectEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_SCALE_EFFECT_ENABLED, DEFAULT_SCALE_EFFECT_ENABLED)
        }

        fun setScaleEffectEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_SCALE_EFFECT_ENABLED, enabled).apply()
        }

        fun getTouchColor(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_TOUCH_COLOR, DEFAULT_TOUCH_COLOR)
        }

        fun setTouchColor(context: Context, color: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_TOUCH_COLOR, color).apply()
            // 터치 색상은 재생성 없이도 적용 가능하지만, 일관성을 위해 플래그 설정
            prefs.edit().putBoolean("needs_recreate", true).apply()
        }

        fun getEnterLongPressThreshold(context: Context): Long {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getLong(KEY_ENTER_LONG_PRESS_THRESHOLD, DEFAULT_ENTER_LONG_PRESS_THRESHOLD)
        }

        fun setEnterLongPressThreshold(context: Context, thresholdMs: Long) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_ENTER_LONG_PRESS_THRESHOLD, thresholdMs).apply()
        }

        fun isShowNumberRow(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_SHOW_NUMBER_ROW, DEFAULT_SHOW_NUMBER_ROW)
        }

        fun setShowNumberRow(context: Context, show: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_SHOW_NUMBER_ROW, show).apply()
        }

        fun getKeySpacing(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_KEY_SPACING, DEFAULT_KEY_SPACING)
        }

        fun setKeySpacing(context: Context, spacing: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_KEY_SPACING, spacing).apply()
            // UI 변경이 필요한 설정이므로 재생성 플래그 설정
            prefs.edit().putBoolean("needs_recreate", true).apply()
        }

        fun getKeyCornerRadius(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_KEY_CORNER_RADIUS, DEFAULT_KEY_CORNER_RADIUS)
        }

        fun setKeyCornerRadius(context: Context, radius: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_KEY_CORNER_RADIUS, radius).apply()
            // UI 변경이 필요한 설정이므로 재생성 플래그 설정
            prefs.edit().putBoolean("needs_recreate", true).apply()
        }

        fun getTextColor(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_TEXT_COLOR, DEFAULT_TEXT_COLOR)
        }

        fun setTextColor(context: Context, color: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_TEXT_COLOR, color).apply()
            prefs.edit().putBoolean("needs_recreate", true).apply()
        }

        fun getKeyBackgroundColor(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_KEY_BACKGROUND_COLOR, DEFAULT_KEY_BACKGROUND_COLOR)
        }

        fun setKeyBackgroundColor(context: Context, color: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_KEY_BACKGROUND_COLOR, color).apply()
            prefs.edit().putBoolean("needs_recreate", true).apply()
        }

        fun getFunctionalKeyColor(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_FUNCTIONAL_KEY_COLOR, DEFAULT_FUNCTIONAL_KEY_COLOR)
        }

        fun setFunctionalKeyColor(context: Context, color: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_FUNCTIONAL_KEY_COLOR, color).apply()
            prefs.edit().putBoolean("needs_recreate", true).apply()
        }

        fun getKeyHeight(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_KEY_HEIGHT, DEFAULT_KEY_HEIGHT)
        }

        fun setKeyHeight(context: Context, height: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_KEY_HEIGHT, height).apply()
            prefs.edit().putBoolean("needs_recreate", true).apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        btnSave = findViewById(R.id.btn_save)
        btnCancel = findViewById(R.id.btn_cancel)
        btnReset = findViewById(R.id.btn_reset)

        // ViewPager 어댑터 설정
        val adapter = SettingsPagerAdapter(this)
        viewPager.adapter = adapter

        // TabLayout과 ViewPager 연결
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "입력"
                1 -> "디자인"
                2 -> "피드백"
                3 -> "정보"
                else -> ""
            }
        }.attach()

        // 버튼 리스너
        btnSave.setOnClickListener {
            saveAllSettings()
            finish()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnReset.setOnClickListener {
            resetToDefaults()
        }
    }

    private fun saveAllSettings() {
        // 각 프래그먼트에서 설정 저장
        android.widget.Toast.makeText(this, "설정이 저장되었습니다", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun resetToDefaults() {
        setSyllableTimeout(this, DEFAULT_TIMEOUT)
        setCharacterCycleEnabled(this, DEFAULT_CHARACTER_CYCLE_ENABLED)
        setSoundEnabled(this, DEFAULT_SOUND_ENABLED)
        setTextSize(this, DEFAULT_TEXT_SIZE)
        setVibrationEnabled(this, DEFAULT_VIBRATION_ENABLED)
        setColorEffectEnabled(this, DEFAULT_COLOR_EFFECT_ENABLED)
        setScaleEffectEnabled(this, DEFAULT_SCALE_EFFECT_ENABLED)
        setTouchColor(this, DEFAULT_TOUCH_COLOR)
        setEnterLongPressThreshold(this, DEFAULT_ENTER_LONG_PRESS_THRESHOLD)
        setShowNumberRow(this, DEFAULT_SHOW_NUMBER_ROW)
        setKeySpacing(this, DEFAULT_KEY_SPACING)
        setKeyCornerRadius(this, DEFAULT_KEY_CORNER_RADIUS)
        setKeyHeight(this, DEFAULT_KEY_HEIGHT)
        setTextColor(this, DEFAULT_TEXT_COLOR)
        setKeyBackgroundColor(this, DEFAULT_KEY_BACKGROUND_COLOR)
        setFunctionalKeyColor(this, DEFAULT_FUNCTIONAL_KEY_COLOR)
        
        // 프래그먼트 새로고침
        recreate()
        
        android.widget.Toast.makeText(this, "기본값으로 복원되었습니다", android.widget.Toast.LENGTH_SHORT).show()
    }

    private class SettingsPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> InputSettingsFragment()
                1 -> DesignSettingsFragment()
                2 -> FeedbackSettingsFragment()
                3 -> AboutSettingsFragment()
                else -> InputSettingsFragment()
            }
        }
    }
}
