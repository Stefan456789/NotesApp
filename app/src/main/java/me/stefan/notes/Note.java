package me.stefan.notes;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@RequiresApi(api = Build.VERSION_CODES.O)
public class Note implements NoteListviewItem {
    public static final transient DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final transient DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    public static final transient int ICON = R.drawable.note;
    public int id;
    public String note;
    public String date;
    public String time;
    public boolean done = false;

    public Note() {}

    public Note(String note, LocalDate date, LocalTime time, boolean done) {
        this.id = (int)(Integer.MAX_VALUE*Math.random());
        this.note = note;
        this.date = date.format(DATE_FORMATTER);
        this.time = time.format(TIME_FORMATTER);
        this.done = done;

    }

    @Override
    public String title() {
        return note;
    }

    @Override
    public String description() {
        return String.format("Due %s at %s", date, time);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean isOverdue() {
        return LocalDateTime.now().isAfter(LocalDateTime.of(getDate(), getTime()));
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public int getIcon() {
        return ICON;
    }

    @Override
    public void toggleDone() {
        done = !done;
    }

    public LocalDate getDate() {
        return LocalDate.parse(date, DATE_FORMATTER);
    }

    public LocalTime getTime() {
        return LocalTime.parse(time, TIME_FORMATTER);
    }

    public void setDate(LocalDate date) {
        this.date = date.format(DATE_FORMATTER);
    }

    public void setTime(LocalTime time) {
        this.time = time.format(TIME_FORMATTER);
    }

    @Override
    public String toString() {
        return date + "," + time + "," + note + "," + done;
    }
}
