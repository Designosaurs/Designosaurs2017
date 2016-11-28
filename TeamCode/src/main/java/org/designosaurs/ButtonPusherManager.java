package org.designosaurs;

import android.util.Log;
import java.util.ArrayList;

public class ButtonPusherManager extends Thread {
	/* Configuration */
	private static final double POWER = 0.2;
	private static final int MOVEMENT_THRESHOLD = 10;
	private static final double TARGET_IDLE_POSITION = DesignosaursHardware.COUNTS_PER_REVOLUTION * 0.25;
	private static final double AT_BASE_TOLERANCE = 50;
	private static final double EXTEND_MAX = 3800;

	/* Available states */
	public static final byte STATE_HOMING = 0;
	public static final byte STATE_RETURNING_TO_BASE = 1;
	public static final byte STATE_AT_BASE = 2;
	public static final byte STATE_SCORING = 3;

	private byte state = -1;
	private int lastPosition = 0;
	private ArrayList<Integer> positionHistory = new ArrayList<>(20);

	private int ticks = 0;
	private int ticksInState = 0;
	private boolean isRunning = true;

	private DesignosaursHardware robot;
	private final String TAG = "ButtonPusherManager";

	ButtonPusherManager(DesignosaursHardware robot) {
		this.robot = robot;
	}

	private void update() {
		ticks++;
		ticksInState++;

		int movementDelta = robot.getAdjustedEncoderPosition(robot.buttonPusher) - lastPosition;
		int maxRecentMovement = 0;
		boolean isStuck = false;

		lastPosition = robot.getAdjustedEncoderPosition(robot.buttonPusher);

		if(ticksInState > 20) {
			positionHistory.remove(0);
			positionHistory.add(Math.abs(movementDelta));

			for(int i = 0; i < positionHistory.size(); i++) {
				Integer val = positionHistory.get(i);

				if(val > maxRecentMovement)
					maxRecentMovement = val;
			}

			if(maxRecentMovement < MOVEMENT_THRESHOLD)
				isStuck = true;
		}

		switch(state) {
			case STATE_HOMING:
				if(isStuck) {
					robot.setButtonPusherPower(0);
					robot.resetEncoder(robot.buttonPusher);

					setStatus(STATE_RETURNING_TO_BASE);
				}
			break;
			case STATE_RETURNING_TO_BASE:
				double buttonPusherPositionDelta = robot.getAdjustedEncoderPosition(robot.buttonPusher) - TARGET_IDLE_POSITION;

				if(Math.abs(buttonPusherPositionDelta) < AT_BASE_TOLERANCE) {
					robot.setButtonPusherPower(0);

					setStatus(STATE_AT_BASE);
				} else
					robot.setButtonPusherPower(robot.getAdjustedEncoderPosition(robot.buttonPusher) <= TARGET_IDLE_POSITION ? POWER : -POWER);
			break;
			case STATE_SCORING:
				if(robot.getAdjustedEncoderPosition(robot.buttonPusher) >= EXTEND_MAX || isStuck)
					setStatus(STATE_RETURNING_TO_BASE);
		}
	}

	public void run() {
		try {
			while(isRunning) {
				update();
				Thread.sleep(5);
			}
		} catch(InterruptedException e) {
			Log.i(TAG, "Shutting down...");
		}
	}

	byte getStatus() {
		return state;
	}

	String getStatusMessage() {
		switch(state) {
			case STATE_HOMING:
				return "homing...";
			case STATE_AT_BASE:
				return "at base";
			case STATE_RETURNING_TO_BASE:
				return "returning to base...";
			case STATE_SCORING:
				return "scoring...";
		}

		return "unknown";
	}

	public void setStatus(byte state) {
		Log.i(TAG, "*** SWITCHING STATES ***");
		Log.i(TAG, "Previous state: " + this.state);
		Log.i(TAG, "New state: " + state);
		Log.i(TAG, "Time in state: " + ticksInState + " ticks");

		this.state = state;

		positionHistory.clear();
		for(int i = 1; i <= 80; i++)
			positionHistory.add(10);

		switch(state) {
			case STATE_HOMING:
				robot.setButtonPusherPower(-POWER);
			break;
			case STATE_SCORING:
				robot.setButtonPusherPower(POWER);
				positionHistory.clear();

				for(int i = 1; i <= 40; i++)
					positionHistory.add(10);
		}
	}

	int getTicksInState() {
		return ticksInState;
	}

	void shutdown() {
		isRunning = false;
			robot.setButtonPusherPower(0);
	}

}
