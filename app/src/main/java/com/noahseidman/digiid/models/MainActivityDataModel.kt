package com.noahseidman.digiid.models

import android.content.Context
import android.preference.PreferenceManager
import com.noahseidman.digiid.MainActivity
import com.noahseidman.digiid.interfaces.DataStore
import com.noahseidman.digiid.listeners.OnAdIdUpdateListener
import com.noahseidman.digiid.listeners.RestoreListener
import com.noahseidman.digiid.listeners.SaveListener
import com.noahseidman.digiid.utils.FireBaseUtils
import com.noahseidman.digiid.utils.SeedUtil
import com.pvryan.easycrypt.ECResultListener
import com.pvryan.easycrypt.symmetric.ECSymmetric

data class MainActivityDataModel(var seed: String = String()) : DataStore {

    override fun save(context: MainActivity, saveListener: SaveListener) {
        FireBaseUtils.updateAdId(context, object : OnAdIdUpdateListener {
            override fun onComplete() {
                FireBaseUtils.advertisingId?.let {
                    val eCryptSymmetric = ECSymmetric()
                    eCryptSymmetric.encrypt(seed, it, object : ECResultListener {
                        override fun onProgress(i: Int, l: Long, l1: Long) {}
                        override fun <T> onSuccess(result: T) {
                            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                            val editor = prefs.edit()
                            editor.putString("seed", result as String)
                            editor.apply()
                            saveListener.onComplete()
                        }
                        override fun onFailure(s: String, e: Exception) {
                            saveListener.onFailure()
                        }
                    })
                }
            }
            override fun onFailure() {
                saveListener.onFailure()
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
        FireBaseUtils.updateAdId(context, object : OnAdIdUpdateListener {
            override fun onComplete() {
                FireBaseUtils.advertisingId?.let {
                    eCryptSymmetric.decrypt(seed, it, object : ECResultListener {
                        override fun onProgress(i: Int, l: Long, l1: Long) {}
                        override fun <T> onSuccess(result: T) {
                            this@MainActivityDataModel.seed = result as String
                            restoreListener.onComplete(this@MainActivityDataModel.seed)
                        }
                        override fun onFailure(s: String, e: Exception) {
                            restoreListener.onComplete(null)
                        }
                    })
                }
            }
            override fun onFailure() {
                restoreListener.onFailure()
            }
        })
    }

    override fun populate(context: MainActivity, restoreListener: RestoreListener) {
        FireBaseUtils.updateAdId(context, object : OnAdIdUpdateListener {
            override fun onComplete() {
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val eCryptSymmetric = ECSymmetric()
                FireBaseUtils.advertisingId?.let {
                    eCryptSymmetric.decrypt(prefs.getString("seed", ""), it, object : ECResultListener {
                        override fun onProgress(i: Int, l: Long, l1: Long) {}
                        override fun <T> onSuccess(result: T) {
                            seed = result as String
                            restoreListener.onComplete(seed)
                        }
                        override fun onFailure(s: String, e: Exception) {
                            seed = SeedUtil.generateRandomSeed(context)
                            save(context, object : SaveListener {
                                override fun onComplete() {
                                    restoreListener.onComplete(seed)
                                }
                                override fun onFailure() {
                                    restoreListener.onFailure()
                                }
                            })
                        }
                    })
                }
            }
            override fun onFailure() {
                restoreListener.onFailure()
            }
        })
    }
}