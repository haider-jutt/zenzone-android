package com.zenimmersive.android.helper;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zenimmersive.android.R;

public class TextInputLayout extends com.google.android.material.textfield.TextInputLayout {
    public TextInputLayout(@NonNull Context context) {
        super(context);
    }

    public TextInputLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setupFont(context, attrs);
    }

    private void setupFont(Context context, AttributeSet attrs) {
        if (attrs != null) {
            try {
                TypedArray FontViewStyle = context.obtainStyledAttributes(attrs, R.styleable.FontView);
                String fontName = FontViewStyle.getString(R.styleable.FontView_customFont);
                if (fontName != null) {
                    Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/" + fontName);
                    setTypeface(typeface);
                }
            } catch (Exception e) {
            }
        }
    }

    public TextInputLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupFont(context, attrs);
    }
}
