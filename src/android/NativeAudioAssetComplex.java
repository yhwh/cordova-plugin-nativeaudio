//
//
//  NativeAudioAssetComplex.java
//
//  Created by Sidney Bofah on 2014-06-26.
//

package com.rjfun.cordova.plugin.nativeaudio;
import android.os.AsyncTask;
import java.io.IOException;
import java.util.concurrent.Callable;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.Log;
import java.lang.Math;
import java.lang.Thread;

public class NativeAudioAssetComplex implements OnPreparedListener, OnCompletionListener {

	private static final String TAG = NativeAudioAssetComplex.class.getSimpleName();

	private static final int INVALID = 0;
	private static final int PREPARED = 1;
	private static final int PENDING_PLAY = 2;
	private static final int PLAYING = 3;
	private static final int PENDING_LOOP = 4;
	private static final int LOOPING = 5;
	private static final int STOPPED = 6;
	private MediaPlayer mp;
	private MediaPlayer nextMp = null;
	private NativeAudioAssetComplex prevAsset = null;
	private int state;
	private String url;
	private boolean loopChain = false;

    Callable<Void> completeCallback;
    Callable<Void> loadCallback;
    FadeToThread fadeToThread = null;
    AssetFileDescriptor afd;
    float v;

    NativeAudioAssetComplex currentAsset;

	public NativeAudioAssetComplex(AssetFileDescriptor a, String uri, float volume)  throws IOException
	{
		currentAsset = this;
		state = INVALID;
		mp = new CompatMediaPlayer();


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
	
		mp.setOnPreparedListener(this);
		mp.prepareAsync();

		mp.setOnCompletionListener(this);

	}
	
	public MediaPlayer getPlayer() {
		return mp;
	}

	public void chainWithPrev(NativeAudioAssetComplex p) {
		prevAsset = p;
		prevAsset.getPlayer().setNextMediaPlayer(mp);
	}

	public void setLoadCb(Callable<Void> loadCb) {
		loadCallback = loadCb;
	}

	public void setCompleteCb (Callable<Void> completeCb) {
   		completeCallback = completeCb;
	}

	public void prepareLoop() {

		    try {
				createNextMediaPlayer();
				state = LOOPING;
				Log.d(TAG, String.format("AAAAAAAA  PREPARE LOOP !!!!"));

			} catch (IOException e) {
			};
	
	}

	private void createNextMediaPlayer() throws IOException {

        nextMp = new CompatMediaPlayer();

       	if (afd == null) {
            nextMp.setDataSource(url);
       	} else {
		    nextMp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
       	}

        
		nextMp.setAudioStreamType(AudioManager.STREAM_MUSIC); 
		nextMp.setVolume(v, v);
		
		
		nextMp.setOnPreparedListener(new OnPreparedListener() {
		     public void onPrepared(MediaPlayer nextMp) {
		     	try {
		        	mp.setNextMediaPlayer(nextMp);
		    	} catch (Exception e) {

		    	}
		     }
		});

		nextMp.prepareAsync();

		nextMp.setOnCompletionListener(this);
  
    }

	public void play(Callable<Void> completeCb) throws IOException
	{
        completeCallback = completeCb;
		invokePlay( false );
	}
	
	private void invokePlay( Boolean loop ) throws IOException
	{
		Log.d(TAG, String.format("BEFORE invoke: %d", state));

		Boolean playing = mp.isPlaying();



		if (loop) {
			createNextMediaPlayer();
		} 

		if ( playing )
		{
			mp.pause();
			mp.seekTo(0);
			state = (loop ? LOOPING : PLAYING);
			mp.start();
		}

		if ( !playing ) {

			state = (loop ? LOOPING : PLAYING);
			mp.start();

		}


		Log.d(TAG, String.format("\n\nAFTER PLAY INVOKED: %d\n\n", state));

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
			// state = INVALID;
			// 
			if (nextMp != null) {
				try {
					mp.setNextMediaPlayer(null);
				} catch (Exception e) {

				}

				try {
					nextMp.stop();
				} catch (Exception e) {
					
				}

				try {
					nextMp.release();
				} catch (Exception e) {
					
				}
				
				nextMp = null;
			}
			if ( mp.isPlaying() )
			{
				state = STOPPED;
				mp.pause();
				mp.seekTo(0);
	        }
		}
	        catch (IllegalStateException e)
	        {
            // I don't know why this gets thrown; catch here to save app
	        }



	}

	public void fadeTo(float to, int duration, FadeToCallback fadeToCb) {

	    if (fadeToThread != null) {
	    	fadeToThread.cancel();
	    	fadeToThread = null;
	    }

	    fadeToThread = new FadeToThread(to, duration, this, fadeToCb);
	    fadeToThread.start();
 
	}

	public void setVolume(float volume) 
	{
        try
        {
        	if (fadeToThread != null) {
		    	fadeToThread.cancel();
		    	fadeToThread = null;
		    }
        	v = volume;
			mp.setVolume(v, v);
			if (nextMp != null) {
				nextMp.setVolume(v, v);
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

		Log.d(TAG, String.format("\n\n BEFORE PREPARED: %d\n\n", state));


		state = PREPARED;
		
		
		Log.d(TAG, String.format("\n\nAFTER PREPARED: %d\n\n", state));

		if (prevAsset != null) {
			Log.d(TAG, String.format("AAAAAAAA  setting next media player !!!!"));
			prevAsset.getPlayer().setNextMediaPlayer(mp);
		}

		if (loadCallback != null) {
			try {
				loadCallback.call();
			}
			catch (Exception e)
			{
			}
			
			loadCallback = null;
		}

		if (prevAsset != null) {
			prevAsset.getPlayer().setNextMediaPlayer(mp);
		}

	}
	
	public void onCompletion(MediaPlayer mPlayer) 
	{


		// Log.d(TAG, String.format("Completed: %d\n", state));

		if (state != LOOPING)
		{
			try {

				state = INVALID;

				if (completeCallback != null)
	                completeCallback.call();
				}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		} else {	
		    mp.release();	  
            mp = nextMp;

            try {
				createNextMediaPlayer();
			} catch (IOException e) {
			}
		}
	}

  	private class FadeToThread extends Thread {
        private float to;
		private int duration;
		private NativeAudioAssetComplex asset;
		FadeToCallback callback;
		volatile boolean running = true;
        
        FadeToThread(float t, int d, NativeAudioAssetComplex n, FadeToCallback fadeToCb) {
            to = t;
			duration = d;
			asset = n;
			callback = fadeToCb;
        }

         public void run() {
			Log.d(TAG, String.format("\n\nThread STARTED\n\n"));
        	int steps = (int)(Math.floor(duration/50));

        	float increment = (to - v)/steps;
		    while (running  &&
		    		steps > 0 &&
		    		mp.isPlaying() && Math.abs(to - v) > 0) {
	
		    	v += increment;
		    	try {
		            mp.setVolume(v, v);
					if (nextMp != null) {
						nextMp.setVolume(v, v);
					}
				} catch (Exception e) {

				}


	            steps--;
		    	try {
	            	Thread.sleep(50);
	        	} catch (Exception e) {

	        	}
        	
		    }
		
			callback.setSuccess(running);
			try {
				if (running) {
					Log.d(TAG, String.format("\n\nEND OF FADE SUCCESS\n\n"));
				} else {
					Log.d(TAG, String.format("\n\nEND OF FADE INTERRUPT\n\n"));

				}
	        	callback.call(); 
	    	} catch (Exception e) {
	    		Log.d(TAG, String.format("\n\nEND OF FADE ERROR\n\n"));
	    	}
		    
         }

         public void cancel() {
         	running = false;
         }
     }


}
