package com.zenimmersive.android.helper;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;

import com.zenimmersive.android.R;

public class RoundedFrameView extends FrameLayout {

    public enum Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_RIGHT,
        BOTTOM_LEFT,
    }

    private final float[] radii;

    public RoundedFrameView(@NonNull Context context) {
        this(context, null);
    }

    public RoundedFrameView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundedFrameView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RoundedImageView, defStyleAttr, 0);
        final float radius = typedArray.getDimension(R.styleable.RoundedImageView_radius, 0);
        final float topLeftRadius = typedArray.getDimension(R.styleable.RoundedImageView_topLeftRadius, radius);
        final float topRightRadius = typedArray.getDimension(R.styleable.RoundedImageView_topRightRadius, radius);
        final float bottomLeftRadius = typedArray.getDimension(R.styleable.RoundedImageView_bottomLeftRadius, radius);
        final float bottomRightRadius = typedArray.getDimension(R.styleable.RoundedImageView_bottomRightRadius, radius);
        radii = new float[]{topLeftRadius, topLeftRadius, topRightRadius, topRightRadius, bottomRightRadius, bottomRightRadius, bottomLeftRadius, bottomLeftRadius};

        typedArray.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Path path = new Path();
        RectF mainRectF = new RectF(0, 0, getWidth(), getHeight());
        path.addRoundRect(mainRectF, radii, Path.Direction.CW);
        canvas.clipPath(path);
        super.onDraw(canvas);
    }

    public void setRadius(@Px int radius) {
        for (int i = 0; i < radii.length; i++) {
            radii[i] = radius;
        }
        invalidate();
    }

    public void setRadius(Corner corner, @Px int radius) {
        switch (corner) {
            case TOP_LEFT:
                radii[0] = radius;
                radii[1] = radius;
                break;
            case TOP_RIGHT:
                radii[2] = radius;
                radii[3] = radius;
                break;
            case BOTTOM_RIGHT:
                radii[4] = radius;
                radii[5] = radius;
                break;
            case BOTTOM_LEFT:
                radii[6] = radius;
                radii[7] = radius;
                break;
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        Path path = new Path();
        RectF mainRectF = new RectF(0, 0, getWidth(), getHeight());
        path.addRoundRect(mainRectF, radii, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(path);
        super.dispatchDraw(canvas);
        canvas.restore();
    }
}