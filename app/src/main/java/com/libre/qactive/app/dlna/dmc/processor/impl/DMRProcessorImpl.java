package com.libre.qactive.app.dlna.dmc.processor.impl;

import android.util.Log;

import com.libre.qactive.Scanning.Constants;
import com.libre.qactive.Scanning.ScanningHandler;
import com.cumulations.libreV2.model.SceneObject;
import com.libre.qactive.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.libre.qactive.util.LibreLogger;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.support.avtransport.callback.GetTransportInfo;
import org.fourthline.cling.support.avtransport.callback.Pause;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.Seek;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.avtransport.callback.Stop;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.SeekMode;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.renderingcontrol.callback.SetVolume;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DMRProcessorImpl implements DMRProcessor {
    public static final int REMOTE_DEV_MAX_VOLUME = 100;
    private static final String TAG = DMRProcessorImpl.class.getName();
    private static final int UPDATE_INTERVAL = 500;
    private final int TIMEOUT_FOR_RESPONSE_AVTRANSPORT = 10;

    //	private static final int ALMOST_COMPLETE_PERCENT = 97;
//	private boolean m_isAlmostCompleted = false;
    private ControlPoint m_controlPoint;
    private RemoteService m_avtransportService = null;
    private RemoteService m_renderingControl = null;

    private List<DMRProcessorListener> m_listeners = new ArrayList<DMRProcessorListener>();
    private boolean m_isRunning = true;
    private boolean m_canUpdatePosition = true;
    private boolean m_canUpdateVolume = true;
    private boolean m_isSettingURI = false;
    private boolean m_hasPendingURI = false;
    private boolean iscompleted;
    private boolean mGotResponse = true;
//    private boolean isPlaybackStarted;


    private int mCountOfTransportInfoError = 0;

    private String m_latestURI = null;
    private String m_latestURIMeta = null;
    private int SLEEP = 500;
    private boolean m_hasSetURI = false;
    RemoteDevice m_device;
    private boolean m_checkGetVolumeInfo = false;
    private int m_currentVolume;


    public enum DMR_STATE_STATUS {
        DEFAULT, STOP_INITATIED_BY_APP, AV_TRANSPORT_SUCESS, PLAYING;
    }

    public DMR_STATE_STATUS mCurrentDMRState;

    private Thread m_updateThread = new Thread(new Runnable() {

        @Override
        public void run() {
            int counter = 0;
            while (m_isRunning) {
                if (m_hasSetURI && !m_isSettingURI) {
                    {
                        // getControlPointStatus(counter);
                    }

                }
                try {
                    Thread.sleep(UPDATE_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
               /* if (m_renderingControl != null && !m_checkGetVolumeInfo) {
                    m_checkGetVolumeInfo = true;
                    m_controlPoint.execute(new GetVolume(m_renderingControl) {
                        @SuppressWarnings("rawtypes")
                        @Override
                        public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                            *//*fireOnFailEvent(invocation.getAction(), operation, defaultMsg);*//*
                            m_checkGetVolumeInfo = false;
                        }

                        @SuppressWarnings("rawtypes")
                        @Override
                        public void received(ActionInvocation actionInvocation, int currentVolume) {
                            if (m_currentVolume != currentVolume) {
                                m_currentVolume = currentVolume;
                                if (m_currentVolume != 0)
                                    fireUpdateVolumeEvent(currentVolume);
                                Log.d(TAG, "current volume" + currentVolume);
//                                m_checkGetVolumeInfo = false;
                            }
                        }
                    });
                }
*/
            }
        }
    });


    private Thread m_GetTransportStateThread = new Thread(new Runnable() {

        @Override
        public void run() {
            int counter = 2;
            while (m_isRunning) {
                if (m_hasSetURI && !m_isSettingURI) {
                    LibreLogger.d(this, "GetTransportStateThread is Running");
                    getControlPointStatus(counter);
                }
                try {
                    Thread.sleep(SLEEP);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    });

    public DMRProcessorImpl(RemoteDevice remoteDevice, RemoteService service, ControlPoint controlPoint) {
        m_controlPoint = controlPoint;
        m_avtransportService = service;

        m_device = remoteDevice;
        m_renderingControl = m_device.findService(new ServiceType("schemas-upnp-org", "RenderingControl"));
        m_updateThread.start();
        m_GetTransportStateThread.start();
    }

    private void getControlPointStatus(int counter) {
        if (m_avtransportService == null)
            return;
        mGotResponse = false;
        switch (counter) {

            case 1:
                break;
            case 2:
                try {

                    m_controlPoint.execute(new GetTransportInfo(m_avtransportService) {
                        @SuppressWarnings("rawtypes")
                        @Override
                        public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {


                             /* Stopping the thread */
                            if(m_isRunning) {

                                m_isRunning = false;
                                //LibreApplication.PLAYBACK_HELPER_MAP.remove(m_device.getIdentity().getUdn().toString());

                                fireOnFailEvent("" + invocation.getAction(), operation,
                                        GetTransportInfo.class.getSimpleName() + ":" + defaultMsg);

                            }


                        }

                        @SuppressWarnings("rawtypes")
                        @Override
                        public void received(ActionInvocation invocation, TransportInfo transportInfo) {
                            //   LibreLogger.d(this, "TransportInfo State" + transportInfo.getCurrentTransportState());
                            LibreLogger.d(this, "Current Dmr State " + mCurrentDMRState);
                            mCountOfTransportInfoError = 0;

                            switch (transportInfo.getCurrentTransportState()) {
                                case PLAYING:
                                    //                                isPlaybackStarted = true;
                                    fireOnPlayingEvent();
                                    Log.d(TAG, "Fireonplayingevent");

                                    break;
                                case PAUSED_PLAYBACK:
                                    fireOnPausedEvent();
                                    Log.d(TAG, "Fireonpauseevent");
                                    break;
                                case STOPPED:

                                    fireOnStopedEvent();
                                    Log.d(TAG, "Fireonstopevent" + "--mAvResponse--" + mCurrentDMRState +
                                            m_device.getDetails().getBaseURL().getHost());
/*
                                    String mIpaddress = m_device.getDetails().getBaseURL().getHost();

                                    ScanningHandler mScanHandler = ScanningHandler.getInstance();
                                    SceneObject currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(mIpaddress);
                                    if (currentSceneObject != null) {
                                        Log.d("DMR_CURRENT_SOURCE", "" + currentSceneObject.getCurrentSource());
                                    }
                                    if (mCurrentDMRState == DMR_STATE_STATUS.DEFAULT) {

                                        LSSDPNodeDB mLssdpNodeDb = LSSDPNodeDB.getInstance();
                                        LSSDPNodes mNode = mLssdpNodeDb.getTheNodeBasedOnTheIpAddress(mIpaddress);
                                        if (mNode != null && mNode.getDeviceState().equals("M")) {
                                            if (currentSceneObject != null && currentSceneObject.getCurrentSource() == 2)
                                                fireOnPlayCompletedEvent();
                                        } else if (mNode == null) {
                                            if (currentSceneObject != null && currentSceneObject.getCurrentSource() == 2)
                                                fireOnPlayCompletedEvent();
                                        } else {
                                            PlaybackHelper mPlay = LibreApplication.PLAYBACK_HELPER_MAP.get(m_device.getIdentity().getUdn().toString());
                                            mPlay = null;
                                            dispose();
                                            LibreApplication.PLAYBACK_HELPER_MAP.remove(m_device.getIdentity().getUdn().toString());
                                        }
                                    } else if (mCurrentDMRState == DMR_STATE_STATUS.STOP_INITATIED_BY_APP) {

                                    } else if (mCurrentDMRState == DMR_STATE_STATUS.AV_TRANSPORT_SUCESS) {

                                    } else if (mCurrentDMRState == DMR_STATE_STATUS.PLAYING) {
//                                        String mIpaddress = m_device.getDetails().getBaseURL().getHost();
                                        LSSDPNodeDB mLssdpNodeDb = LSSDPNodeDB.getInstance();
                                        LSSDPNodes mNode = mLssdpNodeDb.getTheNodeBasedOnTheIpAddress(mIpaddress);
                                        LibreLogger.d(this, "Device State " + mNode.getDeviceState());
                                        if (mNode.getDeviceState().equals("M")) {
                                            if (currentSceneObject != null && currentSceneObject.getCurrentSource() == 2 ) {


                                                if (currentSceneObject.getPlayUrl()!=null && currentSceneObject.getPlayUrl().contains(com.libre.qactive.luci.Utils.getLocalV4Address(Utils.getActiveNetworkInterface()).getHostAddress())&& currentSceneObject.getTotalTimeOfTheTrack()-currentSceneObject.getCurrentPlaybackSeekPosition()<=2000) {
                                                    fireOnPlayCompletedEvent();
                                                    Log.d("Prav-kannada", "Total position " + currentSceneObject.getTotalTimeOfTheTrack() + " -- Current " + currentSceneObject.getCurrentPlaybackSeekPosition());
                                                }

                                            }
                                        } else if (mNode == null) {

                                            if (currentSceneObject != null && currentSceneObject.getCurrentSource() == 2) {

                                                if (currentSceneObject.getPlayUrl()!=null && currentSceneObject.getPlayUrl().contains(com.libre.qactive.luci.Utils.getLocalV4Address(Utils.getActiveNetworkInterface()).getHostAddress())&& currentSceneObject.getTotalTimeOfTheTrack()-currentSceneObject.getCurrentPlaybackSeekPosition()<=2000) {
                                                    fireOnPlayCompletedEvent();
                                                    Log.d("Prav-kannada", "Total position " + currentSceneObject.getTotalTimeOfTheTrack() + " -- Current " + currentSceneObject.getCurrentPlaybackSeekPosition());
                                                }
                                            }
                                        } else {
                                            PlaybackHelper mPlay = LibreApplication.PLAYBACK_HELPER_MAP.get(m_device.getIdentity().getUdn().toString());
                                            mPlay = null;
                                            dispose();
                                            LibreApplication.PLAYBACK_HELPER_MAP.remove(m_device.getIdentity().getUdn().toString());
                                        }
                                    }*/
                                    break;
                                default:
                                    break;
                            }
                        }

                    }).get(TIMEOUT_FOR_RESPONSE_AVTRANSPORT, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    LibreLogger.d(this, "TransportInfo State Failure " + "Interrupted Exception");
                    LibreLogger.d(this, "GetTransportStateThread is Running Interrupted");
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    LibreLogger.d(this, "TransportInfo State Failure " + "ExecutionException Exception");
                    LibreLogger.d(this, "GetTransportStateThread is Running ExecutionException");
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    LibreLogger.d(this, "TransportInfo State Failure " + "TimeoutException Exception" +
                            m_device.getDetails().getBaseURL().getHost());
                    LibreLogger.d(this, "GetTransportStateThread is Running TimeoutException");
                    mCountOfTransportInfoError++;
                    if (mCountOfTransportInfoError >= 5) {

                        String cause = "Timeout Exception " + "For the Device " + m_device.getDetails().getBaseURL().getHost();
//                        fireOnFailEvent("", null, cause);

                        /*posting error to application here we are using bus because it doest extend DeviceDiscoverActivity*/
                      /*  LibreError error = new LibreError(m_device.getDetails().getBaseURL().getHost(), "Device not reachable");
                        BusProvider.getInstance().post(error);*/

//                        synchronized (m_listeners) {
//                            for (DMRProcessorListener listener : m_listeners) {
//                                listener.onExceptionHappend(null, "Timeout Exception", "For the Device " + m_device.getDetails().getBaseURL().getHost());
//
//                            }
//                        }
                        m_isRunning = false;
                       // LibreApplication.PLAYBACK_HELPER_MAP.remove(m_device.getIdentity().getUdn().toString());
                        e.printStackTrace();

                    }

                } catch (Exception e) {
                    LibreLogger.d(this, "TransportInfo State Failure " + "Exception Exception" +
                            m_device.getDetails().getBaseURL().getHost());
                    LibreLogger.d(this, "GetTransportStateThread is Running Exception");
                    /*mCountOfTransportInfoError ++ ;*/
                    /*if(mCountOfTransportInfoError>=5) */
                    {


                        String cause = " Exception " + "For the Device " + m_device.getDetails().getBaseURL().getHost();
//                        fireOnFailEvent("", null, cause);

                         /*posting error to application here we are using bus because it doest extend DeviceDiscoverActivity*/
                      /*  LibreError error = new LibreError(m_device.getDetails().getBaseURL().getHost(), cause);
                        BusProvider.getInstance().post(error);
*/

//                        synchronized (m_listeners) {
//                            for (DMRProcessorListener listener : m_listeners) {
//                                listener.onExceptionHappend(null, " Exception", "For the Device " + m_device.getDetails().getBaseURL().getHost());
//
//                            }
//                        }
                        m_isRunning = false;
                       // LibreApplication.PLAYBACK_HELPER_MAP.remove(m_device.getIdentity().getUdn().toString());
                        e.printStackTrace();

                    }
                }


                break;
            case 3:
                break;
            case 4:
//			m_controlPoint.execute(new GetVolume(m_renderingControl) {
//			@SuppressWarnings("rawtypes")
//			@Override
//			public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
//				fireOnFailEvent(invocation.getAction(), operation, defaultMsg);
//			}
//
//			@SuppressWarnings("rawtypes")
//			@Override
//			public void received(ActionInvocation actionInvocation, int currentVolume) {
//				if (m_lastVolume != currentVolume) {
//					m_lastVolume = currentVolume;
//					fireUpdateVolumeEvent(currentVolume);
//				}
//			}
//		});
                break;
            default:
                break;
        }
    }

    @SuppressWarnings({"rawtypes"})
    @Override
    public void setURI(final String uri, final String uriMeta) {
        //fireOnSetURIEvent(); /*Multiple Set URI Event*/
      /*  m_hasSetURI = true;
        m_latestURI = uri;
        m_latestURIMeta = uriMeta;
        if (m_isSettingURI) {
            Log.e(TAG, "set AV uri pending:" + uri);
            m_hasPendingURI = true;
            return;
        }
        Log.e(TAG, "set AV uri now:" + uri);
        stopMediaServer();

        m_isSettingURI = true;
        m_hasPendingURI = false;
        m_controlPoint.execute(new SetAVTransportURI(m_avtransportService, uri, uriMeta) {
            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
                Log.e(TAG,"Success setting URI to DMR");
                m_isSettingURI = false;
                if (m_hasPendingURI) {
                    setURI(m_latestURI, m_latestURIMeta);
                } else {
                    play();
                }
            }

           *//* @Override
            public ActionInvocation getActionInvocation() {
                int error=getActionInvocation().getFailure().getErrorCode();

                Log.d(TAG,"actionException"+error);
                return super.getActionInvocation();


            }*//*

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
                m_isSettingURI = false;
                Log.e(TAG,"Failed to SetAVTransportURI");


                fireOnFailEvent(invocation.getAction(), response,
                        SetAVTransportURI.class.getSimpleName() + ":" + defaultMsg);
            }
        });*/
        Log.d(TAG, "Set Uri is  Called ");
        mCurrentDMRState = DMR_STATE_STATUS.STOP_INITATIED_BY_APP;

        Stop stop = new Stop(m_avtransportService) {


            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
                Log.d(TAG, "Stop is sucess");

                m_hasSetURI = true;
                m_latestURI = uri;
                m_latestURIMeta = uriMeta;
                if (m_isSettingURI) {
                    Log.i(TAG, "set AV uri pending:" + uri);
                    m_hasPendingURI = true;
                    return;
                }
                Log.i(TAG, "set AV uri now:" + uri);

                m_isSettingURI = true;

                m_hasPendingURI = false;

                m_controlPoint.execute(new SetAVTransportURI(m_avtransportService, uri, uriMeta) {
                    @Override
                    public void success(ActionInvocation invocation) {
                        super.success(invocation);

                        Log.d(TAG, "set uri is sucess");
                        m_isRunning = true;
                        m_isSettingURI = false;
                        iscompleted = false;
                        mCurrentDMRState = DMR_STATE_STATUS.AV_TRANSPORT_SUCESS;
                        play();


                        m_latestURI = uri;
                        ScanningHandler mScanning  =  ScanningHandler.getInstance();
                        SceneObject mObj = mScanning.getSceneObjectFromCentralRepo(m_device.getDetails().getBaseURL().getHost());
                        if (mObj!=null) {
                            mObj.setPlayUrl(uri.toLowerCase());
                        }
                    }

                    @Override
                    public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
                        Log.e(TAG, "Set uri is failed");
                        m_isSettingURI = false;
                        mCurrentDMRState = DMR_STATE_STATUS.DEFAULT;
                      /*  if (response != null) {
                            iscompleted = false;
                            mAvResponse = -1;
                            //   fireOnFailEvent(invocation.getAction(),response,defaultMsg);


                        } else {
                            setURI(uri, uriMeta);
                        }*/

                        m_isRunning=false;
                        fireOnFailEvent("" + invocation.getAction(), response,
                                SetAVTransportURI.class.getSimpleName() + ":" + defaultMsg);

                    }
                });

            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
                fireOnFailEvent("" + invocation.getAction(), response,
                        Stop.class.getSimpleName() + ":" + defaultMsg);
                Log.d(TAG, "Stop is sucess in Failure ");
                mCurrentDMRState = DMR_STATE_STATUS.DEFAULT;

            }
        };
        //  fireOnSetURIEvent();

        m_controlPoint.execute(stop); /* Always Stop Have to be First*/
        //fireOnSetURIEvent();

    }

    @SuppressWarnings("rawtypes")
    @Override
    public void play() {

        if (!m_hasSetURI)
            return;
        Play play = new Play(m_avtransportService) {

            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
                Log.d(TAG, "Play Sucess ");
                mCurrentDMRState = DMR_STATE_STATUS.PLAYING;
                fireOnPlayingEvent();


            }


            @Override
            public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
                if (iscompleted) {

                }
                mCurrentDMRState = DMR_STATE_STATUS.DEFAULT;
                Log.e(TAG, "Play is failed" + defaultMsg + "Response" + response.getResponseDetails());
                fireOnFailEvent("" + invocation.getAction(), response, defaultMsg);

                /*if(mAvResponse!=0){
                    play();
                    mAvResponse=0;
                }

                if (!iscompleted) {
                   // play();
                    iscompleted = true;
                }*/

            }
        };

        m_controlPoint.execute(play);

    }

    @SuppressWarnings("rawtypes")
    @Override
    public void pause() {
        Pause pause = new Pause(m_avtransportService) {

            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
                fireOnFailEvent("" + invocation.getAction(), response,
                        Pause.class.getSimpleName() + ":" + defaultMsg);
            }

        };
        m_controlPoint.execute(pause);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void stop() {
        Log.v(TAG, "Send Stop");
        mCurrentDMRState = DMR_STATE_STATUS.STOP_INITATIED_BY_APP;

        Stop stop = new Stop(m_avtransportService) {
            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
                m_isRunning = false;
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
                fireOnFailEvent("" + invocation.getAction(), response,
                        Stop.class.getSimpleName() + ":" + defaultMsg);
            }
        };

        m_controlPoint.execute(stop);
    }

    @Override
    public void addListener(DMRProcessorListener listener) {
        boolean isListnerAdded=false;
        synchronized (m_listeners) {
                m_listeners.add(listener);
           }
    }

    @Override
    public void removeListener(DMRProcessorListener listener) {
        synchronized (m_listeners) {


            m_listeners.remove(listener);
        }
    }

    @SuppressWarnings("rawtypes")
    private void fireOnFailEvent(String action, UpnpResponse response, String message) {
        Log.d("Praveen", "Seek action just failed now firing for all the subscribers");


        synchronized (m_listeners) {
            for (DMRProcessorListener listener : m_listeners) {
                Log.d("Praveen", "Sending the listner callback to " +listener);
                listener.onActionFail("" + action, response, message);

            }
        }

    }

    private void fireUpdatePositionEvent(PositionInfo positionInfo) {
        if (m_canUpdatePosition) {
            synchronized (m_listeners) {
                for (DMRProcessorListener listener : m_listeners) {
                    listener.onUpdatePosition(positionInfo.getTrackElapsedSeconds(),
                            positionInfo.getTrackDurationSeconds());
                }
            }
        }
    }


    private void fireUpdateVolumeEvent(int currentVolume) {
        // TODO Auto-generated method stub

        synchronized (m_listeners) {
            for (DMRProcessorListener listener : m_listeners) {
                listener.onUpdateVolume(currentVolume);
            }
        }

    }

    private void fireOnStopedEvent() {
        synchronized (m_listeners) {
            for (DMRProcessorListener listener : m_listeners) {
                listener.onStoped();
            }
        }


        if (iscompleted) {

            //   fireOnPlayCompletedEvent();
//            isPlaybackStarted = false;
        }
//		if (m_isAlmostCompleted) {
//			m_isAlmostCompleted = false;
//			fireOnPlayCompletedEvent();
//		}
    }

    private void fireOnPausedEvent() {
        synchronized (m_listeners) {
            for (DMRProcessorListener listener : m_listeners) {
                listener.onPaused();
            }
        }
    }

    private void fireOnPlayingEvent() {
        synchronized (m_listeners) {
            for (DMRProcessorListener listener : m_listeners) {
                listener.onPlaying();
            }
        }
    }

    private void fireOnPlayCompletedEvent() {
        synchronized (m_listeners) {
            for (DMRProcessorListener listener : m_listeners) {
                listener.onPlayCompleted();
            }
        }
    }

    private void fireOnSetURIEvent() {
        synchronized (m_listeners) {
            for (DMRProcessorListener listener : m_listeners) {
                listener.onSetURI();
            }
        }
    }

    @Override
    public void dispose() {
        m_isRunning = false;
        //stopMediaServer();
    }

    @Override
    public void seek(String position) {
        Log.d(TAG, "Call seek:" + position);
        m_canUpdatePosition = false;
        @SuppressWarnings("rawtypes")
        Seek seek = new Seek(m_avtransportService, SeekMode.REL_TIME, position) {
            @Override
            public void success(ActionInvocation invocation) {
                super.success(invocation);
                Log.d(TAG, "Seek success");
                m_canUpdatePosition = true; // changed for checking
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse reponse, String defaultMsg) {

                Log.d(TAG, "Seek Failed Praveena "+defaultMsg);
                fireOnFailEvent(Constants.SEEK_FAILED + invocation.getAction(), reponse,
                        Seek.class.getSimpleName() + ":" + defaultMsg);

                /*added for pause state*/
                m_canUpdatePosition = true;
//                synchronized (m_listeners) {
//                    for (DMRProcessorListener listener : m_listeners) {
//                        listener.onActionFail("", reponse, defaultMsg);
//
//                    }
//                }
            }
        };
        m_controlPoint.execute(seek);
    }

    @Override
    public void seek(long position) {
        // TODO Auto-generated method stub
        seek(ModelUtil.toTimeString(position));
    }

    @Override
    public void setVolume(int newVolume) {
        Log.d(TAG, "Call setVolume");

        /*Have to check why this variable (m_canUpdateVolume) has been taken*/
        m_canUpdateVolume = false;

        m_controlPoint.execute(new SetVolume(m_renderingControl, newVolume) {

            @SuppressWarnings("rawtypes")
            @Override
            public void success(ActionInvocation invocation) {
                // TODO Auto-generated method stub
                super.success(invocation);
                Log.d(TAG, "Seek success");
                /*Have to check why this variable (m_canUpdateVolume) has been taken*/
                m_canUpdateVolume = true;

                m_checkGetVolumeInfo = false;
            }

            @SuppressWarnings("rawtypes")
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                Log.d(TAG, "setVolume fail: " + defaultMsg);
                /*fireOnFailEvent(invocation.getAction(), operation, defaultMsg);*/
            }
        });
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub
        m_isRunning = false;
    }

    @Override
    public int getMaxVolume() {
        // TODO Auto-generated method stub
        return REMOTE_DEV_MAX_VOLUME;
    }

    @Override
    public int getVolume() {
        // TODO Auto-generated method stub
        // simply return 0, value will be updated by socket
        return m_currentVolume;
    }
}
