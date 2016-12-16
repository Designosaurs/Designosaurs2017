package org.designosaurs.opmode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.wifi.WifiManager;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

class DesignosaursUtils {
	static String getIpAddress(Context context) {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

		// Convert little-endian to big-endian if needed
		if(ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN))
			ipAddress = Integer.reverseBytes(ipAddress);

		byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

		String ipAddressString;
		try {
			ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
		} catch(UnknownHostException ex) {
			ipAddressString = null;
		}

		return ipAddressString;
	}

	static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
		if(maxHeight > 0 && maxWidth > 0) {
			int width = image.getWidth();
			int height = image.getHeight();
			float ratioBitmap = (float) width / (float) height;
			float ratioMax = (float) maxWidth / (float) maxHeight;

			int finalWidth = maxWidth;
			int finalHeight = maxHeight;
			if(ratioMax > 1) {
				finalWidth = (int) ((float) maxHeight * ratioBitmap);
			} else {
				finalHeight = (int) ((float) maxWidth / ratioBitmap);
			}
			image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
			return image;
		} else {
			return image;
		}
	}

	static Bitmap rotate(Bitmap source, float angle) {
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}
}
