package com.wavecat.mivlgu.fragment

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wavecat.mivlgu.R

class LoadingExceptionDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.failed_loading))
            .setMessage(arguments?.getString(EXCEPTION_ARG, ""))
            .setPositiveButton(getString(android.R.string.ok)) { _, _ -> }
            .create()

    companion object {
        const val EXCEPTION_ARG = "exception"
        const val TAG = "LoadingErrorDialog"
    }
}