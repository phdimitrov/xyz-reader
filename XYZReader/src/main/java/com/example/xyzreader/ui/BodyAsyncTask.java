package com.example.xyzreader.ui;

import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

/**
 * Async task to transform large body text to paragraph array
 */
public class BodyAsyncTask extends AsyncTask<String, Void, String[]> {

    private WeakReference<ProgressBar> loaderRef;
    private WeakReference<RecyclerView> recyclerViewRef;

    public BodyAsyncTask(RecyclerView recyclerView, ProgressBar progressBar) {
        super();
        recyclerViewRef = new WeakReference<>(recyclerView);
        loaderRef = new WeakReference<>(progressBar);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        ProgressBar pb = loaderRef.get();
        if (pb != null) {
            pb.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected String[] doInBackground(String... params) {
        String body = params[0];

        //long text processing could be slow and cause frame lost
        final String[] paragraphs = body.split("(\r\n|\n){2,}");
        for (int i = 0; i < paragraphs.length; i++) {
            //remove unnecessary new lines and add tabs for readability
            paragraphs[i] = "\t\t" + paragraphs[i].replaceAll("(\r\n|\n)\\s*", " ").trim();
        }

        return paragraphs;
    }

    @Override
    protected void onPostExecute(String[] result) {
        super.onPostExecute(result);

        RecyclerView recyclerView = recyclerViewRef.get();
        if (recyclerView != null && recyclerView.getAdapter() != null && result != null) {
            ParagraphAdapter adapter = (ParagraphAdapter) recyclerView.getAdapter();
            adapter.update(result);
        }

        onFinished();
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        onFinished();
    }

    private void onFinished() {
        ProgressBar pb = loaderRef.get();
        if (pb != null) {
            pb.setVisibility(View.GONE);
        }
    }
}