package com.libre.Network;

import java.net.HttpURLConnection;

/**
 * Created by karunakaran on 7/30/2015.
 */
public abstract class HttpHandler {

    public abstract void onError(int responseCode);


    public abstract HttpURLConnection getHttpURLConnectionMethod();
    // public abstract HttpUriRequest getHttpRequestMethod();

    public abstract void onResponse(String result);

    public abstract void closeHTTPURLConnection();

    public void execute() {
        new AsyncHTTPTask(this).execute();
    }


}