package org.designosaurs;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "Designosaurs Drive", group = "TeleOp")
public class DesignosaursTeleOp extends LinearOpMode {
	private DesignosaursHardware robot = new DesignosaursHardware();

	private static final double JOYSTICK_DEADBAND = 0.2;

	@Override
	public void runOpMode() throws InterruptedException {
		double left;
		double right;

		robot.init(hardwareMap);

		telemetry.addData("00", "Designosaurs 2017");
		telemetry.addData("00", "Ready to start!");
		telemetry.update();

		waitForStart();

		while(opModeIsActive()) {
			left = -gamepad1.left_stick_y;
			right = -gamepad1.right_stick_y;

			if(Math.abs(left) < JOYSTICK_DEADBAND)
				left = 0;

			if(Math.abs(right) < JOYSTICK_DEADBAND)
				right = 0;

			if(DesignosaursHardware.hardwareEnabled) {
				robot.leftMotor.setPower(left);
				robot.rightMotor.setPower(right);

				telemetry.addData("00", "L power:   " + String.valueOf(left));
				telemetry.addData("01", "L encoder: " + String.valueOf(robot.leftMotor.getCurrentPosition()));
				telemetry.addData("02", "R power:   " + String.valueOf(right));
				telemetry.addData("03", "R encoder: " + String.valueOf(robot.rightMotor.getCurrentPosition()));

				telemetry.update();
			}

			robot.waitForTick(40);
			idle();
		}
	}
}
