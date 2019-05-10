package com.libre.Network;

import android.content.Context;
import android.os.AsyncTask;

import com.libre.Network.NetworkListeners.WifiScanlistListener;
import com.libre.util.LibreLogger;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by suma on 30/11/17.
 */

public class GetScanResults extends AsyncTask<String, Void, String> {


    private Context context;
    WifiScanlistListener listener;

    public GetScanResults(Context context , WifiScanlistListener listener ){
        this.context = context;

        this.listener = listener;
    }
    @Override
    protected String doInBackground(String... strings) {

        LibreLogger.d(this,"firing  scan result api");
        final String BASE_URL = "http://192.168.43.1/scanresult.asp";

        String inputLine;
        URL myUrl = null;
        try {
            myUrl = new URL(BASE_URL);

            HttpURLConnection connection =(HttpURLConnection)
                    myUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(50000);
            connection.setConnectTimeout(50000);

            //Connect to our url
            connection.connect();
            //Create a new InputStreamReader
            InputStreamReader streamReader = new
                    InputStreamReader(connection.getInputStream());
            //Create a new buffered reader and String Builder
            BufferedReader reader = new BufferedReader(streamReader);
            StringBuilder stringBuilder = new StringBuilder();
            //Check if the line we are reading is not null
            while((inputLine = reader.readLine()) != null){
                stringBuilder.append(inputLine);
            }
            //Close our InputStream and Buffered reader
            reader.close();
            streamReader.close();
            //Set our result equal to our stringBuilder

            LibreLogger.d(this,"got scan results "+stringBuilder.toString());
            listener.success(new JSONObject(stringBuilder.toString()));
        } catch (Exception e) {
            e.printStackTrace();
            LibreLogger.d(this,"getting scan result failed: "+e.toString());
            listener.failure(e);
        }
        return null;
    }


    @Override
    protected void onPostExecute(String result) {
        //  httpHandler.onResponse(result);

    }
}
