package com.libre.alexa.userpoolManager.AlexaUtils;

import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by bhargav on 20/6/17.
 */
public class JWTUtils {
    public static String getUsername(String JWTEncoded) throws Exception {
        String username = null;
        try {
            String[] split = JWTEncoded.split("\\.");
            Log.d("JWT_DECODED", "Header: " + getJson(split[0]));
            Log.d("JWT_DECODED", "Body: " + getJson(split[1]));
            JSONObject jsonObject = new JSONObject(getJson(split[1]));
            username = jsonObject.getString("cognito:username");
            Log.d("JWT_DECODED", "username is : " + username);
        } catch (UnsupportedEncodingException e) {
            //Error
        }
        return username;
    }

    private static String getJson(String strEncoded) throws UnsupportedEncodingException{
        byte[] decodedBytes = Base64.decode(strEncoded, Base64.URL_SAFE);
        return new String(decodedBytes, "UTF-8");
    }
}
