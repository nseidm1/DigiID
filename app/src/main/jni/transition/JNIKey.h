//
// Created by Mihail Gutan on 10/9/16.
//

#ifndef BREADWALLET_JNIKEY_H
#define BREADWALLET_JNIKEY_H

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jbyteArray JNICALL Java_com_noahseidman_digiid_Key_compactSign(JNIEnv *env, jobject thiz,
                                                                    jbyteArray data);

JNIEXPORT jboolean JNICALL Java_com_noahseidman_digiid_Key_setPrivKey(JNIEnv *env,
                                                                 jobject thiz,
                                                                 jbyteArray privKey);

JNIEXPORT void JNICALL Java_com_noahseidman_digiid_Key_setSecret(JNIEnv *env,
                                                            jobject thiz,
                                                            jbyteArray privKey);

JNIEXPORT jbyteArray JNICALL Java_com_noahseidman_digiid_Key_encryptNative(JNIEnv *env, jobject thiz,
                                                                      jbyteArray data,
                                                                      jbyteArray nonce);

JNIEXPORT jbyteArray JNICALL Java_com_noahseidman_digiid_Key_decryptNative(JNIEnv *env, jobject thiz,
                                                                      jbyteArray data,
                                                                      jbyteArray nonce);

JNIEXPORT jbyteArray JNICALL Java_com_noahseidman_digiid_Key_address(JNIEnv *env, jobject thiz);

#ifdef __cplusplus
}
#endif

#endif //BREADWALLET_JNIKEY_H
