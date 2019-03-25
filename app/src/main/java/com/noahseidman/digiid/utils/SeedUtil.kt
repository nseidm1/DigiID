package com.noahseidman.digiid.utils

import com.noahseidman.digiid.MainActivity
import java.security.SecureRandom
import java.util.*

object SeedUtil {

    fun generateRandomSeed(ctx: MainActivity): String {
        var languageCode: String? = Locale.getDefault().language
        if (languageCode == null) {
            languageCode = "en"
        }
        val list = Bip39Reader.bip39List(ctx, languageCode)
        val randomSeed = SecureRandom().generateSeed(32)
        return String(ctx.encodeSeed(randomSeed, list.toTypedArray()))
    }

    fun getWordList(ctx: MainActivity): List<String> {
        var languageCode: String? = Locale.getDefault().language
        if (languageCode == null) {
            languageCode = "en"
        }
        return Bip39Reader.bip39List(ctx, languageCode)
    }
}