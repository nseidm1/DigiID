package com.noahseidman.digiid.layouts;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class AnimatedLinearLayout extends LinearLayout {
    public static final String TAG = AnimatedLinearLayout.class.getName();

    private float mXfract = 0f;
    private float mYfract = 0f;

    public AnimatedLinearLayout(Context context) {
        super(context);
    }

    public AnimatedLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimatedLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setYFraction(final float fraction) {
        mYfract=fraction;
        float translationY = getHeight() * fraction;
        setTranslationY(translationY);
    }

    public float getYFraction() {
        return mYfract;
    }

    public void setXFraction(final float fraction) {
        mXfract=fraction;
        float translationX = getWidth() * fraction;
        setTranslationX(translationX);
    }

    public float getXFraction() {
        return mXfract;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        // Correct any translations set before the measure was set
        setTranslationX(mXfract*width);
        setTranslationY(mYfract*height);
    }
}
