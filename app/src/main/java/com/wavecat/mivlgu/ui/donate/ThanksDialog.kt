package com.wavecat.mivlgu.ui.donate

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wavecat.mivlgu.databinding.ThanksDialogBinding

class ThanksDialog : BottomSheetDialogFragment() {
    private val billingModel by activityViewModels<BillingViewModel>()

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = ThanksDialogBinding.inflate(inflater)

        (dialog as BottomSheetDialog).behavior.state = BottomSheetBehavior.STATE_EXPANDED

        billingModel.purchaseId.observe(viewLifecycleOwner) {
            binding.purchaseId.text = it
        }

        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        binding.purchaseId.setOnClickListener {
            val clip = ClipData.newPlainText("purchaseId", binding.purchaseId.text)
            clipboard.setPrimaryClip(clip)
        }

        binding.please.setOnClickListener { dismiss() }

        return binding.getRoot()
    }
}
