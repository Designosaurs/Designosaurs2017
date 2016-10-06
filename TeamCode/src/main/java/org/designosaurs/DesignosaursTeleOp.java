/*
Copyright (c) 2016 Robert Atkinson

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Robert Atkinson nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESSFOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.designosaurs;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.Range;

import org.opencv.android.OpenCVLoader;

@TeleOp(name="Designosaurs Drive", group="TeleOp")
public class DesignosaursTeleOp extends LinearOpMode {
	private DesignosaursHardware robot = new DesignosaursHardware();

	@Override
	public void runOpMode() throws InterruptedException {
		double left;
		double right;

		robot.init(hardwareMap);

		telemetry.addData("Say", "Designosaurs 2017");
		telemetry.addData("Say", "Loading OpenCV...");
		telemetry.update();

		telemetry.addData("Say", "OpenCV" + (OpenCVLoader.initDebug() ? " not " : "") + " loaded!");
		telemetry.update();

		waitForStart();

		while(opModeIsActive()) {
			left = -gamepad1.left_stick_y;
			right = -gamepad1.right_stick_y;

			robot.leftMotor.setPower(left);
			robot.rightMotor.setPower(right);

			telemetry.addData("left",  "Left pwr: %.2f", left);
			telemetry.addData("right", "Right pwr: %.2f", right);
			telemetry.update();

			robot.waitForTick(40);
			idle();
		}
	}
}
