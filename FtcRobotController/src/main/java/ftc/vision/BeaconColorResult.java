package ftc.vision;

import org.opencv.core.Scalar;

public class BeaconColorResult {

	public BeaconColor getLeftColor() {
		return leftColor;
	}

	private final BeaconColor leftColor;

	public BeaconColor getRightColor() {
		return rightColor;
	}

	private final BeaconColor rightColor;

	public BeaconColorResult(BeaconColor leftColor, BeaconColor rightColor) {
		this.leftColor = leftColor;
		this.rightColor = rightColor;
	}

	public enum BeaconColor {
		RED     (ImageUtil.RED),
		GREEN   (ImageUtil.GREEN),
		BLUE    (ImageUtil.BLUE),
		UNKNOWN (ImageUtil.BLACK);


		public final Scalar color;

		BeaconColor(Scalar color) {
			this.color = color;
		}
	}

	@Override
	public String toString(){
		return leftColor + ", " + rightColor;
	}
}