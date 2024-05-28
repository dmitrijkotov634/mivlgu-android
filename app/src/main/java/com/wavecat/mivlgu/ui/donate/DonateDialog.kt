package com.wavecat.mivlgu.ui.donate

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wavecat.mivlgu.databinding.DonateDialogBinding

class DonateDialog : BottomSheetDialogFragment() {
    private val billingModel by activityViewModels<BillingViewModel>()

    private fun getQuantity(value: Float): Int = (value / 0.2).toInt() + 1

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DonateDialogBinding.inflate(inflater)

        (dialog as BottomSheetDialog).behavior.state = BottomSheetBehavior.STATE_EXPANDED

        binding.quantity.addOnChangeListener { _, value, _ ->
            val price = getQuantity(value) * BillingViewModel.COFFEE_PRICE
            binding.buy.text = "$price ₽"
        }

        binding.quantity.setLabelFormatter { value -> "%.1f л".format(value) }

        binding.buy.setOnClickListener {
            billingModel.donate(getQuantity(binding.quantity.value))
        }

        binding.thanks.setOnClickListener {
            billingModel.billingMade()
            dismiss()
        }

        billingModel.donationMade.observe(viewLifecycleOwner) {
            binding.thanks.visibility = if (it) View.VISIBLE else View.GONE
        }

        billingModel.billingMade.observe(viewLifecycleOwner) { status ->
            if (status) dismiss()
        }

        return binding.getRoot()
    }
}
