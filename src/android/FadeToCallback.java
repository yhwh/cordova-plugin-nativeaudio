package com.rjfun.cordova.plugin.nativeaudio;

import java.util.concurrent.Callable;

public class FadeToCallback implements Callable<Void>{
    private boolean success = false;

    public Void call() throws Exception {          
        return null;
    }

    public void setSuccess(boolean s) {
        success = s;
    }

   	public boolean getSuccess() {
        return success;
    }

}