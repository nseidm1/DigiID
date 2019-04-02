package com.noahseidman.digiid.utils

import android.content.Context
import java.security.SecureRandom
import java.util.*

object SeedUtil {

    external fun encodeSeed(seed: ByteArray, wordList: Array<String>): ByteArray

    fun generateRandomSeed(ctx: Context): String {
        var languageCode: String? = Locale.getDefault().language
        if (languageCode == null) {
            languageCode = "en"
        }
        val list = Bip39Reader.bip39List(ctx, languageCode)
        val randomSeed = SecureRandom().generateSeed(32)
        return String(encodeSeed(randomSeed, list.toTypedArray()))
    }

    fun getWordList(ctx: Context): List<String> {
        var languageCode: String? = Locale.getDefault().language
        if (languageCode == null) {
            languageCode = "en"
        }
        return Bip39Reader.bip39List(ctx, languageCode)
    }
}