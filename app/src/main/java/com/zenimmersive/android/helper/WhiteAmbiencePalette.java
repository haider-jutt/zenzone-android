package com.zenimmersive.android.helper;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

/**
 * ColorHsvPalette is a default drawable palette built by HSV (hue, saturation, value) color model
 * for alternating representations of the RGB color model.
 */
public class WhiteAmbiencePalette extends BitmapDrawable {

    private final Paint huePaint;
    private final Paint saturationPaint;

    public WhiteAmbiencePalette(Resources resources, Bitmap bitmap) {
        super(resources, bitmap);
        this.huePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.saturationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public WhiteAmbiencePalette(@NotNull Resources resources, int width, int height) {
        super(resources, Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888));
        this.huePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.saturationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int width = getBounds().width();
        int height = getBounds().height();
        float centerX = width * 0.5f;
        float centerY = height * 0.5f;
        float radius = Math.min(width, height) * 0.5f;

        Shader linearGradient = new LinearGradient(
                0, 0, width, height,
                new int[]{
                        Color.parseColor("#FDAA34"), // Warm white
                        Color.parseColor("#FAD97C"), // Neutral white
                        Color.parseColor("#FFFFFF"), // Cool white
                        Color.parseColor("#88E3F8")  // Cooler white
                },
                new float[]{0.0f, 0.25f, 0.50f, 0.85f},
                Shader.TileMode.CLAMP);
        Matrix matrix = new Matrix();
        linearGradient.getLocalMatrix(matrix);
        matrix.postRotate(45f);
        linearGradient.setLocalMatrix(matrix);

        huePaint.setShader(linearGradient);

        Shader saturationShader =
                new LinearGradient(
                        0, 0, width, height,
                        Color.WHITE, 0x00FFFFFF,
                        Shader.TileMode.CLAMP);
        saturationPaint.setShader(saturationShader);

        canvas.drawCircle(centerX, centerY, radius, huePaint);
        //canvas.drawCircle(centerX, centerY, radius, saturationPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        huePaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        huePaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    public Bitmap createBitmap(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);

        return bitmap;
    }
}
