package com.libre.nowplaying;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cumulations.libreV2.AppConstants;
import com.cumulations.libreV2.AppUtils;
import com.libre.ActiveSceneAdapter;
import com.libre.AudioWave.DummyVisualizer;
import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.DeviceDiscoveryFragment;
import com.libre.LErrorHandeling.LibreError;
import com.libre.LibreApplication;
import com.libre.NewManageDevices.NewManageDevices;
import com.libre.R;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.SceneObject;
import com.libre.SourcesOptionActivity;
import com.libre.TuneInRemoteSourcesList;
import com.libre.alexa.AudioRecordCallback;
import com.libre.alexa.AudioRecordUtil;
import com.libre.alexa.MicExceptionListener;
import com.libre.alexa.MicTcpServer;
import com.libre.app.dlna.dmc.LocalDMSActivity;
import com.libre.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.libre.app.dlna.dmc.utility.DMRControlHelper;
import com.libre.app.dlna.dmc.utility.PlaybackHelper;
import com.libre.app.dlna.dmc.utility.UpnpDeviceManager;
import com.libre.constants.CommandType;
import com.libre.constants.LSSDPCONST;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.luci.LUCIPacket;
import com.libre.luci.Utils;
import com.libre.netty.BusProvider;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;
import com.libre.util.LibreLogger;
import com.libre.util.PicassoTrustCertificates;
import com.squareup.picasso.MemoryPolicy;

import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Action;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.json.JSONObject;

import java.text.DecimalFormat;

/*import android.support.design.widget.Snackbar;*/

/*import android.support.design.widget.Snackbar;*/

/**
 * Created by khajan on 3/8/15.
 */

public class NowPlayingFragment extends DeviceDiscoveryFragment
        implements View.OnClickListener, LibreDeviceInteractionListner,
        DMRProcessor.DMRProcessorListener, VolumeListener.OnChangeVolumeHardKeyListener,
        View.OnTouchListener, View.OnLongClickListener, MicExceptionListener,AudioRecordCallback {
    private ImageView albumArt,alexaSourceImage;

    private TextView mProgressView;

    ImageView tuneInInfoButton;

    private LinearLayout cast_layout;
    private TextView genreTextField, startListeningText;
    private SeekBar volumeBar;
    private SeekBar seekSongBar;
    private TextView artistName;
    private TextView albumName;
    private TextView songName;
    private TextView sceneName;
    private ImageButton play;
    private ImageButton previous;
    private ImageButton next;
    private ImageView mutebutton;
    private ImageView nextSceneImageView;
    private ImageView prevSceneImageView;
    private SceneObject currentSceneObject;
    private String currentIpAddress;
    private TextView currentPlayPosition;
    private TextView totalPlayPosition;
    private TextView mDevices, deviceCount;
    private LinearLayout mSources;
    private ImageButton shuffleButton;
    private ImageView favButton, mSourcesIcon;
    private ImageButton repeatButton;
    private RelativeLayout mic_relative_layout;

    private ProgressDialog m_progressDlg;
//    private Visualizer Visualizer;
    private DummyVisualizer dummyVisualizer;
    private boolean isButtonLongPressed;


    public static final int REPEAT_OFF = 0;
    public static final int REPEAT_ONE = 1;
    public static final int REPEAT_ALL = 2;
    public static final int DMR_SOURCE = 2;

    private ProgressBar loadingProgressBar;


    private boolean isLocalDMRPlayback = false;

    private int WiFi_For_Gear4 = 0;
    /*Added for USB and SDCARD*/

    public static final int USB_SOURCE = 5;
    public static final int SD_CARD = 6;
    public static final int DEEZER_SOURCE = 21;
    public static final int FAV_SOURCE = 23;
    public static final int TIDAL_SOURCE = 22;
    public static final int VTUNER_SOURCE = 8;
    public static final int TUNEIN_SOURCE = 9;
    public static final int NETWORK_DEVICES = 3;
    public static final int ROON_SOURCE = 27;
    ScanningHandler mScanHandler = ScanningHandler.getInstance();

    /* Added to handle the case where trackname is empty string"*/
    private String currentTrackName = "-1";

    private final static int PLAYBACK_TIMER_TIMEOUT = 5000;
    private boolean isStillPlaying = false;
    private boolean is49MsgBoxReceived = false;

    private AudioRecordUtil audioRecordUtil;
    private MicTcpServer micTcpServer;
    private String phoneIp;
    private AlertDialog alertDialog;

    private Handler startPlaybackTimerhandler = new Handler();
    private DisableVpListener vpListener;
    private Runnable startPlaybackTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isStillPlaying) {
                if (is49MsgBoxReceived) {
                    // Do nothing, set is49MsgBoxReceived to false so that for next 49 msg box received
                    // this value gets updated
                    is49MsgBoxReceived = false;
                    /*this is hack to check again if is49MsgBoxReceived is still false after making it false
                    * which will confirm no 49 msg box received after total 10 seconds*/
                    startPlaybackTimerhandler.postDelayed(startPlaybackTimerRunnable, PLAYBACK_TIMER_TIMEOUT);
                } else {

                    if (getActivity() != null && !getActivity().isFinishing()) {
                        disablePlayback();
                    }
                    /*Hack : send any luci command to check device still alive or not when user pauses and stays on same
                            * screen and does nothing*/
                    new LUCIControl(currentIpAddress).SendCommand(50, null, 1);
                }
            }
        }
    };

    private void disablePlayback() {
        /*make player status to paused, ie show play icon*/
        play.setImageResource(R.mipmap.play_without_glow);
        next.setImageResource(R.mipmap.next_without_glow);
        previous.setImageResource(R.mipmap.prev_without_glow);
    }

    Handler showLoaderHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.PREPARATION_INITIATED:
                    loadingProgressBar.setVisibility(View.VISIBLE);
                    /*disabling views while initiating */
                    preparingToPlay(false);
                    break;

                case Constants.PREPARATION_COMPLETED:
                    loadingProgressBar.setVisibility(View.GONE);
                    /*enabling views while initiating */
                    preparingToPlay(true);
                    break;

                case Constants.PREPARATION_TIMEOUT_CONST:

                    /*enabling views while initiating */
                    preparingToPlay(true);

                      /*posting error to application here we are using bus because it doest extend DeviceDiscoverActivity*/
                  /*
                    Commenting the error dialog as it was getting triggered for the external app setting the AV transport url

                    LibreError error = new LibreError(currentIpAddress, getResources.getString(R.string.PREPARATION_TIMEOUT_HAPPENED));
                    BusProvider.getInstance().post(error);
                    */
                    loadingProgressBar.setVisibility(View.GONE);
                    break;

                case Constants.ALEXA_NEXT_PREV_HANDLER:
                    if (m_progressDlg != null && m_progressDlg.isShowing())
                        m_progressDlg.dismiss();
                    break;

            }


        }


    };

    AlertDialog.Builder updateBuilder;
    AlertDialog updateProgressAlert;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.playing_fragment, container, false);

        Bundle arguments = getArguments();
        currentIpAddress = arguments.getString("scene_IP");
        int position = arguments.getInt("scene_POSITION");

        initViews(v, position);
//        mic_relative_layout.setEnabled(false);

        if (currentIpAddress != null) {
            currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(currentIpAddress); // ActiveSceneAdapter.mMasterSpecificSlaveAndFreeDeviceMap.get(currentIpAddress);
            setViews();
            if (currentSceneObject == null) {
                Log.d("NetworkChanged", "NowPlayingFragment onCreateView");
                Toast.makeText(getActivity(), getString(R.string.deviceNotFound), Toast.LENGTH_SHORT).show();
            } else if (currentSceneObject.getCurrentSource() == DMR_SOURCE) {
                /* Check if the playback is through DMR and it is */
                isLocalDMRPlayback = true;
            }
        }


        return v;
    }

    private void gotoSourcesOption(String ipaddress, int currentSource) {
        Intent mActiveScenesList = new Intent(getActivity(), SourcesOptionActivity.class);
        mActiveScenesList.putExtra(Constants.CURRENT_DEVICE_IP, ipaddress);
        mActiveScenesList.putExtra("current_source", "" + currentSource);
        mActiveScenesList.putExtra(Constants.FROM_ACTIVITY, "NowPlayingFragment");
        LibreError error = new LibreError("", getResources().getString(R.string.no_active_playlist), 1);
        BusProvider.getInstance().post(error);

        getActivity().startActivity(mActiveScenesList);
    }

    private boolean isLocalDMR(SceneObject sceneObject) {
        RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(sceneObject.getIpAddress());
        if (renderingDevice != null) {
            String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
            PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
            boolean mIsDmrPlaying = ScanningHandler.getInstance().ToBeNeededToLaunchSourceScreen(sceneObject);
            LibreLogger.d(this, " local DMR is playing" + mIsDmrPlaying);
            return !mIsDmrPlaying;
        }
   /*     if(!LibreApplication.LOCAL_IP.equals("") && sceneObject.getPlayUrl().contains(LibreApplication.LOCAL_IP)){
            return true;
        }*/
        return false;
    }

    private boolean isActivePlayistNotAvailable(SceneObject sceneObject) {
        if (sceneObject != null && sceneObject.getCurrentSource() == Constants.NO_SOURCE
                || (sceneObject.getCurrentSource() == Constants.DMR_SOURCE && ScanningHandler.getInstance().ToBeNeededToLaunchSourceScreen(sceneObject))) {
            return true;
        }
        return false;
    }

   /* private void refreshUiWithNewSceneObject(SceneObject scene) {


        if (scene != null) {

            if (scene.getAlbum_art() != null && !scene.getAlbum_art().equalsIgnoreCase("null")) {
                String album_url = "http://" + scene.getIpAddress() + "/" + scene.getAlbum_art();

                LibreLogger.d(this, "AlbumArt URL is---" + album_url);

                PicassoTrustCertificates.getInstance(getActivity()).load(album_url).memoryPolicy(MemoryPolicy.NO_CACHE)
                        .error(R.mipmap.album_art).skipMemoryCache()
                        .into(albumArt);

                if (currentSceneObject == null) {
                    return;
                }

                if (currentSceneObject.getvolumeZoneInPercentage() >= 0)
                    volumeBar.setProgress(currentSceneObject.getvolumeZoneInPercentage());
               *//* if (currentSceneObject.getVolumeValueInPercentage() >= 0)
                    volumeBar.setProgress(currentSceneObject.getVolumeValueInPercentage());*//*

                artistName.setText(currentSceneObject.getArtist_name());

                albumName.setText("" + currentSceneObject.getAlbum_name());
                songName.setText("" + currentSceneObject.getTrackName());
                sceneName.setText("" + currentSceneObject.getSceneName());


                if (currentSceneObject.getPlaystatus() == SceneObject.CURRENTLY_PLAYING) {
                    play.setImageResource(android.R.drawable.ic_media_pause);
                } else {
                    play.setImageResource(android.R.drawable.ic_media_play);
                }


                if (currentSceneObject.getTotalTimeOfTheTrack() != 0) {
                    seekSongBar.setMax(100);
                    seekSongBar.setProgress((int) (100 * (currentSceneObject.getCurrentPlaybackSeekPosition() / currentSceneObject.getTotalTimeOfTheTrack())));
                    seekSongBar.setSecondaryProgress((int) (100 * (currentSceneObject.getCurrentPlaybackSeekPosition() / currentSceneObject.getTotalTimeOfTheTrack())));


                }


            }
        } else {
            LibreLogger.d(this, "NowPlaying: scene object is empty");
        }
    }
*/

    /*initialize variables here*/
    private void initViews(View v, int position) {
        int mNumberOfDevices = 0;
        mic_relative_layout = (RelativeLayout) v.findViewById(R.id.mic_relative_layout);
//        Visualizer = (Visualizer) v.findViewById(R.id.visualizer);
//        Visualizer.setVisibility(View.INVISIBLE);
        dummyVisualizer = (DummyVisualizer) v.findViewById(R.id.visualizer);
        dummyVisualizer.setVisibility(View.INVISIBLE);
        albumArt = (ImageView) v.findViewById(R.id.album_art);

        cast_layout = (LinearLayout) v.findViewById(R.id.cast_layout);
        genreTextField = (TextView) v.findViewById(R.id.genreTextField);
        startListeningText = (TextView) v.findViewById(R.id.startListeningText);
        startListeningText.setVisibility(View.INVISIBLE);

        alexaSourceImage = (ImageView) v.findViewById(R.id.alexaSourceImg);
        volumeBar = (SeekBar) v.findViewById(R.id.seekBar);
        seekSongBar = (SeekBar) v.findViewById(R.id.scene_seekbar);
        artistName = (TextView) v.findViewById(R.id.artist_name);
        albumName = (TextView) v.findViewById(R.id.album_name);
        songName = (TextView) v.findViewById(R.id.song_name);
        sceneName = (TextView) v.findViewById(R.id.device_name);
        sceneName.setSelected(true);
        sceneName.setVisibility(View.GONE);

        play = (ImageButton) v.findViewById(R.id.media_btn_play);
        previous = (ImageButton) v.findViewById(R.id.media_btn_previous);
        next = (ImageButton) v.findViewById(R.id.media_btn_nxt);
        mutebutton = (ImageView) v.findViewById(R.id.mute_button);

        currentPlayPosition = (TextView) v.findViewById(R.id.currentPlaybackTime);
        totalPlayPosition = (TextView) v.findViewById(R.id.totalPlaybackTime);

        nextSceneImageView = (ImageView) v.findViewById(R.id.nextSceneButton);
        prevSceneImageView = (ImageView) v.findViewById(R.id.prevSceneButton);

        shuffleButton = (ImageButton) v.findViewById(R.id.shuffle);
        repeatButton = (ImageButton) v.findViewById(R.id.repeat);

        favButton = (ImageView) v.findViewById(R.id.favourite_button);
        mSourcesIcon = (ImageView) v.findViewById(R.id.source_icon);

        deviceCount = (TextView) v.findViewById(R.id.device_Count);

        /*progressBar*/
        loadingProgressBar = (ProgressBar) v.findViewById(R.id.loading_progressbar);


        tuneInInfoButton = (ImageView) v.findViewById(R.id.tune_in_info_button);
        tuneInInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), TuneInRemoteSourcesList.class);
                intent.putExtra(Constants.CURRENT_DEVICE_IP, currentSceneObject.getIpAddress());
                intent.putExtra("current_source_index_selected", currentSceneObject.getCurrentSource());
                intent.putExtra("tune_in_info_clicked", "TuneInfoClicked");
                startActivity(intent);
            }
        });

        loadingProgressBar.setVisibility(View.GONE);

        play.setOnClickListener(this);
        previous.setOnClickListener(this);
        next.setOnClickListener(this);
        nextSceneImageView.setOnClickListener(this);
        prevSceneImageView.setOnClickListener(this);

        shuffleButton.setOnClickListener(this);
        repeatButton.setOnClickListener(this);
        favButton.setOnClickListener(this);


        mDevices = (TextView) v.findViewById(R.id.mDevices);
        mDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent newIntent = new Intent(getActivity(), NewManageDevices.class);
                newIntent.putExtra("master_ip", currentIpAddress);
                newIntent.putExtra("activity_name", "NowPlayingActivity");
                startActivity(newIntent);
                getActivity().finish();


            }
        });
        ImageView mSourcesIcon = (ImageView) v.findViewById(R.id.source_icon);
        mSourcesIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(currentIpAddress);//ActiveSceneAdapter.mMasterSpecificSlaveAndFreeDeviceMap.get(currentIpAddress);
                final LSSDPNodes mMasternode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress);
                if (mMasternode.getCurrentSource() == Constants.GCAST_SOURCE
                        && mMasternode.getmPlayStatus() == SceneObject.CURRENTLY_PLAYING) {
                    LibreError error = new LibreError(mMasternode.getFriendlyname(), Constants.SPEAKER_IS_CASTING);
                    //  BusProvider.getInstance().post(error);
                    return;
                }
                if (currentSceneObject != null) {
                    Intent mActiveScenesList = new Intent(getActivity(), SourcesOptionActivity.class);
                    mActiveScenesList.putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress);
                    mActiveScenesList.putExtra("current_source", "" + currentSceneObject.getCurrentSource());
                    mActiveScenesList.putExtra(Constants.FROM_ACTIVITY, "NowPlayingFragment");
                    startActivity(mActiveScenesList);
                } else {
                    Toast.makeText(getActivity(), "Device Got Removed " + currentIpAddress, Toast.LENGTH_SHORT).show();
                    LibreLogger.d(this, "Device Got Removed" + currentIpAddress);
                }
            }
        });
        mSources = (LinearLayout) v.findViewById(R.id.mSources);
        mSources.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(currentIpAddress);//ActiveSceneAdapter.mMasterSpecificSlaveAndFreeDeviceMap.get(currentIpAddress);
                final LSSDPNodes mMasternode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress);

                if (mMasternode!= null && mMasternode.getCurrentSource() == Constants.GCAST_SOURCE
                        && mMasternode.getmPlayStatus() == SceneObject.CURRENTLY_PLAYING) {
                    LibreError error = new LibreError(mMasternode.getFriendlyname(), Constants.SPEAKER_IS_CASTING);
                    //    BusProvider.getInstance().post(error);
                    return;
                }
                if (currentSceneObject != null) {
                    Intent mActiveScenesList = new Intent(getActivity(), SourcesOptionActivity.class);
                    mActiveScenesList.putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress);
                    mActiveScenesList.putExtra("current_source", "" + currentSceneObject.getCurrentSource());
                    mActiveScenesList.putExtra(Constants.FROM_ACTIVITY, "NowPlayingFragment");
                    startActivity(mActiveScenesList);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.deviceRemoved) + currentIpAddress, Toast.LENGTH_SHORT).show();
                    LibreLogger.d(this, "Device Got Removed" + currentIpAddress);
                }
            }
        });

        mSources.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mic_relative_layout.performLongClick();
                return false;
            }
        });
        mSources.setOnTouchListener(this);
        mic_relative_layout.setOnLongClickListener(this);
        mic_relative_layout.setOnTouchListener(this);

        ScanningHandler mScanHandler = ScanningHandler.getInstance();
        /*LSSDPNodes mNode = mScanHandler.getLSSDPNodeFromCentralDB(currentIpAddress);
        if (mNode != null) {
            LibreLogger.d("com.libre.Scanning", "Number of Devices For MasterIp: " + currentIpAddress + "Device State " + mNode.getDeviceState());
            if (mNode.getDeviceState().contains("M")) {
                ArrayList<String> mSlaveList = mScanHandler.getSlaveListFromMasterISourcesOptionActivityp(currentIpAddress);
                LibreLogger.d("com.libre.Scanning", "Number of Devices For MasterIp: " + currentIpAddress + "Number of Devices " + mSlaveList.size());
                mNumberOfDevices = mSlaveList.size() + 1;
                deviceCount.setText("" + mNumberOfDevices);
            }
        } else {
            deviceCount.setText("0");
        }*/
        deviceCount.setText("" + mScanHandler.getNumberOfSlavesForMasterIp(currentIpAddress, mScanHandler.getconnectedSSIDname(getActivity().getApplicationContext())));

        /*Setting adapter*/


        if (position == 0) {
            prevSceneImageView.setVisibility(View.INVISIBLE);
        }


        if (position == mScanHandler.getSceneObjectFromCentralRepo().size() - 1
                ) {//ActiveSceneAdapter.mMasterSpecificSlaveAndFreeDeviceMap.size() - 1) {
            nextSceneImageView.setVisibility(View.INVISIBLE);
        }

        if (ActiveSceneAdapter.getCountNumberofDevicesInAdapter() <= 1) {
            nextSceneImageView.setVisibility(View.INVISIBLE);
            prevSceneImageView.setVisibility(View.INVISIBLE);
        }
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                if (!doVolumeChange(seekBar.getProgress()))
                    Toast.makeText(getActivity(), getString(R.string.actionFailed), Toast.LENGTH_SHORT).show();


            }
        });

        seekSongBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                LibreLogger.d(this, "Bhargav SEEK:Seekbar Position track " + seekBar.getProgress() + "  " + seekBar.getMax());
                return;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                LibreLogger.d(this, " Bhargav SEEK:Seekbar Position trackstop" + seekBar.getProgress() + "  " + seekBar.getMax());

                DMRProcessor theRenderer = getTheRenderer(currentIpAddress);
                if (isLocalDMRPlayback && theRenderer == null) {
                    Toast.makeText(getActivity(), getString(R.string.NoRenderer), Toast.LENGTH_LONG).show();
                } else if (currentSceneObject!=null && currentSceneObject.getCurrentSource() == Constants.ALEXA_SOURCE){
                    return;
                }
                else if (doSeek(seekBar.getProgress()) == false) {
                    if (currentSceneObject != null) {
                        LibreError error = new LibreError(currentSceneObject.getIpAddress(), getResources().getString(R.string.RENDERER_NOT_PRESENT));
                        BusProvider.getInstance().post(error);
                    }
                }

            }
        });


    }


    private void setMarquees() {
        mDevices.setSelected(true);
    }

    /*this method will take care of views while we are preparing to play*/
    private void preparingToPlay(boolean value) {

        if (value) {
            setTheSourceIconFromCurrentSceneObject();
        } else {
            seekSongBar.setEnabled(value);
            seekSongBar.setClickable(value);
            play.setClickable(value);
            play.setEnabled(value);
            mutebutton.setClickable(value);
            mutebutton.setEnabled(value);
            previous.setClickable(value);
            previous.setEnabled(value);
            next.setEnabled(value);
            next.setClickable(value);
            volumeBar.setClickable(value);
            volumeBar.setEnabled(value);
        }
    }

    public void disableViews(int currentSrc, String message) {

        /* Dont have to call this function explicitly make use of setrceIco */

        switch (currentSrc) {

            case 0: {
                   /*setting seek to zero*/
                seekSongBar.setProgress(0);
                seekSongBar.setEnabled(false);
                seekSongBar.setClickable(false);
                currentPlayPosition.setText("0:00");
                totalPlayPosition.setText("0:00");

                /* disable previous and next */
  /*              previous.setClickable(false);
                previous.setEnabled(false);
                next.setEnabled(false);
                next.setClickable(false);
*/

                play.setClickable(false);
                play.setEnabled(false);

                artistName.setText("");
                albumName.setText("");
                songName.setText("");
                favButton.setVisibility(View.GONE);
                LibreLogger.d(this, "Libre - return from WiFi mode through hard key");

                break;
            }

            case 12: {
                   /*setting seek to zero*/
                seekSongBar.setProgress(0);
                seekSongBar.setEnabled(false);
                seekSongBar.setClickable(false);
                currentPlayPosition.setText("0:00");
                totalPlayPosition.setText("0:00");

                /* disable previous and next */
  /*              previous.setClickable(false);
                previous.setEnabled(false);
                next.setEnabled(false);
                next.setClickable(false);
*/

                play.setClickable(false);
                play.setEnabled(false);

                artistName.setText("");
                albumName.setText("");
                songName.setText("");
                LibreLogger.d(this, "Gear4 - return from WiFi mode through hard key");

                break;
            }

            case 1: {/* For Air Play */
                seekSongBar.setEnabled(false);
                seekSongBar.setClickable(false);
            }
            break;
            case 8:
                //vtuner
            {
                /*setting seek to zero*/
                seekSongBar.setProgress(0);
                seekSongBar.setEnabled(false);
                seekSongBar.setClickable(false);
                currentPlayPosition.setText("0:00");
                totalPlayPosition.setText("0:00");

                /* disable previous and next */
              /*  previous.setClickable(false);
                previous.setEnabled(false);
                next.setEnabled(false);
                next.setClickable(false);

                *//* disable play and pause *//*
                play.setClickable(false);
                play.setEnabled(false);*/
                play.setImageResource(R.mipmap.play_without_glow);
                /*making it without glow for vtuner*/
                next.setImageResource(R.mipmap.next_without_glow);
                previous.setImageResource(R.mipmap.prev_without_glow);
                break;

            }

            case 9:
                //tunein
            {
                /*setting seek to zero*/
                seekSongBar.setProgress(0);
                seekSongBar.setEnabled(false);
                seekSongBar.setClickable(false);
                currentPlayPosition.setText("0:00");
                totalPlayPosition.setText("0:00");

                /* disable previous and next */
               /* previous.setClickable(false);
                previous.setEnabled(false);
                next.setEnabled(false);
                next.setClickable(false);*/
                play.setImageResource(R.mipmap.play_without_glow);
                 /* disable play and pause */
                play.setClickable(false);
                play.setEnabled(false);
                tuneInInfoButton.setEnabled(true);
                tuneInInfoButton.setClickable(true);
                break;

            }
            case 14:
                //Aux

            {       /*setting seek to zero*/
                seekSongBar.setProgress(0);
                seekSongBar.setEnabled(false);
                seekSongBar.setClickable(false);
                currentPlayPosition.setText("0:00");
                totalPlayPosition.setText("0:00");
                favButton.setVisibility(View.GONE);
                /* disable previous and next */
                // previous.setClickable(false);
                /// previous.setEnabled(false);
                //next.setEnabled(false);
                // next.setClickable(false);

                // play.setClickable(false);
                //play.setEnabled(false);
                play.setImageResource(R.mipmap.play_without_glow);
                next.setImageResource(R.mipmap.next_without_glow);
                previous.setImageResource(R.mipmap.prev_without_glow);
                artistName.setText(message);
                albumName.setText("");
                songName.setText("");
                LibreLogger.d(this, "we set the song name to empty for disabling " + currentSceneObject.getTrackName() + " in disabling view where artist name is " + message);


                break;

            }

            case 18: {   //QQ

                   /*setting seek to zero*/
              /*  seekSongBar.setProgress(0);
                seekSongBar.setEnabled(false);
                seekSongBar.setClickable(false);*/
                /*currentPlayPosition.setText("0:00");
                totalPlayPosition.setText("0:00");*/

                /* disable previous and next */
                //  previous.setClickable(false);
              /*  previous.setEnabled(false);
                next.setEnabled(false);*/
                //  next.setClickable(false);
    /*making it without glow for qqmusic */
                next.setImageResource(R.mipmap.next_without_glow);
                previous.setImageResource(R.mipmap.prev_without_glow);

                break;
            }
            case 19:
                //Bluetooth
            {
                      /*setting seek to zero*/
                seekSongBar.setProgress(0);
                seekSongBar.setEnabled(false);
                seekSongBar.setClickable(false);
                currentPlayPosition.setText("0:00");
                totalPlayPosition.setText("0:00");
                artistName.setText(message);
                albumName.setText("");
                songName.setText("");
                // hide repeat and shuffle
                repeatButton.setVisibility(View.GONE);
                shuffleButton.setVisibility(View.GONE);
                favButton.setVisibility(View.GONE);
                final LSSDPNodes mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress);
                if (mNode == null) {
                    return;
                }
                LibreLogger.d(this, "BT controller value in sceneobject " + mNode.getBT_CONTROLLER());
                //    Toast.makeText(getContext(), "BT controller value in sceneobject " + currentSceneObject.getBT_CONTROLLER(), Toast.LENGTH_SHORT);
                if (mNode.getBT_CONTROLLER() == 1 || mNode.getBT_CONTROLLER() == 2 || mNode.getBT_CONTROLLER() == 3) {            // play.setClickable(true);
                    //play.setEnabled(true);
                    //   previous.setClickable(true);
                    //  previous.setEnabled(true);
                    //  next.setEnabled(true);
                    //   next.setClickable(true);
                    volumeBar.setEnabled(true);
                    volumeBar.setClickable(true);
                } else {
                    /* disable previous and next */
                    //   previous.setClickable(false);
                    //  previous.setEnabled(false);
                    //  next.setEnabled(false);
                    // next.setClickable(false);

                    //  play.setClickable(false);
                    //   play.setEnabled(false);
                    play.setImageResource(R.mipmap.play_without_glow);
                    next.setImageResource(R.mipmap.next_without_glow);
                    previous.setImageResource(R.mipmap.prev_without_glow);
                }
                break;

            }

            case Constants.GCAST_SOURCE: {
                //gCast is Playing

                  /*setting seek to zero*/
                seekSongBar.setProgress(0);
                seekSongBar.setEnabled(false);
                seekSongBar.setClickable(false);
                currentPlayPosition.setText("0:00");
                totalPlayPosition.setText("0:00");

                /* disable previous and next */
                // previous.setClickable(false);
                // previous.setEnabled(false);
                // next.setEnabled(false);
                // next.setEnabled(false);
                //  next.setClickable(false);

                //   play.setClickable(false);
                // play.setEnabled(false);
                play.setImageResource(R.mipmap.play_with_gl);
                next.setImageResource(R.mipmap.next_without_glow);
                previous.setImageResource(R.mipmap.prev_without_glow);

                volumeBar.setProgress(0);
                volumeBar.setClickable(false);
                volumeBar.setEnabled(false);
                artistName.setText("Casting");
                albumName.setText("");
                songName.setText("");
                cast_layout.setVisibility(View.VISIBLE);
                if (currentSceneObject != null && currentSceneObject.getGenre() != null && !currentSceneObject.getGenre().equalsIgnoreCase("null"))
                    genreTextField.setText(currentSceneObject.getGenre());
                else {
                    genreTextField.setText("");
                }
                break;
            }


            //Deezer
                /*If the current source is deezer, we need to check the trackname.
                if trackname contains :;:radio
                */
            case 21: {

                String trackname = message;
                if (message != null && message.contains(Constants.DEZER_RADIO))

                {
                    previous.setEnabled(false);
                    previous.setClickable(false);

                }/*
                 if (message!=null&& message.contains(Constants.DEZER_SONGSKIP)) {
                     next.setEnabled(false);
                     next.setClickable(false);
                 }*/
                break;
            }

            case Constants.ALEXA_SOURCE: {
                //gCast is Playing

                  /*setting seek to zero*/
               // seekSongBar.setProgress(0);
              //  seekSongBar.setEnabled(false);
                seekSongBar.setClickable(false);

                play.setImageResource(R.mipmap.play_with_gl);
                next.setImageResource(R.mipmap.next_with_glow);
                previous.setImageResource(R.mipmap.prev_with_glow);
                /*artistName.setText("");
                albumName.setText("");
                songName.setText("");*/
                if (currentSceneObject != null && currentSceneObject.getGenre() != null && !currentSceneObject.getGenre().equalsIgnoreCase("null"))
                    genreTextField.setText(currentSceneObject.getGenre());
                else {
                    genreTextField.setText("");
                }
                break;
            }

        }


    }

    public void enableViews() {
        seekSongBar.setEnabled(true);
        seekSongBar.setClickable(true);
        play.setClickable(true);
        play.setEnabled(true);
        // //volumeBar.setClickable(false);
        // volumeBar.setEnabled(false);
        mutebutton.setClickable(true);
        mutebutton.setEnabled(true);
        previous.setClickable(true);
        previous.setEnabled(true);
        next.setEnabled(true);
        next.setClickable(true);
        cast_layout.setVisibility(View.GONE);
        volumeBar.setEnabled(true);
        volumeBar.setClickable(true);


    }

    @Override
    public void onClick(View view) {


        /*stop any previous started playback handler started by removing them*/
        startPlaybackTimerhandler.removeCallbacks(startPlaybackTimerRunnable);

        if (currentSceneObject == null) {
            Toast.makeText(getActivity(), getString(R.string.Devicenotplaying), Toast.LENGTH_SHORT).show();
        } else {
            LUCIControl controlPlay = new LUCIControl(currentSceneObject.getIpAddress());
            LSSDPNodes mNodeWeGotForControl = mScanHandler.getLSSDPNodeFromCentralDB(currentIpAddress);
            if (mNodeWeGotForControl == null)
                return;
            if (currentSceneObject.getCurrentSource() == DMR_SOURCE) {
                isLocalDMRPlayback = true;
            } else {
                isLocalDMRPlayback = false;
            }
            switch (view.getId()) {
                case R.id.media_btn_play:
                    if (currentSceneObject.getCurrentSource() == Constants.AUX_SOURCE
                            || currentSceneObject.getCurrentSource() == Constants.GCAST_SOURCE
                            || currentSceneObject.getCurrentSource() == Constants.VTUNER_SOURCE
                            || currentSceneObject.getCurrentSource() == Constants.TUNEIN_SOURCE
                            || (currentSceneObject.getCurrentSource() == Constants.BT_SOURCE
                            && ((mNodeWeGotForControl.getgCastVerision() == null
                            && ((mNodeWeGotForControl.getBT_CONTROLLER() == 4)
                            || mNodeWeGotForControl.getBT_CONTROLLER() == 0))
                            || (mNodeWeGotForControl.getgCastVerision() != null
                            && (mNodeWeGotForControl.getBT_CONTROLLER() < 2))))) {

                        LibreError error = new LibreError("", getResources().getString(R.string.PLAY_PAUSE_NOT_ALLOWED), 1);
                        BusProvider.getInstance().post(error);
                        return;

                    }
                    if (currentSceneObject.getCurrentSource() == Constants.NO_SOURCE ||
                            (currentSceneObject.getCurrentSource() == Constants.DMR_SOURCE && (currentSceneObject.getPlaystatus() == SceneObject.CURRENTLY_STOPPED
                                    || currentSceneObject.getPlaystatus() == SceneObject.CURRENTLY_NOTPLAYING))) {
                        LibreLogger.d(this, "currently not playing, so take user to sources option activity");
                        gotoSourcesOption(currentSceneObject.getIpAddress(), currentSceneObject.getCurrentSource());
                        return;
                    }

                    if (currentSceneObject.getPlaystatus() == SceneObject.CURRENTLY_PLAYING) {

                        if (doPlayPause(true) == true) {
                            // currentSceneObject.setPlaystatus(SceneObject.CURRENTLY_PAUSED);
                            play.setImageResource(R.mipmap.play_with_gl);
                            next.setImageResource(R.mipmap.next_without_glow);
                            previous.setImageResource(R.mipmap.prev_without_glow);
                            mutebutton.setImageResource(R.mipmap.mut_without_glow);
                        }
                    } else {

                        if (doPlayPause(false) == true) {

                            // currentSceneObject.setPlaystatus(SceneObject.CURRENTLY_PLAYING);
                            play.setImageResource(R.mipmap.pause_with_glow);
                            next.setImageResource(R.mipmap.next_with_glow);
                            previous.setImageResource(R.mipmap.prev_with_glow);
                            mutebutton.setImageResource(R.mipmap.mut_with_glow);
                        }

                    }
                    break;
                case R.id.media_btn_previous:
                    if (currentSceneObject.getCurrentSource() == Constants.AUX_SOURCE
                            || currentSceneObject.getCurrentSource() == Constants.GCAST_SOURCE
                            || currentSceneObject.getCurrentSource() == Constants.VTUNER_SOURCE
                            || currentSceneObject.getCurrentSource() == Constants.TUNEIN_SOURCE
                            || (currentSceneObject.getCurrentSource() == Constants.BT_SOURCE
                            && ((mNodeWeGotForControl.getgCastVerision() == null
                            && ((mNodeWeGotForControl.getBT_CONTROLLER() == 4)
                            || mNodeWeGotForControl.getBT_CONTROLLER() == 0))
                            || (mNodeWeGotForControl.getgCastVerision() != null
                            && (mNodeWeGotForControl.getBT_CONTROLLER() < 2))))) {
                        LibreError error = new LibreError("", getResources().getString(R.string.NEXT_PREVIOUS_NOT_ALLOWED), 1);
                        BusProvider.getInstance().post(error);
                        return;
                    }

                    if (isActivePlayistNotAvailable(currentSceneObject)) {
                        LibreLogger.d(this, "currently not playing, so take user to sources option activity");
                        gotoSourcesOption(currentIpAddress, currentSceneObject.getCurrentSource());
                        return;
                    }
                    if (previous.isEnabled()) {
                        doNextPrevious(false);
                    } else {
                        LibreError error = new LibreError(currentIpAddress, getString(R.string.requestTimeout));
                        BusProvider.getInstance().post(error);
                    }
                    break;
                case R.id.media_btn_nxt:
                    LibreLogger.d(this, "bhargav1");
                    if (currentSceneObject.getCurrentSource() == Constants.AUX_SOURCE
                            || currentSceneObject.getCurrentSource() == Constants.GCAST_SOURCE
                            || currentSceneObject.getCurrentSource() == Constants.VTUNER_SOURCE
                            || currentSceneObject.getCurrentSource() == Constants.TUNEIN_SOURCE
                            || (currentSceneObject.getCurrentSource() == Constants.BT_SOURCE
                            && (mNodeWeGotForControl.getgCastVerision() == null
                            && (((mNodeWeGotForControl.getBT_CONTROLLER() == 4)
                            || mNodeWeGotForControl.getBT_CONTROLLER() == 0))
                            || (mNodeWeGotForControl.getgCastVerision() != null
                            && (mNodeWeGotForControl.getBT_CONTROLLER() < 2))))) {
                        LibreError error = new LibreError("", getResources().getString(R.string.NEXT_PREVIOUS_NOT_ALLOWED), 1);
                        BusProvider.getInstance().post(error);
                        return;
                    }

                    if (isActivePlayistNotAvailable(currentSceneObject)) {
                        LibreLogger.d(this, "currently not playing, so take user to sources option activity");
                        gotoSourcesOption(currentIpAddress, currentSceneObject.getCurrentSource());
                        return;
                    }
                    if (next.isEnabled()) {
                        LibreLogger.d(this, "bhargav12");
                        doNextPrevious(true);
                    } else {
                        LibreLogger.d(this, "bhargav1else");
                        LibreError error = new LibreError(currentIpAddress, getString(R.string.requestTimeout));
                        BusProvider.getInstance().post(error);
                    }
                    break;

                case R.id.favourite_button:
                    LUCIControl favLuci = new LUCIControl(currentSceneObject.getIpAddress());
                    if (currentSceneObject.isFavourite()) {
                        favLuci.SendCommand(MIDCONST.MID_FAVOURITE, "GENERIC_FAV_DELETE", LSSDPCONST.LUCI_SET);
                    } else {
                        favLuci.SendCommand(MIDCONST.MID_FAVOURITE, "FAV_SAVE", LSSDPCONST.LUCI_SET);
                    }


//                    Toast.makeText(getActivity(), "Action completed", Toast.LENGTH_SHORT).show();

//                    Snackbar.make(getActivity().findViewById(android.R.id.content), "Action completed", Snackbar.LENGTH_LONG)
//                            .setAction("Error", null)
//                            .show();

                    break;

                case R.id.shuffle:

                    /*this is added to support shuffle and repeat option for Local Content*/
                    if (isLocalDMRPlayback) {
                        RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentSceneObject.getIpAddress());
                        if (renderingDevice != null) {
                            String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                            PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
                            if (playbackHelper != null) {
                                if (currentSceneObject.getShuffleState() == 0) {
                                /*which means shuffle is off hence making it on*/
                                    playbackHelper.setIsShuffleOn(true);
                                    currentSceneObject.setShuffleState(1);
                                } else {
                                    /*which means shuffle is on hence making it off*/
                                    currentSceneObject.setShuffleState(0);
                                    playbackHelper.setIsShuffleOn(false);
                                }
                                /*inserting to central repo*/
                                mScanHandler.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject);
                                setViews();
                            }
                        }
                    } else {
                        LUCIControl luciControl = new LUCIControl(currentSceneObject.getIpAddress());

                        if (currentSceneObject.getShuffleState() == 0) {
                        /*which means shuffle is off*/
                            luciControl.SendCommand(MIDCONST.MID_PLAYCONTROL, "SHUFFLE:ON", LSSDPCONST.LUCI_SET);

                        } else
                            luciControl.SendCommand(MIDCONST.MID_PLAYCONTROL, "SHUFFLE:OFF", LSSDPCONST.LUCI_SET);
                    }
                    break;


                case R.id.repeat:

                     /*this is added to support shuffle and repeat option for Local Content*/
                    if (isLocalDMRPlayback) {
                        RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentSceneObject.getIpAddress());
                        if (renderingDevice != null) {
                            String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                            PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
                            if (playbackHelper != null) {
                                if (currentSceneObject.getRepeatState() == REPEAT_ALL) {
                                    playbackHelper.setRepeatState(REPEAT_OFF);
                                    currentSceneObject.setRepeatState(REPEAT_OFF);
                                } else if (currentSceneObject.getRepeatState() == REPEAT_OFF) {
                                    playbackHelper.setRepeatState(REPEAT_ONE);
                                    currentSceneObject.setRepeatState(REPEAT_ONE);
                                } else if (currentSceneObject.getRepeatState() == REPEAT_ONE) {
                                    playbackHelper.setRepeatState(REPEAT_ALL);
                                    currentSceneObject.setRepeatState(REPEAT_ALL);
                                }
                                /*inserting to central repo*/
                                mScanHandler.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject);
                                setViews();
                            }
                        }
                    } else {
                        /**/
                        LUCIControl shuffleLuciControl = new LUCIControl(currentSceneObject.getIpAddress());
                        if (currentSceneObject.getRepeatState() == REPEAT_ALL) {
                            shuffleLuciControl.SendCommand(MIDCONST.MID_PLAYCONTROL, "REPEAT:OFF", LSSDPCONST.LUCI_SET);
                            repeatButton.setImageResource(R.drawable.ic_repeat_white);
                            currentSceneObject.setRepeatState(REPEAT_OFF);
                        }
                        //Not equal to spotify
                        else if (currentSceneObject.getRepeatState() == REPEAT_OFF && currentSceneObject.getCurrentSource() != Constants.SPOTIFY_SOURCE) {

                            shuffleLuciControl.SendCommand(MIDCONST.MID_PLAYCONTROL, "REPEAT:ONE", LSSDPCONST.LUCI_SET);
                            repeatButton.setImageResource(R.drawable.ic_repeat_one);
                            currentSceneObject.setRepeatState(REPEAT_ONE);
                        }

                        /* If the current source is spotify  */
                        else if (currentSceneObject.getRepeatState() == REPEAT_OFF && currentSceneObject.getCurrentSource() == Constants.SPOTIFY_SOURCE) {
                            shuffleLuciControl.SendCommand(MIDCONST.MID_PLAYCONTROL, "REPEAT:ALL", LSSDPCONST.LUCI_SET);
                            repeatButton.setImageResource(R.drawable.ic_repeatall);
                            currentSceneObject.setRepeatState(REPEAT_ALL);
                        } else if (currentSceneObject.getRepeatState() == REPEAT_ONE) {
                            shuffleLuciControl.SendCommand(MIDCONST.MID_PLAYCONTROL, "REPEAT:ALL", LSSDPCONST.LUCI_SET);
                            repeatButton.setImageResource(R.drawable.ic_repeatall);
                            currentSceneObject.setRepeatState(REPEAT_ALL);
                        }
                        setViews();
                        /**//*
                        LUCIControl shuffleLuciControl = new LUCIControl(currentSceneObject.getIpAddress());
                        if (currentSceneObject.getRepeatState() == REPEAT_ALL) {
                            shuffleLuciControl.SendCommand(MIDCONST.MID_PLAYCONTROL, "REPEAT:OFF", LSSDPCONST.LUCI_SET);
                        } else if (currentSceneObject.getRepeatState() == REPEAT_OFF) {
                            shuffleLuciControl.SendCommand(MIDCONST.MID_PLAYCONTROL, "REPEAT:ONE", LSSDPCONST.LUCI_SET);
                        } else if (currentSceneObject.getRepeatState() == REPEAT_ONE)
                            shuffleLuciControl.SendCommand(MIDCONST.MID_PLAYCONTROL, "REPEAT:ALL", LSSDPCONST.LUCI_SET);
*/
                    }
                    break;


                case R.id.nextSceneButton: {
                    ViewPager nowPlayingViewPager = (ViewPager) getActivity().findViewById(R.id.now_playing_viewpager);
                    nowPlayingViewPager.setCurrentItem(nowPlayingViewPager.getCurrentItem() + 1, true);
                }
                break;
                case R.id.prevSceneButton: {
                    ViewPager nowPlayingViewPager = (ViewPager) getActivity().findViewById(R.id.now_playing_viewpager);
                    nowPlayingViewPager.setCurrentItem(nowPlayingViewPager.getCurrentItem() - 1, true);
                }
                break;
            }
        }
    }


    /*this method is to get UI and asynchronous call*/
    private void sendLuci() {

        LibreLogger.d(this, "NowPlaying: Sending the async command and 41 and 64 for ip address " + currentIpAddress);
        LUCIControl luciControl = new LUCIControl(currentIpAddress);
                /*Sending asynchronous registration*/
        luciControl.sendAsynchronousCommand();

        luciControl.SendCommand(41, "GETUI:PLAY", 2);
        luciControl.SendCommand(MIDCONST.ZONE_VOLUME, null, 1);
        luciControl.SendCommand(MIDCONST.VOLUME_CONTROL, null, 1);
        /* Getting SCene Name To Update in UI */
        luciControl.SendCommand(MIDCONST.MID_SCENE_NAME, null, 1);

        /* This is where we get the currentSource */
        luciControl.SendCommand(50, null, 1);
        luciControl.SendCommand(51, null, 1);

        luciControl.SendCommand(49, null, 1);

        //luciControl.SendCommand(222, "0:6", 1);


    }


    @Override
    public void onResume() {

        super.onResume();
        registerForDeviceEvents(this);
        /*Praveena*
        /*this should not be commented -else source switching from DMR tro other wont work*/
        isLocalDMRPlayback = false;


        sendLuci();
        setViews();
        setMarquees();

        if (currentSceneObject == null)
            return;

          /* This is done to make sure we retain the album
           art even when the fragment gets recycled in viewpager */
        String album_url = "";
        if (currentSceneObject.getCurrentSource() != 14
                && currentSceneObject.getCurrentSource() != 19
                && currentSceneObject.getCurrentSource() != Constants.GCAST_SOURCE) {
            if (currentSceneObject.getAlbum_art() != null && currentSceneObject.getAlbum_art().equalsIgnoreCase("coverart.jpg")) {

                album_url = "http://" + currentSceneObject.getIpAddress() + "/" + "coverart.jpg";

                boolean mInvalidated = mInvalidateTheAlbumArt(currentSceneObject, album_url);
                LibreLogger.d(this, "Invalidated the URL " + album_url + " Status " + mInvalidated);

                PicassoTrustCertificates.getInstance(getActivity()).load(album_url)
                        .error(R.mipmap.album_art).placeholder(R.mipmap.album_art)
                        .into(albumArt);
            } else if (currentSceneObject.getAlbum_art() == null || ("".equals(currentSceneObject.getAlbum_art().trim()))) {
                if (currentSceneObject.getCurrentSource() == Constants.ALEXA_SOURCE){
                    albumArt.setImageDrawable(getResources().getDrawable(R.drawable.amazonlogo_playing));
                } else {
                    albumArt.setImageDrawable(getResources().getDrawable(R.mipmap.album_art));
                }
            } else {
                album_url = currentSceneObject.getAlbum_art();
                boolean mInvalidated = mInvalidateTheAlbumArt(currentSceneObject, album_url);
                LibreLogger.d(this, "Invalidated the URL " + album_url + " Status " + mInvalidated);
                LSSDPNodes mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress);

                if (!album_url.trim().equalsIgnoreCase("")) {

                    if (mNode != null
                            && mNode.getCurrentSource() != MIDCONST.GCAST_SOURCE) {
                        PicassoTrustCertificates.getInstance(getActivity()).load(album_url)
                            /*.memoryPolicy(MemoryPolicy.NO_CACHE).networkPolicy(NetworkPolicy.NO_CACHE)*/
                                .error(R.mipmap.album_art).placeholder(R.mipmap.album_art)
                                .into(albumArt);
                    }
                }

            }
          /*  if (currentSceneObject.getCurrentSource() == 18) { // QQ MUSIC
                disableSeekBarNextPrevious();
            }*/
        } else {

            if (currentSceneObject.getCurrentSource() == Constants.ALEXA_SOURCE){
                albumArt.setImageResource(R.drawable.amazonlogo_playing);
            } else {
                albumArt.setImageResource(R.mipmap.album_art);
            }
        /*    album_url = "http://" + currentSceneObject.getIpAddress() + "/" + "coverart1.jpg";
            PicassoTrustCertificates.getInstance(getActivity()).load(album_url).memoryPolicy(MemoryPolicy.NO_CACHE).networkPolicy(NetworkPolicy.NO_CACHE)
                    .error(R.mipmap.album_art)
                    .into(albumArt);
        */
        }

        audioRecordUtil = AudioRecordUtil.getAudioRecordUtil();
        registerFragmentMicExceptionActivityListener(this);
        phoneIp = new Utils().getIPAddress(true);
        micTcpServer = MicTcpServer.getMicTcpServer();
    }

    private boolean mInvalidateTheAlbumArt(SceneObject scene, String album_url) {
        if (!scene.getmPreviousTrackName().equalsIgnoreCase(scene.getTrackName())) {

//            PicassoTrustCertificates.getInstance(getActivity()).invalidate(album_url);
            scene.setmPreviousTrackName(scene.getTrackName());
            SceneObject sceneObjectFromCentralRepo = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(scene.getIpAddress());
            if (sceneObjectFromCentralRepo != null) {
                sceneObjectFromCentralRepo.setmPreviousTrackName(scene.getTrackName());
            }

            return true;
        }
        return false;
    }

    public void onStop() {
        super.onStop();
        unRegisterForDeviceEvents();
        unregisterFragmentMicExceptionActivityListener();
        showLoaderHandler.removeCallbacksAndMessages(null);
        startPlaybackTimerhandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        Visualizer.stopListeningThread();
    }

    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {
        /* Karuna : For Updating the Device Count when A Slave Got Removed */
        deviceCount.setText("" + mScanHandler.getNumberOfSlavesForMasterIp(currentIpAddress,
                mScanHandler.getconnectedSSIDname(getActivity().getApplicationContext())));
    }

    @Override
    public void deviceGotRemoved(String mIpAddress) {
        /* Karuna : For Updating the Device Count when A Slave Got Removed */
        deviceCount.setText("" + mScanHandler.getNumberOfSlavesForMasterIp(currentIpAddress,
                mScanHandler.getconnectedSSIDname(getActivity().getApplicationContext())));
    }

    @Override
    public void messageRecieved(NettyData dataRecived) {
        LibreLogger.d(this, "Nowplaying: New message appeared for the device " + dataRecived.getRemotedeviceIp());
        if (dataRecived != null &&
                (new LUCIPacket(dataRecived.getMessage()).getCommand() == MIDCONST.MID_DEVICE_STATE_ACK)) {
            deviceCount.setText("" + mScanHandler.getNumberOfSlavesForMasterIp(currentIpAddress,
                    mScanHandler.getconnectedSSIDname(getActivity().getApplicationContext())));
        }
        if (currentSceneObject != null && dataRecived.getRemotedeviceIp().equalsIgnoreCase(currentIpAddress)) {
            LUCIPacket packet = new LUCIPacket(dataRecived.getMessage());
            LibreLogger.d(this, "Packet is _" + packet.getCommand());

            switch (packet.getCommand()) {
              /*  case MIDCONST.MID_DEVICE_STATE_ACK:
                    deviceCount.setText("" + mScanHandler.getNumberOfSlavesForMasterIp(currentIpAddress,
                            mScanHandler.getConnectedSSIDName(getActivity().getApplicationContext())));
                    break;*/
                case MIDCONST.MID_SCENE_NAME: {
                    /* This message box indicates the Scene Name*//*
;*/                 /* if Command Type 1 , then Scene Name information will be come in the same packet
if command type 2 and command status is 1 , then data will be empty., at that time we should not update the value .*/
                    if (packet.getCommandStatus() == 1
                            && packet.getCommandType() == 2) {
                        return;
                    }
                    String message = new String(packet.getpayload());
                    //int duration = Integer.parseInt(message);
                    try {
                        currentSceneObject.setSceneName(message);// = duration/60000.0f;
                        LibreLogger.d(this, "Recieved the Scene Name to be " + currentSceneObject.getSceneName());
                        if (mScanHandler.isIpAvailableInCentralSceneRepo(currentIpAddress)) {
                            mScanHandler.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject);
                            sceneName.setText(currentSceneObject.getSceneName());
                        }
                    } catch (Exception e) {

                    }
                }
                break;

                case 49: {
//                This message box indicates the current playing status of the scene, information like current seek position*//*
                    String message = new String(packet.getpayload());
                    if (!message.equals("")) {
                        long longDuration = Long.parseLong(message);

                        currentSceneObject.setCurrentPlaybackSeekPosition(longDuration);

                        /* Setting the current seekbar progress -Start*/
                        float duration = currentSceneObject.getCurrentPlaybackSeekPosition();
                        seekSongBar.setMax((int) currentSceneObject.getTotalTimeOfTheTrack() / 1000);
                        seekSongBar.setSecondaryProgress((int) currentSceneObject.getTotalTimeOfTheTrack() / 1000);
                        Log.d("SEEK", "Duration = " + duration / 1000);
                        seekSongBar.setProgress((int) duration / 1000);

                        DecimalFormat twoDForm = new DecimalFormat("#.##");
                           currentPlayPosition.setText(convertMilisecondsToTimeString((int) duration / 1000));
                            totalPlayPosition.setText(convertMilisecondsToTimeString(currentSceneObject.getTotalTimeOfTheTrack() / 1000));

                        /* Setting the current seekbar progress -END*/

                        setTheSourceIconFromCurrentSceneObject();


                            currentPlayPosition.setText(convertMilisecondsToTimeString((long) (duration / 1000)));
                            totalPlayPosition.setText(convertMilisecondsToTimeString(currentSceneObject.getTotalTimeOfTheTrack() / 1000));

                            Log.d("Bhargav SEEK", "Duration = " + duration / 1000);
                            Log.d("SEEK", "Duration = " + duration / 1000);



                        is49MsgBoxReceived = true;
                        if (isStillPlaying) {
                            /*For now we check this only in NowPlaying screen, hence add and remove handler only when fragment is active*/
                            if (getActivity() != null && !getActivity().isFinishing()) {
                            /*stop any previous handler started by removing them*/
                                startPlaybackTimerhandler.removeCallbacks(startPlaybackTimerRunnable);
                            /*start fresh handler as timer to montior device got removed or 49 not recieved after 5 seconds*/
                                startPlaybackTimerhandler.postDelayed(startPlaybackTimerRunnable, PLAYBACK_TIMER_TIMEOUT);
                            }
                        }


                    }

                    if (mScanHandler.isIpAvailableInCentralSceneRepo(currentIpAddress)) {
                        mScanHandler.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject);
                    }
                    LibreLogger.d(this, "Nowplaying: Recieved the current Seek position to be " + (int) currentSceneObject.getCurrentPlaybackSeekPosition());
                    break;
                }

                case 50: {

                    String message = new String(packet.getpayload());
                    try {
                        int duration = Integer.parseInt(message);

                        currentSceneObject.setCurrentSource(duration);
                        setTheSourceIconFromCurrentSceneObject();
                        if (currentSceneObject.getCurrentSource()==14||currentSceneObject.getCurrentSource()==19||currentSceneObject.getCurrentSource()==0||currentSceneObject.getCurrentSource()==12){
                            albumArt.setImageResource(R.mipmap.album_art);
                        }
                        LibreLogger.d(this, "Recieved the current source as  " + currentSceneObject.getCurrentSource());

                        if (currentSceneObject.getCurrentSource() == Constants.ALEXA_SOURCE/*alexa*/){
                           /* currentSceneObject.setAlbum_name("");
                            currentSceneObject.setTrackName("");
                            currentSceneObject.setArtist_name("");*/
                           // currentSceneObject.setTotalTimeOfTheTrack(0);
                           /* currentSceneObject.setPlayUrl("");*/
                            if (mScanHandler.isIpAvailableInCentralSceneRepo(currentIpAddress)) {
                                mScanHandler.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject);
                            }
//                            albumArt.setImageResource(R.mipmap.amazon_album_art);
                            disableViews(currentSceneObject.getCurrentSource(), "");
                        }
                        
					/*Song name, artist name, genre being empty*/
                        setViews();
                        /*if (duration == 14) {
                            disableViews("Aux Playing");
                        } else if (duration == 19) {
                            disableViews("BT Playing");
                        } else if (duration == 18) {
                            disableSeekBarNextPrevious();
                        } else {
                            enableViews();
                        }

*/
                    } catch (Exception e) {

                    }
                    if (mScanHandler.isIpAvailableInCentralSceneRepo(currentIpAddress)) {
                        mScanHandler.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject);
                    }
                    break;
                }
//                case MIDCONST.ZONE_VOLUME: {
//
//                    String message = new String(packet.getpayload());
//                    try {
//                        int duration = Integer.parseInt(message);
//                        currentSceneObject.setvolumeZoneInPercentage(duration);
//                        volumeBar.setProgress(duration);
//                        LibreLogger.d(this, "Recieved the current volume to be " + currentSceneObject.getvolumeZoneInPercentage());
//                    } catch (Exception e) {
//
//                    }
//                    if (mScanHandler.isIpAvailableInCentralSceneRepo(currentIpAddress)) {
//                        mScanHandler.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject);
//                    }
//                }
//                break;
                case MIDCONST.MID_FAVOURITE: {
                    String message = new String(packet.getpayload());
                    LibreLogger.d(this, "70 msg box" + "payload->" + message);
                    switch (message) {
                        case "GENERIC_FAV_SAVE_SUCCESS":
                            /**fav success*/
                            currentSceneObject.setIsFavourite(true);
                            favButton.setImageResource(R.drawable.ic_favorites_orange);
                            Toast.makeText(getActivity(), Constants.FAV_SUCCESSFUL, Toast.LENGTH_SHORT).show();
                            break;
                        case "GENERIC_FAV_DELETE_SUCCESS":
                            /**fav deleted*/
                            currentSceneObject.setIsFavourite(false);
                            favButton.setImageResource(R.drawable.ic_favorites_white);
                            Toast.makeText(getActivity(), getResources().getString(R.string.FAV_DELETED), Toast.LENGTH_SHORT).show();
                            break;
                        case "GENERIC_FAV_SAVE_FAILED":
                            Toast.makeText(getActivity(), getResources().getString(R.string.FAV_FAILED), Toast.LENGTH_SHORT).show();
                            break;
                        case "GENERIC_FAV_EXISTS":
                            Toast.makeText(getActivity(), getResources().getString(R.string.FAV_EXISTS), Toast.LENGTH_SHORT).show();
                            break;
                        case "GENERIC_FAV_VTUNER_DEL":
                            Toast.makeText(getActivity(), getResources().getString(R.string.VTUNER_FAV_DELETE), Toast.LENGTH_SHORT).show();
                            break;
                        default:
                    }
                }
                break;
                case 51: {
                    String message = new String(packet.getpayload());

                    LibreLogger.d(this, "MB : 51, msg = " + message);
                    try {
                        int duration = Integer.parseInt(message);
                        currentSceneObject.setPlaystatus(duration);
                        if (currentSceneObject.getPlaystatus() == SceneObject.CURRENTLY_PLAYING) {
                            if (currentSceneObject.getCurrentSource() != MIDCONST.GCAST_SOURCE) {
                                play.setImageResource(R.mipmap.pause_with_glow);
                                next.setImageResource(R.mipmap.next_with_glow);
                                previous.setImageResource(R.mipmap.prev_with_glow);
                            }
                            if (currentSceneObject.getCurrentSource() != 19) {
                                showLoaderHandler.sendEmptyMessage(Constants.PREPARATION_COMPLETED);
                                showLoaderHandler.removeMessages(Constants.PREPARATION_TIMEOUT_CONST);
                            }
                            isStillPlaying = true;

                        } else {
                            if (currentSceneObject.getCurrentSource() != MIDCONST.GCAST_SOURCE) {
                                play.setImageResource(R.mipmap.play_with_gl);
                                next.setImageResource(R.mipmap.next_without_glow);
                                previous.setImageResource(R.mipmap.prev_without_glow);

                            }
                            if (currentSceneObject.getPlaystatus() == SceneObject.CURRENTLY_PAUSED) {
                                isStillPlaying = false;
                            /* this case happens only when the user has paused from the App so close the existing loader if any */
                                if (currentSceneObject.getCurrentSource() != 19) {
                                    showLoaderHandler.sendEmptyMessage(Constants.PREPARATION_COMPLETED);
                                    showLoaderHandler.removeMessages(Constants.PREPARATION_TIMEOUT_CONST);
                                }

                            }
                            if (currentSceneObject.getPlaystatus() == SceneObject.CURRENTLY_STOPPED && currentSceneObject.getCurrentSource() != 0) {

                                if (currentSceneObject.getCurrentSource() != 19
                                        && currentSceneObject.getCurrentSource() != 15
                                        && currentSceneObject.getCurrentSource() != Constants.GCAST_SOURCE
                                        && currentSceneObject.getCurrentSource() != Constants.ALEXA_SOURCE) {
                                    /* When I am Stopping it , will change the album art to Default instead of Invalidate */
                                   /* String default_album_url = "http://" + currentSceneObject.getIpAddress() + "/" + "coverart1.jpg";
                                    PicassoTrustCertificates.getInstance(getActivity()).load(default_album_url).
                                            placeholder(R.mipmap.album_art)
                                            .error(R.mipmap.album_art).into(albumArt);

    */
                                    showLoaderHandler.sendEmptyMessageDelayed(Constants.PREPARATION_TIMEOUT_CONST, Constants.PREPARATION_TIMEOUT);
                                    showLoaderHandler.sendEmptyMessage(Constants.PREPARATION_INITIATED);
                                }
                            }
                        }

                        setTheSourceIconFromCurrentSceneObject();


/*                            if(currentSceneObject.getCurrentSource() == 19){
                                disableViews("BT is Playing");
                            }else {
//                            if (isLoclaDMRPlayback) {
                            *//*device is playing now so closing loader*//*
                                showLoaderHandler.sendEmptyMessage(Constants.PREPARATION_COMPLETED);
                                showLoaderHandler.removeMessages(Constants.PREPARATION_TIMEOUT_CONST);
//                            }

                            }
                            LibreLogger.d(this, "Play state is received");


                        } else if (currentSceneObject.getPlaystatus() == SceneObject.CURRENTLY_PAUSED) {
                            if(currentSceneObject.getCurrentSource() == 19){
                                disableViews("BT is Paused");
                            }
                            LibreLogger.d(this, "Pause state is received");
                        } else {
                            *//*which means stopped*//*
                            play.setImageResource(R.mipmap.play_with_gl);
                            next.setImageResource(R.mipmap.next_without_glow);
                            previous.setImageResource(R.mipmap.prev_without_glow);

                            if(currentSceneObject.getCurrentSource() == 19) {
                                disableViews("BT is Stopped");
                            }else {
//                            if (isLoclaDMRPlayback) {
                            *//*which means state is not playing*//*
                                showLoaderHandler.sendEmptyMessageDelayed(Constants.PREPARATION_TIMEOUT_CONST, Constants.PREPARATION_TIMEOUT);
                                showLoaderHandler.sendEmptyMessage(Constants.PREPARATION_INITIATED);
//                            }
                            }*/
                        LibreLogger.d(this, "Stop state is received");
                        LibreLogger.d(this, "Recieved the playstate to be" + currentSceneObject.getPlaystatus());
                    } catch (Exception e) {

                    }
                    if (mScanHandler.isIpAvailableInCentralSceneRepo(currentIpAddress)) {
                        mScanHandler.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject);
                    }
                }
                break;

                case 66: {

                    /*String message = new String(packet.getpayload());
                    try {

                        if ( updateBuilder==null) {

                            updateBuilder = new AlertDialog.Builder(getActivity());
                            mProgressView=new TextView(getActivity());

                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);

                            mProgressView.setLayoutParams(lp);
                            mProgressView.setGravity(Gravity.CENTER);
                            mProgressView.setText(message);
                            mProgressView.setPadding(10,10,10,10);

                            updateBuilder.setTitle(getString(R.string.FirmwareUpdateinProgress)).setCancelable(false);
                            updateBuilder.setView(mProgressView);
                            updateBuilder.setPositiveButton(getString(R.string.back), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    getActivity().onBackPressed();
                                }
                            }).show();

                        }
                        else{
                            mProgressView.setText(message);
                        }
                    } catch (Exception e) {

                        e.printStackTrace();


                    }*/

                }
                break;
                case 40: {
                    /*            "Error_Fail" ,
                     "Error_NoURL" ,
                        "Error_LastSong"
*/
                    String message = new String(packet.getpayload());
                    String ERROR_FAIL = "Error_Fail";
                    String ERROR_NOURL = "Error_NoURL";
                    String ERROR_LASTSONG = "Error_LastSong";
                    LibreLogger.d(this, "recieved 40 " + message);
                    try {
                        if (message.equalsIgnoreCase(ERROR_FAIL)) {
                            LibreError error = new LibreError(currentSceneObject.getIpAddress(), Constants.ERROR_FAIL);
                            BusProvider.getInstance().post(error);
                        } else if (message.equalsIgnoreCase(ERROR_NOURL)) {
                            LibreError error = new LibreError(currentSceneObject.getIpAddress(), Constants.ERROR_NOURL);
                            BusProvider.getInstance().post(error);
                        } else if (message.equalsIgnoreCase(ERROR_LASTSONG)) {
                            LibreError error = new LibreError(currentSceneObject.getIpAddress(), Constants.ERROR_LASTSONG);
                            BusProvider.getInstance().post(error);

                            showLoaderHandler.sendEmptyMessage(Constants.PREPARATION_COMPLETED);
                            preparingToPlay(false);
                            showLoaderHandler.removeMessages(Constants.PREPARATION_TIMEOUT_CONST);
                        }

                    } catch (Exception e) {

                    }
                }
                break;

                case MIDCONST.VOLUME_CONTROL: {

                    String message = new String(packet.getpayload());
                    try {
                        int duration = Integer.parseInt(message);
                        currentSceneObject.setVolumeValueInPercentage(duration);
                        LibreApplication.INDIVIDUAL_VOLUME_MAP.put(dataRecived.getRemotedeviceIp(), duration);
                        if (mScanHandler.isIpAvailableInCentralSceneRepo(currentIpAddress)) {
                            // if(currentSceneObject.getCurrentSource()==Constants.AIRPLAY_SOURCE)
                            //volumeBar.setProgress(duration);
                            volumeBar.setProgress(LibreApplication./*ZONE_VOLUME_MAP*/INDIVIDUAL_VOLUME_MAP.get(currentSceneObject.getIpAddress()));
                        }
                        LibreLogger.d(this, "Recieved the current volume to be " + currentSceneObject.getVolumeValueInPercentage());
                    } catch (Exception e) {

                    }
                    if (mScanHandler.isIpAvailableInCentralSceneRepo(currentIpAddress)) {
                        mScanHandler.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject);
                    }
                }
                break;

                case 222: {

                    /*String message = new String(packet.payload);
                    if (message.equalsIgnoreCase("0:2")|| message.equalsIgnoreCase("2")) {

                        if (getActivity() != null) {
                            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle("New Update Available!").setMessage("Do you wish to update the firmware now?")
                                    .setCancelable(false)
                                    .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.cancel();
                                        }
                                    })
                                    .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            LUCIControl favLuci = new LUCIControl(currentSceneObject.getIpAddress());
                                            favLuci.SendCommand(55, "", LSSDPCONST.LUCI_SET);

                                        }


                                    });

                            AlertDialog alertDialog1 = builder.create();
                            alertDialog1.show();
                        }
                    }
                 else   if (message.equalsIgnoreCase("0:3")|| message.equalsIgnoreCase("3")) {

                        if (getActivity() != null) {
                            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle("New Update Available!").setMessage("Do you wish to update the firmware now?")
                                    .setCancelable(false)
                                    .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.cancel();
                                        }
                                    })
                                    .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            LUCIControl favLuci = new LUCIControl(currentSceneObject.getIpAddress());
                                            favLuci.SendCommand(222, "0:4", LSSDPCONST.LUCI_SET);

                                        }


                                    });

                            AlertDialog alertDialog1 = builder.create();
                            alertDialog1.show();
                        }
                    }
                    else if((message.equalsIgnoreCase("0:4"))|| message.equalsIgnoreCase("4")){
                        // forceful update
                        if (getActivity() != null) {
                            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle("Cast Device Updated").setMessage("Devce will reboot")
                                    .setCancelable(false)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            Intent intent = new Intent(getActivity(), ActiveScenesListActivity.class);
                                            startActivity(intent);
                                            getActivity().finish();
                                        }


                                    });

                            AlertDialog alertDialog1 = builder.create();
                            alertDialog1.show();
                        }

                    }*/

                }
                break;

                case 42: {


                    try {

                        String message = new String(packet.payload);

                        LibreLogger.d(this, "MB : 42, msg = " + message);
                        JSONObject root = new JSONObject(message);
                        int cmd_id = root.getInt(LUCIMESSAGES.TAG_CMD_ID);
                        JSONObject window = root.getJSONObject(LUCIMESSAGES.ALEXA_KEY_WINDOW_CONTENT);
                        LibreLogger.d(this, "PLAY JSON is \n= " + message + "\n For ip" + currentIpAddress);


                        if (cmd_id == 3) {

                            showLoaderHandler.sendEmptyMessageDelayed(Constants.PREPARATION_COMPLETED, 1000);
                            showLoaderHandler.removeMessages(Constants.PREPARATION_TIMEOUT_CONST);
                           /* String newTrackname=window.getString("TrackName");
                            int newPlayState= window.getInt("PlayState");
                            int currentPlayState= window.getInt("Current_time");

                            if (!newTrackname.equalsIgnoreCase(currentSceneObject.getSceneName()) || newPlayState!=currentSceneObject.getPlaystatus()) {


                                if(!newTrackname.equalsIgnoreCase(currentSceneObject.getSceneName()))

                                {   *//* this is done to avoid  image refresh everytime the 42 message is recieved and the song playing back is the same *//*
                                    Picasso.with(getActivity()).invalidate("http://" + currentSceneObject.getIpAddress() + "/" + currentSceneObject.getAlbum_art());
                                }

                                currentSceneObject.setSceneName(window.getString("TrackName"));
                                currentSceneObject.setAlbum_art(window.getString("CoverArtUrl"));

                                if (currentPlayState>=0)
                                    currentSceneObject.setPlaystatus(window.getInt("PlayState"));
                            */
                            String newTrackname = window.getString("TrackName");
                            int newPlayState = window.getInt("PlayState");
                            int currentPlayState = window.getInt("Current_time");
                            int currentSource = window.getInt("Current Source");
                            String album_arturl = window.getString("CoverArtUrl");
                            String genre = window.getString("Genre");


                            String nAlbumName = window.getString("Album");
                            String nArtistName = window.getString("Artist");
                            long totaltime = window.getLong("TotalTime");
                            AppUtils.INSTANCE.parseControlJsonForAlexa(window,currentSceneObject);
                            // currentSceneObject.setSceneName(window.getString("TrackName"));
                            currentSceneObject.setPlaystatus(window.getInt("PlayState"));
                            currentSceneObject.setAlbum_art(album_arturl);
                            String mPlayURL = window.getString("PlayUrl");
                            if (mPlayURL != null)
                                currentSceneObject.setPlayUrl(mPlayURL);
                            currentSceneObject.setTrackName(window.getString("TrackName"));
                            /*For favourite*/
                            currentSceneObject.setIsFavourite(window.getBoolean("Favourite"));

                            if (genre != null)
                                currentSceneObject.setGenre(window.getString("Genre"));


                            if (!LibreApplication.LOCAL_IP.equals("") && currentSceneObject.getPlayUrl().contains(LibreApplication.LOCAL_IP))
                                isLocalDMRPlayback = true;
                            else if (currentSceneObject.getCurrentSource() == DMR_SOURCE)
                                isLocalDMRPlayback = true;


                            /*Added for Shuffle and Repeat*/
                            if (isLocalDMRPlayback) {
                                /**this is for local content*/
                                RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentSceneObject.getIpAddress());
                                if (renderingDevice != null) {
                                    String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                                    PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
                                    if (playbackHelper != null) {
                                        /**shuffle is on*/
                                        if (playbackHelper.isShuffleOn()) {
                                            currentSceneObject.setShuffleState(1);
                                        } else {
                                            currentSceneObject.setShuffleState(0);
                                        }
                                        /*setting by default*/
                                        if (playbackHelper.getRepeatState() == REPEAT_ONE) {
                                            currentSceneObject.setRepeatState(REPEAT_ONE);
                                        }
                                        if (playbackHelper.getRepeatState() == REPEAT_ALL) {
                                            currentSceneObject.setRepeatState(REPEAT_ALL);
                                        }
                                        if (playbackHelper.getRepeatState() == REPEAT_OFF) {
                                            currentSceneObject.setRepeatState(REPEAT_OFF);
                                        }

                                    }
                                }
                            } else {
                                /**this check made as we will not get shuffle and repeat state in 42, case of DMR
                                 * so we are updating it locally*/
                                currentSceneObject.setShuffleState(window.getInt("Shuffle"));
                                currentSceneObject.setRepeatState(window.getInt("Repeat"));
                            }


                            currentSceneObject.setAlbum_name(nAlbumName);
                            currentSceneObject.setArtist_name(nArtistName);
                            currentSceneObject.setTotalTimeOfTheTrack(totaltime);
                            currentSceneObject.setCurrentSource(currentSource);

                            setViews(); //This will take care of disabling the views


/*                            if (currentSource == 14) { // AUX
                                disableViews("Aux Playing");
                            } else if (currentSource == 19) {
                                disableViews("BT Playing");
                            }else  if (currentSource == 18) { // QQ MUSIC // KK Change For QQ MUSIC Disable the Seekbar
                                disableSeekBarNextPrevious();
                            } else {
                                enableViews();
                                setViews();
                            }*/
                            String album_url = "";
                            if (album_arturl.contains("http://")) {
                                album_url = currentSceneObject.getAlbum_art();
                            } else if (currentSceneObject.getCurrentSource() == Constants.ALEXA_SOURCE) {
//                                albumArt.setImageResource(R.mipmap.amazon_album_art);
                            } else {
                                album_url = "http://" + currentSceneObject.getIpAddress() + "/" + "coverart.jpg";
                            }

                            if (currentSceneObject.getTrackName() == null
                                    || !newTrackname.equalsIgnoreCase(currentSceneObject.getTrackName()))
//                                PicassoTrustCertificates.getInstance(getActivity()).invalidate(album_url);
                            LibreLogger.d(this, "Recieved the scene details as trackname = " + currentSceneObject.getSceneName() + ": " + currentSceneObject.getCurrentPlaybackSeekPosition());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (mScanHandler.isIpAvailableInCentralSceneRepo(currentIpAddress)) {
                        mScanHandler.putSceneObjectToCentralRepo(currentIpAddress, currentSceneObject);
                    }
                    break;

                }
                case 54: {
                    String message = new String(packet.payload);
                    LibreLogger.d(this, " message 54 recieved  " + message);
                    try {
                        LibreError error = null;
                        if (message != null && (message.equalsIgnoreCase(Constants.DMR_PLAYBACK_COMPLETED) || message.contains(Constants.FAIL))) {

                            /* If the song completes then make the seekbar to the starting of the song */
                            currentSceneObject.setCurrentPlaybackSeekPosition(0);
                            seekSongBar.setProgress(0);
                        /* This is handled in nett
                           LibreLogger.d(this,"Libre logger sent the DMR playback completed ");
                            LibreLogger.d(this,"DMR completed for URL "+currentSceneObject.getPlayUrl());
                                if( currentSceneObject.getPlayUrl()!=null && currentSceneObject.getPlayUrl().contains(com.libre.luci.Utils.getLocalV4Address(Utils.getActiveNetworkInterface()).getHostAddress()))
                            {
                                LibreLogger.d(this,"App is going to next song");

                                doNextPrevious(true);
                            }*/
                        } else if (message.contains(Constants.FAIL)) {
                            error = new LibreError(currentIpAddress, getResources().getString(R.string.FAIL_ALERT_TEXT));
                        } else if (message.contains(Constants.SUCCESS)) {
                            closeLoader();
                        } else if (message.contains(Constants.NO_URL)) {
                            error = new LibreError(currentIpAddress, getResources().getString(R.string.NO_URL_ALERT_TEXT));
                        } else if (message.contains(Constants.NO_PREV_SONG)) {
                            error = new LibreError(currentIpAddress, getResources().getString(R.string.NO_PREV_SONG_ALERT_TEXT));
                        } else if (message.contains(Constants.NO_NEXT_SONG)) {
                            error = new LibreError(currentIpAddress, getResources().getString(R.string.NO_NEXT_SONG_ALERT_TEXT));
                        } else if (message.contains(Constants.DMR_SONG_UNSUPPORTED)) {
                            error = new LibreError(currentIpAddress, getResources().getString(R.string.SONG_NOT_SUPPORTED));
                        } else if (message.contains(LUCIMESSAGES.NEXTMESSAGE)) {
                            doNextPrevious(true, true);
                        } else if (message.contains(LUCIMESSAGES.PREVMESSAGE)) {
                            doNextPrevious(false, true);
                        }
//                        PicassoTrustCertificates.getInstance(getActivity()).invalidate(currentSceneObject.getAlbum_art());
                        closeLoader();
                        if (error != null)
                            BusProvider.getInstance().post(error);

                    } catch (Exception e) {
                        e.printStackTrace();
                        LibreLogger.d(this, " Json exception ");

                    }
                }
                break;

                case MIDCONST.MID_MIC: {

                    String message = new String(packet.getpayload());
                    if (message.contains("BLOCKED")){
                        audioRecordUtil.stopRecording();
                        String dialogMsg = getString(R.string.deviceMicOn);
                        micAlertDialog(dialogMsg);
                    }
                }
                break;


            }
        }

    }


    /*showing loader for playing condition*/
    private void showLoader() {
        if (getActivity().isFinishing())
            return;

        if (m_progressDlg == null)
            m_progressDlg = ProgressDialog.show(getActivity(), getString(R.string.notice), getString(R.string.prepareingToPlay), true, true, null);

        if (!m_progressDlg.isShowing()) {
            m_progressDlg.show();
        }
    }

    /*closing loader*/
    private void closeLoader() {

        if (m_progressDlg != null) {

            if (m_progressDlg.isShowing() == true) {
                m_progressDlg.dismiss();
            }

        }

    }


    /*Setting views here*/
    private void setViews() {
        if (currentSceneObject == null) {
            return;
        }

        /*making visibility gone*/
        shuffleButton.setVisibility(View.GONE);
        repeatButton.setVisibility(View.GONE);
        favButton.setVisibility(View.GONE);
        tuneInInfoButton.setVisibility(View.GONE);
        if (currentSceneObject.getAlbum_art()==null || currentSceneObject.getAlbum_art().isEmpty() && currentSceneObject.getCurrentSource() == Constants.ALEXA_SOURCE){
            albumArt.setImageDrawable(getResources().getDrawable(R.drawable.amazonlogo_playing));
        }

        if (currentSceneObject.getCurrentSource() == TUNEIN_SOURCE) {
            tuneInInfoButton.setVisibility(View.VISIBLE);
        }



        /*setting views*/
       /* if (currentSceneObject.getVolumeValueInPercentage() >= 0)
            volumeBar.setProgress(currentSceneObject.getVolumeValueInPercentage());*/

//        if (currentSceneObject.getvolumeZoneInPercentage() >= 0)
//            volumeBar.setProgress(currentSceneObject.getvolumeZoneInPercentage());
        if (LibreApplication./*ZONE_VOLUME_MAP*/INDIVIDUAL_VOLUME_MAP.containsKey(currentSceneObject.getIpAddress())) {
            volumeBar.setProgress(LibreApplication./*ZONE_VOLUME_MAP*/INDIVIDUAL_VOLUME_MAP.get(currentSceneObject.getIpAddress()));
        } else {
            LUCIControl control = new LUCIControl(currentSceneObject.getIpAddress());
            control.SendCommand(MIDCONST./*ZONE_VOLUME*/VOLUME_CONTROL, null, LSSDPCONST.LUCI_GET);
            if (currentSceneObject.getVolumeValueInPercentage() >= 0)
                volumeBar.setProgress(currentSceneObject.getVolumeValueInPercentage());
        }

        if (currentSceneObject.getArtist_name() != null && !currentSceneObject.getArtist_name().equalsIgnoreCase("NULL")
              ) {
            artistName.setText(currentSceneObject.getArtist_name());
            LibreLogger.d(this,""+currentSceneObject.getArtist_name());
        }else{
            artistName.setText("");
        }
        if (currentSceneObject.getAlbum_name() != null && !currentSceneObject.getAlbum_name().equalsIgnoreCase("NULL")
                ) {
            albumName.setText(currentSceneObject.getAlbum_name());
            LibreLogger.d(this, "" + currentSceneObject.getAlbum_name());
        }else{
            albumName.setText("");
        }
        if (currentSceneObject.getTrackName() != null && !currentSceneObject.getTrackName().equalsIgnoreCase("NULL")
                ) {
            String trackname=currentSceneObject.getTrackName();

            /* This change is done to handle the case of deezer where the song name is appended by radio or skip enabled */
            if (trackname != null && trackname.contains(Constants.DEZER_RADIO))
                trackname = trackname.replace(Constants.DEZER_RADIO, "");

            if (trackname != null && trackname.contains(Constants.DEZER_SONGSKIP))
                trackname = trackname.replace(Constants.DEZER_SONGSKIP, "");

            songName.setText(trackname);
            LibreLogger.d(this, "" + currentSceneObject.getTrackName());
        } else {
            songName.setText("");
        }


        /*this condition making sure that shuffle and repeat is being shown only for USB/SD card*/
        /*added for deezer/tidal source*/
        if (currentSceneObject.getCurrentSource() == USB_SOURCE
                || currentSceneObject.getCurrentSource() == SD_CARD
                || currentSceneObject.getCurrentSource() == DEEZER_SOURCE
                || currentSceneObject.getCurrentSource() == TIDAL_SOURCE
                || currentSceneObject.getCurrentSource() == Constants.SPOTIFY_SOURCE
                || currentSceneObject.getCurrentSource() == NETWORK_DEVICES
                || currentSceneObject.getCurrentSource() == FAV_SOURCE
                || currentSceneObject.getCurrentSource() == DMR_SOURCE
                ) {

            /*making visibile only for USB/SD card*/
            shuffleButton.setVisibility(View.VISIBLE);
            repeatButton.setVisibility(View.VISIBLE);

            if (currentSceneObject.getShuffleState() == 0) {
            /*which means shuffle is off*/
                shuffleButton.setImageResource(R.drawable.ic_shuffle_white);
            } else {
            /*shuffle is on */
                shuffleButton.setImageResource(R.drawable.ic_shuffle_on);
            }


            /*if other is playing local DMR we should hide repeat and shuffle*/
            if (currentSceneObject.getPlayUrl() != null && !currentSceneObject.getPlayUrl().contains(LibreApplication.LOCAL_IP) && (currentSceneObject.getCurrentSource() == DMR_SOURCE)) {
                shuffleButton.setVisibility(View.GONE);
                repeatButton.setVisibility(View.GONE);
            }
        /*this for repeat state*/
            switch (currentSceneObject.getRepeatState()) {
                case REPEAT_OFF:
                    repeatButton.setImageResource(R.drawable.ic_repeat_white);
                    break;
                case REPEAT_ONE:
                    repeatButton.setImageResource(R.drawable.ic_repeat_one);
                    break;
                case REPEAT_ALL:
                    repeatButton.setImageResource(R.drawable.ic_repeatall);
                    break;
            }
        }

        if (currentSceneObject.getCurrentSource() == DEEZER_SOURCE
                || currentSceneObject.getCurrentSource() == TIDAL_SOURCE
                || currentSceneObject.getCurrentSource() == VTUNER_SOURCE
                || currentSceneObject.getCurrentSource() == TUNEIN_SOURCE
                || currentSceneObject.getCurrentSource() == FAV_SOURCE) {
            favButton.setVisibility(View.VISIBLE);

            if (currentSceneObject.isFavourite())
                favButton.setImageResource(R.drawable.ic_favorites_orange);
            else
                favButton.setImageResource(R.drawable.ic_favorites_white);

        }


        sceneName.setText("" + currentSceneObject.getSceneName());
        if (currentSceneObject.getPlaystatus() == SceneObject.CURRENTLY_PLAYING) {
            if (shudUpdateIcons(currentIpAddress, currentSceneObject)) {
                play.setImageResource(R.mipmap.pause_with_glow);
                Log.e("sumacheck", "playcheck1");
                next.setImageResource(R.mipmap.next_with_glow);
                previous.setImageResource(R.mipmap.prev_with_glow);
            }
        } else {
            if (shudUpdateIcons(currentIpAddress, currentSceneObject)) {
                play.setImageResource(R.mipmap.play_with_gl);
                next.setImageResource(R.mipmap.next_without_glow);
                previous.setImageResource(R.mipmap.prev_without_glow);
            }
        }
        if (currentSceneObject.getCurrentSource() == MIDCONST.GCAST_SOURCE) {
            play.setImageResource(R.mipmap.pause_with_glow);
            next.setImageResource(R.mipmap.next_without_glow);
            previous.setImageResource(R.mipmap.prev_without_glow);
        }

        /* Setting the current seekbar progress -Start*/
        float duration = currentSceneObject.getCurrentPlaybackSeekPosition();
        seekSongBar.setMax((int) currentSceneObject.getTotalTimeOfTheTrack() / 1000);
        seekSongBar.setSecondaryProgress((int) currentSceneObject.getTotalTimeOfTheTrack() / 1000);
        Log.d("SEEK", "Duration = " + duration / 1000);
        seekSongBar.setProgress((int) duration / 1000);

        DecimalFormat twoDForm = new DecimalFormat("#.##");
            currentPlayPosition.setText(convertMilisecondsToTimeString((int) duration / 1000));
            totalPlayPosition.setText(convertMilisecondsToTimeString(currentSceneObject.getTotalTimeOfTheTrack() / 1000));


        if (currentIpAddress != null && currentSceneObject.getTrackName() != null
                && (!currentTrackName.equalsIgnoreCase(currentSceneObject.getTrackName()))) {
            String album_url = "";

            /* Added to handle the case where trackname is empty string"*/
//            if (currentSceneObject.getTrackName().trim().length() > 0)
//                currentTrackName = currentSceneObject.getTrackName();

            currentTrackName = currentSceneObject.getTrackName();

            if (currentSceneObject.getCurrentSource() != 14
                    && currentSceneObject.getCurrentSource() != 19
                    && currentSceneObject.getCurrentSource() != Constants.GCAST_SOURCE) {
                if (currentSceneObject.getAlbum_art() != null &&
                        currentSceneObject.getAlbum_art().equalsIgnoreCase("coverart.jpg") && currentSceneObject.getCurrentSource() != Constants.ALEXA_SOURCE) {

                    album_url = "http://" + currentSceneObject.getIpAddress() + "/" + "coverart.jpg";

                        /* If Track Name is Different just Invalidate the Path
                        * And if we are resuming the Screen(Screen OFF and Screen ON) , it will not re-download it */
                    boolean mInvalidated = mInvalidateTheAlbumArt(currentSceneObject, album_url);
                    LibreLogger.d(this, "Invalidated the URL " + album_url + " Status " + mInvalidated);
                    PicassoTrustCertificates.getInstance(getActivity()).load(album_url)
                            .error(R.mipmap.album_art).placeholder(R.mipmap.album_art)
                            .into(albumArt);
                } else if (currentSceneObject.getAlbum_art() == null || ("".equals(currentSceneObject.getAlbum_art().trim()))) {
                    if (currentSceneObject.getCurrentSource() == Constants.ALEXA_SOURCE) {
                        albumArt.setImageDrawable(getResources().getDrawable(R.drawable.amazonlogo_playing));
                    } else {
                        albumArt.setImageDrawable(getResources().getDrawable(R.mipmap.album_art));
                    }
                } else {
                    album_url = currentSceneObject.getAlbum_art();
                    boolean mInvalidated = mInvalidateTheAlbumArt(currentSceneObject, album_url);
                    LibreLogger.d(this, "Invalidated the URL " + album_url + " Status " + mInvalidated);
                    if (!album_url.trim().equalsIgnoreCase(""))
                        PicassoTrustCertificates.getInstance(getActivity()).load(album_url).placeholder(R.mipmap.album_art)
                                /*.memoryPolicy(MemoryPolicy.NO_CACHE).networkPolicy(NetworkPolicy.NO_CACHE)*/
                                .error(R.mipmap.album_art)
                                .into(albumArt);
                }
                /*if (currentSceneObject.getCurrentSource() == 18) { // QQ MUSIC
                    disableSeekBarNextPrevious();
                }*/
            } else {


                if (currentSceneObject.getCurrentSource() == Constants.ALEXA_SOURCE) {
//                    albumArt.setImageDrawable(getResources().getDrawable(R.mipmap.amazon_album_art));
                } else {
                    /*    album_url = "http://" + currentSceneObject.getIpAddress() + "/" + "coverart1.jpg";
                PicassoTrustCertificates.getInstance(getActivity()).load(album_url).
                        placeholder(R.mipmap.album_art)
                        .memoryPolicy(MemoryPolicy.NO_CACHE)
                        .networkPolicy(NetworkPolicy.NO_CACHE)
                        .error(R.mipmap.album_art)
                        .into(albumArt);*/
                    PicassoTrustCertificates.getInstance(getActivity())
                            .load(R.mipmap.album_art)
                            .placeholder(R.mipmap.album_art)
                            .memoryPolicy(MemoryPolicy.NO_STORE);
                }


            }

            LSSDPNodes mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress);
            if (mNode != null) {
                if (mNode.getCurrentSource() == MIDCONST.GCAST_SOURCE) {
                    albumArt.setImageResource(R.mipmap.album_art);
                }
            }
            if (mNode != null && mNode.getCurrentSource() == Constants.ALEXA_SOURCE) {
                setControlsForAlexaSource(currentSceneObject);
            } else {
                alexaSourceImage.setVisibility(View.GONE);
            }
            setTheSourceIconFromCurrentSceneObject();
        }
    }

    private void setControlsForAlexaSource(SceneObject currentSceneObject) {
        if (currentSceneObject==null){
            return;
        }
        alexaSourceImage.setVisibility(View.VISIBLE);
        ((NowPlayingActivity)getActivity()).setControlIconsForAlexa(currentSceneObject,play,next,previous);
       /* String alexaSourceURL = currentSceneObject.getAlexaSourceImageURL();
        if (alexaSourceURL!=null){
            setImageFromURL(alexaSourceURL,R.drawable.default_album_art,alexaSourceImage);
        }*/
        if (currentSceneObject.getPlayUrl().toLowerCase().contains("tunein")) {
            alexaSourceImage.setImageResource(R.drawable.tunein_image2);
        } else if (currentSceneObject.getPlayUrl().toLowerCase().contains("iheartradio")) {
            alexaSourceImage.setImageResource(R.drawable.iheartradio_image2);
        } else if (currentSceneObject.getPlayUrl().toLowerCase().contains("amazon music")) {
            alexaSourceImage.setImageResource(R.drawable.amazon_image2);
        }
        else if(currentSceneObject.getPlayUrl().toLowerCase().contains("siriusxm")){
            alexaSourceImage.setImageResource(R.drawable.sirius_image2);
        } else{
            alexaSourceImage.setImageResource(R.mipmap.amazon_default);
        }
    }

    /* This Function For QQMusic To Disable the Seekbar Seek and Previous and Next*/
    private void disableSeekBarNextPrevious() {
        //seekSongBar.setClickable(false);

        previous.setClickable(false);
        next.setClickable(false);
    }

    /* This function takes care of setting the image next to sources button depending on the
    current source that is being played.
    */
    private void setTheSourceIconFromCurrentSceneObject() {

        if (mSources != null) {

            /* Enabling the views by default which gets updated to disabled based on the need */

            enableViews();

            Drawable img = getActivity().getResources().getDrawable(
                    R.mipmap.ic_sources);
            if (currentSceneObject == null)
                return;
            if (currentSceneObject.getCurrentSource() == WiFi_For_Gear4) {
                // This is done because when they switch between BT / AUX to WiFi mode which is the normal mode. We recieve source as 0
                //do nothing because we have used enableviews() which does the work
                disableViews(currentSceneObject.getCurrentSource(), "WiFi mode");
                LibreLogger.d("this", " Gear4 Recieved sources to be " + currentSceneObject.getCurrentSource() + " - returned to WiFi mode by hardkey press");
            }
            if (currentSceneObject.getCurrentSource() == 1) {
                img = getActivity().getResources().getDrawable(
                        R.mipmap.airplay_logo);
                disableViews(currentSceneObject.getCurrentSource(), "AirPlay Mode");
            }

            if (currentSceneObject.getCurrentSource() == DMR_SOURCE) {
                img = getActivity().getResources().getDrawable(
                        R.mipmap.dmr_icon);
            }
            if (currentSceneObject.getCurrentSource() == 3) {
                img = getActivity().getResources().getDrawable(
                        R.mipmap.network);
            }

            if (currentSceneObject.getCurrentSource() == 4) {
                img = getActivity().getResources().getDrawable(
                        R.mipmap.spotify);
            }
            if (currentSceneObject.getCurrentSource() == 5) {
                img = getActivity().getResources().getDrawable(
                        R.mipmap.usb);
            }
            if (currentSceneObject.getCurrentSource() == 6) {
                img = getActivity().getResources().getDrawable(
                        R.mipmap.sdcard);
            }
            if (currentSceneObject.getCurrentSource() == VTUNER_SOURCE) {
                img = getActivity().getResources().getDrawable(
                        R.mipmap.vtuner_logo);

                /*disabling views for VTUNER*/
                disableViews(currentSceneObject.getCurrentSource(), currentSceneObject.getAlbum_name());
                favButton.setVisibility(View.VISIBLE);

            }
            if (currentSceneObject.getCurrentSource() == TUNEIN_SOURCE) {
                img = getActivity().getResources().getDrawable(
                        R.mipmap.tunein_logo1);

                /*disabling views for TUNEIN*/
                disableViews(currentSceneObject.getCurrentSource(), currentSceneObject.getAlbum_name());
                favButton.setVisibility(View.VISIBLE);

            }
            if (currentSceneObject.getCurrentSource() == 14) { // Mail
                img = getActivity().getResources().getDrawable(
                        R.mipmap.aux_in);
                /* added to make sure we dont show the album art during aux */


                disableViews(currentSceneObject.getCurrentSource(), getString(R.string.aux));
            }
            if (currentSceneObject.getCurrentSource() == 18) {
                img = getActivity().getResources().getDrawable(
                        R.mipmap.qqmusic);
                disableViews(currentSceneObject.getCurrentSource(), "");

            }
            if (currentSceneObject.getCurrentSource() == 19) {
                img = getActivity().getResources().getDrawable(
                        R.mipmap.bluetooth);

                if (currentSceneObject.getPlaystatus() == SceneObject.CURRENTLY_STOPPED) {
                    disableViews(currentSceneObject.getCurrentSource(), getString(R.string.btOn));
                } else if (currentSceneObject.getPlaystatus() == SceneObject.CURRENTLY_PAUSED) {
                    disableViews(currentSceneObject.getCurrentSource(), getString(R.string.btOn));
                } else {
                    disableViews(currentSceneObject.getCurrentSource(), getString(R.string.btOn));

                }


            }
            if (currentSceneObject.getCurrentSource() == DEEZER_SOURCE) {
                img = getActivity().getResources().getDrawable(
                        R.mipmap.deezer_logo);
                favButton.setVisibility(View.VISIBLE);
                disableViews(currentSceneObject.getCurrentSource(), currentSceneObject.getTrackName());


            }
            if (currentSceneObject.getCurrentSource() == TIDAL_SOURCE) {
                img = getActivity().getResources().getDrawable(
                        R.mipmap.tidal_white_logo);
                favButton.setVisibility(View.VISIBLE);
            }

            if (currentSceneObject.getCurrentSource() == FAV_SOURCE) {
                img = getActivity().getResources().getDrawable(
                        R.drawable.ic_favorites_white);
            }

            if (currentSceneObject.getCurrentSource() == 24) {
                img = getActivity().getResources().getDrawable(
                        R.mipmap.ic_cast_white_24dp_2x);

                disableViews(currentSceneObject.getCurrentSource(), "gCast is playing");
            }

            if (currentSceneObject.getCurrentSource() == ROON_SOURCE) {
                img = getActivity().getResources().getDrawable(
                        R.mipmap.ic_roon);

            }

            if(currentSceneObject.getCurrentSource() == Constants.ALEXA_SOURCE /*alexa*/){
                img = getActivity().getResources().getDrawable(
                        R.mipmap.ic_sources);

            }
           /* Drawable  img = getActivity().getResources().getDrawable(
                        R.mipmap.ic_sources);

            if(currentSceneObject.getCurrentSource()==3){
                img = getActivity().getResources().getDrawable(
                        R.mipmap.dmr_icon);
            }

            if(currentSceneObject.getCurrentSource()==4){
                img = getActivity().getResources().getDrawable(
                        R.mipmap.spotify);
            }
            if(currentSceneObject.getCurrentSource()==5){
                img = getActivity().getResources().getDrawable(
                        R.mipmap.usb);
            }
            if(currentSceneObject.getCurrentSource()==6){
                img = getActivity().getResources().getDrawable(
                        R.mipmap.sdcard);
            }
            if(currentSceneObject.getCurrentSource()==8){
                img = getActivity().getResources().getDrawable(
                        R.mipmap.vtuner_logo);
            }
            if(currentSceneObject.getCurrentSource()==19){
                img = getActivity().getResources().getDrawable(
                        R.mipmap.tunein_logo1);
            }
            if(currentSceneObject.getCurrentSource()==20){
                img = getActivity().getResources().getDrawable(
                        R.mipmap.deezer_logo);
            }
            if(currentSceneObject.getCurrentSource()==21){
                img = getActivity().getResources().getDrawable(
                                R.mipmap.tidal_logo1);
            }*/

            mSourcesIcon.setImageDrawable(img);
            //           img.setBounds(0, 0, convertPixelsToDp(75), convertPixelsToDp(75));
            //mSources.setCompoundDrawables(img, null, null, null);
/*


            if(currentSceneObject.getCurrentSource()==4)
                mSources.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.spotify, 0, 0, 0);
            if(currentSceneObject.getCurrentSource()==5)
                mSources.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.usb, 0, 0, 0);
            if(currentSceneObject.getCurrentSource()==6)
                mSources.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.sdcard, 0, 0, 0);
            if(currentSceneObject.getCurrentSource()==8)
                mSources.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.vtuner_logo, 0, 0, 0);
            if(currentSceneObject.getCurrentSource()==19)
                mSources.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.tunein_logo1, 0, 0, 0);
            if(currentSceneObject.getCurrentSource()==20)
                mSources.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.deezer_logo, 0, 0, 0);
            if(currentSceneObject.getCurrentSource()==21)
                mSources.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.tidal_logo1, 0, 0, 0);
            mSources.setCompoundDrawablesWithIntrinsicBounds(img, 0, 0, 0);
*/
        }


        handleThePlayIconsForGrayoutOption();
    }

    private void handleThePlayIconsForGrayoutOption() {
        if (previous != null && previous.isEnabled()) {
            previous.setImageResource(R.mipmap.prev_with_glow);
        } else if (previous != null) {
            previous.setImageResource(R.mipmap.prev_without_glow);
        }


        if (next != null && next.isEnabled()) {
            next.setImageResource(R.mipmap.next_with_glow);
        } else if (next != null) {
            next.setImageResource(R.mipmap.next_without_glow);
        }

        if (currentSceneObject.getCurrentSource() != MIDCONST.GCAST_SOURCE) {

            if (play != null && play.isEnabled() == false) {

                play.setImageResource(R.mipmap.play_with_gl);

            } else if (currentSceneObject != null && currentSceneObject.getPlaystatus() == SceneObject.CURRENTLY_PLAYING) {
                play.setImageResource(R.mipmap.pause_with_glow);
            } else {
                play.setImageResource(R.mipmap.play_with_gl);
            }
            playPauseNextPrevious(currentSceneObject);
        }
    }

    public void playPauseNextPrevious(SceneObject sceneObject) {
        if (sceneObject != null && (sceneObject.getCurrentSource() == Constants.VTUNER_SOURCE
                || sceneObject.getCurrentSource() == Constants.TUNEIN_SOURCE
                || sceneObject.getCurrentSource() == Constants.BT_SOURCE
                || sceneObject.getCurrentSource() == Constants.AUX_SOURCE
                || sceneObject.getCurrentSource() == 0)) {
            play.setImageResource(R.mipmap.play_without_glow);
            next.setImageResource(R.mipmap.next_without_glow);
            previous.setImageResource(R.mipmap.prev_without_glow);
        }
    }

    public String convertMilisecondsToTimeString(long time) {

        DecimalFormat twoDForm = new DecimalFormat("#.##");
        int seconds = (int) time % 60;
        int mins = (int) (time / 60) % 60;
        if (seconds < 10)
            return mins + ":0" + seconds;
        return mins + ":" + seconds;
    }

    public DMRProcessor getTheRenderer(String ipAddress) {
        RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(ipAddress);
        if (renderingDevice != null) {
            String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
            PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
            if (playbackHelper != null) {
                LibreApplication.PLAYBACK_HELPER_MAP.put(renderingUDN, playbackHelper);

                DMRControlHelper m_dmrControlHelper = playbackHelper.getDmrHelper();
                if (m_dmrControlHelper != null) {
                    DMRProcessor m_dmrProcessor = m_dmrControlHelper.getDmrProcessor();
                    m_dmrProcessor.removeListener(this);
                    m_dmrProcessor.addListener(this);
                    return m_dmrProcessor;
                } else
                    return null;
            } else
                return null;
        } else
            return null;
    }


    boolean doPlayPause(boolean isCurrentlyPlaying) {

    /*    if (isLocalDMRPlayback) {
            RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentIpAddress);
            if (renderingDevice != null) {
                String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);


                if ( playbackHelper==null|| playbackHelper.getDmsHelper() == null||( currentSceneObject!=null && false==currentSceneObject.getPlayUrl().contains(LibreApplication.LOCAL_IP))) {
                    Toast.makeText(getActivity(), "No active playlist,\nPlease select a song to play", Toast.LENGTH_SHORT).show();


                        *//* In SA mode we need to go to local content while in HN mode we will go to sources option *//*

                    if (LibreApplication.connectedSSID.equalsIgnoreCase(Constants.DDMS_SSID)) {
                        Intent localIntent = new Intent(getActivity(), LocalDMSActivity.class);
                        localIntent.putExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, true);
                        localIntent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress);
                        startActivity(localIntent);
                    } else {
                        Intent localIntent = new Intent(getActivity(), SourcesOptionActivity.class);
                        localIntent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress);
                        localIntent.putExtra("current_source", "" + currentSceneObject.getCurrentSource());
                        localIntent.putExtra(Constants.FROM_ACTIVITY, "NowPlayingFragment");
                        startActivity(localIntent);
                    }
                    getActivity().finish();

                    return false;
                }


                DMRProcessor theRenderer = getTheRenderer(currentIpAddress);
                if (theRenderer == null)
                    return false;
                if (isCurrentlyPlaying){
                    theRenderer.pause();
    isStillPlaying = false;
}
                else{
                    theRenderer.play();
                isStillPlaying = true;
}
                return true;

            }
            else if ( currentSceneObject.getPlayUrl()!=null && currentSceneObject.getPlayUrl().contains(LibreApplication.LOCAL_IP)){
                        *//* renderer is not found and hence there is no playlist also so open the sources option *//*


                *//* In SA mode we need to go to local content while in HN mode we will go to sources option *//*
                if (LibreApplication.connectedSSID.equalsIgnoreCase(Constants.DDMS_SSID)) {
                    Intent localIntent = new Intent(getActivity(), LocalDMSActivity.class);
                    localIntent.putExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, true);
                    localIntent.putExtra(Constants.CURRENT_DEVICE_IP, currentSceneObject.getIpAddress());
                    getActivity().startActivity(localIntent);
                } else {
                    Intent localIntent = new Intent(getActivity(), SourcesOptionActivity.class);
                    localIntent.putExtra(Constants.CURRENT_DEVICE_IP, currentSceneObject.getIpAddress());
                    localIntent.putExtra("current_source", "" + currentSceneObject.getCurrentSource());
                    getActivity().startActivity(localIntent);
                }
                return true;
            }

        } else */
        {

            LUCIControl control = new LUCIControl(currentSceneObject.getIpAddress());
            if (isCurrentlyPlaying) {
                control.SendCommand(MIDCONST.MID_PLAYCONTROL, LUCIMESSAGES.PAUSE, CommandType.SET);
                isStillPlaying = false;
            } else {
                if (currentSceneObject.getCurrentSource() == 19) {
                    control.SendCommand(MIDCONST.MID_PLAYCONTROL, LUCIMESSAGES.PLAY, CommandType.SET);
                    isStillPlaying = true;
                } else {
                    control.SendCommand(MIDCONST.MID_PLAYCONTROL, LUCIMESSAGES.RESUME, CommandType.SET);
                    isStillPlaying = true;
                }
            }

        }
        if (currentSceneObject.getCurrentSource() != 19
                && currentSceneObject.getCurrentSource() != 15
                && currentSceneObject.getCurrentSource() != Constants.GCAST_SOURCE
                && currentSceneObject.getCurrentSource() != Constants.ALEXA_SOURCE) {
            showLoaderHandler.sendEmptyMessageDelayed(Constants.PREPARATION_TIMEOUT_CONST, Constants.PREPARATION_TIMEOUT);
            showLoaderHandler.sendEmptyMessage(Constants.PREPARATION_INITIATED);
        }
        return true;
    }

    /* Seek should work on both Luci command and DMR command */
    boolean doSeek(int pos) {

        String duration = Integer.toString(pos * 1000);

      /*  String duration = Integer.toString(pos * 1000);
        LUCIControl control = new LUCIControl(currentSceneObject.getIpAddress());
        control.SendCommand(40, "SEEK:" + duration, LSSDPCONST.LUCI_SET);*/

        if (isLocalDMRPlayback) {
            DMRProcessor theRenderer = getTheRenderer(currentIpAddress);

            if (theRenderer == null)
                return false;

            String format = ModelUtil.toTimeString(pos);
            theRenderer.seek(format);
            LibreLogger.d(this, "LocalDMR pos = " + pos + " total time of the song " + currentSceneObject.getTotalTimeOfTheTrack() / 1000 + "format = " + format);
            return true;
        }

        /*this is to prevent playback pause when clicking on seekbar while vtuner and tunein */
        else if (currentSceneObject.getCurrentSource() == VTUNER_SOURCE || currentSceneObject.getCurrentSource() == TUNEIN_SOURCE) {
            Toast.makeText(getActivity(), getString(R.string.seek_not_allowed), Toast.LENGTH_SHORT).show();
            return true;
        } else {
            LUCIControl control = new LUCIControl(currentSceneObject.getIpAddress());
            LibreLogger.d(this, "Remote seek = " + pos + " total time of the song " + currentSceneObject.getTotalTimeOfTheTrack());

            //duration = (pos * 0.01 * currentSceneObject.getTotalTimeOfTheTrack()) + "";
            duration = (pos * 1000) + "";
            seekSongBar.setProgress(pos);
            LibreLogger.d(this, "Rempote Seek  = " + duration + " total time of the song " + currentSceneObject.getTotalTimeOfTheTrack());
            control.SendCommand(40, "SEEK:" + duration, LSSDPCONST.LUCI_SET);

        }
        return true;

    }


    boolean doNextPrevious(boolean isNextPressed) {


        if (isLocalDMRPlayback && (currentSceneObject != null && (currentSceneObject.getCurrentSource() == 0 || currentSceneObject.getCurrentSource() == 2))) {
            RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentIpAddress);
            if (renderingDevice != null) {
                String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);

                if (playbackHelper == null || playbackHelper.getDmsHelper() == null || (currentSceneObject != null && false == currentSceneObject.getPlayUrl().contains(LibreApplication.LOCAL_IP))) {
                    Toast.makeText(getActivity(), "No active playlist,\nPlease select a song to play", Toast.LENGTH_SHORT).show();

                    /* In SA mode we need to go to local content while in HN mode we will go to sources option */
                    if (LibreApplication.activeSSID.contains(Constants.DDMS_SSID)) {
                        Intent localIntent = new Intent(getActivity(), LocalDMSActivity.class);
                        localIntent.putExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, true);
                        localIntent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress);
                        startActivity(localIntent);
                    } else {
                        Intent localIntent = new Intent(getActivity(), SourcesOptionActivity.class);
                        localIntent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress);
                        localIntent.putExtra("current_source", "" + currentSceneObject.getCurrentSource());
                        localIntent.putExtra(Constants.FROM_ACTIVITY, "NowPlayingFragment");
                        startActivity(localIntent);
                    }
                    getActivity().finish();

                    return false;
                }

                showLoaderHandler.sendEmptyMessageDelayed(Constants.PREPARATION_TIMEOUT_CONST, Constants.PREPARATION_TIMEOUT);
                showLoaderHandler.sendEmptyMessage(Constants.PREPARATION_INITIATED);


                if (isNextPressed) {
                    playbackHelper.playNextSong(1);
                } else
                    playbackHelper.playNextSong(-1);
                return true;


            } else
                return false;
        } else {


            if (currentSceneObject != null && currentSceneObject.getCurrentSource() == 21) {

                String trackname = currentSceneObject.getTrackName();

                if (isNextPressed) {

                    if (trackname != null && trackname.contains(Constants.DEZER_SONGSKIP)) {

                        closeLoader();
                        Toast.makeText(getActivity(), "Activate Deezer Premium+ from your computer", Toast.LENGTH_LONG).show();
                        return false;
                    }

                }
            }

            if (currentSceneObject.getCurrentSource() != 19
                    && currentSceneObject.getCurrentSource() != 15
                    && currentSceneObject.getCurrentSource() != Constants.GCAST_SOURCE) {

                if (currentSceneObject.getCurrentSource() == Constants.ALEXA_SOURCE) {
                    m_progressDlg = ProgressDialog.show(getActivity(), getString(R.string.pleaseWait), getString(R.string.alexaNextPrevMsg), true, true, null);
                    showLoaderHandler.sendEmptyMessageDelayed(Constants.ALEXA_NEXT_PREV_HANDLER, Constants.ALEXA_NEXT_PREV_TIMEOUT);
                } else {
                    showLoaderHandler.sendEmptyMessageDelayed(Constants.PREPARATION_TIMEOUT_CONST, Constants.PREPARATION_TIMEOUT);
                    showLoaderHandler.sendEmptyMessage(Constants.PREPARATION_INITIATED);
                }
            }
            LUCIControl control = new LUCIControl(currentSceneObject.getIpAddress());
            if (isNextPressed)
                control.SendCommand(MIDCONST.MID_PLAYCONTROL, LUCIMESSAGES.PLAY_NEXT, CommandType.SET);
            else
                control.SendCommand(MIDCONST.MID_PLAYCONTROL, LUCIMESSAGES.PLAY_PREV, CommandType.SET);
            return true;
        }


    }

    boolean doNextPrevious(boolean isNextPressed, boolean mValue) {


        if (isLocalDMRPlayback && (currentSceneObject != null && (currentSceneObject.getCurrentSource() == 0 || currentSceneObject.getCurrentSource() == 2))) {
            RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentIpAddress);
            if (renderingDevice != null) {
                String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);

                if (playbackHelper == null || playbackHelper.getDmsHelper() == null || (currentSceneObject != null && false == currentSceneObject.getPlayUrl().contains(LibreApplication.LOCAL_IP))) {
                    if (mValue)
                        return true;
                    Toast.makeText(getActivity(), "No active playlist,\nPlease select a song to play", Toast.LENGTH_SHORT).show();

                    /* In SA mode we need to go to local content while in HN mode we will go to sources option */
                    if (LibreApplication.activeSSID.contains(Constants.DDMS_SSID)) {
                        Intent localIntent = new Intent(getActivity(), LocalDMSActivity.class);
                        localIntent.putExtra(AppConstants.IS_LOCAL_DEVICE_SELECTED, true);
                        localIntent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress);
                        startActivity(localIntent);
                    } else {
                        Intent localIntent = new Intent(getActivity(), SourcesOptionActivity.class);
                        localIntent.putExtra(Constants.CURRENT_DEVICE_IP, currentIpAddress);
                        localIntent.putExtra("current_source", "" + currentSceneObject.getCurrentSource());
                        localIntent.putExtra(Constants.FROM_ACTIVITY, "NowPlayingFragment");
                        startActivity(localIntent);
                    }
                    getActivity().finish();

                    return false;
                }

                showLoaderHandler.sendEmptyMessageDelayed(Constants.PREPARATION_TIMEOUT_CONST, Constants.PREPARATION_TIMEOUT);
                showLoaderHandler.sendEmptyMessage(Constants.PREPARATION_INITIATED);


                if (isNextPressed) {
                    playbackHelper.playNextSong(1);
                } else {
                   /* Setting the current seekbar progress -Start*/
                    float duration = currentSceneObject.getCurrentPlaybackSeekPosition();
                    Log.d("Current Duration ", "Duration = " + duration / 1000);
                    float durationInSeeconds = duration / 1000;
                    if (durationInSeeconds < 5) {
                        playbackHelper.playNextSong(-1);
                    } else {
                        playbackHelper.playNextSong(0);
                    }
                }
                return true;


            } else
                return false;
        }
        return true;
    }

    public static int convertPixelsToDp(float px) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float dp = px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return (int) dp;
    }

    boolean doVolumeChange(int currentVolumePosition) {

//        if (isLoclaDMRPlayback) {
//
//            DMRProcessor theRenderer = getTheRenderer(currentIpAddress);
//
//            if (theRenderer == null)
//                return false;
//
//            theRenderer.setVolume(currentVolumePosition);
//            return true;
//        }
//        else {
        /* We can make use of CurrentIpAddress instead of CurrenScneObject.getIpAddress*/
        LUCIControl control = new LUCIControl(currentIpAddress);
        if (currentSceneObject.getCurrentSource() != Constants.AIRPLAY_SOURCE) {
            //control.SendCommand(MIDCONST.VOLUME_CONTROL, "" + currentVolumePosition, CommandType.SET);
            control.SendCommand(MIDCONST./*ZONE_VOLUME*/VOLUME_CONTROL, "" + currentVolumePosition, CommandType.SET);
        } else {
            control.SendCommand(MIDCONST.VOLUME_CONTROL, "" + currentVolumePosition, CommandType.SET);
        }
        SceneObject sceneObjectFromCentralRepo = mScanHandler.getSceneObjectFromCentralRepo(currentIpAddress);
        //currentSceneObject.setVolumeValueInPercentage(currentVolumePosition);
        currentSceneObject.setVolumeValueInPercentage(currentVolumePosition);
        if (sceneObjectFromCentralRepo != null) {
            sceneObjectFromCentralRepo.setVolumeValueInPercentage(currentVolumePosition);
//            sceneObjectFromCentralRepo.setvolumeZoneInPercentage(currentVolumePosition);
            mScanHandler.putSceneObjectToCentralRepo(currentIpAddress, sceneObjectFromCentralRepo);
        }
        return true;
//        }


    }

    /*this method will get called whenever hardware volume is pressed*/
    @Override
    public void updateVolumeChangesFromHardwareKey(int keyCode) {


        /* Added to avoid user pressing the volume change when user is getting call*/
        if (NowPlayingActivity.isPhoneCallbeingReceived)
            return;

        if (currentSceneObject == null) {
            Log.d("VolumeHardKey", "Current Scene is--" + null);
            return;
        }

        Log.d("VolumeHardKey", "Device ip is--" + currentSceneObject.getIpAddress());

        LUCIControl volumeControl = new LUCIControl(currentSceneObject.getIpAddress());
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {

            if (volumeBar.getProgress() > 85) {
                //  volumeControl.SendCommand(MIDCONST.VOLUME_CONTROL, "" + 100, CommandType.SET);
                volumeControl.SendCommand(MIDCONST./*ZONE_VOLUME*/VOLUME_CONTROL, "" + 100, CommandType.SET);
            } else {
                //volumeControl.SendCommand(MIDCONST.VOLUME_CONTROL, "" + (volumeBar.getProgress() + 10), CommandType.SET);
                volumeControl.SendCommand(MIDCONST./*ZONE_VOLUME*/VOLUME_CONTROL, "" + (volumeBar.getProgress() + 6), CommandType.SET);
            }
        }

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (volumeBar.getProgress() < 15) {
                //volumeControl.SendCommand(MIDCONST.VOLUME_CONTROL, "" + 0, CommandType.SET);
                volumeControl.SendCommand(MIDCONST./*ZONE_VOLUME*/VOLUME_CONTROL, "" + 0, CommandType.SET);
            } else {
                //volumeControl.SendCommand(MIDCONST.VOLUME_CONTROL, "" + (volumeBar.getProgress() - 10), CommandType.SET);
                volumeControl.SendCommand(MIDCONST./*ZONE_VOLUME*/VOLUME_CONTROL, "" + (volumeBar.getProgress() - 6), CommandType.SET);
            }


        }


    }


    @Override
    public void onUpdatePosition(long position, long duration) {

    }

    @Override
    public void onUpdateVolume(int currentVolume) {

    }

    @Override
    public void onPaused() {

    }

    @Override
    public void onStoped() {

    }

    @Override
    public void onSetURI() {

    }

    @Override
    public void onPlayCompleted() {

    }

    @Override
    public void onPlaying() {

    }

    @Override
    public void onActionSuccess(Action action) {

    }

    @Override
    public void onActionFail(final String actionCallback, UpnpResponse response, String cause) {
        LibreLogger.d(this, " fragment that recieved the callback is " + currentSceneObject.getIpAddress());

        if (cause != null && cause.contains("Seek:Error")) {
            cause = "Seek Failed!";
        }
        LibreError error = new LibreError(currentIpAddress, cause);
        BusProvider.getInstance().post(error);
        LibreLogger.d(this, " fragment posted the error " + currentSceneObject.getIpAddress());

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                /*we are setting seek bar to previous position once seek got failed*/
                if (actionCallback != null && actionCallback.contains(getResources().getString(R.string.SEEK_FAILED)))
                    seekSongBar.setProgress((int) (currentSceneObject.getCurrentPlaybackSeekPosition() / 1000));
            }
        });

    }

    @Override
    public void onExceptionHappend(Action actionCallback, final String mTitle, final String cause) {
        LibreLogger.d(this, "Exception Happend for the Title " + mTitle + " for the cause of " + cause);
        try {

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

//                    Toast.makeText(getActivity(), mTitle + cause, Toast.LENGTH_SHORT).show();

                }
            });
        } catch (Exception e) {

        }
    }

 private boolean shudUpdateIcons(String ipaddress, SceneObject sceneObject){
        LSSDPNodes mNodeWeGotForControl = ScanningHandler.getInstance().getLSSDPNodeFromCentralDB(ipaddress);
        if (sceneObject!=null  && (sceneObject.getCurrentSource() == Constants.VTUNER_SOURCE
                || sceneObject.getCurrentSource() == Constants.TUNEIN_SOURCE
                || ((sceneObject.getCurrentSource() == Constants.BT_SOURCE
                && (mNodeWeGotForControl!=null &&(mNodeWeGotForControl.getgCastVerision() == null
                && ((mNodeWeGotForControl.getBT_CONTROLLER() == 4)
                || mNodeWeGotForControl.getBT_CONTROLLER() == 0))
                ||(mNodeWeGotForControl.getgCastVerision() != null
                && (mNodeWeGotForControl.getBT_CONTROLLER() < 2)))))
                || sceneObject.getCurrentSource() == Constants.AUX_SOURCE
                || sceneObject.getCurrentSource() == 0)) {
            return false;
        }
        return true;
    }

    private void setSceneName(){
        ((NowPlayingActivity)getActivity()).setSceneName();
    }
    @Override
    public boolean onLongClick(View view) {

        if (vpListener!=null)
            vpListener.setViewPagerSwipeable(false);
        LSSDPNodes lssdpNodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(currentIpAddress);
        if (lssdpNodes == null || lssdpNodes.getAlexaRefreshToken()==null || lssdpNodes.getAlexaRefreshToken().isEmpty()){
            Toast.makeText(getActivity(), "Login into Alexa.", Toast.LENGTH_SHORT).show();
            return true;
        }

        startListeningText.setVisibility(View.VISIBLE);
        isButtonLongPressed = true;
        dummyVisualizer.setVisibility(View.VISIBLE);
        dummyVisualizer.startListening();
        if (currentIpAddress!=null && !currentIpAddress.isEmpty()
                && phoneIp!=null && !phoneIp.isEmpty()) {
            new LUCIControl(currentIpAddress).SendCommand(MIDCONST.MID_MIC, Constants.START_MIC + phoneIp + "," + MicTcpServer.MIC_TCP_SERVER_PORT, LSSDPCONST.LUCI_SET);
            Log.d("Bugss","phone ip: "+phoneIp + "port: "+MicTcpServer.MIC_TCP_SERVER_PORT);
            audioRecordUtil.startRecording(this);
        } else {
            Toast.makeText(getActivity(), "Ip not available", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {


        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (isButtonLongPressed) {
                dummyVisualizer.stopListening();
                dummyVisualizer.setVisibility(View.INVISIBLE);
                startListeningText.setVisibility(View.INVISIBLE);
                isButtonLongPressed = false;
                if (vpListener!=null)
                    vpListener.setViewPagerSwipeable(true);

                return true;
            }
        }
        return false;
    }

    @Override
    public void micExceptionCaught(Exception e) {
        if (e.getMessage().contains("Broken pipe")) {
//            micTcpServer.createVerificationFile();
        }
        audioRecordUtil.stopRecording();
    }

    @Override
    public void recordError(String error) {
        ((CTDeviceDiscoveryActivity)getActivity()).showToast(error);
    }

    @Override
    public void recordStopped() {

    }

    @Override
    public void recordProgress(byte[] byteBuffer) {

    }

    @Override
    public void sendBufferAudio(byte[] audioBufferBytes) {
        micTcpServer.sendDataToClient(audioBufferBytes);
    }

    private void micAlertDialog(String alertMsg){
        AlertDialog.Builder requestPermission = new AlertDialog.Builder(getActivity());
        requestPermission.setTitle(getString(R.string.micBlocked))
                .setMessage(alertMsg)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //navigate to settings
                        alertDialog.dismiss();
                    }
                })
                .setCancelable(false);
        if (alertDialog == null) {
            alertDialog = requestPermission.create();
        }
        if (alertDialog != null && !alertDialog.isShowing())
            alertDialog.show();
    }

    public interface DisableVpListener{
        void setViewPagerSwipeable(boolean disable);
    }

    public void removeVpListener() {
        vpListener = null;
    }

    public void setVpListener(DisableVpListener vpListener) {
        this.vpListener = vpListener;
    }
}

