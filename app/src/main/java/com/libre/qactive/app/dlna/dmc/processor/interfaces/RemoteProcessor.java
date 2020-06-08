package com.libre.qactive.app.dlna.dmc.processor.interfaces;

/**
 * Created by libre on 09-09-2014.
 */
public interface RemoteProcessor {

    void play_remote();
    void pause_remote();
    void stop_remote();
    String getAlbumname();
    String getArtist();
    float getcurrent_time();
    void SendCommand(byte[] cmd);



}
