package org.designosaurs.opmode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.qualcomm.ftcrobotcontroller.R;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.vuforia.Matrix34F;
import com.vuforia.Tool;
import com.vuforia.Vec3F;

import org.designosaurs.Vector2;
import org.designosaurs.VuforiaLocalizerImplSubclass;
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

	// Whether to block out the garbage data in the center of the beacon, assuming that it's not taped
	// The field setup guide says it should be taped on the inside, I have yet to see one configured as such
	private final boolean OBFUSCATE_MIDDLE = true;
	private final String VUFORIA_LICENCE_KEY = "ATwI0oz/////AAAAGe9HyiYVEU6pmTFAb65tOfUrioTxlZtITHRLN1h3wllaw67kJsUOHwPVDsCN0vxiKy/9Qi9NnjpkVfUnn0gwIHyKJgTYkG7+dCaJtFJlY94qa1YPCy0y4rwhVQFkDkcaCiNoiS7ZSU5KLeIABF4Gvz9qYwJJtwxWGp4fbjyu+arTOUw160+Fg5XMjoftS8FAQPx4wF33sVdGw+CYX0fHdwQzOyN0PpIwBQ9xvb8e1c76FoHF0YUZyV/q0XeR97nRj1TfnesPc+v7Z72SEDCXAAdVVS6L9u/mVAxq4zTaXsdGcVsqHeaouoGmQ/1Ey/YYShqHaRZXWwC4GsgaxO9tCkWNH+hTjFZA2pgvKVl5HmLR";

	/* Configuration */
	private static final double FAST_DRIVE_POWER = 0.85;
	private static final double TURN_POWER = 0.35;
	private static final double DRIVE_POWER = 0.25;
	private static final double SLOW_DOWN_AT = 3000;
	private static final int BEACON_ALIGNMENT_TOLERANCE = 100;
	public static final boolean SAVE_IMAGES = false;

	/* State Machine */
	private final byte STATE_SHOOTING = 0;
	private final byte STATE_INITIAL_POSITIONING = 1;
	private final byte STATE_SEARCHING = 2;
	private final byte STATE_ALIGNING_WITH_BEACON = 3;
	private final byte STATE_WAITING_FOR_PLACER = 4;
	private final byte STATE_FINISHED = 5;

	/* Team Colors */
	private final byte TEAM_UNSELECTED = 0;
	private final byte TEAM_BLUE = 1;
	private final byte TEAM_RED = 2;

	/* Beacon Sides */
	private final byte SIDE_LEFT = 0;
	private final byte SIDE_RIGHT = 1;

	/* Current State */
	private byte autonomousState = STATE_SHOOTING;
	private int ticksInState = 0;
	private String stateMessage = "Starting...";
	private byte beaconsFound = 0;

	private static String TAG = "DesignosaursAuto";
	private int centeredPos = Integer.MAX_VALUE;
	private long lastTelemetryUpdate = 0;
	private byte teamColor = TEAM_UNSELECTED;
	private byte targetSide = SIDE_LEFT;
	private String lastScoredBeaconName = "";
	private Context appContext;
	private Mat output = null;

	// Interpret the initialization string returned by the IMU
	private String getIMUState() {
		if(robot.getCalibrationStatus().contains(" "))
			switch(Integer.valueOf(robot.getCalibrationStatus().split("\\s+")[1].substring(1, 2))) {
				case 0:
					return "disabled";
				case 1:
					return "Initializing...";
				case 2:
					return "Calibrating...";
				case 3:
					return "Ready!";
			}

		return robot.getCalibrationStatus();
	}

	// Write pre-run state to telemetry (driver station)
	private void setInitState(String state) {
		telemetry.clear();
		telemetry.addLine("== Designosaurs 2017 ==");
		telemetry.addLine(state);
		telemetry.addLine("");
		telemetry.addLine("IMU: " + getIMUState());
		telemetry.addLine("Team color: " + teamColorToString());
		telemetry.update();
	}

	// Write running state to telemetry (driver station)
	private void updateRunningState() {
		if(System.currentTimeMillis() - lastTelemetryUpdate < 250)
			return;

		lastTelemetryUpdate = System.currentTimeMillis();
		telemetry.clear();
		telemetry.addLine(stateMessage);
		telemetry.addLine("");
		telemetry.addLine("State: " + getStateMessage());
		telemetry.addLine("Time in state: " + String.valueOf(ticksInState));
		telemetry.addLine("Button pusher: " + buttonPusherManager.getStatusMessage());
		telemetry.addLine("Beacons scored: " + String.valueOf(beaconsFound));
		telemetry.update();
	}

	// Shortcut method
	private void updateRunningState(String newStateMessage) {
		stateMessage = newStateMessage;

		updateRunningState();
	}

	// Looped before start because IMU initializes asynchronously
	private void updateTeamColor() {
		if(gamepad1.x)
			teamColor = TEAM_BLUE;

		if(gamepad1.b)
			teamColor = TEAM_RED;

		if(teamColor != TEAM_UNSELECTED)
			setInitState("Ready!");
	}

	// Uses the separate OpenCV Manager app to shave a bit off the app size
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(appContext) {
		@Override
		public void onManagerConnected(int status) {
			switch(status) {
				case LoaderCallbackInterface.SUCCESS:
					Log.i(TAG, "OpenCV loaded successfully!");
					output = new Mat();
				break;
				default:
					Log.i(TAG, "OpenCV load failure.");
			}
		}
	};


	// This is where the main logic block lives
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

		// Vuforia tracks the images, we'll call them beacons
		VuforiaTrackables beacons = vuforia.loadTrackablesFromAsset("FTC_2016-17");
		beacons.get(0).setName("wheels");
		beacons.get(1).setName("tools");
		beacons.get(2).setName("legos");
		beacons.get(3).setName("gears");

		setInitState("Initializing OpenCV...");
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, hardwareMap.appContext, mLoaderCallback);

		setInitState("Select team color using the gamepad.");

		while(!isStarted() && !isStopRequested()) {
			updateTeamColor();

			robot.waitForTick(25);
		}

		if(DesignosaursHardware.hardwareEnabled) {
			buttonPusherManager.start();
			buttonPusherManager.setStatus(ButtonPusherManager.STATE_HOMING);
		}

		beacons.activate();

		while(opModeIsActive()) {
			boolean havePixelData = false;
			String beaconName = "";

			// Detect and process images
			for(VuforiaTrackable beac : beacons) {
				OpenGLMatrix pose = ((VuforiaTrackableDefaultListener) beac.getListener()).getRawPose();

				if(pose != null) {
					beaconName = beac.getName();

					if(beac.getName().equals(lastScoredBeaconName)) // fixes seeing the first beacon and wanting to go back to it
						continue;

					Matrix34F rawPose = new Matrix34F();
					float[] poseData = Arrays.copyOfRange(pose.transposed().getData(), 0, 12);
					rawPose.setData(poseData);

					Vector2 center = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(0, 0, 0)));
					Vector2 upperLeft = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(-100, 260, 0))); // -127, 92, 0
					Vector2 upperRight = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(100, 260, 0))); // 127, 92, 0
					Vector2 lowerLeft = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(-100, 142, 0))); // -127, -92, 0
					Vector2 lowerRight = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(100, 142, 0))); // 127, -92, 0

					centeredPos = center.y; // drive routines align based on this

					if(vuforia.rgb == null)
						continue;

					Bitmap bm = Bitmap.createBitmap(vuforia.rgb.getWidth(), vuforia.rgb.getHeight(), Bitmap.Config.RGB_565);
					bm.copyPixelsFromBuffer(vuforia.rgb.getPixels());

					Vector2 start = new Vector2(Math.max(0, Math.min(upperLeft.x, lowerLeft.x)), Math.min(bm.getHeight() - 1, Math.max(0, Math.min(upperLeft.y, upperRight.y))));
					Vector2 end = new Vector2(Math.min(bm.getWidth(), Math.max(lowerRight.x, upperRight.x)), Math.min(bm.getHeight() - 1, Math.max(0, Math.max(lowerLeft.y, lowerRight.y))));

					if(end.x - start.x == 0 || end.y - start.y == 0)
						continue;

					try {
						// Pass the cropped portion of the detected image to OpenCV:
						Bitmap a = Bitmap.createBitmap(bm, start.x, start.y, end.x, end.y);

						Bitmap resizedbitmap = DesignosaursUtils.resize(a, a.getWidth() / 2, a.getHeight() / 2);
						resizedbitmap = DesignosaursUtils.rotate(resizedbitmap, 90);

						if(OBFUSCATE_MIDDLE) {
							Canvas canvas = new Canvas(resizedbitmap);
							Paint paint = new Paint();
							paint.setColor(Color.WHITE);

							canvas.drawRect(resizedbitmap.getWidth() * 12 / 30, 0, resizedbitmap.getWidth() * 17 / 30, resizedbitmap.getHeight(), paint);
						}

						// TODO: Fix web debugging view
						//FtcRobotControllerActivity.simpleController.setImage(resizedbitmap);
						//FtcRobotControllerActivity.simpleController.setImage2(bm);

						Utils.bitmapToMat(resizedbitmap, output);

						havePixelData = true;
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}

			switch(autonomousState) {
				case STATE_SHOOTING:
					robot.shooter.setPower(-0.8);
					if(ticksInState >= 150) {
						robot.shooter.setPower(0);
						setState(STATE_INITIAL_POSITIONING);
					}
				break;
				case STATE_INITIAL_POSITIONING:
					if(teamColor == TEAM_BLUE) {
						robot.rightMotor.setDirection(DcMotor.Direction.REVERSE);
						robot.leftMotor.setDirection(DcMotor.Direction.FORWARD);
					}

					updateRunningState("Accelerating...");
					robot.accel(0.8, 0.4);

					if(teamColor == TEAM_RED) {
						updateRunningState("Initial turn...");
						robot.turn(-35, 0.3);

						updateRunningState("Secondary move...");
						robot.accel(0.5, FAST_DRIVE_POWER);
						robot.goStraight(1.7, FAST_DRIVE_POWER);
						robot.decel(0.5, 0.1);

						updateRunningState("Secondary turn...");
						robot.turn(34, TURN_POWER);
					} else {
						updateRunningState("Initial turn...");
						robot.turn(40, 0.3);

						updateRunningState("Secondary move...");
						robot.accel(0.5, FAST_DRIVE_POWER);
						robot.goStraight(1.75, FAST_DRIVE_POWER);
						robot.decel(0.5, 0.1);

						updateRunningState("Secondary turn...");
						robot.turn(-39, TURN_POWER);
					}

					setState(STATE_SEARCHING);
				break;
				case STATE_SEARCHING:
					if(Math.abs(getRelativePosition()) < BEACON_ALIGNMENT_TOLERANCE) {
						stateMessage = "Analysing beacon data...";

						if(havePixelData) {
							robot.setDrivePower(0);
							stateMessage = "Beacon found!";

							BeaconColorResult lastBeaconColor = beaconProcessor.process(System.currentTimeMillis(), output, SAVE_IMAGES).getResult();
							BeaconColorResult.BeaconColor targetColor = (teamColor == TEAM_RED ? BeaconColorResult.BeaconColor.RED : BeaconColorResult.BeaconColor.BLUE);

							Log.i(TAG, "*** BEACON FOUND ***");
							Log.i(TAG, "Target color: " + (targetColor == BeaconColorResult.BeaconColor.BLUE ? "Blue" : "Red"));

							robot.resetDriveEncoders();
							targetSide = lastBeaconColor.getLeftColor() == targetColor ? SIDE_LEFT : SIDE_RIGHT;

							setState(STATE_ALIGNING_WITH_BEACON);
						} else {
							// Slow down, assuming the camera can't see the whole beacon yet
							robot.setDrivePower(0.2);
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
							robot.returnToZero();
							robot.turn(-1, TURN_POWER);
							robot.accel(0.5, FAST_DRIVE_POWER);
							robot.goStraight(targetSide == SIDE_LEFT ? 2 : 1.5, FAST_DRIVE_POWER);
							robot.decel(0.5, DRIVE_POWER);
							robot.returnToZero();

							setState(STATE_SEARCHING);
						}
					}
			}

			ticksInState++;
			updateRunningState();
			robot.waitForTick(20);
		}
	}

	// DesignosaursOpMode created so this gets called
	@Override
	public void onStop() {
		Log.i(TAG, "*** SHUTTING DOWN ***");
		buttonPusherManager.shutdown();
		robot.shutdown();
	}

	// Please use this instead of directly updating state machine
	private void setState(byte newState) {
		Log.i(TAG, "*** SWITCHING STATES ***");
		Log.i(TAG, "New state: " + String.valueOf(newState));
		Log.i(TAG, "Time in previous state: " + String.valueOf(ticksInState));

		switch(newState) {
			case STATE_SEARCHING:
				robot.setDrivePower(DRIVE_POWER);
				robot.startOrientationTracking();

				stateMessage = "Searching for beacon...";
			break;
			case STATE_FINISHED:
				stateMessage = "Done.";
		}

		autonomousState = newState;
		ticksInState = 0;
	}

	// Gets written to the driver station to explain the current state
	private String getStateMessage() {
		switch(autonomousState) {
			case STATE_SHOOTING:
				return "shooting";
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

	// Gets written to the driver station to represent what's selected on the joysticks
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

	// Offset here represents how far the camera is from the button pusher
	private int getRelativePosition() {
		return centeredPos - 350;
	}
}