package com.example.neo_f

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment

class DesignSettingsFragment : Fragment() {

    private lateinit var textSizeSeekBar: SeekBar
    private lateinit var textSizeValueText: TextView
    private lateinit var keySpacingSeekBar: SeekBar
    private lateinit var keySpacingValueText: TextView
    private lateinit var keyCornerRadiusSeekBar: SeekBar
    private lateinit var keyCornerRadiusValueText: TextView
    private lateinit var keyHeightSeekBar: SeekBar
    private lateinit var keyHeightValueText: TextView
    private lateinit var numberRowSwitch: SwitchCompat
    
    private lateinit var textColorPreview: View
    private lateinit var textColorButton: android.widget.Button
    private lateinit var keyBackgroundColorPreview: View
    private lateinit var keyBackgroundColorButton: android.widget.Button
    private lateinit var functionalKeyColorPreview: View
    private lateinit var functionalKeyColorButton: android.widget.Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.settings_tab_design, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textSizeSeekBar = view.findViewById(R.id.text_size_seekbar)
        textSizeValueText = view.findViewById(R.id.text_size_value)
        keySpacingSeekBar = view.findViewById(R.id.key_spacing_seekbar)
        keySpacingValueText = view.findViewById(R.id.key_spacing_value)
        keyCornerRadiusSeekBar = view.findViewById(R.id.key_corner_radius_seekbar)
        keyCornerRadiusValueText = view.findViewById(R.id.key_corner_radius_value)
        keyHeightSeekBar = view.findViewById(R.id.key_height_seekbar)
        keyHeightValueText = view.findViewById(R.id.key_height_value)
        numberRowSwitch = view.findViewById(R.id.number_row_switch)
        
        textColorPreview = view.findViewById(R.id.text_color_preview)
        textColorButton = view.findViewById(R.id.text_color_button)
        keyBackgroundColorPreview = view.findViewById(R.id.key_background_color_preview)
        keyBackgroundColorButton = view.findViewById(R.id.key_background_color_button)
        functionalKeyColorPreview = view.findViewById(R.id.functional_key_color_preview)
        functionalKeyColorButton = view.findViewById(R.id.functional_key_color_button)

        // 현재 설정 불러오기
        val currentTextSize = SettingsActivity.getTextSize(requireContext())
        val currentKeySpacing = SettingsActivity.getKeySpacing(requireContext())
        val currentKeyCornerRadius = SettingsActivity.getKeyCornerRadius(requireContext())
        val currentKeyHeight = SettingsActivity.getKeyHeight(requireContext())
        val currentShowNumberRow = SettingsActivity.isShowNumberRow(requireContext())
        val currentTextColor = SettingsActivity.getTextColor(requireContext())
        val currentKeyBackgroundColor = SettingsActivity.getKeyBackgroundColor(requireContext())
        val currentFunctionalKeyColor = SettingsActivity.getFunctionalKeyColor(requireContext())

        updateTextSizeSeekBar(currentTextSize)
        updateKeySpacingSeekBar(currentKeySpacing)
        updateKeyCornerRadiusSeekBar(currentKeyCornerRadius)
        updateKeyHeightSeekBar(currentKeyHeight)
        numberRowSwitch.isChecked = currentShowNumberRow
        
        textColorPreview.setBackgroundColor(currentTextColor)
        keyBackgroundColorPreview.setBackgroundColor(currentKeyBackgroundColor)
        functionalKeyColorPreview.setBackgroundColor(currentFunctionalKeyColor)

        // 글자 크기 SeekBar 리스너
        textSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val textSize = (progress + 12).toFloat()
                textSizeValueText.text = "${textSize.toInt()} sp"
                if (fromUser) {
                    SettingsActivity.setTextSize(requireContext(), textSize)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 자판 간격 SeekBar 리스너
        keySpacingSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                keySpacingValueText.text = "$progress dp"
                if (fromUser) {
                    SettingsActivity.setKeySpacing(requireContext(), progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 키 라운드 SeekBar 리스너
        keyCornerRadiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                keyCornerRadiusValueText.text = "$progress dp"
                if (fromUser) {
                    SettingsActivity.setKeyCornerRadius(requireContext(), progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 키 높이 SeekBar 리스너
        keyHeightSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val height = progress + 32  // 32dp ~ 64dp
                keyHeightValueText.text = "$height dp"
                if (fromUser) {
                    SettingsActivity.setKeyHeight(requireContext(), height)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 숫자 행 스위치 리스너
        numberRowSwitch.setOnCheckedChangeListener { _, isChecked ->
            SettingsActivity.setShowNumberRow(requireContext(), isChecked)
        }
        
        // 색상 선택 버튼 리스너
        textColorButton.setOnClickListener {
            showColorPickerDialog(currentTextColor) { color ->
                SettingsActivity.setTextColor(requireContext(), color)
                textColorPreview.setBackgroundColor(color)
            }
        }
        
        keyBackgroundColorButton.setOnClickListener {
            showColorPickerDialog(currentKeyBackgroundColor) { color ->
                SettingsActivity.setKeyBackgroundColor(requireContext(), color)
                keyBackgroundColorPreview.setBackgroundColor(color)
            }
        }
        
        functionalKeyColorButton.setOnClickListener {
            showColorPickerDialog(currentFunctionalKeyColor) { color ->
                SettingsActivity.setFunctionalKeyColor(requireContext(), color)
                functionalKeyColorPreview.setBackgroundColor(color)
            }
        }
    }
    
    private fun showColorPickerDialog(currentColor: Int, onColorSelected: (Int) -> Unit) {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("색상 선택")
        
        // ColorPickerView 생성 - 크기 지정
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
            onColorSelected(selectedColor)
        }
        builder.setNegativeButton("취소", null)
        builder.show()
    }

    private fun updateTextSizeSeekBar(textSize: Float) {
        val progress = (textSize - 12).toInt().coerceIn(0, 20)
        textSizeSeekBar.progress = progress
        textSizeValueText.text = "${textSize.toInt()} sp"
    }

    private fun updateKeySpacingSeekBar(spacing: Int) {
        keySpacingSeekBar.progress = spacing.coerceIn(0, 8)
        keySpacingValueText.text = "$spacing dp"
    }

    private fun updateKeyCornerRadiusSeekBar(radius: Int) {
        keyCornerRadiusSeekBar.progress = radius.coerceIn(0, 24)
        keyCornerRadiusValueText.text = "$radius dp"
    }

    private fun updateKeyHeightSeekBar(height: Int) {
        keyHeightSeekBar.progress = (height - 32).coerceIn(0, 32)
        keyHeightValueText.text = "$height dp"
    }
}
