package com.example.callmind.features.postcall

import androidx.lifecycle.ViewModel
import com.example.callmind.core.communication.CommunicationLauncher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PostCallViewModel @Inject constructor(
    private val communicationLauncher: CommunicationLauncher
) : ViewModel() {

    fun callAgain(phoneNumber: String) {
        communicationLauncher.launchDialer(phoneNumber)
    }

    fun openWhatsApp(phoneNumber: String) {
        communicationLauncher.launchWhatsApp(phoneNumber)
    }

    fun openTelegram(phoneNumber: String) {
        communicationLauncher.launchTelegram(phoneNumber)
    }

    fun openSMS(phoneNumber: String) {
        communicationLauncher.launchSMS(phoneNumber)
    }

    fun openGPay(phoneNumber: String) {
        communicationLauncher.launchGPay(phoneNumber)
    }

    fun openContact(phoneNumber: String) {
        communicationLauncher.viewContact(phoneNumber)
    }

    fun copyNumber(phoneNumber: String) {
        communicationLauncher.copyToClipboard(phoneNumber)
    }
}
