package ftc.vision;

public class BeaconPositionResult {
	private int deltaX;
	private int startX;
	private int endX;

	public BeaconPositionResult(int deltaX, int startX, int endX) {
		this.deltaX = deltaX;
		this.startX = startX;
		this.endX = endX;
	}

	public int getOffsetEncoderCounts() {
		return 0;
	}

	public int getOffsetFeet() {
		return 0;
	}

	public int getOffsetPixels() {
		return deltaX;
	}

	public int[] getRangePixels() {
		return new int[] {startX, endX};
	}

	@Override
	public String toString() {
		return "Offset is " + getOffsetEncoderCounts() + " encoder counts. Beacon exists from [" + startX + ", " + endX + "] px.";
	}
}
