//
//
//  NativeAudioAssetComplex.java
//
//  Created by Sidney Bofah on 2014-06-26.
//

package com.rjfun.cordova.plugin.nativeaudio;

import java.io.IOException;
import java.util.concurrent.Callable;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.Log;

public class NativeAudioAssetComplex implements OnPreparedListener, OnCompletionListener {

	private static final String TAG = NativeAudioAssetComplex.class.getSimpleName();

	private static final int INVALID = 0;
	private static final int PREPARED = 1;
	private static final int PENDING_PLAY = 2;
	private static final int PLAYING = 3;
	private static final int PENDING_LOOP = 4;
	private static final int LOOPING = 5;
	
	private MediaPlayer mp;
	private MediaPlayer nextMp = null;
	private NativeAudioAsset nextAsset = null;
	private int state;
	private String url;
	private boolean loopChain;

    Callable<Void> completeCallback;

    AssetFileDescriptor afd;
    float v;

	public NativeAudioAssetComplex(AssetFileDescriptor a, String uri, float volume)  throws IOException
	{
		state = INVALID;
		mp = new MediaPlayer();

        mp.setOnPreparedListener(this);
  		afd = a;

  		url = uri;

       	if (afd == null) {
            mp.setDataSource(url);
       	} else {
		    mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
       	}
        
        v = volume;
		mp.setAudioStreamType(AudioManager.STREAM_MUSIC); 
		mp.setVolume(volume, volume);
		mp.prepare();
		mp.setOnCompletionListener(this);

	}
	
	public MediaPlayer getPlayer() {
		return mp;
	}

	public void chain (NativeAudioAsset n) {
		nextAsset = n;
		nextAsset.getPlayer().seekTo(0);
		mp.setNextMediaPlayer(nextAsset.getPlayer());
	}

	public void setCompleteCb (Callable<Void> completeCb) {
   		completeCallback = completeCb;
	}

	public void prepareLoop() {

		    try {
				createNextMediaPlayer();
				state = LOOPING;
			} catch (IOException e) {
			};
	
	}

	private void createNextMediaPlayer() throws IOException {

        nextMp = new MediaPlayer();

       	if (afd == null) {
            nextMp.setDataSource(url);
       	} else {
		    nextMp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
       	}

        
		nextMp.setAudioStreamType(AudioManager.STREAM_MUSIC); 
		nextMp.setVolume(v, v);
		nextMp.prepare();

		nextMp.setOnCompletionListener(this);
	

		mp.setNextMediaPlayer(nextMp);
  
    }

	public void play(Callable<Void> completeCb) throws IOException
	{
        completeCallback = completeCb;
		invokePlay( false );
	}
	
	private void invokePlay( Boolean loop ) throws IOException
	{
		// Log.d(TAG, String.format("BEFORE invoke: %d", state));

		Boolean playing = mp.isPlaying();



		if (loop) {
			createNextMediaPlayer();
		} 

		if ( playing )
		{
			mp.pause();
			mp.seekTo(0);
			state = PLAYING;
			mp.start();
		}

		if ( !playing )
		{
			state = (loop ? PENDING_LOOP : PENDING_PLAY);
			onPrepared( mp );
		}


		if (state != INVALID && nextAsset != null) {
		
			chain(nextAsset);
		
		}

	}

	public boolean pause()
	{
		try
		{
			if ( mp.isPlaying() )
			{
				mp.pause();
				return true;
			}
    	}
		catch (IllegalStateException e)
		{
		// I don't know why this gets thrown; catch here to save app
		}
		return false;
	}

	public void resume()
	{
		mp.start();
	}

    public void stop()
	{
		try
		{
			// mp.setNextMediaPlayer(null);
			if ( mp.isPlaying() )
			{
				state = INVALID;
				mp.pause();
				mp.seekTo(0);
	        }
		}
	        catch (IllegalStateException e)
	        {
            // I don't know why this gets thrown; catch here to save app
	        }



	}

	public void setVolume(float volume) 
	{
        try
        {
			mp.setVolume(volume,volume);
			v = volume;
			if (nextMp != null) {
				nextMp.setVolume(volume, volume);
			}
        }
            catch (IllegalStateException e) 
		{
                // I don't know why this gets thrown; catch here to save app
		}
	}
	
	public void loop() throws IOException
	{
		invokePlay( true );
	}
	
	public void unload() throws IOException
	{
		this.stop();
		mp.release();
	}
	
	public void onPrepared(MediaPlayer mPlayer) 
	{
		if (state == PENDING_PLAY) 
		{
			state = PLAYING;
			mp.start();
			
		}
		else if ( state == PENDING_LOOP )
		{
			state = LOOPING;
			mp.start();

		}
		else
		{
			state = PREPARED;
		}

	}
	
	public void onCompletion(MediaPlayer mPlayer) 
	{


		Log.d(TAG, String.format("Completed: %d\n", state));

		if (state != LOOPING)
		{
			this.state = INVALID;

			mp.stop();
			mp.release();

			try {

				state = INVALID;
				mp = new MediaPlayer();

		        mp.setOnPreparedListener(this);

		       	if (afd == null) {
		            mp.setDataSource(url);
		       	} else {
		       		mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
		       	}

				mp.setAudioStreamType(AudioManager.STREAM_MUSIC); 
				mp.setVolume(v, v);

				mp.prepare();
				mp.setOnCompletionListener(this);

				if (completeCallback != null)
                completeCallback.call();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		} else {
			mPlayer.release();	  
            mp = nextMp;
            try {
				createNextMediaPlayer();
				mp.setOnCompletionListener(this);
			} catch (IOException e) {
			}
	
		}
	}
}
