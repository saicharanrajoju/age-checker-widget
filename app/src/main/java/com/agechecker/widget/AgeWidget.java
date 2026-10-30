package com.agechecker.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.RemoteViews;
import java.time.LocalDate;
import java.util.Locale;

public class AgeWidget extends AppWidgetProvider {
    private static final String PREFIX = "com.agechecker.widget.";
    private static final String START = PREFIX + "START_ENTRY", DIGIT = PREFIX + "DIGIT", BACK = PREFIX + "BACK";
    private static final String DONE = PREFIX + "DONE", CANCEL = PREFIX + "CANCEL", THEME = PREFIX + "THEME";
    private static final String EXTRA_ID = "widget_id", EXTRA_DIGIT = "digit";

    static void refresh(Context context) {
        AppWidgetManager m = AppWidgetManager.getInstance(context);
        for (int id : m.getAppWidgetIds(new ComponentName(context, AgeWidget.class))) update(context, m, id);
    }

    static void update(Context c, AppWidgetManager m, int id) {
        Themes.maybeDailyChange(c);
        if (Themes.prefs(c).getBoolean("entry_" + id, false)) updateKeypad(c, m, id);
        else updateResult(c, m, id);
    }

    private static void updateResult(Context c, AppWidgetManager m, int id) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_layout);
        v.setImageViewBitmap(R.id.art, artwork(c, m, id));
        int selected = Themes.selected(c);
        v.setTextViewText(R.id.theme, Themes.name(selected).replace(" Flow", "").toUpperCase(Locale.US));
        String saved = Themes.prefs(c).getString("dob", "");
        if (!saved.isEmpty()) {
            try {
                LocalDate birth = LocalDate.parse(saved, DobParser.DISPLAY);
                int years = AgeCalc.completedYears(birth, LocalDate.now());
                boolean eligible = years >= AgeCalc.THRESHOLD;
                v.setTextViewText(R.id.dob, saved);
                v.setTextViewText(R.id.result_icon, eligible ? "✓" : "×");
                v.setTextViewText(R.id.result, eligible ? "21 OR ABOVE" : "BELOW 21");
                v.setTextViewText(R.id.age, years + " years old");
                int color = Color.parseColor(eligible ? "#63E6A0" : "#FF7777");
                v.setTextColor(R.id.result, color); v.setTextColor(R.id.result_icon, color);
                v.setInt(R.id.result_card, "setBackgroundResource", eligible ? R.drawable.result_good : R.drawable.result_bad);
            } catch (RuntimeException ignored) { }
        }
        PendingIntent start = widgetAction(c, START, id, -1);
        v.setOnClickPendingIntent(R.id.dob, start);
        v.setOnClickPendingIntent(R.id.submit, start);
        v.setOnClickPendingIntent(R.id.result_card, start);
        v.setOnClickPendingIntent(R.id.mic, panelIntent(c, true));
        v.setOnClickPendingIntent(R.id.title, widgetAction(c, THEME, id, -1));
        v.setOnClickPendingIntent(R.id.theme, widgetAction(c, THEME, id, -1));
        m.updateAppWidget(id, v);
    }

    private static void updateKeypad(Context c, AppWidgetManager m, int id) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_keypad);
        v.setImageViewBitmap(R.id.art, artwork(c, m, id));
        String draft = Themes.prefs(c).getString("draft_" + id, "");
        v.setTextViewText(R.id.key_display, draft.isEmpty() ? "MM/DD/YYYY" : DobParser.formatTyping(draft));
        v.setTextColor(R.id.key_display, Themes.prefs(c).getBoolean("error_" + id, false) ? 0xFFFF7777 : Color.WHITE);
        int[] keys = {R.id.key_0,R.id.key_1,R.id.key_2,R.id.key_3,R.id.key_4,R.id.key_5,R.id.key_6,R.id.key_7,R.id.key_8,R.id.key_9};
        for (int n = 0; n <= 9; n++) v.setOnClickPendingIntent(keys[n], widgetAction(c, DIGIT, id, n));
        v.setOnClickPendingIntent(R.id.key_back, widgetAction(c, BACK, id, -1));
        v.setOnClickPendingIntent(R.id.key_done, widgetAction(c, DONE, id, -1));
        v.setOnClickPendingIntent(R.id.key_cancel, widgetAction(c, CANCEL, id, -1));
        m.updateAppWidget(id, v);
    }

    @Override public void onReceive(Context c, Intent intent) {
        super.onReceive(c, intent);
        String action = intent.getAction();
        if (action == null || !action.startsWith(PREFIX)) return;
        int id = intent.getIntExtra(EXTRA_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return;
        String draftKey = "draft_" + id, entryKey = "entry_" + id;
        String draft = Themes.prefs(c).getString(draftKey, "");
        if (START.equals(action)) {
            Themes.prefs(c).edit().putBoolean(entryKey, true).putString(draftKey, "").putBoolean("error_" + id, false).apply();
        } else if (DIGIT.equals(action)) {
            if (draft.length() < 8) Themes.prefs(c).edit().putString(draftKey, draft + intent.getIntExtra(EXTRA_DIGIT, 0)).putBoolean("error_" + id, false).apply();
        } else if (BACK.equals(action)) {
            if (!draft.isEmpty()) Themes.prefs(c).edit().putString(draftKey, draft.substring(0, draft.length() - 1)).apply();
        } else if (CANCEL.equals(action)) {
            Themes.prefs(c).edit().putBoolean(entryKey, false).remove(draftKey).apply();
        } else if (THEME.equals(action)) {
            int current = Themes.selected(c); Themes.applyPreset(c, current >= 0 ? (current + 1) % Themes.PRESETS.length : 0);
        } else if (DONE.equals(action)) {
            DobParser.Parse parsed = DobParser.parseManual(draft, LocalDate.now());
            if (parsed.error == null) {
                Themes.prefs(c).edit().putString("dob", parsed.date.format(DobParser.DISPLAY)).putBoolean(entryKey, false).remove(draftKey).remove("error_" + id).apply();
                Themes.onAgeChecked(c);
            } else Themes.prefs(c).edit().putBoolean("error_" + id, true).apply();
        }
        update(c, AppWidgetManager.getInstance(c), id);
    }

    private static Bitmap artwork(Context c, AppWidgetManager m, int id) {
        Bundle o = m.getAppWidgetOptions(id);
        int dpW = Math.max(250, o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 360));
        int dpH = Math.max(140, o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 230));
        float density = c.getResources().getDisplayMetrics().density;
        float scale = Math.min(1.35f, Math.max(1f, density * .55f));
        return RibbonPainter.renderWidget(Themes.colors(c), Math.min(650, (int)(dpW * scale)), Math.min(430, (int)(dpH * scale)), scale);
    }

    private static PendingIntent widgetAction(Context c, String action, int id, int digit) {
        Intent i = new Intent(c, AgeWidget.class).setAction(action).putExtra(EXTRA_ID, id);
        if (digit >= 0) i.putExtra(EXTRA_DIGIT, digit);
        int kind = START.equals(action) ? 1 : DIGIT.equals(action) ? 2 : BACK.equals(action) ? 3 : DONE.equals(action) ? 4 : CANCEL.equals(action) ? 5 : 6;
        int request = id * 1000 + kind * 20 + Math.max(0, digit);
        return PendingIntent.getBroadcast(c, request, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent panelIntent(Context c, boolean speech) {
        Intent i = new Intent(c, InputActivity.class).putExtra("start_speech", speech).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, speech ? 2 : 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) update(c, m, id); }
    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle options) { update(c, m, id); }
    @Override public void onEnabled(Context c) { ThemeAlarmReceiver.schedule(c); }
    @Override public void onDeleted(Context c, int[] ids) { for (int id : ids) Themes.prefs(c).edit().remove("entry_" + id).remove("draft_" + id).remove("error_" + id).apply(); }
}
