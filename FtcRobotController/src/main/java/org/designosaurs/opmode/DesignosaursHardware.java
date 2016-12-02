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
import java.util.List;

class DesignosaursHardware {
	static final boolean hardwareEnabled = true;

	DcMotor leftMotor = null;
	DcMotor rightMotor = null;
	DcMotor buttonPusher = null;
	AdafruitBNO055IMU imu = null;

	static final int COUNTS_PER_REVOLUTION = 2880;
	static final int COUNTS_PER_ROTATION = 7715;
	static final int COUNTS_PER_FOOT = 2128;

	private ElapsedTime period = new ElapsedTime();
	private SparseIntArray encoderOffsets = new SparseIntArray(3);

	private Orientation orientation;
	private final String TAG = "DesignosaursHardware";
	private DecimalFormat decimalFormat = new DecimalFormat("#.00");

	DesignosaursHardware() {}

	void init(HardwareMap hwMap) {
		if(hardwareEnabled) {
			leftMotor = hwMap.dcMotor.get("left");
			rightMotor = hwMap.dcMotor.get("right");
			buttonPusher = hwMap.dcMotor.get("buttonPusher");
			imu = new AdafruitBNO055IMU(hwMap.i2cDeviceSynch.get("imu"));

			leftMotor.setDirection(DcMotor.Direction.REVERSE);
			leftMotor.setPower(0);
			rightMotor.setPower(0);

			buttonPusher.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
			buttonPusher.setDirection(DcMotor.Direction.REVERSE);
			buttonPusher.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

			encoderOffsets.put(leftMotor.hashCode(), 0);
			encoderOffsets.put(rightMotor.hashCode(), 0);
			encoderOffsets.put(buttonPusher.hashCode(), 0);
			resetDriveEncoders();

			BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
			parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
			parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
			//parameters.calibrationDataFile = "AdafruitIMUCalibration.json"; // see the calibration sample opmode
			parameters.loggingEnabled = false;
			parameters.temperatureUnit = BNO055IMU.TempUnit.FARENHEIT;

			imu.initialize(parameters);

			Log.i(TAG, "Hardware initialized.");
			return;
		}

		Log.i(TAG, "Skipping hardware initialization (test mode).");
	}

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

	private double getDistance() {
		return (double) (Math.max(getAdjustedEncoderPosition(leftMotor), getAdjustedEncoderPosition(rightMotor))) / COUNTS_PER_FOOT;
	}

	void setDrivePower(double power) {
		if(hardwareEnabled) {
			leftMotor.setPower(power);
			rightMotor.setPower(power);
		}
	}

	void setButtonPusherPower(double power) {
		if(hardwareEnabled)
			buttonPusher.setPower(power);
	}

	void goStraight(double feet, double power) {
		Log.i(TAG, "Going for " + decimalFormat.format(feet) + " ft at " + (power * 100) + "% power...");

		feet = Math.abs(feet);

		setDrivePower(power);
		resetDriveEncoders();

		while(Math.abs(getDistance()) < feet)
			try {
				Thread.sleep(1);
			} catch(Exception e) {
				return;
			}

		setDrivePower(0);
		resetDriveEncoders();

		Log.i(TAG, "Done.");
	}

	void turn(double degrees, double power) {
		DcMotor primaryMotor, secondaryMotor;
		double targetDegrees;

		Log.i(TAG, "Starting pivot to " + decimalFormat.format(degrees) + " deg at " + (power * 100) + "% power...");

		resetEncoder(leftMotor);
		resetEncoder(rightMotor);
		updateOrientation();

		primaryMotor = degrees > 0 ? rightMotor : leftMotor;
		secondaryMotor = degrees > 0 ? leftMotor : rightMotor;

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

		while(degrees > 0 ? getHeading() <= targetDegrees : getHeading() >= targetDegrees)
			try {
				Thread.sleep(15);
				updateOrientation();
			} catch(Exception e) {
				return;
			}

		setDrivePower(0);
		resetDriveEncoders();

		Log.i(TAG, "Done, new rotation is " + decimalFormat.format(getHeading()) + ".");
	}

	void emergencyStop() {
		try {
			Thread.sleep(Integer.MAX_VALUE);
		} catch(InterruptedException e) {
			shutdown();
		}
	}

	/*** Encoders ***/

	void returnToZero() {
		updateOrientation();

		if(getHeading() > 180)
			turn(360 - getHeading(), 0.4);
		else
			turn(-getHeading(), 0.4);
	}

	void resetDriveEncoders() {
		resetEncoder(leftMotor);
		resetEncoder(rightMotor);
	}

	int getAdjustedEncoderPosition(DcMotor motor) {
		if(motor != null)
			return motor.getCurrentPosition() - encoderOffsets.get(motor.hashCode());

		return 0;
	}

	void resetEncoder(DcMotor motor) {
		if(motor != null)
			encoderOffsets.put(motor.hashCode(), motor.getCurrentPosition());
	}

	/*** Sensors ***/

	void startOrientationTracking() {
		if(imu != null)
			imu.startAccelerationIntegration(new Position(), new Velocity(), 1000);
	}

	private void updateOrientation() {
		if(imu != null)
			orientation = imu.getAngularOrientation();
	}

	String getCalibrationStatus() {
		return imu == null ? "disabled" : imu.getCalibrationStatus().toString();
	}

	float getHeading() {
		return imu == null ? 0 : -orientation.firstAngle;
	}

	void shutdown() {
		imu.stopAccelerationIntegration();
		setDrivePower(0);
	}
}