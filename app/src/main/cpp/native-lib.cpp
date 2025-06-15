#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>

#define LOG_TAG "native-lib"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

using namespace cv;

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_edgedetector_FrameProcessor_processFrame(
        JNIEnv *env,
        jobject,
        jbyteArray inputFrame,
        jint width,
        jint height) {

    if (inputFrame == nullptr || width <= 0 || height <= 0) {
        LOGD("Invalid input to native code.");
        return nullptr;
    }

    jbyte *inputBytes = env->GetByteArrayElements(inputFrame, nullptr);
    int yuvSize = env->GetArrayLength(inputFrame);

    int expectedSize = width * height * 3 / 2;  // For NV21 format
    if (yuvSize != expectedSize) {
        LOGD("Mismatched input size. Expected %d but got %d", expectedSize, yuvSize);
        env->ReleaseByteArrayElements(inputFrame, inputBytes, JNI_ABORT);
        return nullptr;
    }

    // Create YUV Mat
    Mat yuvImage(height + height / 2, width, CV_8UC1, (unsigned char *)inputBytes);
    Mat bgrImage;
    try {
        cvtColor(yuvImage, bgrImage, COLOR_YUV2BGR_NV21);
    } catch (cv::Exception &e) {
        LOGD("OpenCV cvtColor failed: %s", e.what());
        env->ReleaseByteArrayElements(inputFrame, inputBytes, JNI_ABORT);
        return nullptr;
    }

    // Apply Canny
    Mat gray, blurred, edges, edgesBGR;

    cvtColor(bgrImage, gray, COLOR_BGR2GRAY);
    GaussianBlur(gray, blurred, Size(5, 5), 1.5); // Reduce noise
    Canny(blurred, edges, 50, 150);
    cvtColor(edges, edgesBGR, COLOR_GRAY2BGR);


    int outputSize = edgesBGR.total() * edgesBGR.elemSize();
    jbyteArray result = env->NewByteArray(outputSize);
    env->SetByteArrayRegion(result, 0, outputSize, (jbyte *)edgesBGR.data);

    env->ReleaseByteArrayElements(inputFrame, inputBytes, JNI_ABORT);

    LOGD("Processed frame successfully");
    return result;
}
