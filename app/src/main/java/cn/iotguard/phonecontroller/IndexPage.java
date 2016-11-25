package cn.iotguard.phonecontroller;

/**
 * Created by caowentao on 2016/11/25.
 */

public class IndexPage {
    static String sHTML = "<!DOCTYPE html><html><head><title>Android Controller</title><style>#screen {" +
        "user-drag: none;user-select: none;-moz-user-select: none;-webkit-user-drag: none;-webkit-user-select: none;-ms-user-select: none;}" +
    "</style></head><body><img src=\"screenshot.jpg\" id=\"screen\"/></body>" +
    "<script type=\"text/javascript\">var screenFile = 'screenshot.jpg?';var ws = new WebSocket(\"ws://localhost:"+Main.LISTEN_PORT+"/input\");"+
    "var screen = document.getElementById(\"screen\");var shouldSendMoveEvent = false;" +
    "var down = function(event) {shouldSendMoveEvent = true;var x = event.pageX - screen.offsetLeft;var y = event.pageY - screen.offsetTop;"+
    "var eventjson = '{\"type\":\"fingerdown\",\"x\":'+x+',\"y\":'+y+'}';ws.send(eventjson);}"+
    "\nvar up = function(event) {var x = event.pageX - screen.offsetLeft;var y = event.pageY - screen.offsetTop;var eventjson = '{\"type\":\"fingerup\",\"x\":'+x+',\"y\":'+y+'}';"+
            "ws.send(eventjson);shouldSendMoveEvent = false;}"+
    "\nvar move = function(event) {if (shouldSendMoveEvent) {var x = event.pageX - screen.offsetLeft;var y = event.pageY - screen.offsetTop;"+
            "var eventjson = '{\"type\":\"fingermove\",\"x\":'+x+',\"y\":'+y+'}';ws.send(eventjson);}}"+
    "\nvar updateScreen = function() {screen.src = screenFile + new Date().getTime();}"+
    "\nscreen.addEventListener('mousedown', down, false);\nscreen.addEventListener('mouseup', up, false);\nscreen.addEventListener('mousemove', move, false);\nsetInterval('updateScreen()', 200);"+
    "</script></html>";
}
