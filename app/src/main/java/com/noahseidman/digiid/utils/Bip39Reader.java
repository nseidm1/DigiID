package com.noahseidman.digiid.utils;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 9/28/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

class Bip39Reader {

    private static final String TAG = Bip39Reader.class.getName();
    private static final int WORD_LIST_SIZE = 2048;
    private static final String[] LANGS = {"en", "es", "fr", "ja", "zh"};

    //if lang is null then all the lists
    static List<String> bip39List(Context context, String lang) {

        String[] langs = null;
        if (lang == null)
            langs = LANGS; //return all the words for all langs
        else {
            boolean exists = false;
            for (String s : LANGS) if (s.equalsIgnoreCase(lang)) exists = true;
            if (exists)
                langs = new String[]{lang};//if lang is one of the language we support for paper key creation, then use it
            else
                langs = new String[]{"en"};// if not than return 'en'
        }

        List<String> result = new ArrayList<>();

        for (String l : langs) {
            String fileName = "words/" + l + "-BIP39Words.txt";
            List<String> wordList = new ArrayList<>();
            BufferedReader reader = null;
            try {
                AssetManager assetManager = context.getResources().getAssets();
                InputStream inputStream = assetManager.open(fileName);
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;

                while ((line = reader.readLine()) != null) {
                    wordList.add(cleanWord(line));
                }
            } catch (Exception ex) {
                ex.printStackTrace();

            } finally {
                try {
                    if (reader != null)
                        reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (wordList.size() % WORD_LIST_SIZE != 0) {
                throw new IllegalStateException("Empty Words List");
            }
            result.addAll(wordList);

        }
        return result;
    }

    private static String cleanWord(String word) {
        return Normalizer.normalize(word.trim().replace("　", "")
                .replace(" ", ""), Normalizer.Form.NFKD);
    }
}
