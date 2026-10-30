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

public class AgeWidget extends AppWidgetProvider {
    static void refresh(Context context) {
        AppWidgetManager m = AppWidgetManager.getInstance(context);
        for (int id : m.getAppWidgetIds(new ComponentName(context, AgeWidget.class))) update(context, m, id);
    }

    static void update(Context c, AppWidgetManager m, int id) {
        Themes.maybeDailyChange(c);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_layout);
        Bundle o = m.getAppWidgetOptions(id);
        int dpW = Math.max(250, o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 360));
        int dpH = Math.max(140, o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 230));
        float density = c.getResources().getDisplayMetrics().density;
        float scale = Math.min(1.35f, Math.max(1f, density * .55f));
        Bitmap art = RibbonPainter.renderWidget(Themes.colors(c), Math.min(650, (int)(dpW * scale)), Math.min(430, (int)(dpH * scale)), scale);
        v.setImageViewBitmap(R.id.art, art);

        String saved = Themes.prefs(c).getString("dob", "");
        if (!saved.isEmpty()) {
            try {
                LocalDate birth = LocalDate.parse(saved, DobParser.DISPLAY);
                int years = AgeCalc.completedYears(birth, LocalDate.now());
                boolean eligible = years >= AgeCalc.THRESHOLD;
                v.setTextViewText(R.id.dob, saved);
                v.setTextViewText(R.id.result_icon, eligible ? "✓" : "×");
                v.setTextViewText(R.id.result, eligible ? "21 OR ABOVE" : "BELOW 21");
                v.setTextViewText(R.id.age, "Age: " + years + " years");
                int color = Color.parseColor(eligible ? "#63E6A0" : "#FF7777");
                v.setTextColor(R.id.result, color);
                v.setTextColor(R.id.result_icon, color);
                v.setInt(R.id.result_card, "setBackgroundResource", eligible ? R.drawable.result_good : R.drawable.result_bad);
            } catch (RuntimeException ignored) { }
        }
        v.setOnClickPendingIntent(R.id.dob, panelIntent(c, false));
        v.setOnClickPendingIntent(R.id.mic, panelIntent(c, true));
        v.setOnClickPendingIntent(R.id.submit, panelIntent(c, false));
        v.setOnClickPendingIntent(R.id.title, panelIntent(c, false));
        v.setOnClickPendingIntent(R.id.result_card, panelIntent(c, false));
        m.updateAppWidget(id, v);
    }

    private static PendingIntent panelIntent(Context c, boolean speech) {
        Intent i = new Intent(c, InputActivity.class).putExtra("start_speech", speech)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, speech ? 2 : 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) update(c, m, id); }
    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle options) { update(c, m, id); }
    @Override public void onEnabled(Context c) { ThemeAlarmReceiver.schedule(c); }
}
