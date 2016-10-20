package org.designosaurs;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.qualcomm.ftcrobotcontroller.R;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.vuforia.Matrix34F;
import com.vuforia.Tool;
import com.vuforia.Vec3F;

import org.firstinspires.ftc.robotcontroller.internal.FtcRobotControllerActivity;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.Arrays;

import ftc.vision.BeaconProcessor;
import ftc.vision.ImageProcessorResult;

@Autonomous(name = "Designosaurs Autonomous", group = "Auto")
public class DesignosaursAuto extends LinearOpMode {
	private DesignosaursHardware robot = new DesignosaursHardware();
	private BeaconProcessor beaconProcessor = new BeaconProcessor();

	private final boolean FAKE_TAPE = true;

	public final String VUFORIA_LICENCE_KEY = "ATwI0oz/////AAAAGe9HyiYVEU6pmTFAb65tOfUrioTxlZtITHRLN1h3wllaw67kJsUOHwPVDsCN0vxiKy/9Qi9NnjpkVfUnn0gwIHyKJgTYkG7+dCaJtFJlY94qa1YPCy0y4rwhVQFkDkcaCiNoiS7ZSU5KLeIABF4Gvz9qYwJJtwxWGp4fbjyu+arTOUw160+Fg5XMjoftS8FAQPx4wF33sVdGw+CYX0fHdwQzOyN0PpIwBQ9xvb8e1c76FoHF0YUZyV/q0XeR97nRj1TfnesPc+v7Z72SEDCXAAdVVS6L9u/mVAxq4zTaXsdGcVsqHeaouoGmQ/1Ey/YYShqHaRZXWwC4GsgaxO9tCkWNH+hTjFZA2pgvKVl5HmLR";

	private float mmPerInch = 25.4f;
	private float mmBotWidth = 18 * mmPerInch;
	private float mmFTCFieldWidth = (12 * 12 - 2) * mmPerInch;

	@Override
	public void runOpMode() throws InterruptedException {
		robot.init(hardwareMap);

		VuforiaLocalizer.Parameters params = new VuforiaLocalizer.Parameters(R.id.cameraMonitorViewId);
		params.cameraDirection = VuforiaLocalizer.CameraDirection.BACK;
		params.vuforiaLicenseKey = VUFORIA_LICENCE_KEY;

		VuforiaLocalizerImplSubclass vuforia = new VuforiaLocalizerImplSubclass(params);

		VuforiaTrackables beacons = vuforia.loadTrackablesFromAsset("FTC_2016-17");
		beacons.get(0).setName("Wheels");
		beacons.get(1).setName("Tools");
		beacons.get(2).setName("Lego");
		beacons.get(3).setName("Gears");

		waitForStart();

		//robot.buttonPusher.setTargetPosition((int) (DesignosaursHardware.COUNTS_PER_REVOLUTION * 1.5));
//		robot.setDrivePower(0.5);

		beacons.activate();

		while(opModeIsActive()) {
			for(VuforiaTrackable beac : beacons) {
				OpenGLMatrix pose = ((VuforiaTrackableDefaultListener) beac.getListener()).getRawPose();

				if(pose != null) {
//					robot.setDrivePower(0);

					Matrix34F rawPose = new Matrix34F();
					float[] poseData = Arrays.copyOfRange(pose.transposed().getData(), 0, 12);
					rawPose.setData(poseData);

					Vector2 upperLeft = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(-100, 260, 0)));//-127, 92, 0
					Vector2 upperRight = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(100, 260, 0)));//127, 92, 0
					Vector2 lowerLeft = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(-100, 142, 0)));//-127, -92, 0
					Vector2 lowerRight = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(100, 142, 0)));//127, -92, 0

					Vector2 start = new Vector2(Math.min(upperLeft.x, lowerLeft.x), Math.min(upperLeft.y, upperRight.y));
					Vector2 end = new Vector2(Math.max(lowerRight.x, upperRight.x), Math.max(lowerLeft.y, lowerRight.y));
					String debugData = "";
					debugData += "upperLeft" + upperLeft.toString();
					debugData += "upperRight" + upperRight.toString();
					debugData += "lowerLeft" + lowerLeft.toString();
					debugData += "lowerRight" + lowerRight.toString();

					debugData += "Start(" + start.x + "," + start.y + "),";
					debugData += "End(" + end.x + "," + end.y + ")";

					VectorF translation = pose.getTranslation();

					telemetry.addData(beac.getName() + "-Translation", translation);

					double degreesToTurn = Math.toDegrees(Math.atan2(translation.get(1), translation.get(2)));

					telemetry.addData(beac.getName() + "-Degrees", degreesToTurn);

					if(vuforia.rgb != null) {
						Bitmap bm = Bitmap.createBitmap(vuforia.rgb.getWidth(), vuforia.rgb.getHeight(), Bitmap.Config.RGB_565);
						bm.copyPixelsFromBuffer(vuforia.rgb.getPixels());
//						bm = RotateBitmap(bm, 90);
						int startX = Math.max(0, start.x);
						int startY = Math.max(0, start.y);
						int endX = Math.max(0, Math.abs(start.x - end.x));
						int endY = Math.max(0, Math.abs(start.y - end.y));

//						try {
							Bitmap a = Bitmap.createBitmap(bm, startX, startY, endX, endY);

							Bitmap resizedbitmap = resize(a, a.getWidth() / 2, a.getHeight() / 2);
							resizedbitmap = RotateBitmap(resizedbitmap, 90);

							if(FAKE_TAPE) {
								Canvas canvas = new Canvas(resizedbitmap);
								Paint paint = new Paint();
								paint.setColor(Color.WHITE);

								canvas.drawRect(resizedbitmap.getWidth() * 12 / 30, 0, resizedbitmap.getWidth() * 17 / 30, resizedbitmap.getHeight(), paint);
							}

							Mat output = new Mat();

							Utils.bitmapToMat(resizedbitmap, output);

							ImageProcessorResult result = beaconProcessor.process(System.currentTimeMillis(), output, false);

							debugData += result.getResult().toString();

							FtcRobotControllerActivity.simpleController.setImage(resizedbitmap);

							FtcRobotControllerActivity.simpleController.setImage2(bm);

//						} catch(IllegalArgumentException e) {
//							e.printStackTrace();
//						}

						//FtcRobotControllerActivity.simpleController.text = debugData;
						SimpleController.coords.clear();
						SimpleController.coords2.clear();

						SimpleController.coords.add(upperLeft);
						SimpleController.coords.add(upperRight);
						SimpleController.coords.add(lowerLeft);
						SimpleController.coords.add(lowerRight);

						SimpleController.coords2.add(start);
						SimpleController.coords2.add(end);
					}
				}
			}
			telemetry.update();
		}
	}

	private static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
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

	static Bitmap RotateBitmap(Bitmap source, float angle) {
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}
}