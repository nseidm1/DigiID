package com.noahseidman.digiid.interfaces

import android.content.Context
import com.noahseidman.digiid.listeners.RestoreListener

interface DataStore {
    fun save(context: Context)

    fun populate(context: Context, restoreListener: RestoreListener)
}