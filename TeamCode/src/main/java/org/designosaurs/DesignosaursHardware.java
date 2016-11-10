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
	public static final int COUNTS_PER_FOOT = 8640;

	private static final int MIN_DRIFT_CORRECTION = COUNTS_PER_REVOLUTION / 10;
	private static final double DRIFT_CORRECTION_FACTOR = 0.9;

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
			buttonPusher.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

			encoderOffsets.put(leftMotor.hashCode(), 0);
			encoderOffsets.put(rightMotor.hashCode(), 0);
			encoderOffsets.put(buttonPusher.hashCode(), 0);
		}
	}

	public void setDrivePower(double power) {
		if(hardwareEnabled) {
			leftMotor.setPower(-power);
			rightMotor.setPower(-power);
		}
	}

	public void waitForTick(long periodMs) throws InterruptedException {
		long remaining = periodMs - (long) period.milliseconds();

		if(remaining > 0) Thread.sleep(remaining);

		period.reset();
	}

	public void rotateToPosition(int degrees, double power) {
		resetEncoder(leftMotor);
		resetEncoder(rightMotor);

		DcMotor targetMotor = degrees > 0 ? rightMotor : leftMotor;
		double targetPosition = (degrees / 360) * COUNTS_PER_REVOLUTION * 3;

		while(getAdjustedEncoderPosition(targetMotor) < targetPosition) {
			try {
				Thread.sleep(50);
			} catch(InterruptedException e) {}
		}
	}

	public void driveStraightFeet(double distance, double power) {
		resetEncoder(leftMotor);
		resetEncoder(rightMotor);

		leftMotor.setPower(power);
		rightMotor.setPower(power);

		boolean alreadyCorrect = true;

		while((getAdjustedEncoderPosition(leftMotor) + getAdjustedEncoderPosition(rightMotor)) / 2 < distance * COUNTS_PER_FOOT) {
			if(Math.abs(getAdjustedEncoderPosition(leftMotor) - getAdjustedEncoderPosition(rightMotor)) > MIN_DRIFT_CORRECTION) {
				alreadyCorrect = false;

				if((leftMotor.getCurrentPosition() - rightMotor.getCurrentPosition()) > 0)
					rightMotor.setPower(power * DRIFT_CORRECTION_FACTOR);
				else
					leftMotor.setPower(power * DRIFT_CORRECTION_FACTOR);
			} else {
				if(!alreadyCorrect) {
					alreadyCorrect = true;

					leftMotor.setPower(power);
					rightMotor.setPower(power);
				}
			}
		}

		resetEncoder(leftMotor);
		resetEncoder(rightMotor);
	}

	public int getAdjustedEncoderPosition(DcMotor motor) {
		return motor.getCurrentPosition() - encoderOffsets.get(motor.hashCode());
	}

	public void resetEncoder(DcMotor motor) {
		encoderOffsets.put(motor.hashCode(), motor.getCurrentPosition());
	}
}