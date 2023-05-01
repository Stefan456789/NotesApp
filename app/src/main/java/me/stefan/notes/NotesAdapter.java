package me.stefan.notes;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import me.stefan.notes.backend.Backend;

public class NotesAdapter extends BaseAdapter implements Filterable {
    private final List<NoteFolder> notes;
    private List<NoteListviewItem> filteredNotes;
    private final MainActivity ctx;
    private final LayoutInflater inflater;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public NotesAdapter(MainActivity ctx, List<NoteFolder> notes) {
        this.notes = notes;
        this.filteredNotes = notes.stream().map(n -> (NoteListviewItem)n).collect(Collectors.toList());
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
        NoteListviewItem n = filteredNotes.get(i);
        View listItem = (view == null) ? inflater.inflate(R.layout.listitem_note, null ) : view;
        ((TextView) listItem.findViewById(R.id.itemMessage)).setText(n.title());
        ((TextView) listItem.findViewById(R.id.itemDateTime)).setText(n.description());

        ((ImageView)listItem.findViewById(R.id.noteIcon)).setImageResource(n.getIcon());

        if (n.isDone()){
            int color = ctx.prefs.getInt("doneNoteBackground", ContextCompat.getColor(ctx, R.color.light_red));
            listItem.setBackgroundColor(Color.parseColor("#"+Integer.toHexString(color)));

        } else if (n.isOverdue()){
            int color = ctx.prefs.getInt("overdueNoteBackground", ContextCompat.getColor(ctx, R.color.light_red));
            listItem.setBackgroundColor(Color.parseColor("#"+Integer.toHexString(color)));

        }else
            listItem.setBackground(null);

        return listItem ;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)

    @Override
    public Filter getFilter() {

        Filter filter = new Filter() {

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                filteredNotes = (List<NoteListviewItem>) results.values;
                notifyDataSetChanged();
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();
                List<NoteListviewItem> filteredNoteResults;

                int selectedFolder = Integer.parseInt(constraint.toString().split("&")[0]);
                boolean showOverdue = Boolean.parseBoolean(constraint.toString().split("&")[1]);

                if (selectedFolder == -1){
                    filteredNoteResults = notes.stream().map(n -> (NoteListviewItem)n).collect(Collectors.toList());
                } else {
                    filteredNoteResults = notes.get(selectedFolder).notes.stream().map(n -> (NoteListviewItem)n).collect(Collectors.toList());
                }

                if (!showOverdue){
                    filteredNoteResults = filteredNoteResults.stream().filter(n -> !n.isOverdue()).collect(Collectors.toList());
                }

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
        /*
        ProgressDialog dialog = ProgressDialog.show(ctx, "", "Loading. Please wait...", false);
        Backend b = Backend.login("x", "y", dialog::dismiss);
        if (b == null){
            Toast.makeText(ctx, "Account not found, please register!", Toast.LENGTH_LONG).show();
        }*/
    }
}
