package com.onedictprojects.soundrecorder;

import java.io.Serializable;

/**
 *       Easy Record_1.0_1412265_1412317
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
