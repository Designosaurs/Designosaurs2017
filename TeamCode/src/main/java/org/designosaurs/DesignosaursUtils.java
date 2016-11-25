package org.designosaurs;

import android.graphics.Bitmap;
import android.graphics.Matrix;

class DesignosaursUtils {
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
