/*
 * Copyright (c) 2025 Hasan Oso. All Rights Reserved.
 * 
 * PROPRIETARY SOFTWARE - DO NOT DISTRIBUTE
 * 
 * This file is part of DiagnosticApp.
 * Unauthorized use is strictly prohibited.
 * 
 * Owner: Hasan Oso
 * Student ID: 230212925
 * Institution: OST?M Technical University
 */
package com.diagnostics.app.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.*
import com.diagnostics.app.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SurveyDialog(context: Context, private val onSubmit: (SurveyResponse) -> Unit) : Dialog(context) {
    
    private lateinit var q1RadioGroup: RadioGroup
    private lateinit var q3RadioGroup: RadioGroup
    private lateinit var q4RadioGroup: RadioGroup
    private lateinit var q6Seekbar: SeekBar
    private lateinit var q6Value: TextView
    private lateinit var q7Notes: EditText
    
    // Q2 checkboxes
    private lateinit var q2None: CheckBox
    private lateinit var q2Screen: CheckBox
    private lateinit var q2Battery: CheckBox
    private lateinit var q2Motherboard: CheckBox
    private lateinit var q2Camera: CheckBox
    private lateinit var q2OtherText: EditText
    
    // Q5 checkboxes
    private lateinit var q5Slow: CheckBox
    private lateinit var q5Heat: CheckBox
    private lateinit var q5BatteryDrain: CheckBox
    private lateinit var q5Freezing: CheckBox
    private lateinit var q5Charging: CheckBox
    private lateinit var q5None: CheckBox
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_survey)
        
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        initViews()
        setupListeners()
    }
    
    private fun initViews() {
        q1RadioGroup = findViewById(R.id.q1_radio_group)
        q3RadioGroup = findViewById(R.id.q3_radio_group)
        q4RadioGroup = findViewById(R.id.q4_radio_group)
        
        // Q2
        q2None = findViewById(R.id.q2_none)
        q2Screen = findViewById(R.id.q2_screen)
        q2Battery = findViewById(R.id.q2_battery)
        q2Motherboard = findViewById(R.id.q2_motherboard)
        q2Camera = findViewById(R.id.q2_camera)
        q2OtherText = findViewById(R.id.q2_other_text)
        
        // Q5
        q5Slow = findViewById(R.id.q5_slow)
        q5Heat = findViewById(R.id.q5_heat)
        q5BatteryDrain = findViewById(R.id.q5_battery_drain)
        q5Freezing = findViewById(R.id.q5_freezing)
        q5Charging = findViewById(R.id.q5_charging)
        q5None = findViewById(R.id.q5_none)
        
        q6Seekbar = findViewById(R.id.q6_seekbar)
        q6Value = findViewById(R.id.q6_value)
        q7Notes = findViewById(R.id.q7_notes)
    }
    
    private fun setupListeners() {
        // Q6 Seekbar
        q6Seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                q6Value.text = progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Q2 None checkbox - uncheck others when selected
        q2None.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                q2Screen.isChecked = false
                q2Battery.isChecked = false
                q2Motherboard.isChecked = false
                q2Camera.isChecked = false
                q2OtherText.text?.clear()
            }
        }
        
        // Q5 None checkbox - uncheck others when selected
        q5None.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                q5Slow.isChecked = false
                q5Heat.isChecked = false
                q5BatteryDrain.isChecked = false
                q5Freezing.isChecked = false
                q5Charging.isChecked = false
            }
        }
        
        // Buttons
        findViewById<Button>(R.id.btn_skip).setOnClickListener {
            dismiss()
        }
        
        findViewById<Button>(R.id.btn_submit).setOnClickListener {
            if (validateInputs()) {
                val response = collectResponses()
                onSubmit(response)
                dismiss()
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        // Q1 required
        if (q1RadioGroup.checkedRadioButtonId == -1) {
            Toast.makeText(context, context.getString(R.string.select_condition), Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Q3 required
        if (q3RadioGroup.checkedRadioButtonId == -1) {
            Toast.makeText(context, context.getString(R.string.select_authenticity), Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Q4 required
        if (q4RadioGroup.checkedRadioButtonId == -1) {
            Toast.makeText(context, context.getString(R.string.select_intensity), Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun collectResponses(): SurveyResponse {
        // Q1: Overall health score (map radio to score)
        val overallHealthScore = when (q1RadioGroup.checkedRadioButtonId) {
            R.id.q1_excellent -> 90
            R.id.q1_very_good -> 75
            R.id.q1_good -> 60
            R.id.q1_fair -> 40
            R.id.q1_poor -> 20
            else -> 50
        }
        
        // Q2: Replaced components
        val replacedComponents = mutableListOf<String>()
        if (q2None.isChecked) replacedComponents.add("none")
        if (q2Screen.isChecked) replacedComponents.add("screen")
        if (q2Battery.isChecked) replacedComponents.add("battery")
        if (q2Motherboard.isChecked) replacedComponents.add("motherboard")
        if (q2Camera.isChecked) replacedComponents.add("camera")
        val otherText = q2OtherText.text.toString().trim()
        if (otherText.isNotBlank()) replacedComponents.add("other: $otherText")
        
        // Q3: Authenticity
        val authenticity = when (q3RadioGroup.checkedRadioButtonId) {
            R.id.q3_original -> "original"
            R.id.q3_official_refurb -> "official_refurbished"
            R.id.q3_unofficial_refurb -> "unofficial_refurbished"
            R.id.q3_unsure -> "unsure"
            else -> "unknown"
        }
        
        // Q4: Usage intensity
        val usageIntensity = when (q4RadioGroup.checkedRadioButtonId) {
            R.id.q4_light -> "light"
            R.id.q4_moderate -> "moderate"
            R.id.q4_heavy -> "heavy"
            else -> "moderate"
        }
        
        // Q5: Known issues
        val knownIssues = mutableListOf<String>()
        if (q5Slow.isChecked) knownIssues.add("slow_performance")
        if (q5Heat.isChecked) knownIssues.add("overheating")
        if (q5BatteryDrain.isChecked) knownIssues.add("battery_drain")
        if (q5Freezing.isChecked) knownIssues.add("app_freezing")
        if (q5Charging.isChecked) knownIssues.add("charging")
        if (q5None.isChecked) knownIssues.add("none")
        
        // Q6: Expected score
        val expectedScore = q6Seekbar.progress
        
        // Q7: Notes
        val technicianNotes = q7Notes.text.toString().trim()
        
        return SurveyResponse(
            overallHealthScore = overallHealthScore,
            replacedComponents = replacedComponents,
            authenticity = authenticity,
            usageIntensity = usageIntensity,
            knownIssues = knownIssues,
            expectedScore = expectedScore,
            technicianNotes = technicianNotes,
            labeledBy = "user-self",
            labeledAt = System.currentTimeMillis()
        )
    }
}

@Serializable
data class SurveyResponse(
    val overallHealthScore: Int,          // 0-100 mapped from Q1
    val replacedComponents: List<String>, // Q2
    val authenticity: String,             // Q3
    val usageIntensity: String,           // Q4: light, moderate, heavy
    val knownIssues: List<String>,        // Q5
    val expectedScore: Int,               // Q6: 0-100
    val technicianNotes: String = "",     // Q7
    val labeledBy: String = "user-self",
    val labeledAt: Long = System.currentTimeMillis()
) {
    fun toJson(): String = Json.encodeToString(this)
}
