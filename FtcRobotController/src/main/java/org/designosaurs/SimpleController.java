package org.designosaurs;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

public class SimpleController extends NanoHTTPD {
    private String imageData = "";
    private String imageData2 = "";
	public static ArrayList<Vector2> coords = new ArrayList<>();
	public static ArrayList<Vector2> coords2 = new ArrayList<>();
	public static String page;

    /* Configuration */
    private static final int httpPort = 9001;
    private static final int wsPort = 9002;
    public static boolean enabled = true;
    public static boolean debug = true;

    public SimpleController() throws IOException {
        super(httpPort);

        if(!enabled)
            return;

        NanoWSD ws = new WebsocketServer(wsPort, debug);
        ws.start();

        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("\nSimple Web Controller Interface started on port " + httpPort + "\n");
    }

    public void setImage(Bitmap bmp) {
        Bitmap scaled = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight());

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        scaled.compress(Bitmap.CompressFormat.JPEG, 20, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        imageData = Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

	public void setImage2(Bitmap bmp) {
		Bitmap scaled = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight());

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		scaled.compress(Bitmap.CompressFormat.JPEG, 20, byteArrayOutputStream);
		byte[] byteArray = byteArrayOutputStream.toByteArray();

		imageData2 = Base64.encodeToString(byteArray, Base64.DEFAULT);
	}

    public void setImage(Mat data) {
        try {
			Bitmap bmp = Bitmap.createBitmap(data.cols(), data.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(data, bmp);

            data.release();

            setImage(bmp);
        } catch (CvException | NullPointerException e) {
            Log.d("WebServer", e.getMessage());
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
//        String msg = "<html><head><style>#camera { position: absolute; top: 200px; left: 0 }\n.point { position: absolute; width: 8px; height: 8px; border-radius: 4px; background-color: blue }</style></head><body><h1>Hello server</h1>\n";
//        Map<String, String> parms = session.getParms();
//        if(parms.get("username") == null) {
//            msg += "<form action='?' method='get'>\n  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
//
//            msg += "<img src=\"data:image/jpeg;base64," + imageData + "\" style=\"position:absolute; top: 0px; left: 0px\"/>";
//            msg += "<img src=\"data:image/jpeg;base64," + imageData2 + "\" id=\"camera\"/>";
//			for(Vector2 pos : coords)
//				msg += "<div class=\"point\" style=\"left: " + (pos.x) + "px; top: " + (200 + pos.y) + "px\"> </div>";
//			for(Vector2 pos : coords2)
//				msg += "<div class=\"point\" style=\"background-color: red; left: " + (pos.x) + "px; top: " + (200 + pos.y) + "px\"> </div>";
//
//            msg += "<p style=\"position:absolute; top: 0px; left: 320px\">" + text + "<p/>";
//        } else {
//            msg += "<p >Hello, " + parms.get("username") + "!</p>";
//        }
//
//        return newFixedLengthResponse(msg + "</body></html>\n");
		return newFixedLengthResponse(page);
    }
}