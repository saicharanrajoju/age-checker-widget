package com.agechecker.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

public class AgeWidget extends AppWidgetProvider {
    static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("MM/dd/uuuu");
    static void refresh(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, AgeWidget.class));
        for (int id : ids) update(context, manager, id);
    }
    static void update(Context context, AppWidgetManager manager, int id) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        String saved = context.getSharedPreferences("age", Context.MODE_PRIVATE).getString("dob", "");
        if (!saved.isEmpty()) {
            try {
                LocalDate birth = LocalDate.parse(saved, FORMAT);
                int age = Period.between(birth, LocalDate.now()).getYears();
                views.setTextViewText(R.id.dob, saved + "   •   Tap to change 🎙");
                views.setTextViewText(R.id.result, (age >= 21 ? "✓  21 OR ABOVE" : "✕  BELOW 21") + "  ·  Age: " + age);
                views.setTextColor(R.id.result, Color.parseColor(age >= 21 ? "#63E6A0" : "#FF7777"));
            } catch (Exception ignored) { views.setTextViewText(R.id.result, "Enter a valid DOB"); }
        }
        Intent intent = new Intent(context, InputActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.dob, pending);
        views.setOnClickPendingIntent(R.id.title, pending);
        views.setOnClickPendingIntent(R.id.result, pending);
        manager.updateAppWidget(id, views);
    }
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) update(c,m,id); }
}
