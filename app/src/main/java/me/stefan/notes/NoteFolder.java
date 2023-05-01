package me.stefan.notes;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NoteFolder implements NoteListviewItem{
    public int id;
    public String name;
    public List<Note> notes = new ArrayList<>();
    public static final transient int ICON = R.drawable.folder;

    public NoteFolder() {}

    public NoteFolder(String name) {
        this.name = name;
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
        Note nextDueNote = notes.stream().min(Comparator.comparing(a -> a.date)).get();
        return "Next note is due " + nextDueNote.date + " at " + nextDueNote.time;
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
}
