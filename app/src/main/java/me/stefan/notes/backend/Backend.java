package me.stefan.notes.backend;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import me.stefan.notes.Note;
import me.stefan.notes.NoteFolder;
import me.stefan.notes.backend.dto.TodoDTO;
import me.stefan.notes.backend.dto.TodoListDTO;


public class Backend {
    private String username;
    private String password;
    private Runnable onFailure;


    private Backend(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public static void login(String username, String password, Consumer<Backend> onSuccess, Runnable onFailure) {

        ConnectionHandler.get("https://www.docsced.at/notesserver/todolists.php?username=" + username + "&password=" + password, (response) -> {
            Backend backend = new Backend(username, password);
            backend.onFailure = onFailure;
            onSuccess.accept(backend);
        });
    }

    public static void register(String name, String username, String password, Consumer<Backend> onSuccess, Runnable onFailure) {
        String json = String.format("{\"username\": \"%s\", \"password\": \"%s\", \"name\": \"%s\"}", username, password, name);

        ConnectionHandler.get("https://www.docsced.at/notesserver/todolists.php?username=" + username + "&password=" + password, (response) -> {
            Backend backend = new Backend(username, password);
            backend.onFailure = onFailure;
            onSuccess.accept(backend);
        });
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void uploadNotes(List<NoteFolder> notes, Runnable onSuccess, Runnable onFailure) {

        ConnectionHandler.get("https://www.docsced.at/notesserver/todolists.php?username=" + username + "&password=" + password, (todoListsJson) -> {
            List<TodoListDTO> listDTOs = Arrays.stream(new Gson().fromJson(todoListsJson, TodoListDTO[].class)).collect(Collectors.toList());
            ConnectionHandler.get("https://www.docsced.at/notesserver/todo.php?username=" + username + "&password=" + password, (todosJson) -> {
                List<TodoDTO> todoDTOs = Arrays.stream(new Gson().fromJson(todosJson, TodoDTO[].class)).collect(Collectors.toList());
                notes.forEach(list -> {
                    String listJson = String.format("{\"name\": \"%s\", \"additionalData\": \"\"}", list.name);
                    if (list.id == -1 || listDTOs.stream().noneMatch(listDTO -> Integer.parseInt(listDTO.id) == list.id)) {
                        if (list.queueDeletion) {
                            return;
                        }
                        ConnectionHandler.post("https://www.docsced.at/notesserver/todolists.php?username=" + username + "&password=" + password, listJson, (todoListJson) -> {
                            list.id = Integer.parseInt(new Gson().fromJson(todoListJson, TodoListDTO.class).id);
                            uploadNotes(todoDTOs, list);
                        });
                    } else {
                        if (list.queueDeletion) {
                            ConnectionHandler.delete("https://www.docsced.at/notesserver/todolists.php?id=" + list.id + "&username=" + username + "&password=" + password, null);
                            return;
                        }
                        ConnectionHandler.put("https://www.docsced.at/notesserver/todolists.php?id=" + list.id + "&username=" + username + "&password=" + password, listJson, (result) -> {
                            uploadNotes(todoDTOs, list);
                        });
                    }

                });
            });


        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void uploadNotes(List<TodoDTO> todoDTOs, NoteFolder list) {
        list.notes.forEach(note -> {

            String noteJson = String.format("{\"todoListId\": \"%s\", \"title\": \"%s\", \"description\": \"NAN\", \"dueDate\": \"%s\", \"state\": \"%s\", \"additionalData\": \"NAN\"}",
                    list.id, note.note, note.getDateTimeString(), note.done ? "CLOSED" : "OPEN");

            if (note.id == -1 || todoDTOs.stream().noneMatch(todoDTO -> Integer.parseInt(todoDTO.id) == note.id)) {
                if (note.queueDeletion) {
                    return;
                }
                ConnectionHandler.post("https://www.docsced.at/notesserver/todo.php?username=" + username + "&password=" + password, noteJson, (todoJson) -> {
                    note.id = Integer.parseInt(new Gson().fromJson(todoJson, TodoDTO.class).id);
                });
            }else {
                if (note.queueDeletion) {
                    ConnectionHandler.delete("https://www.docsced.at/notesserver/todo.php?id=" + note.id + "&username=" + username + "&password=" + password, null);
                    return;
                }
                ConnectionHandler.put("https://www.docsced.at/notesserver/todo.php?id=" + note.id + "&username=" + username + "&password=" + password, noteJson, null);
            }


        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void downloadNotes(Consumer<List<NoteFolder>> onDownloaded) {
        ConnectionHandler.get("https://www.docsced.at/notesserver/todolists.php?username=" + username + "&password=" + password, (response) -> {
            if (response == null ){
                onFailure.run();
                return;
            }
            List<TodoListDTO> listDTOs = Arrays.stream(new Gson().fromJson(response, TodoListDTO[].class)).collect(Collectors.toList());
            ConnectionHandler.get("https://www.docsced.at/notesserver/todo.php?username=" + username + "&password=" + password, (response2) -> {
                if (response2 == null ) {
                    onFailure.run();
                    return;
                }
                List<TodoDTO> todoDTOs = Arrays.stream(new Gson().fromJson(response2, TodoDTO[].class)).collect(Collectors.toList());
                List<NoteFolder> notes = listDTOs.stream().map(list -> {
                    NoteFolder folder = new NoteFolder(list.name);
                    folder.id = Integer.parseInt(list.id);
                    folder.notes = todoDTOs.stream()
                            .filter(todo -> Integer.parseInt(todo.todoListId) == folder.id).map(todo -> {
                                Note note = new Note();
                                note.id = Integer.parseInt(todo.id);
                                note.note = todo.title;
                                note.setDate(LocalDate.parse(todo.dueDate.split(" ")[0]));
                                note.setTime(LocalTime.parse(todo.dueDate.split(" ")[1]));
                                note.done = todo.state.equals("CLOSED");
                                return note;
                            }).collect(Collectors.toList());
                    return folder;
                }).collect(Collectors.toList());
                onDownloaded.accept(notes);
            });
        });

    }
}
