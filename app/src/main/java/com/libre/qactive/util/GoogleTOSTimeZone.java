package com.libre.qactive.util;

/**
 * Created by praveena on 5/5/16.
 */
public class GoogleTOSTimeZone {

    public GoogleTOSTimeZone(String s) {

        String[] splitStr = s.split(":");

        if (splitStr.length >= 2) {
            timezone = splitStr[1];
        }
        if (splitStr.length >= 1){
            tosANDShare = splitStr[0];
    }
}



    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getTosANDShare() {
        return tosANDShare;
    }

    public void setTosANDShare(String tosANDShare) {
        this.tosANDShare = tosANDShare;
    }

    String tosANDShare="";
    String timezone="";


    public boolean isTOSAccepted(){

        if (tosANDShare.equalsIgnoreCase("0")||tosANDShare.equalsIgnoreCase("2"))
            return  false;

        return true;

    }
    public boolean isShareAccepted(){

        if (tosANDShare.equalsIgnoreCase("0")||tosANDShare.equalsIgnoreCase("1"))
            return  false;

        return true;


    }


}
