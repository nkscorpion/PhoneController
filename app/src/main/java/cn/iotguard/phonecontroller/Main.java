package cn.iotguard.phonecontroller;

import android.graphics.Bitmap;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.view.InputDeviceCompat;
import android.view.InputEvent;
import android.view.MotionEvent;

import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
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
    private static InputManager sInputManager;
    private static Method sInjectInputEventMethod;
    private static int sScreenWidth = 720;
    private static int sScreenHeight = 1280;
    public static void main(String[] args) {
        Looper.prepare();
        System.out.println(
                String.format(Locale.CHINA, "PhoneController start...%dx%d", sScreenWidth, sScreenHeight));
        try {
            sInputManager = (InputManager) InputManager.class.getDeclaredMethod("getInstance").invoke(null);
            sInjectInputEventMethod = InputManager.class.getMethod("injectInputEvent", InputEvent.class, Integer.TYPE);
            AsyncHttpServer httpServer = new AsyncHttpServer();
            httpServer.get("/screenshot.jpg", new RequestHandler());
            httpServer.websocket("/input", new InputHandler());
            httpServer.listen(LISTEN_PORT);
            Looper.loop();
        } catch (Exception e) {
            System.out.print("error: " + e.getMessage());
        }
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
                                injectMotionEvent(InputDeviceCompat.SOURCE_TOUCHSCREEN, 0,
                                        SystemClock.uptimeMillis(), x, y, 1.0f);
                                break;
                            case KEY_FINGER_UP:
                                injectMotionEvent(InputDeviceCompat.SOURCE_TOUCHSCREEN, 1,
                                        SystemClock.uptimeMillis(), x, y, 1.0f);
                                break;
                            case KEY_FINGER_MOVE:
                                injectMotionEvent(InputDeviceCompat.SOURCE_TOUCHSCREEN, 2,
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

    private static void injectMotionEvent(int inputSource, int action, long when, float x, float y, float pressure) {
        try {
            MotionEvent event = MotionEvent.obtain(when, when, action, x, y, pressure, 1.0f, 0, 1.0f, 1.0f, 0, 0);
            event.setSource(inputSource);
            sInjectInputEventMethod.invoke(sInputManager, event, 0);
            event.recycle();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}