package me.stefan.notes;

public interface NoteListviewItem {
    public int id();
    public String title();
    public String description();
    public boolean isOverdue();
    public boolean isDone();
    public void toggleDone();
    public int getIcon();
    public boolean queueDeletion();
    public void setQueueDeletion(boolean b);
}
