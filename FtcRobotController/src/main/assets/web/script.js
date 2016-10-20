var ws = new WebSocket("ws://" + location.hostname + ":9002/"),
    state = {
        connected: false,
        opmode: "",
        opmodeState: 0,
        battery: 0
    };

function opmodeStateToDisplayName(opmodeState) {
    switch(opmodeState) {
        case 0:
            return "Unknown";
        case 1:
            return "Stopped";
        case 2:
            return "Init";
        case 3:
            return "Running";
    }
}

function updateState() {
    $("#stateConnected").html(state.connected ? "Connected" : "Disconnected");
    $("#stateOpmode").html(state.opmode);
    $("#stateOpmodeState").html(opmodeStateToDisplayName(state.opmodeState));
    $("#stateBattery").html(state.battery);
}

ws.onopen = function() {
    state.connected = true;
    updateState();
};

ws.onclose = function() {
    state.connected = false;
    updateState();
};

ws.onmessage = function(e) {
    console.log(e);
};
