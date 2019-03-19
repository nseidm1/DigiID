package com.noahseidman.digiid.utils;

import com.noahseidman.digiid.MainActivity;

import javax.annotation.Nonnull;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

public class SeedUtil {

    @Nonnull
    public static String generateRandomSeed(@Nonnull final MainActivity ctx) {
        SecureRandom sr = new SecureRandom();
        final String[] words;
        List<String> list;
        String languageCode = Locale.getDefault().getLanguage();
        if (languageCode == null) {
            languageCode = "en";
        }
        list = Bip39Reader.bip39List(ctx, languageCode);
        words = list.toArray(new String[0]);
        final byte[] randomSeed = sr.generateSeed(32);
        return new String(ctx.encodeSeed(randomSeed, words));
    }
}
