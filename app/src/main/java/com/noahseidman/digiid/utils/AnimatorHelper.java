package com.noahseidman.digiid.utils;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.view.ViewGroup;

public class AnimatorHelper {

    private final static int SLIDE_ANIMATION_DURATION = 300;

    public static ObjectAnimator animateBackgroundDim(final ViewGroup backgroundLayout,
                                                      boolean reverse, OnAnimationEndListener onAnimationEndListener) {
        ObjectAnimator colorFade =
                ObjectAnimator.ofObject(backgroundLayout, "backgroundColor", new ArgbEvaluator(),
                        reverse ? Color.argb(200, 0, 0, 0) : Color.argb(0, 0, 0, 0),
                        reverse ? Color.argb(0, 0, 0, 0) : Color.argb(200, 0, 0, 0));
        colorFade.setDuration(SLIDE_ANIMATION_DURATION);
        colorFade.addListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {

            }

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (onAnimationEndListener != null) {
                    onAnimationEndListener.onAnimationEnd();
                }
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {

            }

            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {

            }
        });
        return colorFade;
    }

    public interface OnAnimationEndListener {
        void onAnimationEnd();
    }
}
