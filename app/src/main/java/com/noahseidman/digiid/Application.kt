package com.noahseidman.digiid

import com.google.firebase.FirebaseApp

class Application : android.app.Application() {

    companion object {
        init {
            System.loadLibrary("core-lib")
        }
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
