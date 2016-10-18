package org.designosaurs;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.qualcomm.ftcrobotcontroller.R;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.vuforia.Matrix34F;
import com.vuforia.Tool;
import com.vuforia.Vec2F;
import com.vuforia.Vec3F;

import org.firstinspires.ftc.robotcontroller.internal.FtcRobotControllerActivity;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;

import java.util.Arrays;

/**
 * This OpMode illustrates the basics of using the Vuforia localizer to determine
 * positioning and orientation of robot on the FTC field.
 * The code is structured as a LinearOpMode
 *
 * Vuforia uses the phone's camera to inspect it's surroundings, and attempt to locate target images.
 *
 * When images are located, Vuforia is able to determine the position and orientation of the
 * image relative to the camera.  This sample code than combines that information with a
 * knowledge of where the target images are on the field, to determine the location of the camera.
 *
 * This example assumes a "diamond" field configuration where the red and blue alliance stations
 * are adjacent on the corner of the field furthest from the audience.
 * From the Audience perspective, the Red driver station is on the right.
 * The two vision target are located on the two walls closest to the audience, facing in.
 * The Stones are on the RED side of the field, and the Chips are on the Blue side.
 *
 * A final calculation then uses the location of the camera on the robot to determine the
 * robot's location and orientation on the field.
 *
 * @see VuforiaLocalizer
 * @see VuforiaTrackableDefaultListener
 * see  ftc_app/doc/tutorial/FTC_FieldCoordinateSystemDefinition.pdf
 *
 * Use Android Studio to Copy this Class, and Paste it into your team's code folder with a new name.
 * Remove or comment out the @Disabled line to add this opmode to the Driver Station OpMode list.
 *
 * IMPORTANT: In order to use this OpMode, you need to obtain your own Vuforia license key as
 * is explained below.
 */

@Autonomous(name = "Designosaurs Autonomous", group = "Auto")
public class DesignosaursAuto extends LinearOpMode {
	private DesignosaursHardware robot = new DesignosaursHardware();

	public final String VUFORIA_LICENCE_KEY = "ATwI0oz/////AAAAGe9HyiYVEU6pmTFAb65tOfUrioTxlZtITHRLN1h3wllaw67kJsUOHwPVDsCN0vxiKy/9Qi9NnjpkVfUnn0gwIHyKJgTYkG7+dCaJtFJlY94qa1YPCy0y4rwhVQFkDkcaCiNoiS7ZSU5KLeIABF4Gvz9qYwJJtwxWGp4fbjyu+arTOUw160+Fg5XMjoftS8FAQPx4wF33sVdGw+CYX0fHdwQzOyN0PpIwBQ9xvb8e1c76FoHF0YUZyV/q0XeR97nRj1TfnesPc+v7Z72SEDCXAAdVVS6L9u/mVAxq4zTaXsdGcVsqHeaouoGmQ/1Ey/YYShqHaRZXWwC4GsgaxO9tCkWNH+hTjFZA2pgvKVl5HmLR";

	private float mmPerInch = 25.4f;
	private float mmBotWidth = 18 * mmPerInch;
	private float mmFTCFieldWidth = (12*12 - 2) * mmPerInch;

	enum Side {
		upperLeft(0),
		upperRight(1),
		lowerLeft(2),
		lowerRight(3);

		final int value;

		Side(int value) {
			this.value = value;
		}
	}

	@Override
	public void runOpMode() throws InterruptedException {
		robot.init(hardwareMap);

		VuforiaLocalizer.Parameters params = new VuforiaLocalizer.Parameters(R.id.cameraMonitorViewId);
		params.cameraDirection = VuforiaLocalizer.CameraDirection.BACK;
		params.vuforiaLicenseKey = VUFORIA_LICENCE_KEY;

		VuforiaLocalizerImplSubclass vuforia = new VuforiaLocalizerImplSubclass(params);

		VuforiaTrackables beacons = vuforia.loadTrackablesFromAsset("FTC_2016-17");
		beacons.get(0).setName("Wheels");
		beacons.get(1).setName("Tools");
		beacons.get(2).setName("Lego");
		beacons.get(3).setName("Gears");

		waitForStart();

		//robot.buttonPusher.setTargetPosition((int) (DesignosaursHardware.COUNTS_PER_REVOLUTION * 1.5));
//		robot.setDrivePower(0.5);

		beacons.activate();

		while(opModeIsActive()) {
			for(VuforiaTrackable beac : beacons) {
				OpenGLMatrix pose = ((VuforiaTrackableDefaultListener) beac.getListener()).getRawPose();

				if(pose != null) {
//					robot.setDrivePower(0);

					Matrix34F rawPose = new Matrix34F();
					float[] poseData = Arrays.copyOfRange(pose.transposed().getData(), 0, 12);
					rawPose.setData(poseData);

					Vec2F upperLeft = Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(-127, 92, 0));//254.000000 184
					Vec2F upperRight = Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(127, 92, 0));
					Vec2F lowerLeft = Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(-127, -92, 0));
					Vec2F lowerRight = Tool.projectPoint(vuforia.getCameraCalibration(), rawPose, new Vec3F(127, -92, 0));

					float[][] data = new float[4][];//Its y, x D:

					data[Side.upperLeft.value] = swapValues(upperLeft.getData());//[0] x pos in video output
					data[Side.upperRight.value] = swapValues(upperRight.getData());
					data[Side.lowerLeft.value] = swapValues(lowerLeft.getData());
					data[Side.lowerRight.value] = swapValues(lowerRight.getData());

					Vector2 start = new Vector2(Math.min(data[Side.upperRight.value][0], data[Side.upperLeft.value][0]), Math.min(data[Side.upperLeft.value][1], data[Side.lowerLeft.value][1]));
					Vector2 end = new Vector2(Math.max(data[Side.upperRight.value][0], data[Side.lowerLeft.value][0]), Math.max(data[Side.upperLeft.value][1], data[Side.lowerLeft.value][1]));
					String debugData = "Start(" + start.x + "," + start.y + "),";
					debugData += "End(" + end.x + "," + end.y + ")";

					VectorF translation = pose.getTranslation();

					telemetry.addData(beac.getName() + "-Translation", translation);

					double degreesToTurn = Math.toDegrees(Math.atan2(translation.get(1), translation.get(2)));

					telemetry.addData(beac.getName() + "-Degrees", degreesToTurn);

					if(vuforia.rgb != null) {
						Bitmap bm = Bitmap.createBitmap(vuforia.rgb.getWidth(), vuforia.rgb.getHeight(), Bitmap.Config.RGB_565);
						bm.copyPixelsFromBuffer(vuforia.rgb.getPixels());
						bm = RotateBitmap(bm, 90);

						Bitmap resizedbitmap = Bitmap.createBitmap(bm, Math.max(0, start.x), Math.max(0, start.y), Math.abs(start.x - end.x), Math.abs(start.y - end.y));

						FtcRobotControllerActivity.simpleController.setImage(bm);
					}
				}
			}
			telemetry.update();
		}
	}

	static Bitmap RotateBitmap(Bitmap source, float angle) {
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}

	static float[] swapValues(float[] values) {
		return new float[] { values[1], values[0] };
	}
}