package org.designosaurs;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
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
	public DcMotor leftMotor = null;
	public DcMotor rightMotor = null;
	public DcMotor buttonPusher = null;

	public static final int COUNTS_PER_REVOLUTION = 1120;

	private HardwareMap hwMap = null;
	private ElapsedTime period = new ElapsedTime();

	public DesignosaursHardware() {}

	/* Initialize standard Hardware interfaces */
	public void init(HardwareMap ahwMap) {
		hwMap = ahwMap;

		leftMotor = hwMap.dcMotor.get("left");
		rightMotor = hwMap.dcMotor.get("right");
		leftMotor.setDirection(DcMotor.Direction.REVERSE);

		leftMotor.setPower(0);
		rightMotor.setPower(0);

		leftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
		rightMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
		buttonPusher.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
	}

	public void setDrivePower(double power) {
		leftMotor.setPower(power);
		rightMotor.setPower(power);
	}

	public void waitForTick(long periodMs) throws InterruptedException {
		long remaining = periodMs - (long) period.milliseconds();

		if(remaining > 0) Thread.sleep(remaining);

		period.reset();
	}
}
