package com.libre.qactive.firmwareupgarde;

import android.os.Handler;

import com.libre.qactive.LibreApplication;
import com.libre.qactive.Ls9Sac.EurekaJSON;
import com.libre.qactive.Scanning.Constants;
import com.libre.qactive.constants.LSSDPCONST;
import com.libre.qactive.constants.MIDCONST;
import com.libre.qactive.luci.LSSDPNodeDB;
import com.libre.qactive.luci.LUCIControl;
import com.libre.qactive.util.GoogleTOSTimeZone;
import com.libre.qactive.util.LibreLogger;
import com.squareup.okhttp.OkHttpClient;

import java.io.File;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.mime.TypedFile;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;


/**
* Created by praveena on 12/2/15.
*/

public  class URLclient {

private final APIService service;
final int MEDIA_PROCESS_INIT = 415;
private final int MEDIA_PROCESS_DONE = 416;
private final int MEDIA_PROCESS_FAIL = 417;
final int DEVICE_FLASH_STARTED=419;
final int DEVICE_FLASH_FAILED=420;

final  int STARTING_BSL_INITIATED=421;
final  int STARTING_BSL_FAILED=422;

    public interface APIService {

    @Multipart
    @POST("/action/uploadTest")
    void uploadFileToDevice(@Part("filename") TypedFile file, Callback<Object> callback);

    @FormUrlEncoded
    @POST("/action/firmwareupdateconfirm")
    void confirmToUpload(@Field("button") String buttonText, Callback<Object> callback);


    @FormUrlEncoded
    @POST("/goform/UpgradeDevice")
    void upgradeDevice(@Field("FWMethod") String fwMethod, @Field("configure_factoryreset") String configure_factoryreset, Callback<Object> callback);


    @Multipart
    @POST("/purposify/device/storyvideo")
    void uploadFile(@Part("story_video") TypedFile file, @Part("story_id") String email, @Part("user_id") String user, Callback<String> callback);


    @Multipart
    @POST("/device/profilepic")
    void uploadFileToHeroku(@Part("profile_pic") TypedFile file, @Part("user_id") String user, Callback<String> callback);

    @GET("/setup/eureka_info")
    void getEureka(Callback<EurekaJSON> callback);


}

public URLclient(String hostName) {




    final String BASE_URL = "http://"+hostName;

    RestAdapter restAdapter = new RestAdapter.Builder()
            .setEndpoint(BASE_URL)
            .setConverter(new CustomConverter())
            .setClient(new OkClient(getClient()))
            .build();


    service = restAdapter.create(APIService.class);
}

public URLclient(String hostName,String n) {




    final String BASE_URL = "http://"+"192.168.0.103/purposify/device/storyvideo";

    RestAdapter restAdapter = new RestAdapter.Builder()
            .setLogLevel(RestAdapter.LogLevel.FULL)
            .setEndpoint(BASE_URL)
            .setClient(new OkClient(getClient()))
            .build();


    service = restAdapter.create(APIService.class);
}
    private String mHostName;
    public URLclient(String hostName,boolean isLs9) {
        this.mHostName = hostName;

        final String BASE_URL = "http://" + hostName+":8008";

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(BASE_URL)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setClient(new OkClient(getClient()))
                .build();


        service = restAdapter.create(APIService.class);
    }

    public void getBuildVersionFromEureka(String mIpaddress, final Handler mHandler){
        final LUCIControl luciControl = new LUCIControl(mHostName);
        service.getEureka(new Callback<EurekaJSON>() {
            @Override
            public void success(EurekaJSON eurekaJson, Response response) {
                try {

                    boolean mTosAccepted= false ;
                    boolean mShareAccepted = false;;
                    if(eurekaJson==null
                            || eurekaJson.getTosAccepted() ==null
                            || eurekaJson.getOptIn().getStats()==null
                            ){
                        LibreLogger.d(this, "Returned Null in URL Client for EUReka");
                        mHandler.sendEmptyMessage(Constants.FAIL_TO_FETCH_GCASTVERSION);
                        return;
                    }

                    if(eurekaJson.getCastBuildRevision()!=null){
                        LSSDPNodeDB.getInstance()
                                .getTheNodeBasedOnTheIpAddress(mHostName)
                                .setgCastVersionFromEureka(eurekaJson.getCastBuildRevision());
                        mHandler.sendEmptyMessage(Constants.SUCCESS_FOR_FETCH_GCASTVERSION);
                    }
                    mHandler.sendEmptyMessage(Constants.FAIL_TO_FETCH_GCASTVERSION);
                } catch (Exception e) {
                    e.printStackTrace();
                    mHandler.sendEmptyMessage(Constants.FAIL_TO_FETCH_GCASTVERSION);
                }
//                LibreLogger.d(this, eurekaJson.getBssid() + " " + eurekaJson.getOptIn().getStats());
            }

            @Override
            public void failure(RetrofitError error) {
                mHandler.sendEmptyMessage(Constants.FAIL_TO_FETCH_GCASTVERSION);
            }
        });
    }
    public void getTheEurekaJson(final GoogleTOSTimeZone googleTOSTimeZone){
        final LUCIControl luciControl = new LUCIControl(mHostName);
        service.getEureka(new Callback<EurekaJSON>() {
            @Override
            public void success(EurekaJSON eurekaJson, Response response) {
                try {

                    boolean mTosAccepted= false ;
                    boolean mShareAccepted = false;;
                    if(eurekaJson==null
                            || eurekaJson.getTosAccepted() ==null
                            || eurekaJson.getOptIn().getStats()==null
                            ){
                        LibreLogger.d(this,"Returned Null in URL Client");
                       // googleTOSTimeZone.setTimezone(TimeZone.getDefault().getID().toString());
                        LibreApplication.GOOGLE_TIMEZONE_MAP.put(mHostName, googleTOSTimeZone);
                        luciControl.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "1:"
                                        + googleTOSTimeZone.getTimezone(),
                                LSSDPCONST.LUCI_SET);
                        return;
                    }

                    if(eurekaJson.getBuildVersion()!=null){
                        LSSDPNodeDB.getInstance()
                                .getTheNodeBasedOnTheIpAddress(mHostName)
                                .setgCastVersionFromEureka(eurekaJson.getBuildVersion());
                    }

                    mTosAccepted = eurekaJson.getTosAccepted();
                    mShareAccepted = eurekaJson.getOptIn().getStats();
                    if(!mTosAccepted && mShareAccepted){
                        //googleTOSTimeZone.setTimezone(TimeZone.getDefault().getID().toString());
                        //LibreApplication.GOOGLE_TIMEZONE_MAP.put(mHostName, googleTOSTimeZone);
                        luciControl.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "3:"
                                        + googleTOSTimeZone.getTimezone(),
                                LSSDPCONST.LUCI_SET);
                    }else if(!mTosAccepted && !mShareAccepted){
                        //googleTOSTimeZone.setTimezone(TimeZone.getDefault().getID().toString());
                        //LibreApplication.GOOGLE_TIMEZONE_MAP.put(mHostName, googleTOSTimeZone);
                        luciControl.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "1:"
                                        + googleTOSTimeZone.getTimezone(),
                                LSSDPCONST.LUCI_SET);
                    }else if(mTosAccepted && !mShareAccepted){

                        luciControl.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "1:"
                                        + googleTOSTimeZone.getTimezone(),
                                LSSDPCONST.LUCI_SET);
                    }else if(mTosAccepted && mShareAccepted){

                        luciControl.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "3:"
                                        + googleTOSTimeZone.getTimezone(),
                                LSSDPCONST.LUCI_SET);
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
//                LibreLogger.d(this, eurekaJson.getBssid() + " " + eurekaJson.getOptIn().getStats());
            }

            @Override
            public void failure(RetrofitError error) {
                try {
                    LibreLogger.d(this, "Error" + error.getResponse());
                   // googleTOSTimeZone.setTimezone(TimeZone.getDefault().getID().toString());
                    LibreApplication.GOOGLE_TIMEZONE_MAP.put(mHostName, googleTOSTimeZone);
                    luciControl.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "1:"
                                    + googleTOSTimeZone.getTimezone(),
                            LSSDPCONST.LUCI_SET);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

public URLclient(String hostName,int i) {
    final String BASE_URL = "http://fast-wave-8947.herokuapp.com/";

    RestAdapter restAdapter = new RestAdapter.Builder()
            .setLogLevel(RestAdapter.LogLevel.FULL)
            .setEndpoint(BASE_URL)
            .setClient(new OkClient(getClient()))
            .build();


    service = restAdapter.create(APIService.class);
}


private OkHttpClient getClient() {
    OkHttpClient client = new OkHttpClient();
    client.setConnectTimeout(1, TimeUnit.MINUTES);
    client.setReadTimeout(10, TimeUnit.MINUTES);
    client.setWriteTimeout(10, TimeUnit.MINUTES);

    return client;
}



public void sendTheFileToDevice(File file, final Handler handler) {

    TypedFile typedFile = null;
    typedFile = new TypedFile("multipart/form-data", file);
    service.uploadFileToDevice(typedFile, new Callback<Object>() {
        @Override
        public void success(Object s, retrofit.client.Response response) {

            handler.sendEmptyMessage(MEDIA_PROCESS_DONE);
        }

        @Override
        public void failure(RetrofitError error) {
            error.printStackTrace();
            handler.sendEmptyMessage(MEDIA_PROCESS_FAIL);
        }
    });
}

public void restartTheDUTInBSL(final Handler handler){

    service.upgradeDevice("Network", "false", new Callback<Object>() {
        @Override
        public void success(Object o, retrofit.client.Response response) {
            handler.sendEmptyMessageDelayed(MEDIA_PROCESS_INIT,55000);
        }

        @Override
        public void failure(RetrofitError error) {
            handler.sendEmptyMessage(STARTING_BSL_FAILED);
        }
    });
}

public void startDeviceFlash(final Handler handler){

    service.confirmToUpload("      Ok      ",new Callback<Object>() {

        @Override
        public void success(Object s, retrofit.client.Response response) {

            handler.sendEmptyMessage(DEVICE_FLASH_STARTED);
        }

        @Override
        public void failure(RetrofitError error) {
            error.printStackTrace();
            handler.sendEmptyMessage(DEVICE_FLASH_FAILED);
        }
    });


}       

class CustomConverter implements Converter {


    @Override
    public Object fromBody(TypedInput body, Type type) throws ConversionException {
        return null;
    }

    @Override
    public TypedOutput toBody(Object object) {
        return null;
    }
}



}




