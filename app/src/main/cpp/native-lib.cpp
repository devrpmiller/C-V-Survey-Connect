#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring

//Called from Java code
JNICALL
Java_net_cvc_1inc_cvsurveyconnect_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
