cmake_minimum_required(VERSION 3.22.1)
project("edgedetector")

set(OpenCV_SDK_PATH "C:/Users/pyans/Downloads/opencv-4.11.0-android-sdk/OpenCV-android-sdk")

include_directories("${OpenCV_SDK_PATH}/sdk/native/jni/include")

add_library(opencv_java4 SHARED IMPORTED)
set_target_properties(opencv_java4 PROPERTIES
        IMPORTED_LOCATION "${OpenCV_SDK_PATH}/sdk/native/libs/${ANDROID_ABI}/libopencv_java4.so"
)

add_library(native-lib SHARED native-lib.cpp)

target_link_libraries(native-lib
        opencv_java4
        ${OpenCV_LIBS}
        log
        android
)
