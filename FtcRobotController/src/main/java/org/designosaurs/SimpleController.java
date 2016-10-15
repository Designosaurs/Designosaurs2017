package org.designosaurs;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class SimpleController extends NanoHTTPD {
    private static final int PORT = 9001;
    private String imageData = "";

    public SimpleController() throws IOException {
        super(PORT);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("\nSimple Web Controller Interface started on port " + PORT + "\n");
    }

    public void setImage(Bitmap bmp) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        bmp.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        imageData = Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public void setImage(Mat data) {
        Bitmap bmp = null;

        try {
            bmp = Bitmap.createBitmap(data.cols(), data.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(data, bmp);

            data.release();

            setImage(bmp);
        } catch (CvException | NullPointerException e) {
            Log.d("WebServer", e.getMessage());
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String msg = "<html><body><h1>Hello server</h1>\n";
        Map<String, String> parms = session.getParms();
        if(parms.get("username") == null) {
            msg += "<form action='?' method='get'>\n  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";

            msg += "<img src=\"data:image/png;base64," + imageData + "\"/>";
        } else {
            msg += "<p>Hello, " + parms.get("username") + "!</p>";
        }

        return newFixedLengthResponse(msg + "</body></html>\n");
    }
}