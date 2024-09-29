package com.id.verification

import android.content.Context
import com.onfido.android.sdk.capture.token.TokenExpirationHandler

class ExpirationHandler : TokenExpirationHandler {
    private lateinit var context: Context
    override fun refreshToken(injectNewToken: (String?) -> Unit) {
        val newToken = "eyJhbGciOiJFUzUxMiJ9.eyJleHAiOjE3MjcyNDg4ODMsInBheWxvYWQiOnsiYXBwIjoiNjI5YjI3ZWUtMDRjYS00ZWJkLTg4NjktY2ZmYjg0ODdiM2IxIiwiYXBwbGljYXRpb25faWQiOiI5OGUwZGRhOC03NDZjLTRiNTItOGQ5Mi0yNjIyMWI1MTQ2ZGYiLCJjbGllbnRfdXVpZCI6Ijc3ZWQzNGJiLTFkZDktNDg2NS1iMjcwLTFkZTM1MDcyMTkzOSIsImlzX3NhbmRib3giOnRydWUsImlzX3NlbGZfc2VydmljZV90cmlhbCI6dHJ1ZSwiaXNfdHJpYWwiOnRydWUsInNhcmRpbmVfc2Vzc2lvbiI6Ijg4Njc1ZDdmLTBhMzMtNGI4Yi1hYmZjLWJhMWU3M2Y3MmU0ZSJ9LCJ1dWlkIjoicGxhdGZvcm1fc3RhdGljX2FwaV90b2tlbl91dWlkIiwidXJscyI6eyJkZXRlY3RfZG9jdW1lbnRfdXJsIjoiaHR0cHM6Ly9zZGsub25maWRvLmNvbSIsInN5bmNfdXJsIjoiaHR0cHM6Ly9zeW5jLm9uZmlkby5jb20iLCJob3N0ZWRfc2RrX3VybCI6Imh0dHBzOi8vaWQub25maWRvLmNvbSIsImF1dGhfdXJsIjoiaHR0cHM6Ly9hcGkub25maWRvLmNvbSIsIm9uZmlkb19hcGlfdXJsIjoiaHR0cHM6Ly9hcGkub25maWRvLmNvbSIsInRlbGVwaG9ueV91cmwiOiJodHRwczovL2FwaS5vbmZpZG8uY29tIn19.MIGHAkFLkOqh9KiXJENjINy24s2shatZeDT84FnICoUncQ5_SKXQwP9T3gYt17s2_K7JN0wX3L5m7cKMzhPARzFqMghlaAJCAcU1IhFuRM4dLpjbdq9OSCAeHvQNm4g9D9avanlZTSNJ30cBnulU_mFih5iLBf6dWynZKXmEIzmYyKw16jbd2rE0"
        injectNewToken(newToken)
    }

//    val config = OnfidoConfig.builder(context)
//        .withSDKToken("", tokenExpirationHandler = ExpirationHandler())
}


