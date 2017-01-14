package org.designosaurs.opmode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

import java.text.DecimalFormat;

@TeleOp(name = "Designosaurs Shooter", group = "Autonomous")
public class DesignosaursShooter extends LinearOpMode {
	private DesignosaursHardware robot = new DesignosaursHardware(true);
	private ShooterManager shooterManager = new ShooterManager(robot);

	private static final double JOYSTICK_DEADBAND = 0.2;
	private static final double BUTTON_PUSHER_POWER = 0.3;

	private static final double HIGH_GEAR = 1;
	private static final double LOW_GEAR = 0.5;

	private double drivePower = HIGH_GEAR;

	private DecimalFormat decimalFormat = new DecimalFormat("#.00");

	private void setInitStatus(String status) {
		telemetry.clear();
		telemetry.addLine("== Designosaurs 2017 ==");
		telemetry.addLine(status);
		telemetry.update();
	}

	@Override
	public void runOpMode() {
		setInitStatus("Initializing hardware...");
		robot.init(hardwareMap);

		setInitStatus("Ready to start!");
		waitForStart();

		robot.accel(0.3, 0.5);
		shooterManager.setStatus(ShooterManager.STATE_SCORING);
		robot.setDrivePower(0);
		robot.waitForTick(1400);
		robot.accel(0.3, 0.5);
		robot.waitForTick(3000);
		shooterManager.setStatus(ShooterManager.STATE_HOMING);
		robot.waitForTick(10000);
		robot.goStraight(4.5, 1);
		robot.setDrivePower(0);
	}
}
