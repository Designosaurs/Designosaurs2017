package org.designosaurs;

import com.vuforia.Vec2F;

public class Vector2 {

	public int X;
	public int Y;

	public Vector2(int x, int y) {
		this.X = x;
		this.Y = y;
	}

	public Vector2(float x, float y) {
		this.X = Math.round(x);
		this.Y = Math.round(y);
	}

	public Vector2(Vec2F value) {
		float[] v = value.getData();
		this.X = Math.round(v[0]);
		this.Y = Math.round(v[1]);
	}

	public String toString() {
		return "(" + X + ", " + Y + ")";
	}
}