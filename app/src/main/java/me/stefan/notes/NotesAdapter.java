package me.stefan.notes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

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
    private Drawable originalBgDrawable;

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
        ((TextView) listItem.findViewById(R.id.itemDateTime)).setText("Date: " + n.date + " Time: " + n.time);
        if (originalBgDrawable != null)
            originalBgDrawable = listItem.getBackground();
//if (((CheckBox)listItem.findViewById(R.id.isDone)))
        if (isOverdue(n)){
            int color = ctx.prefs.getInt("overdueNoteBackground", ContextCompat.getColor(ctx, R.color.light_red));
            listItem.setBackgroundColor(Color.parseColor("#"+Integer.toHexString(color)));

        }else
            listItem.setBackground(originalBgDrawable);

        return listItem ;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean isOverdue(Note n) {
        return LocalDateTime.of(n.date, n.time).isAfter(LocalDateTime.now());
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

                if (constraint.equals("true"))
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
}
