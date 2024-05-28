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
import java.util.UUID

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

    private val _donationMade = MutableLiveData(repository.donationMade)
    val donationMade: LiveData<Boolean> = _donationMade

    private val _purchaseId = MutableLiveData(repository.purchaseId)
    val purchaseId: LiveData<String> = _purchaseId

    private val _billingMade = MutableLiveData(false)
    val billingMade: LiveData<Boolean> = _billingMade

    fun checkAvailability() {
        if (billingAvailability.value == true)
            return

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

    fun buyMore() {
        _billingMade.postValue(false)
    }

    fun billingMade() {
        _billingMade.postValue(true)
    }

    fun donate(quantity: Int) {
        val purchasesUseCase: PurchasesUseCase = billingClient.purchases
        purchasesUseCase.purchaseProduct(
            productId = COFFEE,
            orderId = UUID.randomUUID().toString(),
            quantity = quantity,
            developerPayload = null,
        ).addOnSuccessListener { paymentResult: PaymentResult ->
            when (paymentResult) {
                is PaymentResult.Success -> {
                    repository.donationMade = true
                    _donationMade.postValue(true)
                    repository.purchaseId = paymentResult.purchaseId
                    _purchaseId.postValue(paymentResult.purchaseId)
                    purchasesUseCase.confirmPurchase(
                        purchaseId = paymentResult.purchaseId,
                        developerPayload = null
                    )
                        .addOnSuccessListener {
                            billingMade()
                        }.addOnFailureListener {
                            it.printStackTrace()
                        }
                }

                is PaymentResult.Cancelled -> {}
                is PaymentResult.Failure -> {}
                PaymentResult.InvalidPaymentState -> {}
            }
        }.addOnFailureListener {
            it.printStackTrace()
        }
    }

    companion object {
        const val APPLICATION_ID = "2063493570"
        const val DEEPLINK = "timetable"

        const val COFFEE = "coffee_150"
        const val COFFEE_PRICE = 150
    }
}