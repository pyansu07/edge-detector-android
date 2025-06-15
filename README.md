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
---

## 🖼️ Screenshots

| Raw Preview | Edge Detection View |
|-------------|----------------------|
| ![raw](https://github.com/user-attachments/assets/5aee1ba3-f1e2-4306-8571-316eaa9e1f3c) | ![processed](https://github.com/user-attachments/assets/dc7581d7-267c-4d46-8388-e9d4baebb9b0) |

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
![Architecture](https://github.com/user-attachments/assets/494c61f1-d37c-4113-87fe-fb5504fc235a)

### 🔁 Frame Flow

1. **Camera2 API** (`MainActivity`)  
   Captures real-time frames in `YUV_420_888` format using `ImageReader`.

2. **YUV → NV21 Conversion**  
   Planes of Y, U, and V are restructured in Java to NV21 byte array (Y + interleaved VU).

3. **JNI Bridge** (`FrameProcessor`)  
   Passes the `nv21[]` frame with width and height to native C++ using JNI (`native-lib.cpp`).

4. **OpenCV Processing (C++)**
   - Converts `NV21` to `BGR`
   - Applies `cv::Canny()` for edge detection
   - Converts result to `ARGB_8888` byte buffer for OpenGL

5. **Back to Java**  
   Returns processed `ARGB` byte array to Java using `jbyteArray`.

6. **Rendering via OpenGL (`EdgeRenderer`)**
   - Updates texture using the `ARGB` data
   - Renders using `GLSurfaceView` with custom shaders
   - Supports toggle between raw and edge view

---


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
Frame time: xx ms
```
