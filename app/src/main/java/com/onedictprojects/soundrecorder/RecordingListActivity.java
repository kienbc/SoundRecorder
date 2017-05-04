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

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
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
    SeekBar seekBar;
    TextView textViewFileNamePlaying = null;
    Handler threadHandler = new Handler();

    boolean isPlaying = false;
    boolean isNewAudio = false;
    boolean isPlayed = false;
    String currentAudioFilePath = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_list);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imageButtonPlayPause = (ImageButton) findViewById(R.id.imageButton_playPause);
        textViewFileNamePlaying= (TextView) findViewById(R.id.textView_filenamePlaying);
        imageButtonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPlaying) {
                    pauseAudio();
                }
                else playAudio(currentAudioFilePath);
            }
        });
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setClickable(false);

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
                textViewFileNamePlaying.setText(item.getFilename());
                playAudio(currentAudioFilePath);
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
                intentShare.putExtra(Intent.EXTRA_STREAM,Uri.parse(selectedAudioItem.getPath()));
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
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_rename);
        dialog.setTitle("................Rename.................");

        // set the custom dialog components - text, image and button
        final TextView txtNewname = (TextView) dialog.findViewById(R.id.txtNewname);
        String[] tmp = selectedAudioItem.getFilename().split("\\.");
        txtNewname.setText(tmp[0]);
        txtNewname.setSelectAllOnFocus(true);
        Button dialogButton = (Button) dialog.findViewById(R.id.btnOK);
        // if button is clicked, close the custom dialog
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strNewname = txtNewname.getText().toString() + "." + selectedAudioItem.getFileType();
                selectedAudioItem.renameFile(strNewname);
                renameItem(index, strNewname);
                //adapter.notifyDataSetChanged();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    void renameItem(int index, String newName){
        //View v = listviewItems.getChildAt(index - listviewItems.getFirstVisiblePosition());
        View v = listviewItems.getChildAt(index);
        if(v == null)
            return;

        TextView filename = (TextView) v.findViewById(R.id.listitem_textview_filename);
            if(filename != null)
            filename.setText(newName);
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
        if(currentPosition == 0) {
            this.seekBar.setMax(duration);
        }

        player.start();
        imageButtonPlayPause.setImageResource(R.drawable.pause50);
        isPlaying=true;
        isNewAudio=false;


        //tao thread update seekbar
        UpdateSeekbarThread updateSeekbarThread = new UpdateSeekbarThread();
        threadHandler.postDelayed(updateSeekbarThread,50);
    }

    class UpdateSeekbarThread implements Runnable {
        public void run() {
            int currentPosition = player.getCurrentPosition();
            int duration = player.getDuration();
            seekBar.setProgress(currentPosition);
            threadHandler.postDelayed(this,50);

            // chuyen trang thai nut pause sang play khi phat xong
            if(duration-currentPosition<=50) {
                player.reset();
                imageButtonPlayPause.setImageResource(R.drawable.play50);

                seekBar.setProgress(0);

                isPlayed=true;

                isPlaying=false;
                isNewAudio=false;
            }
        }
    }

    private void pauseAudio() {
        if(isPlaying) {
            player.pause();
            isPlaying=false;
            imageButtonPlayPause.setImageResource(R.drawable.play50);
        }
    }
}
