package com.noahseidman.digiid.utils

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.crashlytics.android.Crashlytics
import com.jniwrappers.BRBIP32Sequence
import com.noahseidman.digiid.MainActivity
import com.noahseidman.digiid.NotificationFragment
import com.noahseidman.digiid.R
import com.noahseidman.digiid.listeners.SecurityPolicyCallback
import com.noahseidman.digiid.models.KeyModel
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MultipartBody
import okhttp3.Request
import org.bitcoinj.core.Base58
import org.bitcoinj.core.Utils
import java.net.URI
import java.util.concurrent.Executors
import kotlin.experimental.xor
import kotlin.math.floor


object DigiPassword {

    fun isDigiPassword(uri: String): Boolean {
        try {
            val bitIdUri = URI(uri)
            if ("digipassword".equals(bitIdUri.scheme, ignoreCase = true)) {
                return true
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return false
    }

    fun digiPasswordAuthPrompt(context: MainActivity, seed: String, url: String) {
        val uri = Uri.parse(url)
        val domain: String? = uri.host
        domain?.let {
            BiometricHelper.processSecurityPolicy(context, object : SecurityPolicyCallback {
                override fun proceed() {
                    Handler(Looper.getMainLooper()).post {
                        show(context, seed, url)
                    }
                }

                override fun getDescription(): String {
                    return it
                }

                override fun getTitle(): String {
                    return context.getString(R.string.BiometricAuthRequest)
                }
            })
        } ?: run {
            NotificationFragment.show(context, context.getString(R.string.InvalidWebsite), "", R.raw.error_check, null)
        }
    }

    private fun show(context: MainActivity, seed: String, url: String) {
        val uri = Uri.parse(url)
        val domain: String? = uri.host
        domain?.let {
            val password = getPassword(context, seed, it, getPasswordNumber(null))
            val showBuilder = AlertDialog.Builder(context)
            showBuilder.setTitle(R.string.Password)
            showBuilder.setIcon(R.drawable.ic_digiid)
            showBuilder.setMessage(password)
            showBuilder.setNeutralButton(android.R.string.cancel) { dialog, which -> }
            showBuilder.setPositiveButton(R.string.Copy) { dialog, which ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(context.getString(R.string.DigiIDSeedPhrase), password)
                clipboard.primaryClip = clip
                NotificationFragment.show(context, context.getString(R.string.CopiedToClipboard), "", R.raw.success_check, null)
            }
            showBuilder.setNegativeButton(R.string.Send) { dialog, which ->
                sendResponse(context, url, seed, it, 0)

            }
            showBuilder.show()
        }
    }

    fun getPassword(context: MainActivity, seedPhrase: String, domain: String, password_number: Int): String {
        //calculate sudo random number
        val seed = context.getSeedFromPhrase(TypesConverter.getNullTerminatedPhrase(seedPhrase.toByteArray()))
        val address = KeyModel(BRBIP32Sequence.getInstance().bip32PasswordKey(seed, 0, domain, password_number)).address()
        val key = Base58.decode(address)
        val seudoRandom = IntArray(20)
        for (i in 1..20) {
            seudoRandom[i - 1] = toUnsignedInt(key[i])
        }
        //generate a password based on seudoRandom
        val parts = ArrayList<String>()
        val wordList = SeedUtil.getWordList(context)
        for (i in 0..3) {
            val part = seudoRandom[i * 2] * 0x100 + seudoRandom[i * 2 + 1]
            var word = wordList[part % 2048]
            if (part >= 0x8000) {
                word = word.substring(0, 1).toUpperCase() + word.substring(1) //upper case if msb is 1
            }
            parts.add(word)
            parts.add("")
        }
        //put symbol in remaining places
        val symbol = "!@#\$%^&*-".get((seudoRandom[10] % 8))
        for (i in 1..8 step 2) {
            parts[i] = symbol.toString();
        }
        //add a 10 bit number in any of the 4 spaces
        val part = seudoRandom[8] * 0x100 + seudoRandom[9]
        val num = part % 1024
        val loc = (part / 0x4000) * 2 + 1
        val aParts = parts.toTypedArray()
        aParts[loc] = Integer.toString(num)
        //13*4 bits four word parts
        //10 bits for number
        //2 bits for location of number
        //3 bits for symbol
        //total 67
        return TextUtils.join("", aParts)
    }

    private fun sendResponse(context: MainActivity, url: String, seedPhrase: String, domain: String, password_number: Int) {
        val uri = Uri.parse(url)
        val s = uri.getQueryParameter("s")
        val nonce = uri.getQueryParameter("x")
        val p = uri.getQueryParameter("p")

        if (s.isNullOrEmpty() || nonce.isNullOrEmpty() || p.isNullOrEmpty()) throw IllegalArgumentException()
        if (s.length != 44) throw IllegalArgumentException()
        if (nonce.length < 10) throw IllegalArgumentException()

        val key = toUnsignedByteArray(Utils.HEX.decode(s))
        val callback = "https://$p"
        Log.d("DigiPassword", callback)
        val config = 3f
        val seed = context.getSeedFromPhrase(TypesConverter.getNullTerminatedPhrase(seedPhrase.toByteArray()))
        val payload = toUnsignedByteArray(randomSublistWithSpace(Base58.decode(KeyModel(BRBIP32Sequence.getInstance().bip32PasswordKey(seed, 0, domain, password_number)).address())))
        payload[20] = floor(config / 256f).toByte()
        payload[21] = (config % 256f).toByte()
        for (i in 0..21) {
            payload[i] = payload[i].xor(key[i])
        }

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("x", nonce).addFormDataPart("p", Utils.HEX.encode(payload)).build();
        val request = Request.Builder().url(callback).post(requestBody).build()
        val handler = Handler(Looper.getMainLooper())
        val executor = Executors.newSingleThreadExecutor()
        handler.post { context.progress.visibility = View.VISIBLE }
        executor.execute {
            val res = APIClient.getInstance(context).sendRequest(request)
            if (res.code() == 200 || res.code() == 201) {
                handler.post {
                    NotificationFragment.show(context, context.getString(R.string.DigiPasswordSuccess), context.getString(R.string.Transmitting), R.raw.success_check) {}
                    context.progress.visibility = View.INVISIBLE
                }
            } else {
                Crashlytics.getInstance().core.log("Server Response: " + res.code())
                handler.post {
                    NotificationFragment.show(context, Integer.toString(res.code()), context.getString(R.string.ErrorSigning), R.raw.error_check) {}
                    context.progress.visibility = View.INVISIBLE
                }
            }
        }
    }

    fun toUnsignedInt(x: Byte): Int {
        return x.toInt() and 0xff
    }

    fun toUnsignedByte(x: Byte): Byte {
        return (x.toInt() and 0xff).toByte()
    }

    fun getPasswordNumber(account: String?): Int {
        return 0
    }

    private fun toUnsignedByteArray(bytes: ByteArray): ByteArray {
        for(i in 0 until bytes.size) {
            bytes[i] = toUnsignedByte(bytes[i])
        }
        return bytes
    }

    private fun randomSublistWithSpace(bytes: ByteArray): ByteArray {
        val newBytes = ByteArray(22)
        for (i in 1..20) {
            newBytes[i - 1] = bytes[i]
        }
        return newBytes;
    }
}