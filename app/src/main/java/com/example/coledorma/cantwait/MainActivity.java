package com.example.coledorma.cantwait;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Contacts;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "tag";
    private TabLayout tabLayout;
    private ListView listView;
    private ImageView bg_imageView;
    private Context ctx;
    private ArrayList<Event> events = new ArrayList<Event>();
    private CustomStringComparator timeComparator = new CustomStringComparator();
    private CustomDateComparator dateComparator = new CustomDateComparator();
    private int tabNum = 0;

    private ArrayList<Event> dailyEvents = new ArrayList<Event>();
    private ArrayList<Event> shortEvents = new ArrayList<Event>();
    private ArrayList<Event> longEvents = new ArrayList<Event>();

    private ArrayList<Event> tomorrowEvents = new ArrayList<Event>();
    private ArrayList<Event> recurringEvents = new ArrayList<Event>();

    public ArrayList<AlarmParams> AlarmList = new ArrayList<AlarmParams>();

    private String[] galleryPermissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    //Channel for event alarm
    int channel = 1;
    //Channel for reminder alarm
    int reminderChannel = 50000;
    //Channel for static alarm
    int staticChannel = 100000;

    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;

    private String imageUrl;
    private Boolean textColourCheckPref = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        listView = (ListView) findViewById(R.id.listView);
        bg_imageView = (ImageView) findViewById(R.id.bg_imageView);
        ctx = getApplicationContext();

        // adapt the image to the size of the display
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        Bitmap bitmap = getThumbnail("testBImage.png");
        if (bitmap == null) {
            //Set some default image that will be visible before selecting image
            //listView.setBackgroundResource(R.drawable.carnival_lighter);
        } else {
            BitmapDrawable background = new BitmapDrawable(bitmap);
            //bg_imageView.setImageDrawable(background);
            bg_imageView.setBackground(background);
        }

        //Restore settings preferences
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        textColourCheckPref = sharedPref.getBoolean(SettingsActivity.KEY_PREF_TEXT_CHECK, false);

        //Restore all daily, short, and long term events, all alarms and settings
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        editor = sharedPrefs.edit();

        updateAdapterList();

        Gson gsonDaily = new Gson();
        String jsonDaily = sharedPrefs.getString("DailyEvent", null);
        Type typeDaily = new TypeToken<ArrayList<Event>>() {}.getType();
        ArrayList<Event> sharedDailyEvents = gsonDaily.fromJson(jsonDaily, typeDaily);

        Gson gsonShort = new Gson();
        String jsonShort = sharedPrefs.getString("ShortEvent", null);
        Type typeShort = new TypeToken<ArrayList<Event>>() {}.getType();
        ArrayList<Event> sharedShortEvents = gsonShort.fromJson(jsonShort, typeShort);

        Gson gsonLong = new Gson();
        String jsonLong = sharedPrefs.getString("LongEvent", null);
        Type typeLong = new TypeToken<ArrayList<Event>>() {}.getType();
        ArrayList<Event> sharedLongEvents = gsonLong.fromJson(jsonLong, typeLong);

        Gson gsonAlarms = new Gson();
        String jsonAlarms = sharedPrefs.getString("Alarms", null);
        Type typeAlarms = new TypeToken<ArrayList<AlarmParams>>() {}.getType();
        ArrayList<AlarmParams> sharedAlarmList = gsonAlarms.fromJson(jsonAlarms, typeAlarms);

        //Restore the alarm channels as they are all stored on separate channels
        channel = sharedPrefs.getInt("CHANNEL", channel);
        reminderChannel = sharedPrefs.getInt("CHANNEL2", reminderChannel);
        staticChannel = sharedPrefs.getInt("CHANNEL3", staticChannel);

        //Set the restored daily, short, long term, and alarm lists to lists that are local to the app
        if ((sharedDailyEvents != null && !sharedDailyEvents.isEmpty()) || (sharedShortEvents != null && !sharedShortEvents.isEmpty()) || (sharedLongEvents != null && !sharedLongEvents.isEmpty())) {
            dailyEvents = sharedDailyEvents;
            shortEvents = sharedShortEvents;
            longEvents = sharedLongEvents;
            AlarmList = sharedAlarmList;
        }

        //Clear the alarms that are in the past
        clearAlarms();

        //Update all the lists being shown by only showing events that should be shown
        updateAdapterList();

        final FloatingActionButton[] fab = {(FloatingActionButton) findViewById(R.id.fab)};

        fab[0].setImageResource(R.drawable.ic_add_white_24px);

        fab[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String[] newEvent = new String[2];

                final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                final Intent notificationIntent = new Intent("android.media.action.DISPLAY_NOTIFICATION");

                //Channel for the actual event alarm
                channel++;
                //Channel2 for the reminder alarm for the events
                reminderChannel++;
                staticChannel++;

                //Recurring dialog
                final AlertDialog.Builder recurDialog = new AlertDialog.Builder(MainActivity.this);
                recurDialog.setTitle("Recurring");
                recurDialog.setMessage("Would you like for this to be an event for just today or everyday?");
                recurDialog.setIcon(R.drawable.ic_mood_black_24px);

                recurDialog.setPositiveButton("Today",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Event addEvent = new Event(dailyEvents.get(dailyEvents.size() - 1).time, dailyEvents.get(dailyEvents.size() - 1).name);
                                tomorrowEvents.add(addEvent);

                                String new12Time = timeTo12Conversion(dailyEvents.get(dailyEvents.size() - 1).getTime());

                                dailyEvents.get(dailyEvents.size() - 1).setTime(new12Time);

                                dialog.cancel();

                                //building alarm intent
                                buildIntent(tomorrowEvents.get(tomorrowEvents.size() - 1).getName(), tomorrowEvents.get(tomorrowEvents.size() - 1).getTime(),
                                        channel, alarmManager, notificationIntent, false);

                                updateAdapterList();

                            }
                        });

                recurDialog.setNegativeButton("Everyday",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Event addEvent = new Event(dailyEvents.get(dailyEvents.size() - 1).time, dailyEvents.get(dailyEvents.size() - 1).name);
                                recurringEvents.add(addEvent);

                                String new12Time = timeTo12Conversion(dailyEvents.get(dailyEvents.size() - 1).getTime());

                                dailyEvents.get(dailyEvents.size() - 1).setTime(new12Time);

                                dialog.cancel();

                                //building alarm intent
                                buildIntent(recurringEvents.get(recurringEvents.size() - 1).getName(), recurringEvents.get(recurringEvents.size() - 1).getTime(),
                                        channel, alarmManager, notificationIntent, true);

                                updateAdapterList();
                            }
                        });

                //time picker dialog
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                final TimePickerDialog mTimePicker;

                mTimePicker = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        newEvent[0] = String.format("%02d:%02d", selectedHour, selectedMinute);
                        Event addEvent = new Event( newEvent[0],"I can't wait until " + newEvent[1]);

                        dailyEvents.add(addEvent);
                        recurDialog.show();

                    }
                }, hour, minute, false);//Yes 24 hour time
                mTimePicker.setTitle("Select Time");
                mTimePicker.setIcon(R.drawable.ic_mood_black_24px);

                //date picker dialog
                final Calendar c = Calendar.getInstance();
                int mYear, mMonth, mDay;

                mYear = c.get(Calendar.YEAR);
                mMonth = c.get(Calendar.MONTH);
                mDay = c.get(Calendar.DAY_OF_MONTH);

                final DatePickerDialog dateDialog = new DatePickerDialog(MainActivity.this,
                        new DatePickerDialog.OnDateSetListener() {

                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int monthOfYear, int dayOfMonth) {
                                newEvent[0] = dayOfMonth + "-" + (monthOfYear + 1) + "-" + year;

                                Event addEvent = new Event(newEvent[0],"I can't wait until " +newEvent[1]);
                                if (tabNum == 1) {
                                    shortEvents.add(addEvent);

                                    //building alarm intent
                                    buildIntent(shortEvents.get(shortEvents.size() - 1).getName(), dayOfMonth + " " + (monthOfYear + 1) + "-" + year,
                                            channel, alarmManager, notificationIntent, false);

                                    //Call algorithm and build reminder alarm intent
                                    String reminderTime = alarmReminderAlgorithm(dayOfMonth + " " + (monthOfYear + 1) + "-" + year);

                                    //If there is a reminder alarm need to be set, build alarm intent
                                    if (reminderTime.length() >= 1) {
                                        buildIntent("Are you getting ready for " + shortEvents.get(shortEvents.size() - 1).getName().substring(19, shortEvents.get(shortEvents.size() - 1).getName().length()) + "?", reminderTime, reminderChannel,
                                                alarmManager, notificationIntent, true);
                                    }

                                    //Function for building static alarms based on time called here
                                    staticAlarmBuilder(shortEvents.get(shortEvents.size() - 1).getName().substring(19, shortEvents.get(shortEvents.size() - 1).getName().length()), dayOfMonth + " " + (monthOfYear + 1) + "-" + year,
                                            channel, alarmManager, notificationIntent, false);

                                    updateAdapterList();

                                } else if (tabNum == 2) {
                                    longEvents.add(addEvent);

                                    //building alarm intent
                                    buildIntent(longEvents.get(longEvents.size() - 1).getName(), dayOfMonth + " " + (monthOfYear + 1) + "-" + year,
                                            channel, alarmManager, notificationIntent, false);

                                    //Call algorithm and build reminder alarm intent
                                    String reminderTime = alarmReminderAlgorithm(dayOfMonth + " " + (monthOfYear + 1) + "-" + year);

                                    //If there is a reminder alarm need to be set, build alarm intent
                                    if (reminderTime.length() >= 1) {
                                        buildIntent("Are you getting ready for " + longEvents.get(longEvents.size() - 1).getName().substring(19, longEvents.get(longEvents.size() - 1).getName().length()) + "?", reminderTime, reminderChannel,
                                                alarmManager, notificationIntent, true);
                                    }

                                    //Function for building static alarms based on time called here
                                    staticAlarmBuilder(longEvents.get(longEvents.size() - 1).getName().substring(19, longEvents.get(longEvents.size() - 1).getName().length()), dayOfMonth + " " + (monthOfYear + 1) + "-" + year,
                                            channel, alarmManager, notificationIntent, false);

                                    updateAdapterList();
                                }
                            }
                        }, mYear, mMonth, mDay);

                dateDialog.setIcon(R.drawable.ic_mood_black_24px);


                //Input text dialog
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                alertDialog.setTitle("Can't Wait");
                alertDialog.setMessage("I can't wait for...");
                final EditText input = new EditText(MainActivity.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                alertDialog.setView(input);

                alertDialog.setPositiveButton("DONE",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                newEvent[1] = input.getText().toString();
                                dialog.cancel();

                                //If daily, show Timepicker dialog, else show date dialog
                                if (tabNum == 0) {
                                    mTimePicker.show();
                                } else if (tabNum > 0) {
                                    dateDialog.show();
                                }
                            }
                        });

                alertDialog.setIcon(R.drawable.ic_mood_black_24px);
                alertDialog.show();

            }

        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tabNum = tab.getPosition();

                if (tabNum == 0) { //Daily selected
                    System.out.println("TAB 0");
                    clearAlarms();
                    updateAdapterList();
                } else if (tabNum == 1) { //Short selected
                    System.out.println("TAB 1");
                    clearAlarms();
                    updateAdapterList();
                } else if (tabNum == 2) { //Long selected
                    System.out.println("TAB 2");
                    clearAlarms();
                    updateAdapterList();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        //Selecting an item on a list
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    final int position, long id) {

                final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                final Intent notificationIntent = new Intent("android.media.action.DISPLAY_NOTIFICATION");

                AlertDialog.Builder editDialog = new AlertDialog.Builder(MainActivity.this);
                editDialog.setTitle("Edit or Delete");
                editDialog.setMessage("Would you like to edit or delete this event?");
                editDialog.setIcon(R.drawable.ic_mood_black_24px);

                editDialog.setPositiveButton("EDIT",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                final String[] newEvent = new String[2];

                                int hour = 12;
                                int minute = 25;

                                int year = 17;
                                int month = 9;
                                int day = 15;

                                if (tabNum == 0) {
                                    String new24Time = timeTo24Conversion(dailyEvents.get(position).getTime());
                                    hour = Integer.parseInt(new24Time.substring(0, new24Time.indexOf(":")));
                                    minute = Integer.parseInt(new24Time.substring(new24Time.indexOf(":") + 1,
                                            new24Time.length()));
                                } else if (tabNum == 1) {
                                    String[] date = shortEvents.get(position).getTime().split("-");
                                    day = Integer.parseInt(date[0]);
                                    month = Integer.parseInt(date[1]) - 1;
                                    year = Integer.parseInt(date[2]);
                                } else if (tabNum == 2) {
                                    String[] date = longEvents.get(position).getTime().split("-");
                                    day = Integer.parseInt(date[0]);
                                    month = Integer.parseInt(date[1]) - 1;
                                    year = Integer.parseInt(date[2]);
                                }

                                //Recurring dialog
                                final AlertDialog.Builder recurDialog = new AlertDialog.Builder(MainActivity.this);
                                recurDialog.setTitle("Recurring");
                                recurDialog.setMessage("Would you like for this to be an event for just today or everyday?");
                                recurDialog.setIcon(R.drawable.ic_mood_black_24px);

                                recurDialog.setPositiveButton("Today",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                Event addEvent = new Event(dailyEvents.get(dailyEvents.size() - 1).time, dailyEvents.get(dailyEvents.size() - 1).name);
                                                tomorrowEvents.add(addEvent);

                                                String new12Time = timeTo12Conversion(dailyEvents.get(dailyEvents.size() - 1).getTime());

                                                dailyEvents.get(dailyEvents.size() - 1).setTime(new12Time);

                                                buildIntent(tomorrowEvents.get(tomorrowEvents.size() - 1).getName(), tomorrowEvents.get(tomorrowEvents.size() - 1).getTime(),
                                                        channel, alarmManager, notificationIntent, false);

                                                updateAdapterList();

                                                dialog.cancel();
                                            }
                                        });

                                recurDialog.setNegativeButton("Everyday",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                Event addEvent = new Event(dailyEvents.get(dailyEvents.size() - 1).time, dailyEvents.get(dailyEvents.size() - 1).name);
                                                recurringEvents.add(addEvent);

                                                String new12Time = timeTo12Conversion(dailyEvents.get(dailyEvents.size() - 1).getTime());

                                                dailyEvents.get(dailyEvents.size() - 1).setTime(new12Time);

                                                buildIntent(recurringEvents.get(recurringEvents.size() - 1).getName(), recurringEvents.get(recurringEvents.size() - 1).getTime(),
                                                        channel, alarmManager, notificationIntent, true);

                                                updateAdapterList();

                                                dialog.cancel();
                                            }
                                        });



                                //time picker dialog
                                final TimePickerDialog mTimePicker;
                                mTimePicker = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                                    @Override
                                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                                        newEvent[0] = String.format("%02d:%02d", selectedHour, selectedMinute);
                                        Event addEvent = new Event(newEvent[0],"I can't wait until " +newEvent[1]);

                                        dailyEvents.add(addEvent);

                                        recurDialog.show();

                                    }
                                }, hour, minute, false);
                                mTimePicker.setTitle("Edit Time");
                                mTimePicker.setIcon(R.drawable.ic_mood_black_24px);

                                //date picker dialog
                                final DatePickerDialog dateDialog = new DatePickerDialog(MainActivity.this,
                                        new DatePickerDialog.OnDateSetListener() {

                                            @Override
                                            public void onDateSet(DatePicker view, int year,
                                                                  int monthOfYear, int dayOfMonth) {
                                                newEvent[0] = dayOfMonth + "-" + (monthOfYear + 1) + "-" + year;

                                                Event addEvent = new Event(newEvent[0], "I can't wait until " + newEvent[1]);
                                                if (tabNum == 1) {
                                                    shortEvents.add(addEvent);

                                                    buildIntent(shortEvents.get(shortEvents.size() - 1).getName(), dayOfMonth + " " + (monthOfYear + 1) + "-" + year,
                                                            channel, alarmManager, notificationIntent, false);

                                                    //Call algorithm and build reminder alarm intent
                                                    String reminderTime = alarmReminderAlgorithm(dayOfMonth + " " + (monthOfYear + 1) + "-" + year);

                                                    //If there is a reminder alarm need to be set, build alarm intent
                                                    if (reminderTime.length() >= 1) {
                                                        buildIntent("Are you getting ready for " + shortEvents.get(shortEvents.size() - 1).getName().substring(19, shortEvents.get(shortEvents.size() - 1).getName().length()) + "?", reminderTime, reminderChannel,
                                                                alarmManager, notificationIntent, true);
                                                    }

                                                    //Function for building static alarms based on time called here
                                                    staticAlarmBuilder(shortEvents.get(shortEvents.size() - 1).getName().substring(19, shortEvents.get(shortEvents.size() - 1).getName().length()), dayOfMonth + " " + (monthOfYear + 1) + "-" + year,
                                                            channel, alarmManager, notificationIntent, false);

                                                    updateAdapterList();

                                                } else if (tabNum == 2) {
                                                    longEvents.add(addEvent);

                                                    buildIntent(longEvents.get(longEvents.size() - 1).getName(), dayOfMonth + " " + (monthOfYear + 1) + "-" + year,
                                                            channel, alarmManager, notificationIntent, false);

                                                    //Call algorithm and build reminder alarm intent
                                                    String reminderTime = alarmReminderAlgorithm(dayOfMonth + " " + (monthOfYear + 1) + "-" + year);

                                                    //If there is a reminder alarm need to be set, build alarm intent
                                                    if (reminderTime.length() >= 1) {
                                                        buildIntent("Are you getting ready for " + longEvents.get(longEvents.size() - 1).getName().substring(19, longEvents.get(longEvents.size() - 1).getName().length()) + "?", reminderTime, reminderChannel,
                                                                alarmManager, notificationIntent, true);
                                                    }

                                                    //Function for building static alarms based on time called here
                                                    staticAlarmBuilder(longEvents.get(longEvents.size() - 1).getName().substring(19, longEvents.get(longEvents.size() - 1).getName().length()), dayOfMonth + " " + (monthOfYear + 1) + "-" + year,
                                                            channel, alarmManager, notificationIntent, false);

                                                    updateAdapterList();
                                                }
                                            }
                                        }, year, month, day);
                                dateDialog.setIcon(R.drawable.ic_mood_black_24px);


                                //text input dialog
                                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                                alertDialog.setTitle("Edit Event");
                                alertDialog.setMessage("I can't wait for...");
                                alertDialog.setIcon(R.drawable.ic_mood_black_24px);

                                final EditText input = new EditText(MainActivity.this);

                                //Get and set input text to item selected text
                                if (tabNum == 0) {
                                    input.setText(dailyEvents.get(position).getName().substring(19, dailyEvents.get(position).getName().length()));
                                } else if (tabNum == 1) {
                                    input.setText(shortEvents.get(position).getName().substring(19, shortEvents.get(position).getName().length()));
                                } else if (tabNum == 2) {
                                    input.setText(longEvents.get(position).getName().substring(19, longEvents.get(position).getName().length()));
                                }

                                input.setSelection(input.getText().length());

                                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.MATCH_PARENT);
                                input.setLayoutParams(lp);
                                alertDialog.setView(input);
                                alertDialog.setIcon(R.drawable.ic_mood_black_24px);

                                alertDialog.setPositiveButton("DONE",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                newEvent[1] = input.getText().toString();
                                                dialog.cancel();
                                                if (tabNum == 0) {
                                                    mTimePicker.show();
                                                } else if (tabNum > 0){
                                                    dateDialog.show();
                                                }
                                            }
                                        });

                                dialog.cancel();
                                alertDialog.show();

                                //If statements for removing the selected list item and it's alarm initially then to re-add it with the edited information after
                                if (tabNum == 0) {
                                    Event removed = dailyEvents.remove(position);
                                    recurringEvents.remove(removed);
                                    tomorrowEvents.remove(removed);
                                    for (int i = 0; i < AlarmList.size(); i++) {
                                        if (AlarmList.get(i).title.equals(removed.name)) {
                                            AlarmParams deleteAlarm = AlarmList.get(i);
                                            AlarmList.remove(deleteAlarm);
                                            System.out.println("AlarmList Size: " + AlarmList.size());
                                            cancelIntent(deleteAlarm.channel, alarmManager, notificationIntent);
                                        }
                                    }

                                } else if (tabNum == 1) {
                                    Event removed = shortEvents.remove(position);
                                    shortEvents.remove(removed);
                                    for (int i = AlarmList.size() - 1; i >= 0; i--) {
                                        if (AlarmList.get(i).title.equals(removed.name) || AlarmList.get(i).title.contains(removed.name)) {
                                            AlarmParams deleteAlarm = AlarmList.get(i);
                                            AlarmList.remove(deleteAlarm);
                                            System.out.println("AlarmList Size: " + AlarmList.size());
                                            cancelIntent(deleteAlarm.channel, alarmManager, notificationIntent);
                                        }
                                    }
                                } else if (tabNum == 2) {
                                    Event removed = longEvents.remove(position);
                                    longEvents.remove(removed);
                                    for (int i = AlarmList.size() - 1; i >= 0; i--) {
                                        if (AlarmList.get(i).title.equals(removed.name) || AlarmList.get(i).title.contains(removed.name)) {
                                            AlarmParams deleteAlarm = AlarmList.get(i);
                                            AlarmList.remove(deleteAlarm);
                                            System.out.println("AlarmList Size: " + AlarmList.size());
                                            cancelIntent(deleteAlarm.channel, alarmManager, notificationIntent);
                                        }
                                    }
                                }

                                //Update adapter list and reload
                                updateAdapterList();

                                //Update the shared preferences lists
                                Gson gsonDaily = new Gson();
                                String jsonDaily = gsonDaily.toJson(dailyEvents);
                                System.out.println("DailyEvent count: " + dailyEvents.size());
                                editor.putString("DailyEvent", jsonDaily);

                                Gson gsonShort = new Gson();
                                String jsonShort = gsonShort.toJson(shortEvents);
                                System.out.println("ShortEvent count: " + shortEvents.size());
                                editor.putString("ShortEvent", jsonShort);

                                Gson gsonLong = new Gson();
                                String jsonLong = gsonLong.toJson(longEvents);
                                System.out.println("LongEvent count: " + longEvents.size());
                                editor.putString("LongEvent", jsonLong);

                                editor.apply();
                            }
                        });

                editDialog.setNegativeButton("DELETE",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //If statements for removing the selected list item and it's alarm
                                if (tabNum == 0) {
                                    Event removed = dailyEvents.remove(position);
                                    recurringEvents.remove(removed);
                                    tomorrowEvents.remove(removed);
                                    for (int i = 0; i < AlarmList.size(); i++) {
                                        if (AlarmList.get(i).title.equals(removed.name)) {
                                            AlarmParams deleteAlarm = AlarmList.get(i);
                                            AlarmList.remove(deleteAlarm);
                                            System.out.println("AlarmList Size: " + AlarmList.size());
                                            cancelIntent(deleteAlarm.channel, alarmManager, notificationIntent);
                                        }
                                    }
                                } else if (tabNum == 1) {
                                    Event removed = shortEvents.remove(position);
                                    shortEvents.remove(removed);
                                    for (int i = AlarmList.size() - 1; i >= 0; i--) {
                                        if (AlarmList.get(i).title.equals(removed.name) || AlarmList.get(i).title.contains(removed.name)) {
                                            AlarmParams deleteAlarm = AlarmList.get(i);
                                            AlarmList.remove(deleteAlarm);
                                            System.out.println("AlarmList Size: " + AlarmList.size());
                                            cancelIntent(deleteAlarm.channel, alarmManager, notificationIntent);
                                        }
                                    }
                                } else if (tabNum == 2) {
                                    Event removed = longEvents.remove(position);
                                    longEvents.remove(removed);
                                    for (int i = AlarmList.size() - 1; i >= 0; i--) {
                                        if (AlarmList.get(i).title.equals(removed.name) || AlarmList.get(i).title.contains(removed.name)) {
                                            AlarmParams deleteAlarm = AlarmList.get(i);
                                            AlarmList.remove(deleteAlarm);
                                            System.out.println("AlarmList Size: " + AlarmList.size());
                                            cancelIntent(deleteAlarm.channel, alarmManager, notificationIntent);
                                        }
                                    }
                                }
                                //Update adapter list and reload
                                updateAdapterList();

                                //Update the shared preferences lists
                                Gson gsonDaily = new Gson();
                                String jsonDaily = gsonDaily.toJson(dailyEvents);
                                System.out.println("DailyEvent count: " + dailyEvents.size());
                                editor.putString("DailyEvent", jsonDaily);

                                Gson gsonShort = new Gson();
                                String jsonShort = gsonShort.toJson(shortEvents);
                                System.out.println("ShortEvent count: " + shortEvents.size());
                                editor.putString("ShortEvent", jsonShort);

                                Gson gsonLong = new Gson();
                                String jsonLong = gsonLong.toJson(longEvents);
                                System.out.println("LongEvent count: " + longEvents.size());
                                editor.putString("LongEvent", jsonLong);

                                editor.commit();

                                dialog.cancel();
                            }
                        });

                editDialog.show();
                updateAdapterList();

            }

        });

    }

    //Function for updating the adapter lists being shown and sorting the newely updated lists
    public void updateAdapterList() {
        if (tabNum == 0) {
            Collections.sort(dailyEvents, timeComparator);
        } else if (tabNum == 1) {
            Collections.sort(shortEvents, dateComparator);
        } else if (tabNum == 2) {
            Collections.sort(longEvents, dateComparator);
        }

        List<Map<String, String>> data = new ArrayList<Map<String, String>>();

        if (tabNum == 0) {
            for (Event item : dailyEvents) {
                Map<String, String> datum = new HashMap<String, String>(2);
                datum.put("name", item.getName());
                datum.put("time", item.getTime());
                data.add(datum);
            }
        } else if (tabNum == 1) {
            for (Event item : shortEvents) {
                Map<String, String> datum = new HashMap<String, String>(2);
                datum.put("name", item.getName());
                datum.put("time", item.getTime());
                data.add(datum);
            }
        } else if (tabNum == 2) {
            for (Event item : longEvents) {
                Map<String, String> datum = new HashMap<String, String>(2);
                datum.put("name", item.getName());
                datum.put("time", item.getTime());
                data.add(datum);
            }
        }

        final SimpleAdapter adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_2, new String[] {"name", "time"},
                new int[] {android.R.id.text1, android.R.id.text2}) {

            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                System.out.println("This is text colour pref: "+ textColourCheckPref.toString());
                if (textColourCheckPref.toString().equals("true")){
                    text1.setTextColor(Color.BLACK);
                    text2.setTextColor(Color.BLACK);
                } else {
                    text1.setTextColor(Color.WHITE);
                    text2.setTextColor(Color.WHITE);
                }

                return view;
            };
        };

        listView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            System.out.println("Clicked settings");

            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

            return true;
        } else if (id == R.id.action_background) {
            Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, 1);
        } else if (id == R.id.action_feedback) {
            System.out.println("Clicked feedback");
            final AlertDialog.Builder infoDialog = new AlertDialog.Builder(MainActivity.this);
            infoDialog.setTitle("Information");
            infoDialog.setIcon(R.drawable.ic_mood_black_24px);
            infoDialog.setMessage("Developed by: Cole Dorma\n\nThis application is still in development, therefore if you " +
                    "notice any bugs or would like to give some feedback, please don't hesitate to click that email button below " +
                    "and I will try to get back to you as soon as I can.\n\nI hope you enjoy Can't Wait and it betters the quality of your life!");
            //editDialog.setIcon(R.drawable.key);

            infoDialog.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

            infoDialog.setNegativeButton("EMAIL",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent emailIntent = new Intent(Intent.ACTION_SEND);
                            emailIntent.setType("message/rfc822");
                            emailIntent.putExtra(Intent.EXTRA_EMAIL, "coledorma@hotmail.com");
                            startActivity(Intent.createChooser(emailIntent, "Send Email"));
                        }
                    });

            infoDialog.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            imageUrl = picturePath;
            Bitmap bitmap = null;
            if (EasyPermissions.hasPermissions(this, galleryPermissions)) {
                bitmap = BitmapFactory.decodeFile(imageUrl);
            } else {
                EasyPermissions.requestPermissions(this, "Access for storage",
                        101, galleryPermissions);
                bitmap = BitmapFactory.decodeFile(imageUrl);
            }
            saveImageToInternalStorage(bitmap);
            cursor.close();
        }
        //Recreate this Activity
        finish();
        startActivity(getIntent());
    }



    public boolean saveImageToInternalStorage(Bitmap image) {
        try {
            FileOutputStream fos = openFileOutput("testBImage.png", Context.MODE_PRIVATE);
            image.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Bitmap getThumbnail(String filename) {
        Bitmap thumbnail = null;
        try {
            File filePath = getFileStreamPath(filename);
            FileInputStream fi = new FileInputStream(filePath);
            thumbnail = BitmapFactory.decodeStream(fi);
        } catch (Exception ex) {
            System.out.println("getThumbnail() on internal storage");
        }
        return thumbnail;
    }

    @Override
    protected void onResume() {
        super.onResume();

        clearAlarms();

        updateAdapterList();
    }

    @Override
    protected void onPause() {
        super.onPause();

        clearAlarms();

        updateAdapterList();
    }

    public String getName(){
        System.out.println("Get name" + dailyEvents);
        return dailyEvents.get(0).getName();
    }

    public String getTime(){
        return dailyEvents.get(0).getTime();
    }

    //Algorithm for setting alarm reminder times based on the events time in the future
    public String alarmReminderAlgorithm(String time) {

        System.out.println("Reminder Algorithm");

        int day = Integer.parseInt(time.substring(0, time.indexOf(" ")));
        int month = Integer.parseInt(time.substring(time.indexOf(" ") + 1, time.indexOf("-")));
        int year = Integer.parseInt(time.substring(time.indexOf("-") + 1, time.length()));

        Calendar mcurrentTime = Calendar.getInstance();
        int currYear = mcurrentTime.get(Calendar.YEAR);
        int currMonth = mcurrentTime.get(Calendar.MONTH);
        int currDay = mcurrentTime.get(Calendar.DAY_OF_MONTH);

        day = day + month*30 + year*365;
        currDay = currDay + (currMonth+1)*30 + currYear*365;

        //Day before 3 days before, 7 days, 14, 21, 1 month
        if (day > currDay+2 && day <= currDay+7){ //week -- reminder every 2 days
            System.out.println("Repeating alarm set for every 2 days");
            return "2";
        } else if (day > currDay+7 && day <= currDay+30) { //month -- reminder every 7 days
            System.out.println("Repeating alarm set for every 7 days");
            return "7";
        } else if (day > currDay+30 && day <= currDay+90) { //3 months -- reminder every 14 days
            System.out.println("Repeating alarm set for every 14 days");
            return "14";
        } else if (day > currDay+90 && day <= currDay+180) { //6 months -- reminder every 21 days
            System.out.println("Repeating alarm set for every 21 days");
            return "21";
        } else if (day > currDay+180) { //greater than 6 months -- reminder every 30 days
            System.out.println("Repeating alarm set for every 30 days");
            return "30";
        }
        return "";
    }

    //Function for setting static alarms as the event is approaching
    public void staticAlarmBuilder(String title, String time, int inputChannel, AlarmManager am, Intent intent, boolean repeat) {
        System.out.println("Reminder Algorithm");

        int day = Integer.parseInt(time.substring(0, time.indexOf(" ")));
        int dayOfMonth = day;
        int month = Integer.parseInt(time.substring(time.indexOf(" ") + 1, time.indexOf("-")));
        int year = Integer.parseInt(time.substring(time.indexOf("-") + 1, time.length()));

        Calendar eventTime = Calendar.getInstance();
        eventTime.set(Calendar.YEAR, year);
        eventTime.set(Calendar.MONTH, month);
        eventTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        long eventTimeMilli = eventTime.getTimeInMillis();

        Calendar mcurrentTime = Calendar.getInstance();
        int currYear = mcurrentTime.get(Calendar.YEAR);
        int currMonth = mcurrentTime.get(Calendar.MONTH);
        int currDay = mcurrentTime.get(Calendar.DAY_OF_MONTH);

        day = day + month*30 + year*365;
        currDay = currDay + (currMonth+1)*30 + currYear*365;

        //Format: dayOfMonth + " " + (monthOfYear + 1) + "-" + year
        long oneDay = AlarmManager.INTERVAL_DAY;
        int daysBefore = 0;
        long reminderTime = 0;
        Calendar reminderTimeDate = Calendar.getInstance();

        System.out.println("Day and currDay: " + day + currDay);

        //Setting static alarms based on the time
        if (day >= currDay+1){ //alarm day before
            System.out.println("1 day before alarm set");
            daysBefore = 1;
            reminderTime = eventTimeMilli - (daysBefore*oneDay);
            reminderTimeDate.setTimeInMillis(reminderTime);
            String staticTime = (reminderTimeDate.get(Calendar.DAY_OF_MONTH)) + " " + (reminderTimeDate.get(Calendar.MONTH)) + "-" + reminderTimeDate.get(Calendar.YEAR);
            buildIntent("Only 1 day until " + title + "!", staticTime, inputChannel, am, intent, false);
        }
        if (day >= currDay+3) { //alarm 3 days before
            System.out.println("3 days before alarm set");
            daysBefore = 3;
            reminderTime = eventTimeMilli - (daysBefore*oneDay);
            reminderTimeDate.setTimeInMillis(reminderTime);
            String staticTime = (reminderTimeDate.get(Calendar.DAY_OF_MONTH)) + " " + (reminderTimeDate.get(Calendar.MONTH)) + "-" + reminderTimeDate.get(Calendar.YEAR);
            buildIntent("Only 3 days until " + title + "!", staticTime, inputChannel, am, intent, false);
        }
        if (day >= currDay+7) { //alarm 7 days before
            System.out.println("7 days before alarm set");
            daysBefore = 7;
            reminderTime = eventTimeMilli - (daysBefore*oneDay);
            reminderTimeDate.setTimeInMillis(reminderTime);
            String staticTime = (reminderTimeDate.get(Calendar.DAY_OF_MONTH)) + " " + (reminderTimeDate.get(Calendar.MONTH)) + "-" + reminderTimeDate.get(Calendar.YEAR);
            buildIntent("Only 7 days until " + title + "!", staticTime, inputChannel, am, intent, false);
        }
        if (day >= currDay+14) { //alarm 14 days before
            System.out.println("14 days before alarm set");
            daysBefore = 14;
            reminderTime = eventTimeMilli - (daysBefore*oneDay);
            reminderTimeDate.setTimeInMillis(reminderTime);
            String staticTime = (reminderTimeDate.get(Calendar.DAY_OF_MONTH)) + " " + (reminderTimeDate.get(Calendar.MONTH)) + "-" + reminderTimeDate.get(Calendar.YEAR);
            buildIntent("Only 14 days until " + title + "!", staticTime, inputChannel, am, intent, false);
        }
        if (day >= currDay+21) { //alarm 21 days before
            System.out.println("21 days before alarm set");
            daysBefore = 21;
            reminderTime = eventTimeMilli - (daysBefore*oneDay);
            reminderTimeDate.setTimeInMillis(reminderTime);
            String staticTime = (reminderTimeDate.get(Calendar.DAY_OF_MONTH)) + " " + (reminderTimeDate.get(Calendar.MONTH)) + "-" + reminderTimeDate.get(Calendar.YEAR);
            buildIntent("Only 21 days until " + title + "!", staticTime, inputChannel, am, intent, false);
        }
        if (day >= currDay+30) { //alarm 30 days before
            daysBefore = 30;
            reminderTime = eventTimeMilli - (daysBefore*oneDay);
            reminderTimeDate.setTimeInMillis(reminderTime);
            System.out.println("30 days before alarm set");
            String staticTime = (reminderTimeDate.get(Calendar.DAY_OF_MONTH)) + " " + (reminderTimeDate.get(Calendar.MONTH)) + "-" + reminderTimeDate.get(Calendar.YEAR);
            buildIntent("Only 30 days until " + title + "!", staticTime, inputChannel, am, intent, false);
        }
    }

    //Function for building alarm intent
    public void buildIntent(String title, String time, int inputChannel, AlarmManager am, Intent intent, boolean repeat){
        int intervalTime = 0;

        if (time.length() <= 2){
            intervalTime = Integer.parseInt(time);
            time = "How excited are you?!";
        }

        intent.putExtra("title", title);
        intent.putExtra("time", time);

        PendingIntent broadcast = PendingIntent.getBroadcast(ctx, inputChannel, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar cal = Calendar.getInstance();
        Calendar currCal = Calendar.getInstance();
        int currHour = currCal.get(Calendar.HOUR_OF_DAY);
        int currMinute = currCal.get(Calendar.MINUTE);
        int currTime = currHour*60 + currMinute;


        if (tabNum == 0){ //If in daily
            int hour = 0;
            int minute = 0;

            hour = Integer.parseInt(time.substring(0, time.indexOf(":")));
            minute = Integer.parseInt(time.substring(time.indexOf(":") + 1, time.length()));
            int setTime = hour*60 + minute;

            cal.setTimeInMillis(System.currentTimeMillis());
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
            cal.set(Calendar.MONTH, cal.get(Calendar.MONTH));

            if (repeat) {
                if (currTime > setTime) {
                    cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) + 1);
                }
            } else {
                cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH));
            }

            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 8);
        } else if ((tabNum == 1 && time.length() <= 10) || inputChannel >= 100000){ //If in short term or a static alarm
            int day = 0;
            int month = 0;
            int year = 0;

            System.out.println("Time: " + time);

            day = Integer.parseInt(time.substring(0, time.indexOf(" ")));
            month = Integer.parseInt(time.substring(time.indexOf(" ") + 1, time.indexOf("-")));
            year = Integer.parseInt(time.substring(time.indexOf("-") + 1, time.length()));

            cal.setTimeInMillis(System.currentTimeMillis());
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, day);
            cal.set(Calendar.HOUR_OF_DAY, 8);
            cal.set(Calendar.MINUTE, 1);

        } else if (tabNum == 2 && time.length() <= 10){ //If in long term
            int day = 0;
            int month = 0;
            int year = 0;

            day = Integer.parseInt(time.substring(0, time.indexOf(" ")));
            month = Integer.parseInt(time.substring(time.indexOf(" ") + 1, time.indexOf("-")));
            year = Integer.parseInt(time.substring(time.indexOf("-") + 1, time.length()));

            cal.setTimeInMillis(System.currentTimeMillis());
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, day);
            cal.set(Calendar.HOUR_OF_DAY, 8);
            cal.set(Calendar.MINUTE, 1);

        }

        //Check if alarm is meant to be recurring or not
        if (repeat) {
            //Check if its a regular repeating alarm or a reminder
            if (title.length() >= 25 && title.substring(0,25).equals("Are you getting ready for")) {

                System.out.println("Reminder being created");
                cal.setTimeInMillis(System.currentTimeMillis());
                cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
                cal.set(Calendar.MONTH, cal.get(Calendar.MONTH));
                cal.set(Calendar.DAY_OF_MONTH, (cal.get(Calendar.DAY_OF_MONTH))+2);
                cal.set(Calendar.HOUR_OF_DAY, 10);
                cal.set(Calendar.MINUTE, 15);
                cal.set(Calendar.SECOND, 2);

                System.out.println("Interval time: " + intervalTime);

                am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY*intervalTime, broadcast);

            } else {
                System.out.println("Repeating daily being created");
                am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, broadcast);
            }

            AlarmParams alarm = new AlarmParams(title, time, cal, inputChannel, true);
            AlarmList.add(alarm);
            Gson gsonAlarm = new Gson();
            String jsonAlarm = gsonAlarm.toJson(AlarmList);
            System.out.println("AlarmList count: " + AlarmList.size());

            editor.putString("Alarms", jsonAlarm);
            editor.commit();

        } else {
            AlarmParams alarm = new AlarmParams(title, time, cal, inputChannel, false);
            AlarmList.add(alarm);
            Gson gsonAlarm = new Gson();
            System.out.println("AlarmList count: " + AlarmList.size());
            String jsonAlarm = gsonAlarm.toJson(AlarmList);

            editor.putString("Alarms", jsonAlarm);
            editor.commit();

            am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), broadcast);
        }

        //Update and add to the shared preferences lists
        Gson gsonDaily = new Gson();
        String jsonDaily = gsonDaily.toJson(dailyEvents);
        System.out.println("DailyEvent count: " + dailyEvents.size());
        editor.putString("DailyEvent", jsonDaily);

        Gson gsonShort = new Gson();
        String jsonShort = gsonShort.toJson(shortEvents);
        System.out.println("ShortEvent count: " + shortEvents.size());
        editor.putString("ShortEvent", jsonShort);

        Gson gsonLong = new Gson();
        String jsonLong = gsonLong.toJson(longEvents);
        System.out.println("LongEvent count: " + longEvents.size());
        editor.putString("LongEvent", jsonLong);

        editor.putInt("CHANNEL", channel);
        editor.putInt("CHANNEL2", reminderChannel);
        editor.putInt("CHANNEL3", staticChannel);

        editor.commit();

    }

    //Cancel the given inputted alarm
    public void cancelIntent(int channel, AlarmManager am, Intent intent){
        PendingIntent broadcast = PendingIntent.getBroadcast(ctx, channel, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        am.cancel(broadcast);
    }

    //For checking which alarms are in the past and cancelling them
    public void clearAlarms(){
        final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        final Intent notificationIntent = new Intent("android.media.action.DISPLAY_NOTIFICATION");

        //if statement to check if all are zero, if so, then remove all alarms
        if (dailyEvents.size() == 0 && shortEvents.size() == 0 && longEvents.size() == 0 && AlarmList.size() != 0) {
            for (int i = AlarmList.size() - 1; i >= 0; i--) {
                AlarmParams removed = AlarmList.get(i);
                AlarmList.remove(removed);
                cancelIntent(removed.channel, alarmManager, notificationIntent);
            }
        }

        Calendar mcurrentTime = Calendar.getInstance();
        int currYear = mcurrentTime.get(Calendar.YEAR);
        int currMonth = mcurrentTime.get(Calendar.MONTH);
        int currDay = mcurrentTime.get(Calendar.DAY_OF_MONTH);
        int currHour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int currMinute = mcurrentTime.get(Calendar.MINUTE);

        if (dailyEvents.size() > 0){
            for (int k = dailyEvents.size() - 1; k >= 0; k--){

                for (Event event : dailyEvents){
                    System.out.println("Event time1: " + dailyEvents.get(k).getTime());
                }

                String new24Time = timeTo24Conversion(dailyEvents.get(k).getTime());

                for (Event event : dailyEvents){
                    System.out.println("Event time: " + dailyEvents.get(k).getTime());
                }

                int hour = Integer.parseInt(new24Time.substring(0, new24Time.indexOf(":")));
                int minute = Integer.parseInt(new24Time.substring(new24Time.indexOf(":") + 1,
                        new24Time.length()));

                int totalTime = (hour*60) + minute;

                if (((currHour*60) + currMinute) >= totalTime) {

                    for (int j = AlarmList.size() - 1; j >= 0; j--) {
                        if (AlarmList.get(j).title.equals(dailyEvents.get(k).name) && !(AlarmList.get(j).recur)) {
                            AlarmParams deleteAlarm = AlarmList.get(j);
                            AlarmList.remove(deleteAlarm);
                            cancelIntent(deleteAlarm.channel, alarmManager, notificationIntent);
                            dailyEvents.remove(k);
                            System.out.println("Daily alarm event removed!");
                            break;
                        }
                    }

                }
            }
        }

        if (shortEvents.size() > 0){
            for (int i = shortEvents.size() - 1; i >= 0; i--){
                String[] date = shortEvents.get(i).getTime().split("-");
                int day = Integer.parseInt(date[0]);
                int month = Integer.parseInt(date[1]) - 1;
                int year = Integer.parseInt(date[2]);

                if (currYear >= year && currMonth >= month && currDay >= day){
                    System.out.println("Short event removed!");
                    Event removed = shortEvents.remove(i);

                    for (int j = AlarmList.size() - 1; j >= 0; j--) {
                        if (AlarmList.get(j).title.equals(removed.name) || AlarmList.get(j).title.equals("Are you getting ready for " + removed.name + "?")) {
                            AlarmParams deleteAlarm = AlarmList.get(j);
                            cancelIntent(deleteAlarm.channel, alarmManager, notificationIntent);
                            AlarmList.remove(deleteAlarm);
                            System.out.println("Short alarm event removed!");
                        }
                    }

                }
            }
        }

        if (longEvents.size() > 0){
            for (int i = longEvents.size() - 1; i >= 0; i--){
                String[] date = longEvents.get(i).getTime().split("-");
                int day = Integer.parseInt(date[0]);
                int month = Integer.parseInt(date[1]) - 1;
                int year = Integer.parseInt(date[2]);

                if (currYear >= year && currMonth >= month && currDay >= day){
                    System.out.println("Long event removed!");
                    Event removed = longEvents.remove(i);

                    for (int j = AlarmList.size() - 1; j >= 0; j--) {
                        if (AlarmList.get(j).title.equals(removed.name) || AlarmList.get(j).title.equals("Are you getting ready for " + removed.name + "?")) {
                            AlarmParams deleteAlarm = AlarmList.get(j);
                            cancelIntent(deleteAlarm.channel, alarmManager, notificationIntent);
                            AlarmList.remove(deleteAlarm);
                            System.out.println("Long alarm event removed!");
                        }
                    }

                }
            }
        }

    }

    public String timeTo12Conversion(String time){
        String new12Time = "";

        try {
            final SimpleDateFormat sdf = new SimpleDateFormat("H:mm");
            final Date dateObj = sdf.parse(time);
            new12Time = new SimpleDateFormat("h:mm a").format(dateObj);
        } catch (final ParseException e) {
            e.printStackTrace();
        }

        return new12Time;
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
