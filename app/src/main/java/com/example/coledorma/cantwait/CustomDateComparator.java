package com.example.coledorma.cantwait;

import java.util.Comparator;

/**
 * Created by coledorma on 2017-08-09.
 */

public class CustomDateComparator implements Comparator<Event> {

    @Override
    public int compare(Event str1, Event str2) {

        String [] date1 = str1.getTime().split("-");
        int day1 = Integer.parseInt(date1[0]);
        int month1 = Integer.parseInt(date1[1]);
        int year1 = Integer.parseInt(date1[2]);

        String [] date2 = str2.getTime().split("-");
        int day2 = Integer.parseInt(date2[0]);
        int month2 = Integer.parseInt(date2[1]);
        int year2 = Integer.parseInt(date2[2]);

        day1 = day1 + month1*30 + year1*365;
        day2 = day2 + month2*30 + year2*365;

        return day1 - day2;

    }

}
