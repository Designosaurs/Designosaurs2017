package org.designosaurs;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "Designosaurs Drive", group = "TeleOp")
public class DesignosaursTeleOp extends LinearOpMode {
	private DesignosaursHardware robot = new DesignosaursHardware();

	@Override
	public void runOpMode() throws InterruptedException {
		double left;
		double right;

		robot.init(hardwareMap);

		telemetry.addData("Say", "Designosaurs 2017");
		telemetry.addData("Say", "Ready to start!");
		telemetry.update();

		waitForStart();

		while(opModeIsActive()) {
			left = -gamepad1.left_stick_y;
			right = -gamepad1.right_stick_y;

			robot.leftMotor.setPower(left);
			robot.rightMotor.setPower(right);

			telemetry.addData("left", "Left pwr: %.2f", left);
			telemetry.addData("right", "Right pwr: %.2f", right);
			telemetry.update();

			robot.waitForTick(40);
			idle();
		}
	}
}
