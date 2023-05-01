package me.stefan.notes.backend;

import java.util.List;

import me.stefan.notes.Note;


public class Backend {
    private int userid;
    private String username = "";
    private String password = "";
    private String name = "";


    private Backend(Backend backend, int userid, String username, String password) {
        this.userid = userid;
        this.username = username;
        this.password = password;
    }

    public static Backend login(String username, String password, Runnable onLogin){
        Backend backend = null;
        ConnectionHandler.get("https://www.docsced.at/notesserver/todolists.php?username=" + username + "&password=" + password, (response) -> {

            onLogin.run();
        });
        return null;
    }

    public static Backend register(String name, String username, String password, Runnable onLogin){
        String json = String.format("{\"username\": \"%s\", \"password\": \"%s\", \"name\": \"%s\"}", username, password, name);
        //{"username": "Stefan", "password": "wiesingers190147", "name": "Stefan Wiesinger"}

        return null;
    }


    public void sync(List<Note> notes){
        if (userid == -1){


            String json = String.format("{\"username\": \"%s\", \"password\": \"%s\", \"name\": \"%s\"}", username, password, name);
            ConnectionHandler.post("https://www.docsced.at/notesserver/register.php", json, (response) -> {
                if (response != null){
                    userid = Integer.parseInt(response);
                }
            });

        }
    }

    private void downloadNotes(List<Note> notes){

    }
}
