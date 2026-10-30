package com.agechecker.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/**
 * Continuously animated Abstract Flow background. Frames are driven by
 * postInvalidateOnAnimation (vsync, ~60 FPS) only while the view is attached, visible
 * and its window is visible; it stops automatically when the panel closes or goes to background.
 */
public class RibbonView extends View {
    private RibbonPainter painter;
    private long start;
    private boolean running;

    public RibbonView(Context c) { this(c, null); }

    public RibbonView(Context c, AttributeSet a) {
        super(c, a);
        start = SystemClock.uptimeMillis();
        painter = new RibbonPainter();
        painter.setColors(Themes.colors(c));
    }

    public void setColors(int[] colors) {
        painter.setColors(colors);
        invalidate();
    }

    public boolean isAnimating() { return running; }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        painter.setSize(w, h, getResources().getDisplayMetrics().density);
    }

    @Override protected void onDraw(Canvas c) {
        painter.draw(c, (SystemClock.uptimeMillis() - start) / 1000f);
        if (running) postInvalidateOnAnimation();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateRunning();
    }

    @Override protected void onDetachedFromWindow() {
        running = false;
        super.onDetachedFromWindow();
    }

    @Override protected void onWindowVisibilityChanged(int v) {
        super.onWindowVisibilityChanged(v);
        updateRunning();
    }

    @Override protected void onVisibilityChanged(View changed, int v) {
        super.onVisibilityChanged(changed, v);
        updateRunning();
    }

    private void updateRunning() {
        if (painter == null) return; // called from super constructor
        boolean should = isAttachedToWindow() && getWindowVisibility() == VISIBLE && isShown();
        if (should && !running) {
            running = true;
            postInvalidateOnAnimation();
        } else if (!should) {
            running = false;
        }
    }
}
