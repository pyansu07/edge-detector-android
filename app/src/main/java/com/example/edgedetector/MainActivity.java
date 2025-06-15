package com.example.edgedetector;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.hardware.camera2.*;
import android.os.Handler;
import android.os.HandlerThread;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    private static final String TAG = "EdgeDetector";

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private GLSurfaceView glSurfaceView;
    private EdgeRenderer edgeRenderer;
    private FrameProcessor frameProcessor;
    private Switch toggleView;
    private TextView fpsTextView;

    private long lastFpsTime = System.currentTimeMillis();
    private int frameCount = 0;

    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glSurfaceView = findViewById(R.id.glSurfaceView);
        toggleView = findViewById(R.id.toggleView);
        fpsTextView = findViewById(R.id.fpsCounter);

        glSurfaceView.setEGLContextClientVersion(2);
        edgeRenderer = new EdgeRenderer();
        glSurfaceView.setRenderer(edgeRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        frameProcessor = new FrameProcessor();

        toggleView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            edgeRenderer.setShowEdges(isChecked);
            glSurfaceView.requestRender();
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            startCamera();
        }
    }

    private void startCamera() {
        startBackgroundThread();

        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            Size[] sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(ImageFormat.YUV_420_888);
            Size previewSize = sizes[0];

            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    processFrame(image);
                    image.close();
                }
            }, backgroundHandler);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createCameraSession(previewSize);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                }
            }, backgroundHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createCameraSession(Size previewSize) {
        try {
            Surface surface = imageReader.getSurface();
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(surface);

            cameraDevice.createCaptureSession(
                    java.util.Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;
                            try {
                                captureSession.setRepeatingRequest(builder.build(), null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "Camera config failed.");
                        }
                    },
                    backgroundHandler
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processFrame(Image image) {
        if (image.getFormat() != ImageFormat.YUV_420_888) return;

        int width = image.getWidth();
        int height = image.getHeight();

        try {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();

            byte[] nv21 = new byte[width * height * 3 / 2];
            yBuffer.get(nv21, 0, ySize);

            int chromaRowStride = planes[1].getRowStride();
            int chromaPixelStride = planes[1].getPixelStride();
            int offset = width * height;

            for (int row = 0; row < height / 2; row++) {
                for (int col = 0; col < width / 2; col++) {
                    int vuIndex = row * chromaRowStride + col * chromaPixelStride;
                    nv21[offset++] = vBuffer.get(vuIndex);
                    nv21[offset++] = uBuffer.get(vuIndex);
                }
            }

            byte[] resultBytes = frameProcessor.processFrame(nv21, width, height);
            if (resultBytes == null) {
                Log.e(TAG, "JNI returned null. Skipping frame.");
                return;
            }

            if (toggleView.isChecked()) {
                edgeRenderer.updateFrame(resultBytes, width, height);
            } else {
                edgeRenderer.updateFrame(convertToARGB(nv21, width, height), width, height);
            }

            glSurfaceView.requestRender();

            // FPS Counter
            frameCount++;
            long now = System.currentTimeMillis();
            if (now - lastFpsTime >= 1000) {
                int fps = frameCount;
                runOnUiThread(() -> fpsTextView.setText("FPS: " + fps));
                frameCount = 0;
                lastFpsTime = now;
            }

        } catch (Exception e) {
            Log.e(TAG, "Frame processing error: " + e.getMessage());
        }
    }

    private byte[] convertToARGB(byte[] nv21, int width, int height) {
        byte[] argb = new byte[width * height * 4];
        for (int i = 0; i < argb.length; i += 4) {
            argb[i] = (byte) 0xFF;
            argb[i + 1] = (byte) 0xFF;
            argb[i + 2] = (byte) 0xFF;
            argb[i + 3] = (byte) 0xFF;
        }
        return argb;
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraDevice != null) {
            cameraDevice.close();
        }
        stopBackgroundThread();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
