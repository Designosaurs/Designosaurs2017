package org.designosaurs.opmode;

import android.util.Log;
import android.util.SparseIntArray;

import com.qualcomm.hardware.adafruit.AdafruitBNO055IMU;
import com.qualcomm.hardware.adafruit.BNO055IMU;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.Velocity;

import java.text.DecimalFormat;

class DesignosaursHardware {
	// Disable this to run the robot without sensors/motors, for testing image recognition
	static boolean hardwareEnabled = true;
	static boolean imuEnabled = true;

	/* Hardware goes here */
	DcMotor leftMotor = null;
	DcMotor rightMotor = null;
	DcMotor buttonPusher = null;
	DcMotor shooter = null;
	DcMotor lift = null;
	AdafruitBNO055IMU imu = null;

	// Tune these when gear ratio changes
	static final int COUNTS_PER_REVOLUTION = 2880;
	static final int COUNTS_PER_FOOT = 2128;

	// Threshold for when a turn is considered done
	static final double TURN_TOLERANCE = 1;

	private ElapsedTime period = new ElapsedTime();
	private SparseIntArray encoderOffsets = new SparseIntArray(3);

	private Orientation orientation;
	private final String TAG = "DesignosaursHardware";
	private DecimalFormat decimalFormat = new DecimalFormat("#.00");
	boolean isTeleOp = false;
	boolean hasInitializedImu = false;
	DesignosaursAuto parent = null;

	private long timer = 0;

	DesignosaursHardware() {}

	DesignosaursHardware(boolean isTeleOp) {
		this.isTeleOp = isTeleOp;

		if(isTeleOp)
			imuEnabled = false;
	}

	DesignosaursHardware(DesignosaursAuto parent) {
		this.parent = parent;
	}

	// Called in initialization, before start. Does not initialize IMU.
	void init(HardwareMap hwMap) {
		if(hardwareEnabled) {
			leftMotor = hwMap.dcMotor.get("left");
			rightMotor = hwMap.dcMotor.get("right");
			buttonPusher = hwMap.dcMotor.get("buttonPusher");
			shooter = hwMap.dcMotor.get("shooter");
			lift = hwMap.dcMotor.get("lift");

			leftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
			rightMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
			leftMotor.setDirection(DcMotor.Direction.REVERSE);

			buttonPusher.setDirection(DcMotor.Direction.REVERSE);
			buttonPusher.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

			shooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
			shooter.setDirection(DcMotor.Direction.REVERSE);

			lift.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

			encoderOffsets.put(leftMotor.hashCode(), 0);
			encoderOffsets.put(rightMotor.hashCode(), 0);
			encoderOffsets.put(buttonPusher.hashCode(), 0);
			resetDriveEncoders();

			if(imuEnabled) {
				imu = new AdafruitBNO055IMU(hwMap.i2cDeviceSynch.get("imu"));

				BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
				parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
				parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
				//parameters.calibrationDataFile = "AdafruitIMUCalibration.json"; // see the calibration sample opmode
				parameters.loggingEnabled = false;
				parameters.temperatureUnit = BNO055IMU.TempUnit.FARENHEIT;

				imu.initialize(parameters);
			}

			Log.i(TAG, "Hardware initialized.");
			return;
		}

		Log.i(TAG, "Skipping hardware initialization (test mode).");
	}

	// Sleep thread for given number of milliseconds
	void waitForTick(long periodMs) {
		long remaining = periodMs - (long) period.milliseconds();

		if(remaining > 0)
			try {
				Thread.sleep(remaining);
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}

		period.reset();
	}

	/*** Drive ***/

	// Get distance in raw encoder counts
	private double getDistanceCounts() {
		return Math.max(getAdjustedEncoderPosition(leftMotor), getAdjustedEncoderPosition(rightMotor));
	}

	// Get current distance in feet based on the encoders, tolerant of one encoder failure
	private double getDistance() {
		return getDistanceCounts() / COUNTS_PER_FOOT;
	}

	// Shortcut function
	void setDrivePower(double power) {
		if(hardwareEnabled) {
			leftMotor.setPower(power);
			rightMotor.setPower(power);
		}
	}

	// Shortcut function
	void setButtonPusherPower(double power) {
		if(hardwareEnabled)
			buttonPusher.setPower(power);
	}

	// Shortcut function
	void setShooterPower(double power) {
		if(hardwareEnabled)
			shooter.setPower(power);
	}

	void goCounts(double distance, double power) {
		if(parent != null && parent.isStopRequested())
			return;

		setDrivePower(distance > 0 ? power : -power);

		distance = Math.abs(distance);
		resetDriveEncoders();

		while(Math.abs(getDistanceCounts()) < distance && (timer != 0 ? System.currentTimeMillis() > timer - 25000 : true))
			waitForTick(10);
	}

	// Move the robot forward for the given number of feet, based on max encoder (we've had encoders die).
	// Does not handle accel/decel, use those functions for that. Blocking.
	void goStraight(double feet, double power) {
		Log.i(TAG, "Going for " + decimalFormat.format(feet) + " ft at " + (power * 100) + "% power...");

		resetDriveEncoders();

		if(feet < 0) {
			setDrivePower(-power);

			feet = -feet;
		} else
			setDrivePower(power);

		long startedAt = System.currentTimeMillis();

		while(Math.abs(getDistance()) < feet && (timer != 0 ? System.currentTimeMillis() > timer - 25000 : true))
			try {
				Thread.sleep(1);
			} catch(Exception e) {
				return;
			}

		Log.i(TAG, "Done (took " + String.valueOf(System.currentTimeMillis() - startedAt) + "ms).");
	}

	void startTimer() {
		timer = System.currentTimeMillis();
	}

	// Pivot the robot, using IMU gyro values. Blocking.
	void turn(double degrees, double power) {
		if(parent != null && parent.isStopRequested())
			return;

		DcMotor primaryMotor, secondaryMotor;
		double targetDegrees;

		Log.i(TAG, "Starting pivot to " + decimalFormat.format(degrees) + " deg at " + (power * 100) + "% power...");

		resetDriveEncoders();
		updateOrientation();

		if(leftMotor.getDirection() == DcMotor.Direction.FORWARD) {
			primaryMotor = degrees > 0 ? rightMotor : leftMotor;
			secondaryMotor = degrees > 0 ? leftMotor : rightMotor;
		} else {
			primaryMotor = degrees > 0 ? leftMotor : rightMotor;
			secondaryMotor = degrees > 0 ? rightMotor : leftMotor;
		}

		Log.i(TAG, "Current rotation: " + decimalFormat.format(getHeading()));

		if(degrees > 0)
			if((getHeading() + degrees) > 360)
				targetDegrees = (getHeading() + degrees) - 360;
			else
				targetDegrees = getHeading() + degrees;
		else
			if((getHeading() + degrees) < 0)
				targetDegrees = (getHeading() + degrees) + 360;
			else
				targetDegrees = getHeading() + degrees;

		Log.i(TAG, "Target rotation: " + targetDegrees);

		primaryMotor.setPower(power);
		secondaryMotor.setPower(-power);

		while(Math.abs(getHeading() - targetDegrees) > TURN_TOLERANCE && (timer != 0 ? System.currentTimeMillis() > timer - 25000 : true))
			try {
				Thread.sleep(12);
				Log.i(TAG, getHeading() + ", " + targetDegrees);
				updateOrientation();
			} catch(Exception e) {
				return;
			}

		Log.i(TAG, "Done, new rotation is " + decimalFormat.format(getHeading()) + ".");
	}

	// Curve amount
	private static final double P_1 = 10/100;

	// Bezier curve implementation, see https://en.wikipedia.org/wiki/B%C3%A9zier_curve#Quadratic_B.C3.A9zier_curves
	private static double bezier(double t, double P_0, double P_2) {
		return (t >= 0 ? 1 : -1) * ((1 - t) * ((1 - t) * P_0 + t * P_1) + t * ((1 - t) * P_1 + t * P_2));
	}

	// Accelerates in a quadratic bezier curve from 0.3 -> power
	void accel(double feet, double power) {
		if(parent != null && parent.isStopRequested())
			return;

		double progress;

		resetDriveEncoders();
		while(getDistance() <= feet && (timer != 0 ? System.currentTimeMillis() > timer - 25000 : true)) {
			progress = getDistance() / feet;

			setDrivePower(bezier(progress, 0.3, power));
			waitForTick(15);
		}
	}

	// Decelerates from current max drive power to power
	void decel(double feet, double power) {
		if(parent != null && parent.isStopRequested())
			return;

		Log.i(TAG, "Decelerating - " + feet + " at " + (power * 100) + "% power...");
		double progress,
			   originalPower = Math.max(leftMotor.getPower(), rightMotor.getPower());

		long startedAt = System.currentTimeMillis();

		resetDriveEncoders();
		while(getDistance() <= feet && (timer != 0 ? System.currentTimeMillis() > timer - 25000 : true)) {
			progress = 1 - (getDistance() / feet);

			setDrivePower(bezier(progress, power, originalPower));
			waitForTick(10);
		}

		Log.i(TAG, "Done, took " + (System.currentTimeMillis() - startedAt) + "ms");
	}

	// Halts thread, great for debugging
	void emergencyStop() {
		try {
			while(true)
				Thread.sleep(10);

		} catch(InterruptedException e) {
			shutdown();
		}
	}

	/*** Encoders ***/

	// Re-center the robot to the gyro's original position at the start of the match
	void returnToZero() {
		Log.i(TAG, "Zeroing...");
		updateOrientation();

		if(getHeading() > 180)
			turn(360 - getHeading(), 0.2);
		else
			turn(-getHeading(), 0.2);

		setDrivePower(0);
	}

	// Shortcut method
	void resetDriveEncoders() {
		resetEncoder(leftMotor);
		resetEncoder(rightMotor);
	}

	// Compensates for the fact that calling setMode(RESET_ENCODER) causes future move commands to ignored
	int getAdjustedEncoderPosition(DcMotor motor) {
		if(motor != null)
			return motor.getCurrentPosition() - encoderOffsets.get(motor.hashCode());

		return 0;
	}

	// Resets current encoder offset based on the motor's unique identifier
	void resetEncoder(DcMotor motor) {
		if(motor != null)
			encoderOffsets.put(motor.hashCode(), motor.getCurrentPosition());
	}

	/*** Sensors ***/

	// Enables the Adafruit IMU gyro
	void startOrientationTracking(boolean force) {
		if(imu != null && (!hasInitializedImu || force)) {
			imu.startAccelerationIntegration(new Position(), new Velocity(), 1000);
			hasInitializedImu = true;
		}
	}

	void startOrientationTracking() {
		startOrientationTracking(false);
	}

	// Updates current gyro position from the I2C bus (expensive, avoid calling multiple times in a loop)
	private void updateOrientation() {
		if(imu != null)
			orientation = imu.getAngularOrientation();
	}

	// Gets the initialization status of the IMU unit, processed into text by DesignosaursAuto::getIMUState
	String getCalibrationStatus() {
		return imu == null ? "disabled" : imu.getCalibrationStatus().toString();
	}

	// Current gyro pos from Adafruit IMU
	float getHeading() {
		return imu == null ? 0 : -orientation.firstAngle;
	}

	// Called when opmode is shut down
	void shutdown() {
		setDrivePower(0);

		if(imu != null)
			imu.stopAccelerationIntegration();
	}
}