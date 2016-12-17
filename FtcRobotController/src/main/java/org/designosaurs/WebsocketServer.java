package org.designosaurs;

import android.util.Log;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

import fi.iki.elonen.NanoWSD.WebSocketFrame.CloseCode;


class WebsocketServer extends NanoWSD.WebSocket {
	private WebServer server;

	private String TAG = "WebsocketServer";

	WebsocketServer(WebServer server, NanoHTTPD.IHTTPSession handshakeRequest) {
		super(handshakeRequest);
		this.server = server;
	}

	@Override
	protected void onOpen() {
		Log.i(TAG, "Client connected!");
	}

	@Override
	protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
		Log.i(TAG, "Client disconnected" + (initiatedByRemote ? "" : " (server initiated)."));

		if(server.debug) {
			System.out.println("C [" + (initiatedByRemote ? "Remote" : "Self") + "] " + (code != null ? code : "UnknownCloseCode[" + code + "]")
					+ (reason != null && !reason.isEmpty() ? ": " + reason : ""));
		}
	}

	@Override
	protected void onMessage(NanoWSD.WebSocketFrame message) {
		message.setUnmasked();
	}

	@Override
	protected void onPong(NanoWSD.WebSocketFrame pong) {
		if(server.debug)
			System.out.println("Pong: " + pong);
	}

	@Override
	protected void onException(IOException exception) {
		Log.e(TAG, "An exception occurred:");
		exception.printStackTrace();
	}

	@Override
	protected void debugFrameReceived(NanoWSD.WebSocketFrame frame) {
		/*if(server.debug)
			System.out.println("Recv: " + frame);*/
	}

	@Override
	protected void debugFrameSent(NanoWSD.WebSocketFrame frame) {
		/*if(server.debug)
			System.out.println("Send: " + frame);*/
	}
}
