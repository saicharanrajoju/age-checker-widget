package com.agechecker.widget;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * Draws the "Abstract Flow" scene: dark glass background, a cluster of silky ribbons sweeping
 * through the top-right corner and a smaller cluster in the bottom-right, leaving the left side
 * calm for content. The same painter is used for the animated panel (time advances every frame)
 * and for the static home-screen widget bitmap (time frozen).
 */
final class RibbonPainter {
    static final float STATIC_TIME = 5.3f;
    private static final int TOP_RIBBONS = 6, BOTTOM_RIBBONS = 3;

    private final Paint bgPaint = new Paint();
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow2Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrimPaint = new Paint();
    private final Path band = new Path(), edge = new Path();

    private int[] colors = Themes.PRESETS[0];
    private int w, h;
    private float density = 1f;
    private LinearGradient[] shaders;
    private boolean dirty = true;

    RibbonPainter() {
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    void setColors(int[] c) {
        colors = c;
        dirty = true;
    }

    void setSize(int w, int h, float density) {
        this.w = w;
        this.h = h;
        this.density = density;
        dirty = true;
    }

    private void rebuild() {
        bgPaint.setShader(new LinearGradient(0, 0, w * 0.3f, h,
                new int[]{0xFF111733, 0xFF0A0F24, 0xFF060914}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        float r = Math.max(w, h) * 0.7f;
        glowPaint.setShader(new RadialGradient(w * 0.85f, h * 0.15f, r,
                Themes.withAlpha(Themes.sample(colors, 0.3f), 0x66), 0, Shader.TileMode.CLAMP));
        glow2Paint.setShader(new RadialGradient(w * 0.95f, h * 1.0f, r * 0.6f,
                Themes.withAlpha(Themes.sample(colors, 0.8f), 0x44), 0, Shader.TileMode.CLAMP));
        scrimPaint.setShader(new LinearGradient(0, 0, w * 0.7f, h * 0.25f,
                new int[]{0x99060914, 0x40060914, 0x00060914}, new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));

        int n = TOP_RIBBONS + BOTTOM_RIBBONS;
        shaders = new LinearGradient[n];
        for (int i = 0; i < n; i++) {
            float k = i < TOP_RIBBONS ? i / (float) (TOP_RIBBONS - 1) : (i - TOP_RIBBONS) / (float) (BOTTOM_RIBBONS - 1);
            int c1 = Themes.sample(colors, k);
            int c2 = Themes.sample(colors, Math.min(1f, k + 0.45f));
            int peak = 110 + (int) (110 * (1 - k));
            shaders[i] = new LinearGradient(w * 0.45f, 0, w, h * 0.7f,
                    new int[]{Themes.withAlpha(c1, 0), Themes.withAlpha(c1, peak),
                            Themes.withAlpha(c2, Math.min(255, peak + 35)), Themes.withAlpha(c2, 60)},
                    new float[]{0f, 0.3f, 0.7f, 1f}, Shader.TileMode.CLAMP);
        }
        dirty = false;
    }

    void draw(Canvas c, float t) {
        if (w <= 0 || h <= 0) return;
        if (dirty) rebuild();
        c.drawRect(0, 0, w, h, bgPaint);

        float breathe = 0.72f + 0.28f * (float) Math.sin(t * 0.8f);
        glowPaint.setAlpha((int) (255 * breathe));
        c.drawRect(0, 0, w, h, glowPaint);
        glow2Paint.setAlpha((int) (255 * (0.72f + 0.28f * (float) Math.cos(t * 0.6f))));
        c.drawRect(0, 0, w, h, glow2Paint);

        for (int i = 0; i < TOP_RIBBONS; i++) topRibbon(c, i, t);
        for (int i = 0; i < BOTTOM_RIBBONS; i++) bottomRibbon(c, i, t);
        c.drawRect(0, 0, w, h, scrimPaint);
    }

    private void topRibbon(Canvas c, int i, float t) {
        float k = i / (float) (TOP_RIBBONS - 1);
        float sp = 0.20f + 0.05f * i;
        float a = (float) Math.sin(t * sp + i * 1.3f);
        float b = (float) Math.cos(t * sp * 0.75f + i * 0.7f);
        float m = (float) Math.sin(t * sp * 0.5f + i * 2.1f);
        float x0 = w * (0.46f + 0.06f * k) + a * w * 0.02f, y0 = -h * 0.08f;
        float x1 = w * (0.60f + 0.03f * k), y1 = h * (0.10f + 0.10f * k) + b * h * 0.06f;
        float x2 = w * (0.78f + 0.03f * m), y2 = h * (0.22f + 0.12f * k) + a * h * 0.07f;
        float x3 = w * 1.08f, y3 = h * (0.42f + 0.10f * k) + b * h * 0.05f;
        float th = h * (0.05f + 0.03f * (1 - k));
        ribbon(c, i, x0, y0, x1, y1, x2, y2, x3, y3,
                th * (0.4f + 0.3f * m), th * (1.5f + 0.6f * a), th * (1.2f + 0.5f * b), th * 0.6f);
    }

    private void bottomRibbon(Canvas c, int i, float t) {
        float k = i / (float) (BOTTOM_RIBBONS - 1);
        float sp = 0.24f + 0.06f * i;
        float a = (float) Math.sin(t * sp + i * 1.9f + 1f);
        float b = (float) Math.cos(t * sp * 0.8f + i * 1.1f);
        float x0 = w * (0.70f + 0.05f * k), y0 = h * 1.08f;
        float x1 = w * (0.82f + 0.02f * a), y1 = h * (0.96f - 0.03f * k) + b * h * 0.03f;
        float x2 = w * (0.93f), y2 = h * (0.82f - 0.04f * k) + a * h * 0.04f;
        float x3 = w * 1.08f, y3 = h * (0.66f + 0.06f * k) + b * h * 0.03f;
        float th = h * 0.045f;
        ribbon(c, TOP_RIBBONS + i, x0, y0, x1, y1, x2, y2, x3, y3,
                th * 0.5f, th * (1.3f + 0.4f * a), th * (1.1f + 0.4f * b), th * 0.7f);
    }

    private void ribbon(Canvas c, int shader, float x0, float y0, float x1, float y1, float x2, float y2,
                        float x3, float y3, float t0, float t1, float t2, float t3) {
        // The band's lower edge is the same curve shifted by a varying thickness, which reads as a twist.
        band.reset();
        band.moveTo(x0, y0);
        band.cubicTo(x1, y1, x2, y2, x3, y3);
        band.lineTo(x3 - t3 * 0.3f, y3 + t3);
        band.cubicTo(x2 - t2 * 0.3f, y2 + t2, x1 - t1 * 0.3f, y1 + t1, x0 - t0 * 0.3f, y0 + t0);
        band.close();
        fillPaint.setShader(shaders[shader]);
        c.drawPath(band, fillPaint);

        edge.reset();
        edge.moveTo(x0, y0);
        edge.cubicTo(x1, y1, x2, y2, x3, y3);
        edgePaint.setShader(shaders[shader]);
        edgePaint.setStrokeWidth(density * 1.2f);
        c.drawPath(edge, edgePaint);
    }

    /** Static widget artwork: ribbons, anti-aliased rounded corners and a soft glowing border. */
    static Bitmap renderWidget(int[] colors, int w, int h, float density) {
        Bitmap scene = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        RibbonPainter p = new RibbonPainter();
        p.setColors(colors);
        p.setSize(w, h, density);
        p.draw(new Canvas(scene), STATIC_TIME);

        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        float radius = 28 * density;
        RectF rect = new RectF(0, 0, w, h);
        Paint img = new Paint(Paint.ANTI_ALIAS_FLAG);
        img.setShader(new BitmapShader(scene, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        c.drawRoundRect(rect, radius, radius, img);
        scene.recycle();

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        float sw = 1.2f * density;
        RectF in = new RectF(sw / 2, sw / 2, w - sw / 2, h - sw / 2);
        int glowA = Themes.withAlpha(Themes.sample(colors, 0.2f), 0x30);
        int glowB = Themes.withAlpha(Themes.sample(colors, 0.9f), 0x30);
        border.setShader(new LinearGradient(0, 0, w, h, 0x22FFFFFF, glowB, Shader.TileMode.CLAMP));
        border.setStrokeWidth(5 * density);
        c.save();
        Path clip = new Path();
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW);
        c.clipPath(clip);
        c.drawRoundRect(rect, radius, radius, border);
        c.restore();
        border.setShader(new LinearGradient(0, 0, w, h,
                new int[]{0x40FFFFFF, Themes.withAlpha(glowA, 0x80), Themes.withAlpha(glowB, 0xB0)},
                null, Shader.TileMode.CLAMP));
        border.setStrokeWidth(sw);
        c.drawRoundRect(in, radius - sw / 2, radius - sw / 2, border);
        return out;
    }
}
