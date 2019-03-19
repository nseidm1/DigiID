package com.noahseidman.digiid

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import com.google.zxing.client.android.CaptureActivity
import com.noahseidman.digiid.databinding.ActivityMainBinding
import com.noahseidman.digiid.listeners.BRAuthCompletionCallback
import com.noahseidman.digiid.listeners.MainActivityCallback
import com.noahseidman.digiid.listeners.OnBackPressListener
import com.noahseidman.digiid.utils.QRUtils
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class BaseActivity : AppCompatActivity(), MainActivityCallback,
        BRAuthCompletionCallback, FragmentManager.OnBackStackChangedListener {

    val handler: Handler = Handler(Looper.getMainLooper())
    val persister: ExecutorService = Executors.newSingleThreadExecutor()
    private val backClickListeners = CopyOnWriteArrayList<OnBackPressListener>()

    companion object {
        init {
            System.loadLibrary("core-lib")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main).callback = this
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            QRUtils.CAMERA_REQUEST_ID -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    handler.postDelayed({
                        val intent = Intent(this, CaptureActivity::class.java)
                        intent.action = "com.google.zxing.client.android.SCAN"
                        intent.putExtra("SAVE_HISTORY", false)
                        startActivityForResult(intent, QRUtils.SCANNER_REQUEST)
                    }, 500)
                }
            }
            QRUtils.SCANNER_RESTORE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    handler.postDelayed({
                        val intent = Intent(this, CaptureActivity::class.java)
                        intent.action = "com.google.zxing.client.android.SCAN"
                        intent.putExtra("SAVE_HISTORY", false)
                        startActivityForResult(intent, QRUtils.SCANNER_RESTORE)
                    }, 500)
                }
            }
        }
    }

    override fun onBackPressed() {
        if (backClickListeners.size == 0) {
            super.onBackPressed()
        } else {
            for (onBackPressListener in backClickListeners) {
                onBackPressListener.onBackPressed()
            }
        }
    }

    override fun onBackStackChanged() {
        //Add back press listeners
        backClickListeners.clear()
        for (i in 0 until supportFragmentManager.backStackEntryCount) {
            val backStackEntry = supportFragmentManager.getBackStackEntryAt(i)
            val fragment = supportFragmentManager.findFragmentByTag(backStackEntry.name)
            if (fragment is OnBackPressListener) {
                val onBackPressListener = fragment as OnBackPressListener
                if (!backClickListeners.contains(onBackPressListener)) {
                    backClickListeners.add(onBackPressListener)
                }
            }
        }
    }
}