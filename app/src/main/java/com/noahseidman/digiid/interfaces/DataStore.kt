package com.noahseidman.digiid.interfaces

import android.content.Context
import com.noahseidman.digiid.listeners.RestoreListener
import com.noahseidman.digiid.listeners.SaveListener

interface DataStore {
    fun save(context: Context, saveListener: SaveListener?)

    fun populate(context: Context, restoreListener: RestoreListener?)
}