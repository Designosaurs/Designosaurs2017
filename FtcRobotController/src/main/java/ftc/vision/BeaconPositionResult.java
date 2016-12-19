package ftc.vision;

class BeaconPositionResult {
    private int deltaX = 0;

    BeaconPositionResult(int deltaX) {
        this.deltaX = deltaX;
    }

    int getEncoderCounts() {
        return 0;
    }

    int getInches() {
        return 0;
    }

    int getPixels() {
        return deltaX;
    }

    @Override
    public String toString() {
        return "BeaconPositionResult (" + deltaX + "px)";
    }
}
