package org.designosaurs;

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
	private Sensor accelerometer;
	private Sensor magnetometer;

	// Orientation values in radians:
	private float azimuth = 0.0f;
	private float pitch = 0.0f;
	private float roll = 0.0f;

	private float[] mGravity;
	private float[] mGeomagnetic;

	DesignosaursHardware() {}

	void init(HardwareMap hwMap) {
		if(hardwareEnabled) {
			leftMotor = hwMap.dcMotor.get("left");
			rightMotor = hwMap.dcMotor.get("right");
			buttonPusher = hwMap.dcMotor.get("buttonPusher");

			leftMotor.setDirection(DcMotor.Direction.REVERSE);
			leftMotor.setPower(0);
			rightMotor.setPower(0);

			buttonPusher.setDirection(DcMotor.Direction.REVERSE);
			buttonPusher.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

			encoderOffsets.put(leftMotor.hashCode(), 0);
			encoderOffsets.put(rightMotor.hashCode(), 0);
			encoderOffsets.put(buttonPusher.hashCode(), 0);
			resetDriveEncoders();
		}

		mSensorManager = (SensorManager) hwMap.appContext.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
	}

	void waitForTick(long periodMs) throws InterruptedException {
		long remaining = periodMs - (long) period.milliseconds();

		if(remaining > 0) Thread.sleep(remaining);

		period.reset();
	}

	/*** Drive ***/

	double getDistance() {
		return ((double) (getAdjustedEncoderPosition(leftMotor) + getAdjustedEncoderPosition(rightMotor))) / (2 * COUNTS_PER_FOOT);
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

		int originalAngle = getRotationDegrees();
		setDrivePower(power);
		resetDriveEncoders();

		while(Math.abs(getDistance()) < feet)
			try {
				Thread.sleep(15);
			} catch(Exception e) {
				return;
			}

		Log.i("DesignosaursAuto", "Drift: " + (getRotationDegrees() - originalAngle) + " deg");

		setDrivePower(0);
		resetDriveEncoders();
	}

	void turn(double degrees, double power) {
		DcMotor primaryMotor, secondaryMotor;

		resetEncoder(leftMotor);
		resetEncoder(rightMotor);

		int originalAngle = getRotationDegrees();

		primaryMotor = degrees > 0 ? rightMotor : leftMotor;
		secondaryMotor = degrees > 0 ? leftMotor : rightMotor;

		double adjustedPower,
			   current = 0,
			   target = Math.abs((degrees / 360) * COUNTS_PER_ROTATION);

		while(current <= target)
			try {
				current = Math.max(Math.abs(getAdjustedEncoderPosition(primaryMotor)), Math.abs(getAdjustedEncoderPosition(secondaryMotor)));

				adjustedPower = Math.abs(Math.floor(current / target)) < 10 ? power * 0.5: power;

				primaryMotor.setPower(adjustedPower);
				secondaryMotor.setPower(-adjustedPower);

				Thread.sleep(5);
			} catch(Exception e) {
				return;
			}

		Log.i("DesignosaursAuto", "Delta: " + (getRotationDegrees() - originalAngle));

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

	int getRotationDegrees() {
		return (int) Math.round(Math.toDegrees(azimuth));
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

	public void onSensorChanged(SensorEvent event) {
		// we need both sensor values to calculate orientation
		// only one value will have changed when this method called, we assume we can still use the other value.
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			mGravity = event.values;

		if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			mGeomagnetic = event.values;

		if(mGravity != null && mGeomagnetic != null) { // make sure we have both before calling getRotationMatrix
			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);

			if(success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);

				azimuth = orientation[0]; // orientation contains: azimuth, pitch and roll
				pitch = orientation[1];
				roll = orientation[2];
			}
		}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// not sure if needed, placeholder just in case
	}

	void shutdown() {
		mSensorManager.unregisterListener(this);
		setDrivePower(0);
	}
}