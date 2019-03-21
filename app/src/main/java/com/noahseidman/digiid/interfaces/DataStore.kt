package com.noahseidman.digiid.interfaces

import com.noahseidman.digiid.MainActivity
import com.noahseidman.digiid.listeners.RestoreListener
import com.noahseidman.digiid.listeners.SaveListener

interface DataStore {
    fun save(context: MainActivity, saveListener: SaveListener)

    fun populate(context: MainActivity, restoreListener: RestoreListener)
}