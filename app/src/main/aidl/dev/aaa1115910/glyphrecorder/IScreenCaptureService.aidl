// IScreenCaptureService.aidl
package dev.aaa1115910.glyphrecorder;

// Declare any non-default types here with import statements

interface IScreenCaptureService {
    boolean startScreenCapture() = 1;
    void updateCircles(String circles) = 3;
    byte[] takeScreenshotBitmapArray() = 4;
}