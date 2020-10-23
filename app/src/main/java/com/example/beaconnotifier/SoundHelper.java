package com.example.beaconnotifier;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.CountDownTimer;

import static android.content.Context.AUDIO_SERVICE;

//*****************************************************************************************************************
public class SoundHelper {
    private Context context;
    private MediaPlayer mp;
    private AudioManager audioManager = null;
    private SoundPool soundPool;
    private int soundIDAlarm;
    private int soundIDBeep;
    private boolean plays = false, loaded = false;
    private float volume, maxVolume;
    private CountDownTimer countDown;
    private int actualSound;
    public final int S_ALARMA=0;
    public final int S_BEEP=1;

    //*****************************************************************************************************************
    SoundHelper(Context ctx, int alarmSound, int beepSound) {
        try {
            context = ctx;
            audioManager = (AudioManager) ctx.getSystemService(AUDIO_SERVICE);
            float actVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            volume = actVolume / maxVolume;
            soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 100);
            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int mySoundId, int status) {
                    loaded = true;
                }
            });
            soundIDAlarm = soundPool.load(context, alarmSound, 1);
            soundIDBeep = soundPool.load(context, beepSound, 1);
        } catch (Exception e) {
            throw e;
        }
    }



    //*****************************************************************************************************************
    public void playSound(int vol, long timeout,int tipo) {
        try {
            if (loaded && !plays) {
                if (vol >= 0) {
                    setVolume((int) ((vol * maxVolume) / 100.0));
                }
                switch (tipo){
                    case S_ALARMA:
                        actualSound=soundPool.play(soundIDAlarm, volume, volume, 1,10 , 1f);
                        break;
                    case S_BEEP:
                        actualSound=soundPool.play(soundIDBeep, volume, volume, 1,10 , 1f);
                        break;
                }
                plays = true;
                if(timeout>0){
                    new CountDownTimer(timeout, timeout/2) {
                        public void onTick(long millisUntilFinished) {
                        }
                        public void onFinish() {
                            stopSound();
                        }
                    }.start();
                }
            }
        } catch (Exception e) {
            throw e;
        }
    }

    //*****************************************************************************************************************
    public void setVolume(int vol) {
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
        } catch (Exception e) {
            throw e;
        }
    }

    //*****************************************************************************************************************
    public void stopSound() {
        try {
            if (plays) {
                soundPool.stop(actualSound);
                plays = false;
            }
        } catch (Exception e) {
            throw e;
        }
    }

    //*****************************************************************************************************************
    public void setManosLibres(boolean on) {

        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(on);
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }
    }

    public void setMicroMute(boolean on) {
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setMicrophoneMute(on);
            audioManager.setMode(AudioManager.MODE_CURRENT);
        }
    }

}
