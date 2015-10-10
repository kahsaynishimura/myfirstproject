package br.com.englishapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ArrayAdapter;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import br.com.englishapp.model.DBHandler;
import br.com.englishapp.model.Lesson;
import br.com.englishapp.adapters.LessonAdapter;
import br.com.englishapp.model.ScriptEntry;

public class LessonActivity extends ActionBarActivity {
    Integer bookId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson);
        loadComponents();
    }

    private void loadComponents() {

        bookId = getIntent().getIntExtra("bookId", 1);
        ArrayList<Lesson> lessons = getLessons(bookId);
        final ArrayAdapter<Lesson> a = new LessonAdapter(LessonActivity.this, R.layout.lesson_list_item, lessons);
        ListView myLessons = (ListView) findViewById(R.id.lessons);
        myLessons.setAdapter(a);
        myLessons.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(LessonActivity.this, TransitionActivity.class);
                Lesson l=(Lesson) a.getItem(position);

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LessonActivity.this);
                SharedPreferences.Editor editor = sharedPreferences.edit();

                editor.putInt("exercise_count", 0);
                editor.putInt("lesson_id", l.get_id());
                editor.commit();
                startActivity(i);
            }
        });
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_lesson, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
