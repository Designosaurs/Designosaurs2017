package org.designosaurs;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

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
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.Arrays;

import ftc.vision.BeaconProcessor;
import ftc.vision.ImageProcessorResult;

@Autonomous(name = "Designosaurs Autonomous", group = "Auto")
public class DesignosaursAuto extends LinearOpMode {
	private DesignosaursHardware robot = new DesignosaursHardware();
	private BeaconProcessor beaconProcessor = new BeaconProcessor();

	private final boolean OBFUSCATE_MIDDLE = true;

	private int centeredPos = Integer.MAX_VALUE;

	private final String VUFORIA_LICENCE_KEY = "ATwI0oz/////AAAAGe9HyiYVEU6pmTFAb65tOfUrioTxlZtITHRLN1h3wllaw67kJsUOHwPVDsCN0vxiKy/9Qi9NnjpkVfUnn0gwIHyKJgTYkG7+dCaJtFJlY94qa1YPCy0y4rwhVQFkDkcaCiNoiS7ZSU5KLeIABF4Gvz9qYwJJtwxWGp4fbjyu+arTOUw160+Fg5XMjoftS8FAQPx4wF33sVdGw+CYX0fHdwQzOyN0PpIwBQ9xvb8e1c76FoHF0YUZyV/q0XeR97nRj1TfnesPc+v7Z72SEDCXAAdVVS6L9u/mVAxq4zTaXsdGcVsqHeaouoGmQ/1Ey/YYShqHaRZXWwC4GsgaxO9tCkWNH+hTjFZA2pgvKVl5HmLR";

	private static final double DRIVE_POWER = 0.75;
	private static final double SLOW_DOWN_AT = 3000;
	private static final double BUTTON_PUSHER_POWER = 0.75;
	private static final double BUTTON_PUSHER_MOVEMENT_THRESHOLD = 10;
	private static final double BUTTON_PUSHER_TARGET_IDLE_POSITION = 3 * DesignosaursHardware.COUNTS_PER_REVOLUTION;
	private static final double BUTTON_PUSHER_THRESHOLD = DesignosaursHardware.COUNTS_PER_REVOLUTION / 10;
	private static final int BEACON_ALIGNMENT_TOLERANCE = 600;

	/* State Machine Options */
	private final byte STATE_RETRACTING_PLACER = 0;
	private final byte STATE_SEARCHING = 1;
	private final byte STATE_WAITING_FOR_PLACER = 2;
	private final byte STATE_ROTATING_TOWARDS_GOAL = 3;
	private final byte STATE_DRIVING_TOWARDS_GOAL = 4;
	private final byte STATE_SCORING_IN_GOAL = 5;
	private final byte STATE_DRIVING_TO_RAMP = 6;
	private final byte STATE_FINISHED = 7;

	/* Current State */
	private byte autonomousState = STATE_SEARCHING;
	private double lastButtonPusherPosition = 0;
	private int ticksInState = 0;
	private byte beaconsFound = 0;

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

		beacons.activate();

		while(opModeIsActive()) {
			boolean isFirstRun = true;

			for(VuforiaTrackable beac : beacons) {
				OpenGLMatrix pose = ((VuforiaTrackableDefaultListener) beac.getListener()).getRawPose();

				if(pose != null) {
					Matrix34F rawPose = new Matrix34F();
					float[] poseData = Arrays.copyOfRange(pose.transposed().getData(), 0, 12);
					rawPose.setData(poseData);

					Vector2 center = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(0, 0, 0)));
					Vector2 upperLeft = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(-100, 260, 0)));//-127, 92, 0
					Vector2 upperRight = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(100, 260, 0)));//127, 92, 0
					Vector2 lowerLeft = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(-100, 142, 0)));//-127, -92, 0
					Vector2 lowerRight = new Vector2(Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(100, 142, 0)));//127, -92, 0

					if(vuforia.rgb != null) {
						Bitmap bm = Bitmap.createBitmap(vuforia.rgb.getWidth(), vuforia.rgb.getHeight(), Bitmap.Config.RGB_565);
						bm.copyPixelsFromBuffer(vuforia.rgb.getPixels());

						centeredPos = center.X;
						isFirstRun = false;

						Vector2 start = new Vector2(Math.max(0, Math.min(upperLeft.X, lowerLeft.X)), Math.min(bm.getHeight()-1,Math.max(0, Math.min(upperLeft.Y, upperRight.Y))));
						Vector2 end = new Vector2(Math.max(0, Math.max(lowerRight.X, upperRight.X)), Math.min(bm.getHeight()-1,Math.max(0, Math.max(lowerLeft.Y, lowerRight.Y))));

						try {
							Bitmap a = Bitmap.createBitmap(bm, start.X, start.Y, end.X, end.Y);

							Bitmap resizedbitmap = DesignosaursUtils.resize(a, a.getWidth() / 2, a.getHeight() / 2);
							resizedbitmap = DesignosaursUtils.rotate(resizedbitmap, 90);

							if(OBFUSCATE_MIDDLE) {
								Canvas canvas = new Canvas(resizedbitmap);
								Paint paint = new Paint();
								paint.setColor(Color.WHITE);

								canvas.drawRect(resizedbitmap.getWidth() * 12 / 30, 0, resizedbitmap.getWidth() * 17 / 30, resizedbitmap.getHeight(), paint);
							}

							Mat output = new Mat();
							Utils.bitmapToMat(resizedbitmap, output);

							ImageProcessorResult beaconResult = beaconProcessor.process(System.currentTimeMillis(), output, false);
							FtcRobotControllerActivity.simpleController.setImage(resizedbitmap);
							FtcRobotControllerActivity.simpleController.setImage2(bm);
						} catch(IllegalArgumentException e) {
							e.printStackTrace();
						}
					}
				}
			}

			if(isFirstRun)
				centeredPos = Integer.MAX_VALUE;

			telemetry.addData("state", autonomousState);
			telemetry.update();

			double buttonPusherPositionDelta = robot.buttonPusher.getCurrentPosition() - BUTTON_PUSHER_TARGET_IDLE_POSITION;
			double buttonPusherMovementSinceLastTick = robot.buttonPusher.getCurrentPosition() - lastButtonPusherPosition;
			lastButtonPusherPosition = robot.buttonPusher.getCurrentPosition();

			if(autonomousState != STATE_RETRACTING_PLACER && autonomousState != STATE_WAITING_FOR_PLACER)
				if(Math.abs(buttonPusherPositionDelta) > BUTTON_PUSHER_THRESHOLD)
					robot.buttonPusher.setPower(buttonPusherPositionDelta > 0 ? -BUTTON_PUSHER_POWER : BUTTON_PUSHER_POWER);
				else
					robot.buttonPusher.setPower(0);

			if(autonomousState <= STATE_SEARCHING)
				robot.setDrivePower(getRelativePosition() < SLOW_DOWN_AT ? DRIVE_POWER * 0.5 : DRIVE_POWER);

			switch(autonomousState) {
				case STATE_RETRACTING_PLACER:
					if((Math.abs(buttonPusherMovementSinceLastTick) < BUTTON_PUSHER_MOVEMENT_THRESHOLD) && ticksInState > 30)
						setState(STATE_SEARCHING);
				break;
				case STATE_SEARCHING:
					if(ticksInState < 50)
						continue;

					if(Math.abs(getRelativePosition()) < BEACON_ALIGNMENT_TOLERANCE) {
						robot.setDrivePower(0);
						robot.buttonPusher.setPower(BUTTON_PUSHER_POWER);

						setState(STATE_WAITING_FOR_PLACER);
					} else {
						if(getRelativePosition() > 0) {
							robot.setDrivePower(Math.abs(getRelativePosition()) < SLOW_DOWN_AT ? DRIVE_POWER * 0.1 : DRIVE_POWER);
						} else {
							robot.setDrivePower(Math.abs(getRelativePosition()) < SLOW_DOWN_AT ? -(DRIVE_POWER * 0.1) : -DRIVE_POWER);
						}
					}
				break;
				case STATE_WAITING_FOR_PLACER:
					if((Math.abs(buttonPusherMovementSinceLastTick) < BUTTON_PUSHER_MOVEMENT_THRESHOLD) && ticksInState > 30) {
						beaconsFound++;

						if(beaconsFound < 2)
							setState(STATE_SEARCHING);
						else
							setState(STATE_ROTATING_TOWARDS_GOAL);
					}
				break;
				case STATE_ROTATING_TOWARDS_GOAL:
				break;
			}

			ticksInState++;
		}
	}

	private void setState(byte newState) {
		autonomousState = newState;
		ticksInState = 0;
	}

	private int getRelativePosition() {
		return centeredPos - 640;
	}
}