package com.noahseidman.digiid.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.firebase.firestore.FirebaseFirestore
import com.noahseidman.digiid.listeners.OnCompleteListener
import com.noahseidman.digiid.listeners.RestoreListener
import com.noahseidman.digiid.listeners.SaveListener
import com.pvryan.easycrypt.extensions.asHexString
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

        fun save(seed: String, context: AppCompatActivity, saveListener: SaveListener) {
            updateAdId(context, object: OnCompleteListener {
                override fun onComplete() {
                    advertisingId?.let {
                        val ref = FirebaseFirestore.getInstance().collection("data").
                            document(MessageDigest.getInstance("SHA-256")
                                .digest(it.toByteArray(Charset.defaultCharset())).asHexString())
                        ref.get().addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {
                                val data = HashMap<String, Any>()
                                data["seed"] = seed
                                ref.update(data)
                                saveListener.onComplete()
                            } else {
                                val data = HashMap<String, Any>()
                                data["seed"] = seed
                                ref.set(data)
                                saveListener.onComplete()
                            }
                        }.addOnFailureListener {
                            saveListener.onFailure()
                        }
                    }
                }
                override fun onFailure() {
                    saveListener.onFailure()
                }
            })
        }

        fun restore(context: Context, restoreListener: RestoreListener) {
            updateAdId(context, object: OnCompleteListener {
                override fun onComplete() {
                    advertisingId?.let {
                        val ref = FirebaseFirestore.getInstance().collection("data").
                            document(MessageDigest.getInstance("SHA-256").
                                digest(it.toByteArray(Charset.defaultCharset())).asHexString())
                        ref.get().addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {
                                restoreListener.onComplete(documentSnapshot.getString("seed"))
                            } else {
                                restoreListener.onComplete(null)
                            }
                        }.addOnFailureListener {
                            restoreListener.onComplete(null)
                        }
                    }
                }
                override fun onFailure() {
                    restoreListener.onFailure()
                }
            })
        }

        fun updateAdId(context: Context, onCompleteListener: OnCompleteListener) {
            advertisingId?.let {
                onCompleteListener.onComplete()
            } ?: run {
                persister.execute {
                    try {
                        val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
                        advertisingId = info.id
                        handler.post { onCompleteListener.onComplete() }
                    } catch (e: IOException) {
                        handler.post { onCompleteListener.onFailure() }
                    } catch (e: GooglePlayServicesNotAvailableException) {
                        handler.post { onCompleteListener.onFailure() }
                    } catch (e: GooglePlayServicesRepairableException) {
                        handler.post { onCompleteListener.onFailure() }
                    } catch( e: Exception) {
                        handler.post { onCompleteListener.onFailure() }
                    }
                }
            }
        }
    }
}