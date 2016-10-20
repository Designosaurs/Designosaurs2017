package org.designosaurs;

import com.vuforia.Vec2F;

public class Vector2 {

	public int x;
	public int y;

	public Vector2(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Vector2(float x, float y) {
		this.x = Math.round(x);
		this.y = Math.round(y);
	}

	public Vector2(Vec2F value) {
		float[] v = value.getData();
		this.x = Math.round(v[0]);
		this.y = Math.round(v[1]);
	}

	public String toString() {
		return "(" + x + ", " + y + ")";
	}
}