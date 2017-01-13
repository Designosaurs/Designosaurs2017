package ftc.vision;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class BeaconFinder implements ImageProcessor<BeaconPositionResult> {
    private static final boolean DEBUG = true;
    private static final String TAG = "BeaconFinder";

    private Scalar blackMin = new Scalar(0, 0, 0, 0);
    private Scalar blackMax = new Scalar(100, 100, 100, 255);

    // Distance between detected centers
    private int MINIMUM_CIRCLE_DISTANCE = 250;
    // Restrictions for canny edge detection, https://en.wikipedia.org/wiki/Canny_edge_detector
    private int UPPER_THRESHOLD = 50;
    private int LOWER_THRESHOLD = 30;
    // Restrictions on circle size
    private int MIN_RADIUS = 20;
    private int MAX_RADIUS = 50;

    // Distance to crop from the button to edge of beacon
    private int BEACON_CROP_DISTANCE = 100;

    @Override
    public ImageProcessorResult<BeaconPositionResult> process(long startTime, Mat rgbaFrame, boolean saveImages) {
        Log.i(TAG, "*** STARTING BEACON FINDER FRAME PROCESSING ***");

        Mat workingFrame = rgbaFrame.clone();
        Mat processedFrame;

        if(DEBUG)
            processedFrame = rgbaFrame.clone();

        // save the original image in the Pictures directory
        if(saveImages)
            ImageUtil.saveImage(TAG, rgbaFrame, Imgproc.COLOR_RGBA2BGR, "BeaconFinder-" + System.currentTimeMillis() + "-0-original");

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
               average,
               buttonsFound = 0;

        for(int i = 0; i < circles.cols(); i++) {
            double data[] = circles.get(0, i);

            if(DEBUG) {
                // draw circles on processed image:
                Imgproc.circle(processedFrame, new Point(data[0], data[1]), (int) data[2], new Scalar(0, 200, 0, 255), 4);
            }

            if(leftButton == 0)
                leftButton = data[0];
            else
                rightButton = data[0];

            buttonsFound++;
        }

        if(leftButton > rightButton) {
            double initialLeft = leftButton;

            leftButton = rightButton;
            rightButton = initialLeft;
        }

        average = Math.floor(((rightButton - leftButton) / 2) + leftButton);

        if(DEBUG && saveImages) {
            ImageUtil.saveImage(TAG, darkSections, -1, "BeaconFinder-" + System.currentTimeMillis() + "-1-black");
            ImageUtil.saveImage(TAG, processedFrame, -1, "BeaconFinder-" + System.currentTimeMillis() + "-2-processed");
        }

        Log.i(TAG, "Buttons found: " + buttonsFound);
        Log.i(TAG, "Processing finished, took " + (System.currentTimeMillis() - startTime) + "ms");

        return new ImageProcessorResult<>(startTime, null, new BeaconPositionResult(buttonsFound == 2, average, leftButton - BEACON_CROP_DISTANCE, rightButton + BEACON_CROP_DISTANCE));
    }
}