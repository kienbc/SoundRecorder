package com.onedictprojects.soundrecorder;

import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by kiencbui on 21/04/2017.
 */

public class AudioItem {
    private String fileType;
    private String filename;
    private String path;
    private String duration;
    private String dateModified;
    private String size;

    public AudioItem(String fileType, String filename, String path) {
        this.fileType = fileType;
        this.filename = filename;
        this.path = path;
    }

    SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEE, MMMM d, yyyy");
    public AudioItem(String path) {
        File f = new File(path);
        Date lastModified = new Date(f.lastModified());
        String name = f.getName();
        this.filename = name;
        String[] tmp = name.split("\\.");
        this.fileType = tmp[1];
        this.path = path;
        this.setDuration(getLength(path));
        this.setDateModified(dateFormatter.format(lastModified));
        String strSize = f.length()/1000 + " KB";
        this.setSize(strSize);
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return this.filename;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDateModified() {
        return dateModified;
    }

    public void setDateModified(String dateModified) {
        this.dateModified = dateModified;
    }

    public String getLength(String path)
    {
        Uri uri = Uri.parse(path);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        int millSecond = Integer.parseInt(durationStr);
        int t = millSecond/1000;
        int m = t/60;
        int s = t&60;
        String res="";
        if(m<10)
            res = "0" + m + ":";
        if(s<10)
            res += "0" + s;

        return res;
    }

    String convertDateToString(Date d)
    {
        Calendar c= Calendar.getInstance();
        c.setTime(d);
        String res = c.getDisplayName(Calendar.DAY_OF_MONTH, Calendar.LONG, Locale.US);
        return res;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void deleteFile() {
        File f = new File(this.path);
        if(f.exists()) f.delete();
    }
}
