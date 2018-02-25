package com.example.coledorma.cantwait;

import java.util.Calendar;

/**
 * Created by coledorma on 2017-08-03.
 */

public class AlarmParams {
    //title, time, calendar'd time channel, recurring
    String title = "";
    String time = "";
    Calendar calTime = null;
    int channel = 0;
    boolean recur = false;

    public AlarmParams(String title, String time, Calendar calTime, int channel, boolean recur){
        this.title = title;
        this.time = time;
        this.calTime = calTime;
        this.channel = channel;
        this.recur = recur;
    }
}
