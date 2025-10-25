package com.example.assignment1

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var score: Int = 0
    private var hold: Int = 0 // 0..9
    private var fallen: Boolean = false

    private lateinit var tvScore: TextView
    private lateinit var btnClimb: Button
    private lateinit var btnFall: Button
    private lateinit var btnReset: Button
    private lateinit var btnLanguage: ImageButton
    private lateinit var holds: List<ImageView>
    private lateinit var character: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvScore = findViewById(R.id.tvScore)
        btnClimb = findViewById(R.id.btnClimb)
        btnFall = findViewById(R.id.btnFall)
        btnReset = findViewById(R.id.btnReset)
        btnLanguage = findViewById(R.id.btnLanguage)
        character = findViewById(R.id.character)
        holds = listOf(
            findViewById(R.id.hold1),
            findViewById(R.id.hold2),
            findViewById(R.id.hold3),
            findViewById(R.id.hold4),
            findViewById(R.id.hold5),
            findViewById(R.id.hold6),
            findViewById(R.id.hold7),
            findViewById(R.id.hold8),
            findViewById(R.id.hold9),
        )

        title = getString(R.string.title_main)

        btnClimb.setOnClickListener {
            Log.d(TAG, "Climb clicked: hold=$hold score=$score fallen=$fallen")
            onClimb()
        }
        btnFall.setOnClickListener {
            Log.d(TAG, "Fall clicked: hold=$hold score=$score fallen=$fallen")
            onFall()
        }
        btnReset.setOnClickListener {
            Log.d(TAG, "Reset clicked: hold=$hold score=$score fallen=$fallen")
            onReset()
        }
        btnLanguage.setOnClickListener {
            Log.d(TAG, "Language toggle clicked")
            toggleLanguage()
        }

        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        }
        updateUi()
    }

    private fun onClimb() {
        if (fallen) {
            Log.d(TAG, getString(R.string.msg_blocked_after_fall))
            return
        }
        if (hold >= 9) {
            // Already at top
            return
        }
        hold += 1
        score += pointsForHold(hold)
        if (score > 18) score = 18
        Log.d(TAG, "After climb: hold=$hold score=$score fallen=$fallen")
        updateUi()
    }

    private fun onFall() {
        if (hold == 0) {
            Log.d(TAG, getString(R.string.msg_fall_before_start))
            return
        }
        if (hold >= 9) {
            Log.d(TAG, getString(R.string.msg_fall_at_top))
            return
        }
        if (fallen) {
            // Already fallen, no further effect
            return
        }
        score = (score - 3).coerceAtLeast(0)
        fallen = true
        Log.d(TAG, "After fall: hold=$hold score=$score fallen=$fallen")
        updateUi()
    }

    private fun onReset() {
        score = 0
        hold = 0
        fallen = false
        Log.d(TAG, "After reset: hold=$hold score=$score fallen=$fallen")
        updateUi()
    }

    private fun pointsForHold(h: Int): Int {
        return when (h) {
            in 1..3 -> 1
            in 4..6 -> 2
            in 7..9 -> 3
            else -> 0
        }
    }

    private fun updateUi() {
        tvScore.text = score.toString()
        val colorRes = when (hold) {
            in 1..3 -> R.color.zone_blue
            in 4..6 -> R.color.zone_green
            in 7..9 -> R.color.zone_red
            else -> R.color.black
        }
        tvScore.setTextColor(ContextCompat.getColor(this, colorRes))
        updateHoldsUi()
    }

    private fun updateHoldsUi() {
        // Visualize progress: reached holds full alpha; next/current slightly larger; others dimmed
        for ((index, imageView) in holds.withIndex()) {
            val holdNumber = index + 1
            val isReached = holdNumber <= hold
            val isCurrentNext = holdNumber == hold + 1 && hold < 9 && !fallen
            val alpha = when {
                isReached -> 1.0f
                isCurrentNext -> 0.9f
                else -> 0.35f
            }
            val scale = when {
                isReached -> 1.0f
                isCurrentNext -> 1.15f
                else -> 1.0f
            }
            imageView.alpha = alpha
            imageView.scaleX = scale
            imageView.scaleY = scale
            imageView.contentDescription = getString(R.string.cd_hold, holdNumber)
        }
        
        // Position character on mountain levels
        updateCharacterPosition()
    }
    
    private fun updateCharacterPosition() {
        // Position character based on current hold level
        val level = when {
            hold == 0 -> 0 // At bottom
            hold in 1..3 -> 1 // Level 1 (blue)
            hold in 4..6 -> 2 // Level 2 (green) 
            hold in 7..9 -> 3 // Level 3 (red)
            else -> 0
        }
        

        character.animate()
            .translationY((3 - level) * 100f) // Move up as level increases
            .setDuration(300)
            .start()
            
        Log.d(TAG, "Character positioned at level $level for hold $hold")
    }

    private fun toggleLanguage() {
        val currentLocale = resources.configuration.locales[0]
        val newLocale = if (currentLocale.language == "vi") {
            Locale("en")
        } else {
            Locale("vi")
        }
        
        Log.d(TAG, "Switching from ${currentLocale.language} to ${newLocale.language}")
        
        val config = Configuration(resources.configuration)
        config.setLocale(newLocale)
        resources.updateConfiguration(config, resources.displayMetrics)

        recreate()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SCORE, score)
        outState.putInt(KEY_HOLD, hold)
        outState.putBoolean(KEY_FALLEN, fallen)
        Log.d(TAG, "State saved: hold=$hold score=$score fallen=$fallen")
    }

    private fun restoreState(state: Bundle) {
        score = state.getInt(KEY_SCORE, 0)
        hold = state.getInt(KEY_HOLD, 0)
        fallen = state.getBoolean(KEY_FALLEN, false)
        Log.d(TAG, "State restored: hold=$hold score=$score fallen=$fallen")
    }

    companion object {
        private const val TAG = "ClimbApp"
        private const val KEY_SCORE = "key_score"
        private const val KEY_HOLD = "key_hold"
        private const val KEY_FALLEN = "key_fallen"
    }
}