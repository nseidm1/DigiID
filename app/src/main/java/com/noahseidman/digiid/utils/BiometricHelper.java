/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.noahseidman.digiid.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.biometrics.BiometricPrompt;
import com.noahseidman.digiid.MainActivity;
import com.noahseidman.digiid.R;
import com.noahseidman.digiid.listeners.SecurityPolicyCallback;

import java.util.concurrent.Executors;

/**
 * Small helper class to manage text/icon around fingerprint authentication UI.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class BiometricHelper extends FingerprintManager.AuthenticationCallback {

    public static boolean biometricAvailable(Context context) {
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(Activity.FINGERPRINT_SERVICE);
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) &&
                fingerprintManager != null &&
                fingerprintManager.isHardwareDetected() &&
                fingerprintManager.hasEnrolledFingerprints();
    }

    public static boolean isBiometricEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("fingerprint", biometricAvailable(context));
    }

    public static void enableBiometric(Context context, boolean enable) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("fingerprint", enable);
        editor.apply();
    }

    public static void processSecurityPolicy(MainActivity context, SecurityPolicyCallback securityPolicyCallback) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (!BiometricHelper.biometricAvailable(context)) {
            if (keyguardManager.isKeyguardSecure()) {
                securityPolicyCallback.proceed();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.SecurityRequirementTitle);
                builder.setIcon(R.drawable.ic_digiid);
                builder.setMessage(R.string.SecurityRequirementMessage);
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setPositiveButton(R.string.Security, (dialog, which) -> context.startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS)));
                builder.show();
            }
        } else if (BiometricHelper.isBiometricEnabled(context)) {
            BiometricPrompt prompt = new BiometricPrompt(context, Executors.newSingleThreadExecutor(), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    securityPolicyCallback.proceed();
                }

                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    securityPolicyCallback.failed();
                }

                @Override
                public void onAuthenticationFailed() {
                    securityPolicyCallback.failed();
                }
            });
            BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder();
            builder.setDescription(securityPolicyCallback.getDescription());
            builder.setTitle(securityPolicyCallback.getTitle());
            builder.setNegativeButtonText(context.getString(android.R.string.cancel));
            prompt.authenticate(builder.build());
        }
        else {
            securityPolicyCallback.proceed();
        }
    }
}