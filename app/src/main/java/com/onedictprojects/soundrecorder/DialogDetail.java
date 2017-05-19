package com.onedictprojects.soundrecorder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * Created by My-PC on 4/27/2017.
 */

public class DialogDetail extends DialogFragment{

    static DialogDetail newInstance(AudioItem curSelItem) {
        DialogDetail f = new DialogDetail();

        Bundle args = new Bundle();
        args.putString("name", curSelItem.getFilename());
        args.putString("type", curSelItem.getFileType());
        args.putString("length", curSelItem.getDuration());
        args.putString("path", curSelItem.getPath());
        args.putString("last_modified", curSelItem.getDateModified());
        args.putString("size", curSelItem.getSize());
        f.setArguments(args);

        return f;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
            // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
//        builder.setView(inflater.inflate(R.layout.dialog_file_detail, null))
//                // Add action buttons
//                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int id) {
//
//                    }
//                })
//                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        DialogDetail.this.getDialog().cancel();
//                    }
//                });
        String strName = getArguments().getString("name");
        String strType = getArguments().getString("type");
        String strLength = getArguments().getString("length");
        String strPath = getArguments().getString("path");
        String strLastModified = getArguments().getString("last_modified");
        String strSize = getArguments().getString("size");
        View view = inflater.inflate(R.layout.dialog_file_detail, null);
        TextView textViewName = (TextView) view.findViewById(R.id.textview_filename);
        TextView textViewType = (TextView) view.findViewById(R.id.textview_filetype);
        TextView textViewModified = (TextView) view.findViewById(R.id.textview_datemodified);
        TextView textViewLength = (TextView) view.findViewById(R.id.textview_filelength);
        TextView textViewSize = (TextView) view.findViewById(R.id.textview_filesize);
        TextView textViewPath = (TextView) view.findViewById(R.id.textview_path);
        textViewType.setText("Type: " + strType);
        textViewModified.setText("Date modified: " + strLastModified);
        textViewLength.setText("Length: " + strLength);
        textViewSize.setText("Size: " + strSize);
        textViewPath.setText("Path: " + strPath);

        textViewName.setText(strName);

        builder.setView(view);
        return builder.create();
    }

}
