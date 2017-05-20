package com.onedictprojects.soundrecorder;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.cleveroad.audiovisualization.AudioVisualization;
import com.cleveroad.audiovisualization.VisualizerDbmHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Vector;

import at.markushi.ui.CircleButton;

public class MediaPlayerActivity extends AppCompatActivity {

    private AudioVisualization audioVisualization;
    private VisualizerDbmHandler musicHandler;
    //------------------------Comment listview--------------------------
    private ArrayList<MyComment> commentProperties = new ArrayList<>();
    ArrayAdapter<MyComment> adapter = null;

    private TextView mStart;
    private TextView mEnd;
    private SeekBar mSeekbar;
    private int currentProgress;
    private CircleButton btnPlay;
    private CircleButton btnAddComment;
    private CircleButton btnShowComments;

    private MediaPlayerService player;
    boolean serviceBound = false;
    updateProgressTask task;

    public static String Broadcast_PLAY_NEW_AUDIO = "PLAY_NEW_AUDIO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        /*-----------------Request get phone state-----------------*/
        //READ_PHONE_STATE: to handle incoming call
        if (ContextCompat.checkSelfPermission(MediaPlayerActivity.this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MediaPlayerActivity.this,
                    new String[]{android.Manifest.permission.READ_PHONE_STATE},
                    10);
        }
        //get file info
        Intent callerIntent = getIntent();
        Bundle packageFromCaller =
                callerIntent.getBundleExtra("Package");
        final AudioItem audioItem = (AudioItem) packageFromCaller.getSerializable("file");
        TextView labelName = (TextView) findViewById(R.id.textViewMediaInfo);
        labelName.setText(audioItem.getFilename());
        //setup media player
        mStart = (TextView) findViewById(R.id.startText);
        mEnd = (TextView) findViewById(R.id.endText);
        mSeekbar = (SeekBar) findViewById(R.id.seekBar1);
        audioVisualization = (AudioVisualization) findViewById(R.id.visualizer_view);

        playAudio(audioItem.getPath());
        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mStart.setText(DateUtils.formatElapsedTime(progress / 1000));
                currentProgress = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                player.mediaPlayer.seekTo(currentProgress);
            }
        });
        task = new updateProgressTask();
        task.execute();

        btnPlay = (CircleButton) findViewById(R.id.buttonPlay);
        final int id = getResources().getIdentifier("com.onedictprojects.soundrecorder:drawable/" + "circle_pause_button", null, null);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
//                else mediaPlayer.start();
                if (player.mediaPlayer.isPlaying()) player.pauseMedia();
                else player.playMedia();
                updatePlayButtonIcon();
            }
        });
        btnAddComment = (CircleButton) findViewById(R.id.buttonNewComment);
        btnAddComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String strTime = DateUtils.formatElapsedTime(player.mediaPlayer.getCurrentPosition()/1000);
                final AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                View mView = getLayoutInflater().inflate(R.layout.dialog_add_comment,null);
                final EditText editTextComment = (EditText) mView.findViewById(R.id.editTextAddComment);
                editTextComment.setHint("Comment as " + strTime);
                Button btnAddComment = (Button) mView.findViewById(R.id.buttonAddComment);

                builder.setView(mView);
                final AlertDialog dialog = builder.create();
                dialog.show();

                btnAddComment.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String strComment = editTextComment.getText().toString();
                        addCommentToDB(audioItem.getFilename(), strTime, strComment);

                        dialog.dismiss();
                    }
                });
            }
        });

        adapter = new CommentArrayAdapter(this, 0, commentProperties);
        btnShowComments = (CircleButton) findViewById(R.id.buttonShowComments);
        btnShowComments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commentProperties.clear();
                commentProperties.addAll(loadCommentDataFromDB(audioItem.getFilename()));
                final AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                View mView = getLayoutInflater().inflate(R.layout.dialog_comments_listview,null);
                ListView listViewComments = (ListView) mView.findViewById(R.id.listviewComments);
                TextView textViewFilname = (TextView) mView.findViewById(R.id.textViewFilenameOfComment);
                textViewFilname.setText(audioItem.getFilename() + " comments:");
                listViewComments.setAdapter(adapter);

                builder.setView(mView);
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
        musicHandler = VisualizerDbmHandler.Factory.newVisualizerHandler(getApplicationContext(), 0);
        audioVisualization.linkTo(musicHandler);
    }

    private String getDataFilepathFromAudioFilename(String filenameExt)
    {
        String[] tmp = filenameExt.split("\\.");
        String filename = tmp[0];
        String path = getFolderPath() + File.separator + filename + ".bin";

        return path;
    }

    private boolean addCommentToDB(String audioFilename, String strTime, String strComment) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        String path = getDataFilepathFromAudioFilename(audioFilename);
        Vector<MyComment> myData = loadCommentDataFromDB(audioFilename);
        MyComment newComment = new MyComment(strTime, strComment);
        myData.add(newComment);
        try {
            fos = new FileOutputStream(path, false);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(myData);
            oos.close();
            fos.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            Log.d("FILE","File not found");

        } catch (IOException e) {
            e.printStackTrace();
            Log.d("FILE","Loi ghi file");
        }

        return true;
    }

    private Vector<MyComment> loadCommentDataFromDB(String filename) {
        String filepath = getDataFilepathFromAudioFilename(filename);
        File f = new File(filepath);
        Vector<MyComment> ds = new Vector<>();
        if (f.exists()==true) {
            FileInputStream fis = null;
            ObjectInputStream ois = null;

            try {
                fis = new FileInputStream(filepath);
                ois = new ObjectInputStream(fis);

                ds = (Vector<MyComment>) ois.readObject();
                Log.d("FILE","Doc du lieu thanh cong");
                ois.close();
                fis.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                Log.d("FILE","Khong doc duoc du lieu tu file");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return ds;
        }
        else {
            Log.d("FILE","Khong tim thay file");
            return ds;
        }
    }

    private void playAudio(String media) {
        //Check is service is active
        if (!serviceBound) {
            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            playerIntent.putExtra("media", media);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            startService(playerIntent);
        } else {
            //Service is active
            //Send media with BroadcastReceiver

        }
    }



    private class updateProgressTask extends AsyncTask<Void, Long, Void> {
        String waitMsg = "Wait\nSome SLOW job is being done... ";

        protected void onPreExecute() {
        }

        protected Void doInBackground(final Void... args) {
            try {
                while(true) {
                    if(player == null) continue;
                    if(!player.mediaPlayer.isPlaying()) continue;
                    if(player.isMediaStop) return null;
                    Long output = Long.valueOf(player.mediaPlayer.getCurrentPosition());
                    publishProgress(output);
                    Log.d("abc", String.valueOf(output/1000));
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                Log.e("slow-job interrupted", e.getMessage());
            }

            return null;
        }

        protected void onProgressUpdate(Long... value)
        {
            super.onProgressUpdate(value);
            int progress = Integer.valueOf(value[0].toString());
            mSeekbar.setProgress(progress);
            mStart.setText(DateUtils.formatElapsedTime(progress/1000));
        }

        protected void onPostExecute(final Void unused) {
        }
    }

    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;
            updateDuration();
            audioVisualization.onResume();
            updatePlayButtonIcon();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private void updateDuration() {
        int duration = player.mediaPlayer.getDuration();
        mSeekbar.setMax(duration);
        mEnd.setText(DateUtils.formatElapsedTime(duration / 1000));
    }

    private void updatePlayButtonIcon()
    {
        if (player.mediaPlayer.isPlaying())
            btnPlay.setImageDrawable(getResources().getDrawable(R.drawable.circle_pause_button));
        else
            btnPlay.setImageDrawable(getResources().getDrawable(R.drawable.circle_play_button));

    }


    @Override
    public void onResume() {
        super.onResume();
        //handleMediaPlayer();

        audioVisualization.onResume();
    }

    @Override
    public void onPause() {
        audioVisualization.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        audioVisualization.release();
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();
            if(!task.isCancelled()) task.cancel(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    public static final String AUDIO_RECORDER_NOTE_FOLDER = "SoundRecorderNote";
    private String getFolderPath()
    {
        String filepath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File folder = new File(filepath, AUDIO_RECORDER_NOTE_FOLDER);
        if(!folder.exists()) {
            folder.mkdir();
        }

        return folder.getAbsolutePath();
    }
}
