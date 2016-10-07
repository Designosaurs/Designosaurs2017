package org.designosaurs;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcontroller.internal.FtcRobotControllerActivity;

import ftc.vision.BeaconColorResult;
import ftc.vision.FrameGrabber;
import ftc.vision.ImageProcessorResult;

@TeleOp(name="Designosaurs Autonomous", group="Auto")
public class DesignosaursAuto extends LinearOpMode {
	private DesignosaursHardware robot = new DesignosaursHardware();

	@Override
	public void runOpMode() throws InterruptedException {
		double left;
		double right;

		robot.init(hardwareMap);

		telemetry.addData("Say", "Hello Driver");
		telemetry.update();

		waitForStart();

		while(opModeIsActive()) {
//			left = -gamepad1.left_stick_y;
//			right = -gamepad1.right_stick_y;
//
//			robot.leftMotor.setPower(left);
//			robot.rightMotor.setPower(right);
//
//			telemetry.addData("left",  "Left pwr: %.2f", left);
//			telemetry.addData("right", "Right pwr: %.2f", right);
//			telemetry.update();
//
//			robot.waitForTick(40);
//			idle();

			FrameGrabber frameGrabber = FtcRobotControllerActivity.frameGrabber; //Get the frameGrabber

			frameGrabber.grabSingleFrame(); //Tell it to grab a frame
			while (!frameGrabber.isResultReady()) { //Wait for the result
				Thread.sleep(5); //sleep for 5 milliseconds
			}

			//Get the result
			ImageProcessorResult imageProcessorResult = frameGrabber.getResult();
			BeaconColorResult result = (BeaconColorResult) imageProcessorResult.getResult();

			BeaconColorResult.BeaconColor leftColor = result.getLeftColor();
			BeaconColorResult.BeaconColor rightColor = result.getRightColor();

			telemetry.addData("Result", result); //Display it on telemetry
			telemetry.update();
			//wait before quitting (quitting clears telemetry)
			Thread.sleep(1000);
		}
	}
}
