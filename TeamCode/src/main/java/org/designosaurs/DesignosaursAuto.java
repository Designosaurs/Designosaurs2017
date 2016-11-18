package org.designosaurs;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.qualcomm.ftcrobotcontroller.R;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.vuforia.Matrix34F;
import com.vuforia.Tool;
import com.vuforia.Vec3F;

import org.firstinspires.ftc.robotcontroller.internal.FtcRobotControllerActivity;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.Arrays;

import ftc.vision.BeaconColorResult;
import ftc.vision.BeaconProcessor;

@Autonomous(name = "Designosaurs Autonomous", group = "Auto")
public class DesignosaursAuto extends LinearOpMode {
	private DesignosaursHardware robot = new DesignosaursHardware();
	private BeaconProcessor beaconProcessor = new BeaconProcessor();
	private ButtonPusherManager buttonPusherManager = new ButtonPusherManager(robot);

	private final boolean OBFUSCATE_MIDDLE = true;

	private int centeredPos = Integer.MAX_VALUE;

	private final String VUFORIA_LICENCE_KEY = "ATwI0oz/////AAAAGe9HyiYVEU6pmTFAb65tOfUrioTxlZtITHRLN1h3wllaw67kJsUOHwPVDsCN0vxiKy/9Qi9NnjpkVfUnn0gwIHyKJgTYkG7+dCaJtFJlY94qa1YPCy0y4rwhVQFkDkcaCiNoiS7ZSU5KLeIABF4Gvz9qYwJJtwxWGp4fbjyu+arTOUw160+Fg5XMjoftS8FAQPx4wF33sVdGw+CYX0fHdwQzOyN0PpIwBQ9xvb8e1c76FoHF0YUZyV/q0XeR97nRj1TfnesPc+v7Z72SEDCXAAdVVS6L9u/mVAxq4zTaXsdGcVsqHeaouoGmQ/1Ey/YYShqHaRZXWwC4GsgaxO9tCkWNH+hTjFZA2pgvKVl5HmLR";

	private static final double FAST_DRIVE_POWER = 1.0;
	private static final double TURN_POWER = 0.4;
	private static final double DRIVE_POWER = 0.2;
	private static final double SLOW_DOWN_AT = 3000;
	private static final int BEACON_ALIGNMENT_TOLERANCE = 100;

	/* State Machine Options */
	private final byte STATE_INITIAL_POSITIONING = 0;
	private final byte STATE_SEARCHING = 1;
	private final byte STATE_ALIGNING_WITH_BEACON = 2;
	private final byte STATE_WAITING_FOR_PLACER = 3;
	private final byte STATE_ROTATING_TOWARDS_GOAL = 4;
	private final byte STATE_DRIVING_TOWARDS_GOAL = 5;
	private final byte STATE_SCORING_IN_GOAL = 6;
	private final byte STATE_DRIVING_TO_RAMP = 7;
	private final byte STATE_FINISHED = 8;

	/* Current State */
	private byte autonomousState = STATE_INITIAL_POSITIONING;
	private int ticksInState = 0;
	private BeaconColorResult lastBeaconColor;
	private BeaconColorResult.BeaconColor targetColor;
	private String stateMessage = "Starting...";

	private static String TAG = "DesignosaursAuto";

	private void setInitState(String state) {
		telemetry.clear();
		telemetry.addLine("== Designosaurs 2017 ==");
		telemetry.addLine(state);
		telemetry.update();
	}

	public void updateRunningState() {
		telemetry.clear();
		telemetry.addLine(stateMessage);
		telemetry.addLine("");
		telemetry.addLine("State: " + String.valueOf(autonomousState));
		telemetry.addLine("Ticks in state: " + String.valueOf(ticksInState));
		telemetry.update();
	}

	@Override
	public void runOpMode() throws InterruptedException {
		boolean isFirstRun = true;

		setInitState("Configuring hardware...");

		robot.init(hardwareMap);

		setInitState("Initializing vuforia...");

		VuforiaLocalizer.Parameters params = new VuforiaLocalizer.Parameters(R.id.cameraMonitorViewId);
		params.cameraDirection = VuforiaLocalizer.CameraDirection.BACK;
		params.vuforiaLicenseKey = VUFORIA_LICENCE_KEY;

		VuforiaLocalizerImplSubclass vuforia = new VuforiaLocalizerImplSubclass(params);

		VuforiaTrackables beacons = vuforia.loadTrackablesFromAsset("FTC_2016-17");
		beacons.get(0).setName("wheels");
		beacons.get(1).setName("tools");
		beacons.get(2).setName("legos");
		beacons.get(3).setName("gears");

		setInitState("Initializing OpenCV...");

		OpenCVLoader.initDebug();

		setInitState("Ready!");

		waitForStart();

		buttonPusherManager.start();
		buttonPusherManager.setStatus(ButtonPusherManager.STATE_HOMING);
		beacons.activate();

		byte beaconsFound = 0;

		while(opModeIsActive()) {
			Mat output = new Mat();
			String imageName = "";
			boolean havePixelData = true;

			for(VuforiaTrackable beac : beacons) {
				OpenGLMatrix pose = ((VuforiaTrackableDefaultListener) beac.getListener()).getRawPose();

				if(pose != null) {
					imageName = beac.getName();

					Matrix34F rawPose = new Matrix34F();
					float[] poseData = Arrays.copyOfRange(pose.transposed().getData(), 0, 12);
					rawPose.setData(poseData);

					Vector2 center = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(0, 0, 0)));
					Vector2 upperLeft = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(-100, 260, 0))); // -127, 92, 0
					Vector2 upperRight = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(100, 260, 0))); // 127, 92, 0
					Vector2 lowerLeft = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(-100, 142, 0))); // -127, -92, 0
					Vector2 lowerRight = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(100, 142, 0))); // 127, -92, 0

					if(vuforia.rgb != null) {
						Bitmap bm = Bitmap.createBitmap(vuforia.rgb.getWidth(), vuforia.rgb.getHeight(), Bitmap.Config.RGB_565);
						bm.copyPixelsFromBuffer(vuforia.rgb.getPixels());

						centeredPos = center.X;
						isFirstRun = false;

						Vector2 start = new Vector2(Math.max(0, Math.min(upperLeft.X, lowerLeft.X)), Math.min(bm.getHeight()-1,Math.max(0, Math.min(upperLeft.Y, upperRight.Y))));
						Vector2 end = new Vector2(Math.max(0, Math.max(lowerRight.X, upperRight.X)), Math.min(bm.getHeight()-1,Math.max(0, Math.max(lowerLeft.Y, lowerRight.Y))));

						try {
							Bitmap a = Bitmap.createBitmap(bm, start.X, start.Y, Math.min(end.X, (1280 - start.X) > 0 ? 1280 - start.X : 50), Math.min(end.Y, (720 - start.Y) > 0 ? 720 - start.Y : 50));

							Bitmap resizedbitmap = DesignosaursUtils.resize(a, a.getWidth() / 2, a.getHeight() / 2);
							resizedbitmap = DesignosaursUtils.rotate(resizedbitmap, 90);

							if(OBFUSCATE_MIDDLE) {
								Canvas canvas = new Canvas(resizedbitmap);
								Paint paint = new Paint();
								paint.setColor(Color.WHITE);

								canvas.drawRect(resizedbitmap.getWidth() * 12 / 30, 0, resizedbitmap.getWidth() * 17 / 30, resizedbitmap.getHeight(), paint);
							}

							Utils.bitmapToMat(resizedbitmap, output);

							FtcRobotControllerActivity.simpleController.setImage(resizedbitmap);
							FtcRobotControllerActivity.simpleController.setImage2(bm);
						} catch(IllegalArgumentException e) {
							havePixelData = false;
							//e.printStackTrace();
						}
					}
				}
			}

			if(isFirstRun)
				centeredPos = Integer.MAX_VALUE;

			buttonPusherManager.update();

			switch(autonomousState) {
				case STATE_INITIAL_POSITIONING:
					stateMessage = "Getting in position...";

					updateRunningState();

					robot.goStraight(2, FAST_DRIVE_POWER);
					robot.turn(30, TURN_POWER);
					robot.goStraight(0.5, FAST_DRIVE_POWER);

					setState(STATE_SEARCHING);
				break;
				case STATE_SEARCHING:
					if(ticksInState > 1000) {
						stateMessage = "Searching for beacon...";

						if(Math.abs(getRelativePosition()) > SLOW_DOWN_AT)  {
							stateMessage = "Slowing down and searching for beacon.";

							robot.setDrivePower(0.6 * DRIVE_POWER);
						}

						if(Math.abs(getRelativePosition()) < BEACON_ALIGNMENT_TOLERANCE) {
							stateMessage = "Analysing beacon data...";

							robot.setDrivePower(0.08);

							if(havePixelData) {
								stateMessage = "Beacon found!";

								lastBeaconColor = beaconProcessor.process(System.currentTimeMillis(), output, false).getResult();
								targetColor = (imageName.equals("wheels") || imageName.equals("legos")) ? BeaconColorResult.BeaconColor.BLUE : BeaconColorResult.BeaconColor.RED;

								Log.i("DesignosaursAuto", "*** BEACON FOUND ***");
								Log.i("DesignosaursAuto", "Target color: " + (targetColor == BeaconColorResult.BeaconColor.BLUE ? "Blue" : "Red"));

								robot.setDrivePower((lastBeaconColor.getLeftColor() == targetColor) ? -DRIVE_POWER * 0.5 : DRIVE_POWER * 0.5);
								robot.resetEncoder(robot.leftMotor);
								robot.resetEncoder(robot.rightMotor);

								setState(STATE_ALIGNING_WITH_BEACON);
							} else
								Log.w("DesignosaursAuto", "Beacon is seen, but failed to transfer data to OpenCV.");
						} else {
							stateMessage = "Aligning with beacon...";

							if(Math.abs(getRelativePosition()) < SLOW_DOWN_AT)
								if(getRelativePosition() > 0)
									robot.setDrivePower(DRIVE_POWER * 0.5);
								else
									robot.setDrivePower(DRIVE_POWER * -0.5);
						}
					}
				break;
				case STATE_ALIGNING_WITH_BEACON:
					stateMessage = "Positioning to deploy placer...";

					if(Math.abs(robot.getAdjustedEncoderPosition(robot.leftMotor)) >= 600) {
						Log.i("DesignosaursAuto", "//// DEPLOYING ////");

						robot.setDrivePower(0);

						buttonPusherManager.setStatus(ButtonPusherManager.STATE_SCORING);

						setState(STATE_WAITING_FOR_PLACER);
					} else {
						Log.i("DesignosaursAuto", "Aligning... " + ticksInState);
					}
				break;
				case STATE_WAITING_FOR_PLACER:
					stateMessage = "Waiting for placer to deploy.";

					if(buttonPusherManager.getStatus() != ButtonPusherManager.STATE_SCORING) {

						beaconsFound++;

						if(beaconsFound == 2)
							setState(STATE_ROTATING_TOWARDS_GOAL);
						else
							setState(STATE_SEARCHING);
					}
				break;
				case STATE_ROTATING_TOWARDS_GOAL:
					robot.rotateToPosition(45, TURN_POWER);
					setState(STATE_DRIVING_TOWARDS_GOAL);
				break;
				case STATE_DRIVING_TOWARDS_GOAL:
					robot.goStraight(4, FAST_DRIVE_POWER);
					setState(STATE_SCORING_IN_GOAL);
				break;
				case STATE_SCORING_IN_GOAL:
					// ...
					setState(STATE_DRIVING_TO_RAMP);
				break;
				case STATE_DRIVING_TO_RAMP:
					robot.goStraight(3, FAST_DRIVE_POWER);

					setState(STATE_FINISHED);
			}

			ticksInState++;
			updateRunningState();
		}
	}

	private void setState(byte newState) {
		Log.i("DesignosaursAuto", "*** SWITCHING STATES ***");
		Log.i("DesignosaursAuto", "New state: " + String.valueOf(newState));
		Log.i("DesignosaursAuto", "Time in previous state: " + String.valueOf(ticksInState));

		autonomousState = newState;
		ticksInState = 0;
	}

	private int getRelativePosition() {
		return centeredPos - 840;
	}
}