package br.com.englishapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import br.com.englishapp.model.DBHandler;
import br.com.englishapp.model.Lesson;

public class LessonCompletedActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_completed);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LessonCompletedActivity.this);

        saveLastLessonCompletedId(sharedPreferences);

        long millis = sharedPreferences.getLong("start_time", 0L);
        Date startTime = new Date(millis);
        Integer correctSentenceCount = sharedPreferences.getInt("correct_sentence_count", 0);
        Integer wrongSentenceCount = sharedPreferences.getInt("wrong_sentence_count", 0);
        Integer percentageWrong = (wrongSentenceCount * 100) / correctSentenceCount;

        //no matter what happens, if the student gets here, he is rewarded.
        Integer userPoints = 2;
        ImageView userPointsImage = (ImageView) findViewById(R.id.user_points);
        userPointsImage.setImageDrawable(getDrawable(R.drawable.pointstwo));

        if (percentageWrong > 60 && percentageWrong <= 100) {
            userPoints = 4;
            userPointsImage.setImageDrawable(getDrawable(R.drawable.pointsfour));
        } else if (percentageWrong > 30 && percentageWrong <= 60) {
            userPoints = 6;
            userPointsImage.setImageDrawable(getDrawable(R.drawable.pointssix));
        } else if (percentageWrong > 10 && percentageWrong <= 30) {
            userPoints = 8;
            userPointsImage.setImageDrawable(getDrawable(R.drawable.pointseight));
        } else if (percentageWrong <= 10) {
            userPoints = 10;
            userPointsImage.setImageDrawable(getDrawable(R.drawable.pointsten));
        }
        DateFormat df = DateFormat.getTimeInstance();
        ((TextView) findViewById(R.id.txt_start_time)).setText(getString(R.string.start_time) + ": " + df.format(startTime));
        ((TextView) findViewById(R.id.txt_finish_time)).setText(getString(R.string.finish_time) + ": " + df.format(new Date()));
        ((TextView) findViewById(R.id.txt_correct)).setText(getString(R.string.correct) + ": " + correctSentenceCount);
        ((TextView) findViewById(R.id.txt_wrong_percentage)).setText(getString(R.string.errors_percentage) + ": " + percentageWrong + "%");
    }

    public void saveLastLessonCompletedId(SharedPreferences sharedPreferences) {

        DBHandler db = null;

        try {
            InputStream is = getBaseContext().getAssets()
                    .open(DBHandler.DATABASE_NAME);
            db = new DBHandler(this, is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (db != null) {
             db.saveLastLessonCompletedId(sharedPreferences.getInt("user_id", 0), sharedPreferences.getInt("lesson_id", 0));
        }
    }

    public void nextLesson(View v) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LessonCompletedActivity.this);

        ArrayList<Lesson> lessons = getLessons(sharedPreferences.getInt("book_id", 1));
        Lesson lastLesson = lessons.get(lessons.size() - 1);
        Integer lessonId = sharedPreferences.getInt("lesson_id", 0);

        if (lessonId != lastLesson.get_id()) {//if that was not the last lesson, start the next one
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("exercise_count", 0);
            editor.putInt("lesson_id", (lessonId + 1));
            editor.putInt("wrong_sentence_count", 0);
            editor.putLong("start_time", 0);
            editor.commit();

            Intent i = new Intent(LessonCompletedActivity.this, TransitionActivity.class);
            startActivity(i);
        } else {
            Intent i = new Intent(LessonCompletedActivity.this, BookCompletedActivity.class);
            startActivity(i);
        }
        finish();
    }

    private ArrayList<Lesson> getLessons(Integer bookId) {

        DBHandler db = null;

        try {
            InputStream is = getBaseContext().getAssets()
                    .open(DBHandler.DATABASE_NAME);
            db = new DBHandler(this, is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (db != null) {
            return db.findLessons(bookId);
        }
        return null;
    }


}
