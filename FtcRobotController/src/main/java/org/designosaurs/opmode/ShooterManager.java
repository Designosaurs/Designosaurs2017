package org.designosaurs.opmode;

import android.util.Log;

import java.util.ArrayList;

public class ShooterManager extends Thread {
	/* Configuration */
	// Power when aligning for the first
	private static final double HOMING_POWER = 0.1;
	// Power to shoot with
	private static final double POWER = 1;
	// Distance to move the plastic piece
	private static final double COUNTS_PER_ROTATION = DesignosaursHardware.COUNTS_PER_REVOLUTION * 2.2;
	// Whether to log states
	private static final boolean LOGGING = false;

	/* Available states */
	static final byte STATE_AT_BASE = 0;
	static final byte STATE_HOMING = 1;
	static final byte STATE_SCORING = 2;

	/* Runtime */
	private byte state = STATE_AT_BASE;
	private int ticksInState = 0;
	private boolean isRunning = true;
	private int lastPosition = 0;
	private ArrayList<Integer> positionHistory = new ArrayList<>(80);

	// Expected change in encoder counts per loop, used to detect whether it's home/stuck
	private static final int MOVEMENT_THRESHOLD = 10;

	private DesignosaursHardware robot;
	private final String TAG = "ShooterManager";

	ShooterManager(DesignosaursHardware robot) {
		this.robot = robot;
	}

	private void update() {
		ticksInState++;

		int movementDelta = robot.getAdjustedEncoderPosition(robot.buttonPusher) - lastPosition;
		int maxRecentMovement = 0;
		boolean isStuck = false;

		lastPosition = robot.getAdjustedEncoderPosition(robot.buttonPusher);

		// Routine to detect whether pusher is actually moving:
		if(ticksInState > 20) {
			positionHistory.remove(0);
			positionHistory.add(Math.abs(movementDelta));

			// Maintain a backlog of recent positions, so singlular values don't throw it off
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
				if(isStuck)
					setStatus(STATE_AT_BASE);
			break;
			case STATE_SCORING:
				if(robot.getAdjustedEncoderPosition(robot.shooter) >= COUNTS_PER_ROTATION)
					setStatus(STATE_AT_BASE);
		}
	}

	public void run() {
		try {
			while(isRunning) {
				update();
				Thread.sleep(5); // careful when adjusting loop time, that affects length of movement history
			}
		} catch(InterruptedException e) {
			if(LOGGING)
				Log.i(TAG, "Shutting down...");
			shutdown();
		}
	}

	byte getStatus() {
		return state;
	}

	// Written to driver station via telemetry
	String getStatusMessage() {
		switch(state) {
			case STATE_HOMING:
				return "homing...";
			case STATE_AT_BASE:
				return "at base";
			case STATE_SCORING:
				return "scoring...";
		}

		return "unknown";
	}

	// Use to progress state machine
	public void setStatus(byte state) {
		if(LOGGING) {
			Log.i(TAG, "*** SWITCHING STATES ***");
			Log.i(TAG, "Previous state: " + this.state);
			Log.i(TAG, "New state: " + state);
			Log.i(TAG, "Time in state: " + ticksInState);
		}

		this.state = state;

		positionHistory.clear();
		for(int i = 1; i <= 100; i++)
			positionHistory.add(10);

		switch(state) {
			case STATE_AT_BASE:
				robot.setShooterPower(0);
			break;
			case STATE_HOMING:
				robot.setShooterPower(0);
				robot.resetEncoder(robot.buttonPusher);

				robot.setShooterPower(HOMING_POWER);
			break;
			case STATE_SCORING:
				robot.resetEncoder(robot.shooter);

				robot.setShooterPower(POWER);
		}
	}

	// Called when opmode cleanly shuts down
	void shutdown() {
		isRunning = false;
		robot.setShooterPower(0);
	}
}
