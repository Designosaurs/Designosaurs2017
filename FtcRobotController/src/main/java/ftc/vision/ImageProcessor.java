package ftc.vision;


import org.opencv.core.Mat;

public interface ImageProcessor<ResultType> {
	ImageProcessorResult<ResultType> process(long startTime, Mat rgbaFrame, boolean saveImages);
}