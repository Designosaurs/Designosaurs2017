package ftc.vision;

public class BeaconPositionResult {
	private double deltaX;
	private double startX;
	private double endX;

	public BeaconPositionResult(double deltaX, double startX, double endX) {
		this.deltaX = deltaX;
		this.startX = startX;
		this.endX = endX;
	}

	public double getOffsetFeet() {
		return 0;
	}

	public double getOffsetPixels() {
		return deltaX;
	}

	public int[] getRangePixels() {
		return new int[] {(int) startX, (int) endX};
	}

	@Override
	public String toString() {
		return "Offset is " + getOffsetFeet() + " feet. Beacon exists from [" + startX + ", " + endX + "] px.";
	}
}
