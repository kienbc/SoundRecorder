package com.onedictprojects.soundrecorder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.onedictprojects.soundrecorder.Visualizer.CDrawer;
import com.onedictprojects.soundrecorder.Visualizer.CSampler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.jar.Manifest;

public class RecordActivity extends AppCompatActivity {

    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    public static final String AUDIO_RECORDER_FOLDER = "SoundRecorder";
    private static final String AUDIO_RECORDER_TEMP_FOLDER = "SoundTemp";
    private static final String AUDIO_RECORDER_TEMP_EXT_FILE = ".raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;


    byte[] data = null;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    private static String tmpPlay = null;

    private TextView hour = null;
    private TextView min = null;
    private TextView sec = null;

    private int hourCounter = 0;
    private int minCounter = 0;
    private int secCounter = 0;

    Handler threadHandler = new Handler();

    // visualizer

//    private CDrawer.CDrawThread mDrawThread;
//    private CDrawer mdrawer;
//    private View.OnClickListener listener;
//    private Boolean m_bStart = Boolean.valueOf(false);
//    private Boolean recording;
//    private CSampler sampler;


    // visualizer
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        /*-----------------Request permission at runtime--------------------*/
        //RECORD_AUDIO: 1
        if (ContextCompat.checkSelfPermission(RecordActivity.this,
                android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(RecordActivity.this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO},
                    1);
        }

        //WRITE_EXTERNAL_STORAGE: 2
        if (ContextCompat.checkSelfPermission(RecordActivity.this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(RecordActivity.this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    2);
        }

        //READ_EXTERNAL_STORAGE: 3
        if (ContextCompat.checkSelfPermission(RecordActivity.this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(RecordActivity.this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    3);
        }

        hour = (TextView) findViewById(R.id.txtHour);
        min = (TextView) findViewById(R.id.txtMin);
        sec = (TextView) findViewById(R.id.txtSec);

        setButtonHandlers();
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        data = new byte[bufferSize];
        UpdateThreadTimer updateThreadTimer = new UpdateThreadTimer();
        threadHandler.postDelayed(updateThreadTimer,1000);


        /*---------------------setup notification-------------------------*/
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals("START")) {
                    Toast.makeText(getApplication(), "START", Toast.LENGTH_LONG).show();
                    processEnableRecording();
                    updateNotificationWhenUserTouchStart();
                    //startBackgroundColorTransition();
                }
                else if(intent.getAction().equals("STOP")) {
                    Toast.makeText(getApplication(), "STOP", Toast.LENGTH_LONG).show();
                    processPauseRecording();
                    processStopRecording();
                    updateNotificationWhenUserTouchStop();
                    //endBackgroundColorTransition();
                }
                else if(intent.getAction().equals("PAUSE")) {
                    Toast.makeText(getApplication(), "PAUSE", Toast.LENGTH_LONG).show();
                    processPauseRecording();
                    updateNotificationWhenUserTouchPause();
                    //pauseBackgroundColorTransition();
                }
                else if(intent.getAction().equals("RESUME")) {
                    Toast.makeText(getApplication(), "RESUME", Toast.LENGTH_LONG).show();
                    processEnableRecording();
                    updateNotificationWhenUserTouchResume();
                    //startBackgroundColorTransition();
                }
                else if(intent.getAction().equals("DELETE")) {
                    Toast.makeText(getApplication(), "DELETE", Toast.LENGTH_LONG).show();
                    processPauseRecording();
                    processDeleteRecording();
                    updateNotificationWhenUserTouchStop();
                    //endBackgroundColorTransition();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("START");
        filter.addAction("PAUSE");
        filter.addAction("STOP");
        filter.addAction("RESUME");
        filter.addAction("DELETE");
        registerReceiver(receiver, filter);

        addNotification();

        //========= Visualizer ============//

//        mdrawer = (CDrawer) findViewById(R.id.drawer);
//        m_bStart = Boolean.valueOf(false);
//
//        while (true) {
//            recording = Boolean.valueOf(false);
//            run();
//            return;
//        }

        //========= Visualizer ============//

    }

//    @Override
//    protected void onPause() {
//        sampler.SetRun(Boolean.valueOf(false));
//        mDrawThread.setRun(Boolean.valueOf(false));
//        sampler.SetSleeping(Boolean.valueOf(true));
//        mDrawThread.SetSleeping(Boolean.valueOf(true));
//        Boolean.valueOf(false);
//        super.onPause();
//
//    }
//
//    @Override
//    protected void onRestart() {
//        m_bStart = Boolean.valueOf(true);
//        super.onRestart();
//    }
//
//    @Override
//    protected void onResume()
//    {
//        System.out.println("onresume");
//        int i = 0;
//        while (true)
//        {
//            if ((sampler.GetDead2().booleanValue()) && (mdrawer.GetDead2().booleanValue()))
//            {
//                System.out.println(sampler.GetDead2() + ", " + mdrawer.GetDead2());
//                sampler.Restart();
//                if (!m_bStart.booleanValue())
//                    mdrawer.Restart(Boolean.valueOf(true));
//                sampler.SetSleeping(Boolean.valueOf(false));
//                mDrawThread.SetSleeping(Boolean.valueOf(false));
//                m_bStart = Boolean.valueOf(false);
//                super.onResume();
//                return;
//            }
//            try
//            {
//                Thread.sleep(500L);
//                System.out.println("Hang on..");
//                i++;
//                if (!sampler.GetDead2().booleanValue())
//                    System.out.println("sampler not DEAD!!!");
//                if (!mdrawer.GetDead2().booleanValue())
//                {
//                    System.out.println("mDrawer not DeAD!!");
//                    mdrawer.SetRun(Boolean.valueOf(false));
//                }
//                if (i <= 4)
//                    continue;
//                mDrawThread.SetDead2(Boolean.valueOf(true));
//            }
//            catch (InterruptedException localInterruptedException)
//            {
//                localInterruptedException.printStackTrace();
//            }
//        }
//    }
//
//    @Override
//    protected void onStart()
//    {
//        System.out.println("onstart");
//        super.onStart();
//    }
//
//    @Override
//    protected void onStop()
//    {
//        System.out.println("onstop");
//        super.onStop();
//    }
//
//    public void setBuffer(short[] paramArrayOfShort)
//    {
//        mDrawThread = mdrawer.getThread();
//        mDrawThread.setBuffer(paramArrayOfShort);
//    }
//
//    public void run()
//    {
//        try
//        {
//            if (mDrawThread == null)
//            {
//                mDrawThread = mdrawer.getThread();
//            }
//            if (sampler == null)
//                sampler = new CSampler(this);
//            Context localContext = getApplicationContext();
//            Display localDisplay = getWindowManager().getDefaultDisplay();
//            Toast localToast = Toast.makeText(localContext, "Please make some noise..", Toast.LENGTH_LONG);
//            localToast.setGravity(48, 0, localDisplay.getHeight() / 8);
//            localToast.show();
//            mdrawer.setOnClickListener(listener);
//            if (sampler != null){
//                try {
//                    sampler.Init();
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//
//                sampler.StartRecording();
//                sampler.StartSampling();
//            }
//        } catch (NullPointerException e) {
//            Log.e("Main_Run", "NullPointer: " + e.getMessage());
//        }
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id==R.id.mnList) {
            // open recording list activity
            Intent intent = new Intent(RecordActivity.this,RecordingListActivity.class);
            startActivity(intent);
        }

        if(id==R.id.mnSettings) {

        }

        if(id==R.id.mnAbout) {

        }
        return super.onOptionsItemSelected(item);
    }

    private void startRecording () {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING,bufferSize);
        int i = recorder.getState();

        if(i==1) {
            recorder.startRecording();
        }

        isRecording=true;
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioToFile();
            }
        },"AudioRecorder Thread");

        recordingThread.start();
    }

    private void writeAudioToFile() {
        String filename = getTempFilename();
        FileOutputStream fileOutputStream =null;

        try {
            fileOutputStream = new FileOutputStream(filename);
        } catch (FileNotFoundException ex) {
            System.out.println("SoundRecorder Error: "+ ex.getMessage());
        }

        int read = 0;

        if(fileOutputStream!=null) {
            while (isRecording) {
                read = recorder.read(data,0,bufferSize);

                if(AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        fileOutputStream.write(data);
                    } catch (IOException ex) {
                        System.out.println("SoundRecorder Error: "+ ex.getMessage());
                    }
                }
            }

            try {
                fileOutputStream.close();
            } catch (IOException ex) {
                System.out.println("SoundRecorder Error: "+ ex.getMessage());
            }
        }
    }

    private void pauseRecording() {
        if(recorder!=null) {
            isRecording = false;
            int i= recorder.getState();
            if(i==1) {
                recorder.stop();
            }

            recorder.release();
            recorder=null;
            recordingThread = null;
        }
    }

    private void stopRecording() {
        tmpPlay = getFilename();
        copyWaveFile(getAllTempFilename(),tmpPlay);
        deleteTempFile(getAllTempFilename());

        secCounter=0;
        minCounter=0;
        hourCounter=0;

        sec.setText("00");
        min.setText("00");
        hour.setText("00");
    }

    private void copyWaveFile(Vector<String> allTempFile,String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;

        long totalAudioLength = 0;
        long totalDataLength = totalAudioLength + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];

        for(int i=0;i<allTempFile.size();i++) {
            try {
                in = new FileInputStream(allTempFile.elementAt(i));
                totalAudioLength += in.getChannel().size();
                in.close();
            } catch (FileNotFoundException ex) {
                System.out.println("SoundRecorder Error: "+ex.getMessage());
            } catch (IOException ex) {
                System.out.println("SoundRecorder Error: "+ex.getMessage());
            }
        }

        totalDataLength = totalAudioLength+36;

        try {
            out = new FileOutputStream(outFilename);
        } catch (FileNotFoundException ex) {
            System.out.println("SoundRecorder Error: "+ex.getMessage());
        }

        writeWaveFileHeader(out, totalAudioLength, totalDataLength, longSampleRate, channels, byteRate);

        for(int i=0;i<allTempFile.size();i++) {

            try {
                in = new FileInputStream(allTempFile.elementAt(i));
                while (in.read(data) != -1) {
                    out.write(data);
                }

                in.close();
            } catch (FileNotFoundException ex) {
                System.out.println("SoundRecorder Error: "+ex.getMessage());
            } catch (IOException ex) {
                System.out.println("SoundRecorder Error: "+ex.getMessage());
            }
        }

        try {
            out.close();
        } catch (IOException ex) {
            System.out.println("SoundRecorder Error: "+ex.getMessage());
        }

    }

    private void writeWaveFileHeader( FileOutputStream out, long totalAudioLength, long totalDataLength, long longSampleRate, int channels, long byteRate) {
        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLength & 0xff);
        header[5] = (byte) ((totalDataLength >> 8) & 0xff);
        header[6] = (byte) ((totalDataLength >> 16) & 0xff);
        header[7] = (byte) ((totalDataLength >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLength & 0xff);
        header[41] = (byte) ((totalAudioLength >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLength >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLength >> 24) & 0xff);

        try {
            out.write(header, 0, 44);
        } catch (IOException ex) {
            System.out.println("SoundRecorder Error: "+ex.getMessage());
        }

    }

    private void deleteTempFile(Vector<String> tempFilename) {

        for(int i=0;i<tempFilename.size();i++) {
            File file = new File(tempFilename.elementAt(i));
            file.delete();
        }
    }

    private Vector<String> getAllTempFilename() {
        Vector<String> tmp = new Vector<>();

        //String path = getFilesDir().getAbsolutePath();
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();

        File dir = new File(path,AUDIO_RECORDER_TEMP_FOLDER);

        if(!dir.exists())
            dir.mkdir();

        File[] files = dir.listFiles();

        for(int i=0;i<files.length;i++)
            tmp.add(files[i].getAbsolutePath());

        return tmp;
    }

    private String getFilename() {
        //String filePath = getFilesDir().getAbsolutePath();
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File file = new File(filePath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()) {
            file.mkdir();
        }

        return (file.getAbsolutePath()+File.separator+System.currentTimeMillis()+AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getTempFilename() { // chua cai dat
        //String filepath = getFilesDir().getAbsolutePath();
        String filepath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File file = new File(filepath,AUDIO_RECORDER_TEMP_FOLDER);

        if(!file.exists()) {
            file.mkdir();
        }

        return (file.getAbsolutePath()+File.separator+System.currentTimeMillis()+AUDIO_RECORDER_TEMP_EXT_FILE);
    }

    private void setButtonHandlers() {
        ((ImageButton) findViewById(R.id.btnStartPause)).setOnClickListener(btnClick);
        ((ImageButton) findViewById(R.id.btnStop)).setOnClickListener(btnClick);
        ((ImageButton) findViewById(R.id.btnDelete)).setOnClickListener(btnClick);
    }

    private void processEnableRecording() {
        enableButton(false);
        toggleStartPause(true);
        startRecording();
    }

    private void processPauseRecording() {
        enableButton(true);
        toggleStartPause(false);
        pauseRecording();
    }

    private void processStopRecording() {
        stopRecording();
        enableButton(false);
    }

    private void processDeleteRecording(){
        secCounter=0;
        minCounter=0;
        hourCounter=0;

        sec.setText("00");
        min.setText("00");
        hour.setText("00");
        deleteTempFile(getAllTempFilename());
        enableButton(false);
    }

    private View.OnClickListener btnClick  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStartPause:
                    if(isRecording==false) {
                        updateNotificationWhenUserTouchStart();
                        processEnableRecording();
                    }
                    else {
                        updateNotificationWhenUserTouchPause();
                        processPauseRecording();
                    }
                    break;
                case R.id.btnStop:
                    updateNotificationWhenUserTouchStop();
                    processStopRecording();
                    break;
                case R.id.btnDelete:
                    updateNotificationWhenUserTouchDelete();
                    processDeleteRecording();
                    break;
            }
        }
    };

    private void toggleStartPause(boolean isRecording) {
        ImageButton button = (ImageButton) findViewById(R.id.btnStartPause);

        if(isRecording)
            button.setImageResource(R.drawable.pause50);
        else
            button.setImageResource(R.drawable.microphone48);

    }

    private void enableButton(int id, boolean isEnable) {
        ImageButton button = (ImageButton) findViewById(id);
        button.setEnabled(isEnable);

        if(isEnable)
            button.setVisibility(View.VISIBLE);
        else
            button.setVisibility(View.INVISIBLE);
    }

    private void enableButton(boolean isRecording) {
        enableButton(R.id.btnStop,isRecording);
        enableButton(R.id.btnDelete,isRecording);
    }
    private String strTimer;
    class UpdateThreadTimer implements Runnable {
        public void run() {
            if(isRecording) {
                if(secCounter<59) {
                    secCounter++;
                    if(secCounter<10)
                        sec.setText("0"+String.valueOf(secCounter));
                    else
                        sec.setText(String.valueOf(secCounter));
                }
                else {
                    secCounter=0;
                    sec.setText("00");
                    if(minCounter<59) {
                        minCounter++;
                        if(minCounter<10)
                            min.setText("0"+String.valueOf(minCounter));
                        else
                            min.setText(String.valueOf(minCounter));
                    }
                    else {
                        minCounter=0;
                        min.setText("00");
                        hourCounter++;
                        if(hourCounter<10)
                            hour.setText("0"+String.valueOf(hourCounter));
                        else
                            hour.setText(String.valueOf(hourCounter));
                    }
                }
                strTimer = hour.getText().toString() + ":" + min.getText().toString()+ ":" + sec.getText().toString();
                updateNotificationContent(strTimer);
            }

            threadHandler.postDelayed(this,1000);
        }
    }

    /*-------------------Notification---------------------*/
    final int NOTIFICATION_ID = 0;
    android.support.v4.app.NotificationCompat.Builder builder;
    private void addNotification() {
        Intent newIntentStart = new Intent("START");
        Intent newIntentOpen = new Intent(this, RecordActivity.class);
        PendingIntent pendingStart = PendingIntent.getBroadcast(this, 0, newIntentStart, 0);
        PendingIntent pendingOpen = PendingIntent.getActivity(this, 0, newIntentOpen,
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.statusbar_icon)
                        .setContentTitle("easyRecord")
                        .setContentText("...ready...")
                        .setOngoing(true)
                        .setAutoCancel(false)
                        .setPriority(Notification.PRIORITY_MAX)
                        .addAction(R.drawable.statusbar_icon,"Start",pendingStart);
        builder.setContentIntent(pendingOpen);

        // Add to status bar
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void updateNotificationWhenUserTouchStart() {
        //xóa các button cũ
        builder.mActions.clear();
        //cập nhật lại icon, title
        builder.setContentText("...recording...");
        //thêm các button mới
        Intent newIntentPause = new Intent("PAUSE");
        Intent newIntentStop = new Intent("STOP");
        Intent newIntentCancel = new Intent("DELETE");
        PendingIntent pendingPause = PendingIntent.getBroadcast(this, 0, newIntentPause, 0);
        PendingIntent pendingStop = PendingIntent.getBroadcast(this, 0, newIntentStop, 0);
        PendingIntent pendingCancel = PendingIntent.getBroadcast(this, 0, newIntentCancel, 0);
        builder.addAction(R.drawable.pause50, "Pause", pendingPause);
        builder.addAction(R.drawable.stop48, "Stop", pendingStop);
        builder.addAction(R.drawable.delete64, "Cancel", pendingCancel);
        // Add to status bar
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void updateNotificationWhenUserTouchStop() {
        //xóa các button cũ
        builder.mActions.clear();
        //cập nhật lại icon, title
        builder.setContentText("...ready...");
        //thêm các button mới
        Intent newIntentStart = new Intent("START");
        PendingIntent pendingStart = PendingIntent.getBroadcast(this, 0, newIntentStart, 0);
        builder.addAction(R.drawable.microphone48, "Start", pendingStart);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void updateNotificationWhenUserTouchPause() {
        //xóa các button cũ
        builder.mActions.clear();
        //cập nhật lại icon, title
        builder.setContentText("...ready...");
        //thêm các button mới
        Intent newIntentResume = new Intent("RESUME");
        Intent newIntentStop = new Intent("STOP");
        Intent newIntentCancel = new Intent("DELETE");
        PendingIntent pendingResume = PendingIntent.getBroadcast(this, 0, newIntentResume, 0);
        PendingIntent pendingStop = PendingIntent.getBroadcast(this, 0, newIntentStop, 0);
        PendingIntent pendingCancel = PendingIntent.getBroadcast(this, 0, newIntentCancel, 0);
        builder.addAction(R.drawable.microphone48, "Resume", pendingResume);
        builder.addAction(R.drawable.stop48, "Stop", pendingStop);
        builder.addAction(R.drawable.delete64, "Delete", pendingCancel);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void updateNotificationWhenUserTouchDelete() {
        updateNotificationWhenUserTouchStop();
    }

    private void updateNotificationWhenUserTouchResume() {
        updateNotificationWhenUserTouchStart();
    }

    private void updateNotificationContent(String content)
    {
        builder.setContentText(content);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
