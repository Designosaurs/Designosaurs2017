package org.designosaurs;

import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class SimpleController extends NanoHTTPD {

    private static final int PORT = 9001;

    public SimpleController() throws IOException {

        super(PORT);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("\nSimple Web Controller Interface started on port " + PORT + "\n");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String msg = "<html><body><h1>Hello server</h1>\n";
        Map<String, String> parms = session.getParms();
        if(parms.get("username") == null) {
            msg += "<form action='?' method='get'>\n  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
        } else {
            msg += "<p>Hello, " + parms.get("username") + "!</p>";
        }

        return newFixedLengthResponse(msg + "</body></html>\n");
    }
}