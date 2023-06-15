package me.stefan.notes.backend;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import me.stefan.notes.Note;


public class Backend {
    private String username;
    private String password;


    private Backend(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public static void login(String username, String password, Consumer<Backend>... onLogin){
        ConnectionHandler.get("https://www.docsced.at/notesserver/todolists.php?username=" + username + "&password=" + password, (response) -> {
            Backend backend = new Backend(username, password);
            Arrays.stream(onLogin).forEach((cons) -> cons.accept(backend));
        });
    }

    public static void register(String name, String username, String password, Consumer<Backend>... onLogin){
        String json = String.format("{\"username\": \"%s\", \"password\": \"%s\", \"name\": \"%s\"}", username, password, name);

        ConnectionHandler.get("https://www.docsced.at/notesserver/todolists.php?username=" + username + "&password=" + password, (response) -> {
            Backend backend = new Backend(username, password);
            Arrays.stream(onLogin).forEach((cons) -> cons.accept(backend));
        });
    }


    public void uploadNotes(List<Note> notes){


            String json = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", username, password);
            ConnectionHandler.post("https://www.docsced.at/notesserver/register.php", json, (response) -> {
            });
    }

    private void downloadNotes(List<Note> notes, Consumer<List<Note>>... onDownload){
        ConnectionHandler.get("https://www.docsced.at/notesserver/todolists.php?username=" + username + "&password=" + password, (response) -> {
            Backend backend = new Backend(username, password);
            Arrays.stream(onDownload).forEach((cons) -> cons.accept());
        });
    }
}
