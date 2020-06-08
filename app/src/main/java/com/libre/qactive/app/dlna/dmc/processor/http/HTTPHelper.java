package com.libre.qactive.app.dlna.dmc.processor.http;

import android.util.Log;

import org.fourthline.cling.support.messagebox.model.DateTime;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.sql.Date;
import java.util.List;

public class HTTPHelper {

	private static final int BUFFERSIZE = 32768;
	private static final String TAG = HTTPHelper.class.getName();
	public static String NEWLINE = "\r\n";

	public static String makeGETrequest(String url) throws MalformedURLException {
		String result = "";
		URL requestURL = new URL(url);
		result += "GET " + requestURL.getFile() + " HTTP/1.1";
		result += NEWLINE;
		result += "Host: " + requestURL.getHost() + ":" + (requestURL.getPort() > 0 ? requestURL.getPort() : 80);
		result += NEWLINE;
		result += "Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2";
		result += NEWLINE;
		result += "Connection: close";
		result += NEWLINE;
		result += NEWLINE;

		return result;
	}

	public static String makeHttp200Reponse(String filename) {
		String result = "";

		File f = new File(filename);

		result += "HTTP/1.1 200 OK";
		result += NEWLINE;
		result += "Content-Type: audio/x-ms-wma";
		result += NEWLINE;
		result += "Content-Length: " + f.length();
		result += NEWLINE;
		result += "Accept-Ranges: bytes";
		result += NEWLINE;
		result += "Date: " + DateTime.getCurrentDate().toString();
		result += NEWLINE;
		result += "Last-Modified: " + new Date(f.lastModified()).toString();
		result += NEWLINE;
		result += "EXT:";
		result += NEWLINE;
		result += "Connection: close";
		result += NEWLINE;
		result += "Server: SimpleDMS";
		result += NEWLINE;
		result += NEWLINE;

		return result;
	}

	public static String makeHttp206Reponse(String filename, long range) {
		String result = "";
		File f = new File(filename);

		result += "HTTP/1.1 206 Partial Content";
		result += NEWLINE;
		result += "Content-Type: audio/mpeg";
		result += NEWLINE;
		result += "Content-Length: " + (f.length() - range);
		result += NEWLINE;
		result += "Content-Range: bytes " + (range + "-" + (f.length() - 1 + "-" + f.length()));
		result += NEWLINE;
		result += "Accept-Ranges: bytes";
		result += NEWLINE;
		result += "Date: " + DateTime.getCurrentDate().toString();
		result += NEWLINE;
		result += "Last-Modified: " + new Date(f.lastModified()).toString();
		result += NEWLINE;
		result += "EXT:";
		result += NEWLINE;
		result += "Connection: close";
		result += NEWLINE;
		result += "Server: SimpleDMS";
		result += NEWLINE;
		result += NEWLINE;

		return result;
	}

	public static void handleClientRequest(final Socket client, String requesttype, long range, String filename) {
		DataInputStream dis = null;
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
			if (range == 0) {
				bw.write(HTTPHelper.makeHttp200Reponse(filename));
				bw.flush();
			} else {
				bw.write(HTTPHelper.makeHttp206Reponse(filename, range));
				bw.flush();
			}

			if (requesttype.compareTo("GET") == 0) {

				dis = new DataInputStream(new FileInputStream(filename));
				System.out.println("Skip " + range + " bytes");
				long skippedbytes = dis.skip(range);
				System.out.println(skippedbytes + " bytes is skipped");
				DataOutputStream dos = new DataOutputStream(client.getOutputStream());

				byte[] b = new byte[BUFFERSIZE];
				long count = range;
				int bytesread = 0;
				while (HTTPServerData.RUNNING) {
					bytesread = dis.read(b);
					count += bytesread;
					dos.write(b, 0, bytesread);
					if (count == new File(filename).length())
						break;
				}
				dos.flush();
				System.out.println(count);
			}
		} catch (IOException e) {
			System.out.println("Client close connection");
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (Throwable t) {}
			}
		}
	}

	public static void handleProxyDataRequest(Socket client, List<String> rawrequest, String directLink) {
		try {
			URL youtube = new URL(directLink);
			Socket socket = new Socket(youtube.getHost(), 80);
			PrintStream ps = new PrintStream(socket.getOutputStream());

			for (String requestLine : rawrequest) {
				Log.d(TAG, "Original request = " + requestLine);
				String[] token = requestLine.split(" ");
				if (requestLine.contains("HEAD") || requestLine.contains("GET")) {
					ps.println(token[0] + " " + youtube.getFile() + " HTTP/1.1");
				} else if (requestLine.contains("Host")) {
					ps.println("Host: " + youtube.getAuthority());
				} else if (requestLine.contains("Connection:")) {
					ps.println("Connection: Close");
				} else {
					ps.println(requestLine);
				}
			}
			ps.println();
			ps.flush();

			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(client.getOutputStream());

			byte[] buffer = new byte[65536];
			int read = -1;
			while ((read = dis.read(buffer)) > 0 && HTTPServerData.RUNNING) {
				dos.write(buffer, 0, read);
			}
			dis.close();
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
