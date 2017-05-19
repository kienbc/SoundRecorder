package com.onedictprojects.soundrecorder;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by My-PC on 5/19/2017.
 */

public class CommentArrayAdapter extends ArrayAdapter<MyComment> {
    private Context context;
    private List<MyComment> myArray;

    public CommentArrayAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull ArrayList<MyComment> objects) {
        super(context, resource, objects);
        this.context = context;
        this.myArray = objects;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        MyComment comment = myArray.get(position);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.list_comment_item_layout, null);

        TextView textviewTime = (TextView) view.findViewById(R.id.textViewTime);
        TextView textviewContent = (TextView) view.findViewById(R.id.textViewContent);
        textviewTime.setText(comment.getTime());
        textviewContent.setText(comment.getContent());

        return view;
    }
}
