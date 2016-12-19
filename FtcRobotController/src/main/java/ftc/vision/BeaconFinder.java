package ftc.vision;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileOutputStream;

public class BeaconFinder implements ImageProcessor<BeaconPositionResult> {
    private static final boolean DEBUG = true;
    private static final String TAG = "BeaconFinder";

    private Scalar blackMin = new Scalar(0, 0, 0, 0);
    private Scalar blackMax = new Scalar(80, 80, 80, 255);

    // Distance between detected centers
    private int MINIMUM_CIRCLE_DISTANCE = 250;
    // Restrictions for canny edge detection, https://en.wikipedia.org/wiki/Canny_edge_detector
    private int UPPER_THRESHOLD = 50;
    private int LOWER_THRESHOLD = 30;
    // Restrictions on circle size
    private int MIN_RADIUS = 14;
    private int MAX_RADIUS = 25;

    // Distance to crop from the button to edge of beacon
    private int BEACON_CROP_DISTANCE = 100;

    void saveRawMat(String name, Mat src) {
        Bitmap bmp = null;
        try {
            bmp = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(src, bmp);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

        FileOutputStream out = null;
        try {
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + System.currentTimeMillis() + "_" + name + ".png";
            Log.i(TAG, "Saving mat to " + path);
            out = new FileOutputStream(path);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(out != null)
                    out.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ImageProcessorResult<BeaconPositionResult> process(long startTime, Mat rgbaFrame, boolean saveImages) {
        Log.i(TAG, "*** STARTING BEACON FINDER FRAME PROCESSING ***");

        Mat workingFrame = rgbaFrame.clone();
        Mat processedFrame;

        if(DEBUG)
            processedFrame = rgbaFrame.clone();

        // save the original image in the Pictures directory
        if(saveImages)
            ImageUtil.saveImage(TAG, rgbaFrame, Imgproc.COLOR_RGBA2BGR, "0_camera", startTime);

        // apply a blur to reduce graininess of the camera, overall noise
        Imgproc.GaussianBlur(workingFrame, workingFrame, new Size(9, 9), 0);

        // detect only dark sections
        Mat darkSections = new Mat(workingFrame.rows(), workingFrame.cols(), workingFrame.type());
        Core.inRange(workingFrame, blackMin, blackMax, darkSections);
        // TODO: this call can be made more efficient
        Imgproc.cvtColor(darkSections, darkSections, Imgproc.COLOR_GRAY2BGRA);
        Imgproc.GaussianBlur(darkSections, darkSections, new Size(9, 9), 0);
        Imgproc.cvtColor(darkSections, darkSections, Imgproc.COLOR_BGRA2GRAY);

        // use hough circle detection method
        Mat circles = new Mat();
        Imgproc.HoughCircles(darkSections, circles, Imgproc.CV_HOUGH_GRADIENT, 1, MINIMUM_CIRCLE_DISTANCE, UPPER_THRESHOLD, LOWER_THRESHOLD, MIN_RADIUS, MAX_RADIUS);

        // get points of buttons
        double leftButton = 0,
               rightButton = 0,
               average;

        for (int i = 0; i < circles.cols(); i++) {
            double data[] = circles.get(0, i);

            if(DEBUG) {
                // draw circles on processed image:
                Imgproc.circle(processedFrame, new Point(data[0], data[1]), (int) data[2], new Scalar(0, 200, 0, 255), 4);
            }

            if(leftButton == 0)
                leftButton = data[0];
            else
                rightButton = data[0];
        }

        average = Math.floor(((rightButton - leftButton) / 2) + leftButton);

        if(DEBUG && saveImages) {
            saveRawMat("black", darkSections);
            saveRawMat("processed", processedFrame);
        }

        Log.i(TAG, "Processing finished, took " + (System.currentTimeMillis() - startTime) + "ms");

        return new ImageProcessorResult<>(startTime, null, new BeaconPositionResult(average, leftButton - BEACON_CROP_DISTANCE, rightButton + BEACON_CROP_DISTANCE));
    }
}