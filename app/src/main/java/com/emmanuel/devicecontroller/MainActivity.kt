package com.emmanuel.devicecontroller

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Prototype test panel. In a real release this screen would be hidden or removed. */
class MainActivity : Activity() {

    private lateinit var controller: DeviceController
    private lateinit var status: TextView
    private lateinit var lockBtn: Button
    private lateinit var tamperBtn: Button
    private lateinit var releaseBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = DeviceController(this)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val owner = controller.isDeviceOwner()
        val sb = StringBuilder()
        sb.append("Device owner: ").append(if (owner) "YES" else "NO").append("\n")
        sb.append("Locked: ").append(if (controller.isLocked) "YES" else "NO").append("\n")
        sb.append("Tamper protection: ").append(if (controller.tamperOn) "ON" else "OFF").append("\n")
        if (!owner) {
            sb.append("\nThis app is not the device owner yet. On a freshly reset phone with no accounts, ")
            sb.append("connect it by USB and run:\n\n")
            sb.append("adb shell dpm set-device-owner ")
            sb.append("com.emmanuel.devicecontroller/.AdminReceiver")
        }
        status.text = sb.toString()
        lockBtn.isEnabled = owner
        tamperBtn.isEnabled = owner
        releaseBtn.isEnabled = owner
        tamperBtn.text = if (controller.tamperOn) "Turn tamper protection OFF" else "Turn tamper protection ON"
    }

    private fun confirmRelease() {
        AlertDialog.Builder(this)
            .setTitle("Release this phone?")
            .setMessage("This removes all restrictions and the device-owner status. Do this before returning or resetting a test phone.")
            .setPositiveButton("Release") { _, _ ->
                controller.release()
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun buildUi() {
        val d = resources.displayMetrics.density
        val pad = (20 * d).toInt()

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        col.addView(TextView(this).apply {
            text = "Device Controller (prototype)"
            textSize = 22f
            setTextColor(Color.parseColor("#14161C"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        status = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.parseColor("#333333"))
            setPadding(0, pad, 0, pad)
            setTextIsSelectable(true)
        }
        col.addView(status)

        lockBtn = Button(this).apply {
            text = "Lock phone now (test)"
            setOnClickListener {
                controller.lock(this@MainActivity)
            }
        }
        col.addView(lockBtn)

        tamperBtn = Button(this).apply {
            setOnClickListener {
                controller.setTamperProtection(!controller.tamperOn)
                refresh()
            }
        }
        col.addView(tamperBtn)

        releaseBtn = Button(this).apply {
            text = "Release phone (remove controller)"
            setOnClickListener { confirmRelease() }
        }
        col.addView(releaseBtn)

        col.addView(TextView(this).apply {
            text = "\nTest unlock code: ${Config.DEV_UNLOCK_CODE}"
            textSize = 13f
            gravity = Gravity.START
            setTextColor(Color.parseColor("#777777"))
        })

        setContentView(ScrollView(this).apply { addView(col) })
    }
}
