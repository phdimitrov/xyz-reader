package com.example.xyzreader.ui;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;

/**
 * Paragraph adapter for book content list
 */
public class ParagraphAdapter extends RecyclerView.Adapter<ParagraphAdapter.TextViewHolder> {

    private String[] paragraphs;

    public void update(String[] paragraphs) {
        this.paragraphs = paragraphs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TextViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_body_paragraph, parent, false);
        return new TextViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TextViewHolder holder, int position) {
        holder.setText(paragraphs[position]);
    }

    @Override
    public int getItemCount() {
        return paragraphs == null ? 0 : paragraphs.length;
    }


    public static class TextViewHolder extends RecyclerView.ViewHolder {

        public TextViewHolder(View itemView) {
            super(itemView);
        }

        public void setText(String text) {
            ((TextView) itemView).setText(text);
        }
    }
}
