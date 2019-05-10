package com.libre.Network;

/**
 * Created by karunakaran on 7/30/2015.
 */

import android.os.AsyncTask;

import com.libre.util.LibreLogger;

import org.eclipse.jetty.http.HttpStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

//import org.apache.http.HttpResponse;
//import org.apache.http.client.HttpClient;
//import org.apache.http.impl.client.DefaultHttpClient;

public class AsyncHTTPTask extends AsyncTask<String, Void, String> {

    private HttpHandler httpHandler;

    public AsyncHTTPTask(HttpHandler httpHandler) {

        this.httpHandler = httpHandler;
    }

    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }


    @Override
    protected String doInBackground(String... arg0) {
        InputStream inputStream = null;
        String result = "";
        try {

            HttpURLConnection urlConnection = (HttpURLConnection) httpHandler.getHttpURLConnectionMethod();
            int responseCode = urlConnection.getResponseCode();

            if (responseCode == HttpStatus.OK_200) {
                result = readStream(urlConnection.getInputStream());
                httpHandler.onResponse(result);
                LibreLogger.d(this, "AsyncHTTPTask" + "REsponse Code is  200:: " + result);

            } else {
                /*handling error case*/
                httpHandler.onError(responseCode);
                LibreLogger.d(this, "AsyncHTTPTask" + "REsponse Code is Not 200:: " + responseCode);
            }

        } catch (Exception e) {
            httpHandler.onError(-1);
            e.printStackTrace();
        } finally {
            if (httpHandler.getHttpURLConnectionMethod() != null)
                httpHandler.getHttpURLConnectionMethod().disconnect();
        }
       /* try {

            // create HttpClient
              HttpClient httpclient = new DefaultHttpClient();

            // make the http request
            HttpResponse httpResponse = httpclient.execute(httpHandler.getHttpRequestMethod());

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
            result = "";
        }*/

        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        //  httpHandler.onResponse(result);
        httpHandler.getHttpURLConnectionMethod().disconnect();
    }

    //--------------------------------------------------------------------------------------------
    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while ((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

}
