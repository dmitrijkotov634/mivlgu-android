package com.wavecat.mivlgu.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.wavecat.mivlgu.R

const val FEEDBACK_EMAIL = "dmitrijkotov634@gmail.com"

fun sendFeedback(context: Context) {
    val i = Intent(Intent.ACTION_SENDTO)
    i.putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
    i.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
    i.putExtra(Intent.EXTRA_TEXT, "")
    i.setData(Uri.parse("mailto:$FEEDBACK_EMAIL"))
    context.startActivity(Intent.createChooser(i, "Send via email"))
}