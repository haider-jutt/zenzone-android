package com.zenimmersive.android.helper;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zenimmersive.android.R;

public class TextInputEditText extends com.google.android.material.textfield.TextInputEditText {
    public TextInputEditText(@NonNull Context context) {
        super(context);
    }

    public TextInputEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
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

    public TextInputEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupFont(context, attrs);
    }
}
