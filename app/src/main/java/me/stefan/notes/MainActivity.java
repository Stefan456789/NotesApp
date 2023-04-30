package me.stefan.notes;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    public SharedPreferences prefs ;
    public static final ArrayList<Note> notes = new ArrayList<>();
    private static final String FILE_NAME = "save.csv";
    private static final int REQUESTCODE_SETTINGS = 1;
    public static NotesAdapter adapter;
    public ListView list;
    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangeListener;
    public boolean tryOnlineSync = false;
    public Backend backend = null;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUpSharedPreferences();


        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(openFileInput(FILE_NAME)));
            Arrays.stream(r.readLine().split(";")).forEach(x ->{
                String[] parts = x.split(",");
                notes.add(new Note(parts[2],LocalDate.parse(parts[0]),LocalTime.parse(parts[1]), Boolean.parseBoolean(parts[3])));
            });
        } catch (IOException | NullPointerException e ) {
            Toast.makeText(this, "Es wurden keine bestehende Notizen gefunden!", Toast.LENGTH_SHORT).show();
        }

        list = findViewById(R.id.list);
        adapter = new NotesAdapter(this, notes);
        list.setAdapter(adapter);


        registerForContextMenu(list);
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

    private void applyFilter(SharedPreferences sharedPrefs) {
        adapter.getFilter().filter("" + sharedPrefs.getBoolean("showOverdueNotes", true));
    }
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
            Note n = (Note) adapter.getItem(info.position);
            switch (item.getItemId()){
                case R.id.edit:
                    openNoteEditor(this, n);
                    break;
                case R.id.details:
                    LinearLayout root = new LinearLayout(this);
                    root.setOrientation(LinearLayout.VERTICAL);
                    float density = this.getResources().getDisplayMetrics().density;
                    root.setPadding((int) (20*density), (int) (5*density), (int) (20*density),0);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                    TextView note = new TextView(this);
                    note.setText("Note: " + n.note);
                    TextView date = new TextView(this);
                    date.setText("Date: " + n.date);
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
                    break;
                case R.id.delete:
                    notes.remove(n);
                    applyFilter();
                    adapter.notifyDataSetChanged();
                    break;

                case R.id.toggleDone:
                    n.done = !n.done;
                    list.invalidateViews();
                    break;
            }

        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId() ;
        switch (id ) {
            case R.id.newNote:
                openNoteEditor(this, null);
                break;
            case R.id.saveNote:
                try {
                    OutputStreamWriter w = new OutputStreamWriter(openFileOutput(FILE_NAME, MODE_PRIVATE));
                    notes.forEach(x -> {
                        try {
                            w.write(x.toString()+";");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
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
                tryOnlineSync = !tryOnlineSync;
                if (tryOnlineSync){

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
                                            ProgressDialog dialog = ProgressDialog.show(root.getContext(), "", "Loading. Please wait...", true);
                                            backend = Backend.login(
                                                    ((EditText)root.findViewById(R.id.loginUsername)).getText().toString(),
                                                    ((EditText)root.findViewById(R.id.loginPassword)).getText().toString(),
                                                    dialog::dismiss);
                                        })
                                        .setView(root).show();
                            })
                            .setNegativeButton("Register", (x, y) -> {
                                View root = getLayoutInflater().inflate(R.layout.dialog_register, null);
                                new AlertDialog.Builder(this)
                                        .setTitle("Register")
                                        .setNegativeButton("Cancel", null)
                                        .setPositiveButton("Login", (dialogInterface1, i1) -> {
                                            ProgressDialog dialog = ProgressDialog.show(root.getContext(), "", "Loading. Please wait...", true);
                                            backend = Backend.register(
                                                    ((EditText)root.findViewById(R.id.registerName)).getText().toString(),
                                                    ((EditText)root.findViewById(R.id.registerUsername)).getText().toString(),
                                                    ((EditText)root.findViewById(R.id.registerPassword)).getText().toString(),
                                                    dialog::dismiss);
                                        })
                                        .setView(root)
                                        .show();
                            })
                            .show();
                    item.setIcon(ContextCompat.getDrawable(this, R.drawable.cloudsyncgreen));
                } else {
                    item.setIcon(ContextCompat.getDrawable(this, R.drawable.cloudsync));
                }

                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void openNoteEditor(Context context, Note n) {
        View root = getLayoutInflater().inflate(R.layout.dialog_add, null);
        if (n != null){
            ((EditText)root.findViewById(R.id.editTextNote)).setText(n.note);
            ((EditText)root.findViewById(R.id.editTextDate)).setText(n.date.toString());
            ((EditText)root.findViewById(R.id.editTextTime)).setText(n.time.toString());
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
                            n.date = LocalDate.parse(date.getText().toString());
                            n.time = LocalTime.parse(time.getText().toString());
                            applyFilter();
                            adapter.notifyDataSetChanged();
                        } else {
                            try {
                                Note newNote = new Note(note.getText().toString(), LocalDate.parse(date.getText().toString()), LocalTime.parse(time.getText().toString()), false);
                                notes.add(newNote);
                            } catch (Exception ex){
                                Toast.makeText(context, "Please enter a valid date and time!", Toast.LENGTH_LONG).show();
                                return;
                            }
                            applyFilter();
                            adapter.notifyDataSetChanged();
                        }

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
}