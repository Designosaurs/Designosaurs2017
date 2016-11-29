package org.designosaurs.opmode;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.util.SparseIntArray;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import java.util.Arrays;

class DesignosaursHardware implements SensorEventListener {
	static final boolean hardwareEnabled = true;

	DcMotor leftMotor = null;
	DcMotor rightMotor = null;
	DcMotor buttonPusher = null;

	static final int COUNTS_PER_REVOLUTION = 2880;
	static final int COUNTS_PER_ROTATION = 7715;
	static final int COUNTS_PER_FOOT = 2128;

	private ElapsedTime period = new ElapsedTime();
	private SparseIntArray encoderOffsets = new SparseIntArray(3);

	private SensorManager mSensorManager;
	private Sensor gyroscope;

	private static final float NS2S = 1.0f / 1000000000.0f;
	private final float[] deltaRotationVector = new float[4];
	private float lastSensorReading;

	DesignosaursHardware() {}

	void init(HardwareMap hwMap) {
		buttonPusher = hwMap.dcMotor.get("buttonPusher");

		buttonPusher.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
		buttonPusher.setDirection(DcMotor.Direction.REVERSE);
		buttonPusher.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

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

		mSensorManager = (SensorManager) hwMap.appContext.getSystemService(Context.SENSOR_SERVICE);
		gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

		mSensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
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
		DcMotor primaryMotor, secondaryMotor;

		resetEncoder(leftMotor);
		resetEncoder(rightMotor);

		primaryMotor = degrees > 0 ? rightMotor : leftMotor;
		secondaryMotor = degrees > 0 ? leftMotor : rightMotor;

		double adjustedPower,
			   current = 0,
			   target = Math.abs((degrees / 360) * COUNTS_PER_ROTATION);

		while(current <= target)
			try {
				current = Math.abs(getAdjustedEncoderPosition(primaryMotor)) >= 2 ? Math.abs(getAdjustedEncoderPosition(primaryMotor)) : Math.abs(getAdjustedEncoderPosition(secondaryMotor)) * 2;

				adjustedPower = Math.abs(Math.floor(current / target)) < 10 ? power * 0.5: power;

				primaryMotor.setPower(adjustedPower);
				secondaryMotor.setPower(-adjustedPower);

				Thread.sleep(1);
			} catch(Exception e) {
				return;
			}

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

	public void onSensorChanged(SensorEvent event) {
		// This timestep's delta rotation to be multiplied by the current rotation
		// after computing it from the gyro sample data.
		if(lastSensorReading != 0) {
			final float dT = (event.timestamp - lastSensorReading) * NS2S;
			// Axis of the rotation sample, not normalized yet.
			float axisX = event.values[0];
			float axisY = event.values[1];
			float axisZ = event.values[2];

			// Calculate the angular speed of the sample
			double omegaMagnitude = Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

			// Integrate around this axis with the angular speed by the timestep
			// in order to get a delta rotation from this sample over the timestep
			// We will convert this axis-angle representation of the delta rotation
			// into a quaternion before turning it into the rotation matrix.
			double thetaOverTwo = omegaMagnitude * dT / 2.0f;
			double sinThetaOverTwo = Math.sin(thetaOverTwo);
			double cosThetaOverTwo = Math.cos(thetaOverTwo);

			deltaRotationVector[0] = (float) sinThetaOverTwo * axisX;
			deltaRotationVector[1] = (float) sinThetaOverTwo * axisY;
			deltaRotationVector[2] = (float) sinThetaOverTwo * axisZ;
			deltaRotationVector[3] = (float) cosThetaOverTwo;
		}

		lastSensorReading = event.timestamp;
		float[] deltaRotationMatrix = new float[9];
		SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
		// User code should concatenate the delta rotation we computed with the current rotation
		// in order to get the updated rotation.

		Log.i("DesignosaursAuto", Arrays.toString(deltaRotationMatrix));

	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// not sure if needed, placeholder just in case
	}

	void shutdown() {
		mSensorManager.unregisterListener(this);
		setDrivePower(0);
	}
}