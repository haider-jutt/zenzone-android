package com.zenimmersive.android.helper;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class StatusBarSizeView extends View {

    // Status bar saved size
    private static int heightSize = 0;

    public StatusBarSizeView(Context context) {
        super(context);
        init();
    }

    public StatusBarSizeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StatusBarSizeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Do nothing if we already have the size
        if (heightSize != 0) {
            return;
        }

        heightSize = getStatusBarHeight(getContext());
    }

    public static int getStatusBarHeight(final Context context) {
        final Resources resources = context.getResources();
        final int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0)
            return resources.getDimensionPixelSize(resourceId);
        else
            return (int) Math.ceil(25 * resources.getDisplayMetrics().density);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int desiredHeight = heightSize; // Set your desired height in pixels

        int measuredHeight = MeasureSpec.makeMeasureSpec(desiredHeight, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, measuredHeight);
    }

    public void resetHeight() {
        heightSize = 0;
        ViewGroup.LayoutParams params = getLayoutParams();;
        params.height = heightSize;
        setLayoutParams(params);
    }
}
