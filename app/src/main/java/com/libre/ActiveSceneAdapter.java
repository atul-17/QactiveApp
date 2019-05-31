package com.libre;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cumulations.libreV2.AppConstants;
import com.cumulations.libreV2.activity.CTDMSBrowserActivityV2;
import com.libre.LErrorHandeling.LibreError;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.app.dlna.dmc.utility.PlaybackHelper;
import com.libre.app.dlna.dmc.utility.UpnpDeviceManager;
import com.libre.constants.CommandType;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.netty.BusProvider;
import com.libre.nowplaying.NowPlayingActivity;
import com.libre.util.LibreLogger;
import com.libre.util.PicassoTrustCertificates;
import com.squareup.picasso.MemoryPolicy;

import org.fourthline.cling.model.meta.RemoteDevice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Created by karunakaran on 7/22/2015.
 */
public class ActiveSceneAdapter extends ArrayAdapter<SceneObject> {

    public static final String SCENE_MAP = "scene_map";
    public static final String CURRENT_IPADDRESS = Constants.CURRENT_DEVICE_IP;


    public static final int PREPARATION_INIT = 0x77;
    public static final java.lang.String SCENE_POSITION = "scene_position";

    public static LinkedHashMap<String, SceneObject> sceneMap = new LinkedHashMap<>();
    Context context;

    Handler activehandler;
    public ActiveSceneAdapter(Context context, int textViewResourceId,Handler handler) {
        super(context, textViewResourceId);
        this.context = context;
       activehandler=handler;
    }
    public static int getCountNumberofDevicesInAdapter(){
        try {

            return sceneMap.keySet().toArray().length;
        } catch (Exception e) {

            return 0;
        }
    }
    public boolean checkSceneobjectIsPresentinList(String mNodeIp) {
        return sceneMap.containsKey(mNodeIp);
    }

    public LinkedHashMap getSceneMap(){
        return this.sceneMap;
    }

    @Override
    public int getCount() {

        try {

            return sceneMap.keySet().toArray().length;
        } catch (Exception e) {

            return 0;
        }
    }

    @Override
    public void add(SceneObject object) {


        /*if((object.getCurrentSource() == MIDCONST.GCAST_SOURCE)
            && (object.getPlaystatus() == SceneObject.CURRENTLY_STOPPED)){

        }else*/{
            sceneMap.put(object.getIpAddress(), object);
            super.add(object);
        }
    }

    @Override
    public void addAll(Collection<? extends SceneObject> collection) {
        super.addAll(collection);

        Iterator<? extends SceneObject> iterator = collection.iterator();
        while (iterator.hasNext()) {
            SceneObject sceneObject = iterator.next();
            sceneMap.put(sceneObject.getIpAddress(), sceneObject);
        }
    }


    @Override
    public void insert(SceneObject object, int index) {
        super.insert(object, index);
    }

    @Override
    public void sort(Comparator<? super SceneObject> comparator) {
        super.sort(comparator);
    }

    @Override
    public void setNotifyOnChange(boolean notifyOnChange) {
        super.setNotifyOnChange(notifyOnChange);
    }

    @Override
    public void notifyDataSetChanged() {

        super.notifyDataSetChanged();
    }

    @Override
    public Context getContext() {
        return super.getContext();
    }

    @Override
    public SceneObject getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public int getPosition(SceneObject item) {
        return super.getPosition(item);
    }

    @Override
    public void setDropDownViewResource(int resource) {
        super.setDropDownViewResource(resource);
    }

    @Override
    public Filter getFilter() {
        return super.getFilter();
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return super.getDropDownView(position, convertView, parent);
    }

    @Override
    public void clear() {
        sceneMap.clear();
        super.clear();

        sceneMap = new LinkedHashMap<>();
    }

    @Override
    public void remove(SceneObject object) {
        LibreLogger.d(this, "ActiveSceneList: Device has been removed");
        if(checkSceneobjectIsPresentinList(object.getIpAddress())) {
            sceneMap.remove(object.getIpAddress());
        }

        super.remove(object);

    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    public class Holder {
        TextView SceneName, trackName, NumberOfDevice;
        ImageView albumArt;
        ImageButton mute;
        SeekBar volumebar;
        ImageButton play, next, previous;
        String ipaddress;
        ImageView mutebutton;
        ProgressBar preparationProgressBar;

        TextView sourceName;
        ImageView sourceImage;

        /* Added to handle the case where trackname is empty string"*/
        String currentTrackName = "-1";
        int previousSourceIndex;

    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final Holder holder;
        final ScanningHandler mScanHandler = ScanningHandler.getInstance();
        // mScanHandler.updateSlaveToMasterHashMap();
        if (convertView == null) {

            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.scenes_listview_layout, null);

            holder = new Holder();

            holder.albumArt = (ImageView) convertView.findViewById(R.id.albumart);
            holder.SceneName = (TextView) convertView.findViewById(R.id.name);
            holder.trackName = (TextView) convertView.findViewById(R.id.trackName);
            holder.NumberOfDevice = (TextView) convertView.findViewById(R.id.numberOfDevice);
            holder.volumebar = (SeekBar) convertView.findViewById(R.id.seekBar);
            holder.play = (ImageButton) convertView.findViewById(R.id.media_btn_play);
            holder.previous = (ImageButton) convertView.findViewById(R.id.media_btn_previous);
            holder.next = (ImageButton) convertView.findViewById(R.id.media_btn_nxt);
            holder.mutebutton = (ImageView) convertView.findViewById(R.id.mute_button);

            holder.preparationProgressBar = (ProgressBar) convertView.findViewById(R.id.progress_loader);
            String ipaddress = (String) sceneMap.keySet().toArray()[position];
            holder.ipaddress = ipaddress;
            holder.sourceImage = (ImageView) convertView.findViewById(R.id.source_image);
            holder.sourceName = (TextView) convertView.findViewById(R.id.source_name);
            holder.currentTrackName = "-1";
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        /* Resetting everytimne the adapter is called so that we can handle source switch between aux to others */
        holder.previous.setClickable(true);
        holder.next.setClickable(true);


        holder.ipaddress = (String) sceneMap.keySet().toArray()[position];
        ;

        holder.NumberOfDevice.setText(context.getString(R.string.speakers) + mScanHandler.getNumberOfSlavesForMasterIp(holder.ipaddress, mScanHandler.getconnectedSSIDname(context)));
        /*LSSDPNodes mNode = mScanHandler.getLSSDPNodeFromCentralDB(holder.ipaddress);
        if (mNode != null) {
            LibreLogger.d("com.libre.Scanning", "Number of Devices For MasterIp: " + holder.ipaddress + "Device State " + mNode.getDeviceState());
            if (mNode.getDeviceState().contains("M")) {
                ArrayList<String> mSlaveList = mScanHandler.getSlaveListFromMasterIp(holder.ipaddress);
                LibreLogger.d("com.libre.Scanning", "Number of Devices For MasterIp: " + holder.ipaddress + "Number of Devices " + mSlaveList.size());
                int mNumberOfDevices = mSlaveList.size() + 1;
                holder.NumberOfDevice.setText("Speakers : " + mNumberOfDevices);
            }
        } else {
            LibreLogger.d("com.libre.Scanning", "Number of Devices For MasterIp: " + holder.ipaddress + "mNode is Null " + "null");
        }*/


        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sceneMap.size() > 0) {
                    ArrayList<String> arrayList = new ArrayList<>();

                    for (String key : sceneMap.keySet()) {
                        arrayList.add(key);
                    }

                    context.startActivity(new Intent(context, NowPlayingActivity.class)
                            .putExtra(CURRENT_IPADDRESS, holder.ipaddress)
                            .putExtra(SCENE_MAP, arrayList)
                            .putExtra(SCENE_POSITION, position));
                }
                else{
                    /**notifying adapter as sometime scene is present but not lssdp node*/
                    notifyDataSetChanged();
                }
            }
        });

        holder.next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Next", "" + position);
                LSSDPNodes mNodeWeGotForControl = mScanHandler.getLSSDPNodeFromCentralDB(holder.ipaddress);
                if(mNodeWeGotForControl==null)
                    return;
                SceneObject scene = sceneMap.get(holder.ipaddress);
                if(scene!=null
                        && scene.getCurrentSource() == Constants.AUX_SOURCE
                        || scene.getCurrentSource() == Constants.GCAST_SOURCE
                        || scene.getCurrentSource() == Constants.VTUNER_SOURCE
                        || scene.getCurrentSource() == Constants.TUNEIN_SOURCE
                        || (scene.getCurrentSource() == Constants.BT_SOURCE
                        && ((mNodeWeGotForControl.getgCastVerision() == null
                        && ((mNodeWeGotForControl.getBT_CONTROLLER() == 4)
                        || mNodeWeGotForControl.getBT_CONTROLLER() == 0))
                        ||(mNodeWeGotForControl.getgCastVerision() != null
                        && (mNodeWeGotForControl.getBT_CONTROLLER() < 2))))){
                    LibreError error = new LibreError("", Constants.NEXT_PREVIOUS_NOT_ALLOWED,1);
                    BusProvider.getInstance().post(error);
                    return ;
                }

                /*if (scene.getCurrentSource()== Constants.ALEXA_SOURCE){
                    doNextPrevious(true,scene);
                    return;
                }*/

                if (scene!=null && isActivePlayistNotAvailable(scene)){
                    LibreLogger.d(this,"currently not playing, so take user to sources option activity");
                    gotoSourcesOption(holder.ipaddress,scene.getCurrentSource());
                    return;
                }

                doNextPrevious(true, scene);
            }
        });
        holder.previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("previous", "" + position);
                /*if (holder.ipaddress!=null) {
                    context.startActivity(new Intent(context, AudioRecordDemoActivity.class)
                            .putExtra("ip", holder.ipaddress));
                    return;
                }*/
                LSSDPNodes mNodeWeGotForControl = mScanHandler.getLSSDPNodeFromCentralDB(holder.ipaddress);

                if(mNodeWeGotForControl==null)
                    return;
                SceneObject scene = sceneMap.get(holder.ipaddress);
                if(scene!=null
                        && scene.getCurrentSource() == Constants.AUX_SOURCE
                        || scene.getCurrentSource() == Constants.GCAST_SOURCE
                        || scene.getCurrentSource() == Constants.VTUNER_SOURCE
                        || scene.getCurrentSource() == Constants.TUNEIN_SOURCE
                        || (scene.getCurrentSource() == Constants.BT_SOURCE
                        && ((mNodeWeGotForControl.getgCastVerision() == null
                        && ((mNodeWeGotForControl.getBT_CONTROLLER() == 4)
                        || mNodeWeGotForControl.getBT_CONTROLLER() == 0))
                        ||(mNodeWeGotForControl.getgCastVerision() != null
                        && (mNodeWeGotForControl.getBT_CONTROLLER() < 2))))){
                    LibreError error = new LibreError("", Constants.NEXT_PREVIOUS_NOT_ALLOWED,1);
                    BusProvider.getInstance().post(error);
                    return ;
                }

                if (scene.getCurrentSource()== Constants.ALEXA_SOURCE){
                    doNextPrevious(false,scene);
                    return;
                }

                if (isActivePlayistNotAvailable(scene)){
                    LibreLogger.d(this,"currently not playing, so take user to sources option activity");
                    gotoSourcesOption(holder.ipaddress,scene.getCurrentSource());
                    return;
                }
                doNextPrevious(false, scene);

            }
        });
        holder.play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LSSDPNodes mNodeWeGotForControl = mScanHandler.getLSSDPNodeFromCentralDB(holder.ipaddress);
                if(mNodeWeGotForControl==null)
                    return;
                SceneObject scene = sceneMap.get(holder.ipaddress);
                if(scene!=null
                        && scene.getCurrentSource() == Constants.AUX_SOURCE
                        || scene.getCurrentSource() == Constants.GCAST_SOURCE
                        || scene.getCurrentSource() == Constants.VTUNER_SOURCE
                        || scene.getCurrentSource() == Constants.TUNEIN_SOURCE
                        || (scene.getCurrentSource() == Constants.BT_SOURCE
                        && ((mNodeWeGotForControl.getgCastVerision() == null
                        && ((mNodeWeGotForControl.getBT_CONTROLLER() == 4)
                        || mNodeWeGotForControl.getBT_CONTROLLER() == 0))
                        ||(mNodeWeGotForControl.getgCastVerision() != null
                        && (mNodeWeGotForControl.getBT_CONTROLLER() < 2))))){
                    LibreError error = new LibreError("", Constants.PLAY_PAUSE_NOT_ALLOWED,1);//TimeoutValue !=0 means ,its VERYSHORT
                    BusProvider.getInstance().post(error);
                    return ;
                }


         /*       if (scene!=null && scene.getCurrentSource() == 2) {
                    LibreLogger.d(this,"current source is DMR");
                    RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(scene.getIpAddress());
                    if (renderingDevice != null) {
                        String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                        PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);

                        if ( playbackHelper==null|| playbackHelper.getDmsHelper() == null||(scene!=null && false==scene.getPlayUrl().contains(LibreApplication.LOCAL_IP))) {
                            Toast.makeText(context, "No active playlist,\nPlease select a song to play", Toast.LENGTH_SHORT).show();


                    *//* In SA mode we need to go to local content while in HN mode we will go to sources option *//*

                            if (LibreApplication.connectedSSID.equalsIgnoreCase(Constants.DDMS_SSID)) {
                                Intent localIntent = new Intent(context, LocalDMSActivity.class);
                                localIntent.putExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, true);
                                localIntent.putExtra(Constants.CURRENT_DEVICE_IP, scene.getIpAddress());
                                context.startActivity(localIntent);
                            } else {
                                Intent localIntent = new Intent(context, SourcesOptionActivity.class);
                                localIntent.putExtra(Constants.CURRENT_DEVICE_IP, scene.getIpAddress());
                                localIntent.putExtra("current_source", "" + scene.getCurrentSource());
                                context.startActivity(localIntent);
                            }
                            return;
                        }

                    }
                    else if (scene!=null&& scene.getPlayUrl()!=null && scene.getPlayUrl().contains(LibreApplication.LOCAL_IP)){
                        *//* renderer is not found and hence there is no playlist also so open the sources option *//*


                                            *//* In SA mode we need to go to local content while in HN mode we will go to sources option *//*

                        if (LibreApplication.connectedSSID.equalsIgnoreCase(Constants.DDMS_SSID)) {
                            Intent localIntent = new Intent(context, LocalDMSActivity.class);
                            localIntent.putExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, true);
                            localIntent.putExtra(Constants.CURRENT_DEVICE_IP, scene.getIpAddress());
                            context.startActivity(localIntent);
                        } else {
                            Intent localIntent = new Intent(context, SourcesOptionActivity.class);
                            localIntent.putExtra(Constants.CURRENT_DEVICE_IP, scene.getIpAddress());
                            localIntent.putExtra("current_source", "" + scene.getCurrentSource());
                            context.startActivity(localIntent);
                        }
                        return;
                    }

                }*/

    /* For Playing , If DMR is playing then we should give control for Play/Pause*/
                if (scene.getCurrentSource()== Constants.NO_SOURCE || scene.getCurrentSource()==Constants.DDMSSLAVE_SOURCE ||
                        (scene.getCurrentSource()== Constants.DMR_SOURCE && (scene.getPlaystatus() == SceneObject.CURRENTLY_STOPPED
                                || scene.getPlaystatus() == SceneObject.CURRENTLY_NOTPLAYING))){
                    LibreLogger.d(this,"currently not playing, so take user to sources option activity");
                    gotoSourcesOption(holder.ipaddress,scene.getCurrentSource());
                    return;
                }

                LibreLogger.d(this,"current source is not DMR"+scene.getCurrentSource());




                if (scene!=null && scene.getPlaystatus() == SceneObject.CURRENTLY_PLAYING) {
                    LUCIControl.SendCommandWithIp(MIDCONST.MID_PLAYCONTROL, LUCIMESSAGES.PAUSE, CommandType.SET, holder.ipaddress);
                 //   scene.setPlaystatus(SceneObject.CURRENTLY_PAUSED);
                    holder.play.setImageResource(R.mipmap.play_with_gl);
                    holder.next.setImageResource(R.mipmap.next_without_glow);
                    holder.previous.setImageResource(R.mipmap.prev_without_glow);
                    holder.mutebutton.setImageResource(R.mipmap.mut_without_glow);

                } else {
                    if(scene!=null && scene.getCurrentSource() == 19){ /* Change Done By Karuna, Because for BT Source there is no RESUME*/
                        LUCIControl.SendCommandWithIp(MIDCONST.MID_PLAYCONTROL, LUCIMESSAGES.PLAY, CommandType.SET, holder.ipaddress);
                    }else {
                        LUCIControl.SendCommandWithIp(MIDCONST.MID_PLAYCONTROL, LUCIMESSAGES.RESUME, CommandType.SET, holder.ipaddress);
                    }
                 //   scene.setPlaystatus(SceneObject.CURRENTLY_PLAYING);
                    holder.play.setImageResource(R.mipmap.pause_with_glow);
                    holder.next.setImageResource(R.mipmap.next_with_glow);
                    holder.previous.setImageResource(R.mipmap.prev_with_glow);
                    holder.mutebutton.setImageResource(R.mipmap.mut_with_glow);


                }

            }
        });
        holder.volumebar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d("onProgresChanged" + progress, "" + position);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d("onStartTracking", "" + position);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                LibreLogger.d("onStopTracking", "" + position);
                SceneObject scene = sceneMap.get(holder.ipaddress);
                if(scene.getCurrentSource() != Constants.AIRPLAY_SOURCE) {
                    LUCIControl.SendCommandWithIp(MIDCONST./*ZONE_VOLUME*/VOLUME_CONTROL, "" + seekBar.getProgress(), CommandType.SET, holder.ipaddress);
                }else{
                    LUCIControl.SendCommandWithIp(MIDCONST.VOLUME_CONTROL, "" + seekBar.getProgress(), CommandType.SET, holder.ipaddress);
                }
                //LUCIControl.SendCommandWithIp(MIDCONST.VOLUME_CONTROL, "" + seekBar.getProgress(), CommandType.SET, holder.ipaddress);
                SceneObject sceneObjectFromCentralRepo = mScanHandler.getSceneObjectFromCentralRepo(holder.ipaddress);

                if (scene != null) {
//                    scene.setvolumeZoneInPercentage(seekBar.getProgress());
                    scene.setVolumeValueInPercentage(seekBar.getProgress());
                    sceneMap.put(holder.ipaddress, scene);
                    if (sceneObjectFromCentralRepo != null) {
                        sceneObjectFromCentralRepo.setVolumeValueInPercentage(seekBar.getProgress());
//                        sceneObjectFromCentralRepo.setvolumeZoneInPercentage(seekBar.getProgress());
                        mScanHandler.putSceneObjectToCentralRepo(holder.ipaddress, sceneObjectFromCentralRepo);
                    }
                }
            }
        });

        try {
            String ipaddress = (String) sceneMap.keySet().toArray()[position];
            SceneObject scene = sceneMap.get(ipaddress);


/*
//            if (CTDeviceDiscoveryActivity.ZONE_VOLUME_MAP.containsKey(ipaddress)) {
//                holder.volumebar.setProgress(CTDeviceDiscoveryActivity.ZONE_VOLUME_MAP.get(ipaddress));
//            } else {
//
//                LUCIControl control = new LUCIControl(ipaddress);
//                control.SendCommand(MIDCONST.ZONE_VOLUME, null, LSSDPCONST.LUCI_GET);
//                if (scene.getvolumeZoneInPercentage() >= 0)
//                    holder.volumebar.setProgress(scene.getvolumeZoneInPercentage());
//
//            }*/

            /*if (scene.getVolumeValueInPercentage() >= 0)
                holder.volumebar.setProgress(scene.getVolumeValueInPercentage());*/
            LibreLogger.d(this, "Scene Ipaddress " + ipaddress + "Scene Ipaddressss" + scene.getIpAddress());
            LibreLogger.d(this, "Scene Ipaddress " + scene.getSceneName());

            /* Fix by KK , When Album art is not updating properly */
            if(holder.currentTrackName==null)
                holder.currentTrackName = "";


           /* if (scene.getCurrentSource() != 14) {*/ /* Karuna Commenting For the Single Function to Update the UI*/
            if (scene != null) {
                if (scene.getSceneName() != null && !scene.getSceneName().equalsIgnoreCase("NULL")) {
                    holder.SceneName.setText(scene.getSceneName());
                }
                if (scene.getTrackName() != null && !scene.getTrackName().equalsIgnoreCase("NULL") && !scene.getTrackName().equalsIgnoreCase("")) {

                    String trackname=scene.getTrackName();

                    /* This change is done to handle the case of deezer where the song name is appended by radio or skip enabled */
                    if (trackname!=null&&trackname.contains(Constants.DEZER_RADIO))
                        trackname=trackname.replace(Constants.DEZER_RADIO,"");

                    if (trackname!=null&&trackname.contains(Constants.DEZER_SONGSKIP))
                        trackname=trackname.replace(Constants.DEZER_SONGSKIP,"");
                    holder.trackName.setText(scene.getTrackName());
                }else if(scene.getAlbum_name()!=null&&!scene.getAlbum_name().equalsIgnoreCase("null")) {
                    holder.trackName.setText(scene.getAlbum_name());
                }else {
                    holder.trackName.setText("");
                }


                    /*this is to show loading dialog while we are preparing to play*/
                showPreparingDialog(scene, holder);

                if (scene.getPreparingState() == SceneObject.PREPARING_STATE.PREPARING_INITIATED) {

                }
					/* Commented Because Volume Have to update For All Sources */
             /*       if (LibreApplication.ZONE_VOLUME_MAP.containsKey(ipaddress)) {
                        holder.volumebar.setProgress(LibreApplication.ZONE_VOLUME_MAP.get(ipaddress));
                    } else {

                        LUCIControl control = new LUCIControl(ipaddress);
                        control.SendCommand(MIDCONST.ZONE_VOLUME, null, LSSDPCONST.LUCI_GET);
                        if (scene.getvolumeZoneInPercentage() >= 0)
                            holder.volumebar.setProgress(scene.getvolumeZoneInPercentage());

                    }*/

                    /*
                    if (scene.getVolumeValueInPercentage() >= 0)
                        holder.volumebar.setProgress(scene.getVolumeValueInPercentage());
*/

                String album_url = "";
                if ((scene.getCurrentSource() != Constants.AUX_SOURCE
                    && scene.getCurrentSource() != Constants.BT_SOURCE
                    && scene.getCurrentSource() != Constants.GCAST_SOURCE)
                    &&(
                        holder.currentTrackName!=null
                        && scene.getTrackName() != null
                        && ((!holder.currentTrackName.equalsIgnoreCase(scene.getTrackName()))
                        ||holder.previousSourceIndex!=scene.getCurrentSource())) ){

                     /* Added to handle the case where trackname is empty string"*/
//                        if (scene.getTrackName().trim().length() > 0)
//                            holder.currentTrackName = scene.getTrackName();
                    holder.currentTrackName = scene.getTrackName();
                    /*Album Art For All other Sources Except */
                    if (scene.getAlbum_art() != null &&
                            scene.getAlbum_art().equalsIgnoreCase("coverart.jpg")) {

                        if (scene.getCurrentSource() == /*alexa*/Constants.ALEXA_SOURCE){
                            holder.albumArt.setImageDrawable(context.getResources().getDrawable(R.drawable.amazonlogo_playing));
                        } else {
                            album_url = "http://" + scene.getIpAddress() + "/" + "coverart.jpg";

                        /* If Track Name is Different just Invalidate the Path
                        * And if we are resuming the Screen(Screen OFF and Screen ON) , it will not re-download it */
                            boolean mInvalidated = mInvalidateTheAlbumArt(scene,album_url);
                            LibreLogger.d(this,"Invalidated the URL " + album_url + " Status "+ mInvalidated);

                            PicassoTrustCertificates.getInstance(context).load(album_url)
                                /*.memoryPolicy(MemoryPolicy.NO_CACHE).networkPolicy(NetworkPolicy.NO_CACHE)*/
                                    .placeholder(R.mipmap.album_art)
                                    .error(R.mipmap.album_art)
                                    .into(holder.albumArt);
                        }
                    } else if (scene.getAlbum_art() == null || "".equals(scene.getAlbum_art().trim())) {
                        if (scene.getCurrentSource() == Constants.ALEXA_SOURCE/*alexa*/){
                            holder.albumArt.setImageDrawable(context.getResources().getDrawable(R.drawable.amazonlogo_playing));
                        } else {
                            holder.albumArt.setImageDrawable(context.getResources().getDrawable(R.mipmap.album_art));
                        }
                    } else {
                        album_url = scene.getAlbum_art();

                        /* Here also we have To invalidate it , because Samsung Native Player always having
                        * a same URI for sending the Album Art */
                        boolean mInvalidated = mInvalidateTheAlbumArt(scene,album_url);
                        LibreLogger.d(this,"Invalidated the URL " + album_url + " Status "+ mInvalidated);
                        if (scene.getCurrentSource() == Constants.ALEXA_SOURCE/*alexa*/){
                            holder.albumArt.setImageDrawable(context.getResources().getDrawable(R.drawable.amazonlogo_playing));
                        }else if (!album_url.trim().equalsIgnoreCase("")) {
                            PicassoTrustCertificates.getInstance(context).load(album_url)
                                 /*   .memoryPolicy(MemoryPolicy.NO_CACHE).networkPolicy(NetworkPolicy.NO_CACHE)*/
                                    .placeholder(R.mipmap.album_art)
                                    .error(R.mipmap.album_art)
                                    .into(holder.albumArt);
                        }
                    }
                } else {
                    if (scene.getCurrentSource() == Constants.ALEXA_SOURCE/*alexa*/){
                        if (scene.getAlbum_art()!=null){
                            PicassoTrustCertificates.getInstance(context).load(scene.getAlbum_art())
                                 /*   .memoryPolicy(MemoryPolicy.NO_CACHE).networkPolicy(NetworkPolicy.NO_CACHE)*/
                                    .placeholder(R.mipmap.album_art)
                                    .error(R.mipmap.album_art)
                                    .into(holder.albumArt);
                        }else{
                            holder.albumArt.setImageDrawable(context.getResources().getDrawable(R.drawable.amazonlogo_playing));
                        }
                    } else {
                        PicassoTrustCertificates.getInstance(context).load(R.mipmap.album_art);
                    }

                }

//                     /* This change is done to ensure that the view is previously had aux on and now its swithced and hence image need to be handled*/
//                else if (holder.previousSourceIndex == 14 && scene.getCurrentSource() != 14 && scene.getCurrentSource() != 19) {
//                    String album_arturl = mScanHandler.getSceneObjectFromCentralRepo(ipaddress).getAlbum_art();
//                    if (album_arturl != null && album_arturl.contains("http://")) {
//                    } else {
//                        album_arturl = "http://" + ipaddress + "/" + "coverart.jpg";
//                    }
//                    Picasso.with(context).invalidate(album_arturl);
//
//                }

                if (scene.getPlaystatus() == SceneObject.CURRENTLY_PLAYING) {
                    holder.play.setImageResource(R.mipmap.pause_with_glow);
                    holder.next.setImageResource(R.mipmap.next_with_glow);
                    holder.previous.setImageResource(R.mipmap.prev_with_glow);
                } else {
                    holder.play.setImageResource(R.mipmap.play_with_gl);
                    holder.next.setImageResource(R.mipmap.next_without_glow);
                    holder.previous.setImageResource(R.mipmap.prev_without_glow);
                }
                if (scene.getCurrentSource() == 18) {
                    seekBarNextPreviousView(holder, false);
                }

                if (scene.getCurrentSource() == Constants.ALEXA_SOURCE){
                    clearViewsForAlexa(holder,scene);
                }
            }
/*
            } else {
                holder.play.setClickable(false);
                holder.play.setEnabled(false);
                holder.play.setImageResource(R.mipmap.play_with_gl);

                holder.mutebutton.setClickable(false);
                holder.mutebutton.setEnabled(false);
                holder.previous.setClickable(false);
                holder.previous.setEnabled(false);
                holder.next.setEnabled(false);
                holder.next.setClickable(false);
                holder.albumArt.setClickable(false);
                holder.albumArt.setImageDrawable(context.getResources().getDrawable(R.mipmap.album_art));
                if (scene.getTrackName() != null) {
                    holder.trackName.setText(scene.getTrackName());
                }
                */
/* To fix the Aux switch issue -Praveena*//*

                if (scene.getCurrentSource() == 19) {
                    holder.trackName.setText("BT Playing");
                } else {
                    holder.trackName.setText("Aux Playing");
                }


            }
*/
            /* Updating the Volume For AUX / BT is not Updating in ActiveScenes */
            if(scene!=null) {
                if(scene.getCurrentSource()!=Constants.AIRPLAY_SOURCE) {
                    if (LibreApplication./*ZONE_VOLUME_MAP*/INDIVIDUAL_VOLUME_MAP.containsKey(ipaddress)) {
                        holder.volumebar.setProgress(LibreApplication./*ZONE_VOLUME_MAP*/INDIVIDUAL_VOLUME_MAP.get(ipaddress));
                    } else {

                        LUCIControl control = new LUCIControl(ipaddress);
                        control.SendCommand(MIDCONST./*ZONE_VOLUME*/VOLUME_CONTROL, null, LSSDPCONST.LUCI_GET);
                        if (scene.getVolumeValueInPercentage() >= 0)
                            holder.volumebar.setProgress(scene.getVolumeValueInPercentage());

                    }
                }else{/* For Airplay We havve to send and updating only 64*/
                    if (LibreApplication.INDIVIDUAL_VOLUME_MAP.containsKey(ipaddress)) {
                        holder.volumebar.setProgress(LibreApplication.INDIVIDUAL_VOLUME_MAP.get(ipaddress));
                    } else {

                        LUCIControl control = new LUCIControl(ipaddress);
                        control.SendCommand(MIDCONST.VOLUME_CONTROL, null, LSSDPCONST.LUCI_GET);
                        if (scene.getVolumeValueInPercentage() >= 0)
                            holder.volumebar.setProgress(scene.getVolumeValueInPercentage());

                    }
                }

                playPauseNextPrevious(holder,scene);
            }
            /* This line should always be here do not move this line above the ip-else loop where we check the current Source*/
            holder.previousSourceIndex = scene.getCurrentSource();

            setTheSourceIconFromCurrentSceneObject(scene, holder);

        } catch (Exception e) {
            e.printStackTrace();
        }


        if (position == getCount()) {
            notifyDataSetChanged();
        }
        doOrDisableViewsForGcast(holder, LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(holder.ipaddress));
        return convertView;

    }

    private void clearViewsForAlexa(Holder holder,SceneObject sceneObject) {
        ((ActiveScenesListActivity)context).setControlIconsForAlexa(sceneObject,holder.play,holder.next,holder.previous);

       // holder.trackName.setText("");
//        holder.SceneName.setText("");
        holder.NumberOfDevice.setVisibility(View.INVISIBLE);

    }

    private void doOrDisableViewsForGcast(Holder holder, LSSDPNodes theNodeBasedOnTheIpAddress) {
        if(theNodeBasedOnTheIpAddress!=null && theNodeBasedOnTheIpAddress.getCurrentSource()==MIDCONST.GCAST_SOURCE){
            holder.play.setImageResource(R.mipmap.play_with_gl);
            holder.next.setImageResource(R.mipmap.next_without_glow);
            holder.previous.setImageResource(R.mipmap.prev_without_glow);
            if (theNodeBasedOnTheIpAddress.getCurrentSource() == Constants.ALEXA_SOURCE){
                holder.albumArt.setImageResource(R.drawable.amazonlogo_playing);
            } else {
                holder.albumArt.setImageResource(R.mipmap.album_art);
            }
        }
    }

    private boolean isActivePlayistNotAvailable(SceneObject sceneObject){
    if (sceneObject.getCurrentSource()==Constants.NO_SOURCE
            || (sceneObject.getCurrentSource()==Constants.DMR_SOURCE && ScanningHandler.getInstance().ToBeNeededToLaunchSourceScreen(sceneObject))){
            return true;
        }
        return false;
    }

    private void gotoSourcesOption(String ipaddress, int currentSource) {
        Intent mActiveScenesList = new Intent(context, SourcesOptionActivity.class);
        mActiveScenesList.putExtra(Constants.CURRENT_DEVICE_IP, ipaddress);
        mActiveScenesList.putExtra("current_source", "" + currentSource);

        LibreError error = new LibreError("", context.getString(R.string.no_active_playlist),1);
        BusProvider.getInstance().post(error);

        context.startActivity(mActiveScenesList);
    }
    private boolean isLocalDMR(SceneObject sceneObject){
        RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(sceneObject.getIpAddress());
        if (renderingDevice != null) {
            String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
            PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
            /* If its Playing in the Same Mobile then it will give Retun True
            * otherwise false;*/
            boolean mIsDmrPlaying = ScanningHandler.getInstance().ToBeNeededToLaunchSourceScreen(sceneObject);
            LibreLogger.d(this," local DMR is playing" + mIsDmrPlaying);
            return mIsDmrPlaying;
            /*if (playbackHelper == null)
                return false;
            if (playbackHelper.getDmsHelper() == null) {
                return false;
            }*/


        }
   /*     if(!LibreApplication.LOCAL_IP.equals("") && sceneObject.getPlayUrl().contains(LibreApplication.LOCAL_IP)){
            return true;
        }*/
        return false;
    }

    private boolean mInvalidateTheAlbumArt(SceneObject scene,String album_url){
        if( !scene.getmPreviousTrackName().equalsIgnoreCase(scene.getTrackName())) {

            PicassoTrustCertificates.getInstance(context).invalidate(album_url);
            scene.setmPreviousTrackName(scene.getTrackName());
            SceneObject sceneObjectFromCentralRepo = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(scene.getIpAddress());
            if (sceneObjectFromCentralRepo != null) {
                sceneObjectFromCentralRepo.setmPreviousTrackName(scene.getTrackName());
            }

            return true;
        }
        return false;
    }
    /*this dialog is to show small progress*/
    private void showPreparingDialog(SceneObject scene, Holder holder) {
        if (scene != null) {
            if (scene.getPreparingState() == SceneObject.PREPARING_STATE.PREPARING_INITIATED) {
                holder.preparationProgressBar.getIndeterminateDrawable().setColorFilter(0xFFE15D29, android.graphics.PorterDuff.Mode.MULTIPLY);
                holder.preparationProgressBar.setVisibility(View.VISIBLE);
                seekBarNextPreviousView(holder, false);
            } else if (scene.getPreparingState() == SceneObject.PREPARING_STATE.PREPARING_SUCCESS) {
                holder.preparationProgressBar.setVisibility(View.GONE);
                seekBarNextPreviousView(holder, true);
            } else if (scene.getPreparingState() == SceneObject.PREPARING_STATE.PREPARING_FAILED) {
                holder.preparationProgressBar.setVisibility(View.GONE);
                seekBarNextPreviousView(holder, true);
            }
        }
    }

    /* This Function For QQMusic To Disable the Seekbar Seek and Previous and Next*/

    /**
     * @TODO Khajan
     * value false means disabling views and vice-versa
     */
    /**/
    private void seekBarNextPreviousView(Holder mHolder, boolean value) {
        mHolder.previous.setClickable(value);
        mHolder.next.setClickable(value);
    }

    private void setTheSourceIconFromCurrentSceneObject(SceneObject currentSceneObject, Holder holder) {


        String source = "Local";
        enableViews(holder);
        Drawable img = context.getResources().getDrawable(
                R.mipmap.ic_sources);

        if (currentSceneObject.getCurrentSource()==0)
        {
            disableViews(currentSceneObject.getCurrentSource(),"",holder);
        }
        if (currentSceneObject.getCurrentSource() == 2) {
            img = context.getResources().getDrawable(
                    R.mipmap.dmr_icon);
            source = "DLNA";
        }
        if (currentSceneObject.getCurrentSource() == 3) {
            img = context.getResources().getDrawable(
                    R.mipmap.ic_sources);
            source = "DMP";
        }

        if (currentSceneObject.getCurrentSource() == 4) {
            img = context.getResources().getDrawable(
                    R.mipmap.spotify);
            source = "Spotify";
        }
        if (currentSceneObject.getCurrentSource() == 5) {
            img = context.getResources().getDrawable(
                    R.mipmap.usb);
            source = "Usb";
        }
        if (currentSceneObject.getCurrentSource() == 6) {
            img = context.getResources().getDrawable(
                    R.mipmap.sdcard);
            source = "SD Card";
        }
        if (currentSceneObject.getCurrentSource() == 8) {
            img = context.getResources().getDrawable(
                    R.mipmap.vtuner_logo);

            source = "Vtuner";
            disableViews(currentSceneObject.getCurrentSource(),"",holder);
        }
        if (currentSceneObject.getCurrentSource() == 9) {
            img = context.getResources().getDrawable(
                    R.mipmap.tunein_logo1);

            source = "Tunein";
            disableViews(currentSceneObject.getCurrentSource(),"",holder);
        }
        if (currentSceneObject.getCurrentSource() == 14) { // Mail
            img = context.getResources().getDrawable(
                    R.mipmap.aux_in);
            source = "Aux";
            disableViews(currentSceneObject.getCurrentSource(),context.getResources().getString(R.string.aux),holder);
        }
        if (currentSceneObject.getCurrentSource() == 18) {
            img = context.getResources().getDrawable(
                    R.mipmap.qqmusic);
            source = "QPlay";
            seekBarNextPreviousView(holder, false);
            disableViews(currentSceneObject.getCurrentSource(), "", holder);
        }
        if (currentSceneObject.getCurrentSource() == 19) {
            img = context.getResources().getDrawable(
                    R.mipmap.bluetooth);
            source = "Bluetooth";
            if(currentSceneObject.getPlaystatus() == SceneObject.CURRENTLY_STOPPED) {
                disableViews(currentSceneObject.getCurrentSource(),context.getResources().getString(R.string.btOn),holder);
            }else if(currentSceneObject.getPlaystatus() == SceneObject.CURRENTLY_PAUSED)
            {
                disableViews(currentSceneObject.getCurrentSource(),context.getResources().getString(R.string.btOn),holder);
            }
            else {
                disableViews(currentSceneObject.getCurrentSource(),context.getResources().getString(R.string.btOn),holder);

            }
        }

        if (currentSceneObject.getCurrentSource() == 24) {
            img = context.getResources().getDrawable(
                    R.mipmap.ic_cast_white_24dp_2x);
            source = "Casting";
                disableViews(currentSceneObject.getCurrentSource(),context.getResources().getString(R.string.casting),holder);


        }
        if (currentSceneObject.getCurrentSource() == 20) {
            img = context.getResources().getDrawable(
                    R.mipmap.deezer_logo);
            source = "Deezer";
        }
        if (currentSceneObject.getCurrentSource() == 21) {
            img = context.getResources().getDrawable(
                    R.mipmap.tidal_logo1);
            source = "Tidal";
            disableViews(currentSceneObject.getCurrentSource(),currentSceneObject.getTrackName(),holder);

        }



        img.setBounds(0, 0, 15, 15);
        holder.sourceName.setText(source);
        holder.sourceImage.setImageDrawable(img);

        handleThePlayIconsForGrayoutOption(holder,currentSceneObject);

    }


    private void handleThePlayIconsForGrayoutOption(Holder holder,SceneObject sceneObject) {
        if (holder!=null) {
            if (holder.previous != null && holder.previous.isEnabled()) {
                holder.previous.setImageResource(R.mipmap.prev_with_glow);
            } else if (holder.previous != null) {
                holder.previous.setImageResource(R.mipmap.prev_without_glow);
            }


            if (holder.next != null && holder.next.isEnabled()) {
                holder.next.setImageResource(R.mipmap.next_with_glow);
            } else if (holder.next != null) {
                holder.next.setImageResource(R.mipmap.next_without_glow);
            }

            if (holder.play!=null&&holder.play.isEnabled()==false){

                holder.play.setImageResource(R.mipmap.play_without_glow);

            }else if (sceneObject!=null &&sceneObject.getPlaystatus()==SceneObject.CURRENTLY_PLAYING){
                holder.play.setImageResource(R.mipmap.pause_with_glow);
            }else {
                holder.play.setImageResource(R.mipmap.play_with_gl);
            }

            playPauseNextPrevious(holder,sceneObject);

        }



    }
    public void playPauseNextPrevious(Holder holder,SceneObject sceneObject){
        if (sceneObject!=null  && (sceneObject.getCurrentSource() == Constants.VTUNER_SOURCE
                || sceneObject.getCurrentSource() == Constants.TUNEIN_SOURCE
                || sceneObject.getCurrentSource() == Constants.BT_SOURCE
                || sceneObject.getCurrentSource() == Constants.AUX_SOURCE
                || sceneObject.getCurrentSource() == 0)) {
            holder.play.setImageResource(R.mipmap.play_without_glow);
            holder.next.setImageResource(R.mipmap.next_without_glow);
            holder.previous.setImageResource(R.mipmap.prev_without_glow);
        }
    }
    public SceneObject getSceneObjectFromAdapter(String ip) {
        return sceneMap.get(ip);
    }


    boolean doNextPrevious(boolean isNextPressed, SceneObject object) {

        /* DMR source callback*/
        if (object.getCurrentSource() == 2|| object.getCurrentSource()==0 || object.getCurrentSource()==12) {
            RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(object.getIpAddress());
            if (renderingDevice != null) {
                String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);


                if ( playbackHelper== null || playbackHelper.getDmsHelper() == null|| ( object!=null && false==object.getPlayUrl().contains(LibreApplication.LOCAL_IP))) {
                    Toast.makeText(context, "No active playlist,\nPlease select a song to play", Toast.LENGTH_SHORT).show();

                                       /* In SA mode we need to go to local content while in HN mode we will go to sources option */
                    if (LibreApplication.activeSSID.contains(Constants.DDMS_SSID)){
                        Intent localIntent = new Intent(context, CTDMSBrowserActivityV2.class);
                        localIntent.putExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, true);
                        localIntent.putExtra(Constants.CURRENT_DEVICE_IP, object.getIpAddress());
                        context.startActivity(localIntent);
                    }else {
                        Intent localIntent = new Intent(context, SourcesOptionActivity.class);
                        localIntent.putExtra(Constants.CURRENT_DEVICE_IP, object.getIpAddress());
                        localIntent.putExtra("current_source", "" + object.getCurrentSource());
                        context.startActivity(localIntent);
                    }


                    return false;
                }


                if (isNextPressed)
                    playbackHelper.playNextSong(1);
                else
                    playbackHelper.playNextSong(-1);

                if(object.getCurrentSource()!=Constants.BT_SOURCE) {
                    Message msg = new Message();
                    msg.what = PREPARATION_INIT;
                    Bundle data = new Bundle();
                    data.putString("ipAddress", object.getIpAddress());
                    msg.setData(data);
                    activehandler.sendMessage(msg);
                }
                return true;


            } else
                return false;
        } else {

            LUCIControl control = new LUCIControl(object.getIpAddress());


            /* Chnage done to provide a message for the deezer next button click */

            if (object.getCurrentSource()==21){

                String trackname=object.getTrackName();

                if (isNextPressed){

                    if (trackname!=null&&trackname.contains(Constants.DEZER_SONGSKIP))
                    {
                        
                        Toast.makeText(context,"Activate Deezer Premium+ from your computer",Toast.LENGTH_LONG).show();
                        return false;
                    }

                }
            }


            if (isNextPressed)
                control.SendCommand(MIDCONST.MID_PLAYCONTROL, LUCIMESSAGES.PLAY_NEXT, CommandType.SET);
            else
                control.SendCommand(MIDCONST.MID_PLAYCONTROL, LUCIMESSAGES.PLAY_PREV, CommandType.SET);
            if(object.getCurrentSource()!=Constants.BT_SOURCE) {
                Message msg = new Message();

                if (object.getCurrentSource() == Constants.ALEXA_SOURCE){
                    msg.what = Constants.ALEXA_NEXT_PREV_INIT;
                } else
                    msg.what = PREPARATION_INIT;
                Bundle data = new Bundle();
                data.putString("ipAddress", object.getIpAddress());
                msg.setData(data);
                activehandler.sendMessage(msg);
            }
            return true;
        }


    }
    public boolean doNextPrevious(boolean isNextPressed, SceneObject object,boolean mValue) {

        /* DMR source callback*/
        if (object.getCurrentSource() == 2|| object.getCurrentSource()==0 || object.getCurrentSource()==12) {
            RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(object.getIpAddress());
            if (renderingDevice != null) {
                String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);


                if ( playbackHelper== null || playbackHelper.getDmsHelper() == null|| ( object!=null && false==object.getPlayUrl().contains(LibreApplication.LOCAL_IP))) {
                    if(mValue)
                        return true;
                    Toast.makeText(context, "No active playlist,\nPlease select a song to play", Toast.LENGTH_SHORT).show();

                                       /* In SA mode we need to go to local content while in HN mode we will go to sources option */
                    if (LibreApplication.activeSSID.contains(Constants.DDMS_SSID)){
                        Intent localIntent = new Intent(context, CTDMSBrowserActivityV2.class);
                        localIntent.putExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, true);
                        localIntent.putExtra(Constants.CURRENT_DEVICE_IP, object.getIpAddress());
                        context.startActivity(localIntent);
                    }else {
                        Intent localIntent = new Intent(context, SourcesOptionActivity.class);
                        localIntent.putExtra(Constants.CURRENT_DEVICE_IP, object.getIpAddress());
                        localIntent.putExtra("current_source", "" + object.getCurrentSource());
                        context.startActivity(localIntent);
                    }


                    return false;
                }


                if (isNextPressed)
                    playbackHelper.playNextSong(1);
                else {

                 /* Setting the current seekbar progress -Start*/
                    float  duration = object.getCurrentPlaybackSeekPosition();
                    Log.d("Current Duration ", "Duration = " + duration / 1000);
                    float durationInSeeconds = duration/1000;
                    if(durationInSeeconds <5) {
                        playbackHelper.playNextSong(-1);
                    }else{
                        playbackHelper.playNextSong(0);
                    }
                }

                if(object.getCurrentSource()!=Constants.BT_SOURCE) {
                    Message msg = new Message();
                    msg.what = PREPARATION_INIT;
                    Bundle data = new Bundle();
                    data.putString("ipAddress", object.getIpAddress());
                    msg.setData(data);
                    activehandler.sendMessage(msg);
                }
                return true;


            } else
                return false;
        } return  true;

    }
    public void enableViews(Holder mHolder) {

        mHolder.play.setClickable(true);
        mHolder.play.setEnabled(true);
        // //volumeBar.setClickable(false);
        // volumeBar.setEnabled(false);
        mHolder.mutebutton.setClickable(true);
        mHolder.mutebutton.setEnabled(true);
        mHolder.previous.setClickable(true);
        mHolder.previous.setEnabled(true);
        mHolder.next.setEnabled(true);
        mHolder.next.setClickable(true);
        mHolder.volumebar.setClickable(true);
        mHolder.volumebar.setEnabled(true);

    }
    public void disableViews(int currentSrc,String message,Holder mHolder) {

        /* Dont have to call this function explicitly make use of setrceIco */

        switch (currentSrc) {


            case 0:
                mHolder.play.setClickable(false);
                break;

            case 12:
                mHolder.play.setClickable(false);
                break;

            case 8:
                //vtuner
            {

                /* disable previous and next */
      /*          mHolder.previous.setClickable(false);
                mHolder.previous.setEnabled(false);
                mHolder.next.setEnabled(false);
                mHolder.next.setClickable(false);
                mHolder.play.setClickable(false);*/

                mHolder.next.setImageResource(R.mipmap.next_without_glow);
                mHolder.previous.setImageResource(R.mipmap.prev_without_glow);
                mHolder.play.setImageResource(R.mipmap.play_without_glow);

                break;

            }

            case 9:
                //tunein
            {
                /*setting seek to zero*/
                /* disable previous and next */
            /*    mHolder.previous.setClickable(false);
                mHolder.previous.setEnabled(false);
                mHolder.next.setEnabled(false);
                mHolder.next.setClickable(false);
                mHolder.play.setClickable(false);*/
                mHolder.next.setImageResource(R.mipmap.next_without_glow);
                mHolder.previous.setImageResource(R.mipmap.prev_without_glow);
                mHolder.play.setImageResource(R.mipmap.play_without_glow);
                break;

            }
            case 14:
                //Aux

            {

                /* disable previous and next */
                //mHolder.previous.setClickable(false);
                //mHolder.previous.setEnabled(false);
                //mHolder.next.setEnabled(false);
                //mHolder.next.setClickable(false);

                //mHolder.play.setClickable(false);
                //mHolder.play.setEnabled(false);
                mHolder.next.setImageResource(R.mipmap.next_without_glow);
                mHolder.previous.setImageResource(R.mipmap.prev_without_glow);
                mHolder.play.setImageResource(R.mipmap.play_without_glow);
                mHolder.trackName.setText(message);
                String album_url = "http://" + mHolder.ipaddress + "/" + "coverart1.jpg";
                PicassoTrustCertificates.getInstance(context).load(R.mipmap.album_art).memoryPolicy(MemoryPolicy.NO_STORE)
                        .placeholder(R.mipmap.album_art)
                        .into(mHolder.albumArt);
                break;

            }

            case 18: {   //QQ

                /* disable previous and next */
             /*   mHolder.previous.setClickable(false);
                mHolder.previous.setEnabled(false);
                mHolder.next.setEnabled(false);
                mHolder.next.setClickable(false);*/


                break;
            }
            case 19:
                //Bluetooth
            {
                mHolder.trackName.setText(message);
                String album_url = "http://" + mHolder.ipaddress + "/" + "coverart1.jpg";

                PicassoTrustCertificates.getInstance(context).load(R.mipmap.album_art).memoryPolicy(MemoryPolicy.NO_STORE).placeholder(R.mipmap.album_art)
                        .into(mHolder.albumArt);

                final LSSDPNodes mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(mHolder.ipaddress);
                if(mNode==null){
                    return;
                }
                    SceneObject currentSceneObject = sceneMap.get(mHolder.ipaddress);
                LibreLogger.d(this,"BT controller value in sceneobject "+mNode.getBT_CONTROLLER());
            //    Toast.makeText(getContext(), "BT controller value in sceneobject " + currentSceneObject.getBT_CONTROLLER(), Toast.LENGTH_SHORT);
                if(mNode.getBT_CONTROLLER()==1 || mNode.getBT_CONTROLLER()==2 ||mNode.getBT_CONTROLLER()==3 )
                {
                /*    mHolder.previous.setClickable(true);
                    mHolder.previous.setEnabled(true);
                    mHolder.next.setEnabled(true);
                    mHolder.next.setClickable(true);
                    mHolder.play.setClickable(true);
                    mHolder.play.setEnabled(true);*/
                }else{
                     /* disable previous and next */
                    /*mHolder.previous.setClickable(false);
                    mHolder.previous.setEnabled(false);
                    mHolder.next.setEnabled(false);
                    mHolder.next.setClickable(false);

                    mHolder.play.setClickable(false);
                    mHolder.play.setEnabled(false);*/
                    mHolder.next.setImageResource(R.mipmap.next_without_glow);
                    mHolder.previous.setImageResource(R.mipmap.prev_without_glow);
                    mHolder.play.setImageResource(R.mipmap.play_without_glow);
                }
                break;

            }
            case Constants.GCAST_SOURCE:{
                //gCast is Playing

                  /*setting seek to zero*/
                /* disable previous and next */
               // mHolder.previous.setClickable(false);
              //  mHolder.previous.setEnabled(false);
              //  mHolder.next.setEnabled(false);
              //  mHolder.next.setClickable(false);

              //  mHolder.play.setClickable(false);
              //  mHolder.play.setEnabled(false);
                mHolder.play.setImageResource(R.mipmap.play_with_gl);
                mHolder.next.setImageResource(R.mipmap.next_without_glow);
                mHolder.previous.setImageResource(R.mipmap.prev_without_glow);
                mHolder.trackName.setText(message);
                mHolder.volumebar.setEnabled(false);
                mHolder.volumebar.setProgress(0);
                mHolder.volumebar.setClickable(false);

                break;
            }

            case 21: {
                String trackname = message;
                if (message != null && message.contains(Constants.DEZER_RADIO))
                {   mHolder. previous.setEnabled(false);
                    mHolder.previous.setClickable(false);



                }
              /*  if (message!=null&& message.contains(Constants.DEZER_SONGSKIP)) {
                    mHolder.next.setEnabled(false);
                    mHolder.next.setClickable(false);
                }*/

                break;
            }
        }





    }

}
