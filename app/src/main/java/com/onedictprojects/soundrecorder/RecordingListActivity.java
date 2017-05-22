package com.onedictprojects.soundrecorder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



public class RecordingListActivity extends AppCompatActivity {

    List<AudioItem> itemsList = null;
    CustomListAdapter adapter = null;
    ListView listviewItems = null;
    MediaPlayer player = null;
    AudioItem selectedAudioItem = new AudioItem();

    ImageButton imageButtonPlayPause;
    TextView textViewFileNamePlaying = null;
    TextView getTextViewFileNamePlayingSize;
    Handler threadHandler = new Handler();

    boolean isPlaying = false;
    boolean isNewAudio = false;
    boolean isPlayed = false;
    String currentAudioFilePath = null;
    LinearLayout mpLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_list);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

//        mpLayout = (LinearLayout) findViewById(R.id.mediaPlayerLayout);
//        mpLayout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intentMediaPlayer = new Intent(RecordingListActivity.this, MediaPlayerActivity.class);
//                Bundle file = new Bundle();
//                file.putSerializable("file", selectedAudioItem);
//                intentMediaPlayer.putExtra("Package", file);
//                startActivity(intentMediaPlayer);
//            }
//        });

        imageButtonPlayPause = (ImageButton) findViewById(R.id.imageButton_playPause);
        textViewFileNamePlaying= (TextView) findViewById(R.id.textView_filenamePlaying);
        getTextViewFileNamePlayingSize = (TextView) findViewById(R.id.textViewFilenamePlayingSize);
        itemsList = getAllFileName();
        listviewItems = (ListView) findViewById(R.id.lvRecording);
        adapter = new CustomListAdapter(itemsList,this);
        listviewItems.setAdapter(adapter);

        listviewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                Object o = listviewItems.getItemAtPosition(position);
                AudioItem item = (AudioItem) o;
                currentAudioFilePath = item.getPath();
                isNewAudio = true;
                textViewFileNamePlaying.setText(item.getFilename() + "   " + item.getDuration());
                getTextViewFileNamePlayingSize.setText(item.getSize());
            }
        });
        listviewItems.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                index = position;
                return false;
            }
        });

        registerForContextMenu(listviewItems);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu,View v,ContextMenu.ContextMenuInfo menuInfo) {
        if(v.getId()==R.id.lvRecording) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            selectedAudioItem = itemsList.get(info.position);

            super.onCreateContextMenu(menu,v,menuInfo);
            getMenuInflater().inflate(R.menu.popup,menu);
        }
    }

    final Context context = this;
    static int index = -1;
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share:
                Intent intentShare = new Intent(Intent.ACTION_SEND);
                intentShare.setType("audio/*");
                File f = new File(selectedAudioItem.getPath());
                intentShare.putExtra(Intent.EXTRA_STREAM,Uri.fromFile(f));
                startActivity(Intent.createChooser(intentShare,"Share audio to..."));
                break;
            case R.id.delete:
                showDeleteConfirmDialog();
                break;
            case R.id.rename:
                showRenameDialog();

                break;
            case R.id.sync:
                break;
            case R.id.detail:
                DialogDetail dialog = DialogDetail.newInstance(selectedAudioItem);
                dialog.show(getFragmentManager(),"dialog");
                break;
            case R.id.mediaplayer:
                Intent intentMediaPlayer = new Intent(RecordingListActivity.this, MediaPlayerActivity.class);
                Bundle file = new Bundle();
                file.putSerializable("file", selectedAudioItem);
                intentMediaPlayer.putExtra("Package", file);
                startActivity(intentMediaPlayer);
                break;
        }

        return super.onContextItemSelected(item);
    }

    public void showDeleteConfirmDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);

        // set title
        alertDialogBuilder.setTitle("Warning");

        // set dialog message
        alertDialogBuilder
                .setMessage("Do you want to delete this file?")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, close
                        selectedAudioItem.deleteFile();
                        itemsList.remove(selectedAudioItem);
                        adapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("No",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();

                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    void showRenameDialog() {
        // custom dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        final View mView = getLayoutInflater().inflate(R.layout.dialog_rename,null);
        // set the custom dialog components - text, image and button
        final TextView txtNewname = (TextView) mView.findViewById(R.id.txtNewname);
        String[] tmp = selectedAudioItem.getFilename().split("\\.");
        txtNewname.setText(tmp[0]);
        txtNewname.setSelectAllOnFocus(true);
        //open anroid keyboard
        final InputMethodManager inputMethodManager =
                (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        Button dialogButton = (Button) mView.findViewById(R.id.btnOK);

        builder.setView(mView);
        final AlertDialog dialog = builder.create();
        dialog.show();

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strNewname = txtNewname.getText().toString() + "." + selectedAudioItem.getFileType();
                selectedAudioItem.renameFile(strNewname);
                renameNoteFile(selectedAudioItem.getFilename(), strNewname);
                renameItem(index, strNewname);
                inputMethodManager.hideSoftInputFromWindow(mView.getWindowToken(), 0);
                listviewItems.invalidateViews();
                dialog.dismiss();
            }
        });
    }

    void renameItem(int index, String newName){
        //View v = listviewItems.getChildAt(index - listviewItems.getFirstVisiblePosition());
//        View v = listviewItems.getChildAt(index);
//        Log.d("INDEX_ITEM",String.valueOf(index));
//        if(v == null) {
//            Log.d("ITEM","not found");
//            return;
//        }
//
//        TextView filename = (TextView) v.findViewById(R.id.listitem_textview_filename);
//        if(filename != null)
//            filename.setText(newName);
//        refreshVisibleViews();
        itemsList = getAllFileName();
        adapter = new CustomListAdapter(itemsList,this);
        listviewItems.setAdapter(adapter);
    }

    void refreshVisibleViews() {
        if (adapter != null) {
            for (int i = listviewItems.getFirstVisiblePosition(); i <= listviewItems.getLastVisiblePosition(); i ++) {
                final int dataPosition = i - listviewItems.getHeaderViewsCount();
                final int childPosition = i - listviewItems.getFirstVisiblePosition();
                if (dataPosition >= 0 && dataPosition < adapter.getCount()
                        && listviewItems.getChildAt(childPosition) != null) {
                    adapter.getView(dataPosition, listviewItems.getChildAt(childPosition), listviewItems);
                }
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // getMenuInflater().inflate(R.menu.popup,menu);
        return  true;
    }

    private List<AudioItem> getAllFileName() {
        //String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        List<AudioItem> list= new ArrayList<AudioItem>();

        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        File dir = new File(path,RecordActivity.AUDIO_RECORDER_FOLDER);

        if(!dir.exists())
            dir.mkdir();

        File[] files = dir.listFiles();

        for(int i=0;i<files.length;i++) {
//            AudioItem audioItem = new AudioItem("wav_file",files[i].getName(),files[i].getAbsolutePath());
            AudioItem audioItem = new AudioItem(files[i].getAbsolutePath());
            list.add(audioItem);
        }

        return list;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void playAudio(String filePath) {

        if(isNewAudio || isPlayed) {

            if(player!=null) {
                player.stop();
                //player.reset();
                player.release();
            }
            player = new MediaPlayer();
            isPlayed=false;
            try {
                player.setDataSource(filePath);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            try {
                player.prepare();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        int duration = this.player.getDuration();
        int currentPosition = player.getCurrentPosition();

        player.start();
        imageButtonPlayPause.setImageResource(R.drawable.pause50);
        isPlaying=true;
        isNewAudio=false;

    }
    private int currentPosition = 0;
    private void pauseAudio() {
        if(isPlaying) {
            player.pause();
            isPlaying=false;
            imageButtonPlayPause.setImageResource(R.drawable.play50);
        }
    }

    public void renameNoteFile(String oldName, String newName) {
        String[] tmp = oldName.split("\\.");
        String path = getFolderPath() + File.separator + tmp[0] + ".bin";
        File from = new File(path);
        String parentDirectory = from.getParent();
        String[] tmp2 = newName.split("\\.");
        String newNameWithExt = tmp2[0] + ".bin";
        File to = new File(parentDirectory, newNameWithExt);
        if(from.exists())
            from.renameTo(to);

    }

    public static final String AUDIO_RECORDER_NOTE_FOLDER = "SoundRecorderNote";
    private String getFolderPath() {
        String filepath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File folder = new File(filepath, AUDIO_RECORDER_NOTE_FOLDER);
        if(!folder.exists()) {
            folder.mkdir();
        }

        return folder.getAbsolutePath();
    }
}
