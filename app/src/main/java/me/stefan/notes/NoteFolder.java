package me.stefan.notes;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NoteFolder implements NoteListviewItem{
    public int id;
    public String name;
    public List<Note> notes = new ArrayList<>();
    public static final transient int ICON = R.drawable.folder;
    public boolean queueDeletion;

    public NoteFolder() {}

    public NoteFolder(String name) {
        this.name = name;
    }

    @Override
    public int id() {
        return id;
    }
    @Override
    public String title() {
        return name;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public String description() {
        if (notes.isEmpty())
            return "No notes";
        Note nextDueNote = notes.stream().min(Comparator.comparing(a -> LocalDateTime.of(a.getDate(), a.getTime()))).get();
        return "Next note is due " + nextDueNote.getDateString() + " at " + nextDueNote.time;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean isOverdue() {
        if (notes.isEmpty())
            return false;
        return notes.stream().anyMatch(Note::isOverdue);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean isDone() {
        if (notes.isEmpty())
            return false;
        return notes.stream().allMatch(Note::isDone);
    }

    @Override
    public int getIcon() {
        return ICON;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void toggleDone() {
        boolean isDone = isDone();
        notes.stream().filter(note -> note.isDone() == isDone).forEach(Note::toggleDone);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NoteFolder that = (NoteFolder) o;

        if (id != that.id) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return notes != null ? notes.equals(that.notes) : that.notes == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (notes != null ? notes.hashCode() : 0);
        return result;
    }
}
