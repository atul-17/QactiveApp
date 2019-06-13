package com.libre.alexa;

import com.libre.constants.LSSDPCONST;
import com.libre.constants.LUCIMESSAGES;
import com.libre.constants.MIDCONST;
import com.libre.luci.LUCIControl;

/**
 * Created by bhargav on 25/10/17.
 */

public class AlexaUtils {

    public static void sendAlexaMetaDataRequest(String ipAddress){
        new LUCIControl(ipAddress).SendCommand(MIDCONST.ALEXA_COMMAND, LUCIMESSAGES.DEVICE_METADATA_REQUEST, LSSDPCONST.LUCI_SET);
    }

    public static void sendAlexaRefreshTokenRequest(String ipAddress){
        new LUCIControl(ipAddress).SendCommand(MIDCONST.MID_ENV_READ, LUCIMESSAGES.READ_ALEXA_REFRESH_TOKEN_MSG, LSSDPCONST.LUCI_GET);    }
}
