package org.designosaurs;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import fi.iki.elonen.NanoWSD;

import fi.iki.elonen.NanoWSD.WebSocketFrame.CloseCode;


public class WebsocketServer extends NanoWSD {
	private static final Logger LOG = Logger.getLogger(WebsocketServer.class.getName());

	private final boolean debug;

	public WebsocketServer(int port, boolean debug) {
		super(port);

		this.debug = debug;
	}

	@Override
	protected WebSocket openWebSocket(IHTTPSession handshake) {
		return new WebsocketInstance(this, handshake);
	}

	private static class WebsocketInstance extends WebSocket {
		private final WebsocketServer server;


		public WebsocketInstance(WebsocketServer server, IHTTPSession handshakeRequest) {
			super(handshakeRequest);
			this.server = server;
		}

		@Override
		protected void onOpen() {

		}

		@Override
		protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
			if (server.debug) {
				System.out.println("C [" + (initiatedByRemote ? "Remote" : "Self") + "] " + (code != null ? code : "UnknownCloseCode[" + code + "]")
						+ (reason != null && !reason.isEmpty() ? ": " + reason : ""));
			}
		}

		@Override
		protected void onMessage(WebSocketFrame message) {
            message.setUnmasked();
		}

		@Override
		protected void onPong(WebSocketFrame pong) {
			if(server.debug)
				System.out.println("Pong: " + pong);
		}

		@Override
		protected void onException(IOException exception) {
			WebsocketServer.LOG.log(Level.SEVERE, "exception occurred", exception);
		}

		@Override
		protected void debugFrameReceived(WebSocketFrame frame) {
			if(server.debug)
				System.out.println("Recv: " + frame);
		}

		@Override
		protected void debugFrameSent(WebSocketFrame frame) {
			if(server.debug)
				System.out.println("Send: " + frame);
		}
	}
}
