// IScreenCaptureService.aidl
package dev.aaa1115910.glyphrecorder;

// Declare any non-default types here with import statements

interface IScreenCaptureService {
    void destroy() = 16777114; // Reserved Shizuku user-service destruction transaction
    boolean startScreenCapture() = 1;
    void stopScreenCapture() = 2;
    void updateCircles(String circles) = 3;
    byte[] takeScreenshotBitmapArray() = 4;
    void attachClient(IBinder client) = 5;
}
