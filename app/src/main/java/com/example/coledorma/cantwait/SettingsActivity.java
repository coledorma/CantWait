package com.example.coledorma.cantwait;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by coledorma on 2018-02-23.
 */

class SettingsActivity extends AppCompatActivity {

    public static final String KEY_PREF_TEXT_CHECK = "text_colour_check";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();


    }
}