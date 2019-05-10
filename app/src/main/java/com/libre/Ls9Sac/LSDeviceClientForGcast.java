package com.libre.Ls9Sac;

import android.util.Log;

import com.libre.Scanning.Constants;
import com.libre.util.LibreLogger;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.http.GET;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/**
 * Created by khajan on 6/4/16.
 */
public class LSDeviceClientForGcast {
    GcastEurekaService gcastEurekaService;
    ExecutorService backgroundExecutor = Executors.newCachedThreadPool();


    public interface GcastEurekaService {

        @GET("/setup/eureka_info")
        void getTOSData(Callback<Object> callback);
    }


   public LSDeviceClientForGcast(String url) {

        Log.d("HellUrlIs", "LSDeviceClient() called with: " + "url = [" + url + "]");

       RestAdapter.Builder builder = new RestAdapter.Builder();
       builder.setEndpoint(url);
       builder.setLogLevel(RestAdapter.LogLevel.FULL);
       builder.setClient(new OkClient(getClient()));
       builder.setConverter(new CustomConverter());
       RestAdapter restAdapter = builder.build();

       gcastEurekaService = restAdapter.create(GcastEurekaService.class);
    }


    private OkHttpClient getClient() {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(1, TimeUnit.MINUTES);
        client.setReadTimeout(1, TimeUnit.MINUTES);
        client.setWriteTimeout(1, TimeUnit.MINUTES);

        client.interceptors().add(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                Response response = null;
                boolean responseOK = false;
                int tryCount = 0;

                while (!responseOK && tryCount < Constants.RETRY_COUNT_FOR_DEVICE_NAME) {
                    try {
                        response = chain.proceed(request);
                        responseOK = response.isSuccessful();
                    }catch (Exception e){
                        LibreLogger.d(this, "Request is not successful - " + tryCount);
                    }finally{
                        tryCount++;
                    }
                }
                // otherwise just pass the original response on
                return response;
            }
        });

        return client;
    }

    public GcastEurekaService getGcastEurekaService() {
        return gcastEurekaService;
    }


    class CustomConverter implements Converter {


        @Override
        public Object fromBody(TypedInput body, Type type) throws ConversionException {
            String text = null;
            try {
                text = fromStream(body.in());
            } catch (IOException ignored) {/*NOP*/ }


            return text;
        }

        @Override
        public TypedOutput toBody(Object object) {
            return null;
        }


        // Custom method to convert stream from request to string
        public String fromStream(InputStream in) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder out = new StringBuilder();
            String newLine = System.getProperty("line.separator");
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
                //out.append(newLine);
            }
            return out.toString();
        }
    }

}
