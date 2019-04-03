package com.noahseidman.digiid

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.noahseidman.digiid.listeners.SecurityPolicyCallback
import com.noahseidman.digiid.utils.BiometricHelper

class BiometricActivity : AppCompatActivity() {

    companion object {
        val URL = "URL"
        var showing = false
        const val EXPIRATION = 600000
        var timeAuthorized = 0L
        var canceled = false

        fun show(context: Context, url: String): Boolean {
            if (System.currentTimeMillis() > timeAuthorized + EXPIRATION) {
                if (!showing) {
                    showing = true
                    val intent = Intent(context, BiometricActivity::class.java)
                    intent.putExtra(URL, url)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
                    context.startActivity(intent)
                }
                return true
            }
            PasswordViewService.biometricAuth(context)
            return false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BiometricHelper.processSecurityPolicy(this, object : SecurityPolicyCallback {
            override fun getDescription(): String {
                return intent.getStringExtra(URL)
            }

            override fun getTitle(): String {
                return getString(R.string.BiometricAuthRequest)
            }

            override fun proceed() {
                timeAuthorized = System.currentTimeMillis()
                finish()
                PasswordViewService.biometricAuth(this@BiometricActivity)
            }

            override fun failed() {
                canceled = true
                finish()
            }
        })
    }

    override fun finish() {
        showing = false
        super.finish()
    }

    override fun onDestroy() {
        showing = false
        super.onDestroy()
    }
}
