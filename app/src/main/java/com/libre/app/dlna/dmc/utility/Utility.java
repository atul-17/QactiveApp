package com.libre.app.dlna.dmc.utility;

import android.util.Base64;
import android.util.Log;


import com.libre.app.dlna.dmc.processor.http.HTTPServerData;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;

public class Utility {
	private static final String TAG = Utility.class.getName();

	public static String intToIp(int i) {
		String result = "";
		result = (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
		return result;
	}

	public static String getMD5(String input) {
		try {
			byte[] bytesOfMessage = input.getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] disgest = md.digest(bytesOfMessage);
			return Base64.encodeToString(disgest, Base64.DEFAULT);
		} catch (Exception ex) {
			Log.e(TAG, "Create hash fail. input = " + input);
			return null;
		}
	}

	public static String createLink(File file) {
		try {
			return new URI("http", HTTPServerData.HOST + ":" + HTTPServerData.PORT, file.getAbsolutePath(), null, null).toString();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
}