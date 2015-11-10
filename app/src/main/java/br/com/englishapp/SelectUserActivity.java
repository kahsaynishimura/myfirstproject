package br.com.englishapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.EditText;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import br.com.englishapp.model.DBHandler;
import br.com.englishapp.model.Lesson;
import br.com.englishapp.model.User;

public class SelectUserActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_user);
    }

    public User findUser(View v) {
        DBHandler db = null;
        User user = new User();
        try {
            InputStream is = getBaseContext().getAssets()
                    .open(DBHandler.DATABASE_NAME);
            db = new DBHandler(this, is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (db != null) {
            String code = ((EditText) findViewById(R.id.student_code)).getText().toString();

            user = db.findUser(code);
            if (user != null) {
                Intent i = new Intent(SelectUserActivity.this, LessonActivity.class);

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(SelectUserActivity.this);
                SharedPreferences.Editor editor = sharedPreferences.edit();

                //getting the current time in milliseconds, and creating a Date object from it:
                Date date = new Date(System.currentTimeMillis()); //or simply new Date();

                //converting it back to a milliseconds representation:
                long millis = date.getTime();

                editor.putInt("exercise_count", 0);
                editor.putInt("correct_sentence_count", 0);
                editor.putInt("wrong_sentence_count", 0);
                editor.putInt("user_id", user.get_id());
                editor.putInt("last_lesson_completed_id", user.getLastCompletedLessonId());
                editor.commit();
                startActivity(i);
                finish();
            }
        }
        return user;
    }
}
