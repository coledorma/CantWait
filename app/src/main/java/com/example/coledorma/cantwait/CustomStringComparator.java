package com.example.coledorma.cantwait;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by coledorma on 2017-07-26.
 */

public class CustomStringComparator implements Comparator<Event> {


    @Override
    public int compare(Event str1, Event str2) {

        String new24Time1 = timeTo24Conversion(str1.getTime());
        String new24Time2 = timeTo24Conversion(str2.getTime());

        int hour1 = Integer.parseInt(new24Time1.substring(0, new24Time1.indexOf(":")));
        int minute1 = Integer.parseInt(new24Time1.substring(new24Time1.indexOf(":") + 1, new24Time1.length()));
        int hour2 = Integer.parseInt(new24Time2.substring(0, new24Time2.indexOf(":")));
        int minute2 = Integer.parseInt(new24Time2.substring(new24Time2.indexOf(":") + 1, new24Time2.length()));
        minute1 = hour1*60 + minute1;
        minute2 = hour2*60 + minute2;

        return minute1 - minute2;

    }

    public String timeTo24Conversion(String time){
        String new24Time = "";

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("H:mm");
            SimpleDateFormat parseFormat = new SimpleDateFormat("h:mm a");
            Date date = parseFormat.parse(time);
            new24Time = sdf.format(date);
        } catch (final ParseException e) {
            e.printStackTrace();
        }

        return new24Time;

    }

}
