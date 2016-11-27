package cn.iotguard.phonecontroller;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.view.InputDeviceCompat;

import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

/**
 * Created by caowentao on 2016/11/23.
 */

public class Main {

    private static final int LISTEN_PORT = 56789;
    private static final String KEY_FINGER_DOWN = "fingerdown";
    private static final String KEY_FINGER_UP = "fingerup";
    private static final String KEY_FINGER_MOVE = "fingermove";
    private static final String KEY_EVENT_TYPE = "type";
    private static EventInput sEventInput;
    private static int sScreenWidth = 720;
    private static int sScreenHeight = 1280;
    public static void main(String[] args) {
        Looper.prepare();
        System.out.println(
                String.format(Locale.CHINA, "PhoneController start...%dx%d", sScreenWidth, sScreenHeight));
        try {
            sEventInput = new EventInput();
        } catch (Exception e) {
            System.out.print("EventInput error: " + e.getMessage());
            sEventInput = null;
        }
        AsyncHttpServer httpServer = new AsyncHttpServer();
        httpServer.get("/screenshot.jpg", new RequestHandler());
        httpServer.websocket("/input", new InputHandler());
        httpServer.listen(LISTEN_PORT);
        Looper.loop();
    }

    private static class InputHandler implements AsyncHttpServer.WebSocketRequestCallback {
        @Override
        public void onConnected(WebSocket webSocket, AsyncHttpServerRequest request) {
            System.out.println("websocket connected.");
            webSocket.setStringCallback(new WebSocket.StringCallback() {
                @Override
                public void onStringAvailable(String s) {
                    try {
                        JSONObject touch = new JSONObject(s);
                        float x = Float.parseFloat(touch.getString("x"))*1.5f;
                        float y = Float.parseFloat(touch.getString("y"))*1.5f;
                        String eventType = touch.getString(KEY_EVENT_TYPE);
                        switch (eventType) {
                            case KEY_FINGER_DOWN:
                                sEventInput.injectMotionEvent(InputDeviceCompat.SOURCE_TOUCHSCREEN, 0,
                                        SystemClock.uptimeMillis(), x, y, 1.0f);
                                break;
                            case KEY_FINGER_UP:
                                sEventInput.injectMotionEvent(InputDeviceCompat.SOURCE_TOUCHSCREEN, 1,
                                        SystemClock.uptimeMillis(), x, y, 1.0f);
                                break;
                            case KEY_FINGER_MOVE:
                                sEventInput.injectMotionEvent(InputDeviceCompat.SOURCE_TOUCHSCREEN, 2,
                                        SystemClock.uptimeMillis(), x, y, 1.0f);
                                break;
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            });
        }
    }

    private static class RequestHandler implements HttpServerRequestCallback {
        public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
            try {
                String surfaceClassName;
                if (Build.VERSION.SDK_INT <= 17) {
                    surfaceClassName = "android.view.Surface";
                } else {
                    surfaceClassName = "android.view.SurfaceControl";
                }
                Bitmap bitmap = (Bitmap) Class.forName(surfaceClassName)
                        .getDeclaredMethod("screenshot", new Class[]{Integer.TYPE, Integer.TYPE})
                        .invoke(null, sScreenWidth / 2, sScreenHeight / 2);
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bout);
                bout.flush();
                response.send("image/jpeg", bout.toByteArray());
            } catch (Exception e) {
                response.code(500);
                response.send(e.toString());
            }
        }
    }
}