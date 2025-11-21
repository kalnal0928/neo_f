package com.example.neo_f

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment

class FeedbackSettingsFragment : Fragment() {

    private lateinit var soundSwitch: SwitchCompat
    private lateinit var vibrationSwitch: SwitchCompat
    private lateinit var colorEffectSwitch: SwitchCompat
    private lateinit var scaleEffectSwitch: SwitchCompat
    private lateinit var btnColorPicker: Button
    private lateinit var colorPreview: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.settings_tab_feedback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        soundSwitch = view.findViewById(R.id.sound_switch)
        vibrationSwitch = view.findViewById(R.id.vibration_switch)
        colorEffectSwitch = view.findViewById(R.id.color_effect_switch)
        scaleEffectSwitch = view.findViewById(R.id.scale_effect_switch)
        btnColorPicker = view.findViewById(R.id.btn_color_picker)
        colorPreview = view.findViewById(R.id.color_preview)

        // 현재 설정 불러오기
        soundSwitch.isChecked = SettingsActivity.isSoundEnabled(requireContext())
        vibrationSwitch.isChecked = SettingsActivity.isVibrationEnabled(requireContext())
        colorEffectSwitch.isChecked = SettingsActivity.isColorEffectEnabled(requireContext())
        scaleEffectSwitch.isChecked = SettingsActivity.isScaleEffectEnabled(requireContext())
        
        val currentColor = SettingsActivity.getTouchColor(requireContext())
        updateColorPreview(currentColor)
        btnColorPicker.isEnabled = colorEffectSwitch.isChecked

        // 스위치 리스너
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            SettingsActivity.setSoundEnabled(requireContext(), isChecked)
        }

        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            SettingsActivity.setVibrationEnabled(requireContext(), isChecked)
        }

        colorEffectSwitch.setOnCheckedChangeListener { _, isChecked ->
            SettingsActivity.setColorEffectEnabled(requireContext(), isChecked)
            btnColorPicker.isEnabled = isChecked
        }

        scaleEffectSwitch.setOnCheckedChangeListener { _, isChecked ->
            SettingsActivity.setScaleEffectEnabled(requireContext(), isChecked)
        }

        // 색상 선택 버튼
        btnColorPicker.setOnClickListener {
            showColorPickerDialog()
        }
    }

    private fun updateColorPreview(color: Int) {
        colorPreview.setBackgroundColor(color)
    }

    private fun showColorPickerDialog() {
        val colors = arrayOf(
            0xFF4CAF50.toInt(), // 초록
            0xFF2196F3.toInt(), // 파랑
            0xFFF44336.toInt(), // 빨강
            0xFFFF9800.toInt(), // 주황
            0xFF9C27B0.toInt(), // 보라
            0xFFFFEB3B.toInt(), // 노랑
            0xFF00BCD4.toInt(), // 청록
            0xFFE91E63.toInt()  // 핑크
        )
        
        val colorNames = arrayOf("초록", "파랑", "빨강", "주황", "보라", "노랑", "청록", "핑크")
        
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("터치 색상 선택")
        builder.setItems(colorNames) { _, which ->
            val selectedColor = colors[which]
            SettingsActivity.setTouchColor(requireContext(), selectedColor)
            updateColorPreview(selectedColor)
        }
        builder.show()
    }
}
