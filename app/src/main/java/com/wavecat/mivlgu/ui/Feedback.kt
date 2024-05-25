package com.wavecat.mivlgu.ui

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import com.wavecat.mivlgu.R

const val FEEDBACK_EMAIL = "dmitrijkotov634@gmail.com"

fun Fragment.sendFeedback(featureName: String?) = requireContext().sendFeedback(featureName)

fun Context.sendFeedback(featureName: String?) {
    val deviceInfo = """
        ___
        Device Info:
        Manufacturer: ${Build.MANUFACTURER}
        Model: ${Build.MODEL}
        Android Version: ${Build.VERSION.RELEASE}
        SDK Version: ${Build.VERSION.SDK_INT}
    """.trimIndent()

    ShareCompat.IntentBuilder(this)
        .setType("message/rfc822")
        .setEmailTo(arrayOf(FEEDBACK_EMAIL))
        .setSubject("${getString(R.string.app_name)}: $featureName")
        .setText("\n\n$deviceInfo")
        .setChooserTitle(R.string.send_email)
        .startChooser()

    Toast.makeText(this, R.string.send_email, Toast.LENGTH_SHORT).show()
}