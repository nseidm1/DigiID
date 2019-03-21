package com.noahseidman.digiid.listeners

import androidx.annotation.Nullable

interface RestoreListener {
    fun onComplete(@Nullable seed: String?)
    fun onFailure()
}