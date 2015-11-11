package br.com.englishapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

import br.com.englishapp.R;
import br.com.englishapp.TransitionActivity;
import br.com.englishapp.model.Lesson;

/**
 * Created by karina on 15-09-30.
 */
public class LessonAdapter extends ArrayAdapter<Lesson> {
    private final LayoutInflater inflater;

    private final int resourceId;
    private Context context;

    public LessonAdapter(Context context, int resource,
                         List<Lesson> objects) {
        super(context, resource, objects);
        this.inflater = LayoutInflater.from(context);
        this.resourceId = resource;
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        view = inflater.inflate(resourceId, parent, false);

        TextView lessonName;
        lessonName = (TextView) view.findViewById(R.id.lessonName);
        Lesson obj = getItem(position);
        final Lesson l = obj;
        lessonName.setText(obj.getName());
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Integer lastCompletedLesson = sharedPreferences.getInt("last_lesson_completed_id", 0);
        if (obj.get_id() <= lastCompletedLesson+1) {
        //the next lesson must be unlocked as well.
            ((ImageView) view.findViewById(R.id.imgPadLock)).setImageDrawable(context.getDrawable(R.drawable.padlock_unlock));
            if (obj.get_id() <= lastCompletedLesson) {
            //completed lessons display a check mark
                ((ImageView) view.findViewById(R.id.imgPadLock)).setImageDrawable(context.getDrawable(R.drawable.checkmark          ));
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(context, TransitionActivity.class);

                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor editor = sharedPreferences.edit();

                    Date date = new Date();

                    //converting it back to a milliseconds representation:
                    long millis = date.getTime();

                    editor.putInt("exercise_count", 0);
                    editor.putInt("correct_sentence_count", 0);
                    editor.putInt("wrong_sentence_count", 0);
                    editor.putLong("start_time", date.getTime());
                    editor.putInt("lesson_id", l.get_id());
                    editor.commit();
                    context.startActivity(i);
                }
            });
        } else {
            ((ImageView) view.findViewById(R.id.imgPadLock)).setImageDrawable(context.getDrawable(R.drawable.padlock_lock));

        }
        return view;
    }
}
