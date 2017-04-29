package com.onedictprojects.soundrecorder.Visualizer;

/**
 * Created by kiencbui on 29/04/2017.
 */

import com.onedictprojects.soundrecorder.RecordActivity;


public class CSleeper
        implements Runnable
{
    private Boolean done = Boolean.valueOf(false);
    private RecordActivity m_ma;
    private CSampler m_sampler;

    public CSleeper(RecordActivity paramMainActivity, CSampler paramCSampler)
    {
        m_ma = paramMainActivity;
        m_sampler = paramCSampler;
    }

    public void run()
    {
        try {
            m_sampler.Init();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        while (true)
            try
            {
                Thread.sleep(1000L);
                System.out.println("Tick");
                continue;
            }
            catch (InterruptedException localInterruptedException)
            {
                localInterruptedException.printStackTrace();
            }
    }
}