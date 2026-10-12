package com.emmanuel.devicecontroller

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.telecom.TelecomManager

/** All device-policy actions live here so later phases (FCM, Paystack) can reuse them. */
class DeviceController(private val ctx: Context) {

    private val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin = ComponentName(ctx, AdminReceiver::class.java)
    private val prefs = ctx.getSharedPreferences("controller", Context.MODE_PRIVATE)

    private val tamperRestrictions = listOf(
        UserManager.DISALLOW_FACTORY_RESET,
        UserManager.DISALLOW_SAFE_BOOT,
        UserManager.DISALLOW_ADD_USER,
        UserManager.DISALLOW_CONFIG_DATE_TIME
    )

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(ctx.packageName)

    var isLocked: Boolean
        get() = prefs.getBoolean("locked", false)
        private set(value) = prefs.edit().putBoolean("locked", value).apply()

    var tamperOn: Boolean
        get() = prefs.getBoolean("tamper", false)
        private set(value) = prefs.edit().putBoolean("tamper", value).apply()

    fun canLockTask(): Boolean = dpm.isLockTaskPermitted(ctx.packageName)

    /** Lock the phone: full-screen kiosk screen, only the dialer is allowed (emergency calls). */
    fun lock(from: Context) {
        if (!isDeviceOwner()) return
        isLocked = true
        allowLockTask()
        val intent = Intent(from, LockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        from.startActivity(intent)
    }

    /** Unlock the phone. LockActivity listens for this broadcast and closes itself. */
    fun unlock() {
        isLocked = false
        ctx.sendBroadcast(Intent(Config.ACTION_UNLOCK).setPackage(ctx.packageName))
    }

    private fun allowLockTask() {
        val packages = mutableListOf(ctx.packageName)
        try {
            val dialer = (ctx.getSystemService(Context.TELECOM_SERVICE) as TelecomManager).defaultDialerPackage
            if (!dialer.isNullOrEmpty()) packages.add(dialer)
        } catch (_: Exception) {
        }
        dpm.setLockTaskPackages(admin, packages.toTypedArray())
        dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
    }

    /** Blocks factory reset, safe mode, new users, date/time changes and uninstall. */
    fun setTamperProtection(on: Boolean) {
        if (!isDeviceOwner()) return
        for (r in tamperRestrictions) {
            if (on) dpm.addUserRestriction(admin, r) else dpm.clearUserRestriction(admin, r)
        }
        dpm.setUninstallBlocked(admin, ctx.packageName, on)
        tamperOn = on
    }

    /** Removes every restriction and the device-owner status so the phone returns to normal. */
    @Suppress("DEPRECATION")
    fun release() {
        if (!isDeviceOwner()) return
        setTamperProtection(false)
        isLocked = false
        ctx.sendBroadcast(Intent(Config.ACTION_UNLOCK).setPackage(ctx.packageName))
        dpm.setLockTaskPackages(admin, emptyArray())
        dpm.clearDeviceOwnerApp(ctx.packageName)
    }
}
