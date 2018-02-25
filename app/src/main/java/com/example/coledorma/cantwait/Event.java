package com.example.coledorma.cantwait;

import android.support.annotation.NonNull;

/**
 * Created by coledorma on 2017-07-25.
 */

public class Event {
    String time;
    String name;

    public Event(String time, String name){
        this.time = time;
        this.name = name;
    }

    public String getTime(){
        return this.time;
    }

    public String getName(){
        return this.name;
    }

    public void setTime(String time){
        this.time = time;
    }

    public void setName(String name){
        this.name = name;
    }

}
