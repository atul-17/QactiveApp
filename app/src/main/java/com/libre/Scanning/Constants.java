package com.libre.Scanning;

import org.jetbrains.annotations.Nullable;

/**
 * Created by karunakaran on 8/2/2015.
 */
public class Constants {
    public static final String SL_NOTIFY = "NOTIFY * HTTP/1.1";
    public static final String SL_MSEARCH = "M-SEARCH * HTTP/1.1";
    public static final String SL_OK = "HTTP/1.1 200 OK";
    public static final String DEFAULT_ZONEID = "239.255.255.251:3000";
    public static final int GETTING_SCAN_RESULTS = 0x6790;
    public static final int HTTP_POST_DONE = 0x01;
    public static final int CONNECTED_TO_MAIN_SSID = 0x02;
    public static final int LSSDP_NEW_NODE_FOUND = 0x03;
    public static final int CONNECTED_SAC_DEVICE = 0x04;

    public static final int HTTP_POST_DONE_SUCCESSFULLY = 12121;
    public static final int HTTP_POST_FAILED = 12133;
    public static final String CURRENT_DEVICE_IP = "ip_address";
    public static final String CURRENT_SOURCE = "current_source";
    public static final int ALEXA_CHECK_TIMEOUT = 12169;
    public static final int ALEXA_NEXT_PREV_HANDLER = 121212;
    public static final int ALEXA_NEXT_PREV_TIMEOUT = 3000;
    public static final String DENIED_PERMISSIONS = "DENIED_PERMISSIONS";
    public static final String RECORD_PERMISSION_DENIED = "RECORD_PERMISSION_DENIED";
    public static final String PREV_SCREEN = "prevScreen";
    public static final String DEVICE_UDN = "device_udn";
    public static final long CHECK_ALIVE_TIMEOUT = 10000;
    public static int CONNECTED_TO_SAC_DEVICE = 12122;
    public static int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 0x1001;

    public static final int CONNECTED_TO_MAIN_SSID_FAIL = 0x1234;
    public static final int CONNECTED_TO_MAIN_SSID_SUCCESS = 0x1333;

    public static final int INITIATED_TO_FETCH_GCASTVERSION = 0x1555;
    public static final int FAIL_TO_FETCH_GCASTVERSION=0x1556;
    public static final int SUCCESS_FOR_FETCH_GCASTVERSION = 0x1557;
    /* Note: Karuna :  WAC SSID Change For Each and Every Customer  */
    public static final String WAC_SSID = "Configure_";
    public static final String RIVAA_WAC_SSID = "RIVAA";

    public static final String WAC_SSID_RIVAA_STADIUM = "RIVAASTADIUM";
    public static final String WAC_SSID_RIVAA_CONCERT = "RIVAACONCERT";

    public static final int NO_SOURCE = 0;

    /* Note: Karuna :  DDMS  SSID Change For Each and Every Customer  */
    public static final String DDMS_SSID = "DIRECT-LB";

    public static final int RETRY_COUNT_FOR_DEVICE_NAME = 10;

    /*Error case constants */
    public static final int PREPARATION_INITIATED = 0x1334;
    public static final int PREPARATION_COMPLETED = 0x1335;
    public static final int PREPARATION_TIMEOUT_CONST = 0x1336;

    public static final int SEARCHING_FOR_DEVICE = 0x1337;
    public static final int CONFIGURED_DEVICE_FOUND = 0x1338;
    public static final int CONNECTED_TO_DIFFERENT_SSID = 0x1339;
    public static final int TIMEOUT_FOR_SEARCHING_DEVICE = 0x1340;
    public static final int CHECK_ALIVE = 0x6789;

    /* 10 Minutes Timeout For Downloading and Upgrading the Firmware Update */
    public static final int DOWNLOADING_UPDATE_TIMEOUT = 10*60*1000;
    public static final int DOWNLOADING_UPDATE_MESSAGE_TIMEOUT = 0x1341;
    public static final int PREPARATION_TIMEOUT = 30000;

    public static final int LOADING_TIMEOUT = 5000;

    public static final int ITEM_CLICKED_TIMEOUT = 10000;
    public static final long REFRESH_DEVICE_TIMEOUT = ITEM_CLICKED_TIMEOUT;
    public static final int INTERNET_PLAY_TIMEOUT = 30000;

    public static final String INTERNET_ITEM_SELECTED_TIMEOUT_MESSAGE = "Device response timed out. Please try again";

    public static final String PLAY_PAUSE_NOT_ALLOWED = "Play/Pause not available";
    public static final String NEXT_PREVIOUS_NOT_ALLOWED = "Next/Prev not available";

    public static final String SPEAKER_IS_CASTING = "Speaker is Casting";

    /*Fav constants*/
    public static final String FAV_SUCCESSFUL = "Song added to favourites";
    public static final String FAV_FAILED = "Adding song to favourites failed";
    public static final String FAV_EXISTS = "Song already added to favourites";
    public static final String FAV_DELETED = "Song deleted from favourites";
    public static final String VTUNER_FAV_DELETE = "please go to vTuner -> Favourites to delete the station from Favourite list";


    // Toast string when dropall in PlayNewActivity
    public static final String NO_DEVICES_TO_DROP = "No devices to free";
    //while we intent from NowPlayingFragment to SourceOptionActivity
    public static final String FROM_ACTIVITY = "fromActivity";


    /*Handling error case here*/
    public static final String SEEK_FAILED = "SeekFailed";


    /*show device volume pressed case*/
    public static final String VOLUME_PRESSED_MESSAGE = "Please go to NowPlaying screen to update volume";

    public static final String REINITIALISING = "Updating the Device Details .. ";
    public static final String DEVICE_REMOVED = "Device got removed,please refresh";
    public static final String DEVICE_ADDED = "New device has come,please refresh";
    public static final String DEVICE_STATE_FAILED = "Something went wrong please reboot device";
    public static final String PREPARATION_TIMEOUT_HAPPENED = "Timeout happened";


    public static final String LOADING_TIMEOUT_REBOOT = "Device is not responding, Please reboot";

    public static final String NO_INTERNET_CONNECTION = "No Internet Connection";

    public static final String RENDERER_NOT_PRESENT = "Renderer not found,please try selecting source again or reboot device";
    public static final String AVTRANSPORT_NOT_FOUND = "AVTransport service not found,please refresh or reboot device";

    public static final String SUCCESS = "success";// This will come for DMR too
    public static final String FAIL = "error_playfail";// This will come for DMR too
    public static final String NO_URL = "error_nourl";
    public static final String NO_NEXT_SONG = "error_nonextsong";
    public static final String NO_PREV_SONG = "error_noprevsong";
    public static final String DMR_PLAYBACK_COMPLETED = "dmrplayback_complete";
    public static final String DMR_SONG_UNSUPPORTED = "error_unsupportedformat";

    public static final String SUCCESS_ALERT_TEXT = "Play Success";// This will come for DMR too
    public static final String FAIL_ALERT_TEXT = "Playback Failed";// This will come for DMR too
    public static final String NO_URL_ALERT_TEXT = "No URL";
    public static final String NO_NEXT_SONG_ALERT_TEXT = "End of playlist";
    public static final String NO_PREV_SONG_ALERT_TEXT = "No previous song";
    public static final String SONG_NOT_SUPPORTED = "Unsupported format";

    public static final String SCENE_NAME_CANT_CHANGE= "Group Name Change Not Allowed,When Slaves Are Inactive";
    public static final String GCAST_FIRMWARE_UPGRADE = "UPDATE_AVAILABLE_REBOOT_REQUEST";
    public static int ALEXA_NEXT_PREV_INIT = 15122;
    public static final String DIDL_TITLE = "didlTitle";

    public interface ACTION {
        String MAIN_ACTION = "com.libre.Scanning.action.main";
        String PREV_ACTION = "com.libre.Scanning.action.prev";
        String PLAY_ACTION = "com.libre.Scanning.action.play";
        String NEXT_ACTION = "com.libre.Scanning.action.next";
        String STARTFOREGROUND_ACTION = "com.libre.Scanning.action.startforeground";
        String STOPFOREGROUND_ACTION = "com.libre.Scanning.action.stopforeground";
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }


    /*Handling Google TOS*/
    public static final String SHOWN_GOOGLE_TOS = "shownGoogleTos";
    public static final String PROPRITARY = "propritary";
    public static final String SEARCH_RESULT_HASH_CODE = "searchHashCode";
    public static final String SEARCH_RESULT_HASH_CODE_VALUE = "searchHashCodeValue";
    public static final String DEZER_RADIO=":;:radio";
	    public static final String  DEZER_SONGSKIP="skipdisabled:;:";


    /*Sources */
    public static final int AIRPLAY_SOURCE = 1;
    public static final int DMR_SOURCE = 2;
    public static final int DMP_SOURCE = 3;
    public static final int NETWORK_DEVICES = DMP_SOURCE;
    public static final int SPOTIFY_SOURCE = 4;
    public static final int USB_SOURCE = 5;
    public static final int SDCARD_SOURCE = 6;
    public static final int MELON_SOURCE = 7;
    public static final int VTUNER_SOURCE = 8;
    public static final int TUNEIN_SOURCE = 9;
    public static final int MIRACAST_SOURCE = 10;
    public static final int DDMSSLAVE_SOURCE = 12;
    public static final int AUX_SOURCE = 14;
    public static final int APPLEDEVICE_SOURCE = 16;
    public static final int DIRECTURL_SOURCE = 17;
    public static final int BT_SOURCE = 19;
    public static final int DEEZER_SOURCE = 21;
    public static final int TIDAL_SOURCE = 22;
    public static final int FAVOURITES_SOURCE = 23;
    public static final int GCAST_SOURCE = 24;
    public static final int EXTERNAL_SOURCE = 25;
    public static final int ALEXA_SOURCE = 28;
    public static final int QPLAY_SOURCE = 18;

    public static final String ERROR_FAIL = FAIL_ALERT_TEXT;
    public static final String ERROR_NOURL = NO_URL_ALERT_TEXT;
    public static final String ERROR_LASTSONG = NO_NEXT_SONG_ALERT_TEXT;

    public static final String GCAST_UPDATE_IMAGE_AVAILABLE = "UPDATE_IMAGE_AVAILABLE";
    public static final String GCAST_NO_UPDATE = "NO_UPDATE";
    public static final String GCAST_UPDATE_STARTED = "UPDATE_STARTED";

    public static final String GCAST_COMPLETE = "complete";
    public static final String GCAST_START = "start";
    public static final String LS9 = "LS9";
    public static final String LS6 = "LS6";
    public static final String LS5B = "LS5B";
    public static final String START_MIC = "START_MIC:";

    public static final String CURRENT_LOCALE = "current_locale";
    public static final String CURRENT_SOURCE_INDEX_SELECTED = "currentSourceIndexSelected";

    public interface MediaEnum {
        int MEDIA_PROCESS_INIT = 1000;
        int MEDIA_PROCESS_DONE = 1001;
        int MEDIA_LOADING_FAIL = 1002;
    }

    public static final int NETWORK_TIMEOUT = 0x1;
    public static final int SERVICE_NOT_FOUND = 0x2;
    public static final int DO_BACKGROUND_DMR = 0X3;
    public static final String TAG_CMD_ID = "CMD ID";
    public static final String TAG_WINDOW_CONTENT = "Window CONTENTS";

    public static final String MUSIC_TYPE = "musicType";
    public static final String CLICKED_DIDL_ID = "clickedDIDLId";
    public static final String USB_DEFAULT_FILE_PATH_PREFIX = "/media/usb/";
    public static final String REPEAT = "_repeat";
    public static final String SHUFFLE = "_shuffle";
}