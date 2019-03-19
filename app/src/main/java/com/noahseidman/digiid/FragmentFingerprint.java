package com.noahseidman.digiid;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.noahseidman.digiid.databinding.FingerprintDialogContainerBinding;
import com.noahseidman.digiid.listeners.BRAuthCompletionCallback;
import com.noahseidman.digiid.listeners.FingerprintFragmentCallback;
import com.noahseidman.digiid.listeners.OnBackPressListener;
import com.noahseidman.digiid.models.FingerprintFragmentViewModel;
import com.noahseidman.digiid.utils.AnimatorHelper;
import com.noahseidman.digiid.utils.FingerprintUiHelper;
import org.jetbrains.annotations.NotNull;

@RequiresApi(api = Build.VERSION_CODES.M)
public class FragmentFingerprint extends Fragment implements FingerprintUiHelper.Callback, OnBackPressListener {
    private static final String AUTH_TYPE = "FragmentFingerprint:AuthType";
    private FingerprintManager.CryptoObject mCryptoObject;
    private FingerprintUiHelper mFingerprintUiHelper;
    private BRAuthCompletionCallback completion;
    private boolean authComplete = false;

    private FingerprintDialogContainerBinding binding;

    private final FingerprintFragmentCallback callback = () -> fadeOutRemove(false);

    public static void show(AppCompatActivity activity, String title, String message, BRAuthCompletionCallback.AuthType type) {
        FragmentFingerprint fingerprintFragment = new FragmentFingerprint();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", message);
        args.putSerializable(AUTH_TYPE, type);
        fingerprintFragment.setArguments(args);
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_bottom,
                R.animator.from_bottom, R.animator.to_bottom);
        transaction.add(android.R.id.content, fingerprintFragment,
                FragmentFingerprint.class.getName());
        transaction.addToBackStack(FragmentFingerprint.class.getName());
        transaction.commitAllowingStateLoss();
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        if (context instanceof BRAuthCompletionCallback) {
            completion = (BRAuthCompletionCallback) context;
        } else {
            throw new RuntimeException("Failed to have the containing activity implement BRAuthCompletion");
        }
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FingerprintDialogContainerBinding.inflate(inflater);
        binding.fingerprintLayout.getLayoutTransition().enableTransitionType(
                LayoutTransition.CHANGING);
        FingerprintFragmentViewModel viewModel = new FingerprintFragmentViewModel();
        binding.setData(viewModel);
        binding.setCallback(callback);
        binding.executePendingBindings();
        viewModel.setTitle(getArguments().getString("title"));
        viewModel.setMessage(getArguments().getString("message"));
        viewModel.setCancelButtonLabel(getString(R.string.Cancel));
        FingerprintUiHelper.FingerprintUiHelperBuilder mFingerprintUiHelperBuilder = new FingerprintUiHelper.FingerprintUiHelperBuilder((FingerprintManager) getContext().getSystemService(Activity.FINGERPRINT_SERVICE));
        mFingerprintUiHelper = mFingerprintUiHelperBuilder.build(binding.fingerprintIcon, binding.fingerprintStatus, this, getContext());
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ObjectAnimator colorFade = AnimatorHelper.animateBackgroundDim(binding.background, false, null);
        colorFade.setStartDelay(350);
        colorFade.setDuration(500);
        colorFade.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        mFingerprintUiHelper.startListening(mCryptoObject);
    }

    @Override
    public void onPause() {
        super.onPause();
        mFingerprintUiHelper.stopListening();
    }

    @Override
    public void onAuthenticated() {
        fadeOutRemove(true);
    }

    private BRAuthCompletionCallback.AuthType getType() {
        return (BRAuthCompletionCallback.AuthType) getArguments().getSerializable(AUTH_TYPE);
    }

    private void fadeOutRemove(boolean authenticated) {
        if (authComplete) {
            return;
        }
        ObjectAnimator colorFade = AnimatorHelper.animateBackgroundDim(binding.background, true,
                () -> {
                    authComplete = true;
                    remove();
                    Handler handler = new Handler(Looper.getMainLooper());
                    if (authenticated) {
                        handler.postDelayed(() -> {
                            if (completion != null) completion.onComplete(getType());
                        }, 350);
                    }
                });
        colorFade.start();
    }

    private void remove() {
        if (getFragmentManager() == null) {
            return;
        }
        try { getFragmentManager().popBackStack(); }
        catch(IllegalStateException e) { e.printStackTrace(); }
    }

    @Override
    public void onBackPressed() {
        fadeOutRemove(false);
    }

    @Override
    public void onError() {

    }
}