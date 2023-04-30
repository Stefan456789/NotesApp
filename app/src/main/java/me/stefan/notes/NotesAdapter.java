package me.stefan.notes;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotesAdapter extends BaseAdapter implements Filterable {
    private final List<Note> notes;
    private List<Note> filteredNotes;
    private final MainActivity ctx;
    private final LayoutInflater inflater;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public NotesAdapter(MainActivity ctx, List<Note> notes) {
        this.notes = notes;
        this.filteredNotes = notes;
        this.ctx = ctx;
        this.inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public int getCount() {
        return filteredNotes.size();
    }

    @Override
    public Object getItem(int i) {
        return filteredNotes.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Note n = filteredNotes.get(i);
        View listItem = (view == null) ? inflater.inflate(R.layout.listitem_note, null ) : view;
        ((TextView) listItem.findViewById(R.id.itemMessage)).setText(n.note);
        ((TextView) listItem.findViewById(R.id.itemDateTime)).setText("Due " + n.date + " at " + n.time);


        if (n.done){
            int color = ctx.prefs.getInt("doneNoteBackground", ContextCompat.getColor(ctx, R.color.light_red));
            listItem.setBackgroundColor(Color.parseColor("#"+Integer.toHexString(color)));

        } else if (isOverdue(n)){
            int color = ctx.prefs.getInt("overdueNoteBackground", ContextCompat.getColor(ctx, R.color.light_red));
            listItem.setBackgroundColor(Color.parseColor("#"+Integer.toHexString(color)));

        }else
            listItem.setBackground(null);

        return listItem ;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean isOverdue(Note n) {
        return LocalDateTime.now().isAfter(LocalDateTime.of(n.date, n.time));
    }

    @Override
    public Filter getFilter() {

        Filter filter = new Filter() {

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                filteredNotes = (List<Note>) results.values;
                notifyDataSetChanged();
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();
                ArrayList<Note> filteredNoteResults = new ArrayList<>();

                if (constraint.equals("false"))
                    notes.forEach(n -> {
                        if (!isOverdue(n))
                            filteredNoteResults.add(n);
                    });
                else
                    filteredNoteResults.addAll(notes);

                results.count = filteredNoteResults.size();
                results.values = filteredNoteResults;

                return results;
            }
        };

        return filter;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        ProgressDialog dialog = ProgressDialog.show(ctx, "", "Loading. Please wait...", false);
        Backend b = Backend.login("x", "y", dialog::dismiss);
        if (b == null){
            Toast.makeText(ctx, "Account not found, please register!", Toast.LENGTH_LONG).show();
        }
    }
}
