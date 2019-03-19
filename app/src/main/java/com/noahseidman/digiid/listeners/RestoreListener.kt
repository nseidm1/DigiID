package com.noahseidman.digiid.listeners

interface RestoreListener {
    fun onComplete(seed: String?)
}