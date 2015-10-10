package br.com.englishapp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Karina Nishimura on 15-09-30.
 */
public class Exercise {
    private Integer _id;
    private String name;
    private Lesson Lesson; //belongs to one lesson
    private ArrayList<ScriptEntry> scriptEntries;//has many script entries

    public Exercise(Integer id, String name, Lesson lesson) {
        _id = id;
        this.name = name;
        Lesson = lesson;
    }

    public Exercise() {

    }

    public Integer get_id() {
        return _id;
    }

    public void set_id(Integer _id) {
        this._id = _id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public br.com.englishapp.model.Lesson getLesson() {
        return Lesson;
    }

    public void setLesson(br.com.englishapp.model.Lesson lesson) {
        Lesson = lesson;
    }

    public ArrayList<ScriptEntry> getScriptEntries() {
        return scriptEntries;
    }

    public void setScriptEntries(ArrayList<ScriptEntry> scriptEntries) {
        this.scriptEntries = scriptEntries;
    }
}
