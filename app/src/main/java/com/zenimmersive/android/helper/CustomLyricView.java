package com.zenimmersive.android.helper;

import static com.zenimmersive.android.helper.KeyStorage.APP_SELECTED_LANGUAGE;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.zenimmersive.android.R;
import com.zenimmersive.android.apiresponsemodel.AlbumMusic;

import java.util.ArrayList;
import java.util.List;

public class CustomLyricView extends View {

    private static final String TAG = "CustomLyricView";
    private static final int MODE_NORMAL = 0;
    private static final int MODE_SEEK = 1;
    private static final int MODE_SCALE = 2;

    private List<LyricLine> lyricLines;
    private int currentHighlightIndex = 0;
    private int normalColor = Color.WHITE;
    private int highlightColor = Color.YELLOW;
    private int seekLineColor = Color.CYAN;
    private int seekLineTextColor = Color.CYAN;
    private int seekLineTextSize = 15;
    private int minSeekLineTextSize = 13;
    private int maxSeekLineTextSize = 18;
    private int lyricTextSize = 30;
    private int minLyricTextSize = 15;
    private int maxLyricTextSize = 35;
    private int lineSpacing = 10;
    private int seekLinePaddingX = 0;
    private int displayMode = MODE_NORMAL;
    private boolean isMoving = false;

    private String loadingText = "";

    private TextPaint normalPaint;
    private TextPaint highlightPaint;
    private TextPaint seekLinePaint;
    private TextPaint seekLineTextPaint;
    private TextPaint loadingTextPaint;

    private float lastTouchY;
    private PointF pointerOneLastPos = new PointF();
    private PointF pointerTwoLastPos = new PointF();
    private boolean isFirstMove = false;

    public CustomLyricView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        loadingText = getContext().getString(R.string.loading_lyrics);
        normalPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        normalPaint.setColor(normalColor);
        normalPaint.setTextSize(lyricTextSize);
        normalPaint.setAlpha(Math.round(0.5f * 255));
        normalPaint.setTextAlign(Paint.Align.CENTER);

        highlightPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(highlightColor);
        highlightPaint.setTextSize(lyricTextSize * 2f);
        highlightPaint.setTextAlign(Paint.Align.CENTER);

        seekLinePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        seekLinePaint.setAlpha(Math.round(0.5f * 255));
        seekLinePaint.setColor(seekLineColor);

        seekLineTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        seekLineTextPaint.setColor(seekLineTextColor);
        seekLineTextPaint.setAlpha(Math.round(0.5f * 255));
        seekLineTextPaint.setTextSize(seekLineTextSize);
        seekLineTextPaint.setTextAlign(Paint.Align.LEFT);

        loadingTextPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        loadingTextPaint.setColor(highlightColor);
        loadingTextPaint.setTextSize(lyricTextSize);
        loadingTextPaint.setTextAlign(TextPaint.Align.CENTER);

//        lyricLines = new ArrayList<>();
//        lyricLines.add(new LyricLine(1000, "Line 1"));
//        lyricLines.add(new LyricLine(2000, "Line 2"));
//        lyricLines.add(new LyricLine(3000, "Line 3"));
//        lyricLines.add(new LyricLine(4000, "Line 4"));
//        lyricLines.add(new LyricLine(5000, "Line 5"));
//        lyricLines.add(new LyricLine(6000, "Line 6 SOMETHIGN TO Draw and display so i wrote this taxt to mesuy jksajflksajioptuoieu j"));
//        lyricLines.add(new LyricLine(7000, "Line 7"));
//        lyricLines.add(new LyricLine(8000, "Line 8"));
//        lyricLines.add(new LyricLine(9000, "Line 9"));
//        lyricLines.add(new LyricLine(10000, "Line 10"));
//        lyricLines.add(new LyricLine(11000, "Line 11"));
//        lyricLines.add(new LyricLine(12000, "Line 12"));
//        lyricLines.add(new LyricLine(13000, "Line 13"));
//        lyricLines.add(new LyricLine(14000, "Line 14"));
//        lyricLines.add(new LyricLine(15000, "Line 15"));
//        lyricLines.add(new LyricLine(16000, "Line 16"));
//
//        currentHighlightIndex = 5;
    }

    public void setLyricLines(List<LyricLine> lines) {
        this.lyricLines = lines;
        invalidate();
    }

    public void updateHighlightIndex(int index) {
        this.currentHighlightIndex = index;
        invalidate();
    }

    public void setLoadingText(String text) {
        this.loadingText = text;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int viewHeight = getHeight();
        int viewWidth = getWidth();

        if (lyricLines == null || lyricLines.isEmpty()) {
            if (loadingText != null) {
                drawTextWithWrap(canvas, loadingText, viewWidth / 2f, viewHeight / 2f - lyricTextSize, loadingTextPaint, viewWidth);
            }
            return;
        }

        float centerY = viewHeight / 2f;
        float centerX = viewWidth / 2f;

        StaticLayout centerTextLayout = drawTextWithWrap(canvas, ExtensionsKt.stripHtmlTags(lyricLines.get(currentHighlightIndex).getText()), centerX, centerY, highlightPaint, viewWidth);

        if (displayMode == MODE_SEEK) {
            canvas.drawLine(seekLinePaddingX, centerY, viewWidth - seekLinePaddingX, centerY, seekLinePaint);
            drawTextWithWrap(canvas, ExtensionsKt.stripHtmlTags(lyricLines.get(currentHighlightIndex).getText()), seekLinePaddingX, centerY, seekLineTextPaint, viewWidth - 2 * seekLinePaddingX);
        }

        float yOffset = centerY - centerTextLayout.getHeight() / 2f - (lineSpacing * 2f);
        for (int i = currentHighlightIndex - 1; i >= 0 && yOffset > -lyricTextSize; i--) {
            StaticLayout drawingView = drawTextWithWrap(canvas, ExtensionsKt.stripHtmlTags(lyricLines.get(i).getText()), centerX, yOffset, normalPaint, viewWidth);
            yOffset -= (lineSpacing + drawingView.getHeight());
        }

        yOffset = centerY + (lineSpacing * 2f) + centerTextLayout.getHeight() / 2f;
        for (int i = currentHighlightIndex + 1; i < lyricLines.size() && yOffset < viewHeight; i++) {
            StaticLayout drawingView = drawTextWithWrap(canvas, ExtensionsKt.stripHtmlTags(lyricLines.get(i).getText()), centerX, yOffset, normalPaint, viewWidth);
            yOffset += (lineSpacing + drawingView.getHeight());
        }
    }

    private StaticLayout drawTextWithWrap(Canvas canvas, String text, float x, float y, TextPaint paint, int width) {
        StaticLayout staticLayout = new StaticLayout(text, paint, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        canvas.save();
        canvas.translate(staticLayout.getWidth() / 2f, y - staticLayout.getHeight() / 2f);
        ///canvas.drawLine(0,0,width/2f,0,paint);
        staticLayout.draw(canvas);
        canvas.restore();
        return staticLayout;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (lyricLines == null || lyricLines.isEmpty()) {
            return super.onTouchEvent(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isMoving = true;
                lastTouchY = event.getY();
                isFirstMove = true;
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 2) {
                    handleScaling(event);
                    return true;
                }
                handleSeeking(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                isMoving = false;
                if (displayMode == MODE_SEEK) {
                    updateHighlightIndex(currentHighlightIndex);
                }
                displayMode = MODE_NORMAL;
                invalidate();
                break;
        }
        return true;
    }

    private void handleScaling(MotionEvent event) {
        if (displayMode == MODE_SEEK) {
            displayMode = MODE_SCALE;
            return;
        }

        if (isFirstMove) {
            displayMode = MODE_SCALE;
            invalidate();
            isFirstMove = false;
            setPointerPositions(event);
        }

        int scaleOffset = calculateScaleOffset(event);
        if (scaleOffset != 0) {
            adjustFontSize(scaleOffset);
            invalidate();
        }
        setPointerPositions(event);
    }

    private void handleSeeking(MotionEvent event) {
        float y = event.getY();
        float offsetY = y - lastTouchY;

        if (Math.abs(offsetY) < 10) {
            return;
        }

        displayMode = MODE_SEEK;
        int lineOffset = Math.abs((int) offsetY / lyricTextSize);

        if (offsetY < 0) {
            currentHighlightIndex += lineOffset;
        } else {
            currentHighlightIndex -= lineOffset;
        }

        currentHighlightIndex = Math.max(0, currentHighlightIndex);
        currentHighlightIndex = Math.min(currentHighlightIndex, lyricLines.size() - 1);

        if (lineOffset > 0) {
            lastTouchY = y;
            invalidate();
        }
    }

    private void setPointerPositions(MotionEvent event) {
        pointerOneLastPos.set(event.getX(0), event.getY(0));
        pointerTwoLastPos.set(event.getX(1), event.getY(1));
    }

    private void adjustFontSize(int scaleOffset) {
        lyricTextSize += scaleOffset;
        seekLineTextSize += scaleOffset;

        lyricTextSize = Math.max(lyricTextSize, minLyricTextSize);
        lyricTextSize = Math.min(lyricTextSize, maxLyricTextSize);
        seekLineTextSize = Math.max(seekLineTextSize, minSeekLineTextSize);
        seekLineTextSize = Math.min(seekLineTextSize, maxSeekLineTextSize);

        normalPaint.setTextSize(lyricTextSize);
        highlightPaint.setTextSize(lyricTextSize * 2f);
        seekLineTextPaint.setTextSize(seekLineTextSize);
        loadingTextPaint.setTextSize(lyricTextSize);
    }

    private int calculateScaleOffset(MotionEvent event) {
        float oldDistance = distanceBetweenPoints(pointerOneLastPos.x, pointerOneLastPos.y, pointerTwoLastPos.x, pointerTwoLastPos.y);
        float newDistance = distanceBetweenPoints(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
        boolean zoomIn = newDistance > oldDistance;

        float distanceChange = Math.abs(newDistance - oldDistance);
        return zoomIn ? (int) (distanceChange / 10) : -(int) (distanceChange / 10);
    }

    private float distanceBetweenPoints(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }


    public void setPlayerPosition(long currentPosition) {
        LogSystem.e("Lyric setPlayerPosition " + currentPosition + " LyricLinesInvalid : " + (lyricLines == null || lyricLines.isEmpty()));
        if (lyricLines == null || lyricLines.isEmpty()) {
            return;
        }

        if (isMoving) {
            return;
        }

        post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < lyricLines.size(); i++) {
                    LyricLine currentLine = lyricLines.get(i);
                    LyricLine nextLine = (i + 1 < lyricLines.size()) ? lyricLines.get(i + 1) : null;

                    if (nextLine != null) {
                        // What if lyric text start from 10 seconds but player at the
                        if (currentPosition <= currentLine.getTimestamp() && nextLine.getTimestamp() > currentPosition) {
                            if(currentHighlightIndex!=i) {
                                currentHighlightIndex = i;
                                invalidate();
                            }
                            return;
                        }
                    }
                    if (nextLine == null || (currentPosition >= currentLine.getTimestamp() && currentPosition < nextLine.getTimestamp())) {
                        if(currentHighlightIndex!=i) {
                            currentHighlightIndex = i;
                            invalidate();
                        }
                        return;
                    }
                }
            }
        });
    }

    public void loadLyric(AlbumMusic albumMusic) {
        try {
            if (lyricLines != null) {
                lyricLines = new ArrayList<>();
                invalidate();
            }
        } catch (Exception e) {
        }
        String lan = KeyStorage.getInstance(getContext()).getString(APP_SELECTED_LANGUAGE);
        String url = "";
        if (!TextUtils.isEmpty(lan) && albumMusic != null) {
            if (lan.equals("en")) {
                url = albumMusic.getLyrics();
            } else {
                url = albumMusic.getLyricsFrench();
            }
        } else if (albumMusic != null) {
            url = albumMusic.getLyrics();
        }
        if(!TextUtils.isEmpty(url))
        {
            LyricLoader.loadLyricsFromUrl(url, this, getContext(), albumMusic);
        }
        else
        {
            lyricLines = new ArrayList<>();
            invalidate();
        }
    }

    public static class LyricLine {
        private long timestamp;
        private String text;

        public LyricLine(long timestamp, String text) {
            this.timestamp = timestamp;
            this.text = text;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getText() {
            return text;
        }
    }
}
