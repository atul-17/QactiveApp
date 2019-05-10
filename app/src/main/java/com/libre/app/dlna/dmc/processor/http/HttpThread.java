package com.libre.app.dlna.dmc.processor.http;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class HttpThread extends Thread {
	private static final String TAG = HttpThread.class.getSimpleName();
	private ServerSocket m_server;

	public HttpThread() {
		HTTPServerData.RUNNING = true;
	}

	public void stopHttpThread() {
		Log.d(TAG, "Stop HTTP Thread");
		HTTPServerData.RUNNING = false;
		try {
			m_server.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		Log.d(TAG, "Start HTTP Thread");
		try {
			m_server = new ServerSocket(0);
			HTTPServerData.PORT = m_server.getLocalPort();
			Log.d(TAG, "Host = " + HTTPServerData.HOST + " PORT = " + String.valueOf(HTTPServerData.PORT));
			while (HTTPServerData.RUNNING) {
				final Socket client = m_server.accept();
				Log.d(TAG, "Client Connected");
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							final BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));
							List<String> rawrequest = new ArrayList<String>();
							String line = null;
							String request = null;
							String requesttype = null;
							long range = 0;
							Log.d(TAG, "<--START HEADER-->");
							while ((line = br.readLine()) != null && (line.length() != 0)) {
								Log.i(TAG, line);
								rawrequest.add(line);
								if (line.contains("GET")) {
									requesttype = "GET";
									request = getRequestFilePath(line);
								} else if (line.contains("HEAD")) {
									requesttype = "HEAD";
									request = getRequestFilePath(line);
								} else if (line.contains("Range")) {
									String strrange = line.substring(13, line.lastIndexOf('-'));
									range = Long.valueOf(strrange);
								}
							}
							Log.d(TAG, "<--END HEADER-->");
							String filename = null;
							if (request != null) {
								Log.d(TAG, "Request = " + request);
								if (HTTPLinkManager.LINK_MAP.containsKey(request)) {
									Log.d(TAG, "Youtube Proxy mode");
									HTTPHelper.handleProxyDataRequest(client, rawrequest, HTTPLinkManager.LINK_MAP.get(request));
								} else {
									Log.d(TAG, "Play-to mode");
									filename = URLDecoder.decode(request, "ASCII");
									if (filename != null && filename.length() != 0) {
										HTTPHelper.handleClientRequest(client, requesttype, range, filename);
									}
								}

							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}

					private String getRequestFilePath(String line) {
						String request;
						String[] linepatthern = line.split(" ");
						request = linepatthern[1];
						return request;
					}

				}).start();
			}
			m_server.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
