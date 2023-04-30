package me.stefan.notes;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.LocalDate;
import java.time.LocalTime;

public class Note {
    public final int id;
    public String note;
    public LocalDate date;
    public LocalTime time;
    public boolean done = false;

    public Note(String note, LocalDate date, LocalTime time, boolean done) {
        this.id = (int)(Integer.MAX_VALUE*Math.random());
        this.note = note;
        this.date = date;
        this.time = time;
        this.done = done;

    }


    @Override
    public String toString() {
        return date + "," + time + "," + note + "," + done;
    }
}
