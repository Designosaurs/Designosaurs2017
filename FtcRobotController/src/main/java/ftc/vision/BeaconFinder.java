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
    private int MINIMUM_CIRCLE_DISTANCE = 100;
    // Restrictions for canny edge detection, https://en.wikipedia.org/wiki/Canny_edge_detector
    private int UPPER_THRESHOLD = 200;
    private int LOWER_THRESHOLD = 100;
    // Restrictions on circle size
    private int MIN_RADIUS = 14;
    private int MAX_RADIUS = 25;

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
        saveImages = true; // TODO: remember to turn this off

        Mat workingFrame = rgbaFrame.clone();
        Log.i(TAG, "*** STARTING BEACON FINDER FRAME PROCESSING ***");

        // save the original image in the Pictures directory
        if(saveImages)
            ImageUtil.saveImage(TAG, rgbaFrame, Imgproc.COLOR_RGBA2BGR, "0_camera", startTime);

        // apply a blur to reduce graininess of the camera, overall noise
        Imgproc.cvtColor(rgbaFrame, workingFrame, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(workingFrame, workingFrame, new Size(9, 9), 5);

        // detect only dark sections
        Mat darkSections = new Mat(workingFrame.rows(), workingFrame.cols(), workingFrame.type());
        //Imgproc.rectangle(darkSections, new Point(0, 0), new Point(workingFrame.width(), workingFrame.height()), new Scalar(255, 255, 255, 255), -1);
        Core.inRange(workingFrame, blackMin, blackMax, darkSections);

        // use hough's circle detection method
        Mat circles = new Mat();
        Imgproc.HoughCircles(darkSections, circles, Imgproc.CV_HOUGH_GRADIENT, 1, MINIMUM_CIRCLE_DISTANCE, UPPER_THRESHOLD, LOWER_THRESHOLD, MIN_RADIUS, MAX_RADIUS);
        //Imgproc.HoughCircles(darkSections, circles, Imgproc.CV_HOUGH_GRADIENT, 1, MINIMUM_CIRCLE_DISTANCE);

        Point pt = new Point();
        for (int i = 0; i < circles.cols(); i++) {
            double data[] = circles.get(0, i);
            pt.x = data[0];
            pt.y = data[1];
            double rho = data[2];
            Imgproc.circle(rgbaFrame, pt, (int) rho, new Scalar(0, 200, 0, 255), 4);
        }

        if(DEBUG && saveImages) {
            saveRawMat("black", darkSections);
            saveRawMat("blur", workingFrame);
            saveRawMat("circles", circles);
            saveRawMat("processed", rgbaFrame);
        }

        Log.i(TAG, "Processing finished, took " + (System.currentTimeMillis() - startTime) + "ms");

        return new ImageProcessorResult<>(startTime, null, new BeaconPositionResult(0, 0, rgbaFrame.width()));
    }
}