package com.noahseidman.digiid.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import com.crashlytics.android.Crashlytics;
import com.jniwrappers.BRBIP32Sequence;
import com.noahseidman.digiid.NotificationFragment;
import com.noahseidman.digiid.MainActivity;
import com.noahseidman.digiid.R;
import com.noahseidman.digiid.models.KeyModel;
import com.noahseidman.digiid.models.MainActivityDataModel;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DigiID {
    private static final String BITCOIN_SIGNED_MESSAGE_HEADER = "DigiByte Signed Message:\n";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static boolean isBitId(String uri) {
        try {
            URI bitIdUri = new URI(uri);
            if ("digiid".equalsIgnoreCase(bitIdUri.getScheme())) {
                return true;
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static void digiIDAuthPrompt(@NonNull final MainActivity context, @NonNull String bitID, boolean isDeepLink, @NonNull MainActivityDataModel keyData) {
        Uri bitUri = Uri.parse(bitID);
        String scheme = "https://";
        if (!BiometricHelper.biometricAvailable(context)) {
            KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager.isKeyguardSecure()) {
                byte[] seed = context.getSeedFromPhrase(TypesConverter.getNullTerminatedPhrase(keyData.getSeed().getBytes()));
                digiIDSignAndRespond(context, bitID, isDeepLink, scheme + bitUri.getHost() + bitUri.getPath(), seed);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.SecurityRequirementTitle);
                builder.setIcon(R.drawable.ic_digiid);
                builder.setMessage(R.string.SecurityRequirementMessage);
                builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {});
                builder.setPositiveButton(R.string.Security, (dialog, which) -> {
                    Intent intent=new Intent(Settings.ACTION_SECURITY_SETTINGS);
                    context.startActivity(intent);
                });
                builder.show();
            }
        } else if (BiometricHelper.isBiometricEnabled(context)) {
            BiometricPrompt prompt = new BiometricPrompt(context, Executors.newSingleThreadExecutor(), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, R.string.BiometricAuthFailure, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(context, R.string.BiometricAuthSuccessTransmitting, Toast.LENGTH_SHORT).show();
                        byte[] seed = context.getSeedFromPhrase(TypesConverter.getNullTerminatedPhrase(keyData.getSeed().getBytes()));
                        digiIDSignAndRespond(context, bitID, isDeepLink, scheme + bitUri.getHost() + bitUri.getPath(), seed);
                    });
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, R.string.BiometricAuthFailure, Toast.LENGTH_SHORT).show());
                }
            });
            BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder();
            builder.setDescription(scheme + bitUri.getHost());
            builder.setTitle(context.getString(R.string.BiometricAuthRequest));
            builder.setNegativeButtonText(context.getString(android.R.string.cancel));
            prompt.authenticate(builder.build());
        } else {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, R.string.BiometricAuthSuccessTransmitting, Toast.LENGTH_SHORT).show());
            byte[] seed = context.getSeedFromPhrase(TypesConverter.getNullTerminatedPhrase(keyData.getSeed().getBytes()));
            digiIDSignAndRespond(context, bitID, isDeepLink, scheme + bitUri.getHost() + bitUri.getPath(), seed);
        }
    }

    private static void digiIDSignAndRespond(@NonNull final Activity app, @NonNull String bitID,
                                            boolean isDeepLink, String callbackUrl, byte[] seed) {
        Handler handler = new Handler(Looper.getMainLooper());
        try {
            Uri bitUri = Uri.parse(bitID);
            String nonce = bitUri.getQueryParameter("x");
            if (TextUtils.isEmpty(nonce)) {
                bitID = bitUri.buildUpon().appendQueryParameter("x",
                        Long.toHexString(System.currentTimeMillis())).build().toString();
            }
            final byte[] key = BRBIP32Sequence.getInstance().bip32BitIDKey(seed, 0, callbackUrl);
            final String sig = signMessage(bitID, new KeyModel(key));
            final String address = new KeyModel(key).address();
            final JSONObject postJson = new JSONObject();
            postJson.put("uri", bitID);
            postJson.put("address", address);
            postJson.put("signature", sig);
            final RequestBody requestBody = RequestBody.create(null, postJson.toString());
            final Request request = new Request.Builder()
                    .url(callbackUrl)
                    .post(requestBody)
                    .header("Content-Type", "application/json")
                    .build();
            executor.execute(() -> {
                final Response res = APIClient.getInstance(app).sendRequest(request);
                Log.d(DigiID.class.getSimpleName(),
                        "Response: " + res.code() + ", Message: " + res.message());
                if (res.code() == 200) {
                    handler.post(
                            () -> NotificationFragment.show((AppCompatActivity) app, app.getString(R.string.DigiIDSuccess),
                            app.getString(R.string.Transmitting), R.raw.success_check, () -> {
                                        if (isDeepLink) {
                                            app.finishAffinity();
                                        }
                                   }));
                } else {
                    Crashlytics.getInstance().core.log("Server Response: " + res.code());
                    handler.post(
                            () -> NotificationFragment.show((AppCompatActivity) app,  Integer.toString(res.code()),
                                    app.getString(R.string.ErrorSigning), R.raw.error_check, () -> {
                                        if (isDeepLink) {
                                            app.finishAffinity();
                                        }
                                    }));
                }
            });
        } catch (Exception e) {
            Crashlytics.getInstance().core.logException(e);
            e.printStackTrace();
            handler.post(
                    () -> NotificationFragment.show((AppCompatActivity) app, app.getString(R.string.Exception),
                            app.getString(R.string.ErrorSigning), R.raw.error_check, () -> {
                                if (isDeepLink) {
                                    app.finishAffinity();
                                }
                            }));
        }
    }

    private static String signMessage(String message, KeyModel key) {
        byte[] signingData = formatMessageForBitcoinSigning(message);

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        byte[] sha256First = digest.digest(signingData);
        byte[] sha256Second = digest.digest(sha256First);
        byte[] signature = key.compactSign(sha256Second);

        return Base64.encodeToString(signature, Base64.NO_WRAP);
    }

    private static byte[] formatMessageForBitcoinSigning(String message) {
        byte[] headerBytes = null;
        byte[] messageBytes = null;

        headerBytes = BITCOIN_SIGNED_MESSAGE_HEADER.getBytes(StandardCharsets.UTF_8);
        messageBytes = message.getBytes(StandardCharsets.UTF_8);
        assert (headerBytes != null);
        assert (messageBytes != null);

        int cap = 1 + headerBytes.length + varIntSize(messageBytes.length) + messageBytes.length;

        ByteBuffer dataBuffer = ByteBuffer.allocate(cap).order(ByteOrder.LITTLE_ENDIAN);
        dataBuffer.put((byte) headerBytes.length);          //put header count
        dataBuffer.put(headerBytes);                        //put the header
        putVarInt(message.length(), dataBuffer);            //put message count
        dataBuffer.put(messageBytes);                       //put the message
        return dataBuffer.array();
    }

    /**
     * Returns the encoding size in bytes of its input value.
     *
     * @param i the integer to be measured
     * @return the encoding size in bytes of its input value
     */
    private static int varIntSize(int i) {
        int result = 0;
        do {
            result++;
            i >>>= 7;
        } while (i != 0);
        return result;
    }

    /**
     * Encodes an integer in a variable-length encoding, 7 bits per byte, to a
     * ByteBuffer sink.
     *
     * @param v    the value to encode
     * @param sink the ByteBuffer to add the encoded value
     */
    private static void putVarInt(int v, ByteBuffer sink) {
        while (true) {
            int bits = v & 0x7f;
            v >>>= 7;
            if (v == 0) {
                sink.put((byte) bits);
                return;
            }
            sink.put((byte) (bits | 0x80));
        }
    }
}
