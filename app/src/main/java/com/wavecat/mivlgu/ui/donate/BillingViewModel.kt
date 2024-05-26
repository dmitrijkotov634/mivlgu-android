package com.wavecat.mivlgu.ui.donate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.wavecat.mivlgu.MainRepository
import ru.rustore.sdk.billingclient.RuStoreBillingClient
import ru.rustore.sdk.billingclient.RuStoreBillingClientFactory
import ru.rustore.sdk.billingclient.model.purchase.PaymentResult
import ru.rustore.sdk.billingclient.usecase.PurchasesUseCase
import ru.rustore.sdk.billingclient.utils.pub.checkPurchasesAvailability
import ru.rustore.sdk.core.feature.model.FeatureAvailabilityResult
import java.util.*

class BillingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository by lazy { MainRepository(application) }

    val billingClient: RuStoreBillingClient by lazy {
        RuStoreBillingClientFactory.create(
            context = application,
            consoleApplicationId = APPLICATION_ID,
            deeplinkScheme = DEEPLINK
        )
    }

    private val _billingAvailability = MutableLiveData(false)
    val billingAvailability: LiveData<Boolean> = _billingAvailability

    private val _donationMade = MutableLiveData(false)
    val donationMade: LiveData<Boolean> = _donationMade

    fun checkAvailability() {
        RuStoreBillingClient.checkPurchasesAvailability(getApplication())
            .addOnSuccessListener { result ->
                when (result) {
                    FeatureAvailabilityResult.Available -> {
                        _billingAvailability.postValue(true)
                    }

                    is FeatureAvailabilityResult.Unavailable -> {
                        _billingAvailability.postValue(false)
                    }
                }
            }.addOnFailureListener {
                it.printStackTrace()
            }
    }

    fun donate(quantity: Int) {
        val purchasesUseCase: PurchasesUseCase = billingClient.purchases
        purchasesUseCase.purchaseProduct(
            productId = COFFEE_150,
            orderId = UUID.randomUUID().toString(),
            quantity = quantity,
            developerPayload = null,
        ).addOnSuccessListener { paymentResult: PaymentResult ->
            when (paymentResult) {
                is PaymentResult.Cancelled -> {}

                is PaymentResult.Failure -> {
                    if (paymentResult.errorCode == 40008) {
                        repository.donationMade = true
                        _donationMade.postValue(true)
                    }
                }

                is PaymentResult.Success -> {
                    repository.donationMade = true
                    _donationMade.postValue(true)
                }

                PaymentResult.InvalidPaymentState -> {}
            }
        }.addOnFailureListener {
            it.printStackTrace()
        }
    }

    companion object {
        const val COFFEE_150 = "coffee_150"
        const val APPLICATION_ID = "2063493570"
        const val DEEPLINK = "timetable"
        const val COFFEE_150_PRICE = 150
    }
}