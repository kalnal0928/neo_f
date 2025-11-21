package com.example.neo_f

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment

class InputSettingsFragment : Fragment() {

    private lateinit var timerSeekBar: SeekBar
    private lateinit var timerValueText: TextView
    private lateinit var presetFast: Button
    private lateinit var presetNormal: Button
    private lateinit var presetSlow: Button
    private lateinit var switchCharacterCycle: androidx.appcompat.widget.SwitchCompat
    private lateinit var enterThresholdSeekBar: SeekBar
    private lateinit var enterThresholdValueText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.settings_tab_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        timerSeekBar = view.findViewById(R.id.timer_seekbar)
        timerValueText = view.findViewById(R.id.timer_value_text)
        presetFast = view.findViewById(R.id.preset_fast)
        presetNormal = view.findViewById(R.id.preset_normal)
        presetSlow = view.findViewById(R.id.preset_slow)
        switchCharacterCycle = view.findViewById(R.id.switch_character_cycle)
        enterThresholdSeekBar = view.findViewById(R.id.enter_threshold_seekbar)
        enterThresholdValueText = view.findViewById(R.id.enter_threshold_value)

        // 현재 설정 불러오기
        val currentTimeout = SettingsActivity.getSyllableTimeout(requireContext())
        val currentCharacterCycleEnabled = SettingsActivity.isCharacterCycleEnabled(requireContext())
        val currentEnterThreshold = SettingsActivity.getEnterLongPressThreshold(requireContext())

        updateSeekBarFromTimeout(currentTimeout)
        switchCharacterCycle.isChecked = currentCharacterCycleEnabled
        updateEnterThresholdSeekBar(currentEnterThreshold)

        // 음절 타이머 SeekBar 리스너
        timerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val timeoutMs = (progress + 100).toLong()
                timerValueText.text = "$timeoutMs ms"
                if (fromUser) {
                    SettingsActivity.setSyllableTimeout(requireContext(), timeoutMs)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 프리셋 버튼 리스너
        presetFast.setOnClickListener {
            val timeout = 150L
            SettingsActivity.setSyllableTimeout(requireContext(), timeout)
            updateSeekBarFromTimeout(timeout)
        }

        presetNormal.setOnClickListener {
            val timeout = 300L
            SettingsActivity.setSyllableTimeout(requireContext(), timeout)
            updateSeekBarFromTimeout(timeout)
        }

        presetSlow.setOnClickListener {
            val timeout = 500L
            SettingsActivity.setSyllableTimeout(requireContext(), timeout)
            updateSeekBarFromTimeout(timeout)
        }

        // 문자 순환 스위치 리스너
        switchCharacterCycle.setOnCheckedChangeListener { _, isChecked ->
            SettingsActivity.setCharacterCycleEnabled(requireContext(), isChecked)
        }

        // 엔터키 SeekBar 리스너
        enterThresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val thresholdMs = (progress + 200).toLong()
                enterThresholdValueText.text = "$thresholdMs ms"
                if (fromUser) {
                    SettingsActivity.setEnterLongPressThreshold(requireContext(), thresholdMs)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateSeekBarFromTimeout(timeoutMs: Long) {
        val progress = (timeoutMs - 100).toInt().coerceIn(0, 900)
        timerSeekBar.progress = progress
        timerValueText.text = "$timeoutMs ms"
    }

    private fun updateEnterThresholdSeekBar(thresholdMs: Long) {
        val progress = (thresholdMs - 200).toInt().coerceIn(0, 800)
        enterThresholdSeekBar.progress = progress
        enterThresholdValueText.text = "$thresholdMs ms"
    }
}
