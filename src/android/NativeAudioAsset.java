//
//
//  NativeAudioAsset.java
//
//  Created by Sidney Bofah on 2014-06-26.
//

package com.rjfun.cordova.plugin.nativeaudio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import android.media.MediaPlayer;
import android.content.res.AssetFileDescriptor;

public class NativeAudioAsset
{

	private ArrayList<NativeAudioAssetComplex> voices;
	private int playIndex = 0;
	
	public NativeAudioAsset(AssetFileDescriptor afd, String uri, int numVoices, float volume) throws IOException
	{
		voices = new ArrayList<NativeAudioAssetComplex>();
		
		if ( numVoices < 0 )
			numVoices = 1;
		
		for ( int x=0; x<numVoices; x++) 
		{
			NativeAudioAssetComplex voice = new NativeAudioAssetComplex(afd, uri, volume);
			voices.add( voice );
		}
	}
	
	public void play(Callable<Void> completeCb) throws IOException
	{
		NativeAudioAssetComplex voice = voices.get(playIndex);
		voice.play(completeCb);
		playIndex++;
		playIndex = playIndex % voices.size();
	}

	public boolean pause()
	{
		boolean wasPlaying = false;
		for ( int x=0; x<voices.size(); x++)
		{
				NativeAudioAssetComplex voice = voices.get(x);
				wasPlaying |= voice.pause();
		}
		return wasPlaying;
	}

	public void resume()
	{
		// only resumes first instance, assume being used on a stream and not multiple sfx
		if (voices.size() > 0)
		{
				NativeAudioAssetComplex voice = voices.get(0);
				voice.resume();
		}
	}

	public void chain(NativeAudioAsset asset2, boolean loop)  {
		if (voices.size() > 0)
		{
			NativeAudioAssetComplex voice = voices.get(0);
			if (loop) {
				asset2.prepareLoop();
			}
			asset2.chainWithPrev(voice);
		}
	}

	public void chainWithPrev(NativeAudioAssetComplex p)  {
		if (voices.size() > 0)
		{
			NativeAudioAssetComplex voice = voices.get(0);
			voice.chainWithPrev(p);
		}
	}

	public void prepareLoop()  {
		if (voices.size() > 0)
		{
			NativeAudioAssetComplex voice = voices.get(0);
			voice.prepareLoop();
		}
	}

	public void setCompleteCb (Callable<Void> completeCb) {
		if (voices.size() > 0)
		{
			NativeAudioAssetComplex voice = voices.get(0);
			voice.setCompleteCb(completeCb);
		}
	}

	public void setLoadCb (Callable<Void> loadCb) {
		if (voices.size() > 0)
		{
			NativeAudioAssetComplex voice = voices.get(0);
			voice.setLoadCb(loadCb);
		}
	}
	public MediaPlayer getPlayer() {
		if (voices.size() > 0)
		{
			NativeAudioAssetComplex voice = voices.get(0);
			return voice.getPlayer();
		}	
		return null;
	}

    public void stop()
	{
		for ( int x=0; x<voices.size(); x++) 
		{
			NativeAudioAssetComplex voice = voices.get(x);
			voice.stop();
		}
	}
	
	public void loop(int count) throws IOException
	{
		NativeAudioAssetComplex voice = voices.get(playIndex);
		voice.loop(count);
		playIndex++;
		playIndex = playIndex % voices.size();
	}


	
	public void unload() throws IOException
	{
		this.stop();
		for ( int x=0; x<voices.size(); x++) 
		{
			NativeAudioAssetComplex voice = voices.get(x);
			voice.unload();
		}
		voices.removeAll(voices);
	}
	
	public void setVolume(float volume)
	{
		for (int x = 0; x < voices.size(); x++)
		{
			NativeAudioAssetComplex voice = voices.get(x);
			voice.setVolume(volume);
		}
	}
}