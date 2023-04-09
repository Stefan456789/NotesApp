package me.stefan.notes;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.LocalDate;
import java.time.LocalTime;

public class Note {
    public String note;
    public LocalDate date;
    public LocalTime time;

    public Note(String note, LocalDate date, LocalTime time) {
        this.note = note;
        this.date = date;
        this.time = time;
    }

    @Override
    public String toString() {
        return date + "," + time + "," + note;
    }
}
