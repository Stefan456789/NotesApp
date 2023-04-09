package me.stefan.notes;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
    public static ListView list;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = PreferenceManager.getDefaultSharedPreferences(this );
        setUpSharedPreferences();
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(openFileInput(FILE_NAME)));
            Arrays.stream(r.readLine().split(";")).forEach(x ->{
                String[] parts = x.split(",");
                notes.add(new Note(parts[2],LocalDate.parse(parts[0]),LocalTime.parse(parts[1])));
            });
        } catch (IOException | NullPointerException e ) {
            Toast.makeText(this, "Es wurden keine bestehende Notizen gefunden!", Toast.LENGTH_SHORT).show();
        }

        list = findViewById(R.id.list);
        adapter = new NotesAdapter(this, notes);
        list.setAdapter(adapter);


        registerForContextMenu(list);
    }

    private void setUpSharedPreferences() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.OnSharedPreferenceChangeListener preferencesChangeListener = this::onPreferenceChanged;
        prefs.registerOnSharedPreferenceChangeListener( preferencesChangeListener );
    }

    private void onPreferenceChanged(SharedPreferences sharedPrefs, String key) {
        adapter.getFilter().filter("" + sharedPrefs.getBoolean("showOverdueNotes", false));
        list.setAdapter(adapter);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.list) {
            getMenuInflater().inflate(R.menu.menu_context, menu);
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (info != null){
            Note n = (Note) adapter.getItem(info.position);
            switch (item.getItemId()){
                case R.id.edit:
                    openNoteEditor(this, n);
                    adapter.notifyDataSetChanged();
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
                    adapter.notifyDataSetChanged();
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
                Context context = this;
                openNoteEditor(context, null);
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
        }
        return super.onOptionsItemSelected(item);
    }

    private Note openNoteEditor(Context context, Note n) {
        View root = getLayoutInflater().inflate(R.layout.dialog_add, null);
        if (n != null){
            ((EditText)root.findViewById(R.id.message)).setText(n.note);
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
                dialog.getDatePicker().setMinDate(System.currentTimeMillis());
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
        final Note[] returnVal = {null};
        new AlertDialog.Builder(this)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditText note = root.findViewById(R.id.message);
                        EditText date = root.findViewById(R.id.editTextDate);
                        EditText time = root.findViewById(R.id.editTextTime);

                        if (note.getText().toString().isEmpty() || date.getText().toString().isEmpty() || time.getText().toString().isEmpty())
                            return;

                        if (n != null){
                            n.note = note.getText().toString();
                            n.date = LocalDate.parse(date.getText().toString());
                            n.time = LocalTime.parse(time.getText().toString());
                            returnVal[0] = n;
                        } else {
                            Note newNote = new Note(note.getText().toString(), LocalDate.parse(date.getText().toString()), LocalTime.parse(time.getText().toString()));
                            notes.add(newNote);
                            adapter.notifyDataSetChanged();
                            returnVal[0] = newNote;
                        }

                    }
                })
                .setNegativeButton("Cancle", null)
                .setView(root)
                .setTitle("Neue Notiz")
                .show();
        return returnVal[0];
    }
}