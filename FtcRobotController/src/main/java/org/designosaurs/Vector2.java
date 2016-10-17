package org.designosaurs;

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
}