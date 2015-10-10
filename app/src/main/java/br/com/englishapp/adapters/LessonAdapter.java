package br.com.englishapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import br.com.englishapp.R;
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
        lessonName.setText(obj.getName());

        return view;
    }
}
