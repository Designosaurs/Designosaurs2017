package ftc.vision;


import org.opencv.core.Mat;

public class ImageProcessorResult<ResultType> {
	private final long startTime, endTime;
	private final ResultType result;
	private final Mat frame;

	ImageProcessorResult(long startTime, Mat frame, ResultType result) {
		this.startTime = startTime;
		this.result = result;
		this.frame = frame;
		this.endTime = System.currentTimeMillis();
	}

	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public long getElapsedTime() {
		return endTime - startTime;
	}

	public boolean isResultNull() {
		return result == null;
	}

	public ResultType getResult() {
		return result;
	}

	public Mat getFrame() {
		return frame;
	}

	@Override
	public String toString() {
		if(isResultNull()) {
			return "null";
		} else {
			return result.toString();
		}
	}
}