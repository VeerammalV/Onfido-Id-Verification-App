package com.id.verification

import android.content.Context
import com.onfido.android.sdk.capture.token.TokenExpirationHandler

class ExpirationHandler : TokenExpirationHandler {
    private lateinit var context: Context
    override fun refreshToken(injectNewToken: (String?) -> Unit) {
        val newToken = " YOUR SDK TOKEN "
        injectNewToken(newToken)
    }

//    val config = OnfidoConfig.builder(context)
//        .withSDKToken("", tokenExpirationHandler = ExpirationHandler())
}


