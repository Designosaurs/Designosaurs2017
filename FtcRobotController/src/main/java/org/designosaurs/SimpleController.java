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

public class SimpleController extends NanoHTTPD {
    private static final int PORT = 9001;
    private String imageData = "";
    private String imageData2 = "";
    public String text = "";
	public static ArrayList<Vector2> coords = new ArrayList<>();
	public static ArrayList<Vector2> coords2 = new ArrayList<>();
	public static String page;
    public static boolean enabled = true;

    public SimpleController() throws IOException {
        super(PORT);

        if(!enabled)
            return;

        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("\nSimple Web Controller Interface started on port " + PORT + "\n");
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
		return newFixedLengthResponse(page);
    }
}