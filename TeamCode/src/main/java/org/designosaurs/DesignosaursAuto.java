package org.designosaurs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.qualcomm.ftcrobotcontroller.R;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.vuforia.Matrix34F;
import com.vuforia.Tool;
import com.vuforia.Vec3F;

import org.firstinspires.ftc.robotcontroller.internal.FtcRobotControllerActivity;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.Arrays;

import ftc.vision.BeaconColorResult;
import ftc.vision.BeaconProcessor;

@Autonomous(name = "Designosaurs Autonomous", group = "Auto")
public class DesignosaursAuto extends DesignosaursOpMode {
	private DesignosaursHardware robot = new DesignosaursHardware();
	private BeaconProcessor beaconProcessor = new BeaconProcessor();
	private ButtonPusherManager buttonPusherManager = new ButtonPusherManager(robot);

	private final boolean OBFUSCATE_MIDDLE = true;
	private final String VUFORIA_LICENCE_KEY = "ATwI0oz/////AAAAGe9HyiYVEU6pmTFAb65tOfUrioTxlZtITHRLN1h3wllaw67kJsUOHwPVDsCN0vxiKy/9Qi9NnjpkVfUnn0gwIHyKJgTYkG7+dCaJtFJlY94qa1YPCy0y4rwhVQFkDkcaCiNoiS7ZSU5KLeIABF4Gvz9qYwJJtwxWGp4fbjyu+arTOUw160+Fg5XMjoftS8FAQPx4wF33sVdGw+CYX0fHdwQzOyN0PpIwBQ9xvb8e1c76FoHF0YUZyV/q0XeR97nRj1TfnesPc+v7Z72SEDCXAAdVVS6L9u/mVAxq4zTaXsdGcVsqHeaouoGmQ/1Ey/YYShqHaRZXWwC4GsgaxO9tCkWNH+hTjFZA2pgvKVl5HmLR";

	/* Configuration */
	private static final double FAST_DRIVE_POWER = 0.7;
	private static final double TURN_POWER = 0.25;
	private static final double DRIVE_POWER = 0.3;
	private static final double SLOW_DOWN_AT = 3000;
	private static final int BEACON_ALIGNMENT_TOLERANCE = 100;
	public static final boolean SAVE_IMAGES = false;

	/* State Machine */
	private final byte STATE_INITIAL_POSITIONING = 0;
	private final byte STATE_SEARCHING = 1;
	private final byte STATE_ALIGNING_WITH_BEACON = 2;
	private final byte STATE_WAITING_FOR_PLACER = 3;
	private final byte STATE_FINISHED = 4;

	/* Team Colors */
	private final byte TEAM_UNSELECTED = 0;
	private final byte TEAM_BLUE = 1;
	private final byte TEAM_RED = 2;

	/* Beacon Sides */
	private final byte SIDE_LEFT = 0;
	private final byte SIDE_RIGHT = 1;

	/* Current State */
	private byte autonomousState = STATE_INITIAL_POSITIONING;
	private int ticksInState = 0;
	private String stateMessage = "Starting...";
	private byte beaconsFound = 0;

	private static String TAG = "DesignosaursAuto";
	private int centeredPos = Integer.MAX_VALUE;
	private long lastTelemetryUpdate = 0;
	private byte teamColor = TEAM_RED;
	private byte targetSide = SIDE_LEFT;
	private String lastScoredBeaconName = "";
	private Context appContext;

	private void setInitState(String state) {
		telemetry.clear();
		telemetry.addLine("== Designosaurs 2017 ==");
		telemetry.addLine(state);
		telemetry.addLine("");
		telemetry.addLine("Team color: " + teamColorToString());
		telemetry.update();
	}

	private void updateRunningState() {
		if(System.currentTimeMillis() - lastTelemetryUpdate < 250)
			return;

		lastTelemetryUpdate = System.currentTimeMillis();
		telemetry.clear();
		telemetry.addLine(stateMessage);
		telemetry.addLine("");
		telemetry.addLine("State: " + getStateMessage());
		telemetry.addLine("Button pusher: " + buttonPusherManager.getStatusMessage());
		telemetry.addLine("Time in state: " + String.valueOf(ticksInState));
		telemetry.addLine("Centered: " + String.valueOf(getRelativePosition()));
		telemetry.addLine("Beacons scored: " + String.valueOf(beaconsFound));
		telemetry.update();
	}

	private void updateRunningState(String newStateMessage) {
		stateMessage = newStateMessage;

		updateRunningState();
	}

	public void updateTeamColor() {
		if(gamepad1.x)
			teamColor = TEAM_BLUE;

		if(gamepad1.b)
			teamColor = TEAM_RED;

		setInitState("Ready!");
	}

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(appContext) {
		@Override
		public void onManagerConnected(int status) {
			switch(status) {
				case LoaderCallbackInterface.SUCCESS:
					Log.i(TAG, "OpenCV loaded successfully!");
				break;
				default:
					Log.i(TAG, "OpenCV load failure.");
			}
		}
	};


	@Override
	public void runOpMode() {
		setInitState("Configuring hardware...");
		robot.init(hardwareMap);
		appContext = hardwareMap.appContext;

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
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, hardwareMap.appContext, mLoaderCallback);

		setInitState("Select team color using the gamepad.");

		while(!isStarted()) {
			if(gamepad1.x || gamepad1.b)
				updateTeamColor();

			robot.waitForTick(20);
		}

		buttonPusherManager.start();
		buttonPusherManager.setStatus(ButtonPusherManager.STATE_HOMING);
		beacons.activate();

		while(opModeIsActive()) {
			Mat output = new Mat();
			boolean havePixelData = false;
			String beaconName = "";

			for(VuforiaTrackable beac : beacons) {
				OpenGLMatrix pose = ((VuforiaTrackableDefaultListener) beac.getListener()).getRawPose();

				if(pose != null) {
					beaconName = beac.getName();
					if(beac.getName().equals(lastScoredBeaconName))
						continue;

					Matrix34F rawPose = new Matrix34F();
					float[] poseData = Arrays.copyOfRange(pose.transposed().getData(), 0, 12);
					rawPose.setData(poseData);

					Vector2 center = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(0, 0, 0)));
					Vector2 upperLeft = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(-100, 260, 0))); // -127, 92, 0
					Vector2 upperRight = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(100, 260, 0))); // 127, 92, 0
					Vector2 lowerLeft = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(-100, 142, 0))); // -127, -92, 0
					Vector2 lowerRight = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(100, 142, 0))); // 127, -92, 0

					centeredPos = center.y;

					if(vuforia.rgb == null)
						continue;

					Bitmap bm = Bitmap.createBitmap(vuforia.rgb.getWidth(), vuforia.rgb.getHeight(), Bitmap.Config.RGB_565);
					bm.copyPixelsFromBuffer(vuforia.rgb.getPixels());

					Vector2 start = new Vector2(Math.max(0, Math.min(upperLeft.x, lowerLeft.x)), Math.min(bm.getHeight() - 1, Math.max(0, Math.min(upperLeft.y, upperRight.y))));
					Vector2 end = new Vector2(Math.min(bm.getWidth(), Math.max(lowerRight.x, upperRight.x)), Math.min(bm.getHeight() - 1, Math.max(0, Math.max(lowerLeft.y, lowerRight.y))));

					if(end.x - start.x == 0 || end.y - start.y == 0)
						continue;

					try {
						Bitmap a = Bitmap.createBitmap(bm, start.x, start.y, end.x, end.y);

						Bitmap resizedbitmap = DesignosaursUtils.resize(a, a.getWidth() / 2, a.getHeight() / 2);
						resizedbitmap = DesignosaursUtils.rotate(resizedbitmap, 90);

						if(OBFUSCATE_MIDDLE) {
							Canvas canvas = new Canvas(resizedbitmap);
							Paint paint = new Paint();
							paint.setColor(Color.WHITE);

							canvas.drawRect(resizedbitmap.getWidth() * 12 / 30, 0, resizedbitmap.getWidth() * 17 / 30, resizedbitmap.getHeight(), paint);
						}

						//FtcRobotControllerActivity.simpleController.setImage(resizedbitmap);
						//FtcRobotControllerActivity.simpleController.setImage2(bm);

						Utils.bitmapToMat(resizedbitmap, output);

						havePixelData = true;
					} catch(Exception e) {
						//e.printStackTrace();
					}
				}
			}

			switch(autonomousState) {
				case STATE_INITIAL_POSITIONING:
					updateRunningState("Accelerating...");
					robot.goStraight(0.8, FAST_DRIVE_POWER);

					updateRunningState("Initial turn...");
					robot.turn(60, TURN_POWER);

					updateRunningState("Secondary move...");
					robot.goStraight(2.8, FAST_DRIVE_POWER);

					updateRunningState("Secondary turn...");
					robot.turn(-50, TURN_POWER);

					setState(STATE_SEARCHING);
				break;
				case STATE_SEARCHING:
					if(Math.abs(getRelativePosition()) < BEACON_ALIGNMENT_TOLERANCE) {
						stateMessage = "Analysing beacon data...";

						robot.setDrivePower(0);

						if(havePixelData) {
							stateMessage = "Beacon found!";

							BeaconColorResult lastBeaconColor = beaconProcessor.process(System.currentTimeMillis(), output, SAVE_IMAGES).getResult();
							BeaconColorResult.BeaconColor targetColor = (teamColor == TEAM_RED ? BeaconColorResult.BeaconColor.RED : BeaconColorResult.BeaconColor.BLUE);

							Log.i(TAG, "*** BEACON FOUND ***");
							Log.i(TAG, "Target color: " + (targetColor == BeaconColorResult.BeaconColor.BLUE ? "Blue" : "Red"));

							robot.resetDriveEncoders();
							targetSide = lastBeaconColor.getLeftColor() == targetColor ? SIDE_LEFT : SIDE_RIGHT;
							robot.setDrivePower(-DRIVE_POWER);

							setState(STATE_ALIGNING_WITH_BEACON);
						} else {
							robot.goStraight(-0.5, DRIVE_POWER);
							stateMessage = "Waiting for conversion...";

							Log.i(TAG, "Beacon seen, but unable to pass data to OpenCV.");
						}
					} else if(Math.abs(getRelativePosition()) < SLOW_DOWN_AT) {
						stateMessage = "Beacon seen, centering (" + String.valueOf(getRelativePosition()) + ")...";
						robot.resetDriveEncoders();

						if(getRelativePosition() > 0 && getRelativePosition() != Integer.MAX_VALUE)
							robot.setDrivePower(DRIVE_POWER * -0.5);
						else
							robot.setDrivePower(DRIVE_POWER * 0.5);
					}
				break;
				case STATE_ALIGNING_WITH_BEACON:
					stateMessage = "Positioning to deploy placer...";

					if(Math.max(Math.abs(robot.getAdjustedEncoderPosition(robot.leftMotor)), Math.abs(robot.getAdjustedEncoderPosition(robot.rightMotor))) >= (targetSide == SIDE_LEFT ? 900 : 75)) {
						Log.i(TAG, "//// DEPLOYING ////");

						robot.setDrivePower(0);
						buttonPusherManager.setStatus(ButtonPusherManager.STATE_SCORING);

						setState(STATE_WAITING_FOR_PLACER);
					}
				break;
				case STATE_WAITING_FOR_PLACER:
					stateMessage = "Waiting for placer to deploy.";

					if((buttonPusherManager.getStatus() != ButtonPusherManager.STATE_SCORING && buttonPusherManager.getTicksInState() >= 10) || buttonPusherManager.getStatus() == ButtonPusherManager.STATE_AT_BASE) {
						lastScoredBeaconName = beaconName;
						beaconsFound++;
						centeredPos = Integer.MAX_VALUE;

						if(beaconsFound == 2)
							setState(STATE_FINISHED);
						else {
							robot.turn(1, TURN_POWER);
							robot.goStraight(2.3, FAST_DRIVE_POWER);
							robot.turn(-1, TURN_POWER);

							setState(STATE_SEARCHING);
						}
					}
			}

			ticksInState++;
			updateRunningState();
			robot.waitForTick(20);
		}
	}

	@Override
	public void onStop() {
		Log.i(TAG, "*** SHUTTING DOWN ***");
		buttonPusherManager.shutdown();
		robot.shutdown();
	}

	private void setState(byte newState) {
		Log.i(TAG, "*** SWITCHING STATES ***");
		Log.i(TAG, "New state: " + String.valueOf(newState));
		Log.i(TAG, "Time in previous state: " + String.valueOf(ticksInState));

		switch(newState) {
			case STATE_SEARCHING:
				robot.setDrivePower(DRIVE_POWER);
				robot.leftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
				robot.rightMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
				stateMessage = "Searching for beacon...";
			break;
			case STATE_WAITING_FOR_PLACER:
				robot.leftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
				robot.rightMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
			break;
			case STATE_FINISHED:
				stateMessage = "Done.";
		}

		autonomousState = newState;
		ticksInState = 0;
	}

	private String getStateMessage() {
		switch(autonomousState) {
			case STATE_INITIAL_POSITIONING:
				return "initial positioning";
			case STATE_SEARCHING:
				return "searching";
			case STATE_ALIGNING_WITH_BEACON:
				return "aligning with desired color";
			case STATE_WAITING_FOR_PLACER:
				return "waiting for placer";
			case STATE_FINISHED:
				return "finished";
		}

		return "unknown";
	}

	private String teamColorToString() {
		switch(teamColor) {
			case TEAM_UNSELECTED:
				return "** UNSELECTED **";
			case TEAM_BLUE:
				return "BLUE";
			case TEAM_RED:
				return "RED";
		}

		return "unknown";
	}

	private int getRelativePosition() {
		return centeredPos - 350;
	}
}