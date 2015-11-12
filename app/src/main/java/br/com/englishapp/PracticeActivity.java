package br.com.englishapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import br.com.englishapp.model.CurrentPracticeData;
import br.com.englishapp.model.DBHandler;
import br.com.englishapp.model.Exercise;
import br.com.englishapp.model.Lesson;
import br.com.englishapp.model.ScriptEntry;

//This class handles the screen that will execute the scripts the user is supposed to practice-> the sentences.

public class PracticeActivity extends ActionBarActivity {


    private static final long TRANSITION_PAUSE = 1000;
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;

    private String LOG_TAG = "PracticeActivity";

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
        if (exercises.size() <= sharedPreferences.getInt("exercise_count", 0)) {
            finish();
        } else {
            Exercise e = exercises.get(sharedPreferences.getInt("exercise_count", 0));
            current.setCurrentExercise(e);
            setContentView(R.layout.activity_practice);

            speech = SpeechRecognizer.createSpeechRecognizer(PracticeActivity.this);
            speech.setRecognitionListener(new CustomSpeechRecognition());
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en_US");
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);


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
    }

    public void startExercise() {
        runScriptEntry();
    }

    /*Whenever tryAgain is called, the function runscriptentry is allowed because the variable shouldRunScript was changed to true*/
    public void tryAgain(View v) {
        current.setShouldRunScript(true);
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

            current.setShouldRunScript(false);//prove to me again that I can execute everything ->go to the next exercise.

            if (current.getCurrentScriptIndex() < current.getCurrentExercise().getScriptEntries().size()) {

                final ScriptEntry s = current.getCurrentScriptEntry();
                if (s != null) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            TextView child = new TextView(PracticeActivity.this);
                            child.setTextSize(20f);

                            LinearLayout parent = (LinearLayout) findViewById(R.id.contentFrame);
                            child.setText(s.getTextToShow());
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);


                            child.setLayoutParams(params);
                            ArrayList<TextView> items = new ArrayList<>();

                            items.add(child);
                            for (int i = parent.getChildCount() - 1; i >= 0; i--) {
                                TextView t = (TextView) parent.getChildAt(i);
                                parent.removeViewAt(i);
                            }
                            parent.addView(child);

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
//                            this method needs a little more time, for the sake of uability, to ask for the nest input. Users were getting confused about the sounds built in the Voice Recognition
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    promptSpeechInput();
                                }
                            }, TRANSITION_PAUSE);
                            break;
                        case 4:
                            //shows video and asks for audio input then checks audio
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    final VideoView v = new VideoView(PracticeActivity.this);
                                    final LinearLayout r = (LinearLayout) findViewById(R.id.videoFrame);
                                    r.setVisibility(View.VISIBLE);
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
                                    final LinearLayout r = (LinearLayout) findViewById(R.id.videoFrame);
                                    r.setVisibility(View.VISIBLE);
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

            } else { //exercise completed
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);


                if (exercises.size() > sharedPreferences.getInt("exercise_count", 0)) {
                    Intent i = new Intent(PracticeActivity.this, TransitionActivity.class);
                    startActivity(i);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

                    current.setShouldRunScript(true);
                } else {
                    Intent i = new Intent(PracticeActivity.this, LessonCompletedActivity.class);

                    //getting the current time in milliseconds, and creating a Date object from it:
                    Date date = new Date(System.currentTimeMillis()); //or simply new Date();

                    //converting it back to a milliseconds representation:
                    long millis = date.getTime();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putLong("finish_time", date.getTime());
                    editor.commit();

                    startActivity(i);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                }
                finish();
            }
        }
    }

    /**
     * Showing google speech input dialog ->starts listening to audio input
     */

    private void promptSpeechInput() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {


                speech.startListening(recognizerIntent);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (TTS != null) {
            TTS.shutdown();
        }
    }


    class CustomSpeechRecognition implements RecognitionListener {
        Boolean beganSpeech = false;

        @Override
        public void onReadyForSpeech(Bundle params) {

            ((ImageButton) findViewById(R.id.mic)).setImageDrawable(getDrawable(R.drawable.mic_0_enabled));

            Log.i(LOG_TAG, "onReadyForSpeech");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.i(LOG_TAG, "onBeginningOfSpeech");
            beganSpeech = true;
        }

        @Override
        public void onRmsChanged(float rmsdB) {

            // Log.i(LOG_TAG, "onRmsChanged");

            if (beganSpeech == true) {
                switch ((int) rmsdB) {
                    case 1:
                    case 2:
                        ((ImageButton) findViewById(R.id.mic)).setImageDrawable(getDrawable(R.drawable.mic_1));
                        break;
                    case 3:
                    case 4:
                        ((ImageButton) findViewById(R.id.mic)).setImageDrawable(getDrawable(R.drawable.mic_3));
                        break;
                    case 5:
                    case 6:
                        ((ImageButton) findViewById(R.id.mic)).setImageDrawable(getDrawable(R.drawable.mic_5));
                        break;
                    case 7:
                    case 8:
                        ((ImageButton) findViewById(R.id.mic)).setImageDrawable(getDrawable(R.drawable.mic_7));
                        break;
                    case 9:
                    case 10:
                        ((ImageButton) findViewById(R.id.mic)).setImageDrawable(getDrawable(R.drawable.mic_10));
                        break;
                    default:
                        ((ImageButton) findViewById(R.id.mic)).setImageDrawable(getDrawable(R.drawable.mic_0_enabled));
                        break;
                }
                beganSpeech = false;
            }
        }

        @Override
        public void onBufferReceived(byte[] buffer) {

            Log.i(LOG_TAG, "onBufferReceived");
        }

        @Override
        public void onEndOfSpeech() {

            ((ImageButton) findViewById(R.id.mic)).setImageDrawable(getDrawable(R.drawable.mic_disabled));
            Log.i(LOG_TAG, "onEndOfSpeech");
            speech.stopListening();
        }

        @Override
        public void onError(int error) {
            Log.i(LOG_TAG, "onError: " + getErrorText(error));
            ((ImageButton) findViewById(R.id.mic)).setImageDrawable(getDrawable(R.drawable.mic_disabled));
            speech.cancel();
        }

        public void updateLastSentences(String sentence) {
            TextView tv1 = ((TextView) findViewById(R.id.recognizedText1));
            TextView tv2 = ((TextView) findViewById(R.id.recognizedText2));
            TextView tv3 = ((TextView) findViewById(R.id.recognizedText3));
            TextView tv4 = ((TextView) findViewById(R.id.recognizedText4));
            TextView tv5 = ((TextView) findViewById(R.id.recognizedText5));
            tv1.setText(tv2.getText());
            tv2.setText(tv3.getText());
            tv3.setText(tv4.getText());
            tv4.setText(tv5.getText());
            tv5.setText(sentence);
        }

        @Override
        public void onResults(Bundle results) {

            ((ImageButton) findViewById(R.id.mic)).setImageDrawable(getDrawable(R.drawable.mic_disabled));
            Log.i(LOG_TAG, "onResults");
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            //just show the first result so that the user gets a sense of where he is wrong
            String recognizedSentence = (matches.size() > 0) ? matches.get(0) : "";
            Boolean hit = false;
            for (String r : matches) {
                hit = current.getCurrentScriptEntry().getTextToCheck().toLowerCase().replaceAll("[^a-zA-Z0-9]", "")
                        .equals(r.toLowerCase().replaceAll("[^a-zA-Z0-9]", ""));
                if (hit) {
                    recognizedSentence = r;
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

            } else {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(PracticeActivity.this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("wrong_sentence_count", sharedPreferences.getInt("wrong_sentence_count", 0) + 1);

                editor.commit();
            }
            updateLastSentences(recognizedSentence);
            current.setShouldRunScript(true);
            runScriptEntry();//user should not stop in the middle of the lesson.
        }

        @Override
        public void onPartialResults(Bundle partialResults) {

            Log.i(LOG_TAG, "onPartialResults");
        }

        @Override
        public void onEvent(int eventType, Bundle params) {

            Log.i(LOG_TAG, "onEvent");
        }

        public String getErrorText(int errorCode) {
            String message = errorCode + "";
            switch (errorCode) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message += "Audio recording error";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message += "Client side error";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message += "Insufficient permissions";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message += "Network error";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message += "Network timeout";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message += "No match";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message += "RecognitionService busy";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message += "error from server";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message += "No speech input";
                    break;
                default:
                    message += "Didn't understand, please try again.";
                    break;
            }
            return message;
        }

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
}
