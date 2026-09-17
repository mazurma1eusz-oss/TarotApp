package com.mazur.tarot.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Realna, odczuwalna wibracja urządzenia przy odsłanianiu kart. Compose'owe
 * `LocalHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)` idzie przez
 * `View.performHapticFeedback()`, które na wielu telefonach (szczególnie z nakładkami OEM)
 * jest wyciszone przez systemowe ustawienie "wibracja przy dotyku" niezależnie od kodu
 * aplikacji, albo jest ledwo wyczuwalne. To wywołanie idzie bezpośrednio przez
 * Vibrator/VibratorManager (VIBRATE zadeklarowane w AndroidManifest.xml) i zależy tylko od
 * głównego przełącznika wibracji systemu, więc jest spójnie odczuwalne na każdym urządzeniu.
 */
object HapticUtil {

    private const val DEFAULT_DURATION_MILLIS = 40L

    fun vibrate(context: Context, durationMillis: Long = DEFAULT_DURATION_MILLIS) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
