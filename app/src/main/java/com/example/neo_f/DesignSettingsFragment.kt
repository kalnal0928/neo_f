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
    private lateinit var numberRowSwitch: SwitchCompat

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
        numberRowSwitch = view.findViewById(R.id.number_row_switch)

        // 현재 설정 불러오기
        val currentTextSize = SettingsActivity.getTextSize(requireContext())
        val currentKeySpacing = SettingsActivity.getKeySpacing(requireContext())
        val currentKeyCornerRadius = SettingsActivity.getKeyCornerRadius(requireContext())
        val currentShowNumberRow = SettingsActivity.isShowNumberRow(requireContext())

        updateTextSizeSeekBar(currentTextSize)
        updateKeySpacingSeekBar(currentKeySpacing)
        updateKeyCornerRadiusSeekBar(currentKeyCornerRadius)
        numberRowSwitch.isChecked = currentShowNumberRow

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

        // 숫자 행 스위치 리스너
        numberRowSwitch.setOnCheckedChangeListener { _, isChecked ->
            SettingsActivity.setShowNumberRow(requireContext(), isChecked)
        }
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
}
