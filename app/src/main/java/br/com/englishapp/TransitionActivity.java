package br.com.englishapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

import br.com.englishapp.model.DBHandler;
import br.com.englishapp.model.Exercise;
import br.com.englishapp.model.ScriptEntry;

//keeps track of the current Exercise
public class TransitionActivity extends ActionBarActivity {

    private static final long TRANSITION_PAUSE = 1000;
    private DBHandler db;
    private ArrayList<Exercise> exercises;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transition);
      RelativeLayout container = (RelativeLayout) findViewById(R.id.container_transition);

        //pegar imagem do exercicio dependendo do contador e da licao
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(TransitionActivity.this);

           Exercise exercise= loadExercise(sharedPreferences.getInt("lesson_id",1),sharedPreferences.getInt("exercise_count",0));
        try {
            int imageResource = getResources().getIdentifier("drawable/" + exercise.getTransitionImage(), null, getPackageName());
            container.setBackground(getResources().getDrawable(imageResource));
        }catch (Resources.NotFoundException e){
            Toast.makeText(this,"Erro",Toast.LENGTH_SHORT).show();
        }

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
    private Exercise loadExercise(int lessonId, Integer exerciseCount) {
        //retrieve sentences to practice from db for each exercise
        ArrayList<Exercise >exercises=new ArrayList<>();
        try {
            InputStream is = getAssets()
                    .open(DBHandler.DATABASE_NAME);
            db = new DBHandler(TransitionActivity.this, is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (db != null) {
            exercises= db.findExercises(lessonId);

        }
        return exercises.get(exerciseCount);
    }
}

