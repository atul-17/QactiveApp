package com.libre.app.dlna.dmc.processor.impl;


import com.libre.app.dlna.dmc.processor.interfaces.DMRProcessor;

/**
 * Created by libre on 02-04-2015.
 */
public class RemoteProcessorImpl implements DMRProcessor {



   public RemoteProcessorImpl()
    {

    }
    @Override
    public void setURI(String uri, String musicinfo) {

    }

    @Override
    public void play() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void reset() {

    }

    @Override
    public void seek(String position) {

    }

    @Override
    public void seek(long position) {

    }

    @Override
    public void setVolume(int newVolume) {

    }

    @Override
    public int getVolume() {
        return 0;
    }

    @Override
    public int getMaxVolume() {
        return 0;
    }

    @Override
    public void addListener(DMRProcessorListener listener) {

    }

    @Override
    public void removeListener(DMRProcessorListener listener) {

    }

    @Override
    public void dispose() {

    }
}
