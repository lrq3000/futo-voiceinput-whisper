package org.futo.voiceinput.payments

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import org.futo.voiceinput.openURI

class PayPalBilling(val context: Context, private val coroutineScope: CoroutineScope) : BillingImpl {
    companion object {
        fun isAllowed(): Boolean {
            return true
        }
    }

    override fun checkAlreadyOwnsProduct() {

    }

    override fun startConnection(onReady: () -> Unit) {
        onReady()
    }

    override fun onResume() {
    }

    override fun launchBillingFlow() {
        context.openURI("https://pay2.futo.org/checkout/polar/futo-keyboard/futo-keyboard-voiceinput/checkout-ready?success=redirect-to-organization-page")
    }

    override fun supportsCheckingIfAlreadyOwnsProduct(): Boolean {
        return false
    }

    override fun getName(): String {
        return ""
    }
}