package com.rjfun.cordova.plugin.nativeaudio;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.SystemClock;

public class CompatMediaPlayer extends MediaPlayer implements OnCompletionListener {

    private boolean mCompatMode = true;
    private MediaPlayer mNextPlayer;
    private OnCompletionListener mCompletion;

    public CompatMediaPlayer() {
        mCompatMode = true;
        try {
            MediaPlayer.class.getMethod("setNextMediaPlayer", MediaPlayer.class);
            mCompatMode = false;
        } catch (NoSuchMethodException e) {
            mCompatMode = true;
            super.setOnCompletionListener(this);
        }
    }

    public void setNextMediaPlayer(MediaPlayer next) {
        if (mCompatMode) {
            mNextPlayer = next;
        } else {
            super.setNextMediaPlayer(next);
        }
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        if (mCompatMode) {
            mCompletion = listener;
        } else {
            super.setOnCompletionListener(listener);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mNextPlayer != null) {
            // as it turns out, starting a new MediaPlayer on the completion
            // of a previous player ends up slightly overlapping the two
            // playbacks, so slightly delaying the start of the next player
            // gives a better user experience
            SystemClock.sleep(50);
            mNextPlayer.start();
        }
        mCompletion.onCompletion(this);
    }
}