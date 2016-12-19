package ftc.vision;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class BeaconFinder implements ImageProcessor<BeaconPositionResult> {
    private static final boolean DEBUG = false;
    private static final String TAG = "BeaconFinder";

    private Scalar blackMin = new Scalar(0, 0, 0);
    private Scalar blackMax = new Scalar(50, 50, 50);

    @Override
    public ImageProcessorResult<BeaconPositionResult> process(long startTime, Mat rgbaFrame, boolean saveImages) {
        saveImages = true; // TODO: remember to turn this off

        Log.i(TAG, "*** STARTING BEACON FINDER FRAME PROCESSING ***");

        Mat rgbaFrameBak = rgbaFrame.clone();
        //save the image in the Pictures directory
        if(saveImages)
            ImageUtil.saveImage(TAG, rgbaFrame, Imgproc.COLOR_RGBA2BGR, "0_camera", startTime);

        // make a list of channels that are blank (used for combining binary images)
        List<Mat> rgbaChannels = new ArrayList<>();

        rgbaChannels.add(Mat.zeros(rgbaFrame.size(), CvType.CV_8UC1));

        Mat maskedImage = new Mat();
        Core.inRange(rgbaFrame, blackMin, blackMax, maskedImage);
        rgbaChannels.add(maskedImage);

        rgbaChannels.add(Mat.zeros(rgbaFrame.size(), CvType.CV_8UC1));

        if(DEBUG) {
            //add empty alpha channel
            rgbaChannels.add(Mat.zeros(rgbaFrame.size(), CvType.CV_8UC1));
            //merge the 3 binary images and 1 alpha channel into one image
            Core.merge(rgbaChannels, rgbaFrame);
        }

        Log.i(TAG, "Processing finished, took " + (System.currentTimeMillis() - startTime) + "ms");

        if(DEBUG) {
            Mat output = rgbaFrame.clone();

            ImageUtil.overlayImage(rgbaFrameBak, rgbaFrame, output);

            if(saveImages) {
                ImageUtil.saveImage(TAG, output, Imgproc.COLOR_RGBA2BGR, "0_processed", startTime);
            }

            //construct and return the result
            return new ImageProcessorResult<>(startTime, output, new BeaconPositionResult(0, 0, rgbaFrame.width()));
        } else {
            return new ImageProcessorResult<>(startTime, null, new BeaconPositionResult(0, 0, rgbaFrame.width()));
        }
    }
}