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
    public int id = -1;
    public String note;
    private String date;
    public String time;
    public boolean done = false;
    public boolean queueDeletion = false;

    public Note() {}

    public Note(String note, LocalDate date, LocalTime time, boolean done) {
        this.note = note;
        this.date = date.format(DATE_FORMATTER);
        this.time = time.format(TIME_FORMATTER);
        this.done = done;

    }

    @Override
    public int id() {
        return id;
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
    public boolean queueDeletion() {
        return queueDeletion;
    }


    @Override
    public void setQueueDeletion(boolean b) {
        queueDeletion = b;
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

    public String getDateString() {
        return date;
    }

    public String getTimeString() {
        return time;
    }

    public String getDateTimeString() {
        return date + " " + time;
    }

    @Override
    public String toString() {
        return date + "," + time + "," + note + "," + done;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Note note1 = (Note) o;

        if (id != note1.id) return false;
        if (done != note1.done) return false;
        if (note != null ? !note.equals(note1.note) : note1.note != null) return false;
        if (date != null ? !date.equals(note1.date) : note1.date != null) return false;
        return time != null ? time.equals(note1.time) : note1.time == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (note != null ? note.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        result = 31 * result + (done ? 1 : 0);
        return result;
    }
}
