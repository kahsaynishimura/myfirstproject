package br.com.englishapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import br.com.englishapp.model.DBHandler;
import br.com.englishapp.model.Lesson;

public class LessonCompletedActivity extends ActionBarActivity {
    TextView txt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_completed);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LessonCompletedActivity.this);
        txt = (TextView) findViewById(R.id.txt_message);
        Integer correctSentenceCount = sharedPreferences.getInt("correct_sentence_count", 0);

        txt.setText(getString(R.string.you_completed) + " " + correctSentenceCount + " " + getString(R.string.correct_sentences_in_this_lesson));


        ArrayList<Lesson> lessons = getLessons(sharedPreferences.getInt("book_id", 1));
        Lesson lastLesson = lessons.get(lessons.size() - 1);
        Integer lessonId = sharedPreferences.getInt("lesson_id", 0);
        if (lessonId == lastLesson.get_id()) {//se foi a ultima lição, mostra o certificado
            txt.setText(getString(R.string.you_graduated_from) + " " + getBookName(sharedPreferences.getInt("book_id", 1)));
            Button btn = (Button) findViewById(R.id.congrats_image);
            btn.setText(getString(R.string.exit));
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
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
            editor.putInt("correct_sentence_count", 0);
            editor.commit();

            Intent i = new Intent(LessonCompletedActivity.this, TransitionActivity.class);
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

    public String getBookName(Integer bookId) {

        String[] books = getResources().getStringArray(R.array.books);

        return books[bookId - 1];
    }
}
