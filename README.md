
# 📸 Real-Time Edge Detection Android App

This Android app captures camera frames in real-time, processes them using native C++ (OpenCV via JNI), and displays the results using OpenGL ES 2.0. It supports raw preview or edge-detected view toggle and includes FPS logging for performance insights.

---

## ✅ Features Implemented

-  Real-time camera frame capture using Camera2 API
-  Native frame processing with OpenCV (NDK + JNI)
-  Edge detection using `cv::Canny` in native C++
-  Toggle between raw frame and processed edge view
-  GPU rendering via OpenGL ES 2.0 (`GLSurfaceView`)
-  NV21 to ARGB8888 conversion using native memory
-  FPS counter and processing time logger
-  Optimized frame-to-texture flow with minimal latency

---

## ⚙️ Setup Instructions

### 🧱 Android Studio Setup

1. **Clone the repo**  
   ```bash
   git clone https://github.com/your-username/edge-detector-android.git
   
2. **Open in Android Studio**

3. **Install the required SDK/NDK**
   - NDK version: `25.1.8937393` (set via SDK Manager)
   - CMake: `3.22.1` or later

4. **Install OpenCV Android SDK**
   - Download from: https://opencv.org/releases
   - Extract and place it in:  
     `OpenCV-android-sdk/` in your project root or adjust the path in `CMakeLists.txt`

5. **Connect device**
   - Ensure your Android phone has **Developer Mode** and **USB Debugging** enabled.

---

## 🧠 Architecture Overview
![Gemini_Generated_Image_boihi6boihi6boih](https://github.com/user-attachments/assets/494c61f1-d37c-4113-87fe-fb5504fc235a)

### 📦 Modules

| Component           | Role |
|---------------------|------|
| `MainActivity`      | Handles camera setup, JNI, and UI |
| `FrameProcessor`    | JNI bridge between Java and C++ |
| `native-lib.cpp`    | C++ code using OpenCV for edge detection |
| `EdgeRenderer`      | OpenGL ES renderer that displays ARGB image |
| `GLSurfaceView`     | Used to show GPU-rendered frames |
| `Switch (Toggle)`   | UI control to flip raw/processed views |

## 📊 FPS Logging

Frame processing time (ms) is logged to **Logcat** as:
```bash
Frame time: 16 ms
```
