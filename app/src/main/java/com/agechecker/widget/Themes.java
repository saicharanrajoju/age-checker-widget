package com.agechecker.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import java.time.LocalDate;
import java.util.Random;

/** Theme presets plus persisted theme / auto-change settings. */
final class Themes {
    static final String[] NAMES = {"Ocean Flow", "Sunset Flow", "Aurora Flow", "Neon Flow",
            "Crimson Flow", "Golden Flow", "Ice Flow", "Rainbow Flow"};

    /** Gradient stops per preset, matching the requested swatches. First color is the accent. */
    static final int[][] PRESETS = {
            {0xFF2459E0, 0xFF5B5BEA, 0xFF8B5CF6},             // Ocean
            {0xFFFF8A3D, 0xFFF6656B, 0xFFEC4899},             // Sunset
            {0xFF10B981, 0xFF1FC7A5, 0xFF2DD4BF},             // Aurora
            {0xFF9333EA, 0xFFBE38E0, 0xFFE83ADB},             // Neon
            {0xFFDC1F4B, 0xFF9B1838, 0xFF4A1020},             // Crimson
            {0xFFF5A623, 0xFFFBC04A, 0xFFFFD86B},             // Golden
            {0xFF22D3EE, 0xFF7DD6EE, 0xFFC7DCEC},             // Ice
            {0xFFEC5D9A, 0xFF9F7AEA, 0xFF4F8CF7},             // Rainbow
    };

    static final int CUSTOM = -1;
    static final int AUTO_OFF = 0, AUTO_EVERY_CHECK = 1, AUTO_DAILY = 2;

    private static final String PREFS = "age_checker";
    private static final String K_THEME = "theme", K_CA = "custom_a", K_CB = "custom_b",
            K_AUTO = "auto_mode", K_LAST_DAILY = "last_daily";
    private static final Random RNG = new Random();

    private Themes() {}

    static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static int selected(Context c) { return prefs(c).getInt(K_THEME, 0); }
    static int customA(Context c) { return prefs(c).getInt(K_CA, 0xFF2459E0); }
    static int customB(Context c) { return prefs(c).getInt(K_CB, 0xFFE83ADB); }

    static int[] colors(Context c) { return colorsFor(selected(c), customA(c), customB(c)); }

    static int[] colorsFor(int sel, int a, int b) {
        if (sel == CUSTOM) return new int[]{a, blend(a, b, 0.5f), b};
        if (sel < 0 || sel >= PRESETS.length) return PRESETS[0];
        return PRESETS[sel];
    }

    static String name(int sel) { return sel == CUSTOM ? "Custom Theme" : (sel >= 0 && sel < NAMES.length ? NAMES[sel] : NAMES[0]); }

    static void apply(Context c, int sel, int a, int b) {
        prefs(c).edit().putInt(K_THEME, sel).putInt(K_CA, a).putInt(K_CB, b).apply();
    }

    static void applyPreset(Context c, int sel) { prefs(c).edit().putInt(K_THEME, sel).apply(); }

    static int randomPreset(int exclude) {
        int r;
        do { r = RNG.nextInt(PRESETS.length); } while (r == exclude);
        return r;
    }

    static int autoMode(Context c) { return prefs(c).getInt(K_AUTO, AUTO_OFF); }

    static void setAutoMode(Context c, int mode) {
        SharedPreferences.Editor e = prefs(c).edit().putInt(K_AUTO, mode);
        if (mode == AUTO_DAILY) e.putString(K_LAST_DAILY, LocalDate.now().toString());
        e.apply();
    }

    /** Called after every successful age check. */
    static void onAgeChecked(Context c) {
        if (autoMode(c) == AUTO_EVERY_CHECK) applyPreset(c, randomPreset(selected(c)));
    }

    /** Switches to a random preset at most once per calendar day. Returns true if changed. */
    static boolean maybeDailyChange(Context c) {
        if (autoMode(c) != AUTO_DAILY) return false;
        String today = LocalDate.now().toString();
        SharedPreferences p = prefs(c);
        if (today.equals(p.getString(K_LAST_DAILY, null))) return false;
        p.edit().putInt(K_THEME, randomPreset(selected(c))).putString(K_LAST_DAILY, today).apply();
        return true;
    }

    // ---------------------------------------------------------------- color helpers

    static int accent(int[] colors) { return colors[0]; }

    /** Softer, lighter version of the accent used for the round submit button (like the reference). */
    static int buttonColor(int[] colors) { return blend(sample(colors, 0.6f), Color.WHITE, 0.22f); }

    /** Dark ink on light buttons, white on dark ones, so the arrow always stays readable. */
    static int onColor(int color) {
        double l = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
        return l > 0.68 ? 0xFF0B1024 : Color.WHITE;
    }

    static int blend(int a, int b, float t) {
        float u = 1 - t;
        return Color.argb(
                Math.round(Color.alpha(a) * u + Color.alpha(b) * t),
                Math.round(Color.red(a) * u + Color.red(b) * t),
                Math.round(Color.green(a) * u + Color.green(b) * t),
                Math.round(Color.blue(a) * u + Color.blue(b) * t));
    }

    static int sample(int[] cs, float k) {
        if (cs.length == 1) return cs[0];
        float x = Math.max(0f, Math.min(1f, k)) * (cs.length - 1);
        int i = (int) Math.floor(x);
        if (i >= cs.length - 1) return cs[cs.length - 1];
        return blend(cs[i], cs[i + 1], x - i);
    }

    static int withAlpha(int c, int alpha) { return (c & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24); }
}
