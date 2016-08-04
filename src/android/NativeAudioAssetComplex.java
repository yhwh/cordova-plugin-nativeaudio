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
import android.support.v4.os.AsyncTaskCompat;

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
    FadeToCallback fadeToCallback = null;
    FadeToTask fadeToTask = null;
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

		// if ( playing )
		// {
		// 	mp.pause();
		// 	mp.seekTo(0);
		// 	state = (loop ? LOOPING : PLAYING);
		// 	mp.start();
		// }

		// if ( !playing )
		// {
			// if (state == PREPARED || state == STOPPED) {
				state = (loop ? LOOPING : PLAYING);
				mp.start();
			// } else {
				// state = (loop ? PENDING_LOOP : PENDING_PLAY);
			// }
		// }


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
	
	if (fadeToCallback != null) {
				Log.d(TAG, String.format("\n\nEND OF FADE SKIPPED\n\n"));

		fadeToCallback.setSuccess(false);

		try {
        	fadeToCallback.call();

    	} catch (Exception e) {
    		Log.d(TAG, String.format("\n\nEND OF FADE SKIPPED ERROR\n\n"));

    	}
    }
    if (fadeToTask != null) {
    	fadeToTask.cancel(true);
    	fadeToTask = null;
    }

    fadeToCallback = fadeToCb;
    
    fadeToTask = new FadeToTask();

    fadeToTask.setParams(to, duration, this);
	AsyncTaskCompat.executeParallel(fadeToTask);
    // .execute();
    // fto = to.floatValue;
    // fduration = duration.floatValue;
    // fsteps = floor(fduration/50);
	}

	public void setVolume(float volume) 
	{
        try
        {
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
			(new ResetNoLoop()).execute();
		} else {
			// oldMp = mPlayer;
			(new ResetLoop()).execute();
	
		}
	}


	private class FadeToTask extends AsyncTask<String, Void, String> {
		private float to;
		private int duration;
		private NativeAudioAssetComplex asset;

		@Override
        protected String doInBackground(String... params) {
        	Log.d(TAG, String.format("\n\nTASK STARTED\n\n"));
        	int steps = (int)(Math.floor(duration/100));


        	float increment = (to - v)/steps;

		    while (mp.isPlaying() && steps > 0 && Math.abs(to - v) > 0) {
	            asset.setVolume(v + increment) ;
	            steps--;
	            if (isCancelled()) {
	            	return "";
	            }
		    	try {
	            	Thread.sleep(100);
	        	} catch (Exception e) {

	        	}
        	
		    }
			if (fadeToCallback != null) {
				fadeToCallback.setSuccess(true);
				try {
					Log.d(TAG, String.format("\n\nEND OF FADE SUCCESS\n\n"));
		        	fadeToCallback.call();
		    	} catch (Exception e) {
		    		Log.d(TAG, String.format("\n\nEND OF FADE ERROR\n\n"));
		    	}
		    }
		    return "";

        }

		public void setParams(float t, int d, NativeAudioAssetComplex n) {
			to = t;
			duration = d;
			asset = n;
		}

	}

	private class ResetNoLoop extends AsyncTask<String, Void, String> {

		@Override
        protected String doInBackground(String... params) {
        	try {
            	Thread.sleep(500);
        	} catch (Exception e) {

        	}
        	

			mp.release();

			try {


				state = INVALID;
				mp = new CompatMediaPlayer();

		        mp.setOnPreparedListener(currentAsset);

		       	if (afd == null) {
		            mp.setDataSource(url);
		       	} else {
		       		mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
		       	}

				mp.setAudioStreamType(AudioManager.STREAM_MUSIC); 
				mp.setVolume(v, v);

				mp.prepareAsync();
				mp.setOnCompletionListener(currentAsset);



			if (completeCallback != null)
                completeCallback.call();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return "";
       }


    }
	 private class ResetLoop extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
        	try {
            	Thread.sleep(500);
        	} catch (Exception e) {

        	}
            mp.release();	  
            mp = nextMp;

            try {
				createNextMediaPlayer();
			} catch (IOException e) {
			}

			return "";
        }

    }
}
