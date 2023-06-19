package me.stefan.notes;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

import me.stefan.notes.backend.Backend;

public class MainActivity extends AppCompatActivity {
    public SharedPreferences prefs ;
    public static final List<NoteFolder> notes = new ArrayList<>();
    private static final String FILE_NAME = "save.json";
    private static final int REQUESTCODE_SETTINGS = 1;
    private static final int REQUESTCODE_EXTERNAL_STORAGE = 2;
    public static final int REQUESTCODE_ALARM = 3;
    public static NotesAdapter adapter;
    public ListView list;
    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangeListener;
    public boolean tryOnlineSync = false;
    public Backend backend = null;
    private int selectedFolder = -1;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUpSharedPreferences();


        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(getExternalFilePath(FILE_NAME))));
            notes.addAll(new Gson().fromJson(r.lines().collect(Collectors.joining()), new TypeToken<ArrayList<NoteFolder>>() {}));
        } catch (IOException | NullPointerException e ) {
            Toast.makeText(this, "Es wurden keine bestehende Notizen gefunden!", Toast.LENGTH_SHORT).show();
        }
        list = findViewById(R.id.list);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Object noteItem = adapterView.getItemAtPosition(i);

                if (noteItem instanceof Note) {
                    showNoteDetails((Note) noteItem);
                } else if (noteItem instanceof NoteFolder) {
                    selectedFolder = i;
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    applyFilter();
                }
            }
        });

        adapter = new NotesAdapter(this, notes);
        list.setAdapter(adapter);


        registerForContextMenu(list);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onSupportNavigateUp() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        selectedFolder = -1;
        applyFilter();
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setUpSharedPreferences() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        preferencesChangeListener = this::onPreferenceChanged;
        prefs.registerOnSharedPreferenceChangeListener( preferencesChangeListener );
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void onPreferenceChanged(SharedPreferences sharedPrefs, String key) {

        applyFilter(sharedPrefs);
        list.invalidateViews();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void applyFilter(SharedPreferences sharedPrefs) {
        adapter.getFilter().filter( selectedFolder + "&" + sharedPrefs.getBoolean("showOverdueNotes", true));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void applyFilter() {
        applyFilter(prefs);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.list) {
            getMenuInflater().inflate(R.menu.menu_context, menu);
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (info != null){
            Object noteItem = adapter.getItem(info.position);
            switch (item.getItemId()){
                case R.id.edit:
                    if (noteItem instanceof NoteFolder)
                        openFolderEditor(this, (NoteFolder) noteItem);
                    else if (noteItem instanceof Note)
                        openNoteEditor(this, (Note) noteItem);
                    break;
                case R.id.details:
                    if (noteItem instanceof NoteFolder)
                        showFolderDetails((NoteFolder) noteItem);
                    else if (noteItem instanceof Note)
                        showNoteDetails((Note) noteItem);
                    break;
                case R.id.delete:
                    if (((NoteListviewItem) noteItem).id() == -1)
                        notes.remove(noteItem);
                    else
                        ((NoteListviewItem) noteItem).setQueueDeletion(true);
                    applyFilter();
                    adapter.notifyDataSetChanged();
                    break;

                case R.id.toggleDone:
                    ((NoteListviewItem)noteItem).toggleDone();
                    applyFilter();
                    adapter.notifyDataSetChanged();
                    list.invalidateViews();
                    break;
            }

        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showFolderDetails(NoteFolder n) {
            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            float density = this.getResources().getDisplayMetrics().density;
            root.setPadding((int) (20*density), (int) (5*density), (int) (20*density),0);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            TextView note = new TextView(this);
            note.setText("Title: " + n.name);
            TextView date = new TextView(this);
            date.setText("Number of notes: " + n.notes.size());
            TextView time = new TextView(this);
            time.setText(n.description());
            root.addView(note, params);
            root.addView(date, params);
            root.addView(time, params);
            new AlertDialog.Builder(this)
                    .setView(root)
                    .setNegativeButton("Cancel", null)
                    .setTitle("Details")
                    .show();

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showNoteDetails(Note n) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        float density = this.getResources().getDisplayMetrics().density;
        root.setPadding((int) (20*density), (int) (5*density), (int) (20*density),0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        TextView note = new TextView(this);
        note.setText("Note: " + n.note);
        TextView date = new TextView(this);
        date.setText("Date: " + n.getDateString());
        TextView time = new TextView(this);
        time.setText("Time: " + n.time);
        root.addView(note, params);
        root.addView(date, params);
        root.addView(time, params);
        new AlertDialog.Builder(this)
                .setView(root)
                .setNegativeButton("Cancel", null)
                .setTitle("Details")
                .show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (backend == null && prefs.getBoolean("tryLogin", true))
            login(menu.findItem(R.id.syncNote), prefs.getString("username", null), prefs.getString("password", null));
        return super.onCreateOptionsMenu(menu);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId() ;
        switch (id ) {
            case R.id.newNote:
                if (selectedFolder == -1)
                    openFolderEditor(this, null);
                else
                    openNoteEditor(this, null);
                break;
            case R.id.saveNote:
                if (tryOnlineSync){
                    if (backend == null ){
                        tryOnlineSync = false;
                        item.setIcon(ContextCompat.getDrawable(this, R.drawable.cloudsync));
                        Toast.makeText(this, "You are not logged in!", Toast.LENGTH_LONG).show();
                    } else
                    if (!isNetworkAvailable()){
                        tryOnlineSync = false;
                        item.setIcon(ContextCompat.getDrawable(this, R.drawable.cloudsync));
                        Toast.makeText(this, "No internet connection!", Toast.LENGTH_LONG).show();
                        break;
                    }else{
                    backend.uploadNotes(notes, () -> {
                        Toast.makeText(this, "Notes uploaded successfully!", Toast.LENGTH_LONG).show();
                    }, () -> {
                        Toast.makeText(this, "Notes upload failed!", Toast.LENGTH_LONG).show();
                    });

                    }
                }
                try {
                    OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(getExternalFilePath(FILE_NAME)));
                    w.write(new Gson().toJson(notes));
                    w.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.settings:
                Intent intent = new Intent(this,
                        SettingsActivity.class);
                startActivityForResult(intent, REQUESTCODE_SETTINGS);

                break;
            case R.id.syncNote:
                if (!isNetworkAvailable()){
                    item.setIcon(ContextCompat.getDrawable(this, R.drawable.cloudsync));
                    Toast.makeText(this, "No internet connection!", Toast.LENGTH_LONG).show();
                    break;
                }

                if (!tryOnlineSync){

                    new AlertDialog.Builder(this)
                            .setTitle("You are not logged in!")
                            .setMessage("Do you want to register or login?")
                            .setNeutralButton("Cancel", null)
                            .setPositiveButton("Login", (dialogInterface, i) -> {
                                View root = getLayoutInflater().inflate(R.layout.dialog_login, null);
                                new AlertDialog.Builder(this)
                                        .setTitle("Login")
                                        .setNegativeButton("Cancel", null)
                                        .setPositiveButton("Login", (dialogInterface1, i1) -> {
                                            String username = ((EditText)root.findViewById(R.id.loginUsername)).getText().toString();
                                            String password = ((EditText)root.findViewById(R.id.loginPassword)).getText().toString();
                                            item.setIcon(ContextCompat.getDrawable(this, R.drawable.cloudsyncblue));
                                            login(item, username, password);
                                        })
                                        .setView(root).show();
                            })
                            .setNegativeButton("Register", (x, y) -> {
                                View root = getLayoutInflater().inflate(R.layout.dialog_register, null);
                                new AlertDialog.Builder(this)
                                        .setTitle("Register")
                                        .setNegativeButton("Cancel", null)
                                        .setPositiveButton("Login", (dialogInterface1, i1) -> {
                                            String name = ((EditText)root.findViewById(R.id.registerName)).getText().toString();
                                            String username = ((EditText)root.findViewById(R.id.registerUsername)).getText().toString();
                                            String password = ((EditText)root.findViewById(R.id.registerPassword)).getText().toString();

                                            if (name.isEmpty() || username.isEmpty() || password.isEmpty() || (name+username+password).matches(".*[^A-Za-z0-9]+.*")){
                                                Toast.makeText(this, "Please fill all fields and only use numbers and letters!", Toast.LENGTH_LONG).show();
                                                return;
                                            }


                                            item.setIcon(ContextCompat.getDrawable(this, R.drawable.cloudsyncblue));
                                            Backend.register(name, username, password,
                                                    (newBackend)->{
                                                        prefs.edit().putString("username", username).apply();
                                                        prefs.edit().putString("password", password).apply();
                                                        item.setIcon(ContextCompat.getDrawable(this, R.drawable.cloudsyncgreen));
                                                        backend = newBackend;
                                                        backend.downloadNotes((newNotes)->{
                                                            List<NoteFolder> f = newNotes.stream().filter(newNote -> notes.stream().noneMatch(oldNote -> oldNote.equals(newNote))).collect(Collectors.toList());
                                                            notes.removeIf(note -> note.id != -1 && !newNotes.contains(note));
                                                            notes.addAll(f);

                                                            applyFilter();
                                                            adapter.notifyDataSetChanged();
                                                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show();
                                                            tryOnlineSync = true;
                                                        });
                                                    }, () -> {
                                                        backend = null;
                                                        item.setIcon(ContextCompat.getDrawable(this, R.drawable.cloudsync));
                                                        Toast.makeText(this, "Registration failed!", Toast.LENGTH_LONG).show();
                                                    });

                                        })
                                        .setView(root)
                                        .show();
                            })
                            .show();
                } else {
                    tryOnlineSync = false;
                    backend = null;
                    item.setIcon(ContextCompat.getDrawable(this, R.drawable.cloudsync));
                }

                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void login(MenuItem item, String username, String password) {
        Backend.login(
                username,
                password,
                (newBackend)->{
                    tryOnlineSync = true;
                    prefs.edit().putString("username", username).apply();
                    prefs.edit().putString("password", password).apply();
                    item.setIcon(ContextCompat.getDrawable(this, R.drawable.cloudsyncgreen));
                    backend = newBackend;
                    backend.downloadNotes((newNotes)->{
                        List<NoteFolder> f = newNotes.stream().filter(x -> notes.stream().noneMatch(y -> y.equals(x))).collect(Collectors.toList());
                        notes.removeIf(note -> note.id != -1 && !newNotes.contains(note));
                        notes.addAll(f);

                        applyFilter();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_LONG).show();
                        tryOnlineSync = true;
                    });
                }, () -> {
                    backend = null;
                    item.setIcon(ContextCompat.getDrawable(this, R.drawable.cloudsync));
                    Toast.makeText(this, "Login failed!", Toast.LENGTH_LONG).show();
                });
    }

    private String getExternalFilePath(String fileName) {


        String state = Environment.getExternalStorageState();
        if (! state . equals(Environment.MEDIA_MOUNTED)) return null;
        File outFile = getExternalFilesDir ( null ) ;
        String path = outFile.getAbsolutePath();

        return path + File.separator + fileName;
    }


    private void openFolderEditor(Context context, NoteFolder n) {
        View root = getLayoutInflater().inflate(R.layout.dialog_add_folder, null);
        if (n != null){
            ((EditText)root.findViewById(R.id.editTextTitle)).setText(n.name);
        }

        new AlertDialog.Builder(this)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditText note = root.findViewById(R.id.editTextTitle);

                        if (note.getText().toString().isEmpty()) {
                            Toast.makeText(context, "All fields must be filled!", Toast.LENGTH_LONG).show();
                            return;
                        }

                        if (n != null){
                            n.name = note.getText().toString();
                        } else {
                            NoteFolder newNote = new NoteFolder(note.getText().toString());
                            notes.add(newNote);
                        }
                        applyFilter();
                        adapter.notifyDataSetChanged();

                    }
                })
                .setNegativeButton("Cancle", null)
                .setView(root)
                .setTitle("Neuer Ordner")
                .show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void openNoteEditor(Context context, Note n) {
        View root = getLayoutInflater().inflate(R.layout.dialog_add_note, null);
        if (n != null){
            ((EditText)root.findViewById(R.id.editTextNote)).setText(n.note);
            ((EditText)root.findViewById(R.id.editTextDate)).setText(n.getDateString());
            ((EditText)root.findViewById(R.id.editTextTime)).setText(n.time);
        }

        root.findViewById(R.id.showDatePicker).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                DatePickerDialog dialog = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {

                        EditText edit = root.findViewById(R.id.editTextDate);
                        String year = "" + datePicker.getYear();
                        String month = "" + (datePicker.getMonth()+1);
                        String day = "" + datePicker.getDayOfMonth();
                        month = month.length() == 1 ? "0" + month : month;
                        day = day.length() == 1 ? "0" + day : day;
                        edit.setText(year + "-" + month + "-" + day);
                    }
                }, LocalDate.now().getYear(), LocalDate.now().getMonth().getValue()-1, LocalDate.now().getDayOfMonth());
                dialog.show();

            }
        });

        root.findViewById(R.id.showTimePicker).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int i, int i1) {
                        EditText edit = root.findViewById(R.id.editTextTime);
                        String min = "" + timePicker.getMinute();
                        min = min.length() == 1 ? "0" + min : min;
                        String h = "" + timePicker.getHour();
                        h = h.length() == 1 ? "0" + h : h;
                        edit.setText(h + ":" + min);
                    }
                }, LocalTime.now().getHour(), LocalTime.now().getMinute(), true).show();

            }
        });

        new AlertDialog.Builder(this)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditText note = root.findViewById(R.id.editTextNote);
                        EditText date = root.findViewById(R.id.editTextDate);
                        EditText time = root.findViewById(R.id.editTextTime);

                        if (note.getText().toString().isEmpty() || date.getText().toString().isEmpty() || time.getText().toString().isEmpty()) {
                            Toast.makeText(context, "All fields must be filled!", Toast.LENGTH_LONG).show();
                            return;
                        }

                        if (n != null){
                            n.note = note.getText().toString();
                            n.setDate(LocalDate.parse(date.getText().toString()));
                            n.setTime(LocalTime.parse(time.getText().toString()));
                        } else {
                            try {
                                Note newNote = new Note(note.getText().toString(), LocalDate.parse(date.getText().toString()), LocalTime.parse(time.getText().toString()), false);
                                notes.get(selectedFolder).notes.add(newNote);
                            } catch (Exception ex){
                                Toast.makeText(context, "Please enter a valid date and time!", Toast.LENGTH_LONG).show();
                            }
                        }
                        applyFilter();
                        adapter.notifyDataSetChanged();

                    }
                })
                .setNegativeButton("Cancle", null)
                .setView(root)
                .setTitle("Neue Notiz")
                .show();
    }

    private boolean isNetworkAvailable ( ) {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBackPressed() {
        if (selectedFolder == -1){
            super.onBackPressed();
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            selectedFolder = -1;
            applyFilter();
            adapter.notifyDataSetChanged();
        }
    }

}