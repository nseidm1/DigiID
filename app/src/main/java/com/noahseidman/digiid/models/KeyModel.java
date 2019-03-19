package com.noahseidman.digiid.models;

import java.util.Arrays;

public class KeyModel {
    public static final String TAG = KeyModel.class.getName();

    public KeyModel(byte[] key) throws IllegalArgumentException {
        if (!setPrivKey(key)) {
            throw new IllegalArgumentException("Failed to setup the key: " + Arrays.toString(key));
        }
    }

    private native boolean setPrivKey(byte[] privKey);

    public native byte[] compactSign(byte[] data);

    public native String address();

}
