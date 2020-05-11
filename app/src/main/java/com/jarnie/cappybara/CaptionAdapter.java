package com.jarnie.cappybara;


import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jarnie.subtitleFile.Caption;
import com.jarnie.subtitleFile.Time;
import com.jarnie.subtitleFile.TimedTextObject;

/* Adapter for RecyclerView on editor activity */

public class CaptionAdapter extends RecyclerView.Adapter<CaptionAdapter.ViewHolder> {

    private CaptionAdapterData data;

    // Constructor
    public CaptionAdapter(CaptionAdapterData cd) {
        data = cd;
    }

    // Create new views (invoked by layout manager)
    @NonNull
    @Override
    public CaptionAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Create new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_row_view, parent, false);

        return new ViewHolder(v, new ListenerHolder());
    }

    // Replace contents of a view (invoked by layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // Get element from dataset at this position
        // Replace contents of view with that element

        holder.listenerHolder.updatePosition(position);

        holder.startTime.setText(data.captionAt(position).start.toString());
        holder.endTime.setText(data.captionAt(position).end.toString());
        holder.editText.setText(data.captionAt(position).content);

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data.removeCaption(position);
                notifyDataSetChanged();
            }
        });
    }

    // Return size of dataset (invoked by layout manager)
    @Override
    public int getItemCount() {
        return data.size();
    }




    /* -------------- VIEW HOLDER -------------- */

    // Reference to views for each data item
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public EditText startTime;
        public EditText endTime;
        public EditText editText;
        public ListenerHolder listenerHolder;
        public ImageButton deleteButton;


        public ViewHolder(View v, ListenerHolder listeners) {
            super(v);

            startTime = v.findViewById(R.id.startTime);
            endTime = v.findViewById(R.id.endTime);
            editText = v.findViewById(R.id.textEdit);
            deleteButton = v.findViewById(R.id.deleteButton);
            listenerHolder = listeners;
            listenerHolder.bindViews(startTime, endTime, editText);

        }
    }

    /* -------------- LISTENER HOLDER -------------- */

    private class ListenerHolder {
        public CaptionTextListener caption;
        public StartTimeListener start;
        public EndTimeListener end;

        public ListenerHolder() {
            caption = new CaptionTextListener();
            start = new StartTimeListener();
            end = new EndTimeListener();
        }

        public void updatePosition(int position) {
            caption.updatePosition(position);
            start.updatePosition(position);
            end.updatePosition(position);
        }

        public void bindViews(EditText start, EditText end, EditText content) {
            start.addTextChangedListener(this.start);
            end.addTextChangedListener(this.end);
            content.addTextChangedListener(this.caption);
        }
    }

    /* -------------- EDITTEXT LISTENER -------------- */

    private class CaptionTextListener implements TextWatcher {
        private int position;

        public void updatePosition(int position) {
            this.position = position;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            // huh
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            data.updateCaptionContent(position, charSequence.toString());
        }

        @Override
        public void afterTextChanged(Editable editable) {
            // huh
        }
    }

    /* -------------- STARTTIME LISTENER -------------- */

    private class StartTimeListener implements TextWatcher {
        private int position;

        public void updatePosition(int position) {
            this.position = position;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            // huh
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            if(charSequence.length() != Time.STRING_LEN) return;
            String time = charSequence.toString();
            data.updateCaptionStart(position, time);
        }

        @Override
        public void afterTextChanged(Editable editable) {
            // huh
        }
    }

    /* -------------- ENDTIME LISTENER -------------- */

    private class EndTimeListener implements TextWatcher {
        private int position;

        public void updatePosition(int position) {
            this.position = position;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            // huh
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            if(charSequence.length() != Time.STRING_LEN) return;
            String time = charSequence.toString();
            data.updateCaptionEnd(position, time);
        }

        @Override
        public void afterTextChanged(Editable editable) {
            // huh
        }
    }


}
