package org.designosaurs;

import android.util.Log;
import android.util.SparseIntArray;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

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
	}

	void waitForTick(long periodMs) throws InterruptedException {
		long remaining = periodMs - (long) period.milliseconds();

		if(remaining > 0) Thread.sleep(remaining);

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

	void shutdown() {
		setDrivePower(0);
	}
}