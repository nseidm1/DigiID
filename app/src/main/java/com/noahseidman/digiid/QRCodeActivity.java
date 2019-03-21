package com.noahseidman.digiid;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import com.crashlytics.android.Crashlytics;
import com.firebase.ui.auth.AuthUI;
import com.noahseidman.digiid.databinding.ActivityQrCodeBinding;
import com.noahseidman.digiid.listeners.ActivityQRCodeCallback;
import com.noahseidman.digiid.listeners.OnAdIdUpdateListener;
import com.noahseidman.digiid.listeners.SaveListener;
import com.noahseidman.digiid.utils.AnimatorHelper;
import com.noahseidman.digiid.utils.FireBaseUtils;
import com.noahseidman.digiid.utils.QRUtils;
import com.pvryan.easycrypt.ECResultListener;
import com.pvryan.easycrypt.symmetric.ECSymmetric;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class QRCodeActivity extends AppCompatActivity implements View.OnLongClickListener, View.OnClickListener {

    private static final int WRITE_STORAGE_PERMISSION = 2432;
    private static final int FIREBASE_CODE = 32443;
    private static final String SEED_PHRASE = "QRCodeActivity:SeedPhrase";
    private ActivityQrCodeBinding binding;
    private String phrase;
    private final ActivityQRCodeCallback callback = this::onBackPressed;

    public static void show(AppCompatActivity activity, String seedPhrase) {
        Intent intent = new Intent(activity, QRCodeActivity.class);
        intent.putExtra(SEED_PHRASE, seedPhrase);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_qr_code);
        binding.setCallback(callback);
        binding.qrImage.setOnLongClickListener(this);
        binding.qrImage.setOnClickListener(this);
        phrase = getIntent().getStringExtra(SEED_PHRASE);
        populateQRImage();
        ObjectAnimator colorFade = AnimatorHelper.animateBackgroundDim(binding.background, false,
                null);
        colorFade.setDuration(500);
        colorFade.start();
    }

    private void populateQRImage() {
        QRUtils.generateQR(this, phrase, binding.qrImage);
        Animator fadeIn = ObjectAnimator.ofFloat(binding.qrImage, "alpha", 0f, 1f);
        fadeIn.setDuration(750);
        fadeIn.start();

    }

    @Override
    public void onBackPressed() {
        ObjectAnimator colorFade =
                AnimatorHelper.animateBackgroundDim(binding.background, true, this::finish);
        colorFade.setDuration(500);
        colorFade.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            MediaStore.Images.Media
                    .insertImage(getContentResolver(),
                            QRUtils.getQRImage(QRCodeActivity.this, phrase),
                            "DigiID", "DigiID Backup");
            NotificationFragment.show(QRCodeActivity.this, getString(R.string.SavedToPictures), "", R.raw.success_check, null);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.Options);
        CharSequence[] items = new CharSequence[3];
        items[0] = getString(R.string.SaveQR);
        items[1] = getString(R.string.Google);
        items[2] = getString(R.string.ShowSeedPhrase);
        builder.setItems(items, (d, i) -> {
            switch (i) {
                case 0: {
                    if (ContextCompat
                            .checkSelfPermission(QRCodeActivity.this,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(QRCodeActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                WRITE_STORAGE_PERMISSION);
                    } else {
                        MediaStore.Images.Media.insertImage(getContentResolver(),
                                QRUtils.getQRImage(QRCodeActivity.this, getIntent().getStringExtra(SEED_PHRASE)),
                                "DigiID", "DigiID Backup");
                        NotificationFragment.show(QRCodeActivity.this, getString(R.string.SavedToPictures), "", R.raw.success_check, null);
                    }
                    break;
                }
                case 1: {
                    List<AuthUI.IdpConfig> providers = Collections.singletonList(new AuthUI.IdpConfig.GoogleBuilder().build());
                    startActivityForResult(AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(), FIREBASE_CODE);
                    break;
                }
                case 2: {
                    FireBaseUtils.Companion.updateAdId(QRCodeActivity.this, new OnAdIdUpdateListener() {
                        @Override
                        public void onComplete() {
                            if (FireBaseUtils.Companion.getAdvertisingId() != null) {
                                ECSymmetric eCryptSymmetric = new ECSymmetric();
                                eCryptSymmetric.decrypt(phrase, FireBaseUtils.Companion.getAdvertisingId(), new ECResultListener() {
                                    @Override public void onProgress(int i, long l, long l1) {}
                                    @Override
                                    public <T> void onSuccess(T t) {
                                        new Handler(Looper.getMainLooper()).post(() -> {
                                            AlertDialog.Builder showBuilder = new AlertDialog.Builder(QRCodeActivity.this);
                                            showBuilder.setTitle(R.string.ShowSeedPhrase);
                                            showBuilder.setIcon(R.drawable.ic_digiid);
                                            showBuilder.setMessage(t.toString());
                                            showBuilder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {});
                                            showBuilder.setPositiveButton(R.string.Copy, (dialog, which) -> {
                                                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                                ClipData clip = ClipData.newPlainText(getString(R.string.DigiIDSeedPhrase), t.toString());
                                                clipboard.setPrimaryClip(clip);
                                                NotificationFragment.show(QRCodeActivity.this, getString(R.string.CopiedToClipboard), "", R.raw.success_check, null);
                                            });
                                            showBuilder.show();
                                        });
                                    }
                                    @Override
                                    public void onFailure(@NotNull String s, @NotNull Exception e) {
                                        NotificationFragment.show(QRCodeActivity.this, getString(R.string.CopyFailed), "", R.raw.error_check, null);
                                    }
                                });
                            }
                        }
                        @Override
                        public void onFailure() {
                            NotificationFragment.show(QRCodeActivity.this, getString(R.string.CopyFailed), "", R.raw.error_check, null);
                        }
                    });
                }
            }
        });
        builder.setCancelable(true).setNegativeButton(R.string.button_cancel, (dialog, id) -> {});
        AlertDialog alert = builder.create();
        alert.show();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent d) {
        super.onActivityResult(requestCode, resultCode, d);
        if (requestCode == FIREBASE_CODE) {
            if (resultCode == RESULT_OK) {
                FireBaseUtils.Companion.save(getIntent().getStringExtra(SEED_PHRASE), this, new SaveListener() {
                    @Override
                    public void onComplete() {
                        NotificationFragment.show(QRCodeActivity.this, getString(R.string.SavedToDrive), "", R.raw.success_check, null);
                    }

                    @Override
                    public void onFailure() {
                        NotificationFragment.show(QRCodeActivity.this, getString(R.string.BackupFailed), "", R.raw.error_check, null);
                    }
                });
            }else {
                Crashlytics.log("Firebase Auth Failed");
                NotificationFragment.show(QRCodeActivity.this, getString(R.string.BackupFailed), "", R.raw.error_check, null);
            }
        }
    }

    @Override
    public void onClick(View v) {
        onLongClick(v);
    }
}