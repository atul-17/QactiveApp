package com.libre.nowplaying;

import com.libre.SceneObject;

/**
 * Created by khajan on 4/8/15.
 */
public interface FragmentListner {

    /**
     * TODO Khajan
     *
     * @param sceneObject
     */
    /*this method is written to post SceneObject from activity to fragment while scrolling now playing as viewpager
    * loads to pages at a time*/

    void postSceneObject(SceneObject sceneObject);
}
