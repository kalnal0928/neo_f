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
        val currentColor = SettingsActivity.getTouchColor(requireContext())
        
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("터치 색상 선택")
        
        // ColorPickerView 생성
        val colorPickerView = com.skydoves.colorpickerview.ColorPickerView.Builder(requireContext())
            .setInitialColor(currentColor)
            .build()
        
        // 크기 설정
        val size = (300 * resources.displayMetrics.density).toInt()
        val params = android.widget.LinearLayout.LayoutParams(size, size)
        colorPickerView.layoutParams = params
        
        val layout = android.widget.LinearLayout(requireContext())
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(32, 32, 32, 32)
        layout.gravity = android.view.Gravity.CENTER
        layout.addView(colorPickerView)
        
        builder.setView(layout)
        builder.setPositiveButton("확인") { _, _ ->
            val selectedColor = colorPickerView.color
            SettingsActivity.setTouchColor(requireContext(), selectedColor)
            updateColorPreview(selectedColor)
        }
        builder.setNegativeButton("취소", null)
        builder.show()
    }
}
