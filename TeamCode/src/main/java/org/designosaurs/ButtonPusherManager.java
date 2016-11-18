package org.designosaurs;

import android.util.Log;

import java.util.ArrayList;

public class ButtonPusherManager extends Thread {
    /* Configuration */
    private static final double POWER = 0.2;
    private static final int MOVEMENT_THRESHOLD = 3;
    private static final double TARGET_IDLE_POSITION = DesignosaursHardware.COUNTS_PER_REVOLUTION * 0.25;
    private static final double AT_BASE_TOLERANCE = 100;
    private static final double EXTEND_MAX = DesignosaursHardware.COUNTS_PER_REVOLUTION;

    /* Available states */
    public static final byte STATE_HOMING = 0;
    public static final byte STATE_RETURNING_TO_BASE = 1;
    public static final byte STATE_AT_BASE = 2;
    public static final byte STATE_SCORING = 3;

    private byte state = -1;
    private int lastPosition = 0;
    private ArrayList<Integer> positionHistory = new ArrayList<>(300);

    private int ticks = 0;
    private int ticksInState = 0;

    private DesignosaursHardware robot;

    ButtonPusherManager(DesignosaursHardware robot) {
        this.robot = robot;
    }

    public void update() {
        ticks++;
        ticksInState++;

        Log.i("button pusher", String.valueOf(ticks));
        Log.i("button pusher pos", String.valueOf(robot.getAdjustedEncoderPosition(robot.buttonPusher)));

        int movementDelta = robot.getAdjustedEncoderPosition(robot.buttonPusher) - lastPosition;
        int maxRecentMovement = 0;
        boolean isStuck = false;

        lastPosition = robot.getAdjustedEncoderPosition(robot.buttonPusher);

        if(ticks > 20) {
            positionHistory.add(Math.abs(movementDelta));
            positionHistory.remove(0);

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
                    robot.buttonPusher.setPower(0);
                    robot.resetEncoder(robot.buttonPusher);

                    setStatus(STATE_RETURNING_TO_BASE);
                }
            break;
            case STATE_RETURNING_TO_BASE:
                double buttonPusherPositionDelta = robot.getAdjustedEncoderPosition(robot.buttonPusher) - TARGET_IDLE_POSITION;

                if(Math.abs(buttonPusherPositionDelta) < AT_BASE_TOLERANCE) {
                    robot.buttonPusher.setPower(0);

                    setStatus(STATE_AT_BASE);
                } else
                    robot.buttonPusher.setPower(buttonPusherPositionDelta > 0 ? POWER : -POWER);
            break;
            case STATE_SCORING:
                if(robot.getAdjustedEncoderPosition(robot.buttonPusher) >= EXTEND_MAX || isStuck)
                    setStatus(STATE_RETURNING_TO_BASE);
        }
    }

    public void run() {
        try {
            while(true) {
                update();
                Thread.sleep(20);
            }
        } catch(InterruptedException e) {
            Log.i("ButtonPusherManager", "Shutting down...");
        }
    }

    public byte getStatus() {
        return state;
    }

    public void setStatus(byte state) {
        Log.i("ButtonPusher", "*** SWITCHING STATES ***");
        Log.i("ButtonPusher", "Previous state: " + this.state);
        Log.i("ButtonPusher", "New state: " + state);
        Log.i("ButtonPusher", "Time in state: " + ticksInState + " ticks");

        this.state = state;

        positionHistory.clear();
        for(int i = 1; i <= 300; i++)
            positionHistory.add(15);

        switch(state) {
            case STATE_HOMING:
                robot.buttonPusher.setPower(POWER);
            break;
            case STATE_SCORING:
                robot.buttonPusher.setPower(-(POWER / 2));
                positionHistory.clear();

                for(int i = 1; i <= 5; i++)
                    positionHistory.add(5);
        }
    }
}
