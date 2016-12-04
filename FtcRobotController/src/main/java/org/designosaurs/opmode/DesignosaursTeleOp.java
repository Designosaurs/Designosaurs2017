package org.designosaurs.opmode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

import java.text.DecimalFormat;

@TeleOp(name = "Designosaurs Drive", group = "TeleOp")
public class DesignosaursTeleOp extends LinearOpMode {
	private DesignosaursHardware robot = new DesignosaursHardware();

	private static final double JOYSTICK_DEADBAND = 0.2;
	private static final double DRIVE_POWER = 0.7;
	private static final double BUTTON_PUSHER_POWER = 0.4;

	private DecimalFormat decimalFormat = new DecimalFormat("#.00");

	private void setInitStatus(String status) {
		telemetry.clear();
		telemetry.addLine("== Designosaurs 2017 ==");
		telemetry.addLine(status);
		telemetry.update();
	}

	@Override
	public void runOpMode() {
		double left, right, buttonPusher, lift;

		setInitStatus("Initializing hardware...");
		robot.init(hardwareMap);

		robot.leftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
		robot.rightMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
		robot.buttonPusher.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

		setInitStatus("Ready to start!");

		waitForStart();

		while(opModeIsActive()) {
			left = -gamepad1.left_stick_y;
			right = -gamepad1.right_stick_y;
			buttonPusher = -gamepad2.left_stick_y;
			lift = -gamepad2.right_stick_y;

			if(Math.abs(left) < JOYSTICK_DEADBAND)
				left = 0;

			if(Math.abs(right) < JOYSTICK_DEADBAND)
				right = 0;

			if(Math.abs(buttonPusher) < JOYSTICK_DEADBAND)
				buttonPusher = 0;

			if(Math.abs(lift) < JOYSTICK_DEADBAND)
				lift = 0;

			if(DesignosaursHardware.hardwareEnabled) {
				robot.leftMotor.setPower(left * DRIVE_POWER);
				robot.rightMotor.setPower(right * DRIVE_POWER);
				robot.buttonPusher.setPower(buttonPusher * BUTTON_PUSHER_POWER);
				robot.lift.setPower(lift > 0 ? 0.7 : lift);

				telemetry.clear();
				telemetry.addLine("L power: " + decimalFormat.format(left));
				telemetry.addLine("R power: " + decimalFormat.format(right));
				telemetry.addLine("BP power: " + decimalFormat.format(buttonPusher));
				telemetry.addLine("LIFT power: " + decimalFormat.format(robot.lift.getPower()));
				telemetry.update();
			}

			robot.waitForTick(20);
			idle();
		}
	}
}
