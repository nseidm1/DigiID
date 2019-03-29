package com.noahseidman.digiid

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.*
import android.graphics.BitmapFactory
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.Nullable
import com.firebase.ui.auth.AuthUI
import com.google.common.io.ByteStreams
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.noahseidman.digiid.listeners.DataLoadListener
import com.noahseidman.digiid.listeners.RestoreListener
import com.noahseidman.digiid.listeners.SecurityPolicyCallback
import com.noahseidman.digiid.models.MainActivityDataModel
import com.noahseidman.digiid.utils.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.InputStream
import java.util.*


class MainActivity : BaseActivity(), CompoundButton.OnCheckedChangeListener {

    private val SELECT_IMAGE_RESTORE = 4334
    private val FIREBASE_RESTORE = 32443
    private var keyData = MainActivityDataModel()
    private var nfcAdapter: NfcAdapter? = null
    private var pendingNfcIntent: PendingIntent? = null
    external fun encodeSeed(seed: ByteArray, wordList: Array<String>): ByteArray
    external fun getSeedFromPhrase(phrase: ByteArray): ByteArray

    override fun onFingerprintClick() {
        handler.postDelayed({
            QRUtils.openScanner(this, QRUtils.SCANNER_REQUEST)
        }, 500)
    }

    override fun onRestoreClick() {
        BiometricHelper.processSecurityPolicy(this, object: SecurityPolicyCallback {
            override fun proceed() {
                handler.post { showRestoreDialog() }
            }
            override fun getDescription(): String {
                return getString(R.string.PleaseProvideAuth)
            }
            override fun getTitle(): String {
                return getString(R.string.BiometricAuthRequest)
            }
            override fun failed() {
            }
        })
    }

    private fun showRestoreDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.Options)
        val items = arrayOfNulls<CharSequence>(3)
        items[0] = getString(R.string.Scan)
        items[1] = getString(R.string.Image)
        items[2] = getString(R.string.Google)
        builder.setItems(items) { dialog, item ->
            when (item) {
                0 -> {
                    QRUtils.openScanner(this, QRUtils.SCANNER_RESTORE)
                }
                1 -> {
                    val intent = Intent()
                    intent.type = "image/*"
                    intent.action = Intent.ACTION_GET_CONTENT
                    startActivityForResult(intent, SELECT_IMAGE_RESTORE)
                }
                2 -> {
                    val providers = Arrays.asList(AuthUI.IdpConfig.GoogleBuilder().build())
                    startActivityForResult(
                        AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).build(),
                        FIREBASE_RESTORE
                    )
                }
            }
        }
        builder.setCancelable(true).setNegativeButton(R.string.button_cancel) { dialog, id -> }
        val alert = builder.create()
        alert.show()
    }

    override fun onBackupClick() {
        BiometricHelper.processSecurityPolicy(this, object: SecurityPolicyCallback {
            override fun proceed() {
                handler.post { QRCodeActivity.show(this@MainActivity, keyData.getStoredSeed(this@MainActivity)) }
            }
            override fun getDescription(): String {
                return getString(R.string.PleaseProvideAuth)
            }
            override fun getTitle(): String {
                return getString(R.string.BiometricAuthRequest)
            }
            override fun failed() {
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getData(null)
        supportFragmentManager.addOnBackStackChangedListener(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcAdapter?.let {
            pendingNfcIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0
            )
        }
        onNewIntent(intent)
        biometric_switch.isEnabled = BiometricHelper.biometricAvailable(this)
        biometric_switch.isChecked = biometric_switch.isEnabled && BiometricHelper.isBiometricEnabled(this)
        biometric_switch.setOnCheckedChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingNfcIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            getData(DataLoadListener {
                processDeepLink(it.data?.toString(), it.getStringExtra(Intent.EXTRA_TEXT))
                processNFC(it.getParcelableExtra(NfcAdapter.EXTRA_TAG))
                setIntent(null)
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        when (resultCode) {
            Activity.RESULT_OK -> {
                when (requestCode) {
                    QRUtils.SCANNER_REQUEST -> {
                        handler.postDelayed({
                            val result = intent?.getStringExtra("SCAN_RESULT")
                            result?.let {
                                if (DigiID.isDigiID(it)) {
                                    DigiID.digiIDAuthPrompt(this, it, false, keyData)
                                } else if (DigiPassword.isDigiPassword(it)) {
                                    DigiPassword.digiPasswordAuthPrompt(this@MainActivity, keyData.seed, it)
                                }
                            }
                        }, 500)
                    }
                    QRUtils.SCANNER_RESTORE -> {
                        val scanResult: String? = intent?.getStringExtra("SCAN_RESULT")
                        scanResult?.let {
                            this.keyData.restore(this, it, object : RestoreListener {
                                override fun onComplete(seed: String?) {
                                    seed?.let {
                                        restoreSuccessNotification()
                                    } ?: run {
                                        restoreFailedNotification()
                                    }
                                }
                                override fun onFailure() {
                                    restoreFailedNotification()
                                }
                            })
                        } ?: run {
                            restoreFailedNotification()
                        }
                    }
                    SELECT_IMAGE_RESTORE -> {
                        intent?.data?.let {
                            val inputStream: InputStream = contentResolver.openInputStream(it)!!
                            val rawBytes = ByteStreams.toByteArray(inputStream)
                            val bMap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                            val intArray = IntArray(bMap.width * bMap.height)
                            bMap.getPixels(intArray, 0, bMap.width, 0, 0, bMap.width, bMap.height)

                            val source = RGBLuminanceSource(bMap.width, bMap.height, intArray)
                            val bitmap = BinaryBitmap(HybridBinarizer(source))

                            val hintsMap = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
                            hintsMap[DecodeHintType.TRY_HARDER] = java.lang.Boolean.TRUE
                            hintsMap[DecodeHintType.POSSIBLE_FORMATS] = EnumSet.allOf(BarcodeFormat::class.java)
                            hintsMap[DecodeHintType.PURE_BARCODE] = java.lang.Boolean.FALSE

                            val reader = MultiFormatReader()
                            val result = reader.decode(bitmap, hintsMap)

                            this.keyData.restore(this, result.text, object : RestoreListener {
                                override fun onComplete(seed: String?) {
                                    seed?.let {
                                        restoreSuccessNotification()
                                    } ?: run {
                                        restoreFailedNotification()
                                    }
                                }
                                override fun onFailure() {
                                    restoreFailedNotification()
                                }
                            })
                        }
                    }
                    FIREBASE_RESTORE -> {
                        FireBaseUtils.restore(this, object : RestoreListener {
                            override fun onComplete(seed: String?) {
                                seed?.let {
                                    keyData.restore(this@MainActivity, it, object : RestoreListener {
                                        override fun onComplete(seed: String?) {
                                            seed?.let {
                                                restoreSuccessNotification()
                                            } ?: run {
                                                restoreFailedNotification()
                                            }
                                        }
                                        override fun onFailure() {
                                            restoreFailedNotification()
                                        }
                                    })
                                } ?: run {
                                    NotificationFragment.show(this@MainActivity, getString(R.string.NothingToRestore), "", R.raw.error_check) {}
                                }
                            }
                            override fun onFailure() {
                                restoreFailedNotification()
                            }
                        })
                    }
                }
            }
        }
    }

    private fun processDeepLink(@Nullable uriString : String?, @Nullable shareUrl: String?) {
        uriString?.let {
            if (DigiID.isDigiID(it)) {
                DigiID.digiIDAuthPrompt(this, it, true, keyData)
            } else if (DigiPassword.isDigiPassword(it)) {
                DigiPassword.digiPasswordAuthPrompt(this@MainActivity, keyData.seed, it)
            }
        }
        shareUrl?.let {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.DigiPassword)
            builder.setIcon(R.drawable.ic_digiid)
            val password = DigiPassword.getPassword(this@MainActivity, keyData.seed, it, 0)
            builder.setMessage(getString(R.string.DigiPasswordDescription) + "\n\n" + password)
            builder.setNegativeButton(R.string.Simple, object:DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(getString(R.string.DigiPassword), password)
                    clipboard.primaryClip = clip
                    NotificationFragment.show(this@MainActivity, getString(R.string.CopiedToClipboard), "", R.raw.success_check, null)
                }
            })
            builder.setPositiveButton(R.string.Advanced, object: DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    val i = Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("https://www.openantumid.com/digipassword.php?generate=" + it))
                    startActivity(i);
                }
            })
            builder.setNeutralButton(R.string.Cancel, null)
            builder.show()
        }
    }

    private fun processNFC(@Nullable tag: Tag?) {
        tag?.let {
            val ndef = Ndef.get(tag) ?: return
            val ndefMessage = ndef.cachedNdefMessage ?: return
            val records = ndefMessage.records ?: return
            for (ndefRecord in records) {
                try {
                    val record = String(ndefRecord.payload)
                    if (record.contains("digiid")) {
                        DigiID.digiIDAuthPrompt(this, record.substring(record.indexOf("digiid")), false, keyData)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun getData(dataLoadListener: DataLoadListener?) {
        keyData.populate(this, object : RestoreListener {
            override fun onComplete(seed: String?) {
                dataLoadListener?.onDataLoaded()
            }
            override fun onFailure() {
               handler.post { Toast.makeText(this@MainActivity, R.string.IdentityUnavailable, Toast.LENGTH_SHORT).show() }
            }
        })
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (!isChecked) {
            BiometricHelper.processSecurityPolicy(this, object : SecurityPolicyCallback {
                override fun getDescription(): String {
                    return getString(R.string.PleaseProvideAuth)
                }

                override fun getTitle(): String {
                    return getString(R.string.BiometricAuthRequest)
                }

                override fun proceed() {
                    handler.post { BiometricHelper.enableBiometric(this@MainActivity, false) }
                }

                override fun failed() {
                    handler.post {
                        BiometricHelper.enableBiometric(this@MainActivity, true)
                        biometric_switch.isChecked = true
                    }
                }
            })
        } else {
            BiometricHelper.enableBiometric(this@MainActivity, isChecked)
        }
    }

    fun restoreSuccessNotification() {
        NotificationFragment.show(this@MainActivity, getString(R.string.Restored), "", R.raw.success_check) {}
    }

    fun restoreFailedNotification() {
        NotificationFragment.show(this@MainActivity, getString(R.string.RestoreFailed), "", R.raw.error_check) {}
    }

    fun getProgressBar(): ProgressBar {
        return progress
    }
}