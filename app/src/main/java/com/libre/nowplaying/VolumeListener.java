package com.libre.nowplaying;

/**
 * Created by khajan on 4/8/15.
 */
public interface VolumeListener {

    /*this method is to add volume key listener and will give call back who has registered*/
//    void addVolumeKeyListener(OnChangeVolumeHardKeyListener volumeHardKeyListener);


    /*this method is to remove listener*/
//    void removeVolumeKeyListener(OnChangeVolumeHardKeyListener volumeHardKeyListener);

    interface OnChangeVolumeHardKeyListener {

        /*this method will get called whenever hardware volume is pressed*/
        void updateVolumeChangesFromHardwareKey(int keyCode);
    }
}
