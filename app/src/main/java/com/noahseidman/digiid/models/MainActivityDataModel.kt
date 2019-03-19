package com.noahseidman.digiid.models

import android.content.Context
import android.preference.PreferenceManager
import com.noahseidman.digiid.interfaces.DataStore
import com.noahseidman.digiid.listeners.OnAdIdUpdateListener
import com.noahseidman.digiid.listeners.RestoreListener
import com.noahseidman.digiid.utils.FireBaseUtils
import com.pvryan.easycrypt.ECResultListener
import com.pvryan.easycrypt.symmetric.ECSymmetric

data class MainActivityDataModel(var seed: String = String()) : DataStore {

    override fun save(context: Context) {
        FireBaseUtils.updateAdId(context, object : OnAdIdUpdateListener {
            override fun onComplete() {
                val eCryptSymmetric = ECSymmetric()
                eCryptSymmetric.encrypt(seed, FireBaseUtils.advertisingId!!, object :
                    ECResultListener {
                    override fun onProgress(i: Int, l: Long, l1: Long) {}
                    override fun onFailure(s: String, e: Exception) {}

                    override fun <T> onSuccess(result: T) {
                        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                        val editor = prefs.edit()
                        editor.putString("seed", result as String)
                        editor.apply()
                    }
                })
            }
        })
    }

    fun getStoredSeed(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString("seed", "")
    }

    fun restore(context: Context, seed: String, restoreListener: RestoreListener) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        editor.putString("seed", seed)
        editor.apply()
        val eCryptSymmetric = ECSymmetric()
        eCryptSymmetric.decrypt(seed, FireBaseUtils.advertisingId!!, object : ECResultListener {
            override fun onProgress(i: Int, l: Long, l1: Long) {}
            override fun onFailure(s: String, e: Exception) {
                restoreListener.onComplete(null)
            }

            override fun <T> onSuccess(result: T) {
                this@MainActivityDataModel.seed = result as String
                restoreListener.onComplete(this@MainActivityDataModel.seed)
            }
        })
    }

    override fun populate(context: Context, restoreListener: RestoreListener) {
        FireBaseUtils.updateAdId(context, object : OnAdIdUpdateListener {
            override fun onComplete() {
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val eCryptSymmetric = ECSymmetric()
                eCryptSymmetric.decrypt(prefs.getString("seed", ""), FireBaseUtils.advertisingId!!, object :
                    ECResultListener {
                    override fun onProgress(i: Int, l: Long, l1: Long) {}
                    override fun onFailure(s: String, e: Exception) {
                        restoreListener.onComplete(null)
                    }

                    override fun <T> onSuccess(result: T) {
                        seed = result as String
                        restoreListener.onComplete(seed)
                    }
                })

            }
        })
    }
}