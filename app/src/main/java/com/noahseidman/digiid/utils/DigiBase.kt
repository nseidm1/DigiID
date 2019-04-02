package com.noahseidman.digiid.utils

abstract class DigiBase {
    external fun getSeedFromPhrase(phrase: ByteArray): ByteArray
}