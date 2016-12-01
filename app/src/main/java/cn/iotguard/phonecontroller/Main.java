package cn.iotguard.phonecontroller;

import android.graphics.Bitmap;
import android.graphics.Matrix;
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

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by caowentao on 2016/11/23.
 */

public class Main {

    private static final int LISTEN_PORT = 56789;
    private static final String KEY_FINGER_DOWN = "fingerdown";
    private static final String KEY_FINGER_UP = "fingerup";
    private static final String KEY_FINGER_MOVE = "fingermove";
    private static final String KEY_CHANGE_SIZE = "change_size";
    private static final String KEY_ROTATE = "rotate";
    private static final String KEY_BEATHEART = "beatheart";
    private static final String KEY_EVENT_TYPE = "type";
    private static InputManager sInputManager;
    private static Method sInjectInputEventMethod;
    private static final float BASE_WIDTH = 720;
    private static final float BASE_HEIGHT = 1280;
    private static int sPictureWidth = 360;
    private static int sPictureHeight = 640;
    private static int sRotate = 0;
    private static Thread sSendImageThread;
    private static Timer sTimer;
    private static boolean sViewerIsAlive;
    private static boolean sThreadKeepRunning;

    public static void main(String[] args) {
        Looper.prepare();
        System.out.println("PhoneController start...");
        sTimer = new Timer();
        try {
            sInputManager = (InputManager) InputManager.class.getDeclaredMethod("getInstance").invoke(null);
            sInjectInputEventMethod = InputManager.class.getMethod("injectInputEvent", InputEvent.class, Integer.TYPE);
            AsyncHttpServer httpServer = new AsyncHttpServer();
            httpServer.websocket("/input", new InputHandler());
            httpServer.listen(LISTEN_PORT);
            Looper.loop();
        } catch (Exception e) {
            System.out.println("error: " + e.getMessage());
        }
    }

    private static class InputHandler implements AsyncHttpServer.WebSocketRequestCallback {
        @Override
        public void onConnected(WebSocket webSocket, AsyncHttpServerRequest request) {
            System.out.println("websocket connected.");
            if (sSendImageThread == null) {
                sThreadKeepRunning = true;
                sSendImageThread = new Thread(new SendScreenShotThread((webSocket)));
                sSendImageThread.start();
                sTimer.schedule(new SendScreenShotThreadWatchDog(), 5000, 5000);
                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    @Override
                    public void onStringAvailable(String s) {
                        try {
                            JSONObject event = new JSONObject(s);
                            String eventType = event.getString(KEY_EVENT_TYPE);
                            switch (eventType) {
                                case KEY_FINGER_DOWN:
                                    float x = Float.parseFloat(event.getString("x")) * (BASE_WIDTH / sPictureWidth);
                                    float y = Float.parseFloat(event.getString("y")) * (BASE_WIDTH / sPictureWidth);
                                    injectMotionEvent(InputDeviceCompat.SOURCE_TOUCHSCREEN, 0,
                                            SystemClock.uptimeMillis(), x, y, 1.0f);
                                    break;
                                case KEY_FINGER_UP:
                                    x = Float.parseFloat(event.getString("x")) * (BASE_WIDTH / sPictureWidth);
                                    y = Float.parseFloat(event.getString("y")) * (BASE_WIDTH / sPictureWidth);
                                    injectMotionEvent(InputDeviceCompat.SOURCE_TOUCHSCREEN, 1,
                                            SystemClock.uptimeMillis(), x, y, 1.0f);
                                    break;
                                case KEY_FINGER_MOVE:
                                    x = Float.parseFloat(event.getString("x")) * (BASE_WIDTH / sPictureWidth);
                                    y = Float.parseFloat(event.getString("y")) * (BASE_WIDTH / sPictureWidth);
                                    injectMotionEvent(InputDeviceCompat.SOURCE_TOUCHSCREEN, 2,
                                            SystemClock.uptimeMillis(), x, y, 1.0f);
                                    break;
                                case KEY_BEATHEART:
                                    sViewerIsAlive = true;
                                    break;
                                case KEY_CHANGE_SIZE:
                                    sPictureWidth = Integer.parseInt(event.getString("w"));
                                    sPictureHeight = Integer.parseInt(event.getString("h"));
                                    break;
                                case KEY_ROTATE:
                                    if (sRotate == 270) sRotate = 0;
                                    else sRotate += 90;
                                    break;
                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                });
            } else {
                webSocket.close();
            }
        }
    }

    private static class SendScreenShotThreadWatchDog extends TimerTask {
        @Override
        public void run() {
            if (sViewerIsAlive) {
                sViewerIsAlive = false;
            } else if (sSendImageThread != null) {
                System.out.println("exit thread");
                sThreadKeepRunning = false;
                sSendImageThread = null;
                cancel();
                sTimer.purge();
            }
        }
    }

    private static class SendScreenShotThread implements Runnable {

        WebSocket mWebSocket;
        String mSurfaceName;
        SendScreenShotThread(WebSocket webSocket) {
            mWebSocket = webSocket;
            if (Build.VERSION.SDK_INT <= 17) {
                mSurfaceName = "android.view.Surface";
            } else {
                mSurfaceName = "android.view.SurfaceControl";
            }
        }
        @Override
        public void run() {
            while (sThreadKeepRunning) {
                try {
                    Bitmap bitmap = (Bitmap) Class.forName(mSurfaceName)
                            .getDeclaredMethod("screenshot", new Class[]{Integer.TYPE, Integer.TYPE})
                            .invoke(null, sPictureWidth, sPictureHeight);
                    Matrix matrix = new Matrix();
                    matrix.setRotate(sRotate);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bout);
                    bout.flush();
                    mWebSocket.send(bout.toByteArray());
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    break;
                }
            }
            mWebSocket.close();
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