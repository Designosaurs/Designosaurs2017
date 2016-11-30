package org.designosaurs.opmode;

import android.content.Context;
import android.hardware.SensorManager;
import android.util.Log;
import android.util.SparseIntArray;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.designosaurs.opmode.orientationProvider.CalibratedGyroscopeProvider;
import org.designosaurs.opmode.orientationProvider.OrientationProvider;

class DesignosaursHardware {
	static final boolean hardwareEnabled = true;

	DcMotor leftMotor = null;
	DcMotor rightMotor = null;
	DcMotor buttonPusher = null;

	static final int COUNTS_PER_REVOLUTION = 2880;
	static final int COUNTS_PER_ROTATION = 7715;
	static final int COUNTS_PER_FOOT = 2128;

	private ElapsedTime period = new ElapsedTime();
	private SparseIntArray encoderOffsets = new SparseIntArray(3);

	private OrientationProvider currentOrientationProvider;
	private SensorManager sensorManager;
	private float[] orientation = new float[3];

	DesignosaursHardware() {}

	void init(HardwareMap hwMap) {
		if(hardwareEnabled) {
			leftMotor = hwMap.dcMotor.get("left");
			rightMotor = hwMap.dcMotor.get("right");
			buttonPusher = hwMap.dcMotor.get("buttonPusher");

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
		}

		sensorManager = (SensorManager) hwMap.appContext.getSystemService(Context.SENSOR_SERVICE);
		currentOrientationProvider = new CalibratedGyroscopeProvider(sensorManager);
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
	}

	void turn(double degrees, double power) {
		//degrees *= 0.845;

		DcMotor primaryMotor, secondaryMotor;

		resetEncoder(leftMotor);
		resetEncoder(rightMotor);

		primaryMotor = degrees > 0 ? rightMotor : leftMotor;
		secondaryMotor = degrees > 0 ? leftMotor : rightMotor;

		double targetDegrees;

		Log.i("DesignosaursAuto", "Desired rotation: " + degrees);
		Log.i("DesignosaursAuto", "Current rotation: " + getOrientation()[2]);

		if(degrees > 0)
			if((getOrientation()[2] + degrees) > 360)
				targetDegrees = (getOrientation()[2] + degrees) - 360;
			else
				targetDegrees = getOrientation()[2] + degrees;
		else
			if((getOrientation()[2] + degrees) < 0)
				targetDegrees = (getOrientation()[2] + degrees) + 360;
			else
				targetDegrees = getOrientation()[2] + degrees;

		Log.i("DesignosaursAuto", "Target rotation: " + targetDegrees);

		primaryMotor.setPower(power);
		secondaryMotor.setPower(-power);

		while(degrees > 0 ? getOrientation()[2] <= targetDegrees : getOrientation()[2] >= targetDegrees)
			try {
				Thread.sleep(5);
				Thread.yield();
			} catch(Exception e) {
				return;
			}

		Log.i("DesignosaursAuto", String.valueOf(getOrientation()[2]));

		setDrivePower(0);
		resetDriveEncoders();
	}

	void emergencyStop() {
		try {
			Thread.sleep(Integer.MAX_VALUE);
		} catch(InterruptedException e) {
			shutdown();
		}
	}

	/*** Encoders ***/

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
		currentOrientationProvider.start();
	}

	float[] getOrientation() {
		float[] result = new float[3];

		currentOrientationProvider.getEulerAngles(orientation);
		result[0] = (float) Math.toDegrees(orientation[0]) + 180;
		result[1] = (float) Math.toDegrees(orientation[1]) + 180;
		result[2] = (float) Math.toDegrees(orientation[2]) + 180;

		return result;
	}

	void shutdown() {
		currentOrientationProvider.stop();
		setDrivePower(0);
	}
}