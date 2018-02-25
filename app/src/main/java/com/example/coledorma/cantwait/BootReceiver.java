package com.example.coledorma.cantwait;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;

import static android.content.ContentValues.TAG;

/**
 * Created by coledorma on 2017-07-27.
 */

public class BootReceiver extends BroadcastReceiver {

    private static final String BOOT_COMPLETED =
            "android.intent.action.BOOT_COMPLETED";
    private static final String QUICKBOOT_POWERON =
            "android.intent.action.QUICKBOOT_POWERON";

    @Override
    public void onReceive(Context context, Intent intent) {
        Context ct;
        SharedPreferences sharedPrefs;
        ct = context;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ct);

        System.out.println("BOOT RECEIVER!!!!!");

        Gson gson = new Gson();
        String json = sharedPrefs.getString("Alarms", null);
        Type type = new TypeToken<ArrayList<AlarmParams>>() {}.getType();
        ArrayList<AlarmParams> AlarmList = gson.fromJson(json, type);

        System.out.println("AlarmList count: " + AlarmList.size());

        String action = intent.getAction();
        if (action.equals(BOOT_COMPLETED) ||
                action.equals(QUICKBOOT_POWERON)) {

            AlarmManager alarmManager = (AlarmManager) ct.getSystemService(Context.ALARM_SERVICE);

            //title, time, calendar'd time, channel, recurring
            for (AlarmParams alarm : AlarmList){
                Intent notificationIntent = new Intent(ct, AlarmReceiver.class);
                notificationIntent.putExtra("title", alarm.title);
                notificationIntent.putExtra("time", alarm.time);
                notificationIntent.setAction("android.media.action.DISPLAY_NOTIFICATION");
                PendingIntent broadcast = PendingIntent.getBroadcast(ct, alarm.channel, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                if (alarm.recur) {
                    if (alarm.title.substring(0,30).equals("Do you have everything set for")) {

                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(System.currentTimeMillis());
                        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
                        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH));
                        cal.set(Calendar.DAY_OF_MONTH, (cal.get(Calendar.DAY_OF_MONTH))+2);
                        cal.set(Calendar.HOUR_OF_DAY, 14);
                        cal.set(Calendar.MINUTE, 15);
                        cal.set(Calendar.SECOND, 2);

                        int intervalTime = Integer.parseInt(alarm.time);

                        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY*intervalTime, broadcast);

                    } else {
                        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, alarm.calTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY, broadcast);
                    }
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, alarm.calTime.getTimeInMillis(), broadcast);
                }

            }

            System.out.println("The end!!!!!");

        }
    }

}