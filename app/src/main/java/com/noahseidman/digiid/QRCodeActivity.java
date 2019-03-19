package com.noahseidman.digiid;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import com.noahseidman.digiid.listeners.SignalCompleteCallback;
import com.noahseidman.digiid.utils.AnimatorHelper;
import com.noahseidman.digiid.utils.FireBaseUtils;
import com.noahseidman.digiid.utils.QRUtils;

import java.util.Collections;
import java.util.List;

public class QRCodeActivity extends AppCompatActivity implements View.OnLongClickListener, View.OnClickListener {

    private static final int WRITE_STORAGE_PERMISSION = 2432;
    private static final int FIREBASE_CODE = 32443;
    private static final String SEED_PHRASE = "QRCodeActivity:SeedPhrase";
    private ActivityQrCodeBinding binding;
    private String phrase;
    private final ActivityQRCodeCallback callback = () -> onBackPressed();

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
                AnimatorHelper.animateBackgroundDim(binding.background, true, () -> finish());
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
            FragmentSignal.showBreadSignal(QRCodeActivity.this, getString(R.string.SavedToPictures), "", R.raw.success_check, new SignalCompleteCallback() {
                @Override
                public void onComplete() {

                }
            });
        }
    }

    @Override
    public boolean onLongClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.Options);
        CharSequence[] items = new CharSequence[2];
        items[0] = getString(R.string.SaveQR);
        items[1] = getString(R.string.Google);
        builder.setItems(items, (dialog, item) -> {
            switch (item) {
                case 0: {
                    if (ContextCompat
                            .checkSelfPermission(QRCodeActivity.this,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(QRCodeActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                WRITE_STORAGE_PERMISSION);
                    } else {
                        MediaStore.Images.Media.insertImage(getContentResolver(),
                                QRUtils.getQRImage(QRCodeActivity.this,
                                        getIntent().getStringExtra(SEED_PHRASE)),
                                "DigiID", "DigiID Backup");
                        FragmentSignal.showBreadSignal(QRCodeActivity.this, getString(R.string.SavedToPictures), "", R.raw.success_check, new SignalCompleteCallback() {
                            @Override
                            public void onComplete() {

                            }
                        });
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
            }
        });
        builder.setCancelable(true).setNegativeButton(R.string.button_cancel, (dialog, id) -> {
        });
        AlertDialog alert = builder.create();
        alert.show();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent d) {
        super.onActivityResult(requestCode, resultCode, d);
        if (requestCode == FIREBASE_CODE) {
            if (resultCode == RESULT_OK) {
                FireBaseUtils.Companion.save(getIntent().getStringExtra(SEED_PHRASE), this);
            }else {
                Crashlytics.log("Firebase Auth Failed");
            }
        }
    }

    @Override
    public void onClick(View v) {
        onLongClick(v);
    }
}