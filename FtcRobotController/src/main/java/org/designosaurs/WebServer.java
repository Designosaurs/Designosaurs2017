package org.designosaurs;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

public class WebServer extends NanoWSD {
	public static String page;

    /* Configuration */
    private static final int port = 9001;
    public static boolean enabled = true;
    public static boolean debug = true;
	private String TAG = "WebServer";

	private SparseArray<WebsocketServer> clients = new SparseArray<>(0);

    public WebServer() throws IOException {
        super(port);

        if(!enabled)
            return;

        start(0, true);
        Log.i(TAG, "Web server running on port " + port);
    }

	@Override
	protected NanoWSD.WebSocket openWebSocket(IHTTPSession handshake) {
		WebsocketServer server = new WebsocketServer(this, handshake);
		clients.append(server.hashCode(), server);

		return server;
	}

	public void send(String event, Object data) {
		try {
			JSONObject message = new JSONObject();
			message.put("event", event);
			message.put("data", data);

			for(int i = 0, nsize = clients.size(); i < nsize; i++) {
				WebsocketServer server = clients.valueAt(i);

				if(server.isOpen())
					server.send(message.toString());
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void streamCameraFrame(Bitmap bmp) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 40, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

		send("camera.frame", Base64.encodeToString(byteArray, Base64.DEFAULT));
    }

	public void streamPoints(ArrayList<String> coords) {
		send("camera.points", new JSONArray(coords));
	}

    @Override
    public Response serveHttp(IHTTPSession session) {
		return newFixedLengthResponse(page);
    }
}