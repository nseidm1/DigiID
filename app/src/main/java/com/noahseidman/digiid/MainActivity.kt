package com.noahseidman.digiid

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.widget.CompoundButton
import androidx.annotation.Nullable
import com.firebase.ui.auth.AuthUI
import com.google.common.io.ByteStreams
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.noahseidman.digiid.listeners.BRAuthCompletionCallback
import com.noahseidman.digiid.listeners.RestoreListener
import com.noahseidman.digiid.models.MainActivityDataModel
import com.noahseidman.digiid.utils.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.InputStream
import java.util.*



class MainActivity : BaseActivity(), CompoundButton.OnCheckedChangeListener {

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        BiometricHelper.enableBiometric(this, isChecked)
    }

    private val SELECT_IMAGE_RESTORE: Int = 4334
    private val FIREBASE_RESTORE = 32443
    private var keyData: MainActivityDataModel = MainActivityDataModel()
    private var nfcAdapter: NfcAdapter? = null
    private var pendingNfcIntent: PendingIntent? = null
    external fun encodeSeed(seed: ByteArray, wordList: Array<String>): ByteArray
    external fun getSeedFromPhrase(phrase: ByteArray): ByteArray

    override fun onComplete(authType: BRAuthCompletionCallback.AuthType) {
        val seed = getSeedFromPhrase(TypesConverter.getNullTerminatedPhrase(keyData.seed.toByteArray(Charsets.UTF_8)))
        DigiID.digiIDSignAndRespond(this, authType.bitId, authType.deepLink, authType.callbackUrl, seed)
    }

    override fun onCancel(type: BRAuthCompletionCallback.AuthType) {
    }

    override fun onFingerprintClick() {
        handler.postDelayed({
            QRUtils.openScanner(this, QRUtils.SCANNER_REQUEST)
        }, 500)
    }

    override fun onRestoreClick() {
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
        QRCodeActivity.show(this, keyData.getStoredSeed(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getData()
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
        processNFC(intent)
        processDeepLink(intent)
        fingerprint_switch.isEnabled = BiometricHelper.biometricAvailable(this)
        fingerprint_switch.isChecked = fingerprint_switch.isEnabled && BiometricHelper.isBiometricEnabled(this)
        fingerprint_switch.setOnCheckedChangeListener(this)
        intent = Intent()
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
        processDeepLink(intent)
        processNFC(intent)
        this.intent = Intent()
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
                                DigiID.digiIDAuthPrompt(this, result, false, keyData)
                            }
                        }, 500)
                    }
                    QRUtils.SCANNER_RESTORE -> {
                        val keyData: String? = intent?.getStringExtra("SCAN_RESULT")
                        keyData?.let {
                            this.keyData.restore(this, it, object : RestoreListener {
                                override fun onComplete(seed: String?) {
                                    if (seed.isNullOrEmpty()) {
                                        FragmentSignal.showBreadSignal(this@MainActivity, getString(R.string.RestoreFailed), "", R.raw.error_check) {}
                                    } else {
                                        FragmentSignal.showBreadSignal(this@MainActivity, getString(R.string.Restored), "", R.raw.success_check) {}
                                    }
                                }
                            })
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
                                    if (seed.isNullOrEmpty()) {
                                        FragmentSignal.showBreadSignal(this@MainActivity, getString(R.string.RestoreFailed), "", R.raw.error_check) {}
                                    } else {
                                        FragmentSignal.showBreadSignal(this@MainActivity, getString(R.string.Restored), "", R.raw.success_check) {}
                                    }
                                }
                            })
                        }
                    }
                    FIREBASE_RESTORE -> {
                        FireBaseUtils.restore(this, object : RestoreListener {
                            override fun onComplete(seed: String?) {
                                if (seed == null) {
                                    FragmentSignal.showBreadSignal(this@MainActivity, getString(R.string.NothingToRestore), "", R.raw.error_check) {}
                                } else {
                                    keyData.restore(this@MainActivity, seed, object : RestoreListener {
                                        override fun onComplete(seed: String?) {
                                            if (seed.isNullOrEmpty()) {
                                                FragmentSignal.showBreadSignal(this@MainActivity, getString(R.string.RestoreFailed), "", R.raw.error_check) {}
                                            } else {
                                                FragmentSignal.showBreadSignal(this@MainActivity, getString(R.string.Restored), "", R.raw.success_check) {}
                                            }
                                        }
                                    })
                                }
                            }
                        })
                    }
                }
            }
        }
    }

    private fun processDeepLink(@Nullable intent: Intent?) {
        intent?.data?.let {
            if (DigiID.isBitId(it.toString())) {
                DigiID.digiIDAuthPrompt(this, it.toString(), true, keyData)
            }
        }
    }

    private fun processNFC(@Nullable intent: Intent?) {
        val tag = intent?.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
        val ndef = Ndef.get(tag) ?: return
        val ndefMessage = ndef.cachedNdefMessage ?: return
        val records = ndefMessage.records ?: return
        for (ndefRecord in records) {
            try {
                val record = String(ndefRecord.payload)
                if (record.contains("digiid")) {
                    DigiID.digiIDAuthPrompt(this, record, false, keyData)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getData() {
        keyData.populate(this, object : RestoreListener {
            override fun onComplete(seed: String?) {
                if (keyData.seed.isEmpty()) {
                    keyData = MainActivityDataModel(SeedUtil.generateRandomSeed(this@MainActivity))
                    keyData.save(this@MainActivity)
                }
                Log.d("SeedWatch", keyData.seed)
            }
        })
    }
}