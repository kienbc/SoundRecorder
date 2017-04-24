package com.onedictprojects.soundrecorder;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        hour = (TextView) findViewById(R.id.txtHour);
        min = (TextView) findViewById(R.id.txtMin);
        sec = (TextView) findViewById(R.id.txtSec);

        setButtonHandlers();
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        data = new byte[bufferSize];
        UpdateThreadTimer updateThreadTimer = new UpdateThreadTimer();
        threadHandler.postDelayed(updateThreadTimer,1000);


        //visualizer
    }


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

    private View.OnClickListener btnClick  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStartPause:
                    if(isRecording==false) {
                        enableButton(false);
                        toggleStartPause(true);
                        startRecording();
                    }
                    else {
                        enableButton(true);
                        toggleStartPause(false);
                        pauseRecording();
                    }
                    break;
                case R.id.btnStop:
                    stopRecording();
                    enableButton(false);
                    break;
                case R.id.btnDelete:
                    secCounter=0;
                    minCounter=0;
                    hourCounter=0;

                    sec.setText("00");
                    min.setText("00");
                    hour.setText("00");
                    deleteTempFile(getAllTempFilename());
                    enableButton(false);
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
            }

            threadHandler.postDelayed(this,1000);
        }
    }

}
