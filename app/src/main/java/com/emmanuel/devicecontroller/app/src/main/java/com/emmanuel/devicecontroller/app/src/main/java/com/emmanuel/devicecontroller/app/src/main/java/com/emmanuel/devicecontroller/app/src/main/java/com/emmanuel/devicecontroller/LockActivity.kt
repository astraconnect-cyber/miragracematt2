package com.emmanuel.devicecontroller

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class LockActivity : Activity() {

    private lateinit var controller: DeviceController
    private lateinit var codeInput: EditText
    private lateinit var errorText: TextView

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            leaveLock()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = DeviceController(this)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        buildUi()

        val filter = IntentFilter(Config.ACTION_UNLOCK)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(unlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(unlockReceiver, filter)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!controller.isLocked) {
            leaveLock()
            return
        }
        if (controller.isDeviceOwner() && controller.canLockTask()) {
            try {
                startLockTask()
            } catch (_: Exception) {
            }
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(unlockReceiver)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // Back is disabled while locked.
    }

    private fun leaveLock() {
        try {
            stopLockTask()
        } catch (_: Exception) {
        }
        finish()
    }

    private fun tryUnlock() {
        if (codeInput.text.toString().trim() == Config.DEV_UNLOCK_CODE) {
            controller.unlock()
            leaveLock()
        } else {
            errorText.text = "Wrong code. Please try again."
            codeInput.text.clear()
        }
    }

    private fun emergencyCall() {
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
        } catch (_: Exception) {
        }
    }

    private fun buildUi() {
        val d = resources.displayMetrics.density
        val pad = (28 * d).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(Color.parseColor("#14161C"))
        }

        fun text(t: String, size: Float, color: String, bold: Boolean = false) = TextView(this).apply {
            this.text = t
            textSize = size
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(color))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, (8 * d).toInt(), 0, (8 * d).toInt())
        }

        root.addView(text("PHONE LOCKED", 26f, "#FF5C5C", true))
        root.addView(text("Your installment payment is overdue.", 17f, "#FFFFFF"))
        root.addView(text("Please pay ${Config.STORE_NAME} to unlock this phone.", 15f, "#B8BEC9"))
        root.addView(text("Help: ${Config.SUPPORT_PHONE}", 15f, "#B8BEC9"))

        codeInput = EditText(this).apply {
            hint = "Enter unlock code"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#7A8190"))
            gravity = Gravity.CENTER
        }
        root.addView(codeInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = (24 * d).toInt() })

        errorText = text("", 14f, "#FF8A8A")
        root.addView(errorText)

        val unlockBtn = Button(this).apply {
            text = "UNLOCK"
            setOnClickListener { tryUnlock() }
        }
        root.addView(unlockBtn)

        val emergencyBtn = Button(this).apply {
            text = "EMERGENCY CALL"
            setOnClickListener { emergencyCall() }
        }
        root.addView(emergencyBtn)

        setContentView(root)
    }
}
