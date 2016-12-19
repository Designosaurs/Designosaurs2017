package ftc.vision;

public class BeaconPositionResult {
	private double deltaX;
	private double startX;
	private double endX;
	private boolean conclusive = true;

	public BeaconPositionResult(boolean isConclusive, double deltaX, double startX, double endX) {
		this.conclusive = isConclusive;
		this.deltaX = deltaX;
		this.startX = startX;
		this.endX = endX;
	}

	public boolean isConclusive() {
		return conclusive;
	}

	public void setConclusive(boolean isConclusive) {
		this.conclusive = isConclusive;
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
		return "Offset is " + getOffsetFeet() + " feet. Beacon exists from [" + startX + ", " + endX + "] px" + (conclusive ? "" : "(inconclusive)") + ".";
	}
}
