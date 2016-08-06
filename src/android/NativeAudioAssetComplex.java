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
import 	android.os.Build;
// import android.os.Build;
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
	private int state;
	private String url;
	private boolean loopChain = false;

    Callable<Void> completeCallback;
    Callable<Void> loadCallback;
    FadeToThread fadeToThread = null;
    AssetFileDescriptor afd;
    float v;
	boolean mCompatMode = false; 
    NativeAudioAssetComplex currentAsset;

	public NativeAudioAssetComplex(AssetFileDescriptor a, String uri, float volume)  throws IOException
	{

        mCompatMode = Build.VERSION.SDK_INT < 16;


		currentAsset = this;
		state = INVALID;
		


  		afd = a;

  		url = uri;

		mp = new MediaPlayer();
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

	public void setLoadCb(Callable<Void> loadCb) {
		loadCallback = loadCb;
	}

	public void setCompleteCb (Callable<Void> completeCb) {
   		completeCallback = completeCb;
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


		Boolean playing = mp.isPlaying();

		if (playing)
		{
			mp.pause();
			mp.seekTo(0);
			state = (loop ? LOOPING : PLAYING);
			mp.start();
		}


		if (!playing) {
			state = (loop ? LOOPING : PLAYING);
			mp.start();
		}


		if (loop) {
			if (mCompatMode) {
				mp.setLooping(true);
			} else {
				createNextMediaPlayer();
			}
		} else {
			if (mCompatMode) {
				mp.setLooping(false);
			} else {
				mp.setNextMediaPlayer(null);
			}
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

		state = PREPARED;
		
	
		if (loadCallback != null) {
			try {
				loadCallback.call();
			}
			catch (Exception e)
			{
			}
			
			loadCallback = null;
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
	        	callback.call(); 
	    	} catch (Exception e) {
	    	}
		    
         }

         public void cancel() {
         	running = false;
         }
     }


}
