package com.onedictprojects.soundrecorder;

import java.io.Serializable;

/**
 * Created by My-PC on 5/18/2017.
 */

public class MyComment implements Serializable{
    private String time;
    private String content;

    public MyComment (String time, String content) {
        this.setTime(time);
        this.setContent(content);
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
