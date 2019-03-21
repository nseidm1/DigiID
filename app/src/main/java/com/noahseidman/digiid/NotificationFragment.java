package com.noahseidman.digiid;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.noahseidman.digiid.databinding.FragmentNotificationBinding;
import com.noahseidman.digiid.listeners.SignalCompleteCallback;
import com.noahseidman.digiid.listeners.OnBackPressListener;
import com.noahseidman.digiid.models.FragmentSignalViewModel;
import com.noahseidman.digiid.utils.AnimatorHelper;
import org.jetbrains.annotations.NotNull;

public class NotificationFragment extends Fragment implements OnBackPressListener {
    private static final String TAG = NotificationFragment.class.getName();

    private static final String TITLE = "title";
    private static final String ICON_DESCRIPTION = "iconDescription";
    private static final String RES_ID = "resId";
    private SignalCompleteCallback completion;
    private FragmentNotificationBinding binding;

    public static void show(AppCompatActivity activity, String title,
                            String iconDescription,
                            int drawableId, SignalCompleteCallback completion) {
        NotificationFragment fragmentSignal = new NotificationFragment();
        Bundle bundle = new Bundle();
        bundle.putString(NotificationFragment.TITLE, title);
        bundle.putString(NotificationFragment.ICON_DESCRIPTION, iconDescription);
        fragmentSignal.setCompletion(completion);
        bundle.putInt(NotificationFragment.RES_ID, drawableId);
        fragmentSignal.setArguments(bundle);
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_bottom,
                R.animator.from_bottom, R.animator.to_bottom);
        transaction.add(android.R.id.content, fragmentSignal, NotificationFragment.class.getName());
        transaction.addToBackStack(NotificationFragment.class.getName());
        transaction.commitAllowingStateLoss();
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        binding = FragmentNotificationBinding.inflate(inflater);
        FragmentSignalViewModel viewModel = new FragmentSignalViewModel();
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            String title = bundle.getString(TITLE, "");
            String description = bundle.getString(ICON_DESCRIPTION, "");
            int resId = bundle.getInt(RES_ID, 0);
            viewModel.setTitle(title);
            viewModel.setDescription(description);
            viewModel.setIcon(resId);
        } else {
            Log.e(TAG, "onCreateView: bundle is null!");
        }
        binding.setData(viewModel);
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

    private void setCompletion(SignalCompleteCallback completion) {
        this.completion = completion;
    }

    @Override
    public void onResume() {
        super.onResume();
        new Handler().postDelayed(() -> {
            try {
                if (getActivity() != null)
                    getActivity().getFragmentManager().popBackStack();
            } catch (Exception ignored) {

            }
            new Handler().postDelayed(this::fadeOutRemove, 300);
        }, 2000);
    }

    private void fadeOutRemove() {
        ObjectAnimator colorFade = AnimatorHelper.animateBackgroundDim(binding.background, true,
                () -> {
                    new Handler().postDelayed(() -> {
                        if (completion != null) {
                            completion.onComplete();
                            completion = null;
                        }
                    }, 300);
                    remove();
                });
        colorFade.start();
    }

    private void remove() {
        if (getFragmentManager() == null) {
            return;
        }
        try {
            getFragmentManager().popBackStack();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        fadeOutRemove();
    }

}