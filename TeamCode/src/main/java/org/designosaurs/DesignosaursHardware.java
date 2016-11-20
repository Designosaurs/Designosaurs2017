package org.designosaurs;

import android.util.Log;
import android.util.SparseIntArray;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

public class DesignosaursHardware {
	public static final boolean hardwareEnabled = true;

	public DcMotor leftMotor = null;
	public DcMotor rightMotor = null;
	public DcMotor buttonPusher = null;

	public static final int COUNTS_PER_REVOLUTION = 2880;
	public static final int COUNTS_PER_ROTATION = 9044;
	public static final int COUNTS_PER_FOOT = 2128;

	private HardwareMap hwMap = null;
	private ElapsedTime period = new ElapsedTime();

	private SparseIntArray encoderOffsets = new SparseIntArray(3);

	public DesignosaursHardware() {}

	/* Initialize standard Hardware interfaces */
	public void init(HardwareMap hwMap) {
		this.hwMap = hwMap;

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
		}
	}

	public void setDrivePower(double power) {
		if(hardwareEnabled) {
			resetEncoder(leftMotor);
			resetEncoder(rightMotor);

			leftMotor.setPower(power);
			rightMotor.setPower(power);
		}
	}

	public void waitForTick(long periodMs) throws InterruptedException {
		long remaining = periodMs - (long) period.milliseconds();

		if(remaining > 0) Thread.sleep(remaining);

		period.reset();
	}

	public void rotateToPosition(int degrees, double power) {
		resetEncoder(rightMotor);
		resetEncoder(leftMotor);

		DcMotor targetMotor = degrees > 0 ? rightMotor : leftMotor;
		double targetPosition = (degrees / 360) * COUNTS_PER_REVOLUTION * 3;

		while(getAdjustedEncoderPosition(targetMotor) < targetPosition) {
			try {
				Thread.sleep(50);
			} catch(InterruptedException e) {}
		}
	}

	public double getDistance() {
		return ((double) (getAdjustedEncoderPosition(leftMotor) + getAdjustedEncoderPosition(rightMotor))) / (2 * COUNTS_PER_FOOT);
	}

	public void goStraight(double feet, double power) {
		feet = Math.abs(feet);
		setDrivePower(power);

		while(Math.abs(getDistance()) < feet)
			try {
				Thread.sleep(50);
			} catch(Exception e) {
				return;
			}

		setDrivePower(0);
	}

	public void turn(double degrees, double power) {
		DcMotor primaryMotor, secondaryMotor;

		resetEncoder(leftMotor);
		resetEncoder(rightMotor);

		primaryMotor = degrees > 0 ? rightMotor : leftMotor;
		secondaryMotor = degrees > 0 ? leftMotor : rightMotor;

		primaryMotor.setPower(power);
		secondaryMotor.setPower(-power);

		while(getAdjustedEncoderPosition(primaryMotor) <= ((degrees / 360) * COUNTS_PER_ROTATION))
			try {
				Log.i("DesignosaursAuto", "Current position: " + String.valueOf(getAdjustedEncoderPosition(primaryMotor)));
				Log.i("DesignosaursAuto", "Desired position: " + String.valueOf((degrees / 360) * COUNTS_PER_ROTATION));

				Thread.sleep(50);
			} catch(Exception e) {
				return;
			}

		setDrivePower(0);
	}

	public int getAdjustedEncoderPosition(DcMotor motor) {
		return motor.getCurrentPosition() - encoderOffsets.get(motor.hashCode());
	}

	public void resetEncoder(DcMotor motor) {
		encoderOffsets.put(motor.hashCode(), motor.getCurrentPosition());
	}
}