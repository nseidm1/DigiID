package com.noahseidman.digiid.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.firebase.firestore.FirebaseFirestore
import com.noahseidman.digiid.FragmentSignal
import com.noahseidman.digiid.R
import com.noahseidman.digiid.listeners.OnAdIdUpdateListener
import com.noahseidman.digiid.listeners.RestoreListener
import java.io.IOException
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.Executors

class FireBaseUtils {

    companion object {
        private val handler = Handler(Looper.getMainLooper())
        private val persister = Executors.newSingleThreadExecutor()
        var advertisingId: String? = null

        fun save(seed: String, context: AppCompatActivity) {
            updateAdId(context, object: OnAdIdUpdateListener {
                override fun onComplete() {
                    val ref = FirebaseFirestore.getInstance().collection("data").
                        document(String(MessageDigest.getInstance("SHA-256").digest(advertisingId!!.toByteArray(Charset.defaultCharset()))))
                    ref.get().addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val data = HashMap<String, Any>()
                            data["seed"] = seed
                            ref.update(data)
                            FragmentSignal.showBreadSignal(context, context.getString(R.string.SavedToDrive), "", R.raw.success_check) { }
                        } else {
                            val data = HashMap<String, Any>()
                            data["seed"] = seed
                            ref.set(data)
                            FragmentSignal.showBreadSignal(context, context.getString(R.string.SavedToDrive), "", R.raw.success_check) { }
                        }
                    }.addOnFailureListener {
                        FragmentSignal.showBreadSignal(context, context.getString(R.string.BackupFailed), "", R.raw.error_check) { }
                    }
                }
            })
        }

        fun restore(context: Context, restoreListener: RestoreListener) {
            updateAdId(context, object: OnAdIdUpdateListener {
                override fun onComplete() {
                    val ref = FirebaseFirestore.getInstance().collection("data").
                        document(String(MessageDigest.getInstance("SHA-256").digest(advertisingId!!.toByteArray(Charset.defaultCharset()))))
                    ref.get().addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val seed: String = documentSnapshot.getString("seed")!!
                            restoreListener.onComplete(seed)
                        } else {
                            restoreListener.onComplete(null)
                        }
                    }
                }
            })
        }

        fun updateAdId(context: Context, onCompleteListener: OnAdIdUpdateListener) {
            if (advertisingId.isNullOrEmpty()) {
                persister.execute {
                    try {
                        val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
                        advertisingId = info.id
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } catch (e: GooglePlayServicesNotAvailableException) {
                        e.printStackTrace()
                    } catch (e: GooglePlayServicesRepairableException) {
                        e.printStackTrace()
                    }
                    handler.post {
                        onCompleteListener.onComplete()
                    }
                }
            } else {
                onCompleteListener.onComplete()
            }
        }
     }
}