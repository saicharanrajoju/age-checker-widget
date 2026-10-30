package com.agechecker.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Inexact low-frequency alarm; Android may batch it to protect battery. */
public class ThemeAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (Themes.maybeDailyChange(c)) AgeWidget.refresh(c);
        schedule(c);
    }
    static void schedule(Context c) {
        AlarmManager a = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent p = PendingIntent.getBroadcast(c, 44, new Intent(c, ThemeAlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (Themes.autoMode(c) == Themes.AUTO_DAILY) {
            a.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY, p);
        } else a.cancel(p);
    }
}
