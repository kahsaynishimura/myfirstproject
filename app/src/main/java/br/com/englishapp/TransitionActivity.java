package br.com.englishapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.widget.ImageView;

import java.util.ArrayList;

import br.com.englishapp.model.DBHandler;
import br.com.englishapp.model.Exercise;

//keeps track of the current Exercise
public class TransitionActivity extends ActionBarActivity {

    private static final long TRANSITION_PAUSE = 1000;
    private DBHandler db;
    private ArrayList<Exercise> exercises;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transition);


        ImageView img = (ImageView) findViewById(R.id.exercise_image);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent i = new Intent(TransitionActivity.this, PracticeActivity.class);
                startActivity(i);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                TransitionActivity.this.finish();
            }
        }, TRANSITION_PAUSE);

    }
}

