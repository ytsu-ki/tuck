package com.example.starbox.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.StringRes
import com.example.starbox.R

/**
 * Many Android skins (MIUI/HyperOS, EMUI, ColorOS, OriginOS…) stop background apps beyond what
 * stock Android does, which silently kills the notification listener. We show brand-specific
 * steps and try to open the brand's own autostart/background screen.
 */
enum class Oem(
    @param:StringRes val steps: Int,
    @param:StringRes val brandName: Int,
    /** Extra notification switches this brand keeps outside Android's standard channel settings. */
    @param:StringRes val alertHint: Int? = null,
    /**
     * Settings close to stock Android: Sound and Vibration per notification category, and the
     * newer direct links work. Other brands rebuild these screens (see Permissions).
     */
    val stockSettings: Boolean = false,
) {
    Xiaomi(R.string.oem_steps_xiaomi, R.string.oem_name_xiaomi, R.string.alert_oem_hint_xiaomi),
    Huawei(R.string.oem_steps_huawei, R.string.oem_name_huawei, R.string.alert_oem_hint_huawei),
    Honor(R.string.oem_steps_huawei, R.string.oem_name_honor, R.string.alert_oem_hint_huawei),
    Oppo(R.string.oem_steps_oppo, R.string.oem_name_oppo, R.string.alert_oem_hint_oppo),
    OnePlus(R.string.oem_steps_oppo, R.string.oem_name_oneplus, R.string.alert_oem_hint_oppo),
    Vivo(R.string.oem_steps_vivo, R.string.oem_name_vivo, R.string.alert_oem_hint_vivo),
    Samsung(R.string.oem_steps_samsung, R.string.oem_name_samsung, stockSettings = true),
    Meizu(R.string.oem_steps_meizu, R.string.oem_name_meizu, R.string.alert_oem_hint_meizu),
    Other(R.string.oem_steps_other, R.string.oem_name_other, stockSettings = true),
}

object OemBattery {

    fun current(): Oem = detect(Build.MANUFACTURER, Build.BRAND)

    /** Pure mapping from Build fields, so it can be unit tested. */
    fun detect(manufacturer: String?, brand: String?): Oem {
        val names = listOfNotNull(manufacturer, brand).map { it.lowercase() }
        fun any(vararg keys: String) = names.any { name -> keys.any { name.contains(it) } }
        return when {
            any("xiaomi", "redmi", "poco") -> Oem.Xiaomi
            any("honor") -> Oem.Honor
            any("huawei") -> Oem.Huawei
            any("oneplus") -> Oem.OnePlus
            any("oppo", "realme") -> Oem.Oppo
            any("vivo", "iqoo") -> Oem.Vivo
            any("samsung") -> Oem.Samsung
            any("meizu") -> Oem.Meizu
            else -> Oem.Other
        }
    }

    /** Whether we know a brand screen to open for [oem]. */
    fun hasSettingsScreen(oem: Oem): Boolean = screens(oem).isNotEmpty()

    /**
     * Tries the brand's autostart/background screens in turn. These are private activities that
     * move between OS versions, so each may be missing or refuse to open. Returns false when
     * none opened (the caller can fall back to App info).
     */
    fun openSettings(context: Context, oem: Oem): Boolean =
        Permissions.openFirst(context, screens(oem).map { Intent().setComponent(it) })

    private fun screens(oem: Oem): List<ComponentName> = when (oem) {
        Oem.Xiaomi -> listOf(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        )
        Oem.Huawei -> listOf(
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
        )
        Oem.Honor -> listOf(
            ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        )
        Oem.Oppo -> listOf(
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
        )
        Oem.OnePlus -> listOf(
            ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
        )
        Oem.Vivo -> listOf(
            ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
        )
        Oem.Samsung -> listOf(
            ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
            ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.battery.ui.BatteryActivity"),
        )
        Oem.Meizu -> listOf(
            ComponentName("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity"),
        )
        Oem.Other -> emptyList()
    }
}
