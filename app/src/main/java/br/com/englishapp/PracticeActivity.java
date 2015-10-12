package br.com.englishapp;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import br.com.englishapp.model.CurrentPracticeData;
import br.com.englishapp.model.DBHandler;
import br.com.englishapp.model.Exercise;
import br.com.englishapp.model.Lesson;
import br.com.englishapp.model.ScriptEntry;

import static br.com.englishapp.model.CurrentPracticeData.REQ_CODE_SPEECH_INPUT;

//This class handles the screen that will execute the scripts the user is supposed to practice-> the sentences.

public class PracticeActivity extends ActionBarActivity {


    CurrentPracticeData current;//stores current screen info - current screen state

    private ArrayList<Exercise> exercises;
    private TextToSpeech TTS;
    private DBHandler db;


    private RelativeLayout listLayout;
    public final long INSTRUCTION_PAUSE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        current = new CurrentPracticeData();
        Integer lessonId = sharedPreferences.getInt("lesson_id", 0);
        updateTitleWithLessonName(lessonId);
        loadExercises(lessonId);
        current.setCurrentExercise(exercises.get(sharedPreferences.getInt("exercise_count", 0)));

        setContentView(R.layout.activity_practice);

        if (hasMoreExercises()) {
            selectNextExercise();
            TTS = new TextToSpeech(PracticeActivity.this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {

                    startExercise();
                }
            });
        }
        checkConnection();
    }

    public void startExercise() {
        runScriptEntry();
    }

    private Boolean hasMoreExercises() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return exercises.size() > sharedPreferences.getInt("exercise_count", 0);
    }

    /*Selects the exercise to run*/
    public void selectNextExercise() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        current.setCurrentExercise(exercises.get(sharedPreferences.getInt("exercise_count", 0)));

        if (hasMoreExercises()) {

            editor.putInt("exercise_count", (sharedPreferences.getInt("exercise_count", 0) + 1));

            if (current.getCurrentExercise().getScriptEntries().size() > 0) {
                current.setCurrentScriptIndex(0);
                current.setCurrentScriptEntry(current.getCurrentExercise().getScriptEntries().get(current.getCurrentScriptIndex()));
            }
        }

        editor.commit();
    }


    private void loadExercises(Integer lessonId) {
        //retrieve sentences to practice from db for each exercise
        try {
            InputStream is = getAssets()
                    .open(DBHandler.DATABASE_NAME);
            db = new DBHandler(PracticeActivity.this, is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (db != null) {
            exercises = db.findExercises(lessonId);
            for (Exercise e : exercises) {
                ArrayList<ScriptEntry> scripts = db.findScripts(e.get_id());
                Collections.sort(scripts);
                int i = 0;
                for (ScriptEntry s : scripts) {
                    s.setScriptIndex(i);
                    i++;
                }
                e.setScriptEntries(scripts);
            }
        }
    }

    private void runScriptEntry() {
        if (current.getShouldRunScript()) {
            if (current.getCurrentScriptIndex() < current.getCurrentExercise().getScriptEntries().size()) {

                final ScriptEntry s = current.getCurrentScriptEntry();
                if (s != null) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            TextView child = new TextView(PracticeActivity.this);
                            LinearLayout parent = (LinearLayout) findViewById(R.id.sentences);
                            child.setText(s.getTextToShow());
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);


                            child.setLayoutParams(params);
                            ArrayList<TextView> items = new ArrayList<>();

                            items.add(child);
                            for (int i = parent.getChildCount() - 1; i >= 0; i--) {
                                TextView t = (TextView) parent.getChildAt(i);
                                parent.removeViewAt(i);
                                items.add(t);
                            }
                            for (TextView t : items) {
                                parent.addView(t);
                            }
                            ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_UP);

                        }
                    });

                    //speak
                    Bundle b = new Bundle();
                    b.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, s.get_id().toString());


                    switch (s.getFunctionId()) {

                        case 1://The device is to speak (tts) the text_to_read (used to give instructions about the exercises)


                            TTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                                @Override
                                public void onStart(String utteranceId) {

                                }

                                @Override
                                public void onDone(String utteranceId) {
                                    if (current.getCurrentScriptEntry().getFunctionId() == 1) {
                                        current.setShouldRunScript(true);
                                        current.selectNextScript();
                                        runScriptEntry();

                                    }
                                }

                                @Override
                                public void onError(String utteranceId) {

                                }
                            });
                            TTS.speak(s.getTextToRead(), TextToSpeech.QUEUE_FLUSH, null, s.get_id().toString());

                            break;

                        case 2:
                            //The device is to Read text(tts), Show sentence- tts, Listen to speech, Check against database info= stt. Listen and compare.

                            TTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                                @Override
                                public void onStart(String utteranceId) {
                                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(10);
                                }

                                @Override
                                public void onDone(String utteranceId) {
                                    promptSpeechInput();//shows mic screen
                                    //if voice recognition fails, ask again. no touching button
                                }

                                @Override
                                public void onError(String utteranceId) {

                                }
                            });
                            TTS.speak(s.getTextToRead(), TextToSpeech.QUEUE_FLUSH, null, s.get_id().toString());

                            break;

                        case 3:
                        //only checks the speech -> do not provide any kind of model
                        // (neither spoken by the device nor on video)
                            promptSpeechInput();
                            break;
                        case 4:
                        //shows video and asks for audio input then checks audio
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    final VideoView v = new VideoView(PracticeActivity.this);
                                    final RelativeLayout r = (RelativeLayout) findViewById(R.id.container_practice);
                                    r.addView(v);
                                    int videoResource = getResources().getIdentifier("raw/" + s.getTextToRead(), null, getPackageName());

                                    String path = "android.resource://" + getPackageName() + "/" + videoResource;
                                    v.setVideoURI(Uri.parse(path));
                                    v.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                        @Override
                                        public void onCompletion(MediaPlayer mp) {
                                            r.removeView(v);
                                            promptSpeechInput();
                                        }
                                    });
                                    v.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                        @Override
                                        public void onPrepared(MediaPlayer mp) {

                                            v.start();
                                        }
                                    });
                                }
                            });

                            break;
                        case 5://only shows a video containing instructions. do not ask for audio back
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    final VideoView v = new VideoView(PracticeActivity.this);
                                    final RelativeLayout r = (RelativeLayout) findViewById(R.id.container_practice);
                                    r.addView(v);
                                    int videoResource = getResources().getIdentifier("raw/" + s.getTextToRead(), null, getPackageName());

                                    String path = "android.resource://" + getPackageName() + "/" + videoResource;
                                    v.setVideoURI(Uri.parse(path));
                                    v.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                        @Override
                                        public void onCompletion(MediaPlayer mp) {
                                            r.removeView(v);
                                            current.setShouldRunScript(true);
                                            current.selectNextScript();
                                            runScriptEntry();
                                        }
                                    });
                                    v.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                        @Override
                                        public void onPrepared(MediaPlayer mp) {

                                            v.start();
                                        }
                                    });
                                }
                            });

                            break;
                    }

                }

                current.setShouldRunScript(false);//prove to me again that I can execute everything ->go to the next exercise.

            } else { //exercise completed
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);


                if (exercises.size() > sharedPreferences.getInt("exercise_count", 0)) {

                    Intent i = new Intent(PracticeActivity.this, TransitionActivity.class);
                    startActivity(i);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

                    current.setShouldRunScript(true);
                } else {
                    Intent i = new Intent(PracticeActivity.this, LessonCompletedActivity.class);
                    startActivity(i);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                }
                finish();
            }
        }
    }

    /**
     * Showing google speech input dialog
     */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                current.getCurrentScriptEntry().getTextToShow());
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                current.setShouldRunScript(true);
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Boolean hit = false;
                    for (String r : result) {
                        hit = current.getCurrentScriptEntry().getTextToCheck().toLowerCase().replaceAll("[^a-zA-Z0-9]", "")
                                .equals(r.toLowerCase().replaceAll("[^a-zA-Z0-9]", ""));
                        if (hit) {
                            break;
                        }
                    }

                    if (hit) {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(PracticeActivity.this);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("correct_sentence_count", sharedPreferences.getInt("correct_sentence_count", 0) + 1);
                        editor.commit();
                        if (current.hasMoreScripts()) {
                            current.selectNextScript();
                        }

                    }

                }
                break;
            }
        }
        runScriptEntry();//user should not stop in the middle of the lesson.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TTS.shutdown();
    }

    public void checkConnection() {
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if (activeNetwork == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setMessage(R.string.no_connection);
            builder.setTitle(R.string.no_connection_title);
            builder.setPositiveButton(R.string.action_settings, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    return;
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    return;
                }
            });

            builder.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_practice, menu);
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

    public Lesson updateTitleWithLessonName(Integer lessonId) {
        Lesson l = null;
        try {
            InputStream is = getAssets()
                    .open(DBHandler.DATABASE_NAME);
            db = new DBHandler(PracticeActivity.this, is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (db != null) {
            l = db.findLesson(lessonId);
            setTitle(l.getName());
        }
        return l;
    }
}
