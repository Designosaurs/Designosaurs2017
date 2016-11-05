package org.designosaurs;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * This is NOT an opmode.
 * <p>
 * This class can be used to define all the specific hardware for a single robot.
 * In this case that robot is a K9 robot.
 * <p>
 * This hardware class assumes the following device names have been configured on the robot:
 * Note:  All names are lower case and some have single spaces between words.
 * <p>
 * Motor channel:  Left  drive motor:        "left motor"
 * Motor channel:  Right drive motor:        "right motor"
 * Servo channel:  Servo to raise/lower arm: "arm"
 * Servo channel:  Servo to open/close claw: "claw"
 * <p>
 * Note: the configuration of the servos is such that:
 * As the arm servo approaches 0, the arm position moves up (away from the floor).
 * As the claw servo approaches 0, the claw opens up (drops the game element).
 */
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

			leftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
			rightMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
			leftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
			rightMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
			buttonPusher.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
		}
	}

	public void setDrivePower(double power) {
		if(hardwareEnabled) {
			//leftMotor.setPower(power);
			//rightMotor.setPower(power);
		}
	}

	public void waitForTick(long periodMs) throws InterruptedException {
		long remaining = periodMs - (long) period.milliseconds();

		if(remaining > 0) Thread.sleep(remaining);

		period.reset();
	}

	public void rotateToPosition(int degrees, double power) {
		leftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
		rightMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
		leftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
		rightMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

		DcMotor targetMotor = degrees > 0 ? rightMotor : leftMotor;
		double targetPosition = (degrees / 360) * COUNTS_PER_REVOLUTION * 3;

		while(targetMotor.getCurrentPosition() < targetPosition) {
			try {
				Thread.sleep(50);
			} catch(InterruptedException e) {}
		}

		leftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
		rightMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
		leftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
		rightMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
	}

	public void driveStraightFeet(double distance, double power) {
		leftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
		rightMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
		leftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
		rightMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

		leftMotor.setPower(power);
		rightMotor.setPower(power);

		boolean alreadyCorrect = true;

		while((leftMotor.getCurrentPosition() + rightMotor.getCurrentPosition()) / 2 < distance * COUNTS_PER_FOOT) {
			if(Math.abs(leftMotor.getCurrentPosition() - rightMotor.getCurrentPosition()) > MIN_DRIFT_CORRECTION) {
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

		leftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
		rightMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
		leftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
		rightMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
	}
}