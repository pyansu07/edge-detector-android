package com.example.edgedetector;

public class FrameProcessor {
    static {
        System.loadLibrary("native-lib");
    }

    public native byte[] processFrame(byte[] frameData, int width, int height);
}
